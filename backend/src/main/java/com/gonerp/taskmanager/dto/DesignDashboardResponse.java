package com.gonerp.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignDashboardResponse {
    private int totalCreated;
    private int totalDrafts;
    private int totalCompleted;
    private int totalCancelled;
    private double completionRate;
    private double avgHoursToComplete;
    private int totalRejected;
    private Map<String, Integer> designsByStage;
    private List<MemberStats> memberStats;
    private List<MemberStats> ideaCreatorStats;
    private List<MemberStats> designerStats;
    private List<ProductTypeStats> productTypeStats;
    private List<NicheStats> nicheStats;
    private List<DailyTrend> dailyTrends;

    // Activity in Period metrics
    private int activityCompleted;
    private int activityCancelled;
    private double activityAvgHoursToComplete;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStats {
        private Long userId;
        private String userName;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private int created;
        private int drafts;
        private int completed;
        private double avgHoursToComplete;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductTypeStats {
        private String name;
        private int count;
        private int drafts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NicheStats {
        private String name;
        private int count;
        private int drafts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private int created;
        private int drafts;
        private int completed;
        private int activityCompleted;
    }
}
