package com.gonerp.finance.dto;

import lombok.Data;

@Data
public class TransactionUpdateRequest {
    private String category;
    private String subcategory;
    private String note;
}
