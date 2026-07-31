package com.gonerp.notes.dto;

import com.gonerp.notes.model.NoteView;
import com.gonerp.notes.model.enums.ViewType;
import lombok.Data;

import java.util.List;

@Data
public class ViewResponse {

    private Long id;
    private String name;
    private ViewType type;
    private int position;
    private Long groupById;
    private List<String> filters;
    private List<String> sorts;
    private List<Long> visiblePropertyIds;

    public static ViewResponse from(NoteView v) {
        ViewResponse r = new ViewResponse();
        r.id = v.getId();
        r.name = v.getName();
        r.type = v.getType();
        r.position = v.getPosition();
        r.groupById = v.getGroupById();
        r.filters = v.getFilters();
        r.sorts = v.getSorts();
        r.visiblePropertyIds = v.getVisiblePropertyIds();
        return r;
    }
}
