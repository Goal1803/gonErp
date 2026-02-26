package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.BreakEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakEntryResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;

    public static BreakEntryResponse from(BreakEntry b) {
        return BreakEntryResponse.builder()
                .id(b.getId())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .durationMinutes(b.getDurationMinutes())
                .build();
    }
}
