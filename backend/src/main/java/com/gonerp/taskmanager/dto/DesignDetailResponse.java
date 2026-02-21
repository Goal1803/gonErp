package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.DesignDetail;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DesignDetailResponse {
    private Long id;
    private List<DesignFileResponse> pngFiles;
    private List<DesignFileResponse> psdFiles;
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
                .pngFiles(dd.getDesignFiles().stream()
                        .filter(f -> f.getFileCategory() == DesignFileCategory.PNG)
                        .map(DesignFileResponse::from).toList())
                .psdFiles(dd.getDesignFiles().stream()
                        .filter(f -> f.getFileCategory() == DesignFileCategory.PSD)
                        .map(DesignFileResponse::from).toList())
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
