package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.DesignDetail;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DesignDetailResponse {
    private Long id;
    private String pngFileUrl;
    private String pngFileName;
    private String psdFileUrl;
    private String psdFileName;
    private UserSummaryResponse ideaCreator;
    private List<UserSummaryResponse> designers;
    private LocalDateTime approvalDate;
    private List<LookupResponse> productTypes;
    private List<LookupResponse> niches;
    private LookupResponse occasion;
    private boolean custom;
    private String designStatus;
    private List<DesignMockupResponse> mockups;
    private String mainMockupUrl;

    public static DesignDetailResponse from(DesignDetail dd) {
        String mainMockupUrl = dd.getMockups().stream()
                .filter(m -> m.isMainMockup())
                .map(m -> m.getUrl())
                .findFirst()
                .orElse(null);

        return DesignDetailResponse.builder()
                .id(dd.getId())
                .pngFileUrl(dd.getPngFileUrl())
                .pngFileName(dd.getPngFileName())
                .psdFileUrl(dd.getPsdFileUrl())
                .psdFileName(dd.getPsdFileName())
                .ideaCreator(dd.getIdeaCreator() != null ? UserSummaryResponse.from(dd.getIdeaCreator()) : null)
                .designers(dd.getDesigners().stream().map(UserSummaryResponse::from).toList())
                .approvalDate(dd.getApprovalDate())
                .productTypes(dd.getProductTypes().stream().map(LookupResponse::from).toList())
                .niches(dd.getNiches().stream().map(LookupResponse::from).toList())
                .occasion(dd.getOccasion() != null ? LookupResponse.from(dd.getOccasion()) : null)
                .custom(dd.isCustom())
                .designStatus(dd.getDesignStatus() != null ? dd.getDesignStatus().name() : null)
                .mockups(dd.getMockups().stream().map(DesignMockupResponse::from).toList())
                .mainMockupUrl(mainMockupUrl)
                .build();
    }
}
