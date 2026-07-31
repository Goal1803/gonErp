package com.gonerp.notes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads a TipTap/ProseMirror document. Two things are pulled out of the JSON on
 * every save: a flattened text copy (so search can stay a plain LIKE) and the set
 * of mentioned note ids (which drives backlinks).
 */
@Slf4j
public final class NoteContentUtil {

    /** Node name emitted by the front-end mention extension. */
    public static final String MENTION_NODE = "noteMention";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_PLAIN_TEXT = 200_000;

    private NoteContentUtil() {}

    public static String extractPlainText(String contentJson) {
        JsonNode doc = parse(contentJson);
        if (doc == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        walkText(doc, sb);
        String text = sb.toString().replaceAll("[ \\t]+", " ").trim();
        return text.length() > MAX_PLAIN_TEXT ? text.substring(0, MAX_PLAIN_TEXT) : text;
    }

    public static Set<Long> extractMentionedNoteIds(String contentJson) {
        Set<Long> ids = new LinkedHashSet<>();
        JsonNode doc = parse(contentJson);
        if (doc != null) {
            walkMentions(doc, ids);
        }
        return ids;
    }

    private static JsonNode parse(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(contentJson);
        } catch (Exception e) {
            log.warn("Could not parse note content as JSON: {}", e.getMessage());
            return null;
        }
    }

    private static void walkText(JsonNode node, StringBuilder sb) {
        if (node.isArray()) {
            node.forEach(child -> walkText(child, sb));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.get("type");
        if (type != null && MENTION_NODE.equals(type.asText())) {
            JsonNode label = node.path("attrs").get("label");
            if (label != null && !label.isNull()) {
                sb.append(label.asText()).append(' ');
            }
            return;
        }
        JsonNode text = node.get("text");
        if (text != null && text.isTextual()) {
            sb.append(text.asText());
        }
        // Block-level nodes end a line so snippets don't run words together.
        JsonNode content = node.get("content");
        if (content != null) {
            walkText(content, sb);
            if (type != null && !"text".equals(type.asText())) {
                sb.append('\n');
            }
        }
    }

    private static void walkMentions(JsonNode node, Set<Long> ids) {
        if (node.isArray()) {
            node.forEach(child -> walkMentions(child, ids));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.get("type");
        if (type != null && MENTION_NODE.equals(type.asText())) {
            JsonNode id = node.path("attrs").get("id");
            if (id != null && !id.isNull()) {
                try {
                    ids.add(Long.parseLong(id.asText()));
                } catch (NumberFormatException ignored) {
                    // A mention that lost its id is not a link; skip it.
                }
            }
        }
        JsonNode content = node.get("content");
        if (content != null) {
            walkMentions(content, ids);
        }
    }

    /** Text around the first hit, used for the search-result preview line. */
    public static String snippet(String plainText, String query, int radius) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        String flat = plainText.replace('\n', ' ');
        int at = query == null || query.isBlank() ? -1
                : flat.toLowerCase().indexOf(query.toLowerCase());
        if (at < 0) {
            return flat.length() <= radius * 2 ? flat : flat.substring(0, radius * 2) + "…";
        }
        int from = Math.max(0, at - radius);
        int to = Math.min(flat.length(), at + query.length() + radius);
        return (from > 0 ? "…" : "") + flat.substring(from, to) + (to < flat.length() ? "…" : "");
    }
}
