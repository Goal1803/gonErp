package com.gonerp.ecommerce.parser;

import com.gonerp.ecommerce.model.EcomSupplier;
import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Parser interface for supplier transaction files.
 * Each supplier has its own file format and parser implementation.
 */
public interface SupplierFileParser {

    /**
     * @return The supplier name this parser handles (matched case-insensitively)
     */
    String getSupplierName();

    /**
     * Parse the uploaded file into a list of transaction entities (not yet persisted).
     */
    List<EcomSupplierTransaction> parse(EcomSupplier supplier, MultipartFile file) throws Exception;
}
