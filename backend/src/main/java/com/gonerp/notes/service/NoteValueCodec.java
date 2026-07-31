package com.gonerp.notes.service;

import com.gonerp.notes.model.NoteProperty;
import com.gonerp.notes.model.enums.PropertyType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cell values live in a single text column. This is the one place that knows
 * how each {@link PropertyType} is written, normalised and compared, so
 * filtering, sorting and grouping all agree with each other.
 */
public final class NoteValueCodec {

    private NoteValueCodec() {}

    /** Cleans user input before it is stored; returns null for "empty". */
    public static String normalise(PropertyType type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        switch (type) {
            case NUMBER:
                try {
                    return trimNumber(Double.parseDouble(v.replace(",", ".")));
                } catch (NumberFormatException e) {
                    return null;
                }
            case CHECKBOX:
                return Boolean.parseBoolean(v) || "1".equals(v) ? "true" : "false";
            case DATE:
                try {
                    return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v).toString();
                } catch (Exception e) {
                    return null;
                }
            case MULTI_SELECT:
            case PERSON:
                List<String> parts = new ArrayList<>();
                for (String p : v.split(",")) {
                    if (!p.isBlank() && !parts.contains(p.trim())) {
                        parts.add(p.trim());
                    }
                }
                return parts.isEmpty() ? null : String.join(",", parts);
            default:
                return v;
        }
    }

    private static String trimNumber(double d) {
        return d == Math.rint(d) && !Double.isInfinite(d)
                ? String.valueOf((long) d) : String.valueOf(d);
    }

    public static List<String> asList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static Double asNumber(String value) {
        try {
            return value == null ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean asBoolean(String value) {
        return "true".equals(value);
    }

    /**
     * Ordering key used by sorts. Numbers and dates must not sort as text
     * ("10" &lt; "9"), so they are compared as numbers where possible.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Comparable sortKey(PropertyType type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        switch (type) {
            case NUMBER:
                Double d = asNumber(value);
                return d == null ? null : d;
            case CHECKBOX:
                return asBoolean(value) ? 1 : 0;
            case DATE:
                try {
                    return LocalDate.parse(value);
                } catch (Exception e) {
                    return null;
                }
            default:
                return value.toLowerCase();
        }
    }

    /** Nulls always sort last, regardless of direction. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int compare(Comparable a, Comparable b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        if (a.getClass() != b.getClass()) {
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
        return a.compareTo(b);
    }

    /**
     * Evaluates one filter clause.
     *
     * @param op one of is, is_not, contains, not_contains, is_empty, is_not_empty,
     *           gt, gte, lt, lte, before, after, checked, unchecked
     */
    public static boolean matches(NoteProperty property, String value, String op, String operand) {
        PropertyType type = property.getType();
        boolean empty = value == null || value.isBlank();

        switch (op) {
            case "is_empty":
                return empty;
            case "is_not_empty":
                return !empty;
            case "checked":
                return asBoolean(value);
            case "unchecked":
                return !asBoolean(value);
            default:
                break;
        }
        if (empty) {
            // Nothing but the emptiness checks above can match a blank cell.
            return false;
        }

        switch (op) {
            case "is":
                if (type == PropertyType.MULTI_SELECT || type == PropertyType.PERSON) {
                    return asList(value).contains(operand);
                }
                return value.equalsIgnoreCase(operand);
            case "is_not":
                if (type == PropertyType.MULTI_SELECT || type == PropertyType.PERSON) {
                    return !asList(value).contains(operand);
                }
                return !value.equalsIgnoreCase(operand);
            case "contains":
                return value.toLowerCase().contains(String.valueOf(operand).toLowerCase());
            case "not_contains":
                return !value.toLowerCase().contains(String.valueOf(operand).toLowerCase());
            case "gt":
            case "gte":
            case "lt":
            case "lte":
            case "before":
            case "after": {
                Comparable left = sortKey(type, value);
                Comparable right = sortKey(type, normalise(type, operand));
                if (left == null || right == null) {
                    return false;
                }
                int cmp = compare(left, right);
                return switch (op) {
                    case "gt", "after" -> cmp > 0;
                    case "gte" -> cmp >= 0;
                    case "lt", "before" -> cmp < 0;
                    default -> cmp <= 0;
                };
            }
            default:
                return true;
        }
    }
}
