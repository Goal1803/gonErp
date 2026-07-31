package com.gonerp.notes.service;

import com.gonerp.common.OrgContext;
import com.gonerp.notes.dto.*;
import com.gonerp.notes.model.Note;
import com.gonerp.notes.model.NoteProperty;
import com.gonerp.notes.model.NoteValue;
import com.gonerp.notes.model.NoteView;
import com.gonerp.notes.model.enums.PropertyType;
import com.gonerp.notes.model.enums.ViewType;
import com.gonerp.notes.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Notion-style databases on top of the existing note tree: a database is a note
 * flagged {@code database}, its rows are that note's child notes, and each view
 * decides which rows are shown, in what order and how they are grouped.
 */
@Service
@RequiredArgsConstructor
public class NoteDatabaseService {

    private static final String UNGROUPED = "__none__";

    private final NoteRepository noteRepository;
    private final NotePropertyRepository propertyRepository;
    private final NoteViewRepository viewRepository;
    private final NoteValueRepository valueRepository;
    private final UserRepository userRepository;
    private final NoteService noteService;

    // ------------------------------------------------------------- lifecycle

    /** Turns an ordinary note into a database, seeding a Status column + Table view. */
    @Transactional
    public DatabaseResponse convertToDatabase(Long noteId) {
        String permission = noteService.requireAccess(noteId, true);
        Note note = noteService.requireNote(noteId);
        if (note.isDatabase()) {
            return getSchema(noteId);
        }
        note.setDatabase(true);
        noteRepository.save(note);

        // Existing children become the first rows, so converting never hides content.
        for (Note child : noteRepository.findByParentIdOrderByPositionAsc(noteId)) {
            if (!child.isArchived()) {
                child.setRowOf(note);
                noteRepository.save(child);
            }
        }

        NoteProperty status = propertyRepository.save(NoteProperty.builder()
                .database(note).name("Trạng thái").type(PropertyType.SELECT).position(0)
                .options(new ArrayList<>(List.of(
                        "todo:Cần làm:grey",
                        "doing:Đang làm:blue",
                        "done:Xong:green")))
                .build());
        propertyRepository.save(NoteProperty.builder()
                .database(note).name("Ngày").type(PropertyType.DATE).position(1).build());

        viewRepository.save(NoteView.builder()
                .database(note).name("Bảng").type(ViewType.TABLE).position(0).build());
        viewRepository.save(NoteView.builder()
                .database(note).name("Board").type(ViewType.BOARD).position(1)
                .groupById(status.getId()).build());

        DatabaseResponse r = getSchema(noteId);
        r.setPermission(permission);
        return r;
    }

    @Transactional
    public void convertToPage(Long noteId) {
        noteService.requireAccess(noteId, true);
        Note note = noteService.requireNote(noteId);
        // Rows stay as ordinary child pages; only the schema goes away.
        for (Note row : noteRepository.findByParentIdOrderByPositionAsc(noteId)) {
            row.setRowOf(null);
            noteRepository.save(row);
        }
        for (NoteProperty p : propertyRepository.findByDatabaseIdOrderByPositionAsc(noteId)) {
            valueRepository.deleteByPropertyId(p.getId());
        }
        propertyRepository.deleteByDatabaseId(noteId);
        viewRepository.deleteByDatabaseId(noteId);
        note.setDatabase(false);
        noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public DatabaseResponse getSchema(Long databaseId) {
        String permission = noteService.requireAccess(databaseId, false);
        Note note = requireDatabase(databaseId);

        DatabaseResponse r = new DatabaseResponse();
        r.setId(note.getId());
        r.setTitle(note.getTitle());
        r.setPermission(permission);
        r.setProperties(propertyRepository.findByDatabaseIdOrderByPositionAsc(databaseId).stream()
                .map(PropertyResponse::from).toList());
        r.setViews(viewRepository.findByDatabaseIdOrderByPositionAsc(databaseId).stream()
                .map(ViewResponse::from).toList());
        return r;
    }

    // ------------------------------------------------------------ properties

    @Transactional
    public PropertyResponse createProperty(Long databaseId, PropertyRequest request) {
        noteService.requireAccess(databaseId, true);
        Note db = requireDatabase(databaseId);
        List<NoteProperty> existing = propertyRepository.findByDatabaseIdOrderByPositionAsc(databaseId);

        NoteProperty p = NoteProperty.builder()
                .database(db)
                .name(request.getName() == null || request.getName().isBlank()
                        ? "Thuộc tính" : request.getName().trim())
                .type(request.getType() == null ? PropertyType.TEXT : request.getType())
                .options(request.getOptions() == null ? new ArrayList<>() : request.getOptions())
                .numberFormat(request.getNumberFormat())
                .position(request.getPosition() == null ? existing.size() : request.getPosition())
                .build();
        return PropertyResponse.from(propertyRepository.save(p));
    }

    @Transactional
    public PropertyResponse updateProperty(Long propertyId, PropertyRequest request) {
        NoteProperty p = requireProperty(propertyId);
        noteService.requireAccess(p.getDatabase().getId(), true);

        if (request.getName() != null && !request.getName().isBlank()) {
            p.setName(request.getName().trim());
        }
        if (request.getOptions() != null) {
            p.setOptions(request.getOptions());
        }
        if (request.getNumberFormat() != null) {
            p.setNumberFormat(request.getNumberFormat());
        }
        if (request.getPosition() != null) {
            p.setPosition(request.getPosition());
        }
        if (request.getType() != null && request.getType() != p.getType()) {
            // Changing the type re-reads every stored value through the new rules;
            // anything that no longer parses is cleared rather than left corrupt.
            p.setType(request.getType());
            for (NoteValue v : valueRepository.findByDatabaseId(p.getDatabase().getId())) {
                if (v.getProperty().getId().equals(propertyId)) {
                    v.setValue(NoteValueCodec.normalise(request.getType(), v.getValue()));
                    valueRepository.save(v);
                }
            }
        }
        return PropertyResponse.from(propertyRepository.save(p));
    }

    @Transactional
    public void deleteProperty(Long propertyId) {
        NoteProperty p = requireProperty(propertyId);
        Long databaseId = p.getDatabase().getId();
        noteService.requireAccess(databaseId, true);

        valueRepository.deleteByPropertyId(propertyId);
        // Drop the column from every view that referenced it, so nothing dangles.
        for (NoteView v : viewRepository.findByDatabaseIdOrderByPositionAsc(databaseId)) {
            boolean touched = false;
            if (Objects.equals(v.getGroupById(), propertyId)) {
                v.setGroupById(null);
                touched = true;
            }
            touched |= v.getVisiblePropertyIds().remove(propertyId);
            touched |= v.getFilters().removeIf(f -> f.startsWith(propertyId + ":"));
            touched |= v.getSorts().removeIf(s -> s.startsWith(propertyId + ":"));
            if (touched) {
                viewRepository.save(v);
            }
        }
        propertyRepository.delete(p);
    }

    // ----------------------------------------------------------------- views

    @Transactional
    public ViewResponse createView(Long databaseId, ViewRequest request) {
        noteService.requireAccess(databaseId, true);
        Note db = requireDatabase(databaseId);
        List<NoteView> existing = viewRepository.findByDatabaseIdOrderByPositionAsc(databaseId);
        ViewType type = request.getType() == null ? ViewType.TABLE : request.getType();

        NoteView v = NoteView.builder()
                .database(db)
                .name(request.getName() == null || request.getName().isBlank()
                        ? defaultName(type) : request.getName().trim())
                .type(type)
                .groupById(request.getGroupById())
                .filters(request.getFilters() == null ? new ArrayList<>() : request.getFilters())
                .sorts(request.getSorts() == null ? new ArrayList<>() : request.getSorts())
                .visiblePropertyIds(request.getVisiblePropertyIds() == null
                        ? new ArrayList<>() : request.getVisiblePropertyIds())
                .position(request.getPosition() == null ? existing.size() : request.getPosition())
                .build();

        // A board with nothing to group by is useless — fall back to the first select.
        if (type == ViewType.BOARD && v.getGroupById() == null) {
            propertyRepository.findByDatabaseIdOrderByPositionAsc(databaseId).stream()
                    .filter(p -> p.getType() == PropertyType.SELECT)
                    .findFirst()
                    .ifPresent(p -> v.setGroupById(p.getId()));
        }
        return ViewResponse.from(viewRepository.save(v));
    }

    @Transactional
    public ViewResponse updateView(Long viewId, ViewRequest request) {
        NoteView v = requireView(viewId);
        noteService.requireAccess(v.getDatabase().getId(), true);

        if (request.getName() != null && !request.getName().isBlank()) {
            v.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            v.setType(request.getType());
        }
        if (request.getFilters() != null) {
            v.setFilters(request.getFilters());
        }
        if (request.getSorts() != null) {
            v.setSorts(request.getSorts());
        }
        if (request.getVisiblePropertyIds() != null) {
            v.setVisiblePropertyIds(request.getVisiblePropertyIds());
        }
        if (request.getPosition() != null) {
            v.setPosition(request.getPosition());
        }
        // groupById is nullable on purpose: sending 0 clears it.
        if (request.getGroupById() != null) {
            v.setGroupById(request.getGroupById() == 0 ? null : request.getGroupById());
        }
        return ViewResponse.from(viewRepository.save(v));
    }

    @Transactional
    public void deleteView(Long viewId) {
        NoteView v = requireView(viewId);
        Long databaseId = v.getDatabase().getId();
        noteService.requireAccess(databaseId, true);
        if (viewRepository.findByDatabaseIdOrderByPositionAsc(databaseId).size() <= 1) {
            throw new IllegalArgumentException("A database needs at least one view");
        }
        viewRepository.delete(v);
    }

    // ------------------------------------------------------------------ rows

    @Transactional(readOnly = true)
    public RowsResponse getRows(Long databaseId, Long viewId) {
        noteService.requireAccess(databaseId, false);
        requireDatabase(databaseId);

        List<NoteView> views = viewRepository.findByDatabaseIdOrderByPositionAsc(databaseId);
        NoteView view = views.stream()
                .filter(v -> viewId == null || v.getId().equals(viewId))
                .findFirst()
                .orElse(views.isEmpty() ? null : views.get(0));

        Map<Long, NoteProperty> properties = propertyRepository
                .findByDatabaseIdOrderByPositionAsc(databaseId).stream()
                .collect(Collectors.toMap(NoteProperty::getId, p -> p));

        // One query for all cells, then index by row.
        Map<Long, Map<Long, String>> cells = new HashMap<>();
        for (NoteValue v : valueRepository.findByDatabaseId(databaseId)) {
            cells.computeIfAbsent(v.getRow().getId(), k -> new HashMap<>())
                    .put(v.getProperty().getId(), v.getValue());
        }

        List<Note> rowNotes = noteRepository.findByParentIdOrderByPositionAsc(databaseId).stream()
                .filter(n -> !n.isArchived())
                .toList();

        List<RowResponse> rows = new ArrayList<>();
        for (Note n : rowNotes) {
            Map<Long, String> byProperty = cells.getOrDefault(n.getId(), Map.of());
            if (view != null && !passesFilters(view, properties, byProperty)) {
                continue;
            }
            RowResponse r = RowResponse.from(n);
            for (Long propertyId : properties.keySet()) {
                r.getValues().put(String.valueOf(propertyId), byProperty.get(propertyId));
            }
            rows.add(r);
        }

        if (view != null) {
            applySorts(view, properties, cells, rows);
        }

        RowsResponse out = new RowsResponse();
        out.setViewId(view == null ? null : view.getId());
        out.setTotal(rows.size());

        Long groupBy = view == null ? null : view.getGroupById();
        if (groupBy != null && properties.containsKey(groupBy)) {
            out.setGrouped(true);
            out.setGroups(group(properties.get(groupBy), rows));
        } else {
            out.setRows(rows);
        }
        return out;
    }

    @Transactional
    public RowResponse createRow(Long databaseId, RowValueRequest request) {
        noteService.requireAccess(databaseId, true);
        Note db = requireDatabase(databaseId);
        User me = OrgContext.getCurrentUser(userRepository);

        Note row = Note.builder()
                .title("Untitled")
                .parent(db)
                .rowOf(db)
                .owner(me)
                .organization(db.getOrganization())
                .position(noteRepository.findByParentIdOrderByPositionAsc(databaseId).size())
                .build();
        noteRepository.save(row);
        row.setPath((db.getPath() == null ? "/" : db.getPath()) + row.getId() + "/");
        noteRepository.save(row);

        if (request != null && request.getValues() != null) {
            writeValues(row, request.getValues());
        }
        return buildRow(row, databaseId);
    }

    @Transactional
    public RowResponse setValues(Long rowId, RowValueRequest request) {
        Note row = noteService.requireNote(rowId);
        if (row.getRowOf() == null) {
            throw new IllegalArgumentException("This note is not a database row");
        }
        noteService.requireAccess(row.getRowOf().getId(), true);
        writeValues(row, request.getValues() == null ? Map.of() : request.getValues());
        return buildRow(row, row.getRowOf().getId());
    }

    private void writeValues(Note row, Map<String, String> raw) {
        Map<Long, NoteProperty> properties = propertyRepository
                .findByDatabaseIdOrderByPositionAsc(row.getRowOf().getId()).stream()
                .collect(Collectors.toMap(NoteProperty::getId, p -> p));

        for (Map.Entry<String, String> e : raw.entrySet()) {
            Long propertyId;
            try {
                propertyId = Long.parseLong(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            NoteProperty property = properties.get(propertyId);
            if (property == null) {
                continue;
            }
            String value = NoteValueCodec.normalise(property.getType(), e.getValue());
            NoteValue cell = valueRepository.findByRowIdAndPropertyId(row.getId(), propertyId)
                    .orElseGet(() -> NoteValue.builder().row(row).property(property).build());
            if (value == null) {
                if (cell.getId() != null) {
                    valueRepository.delete(cell);
                }
                continue;
            }
            cell.setValue(value);
            valueRepository.save(cell);
        }
    }

    private RowResponse buildRow(Note row, Long databaseId) {
        RowResponse r = RowResponse.from(row);
        Map<Long, String> byProperty = valueRepository.findByRowId(row.getId()).stream()
                .collect(Collectors.toMap(v -> v.getProperty().getId(), NoteValue::getValue,
                        (a, b) -> a));
        for (NoteProperty p : propertyRepository.findByDatabaseIdOrderByPositionAsc(databaseId)) {
            r.getValues().put(String.valueOf(p.getId()), byProperty.get(p.getId()));
        }
        return r;
    }

    // --------------------------------------------------- filter / sort / group

    private boolean passesFilters(NoteView view, Map<Long, NoteProperty> properties,
                                  Map<Long, String> cells) {
        for (String clause : view.getFilters()) {
            // propertyId:operator:value — the value may itself contain ':'.
            String[] parts = clause.split(":", 3);
            if (parts.length < 2) {
                continue;
            }
            NoteProperty property;
            try {
                property = properties.get(Long.parseLong(parts[0]));
            } catch (NumberFormatException e) {
                continue;
            }
            if (property == null) {
                continue;
            }
            String operand = parts.length > 2 ? parts[2] : "";
            if (!NoteValueCodec.matches(property, cells.get(property.getId()), parts[1], operand)) {
                return false;
            }
        }
        return true;
    }

    private void applySorts(NoteView view, Map<Long, NoteProperty> properties,
                            Map<Long, Map<Long, String>> cells, List<RowResponse> rows) {
        List<String> sorts = view.getSorts();
        if (sorts.isEmpty()) {
            return;
        }
        rows.sort((a, b) -> {
            for (String clause : sorts) {
                String[] parts = clause.split(":");
                if (parts.length < 2) {
                    continue;
                }
                NoteProperty property;
                try {
                    property = properties.get(Long.parseLong(parts[0]));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (property == null) {
                    continue;
                }
                String va = cells.getOrDefault(a.getId(), Map.of()).get(property.getId());
                String vb = cells.getOrDefault(b.getId(), Map.of()).get(property.getId());
                int cmp = NoteValueCodec.compare(
                        NoteValueCodec.sortKey(property.getType(), va),
                        NoteValueCodec.sortKey(property.getType(), vb));
                if (cmp != 0) {
                    return "DESC".equalsIgnoreCase(parts[1]) ? -cmp : cmp;
                }
            }
            return 0;
        });
    }

    /** Board columns, in the order the property's options are defined. */
    private List<RowGroupResponse> group(NoteProperty property, List<RowResponse> rows) {
        LinkedHashMap<String, RowGroupResponse> groups = new LinkedHashMap<>();
        for (String option : property.getOptions()) {
            String[] parts = option.split(":", 3);
            String id = parts[0];
            groups.put(id, RowGroupResponse.of(id,
                    parts.length > 1 ? parts[1] : id,
                    parts.length > 2 ? parts[2] : "grey"));
        }
        groups.put(UNGROUPED, RowGroupResponse.of(null, "Chưa đặt", "grey"));

        String key = String.valueOf(property.getId());
        for (RowResponse row : rows) {
            String value = row.getValues().get(key);
            // MULTI_SELECT lands in the column of its first option, like Notion does.
            String bucket = value == null || value.isBlank()
                    ? UNGROUPED : NoteValueCodec.asList(value).stream().findFirst().orElse(UNGROUPED);
            groups.getOrDefault(bucket, groups.get(UNGROUPED)).getRows().add(row);
        }
        return new ArrayList<>(groups.values());
    }

    // ---------------------------------------------------------------- shared

    private Note requireDatabase(Long id) {
        Note note = noteService.requireNote(id);
        if (!note.isDatabase()) {
            throw new IllegalArgumentException("Note " + id + " is not a database");
        }
        return note;
    }

    private NoteProperty requireProperty(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + id));
    }

    private NoteView requireView(Long id) {
        return viewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("View not found: " + id));
    }

    private static String defaultName(ViewType type) {
        return switch (type) {
            case TABLE -> "Bảng";
            case BOARD -> "Board";
            case GALLERY -> "Thư viện";
            case LIST -> "Danh sách";
        };
    }
}
