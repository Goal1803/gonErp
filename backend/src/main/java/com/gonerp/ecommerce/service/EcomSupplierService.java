package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomSupplierRequest;
import com.gonerp.ecommerce.dto.EcomSupplierResponse;
import com.gonerp.ecommerce.model.EcomSupplier;
import com.gonerp.ecommerce.repository.EcomSupplierRepository;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EcomSupplierService {

    private final EcomSupplierRepository supplierRepository;
    private final EcomAccessService ecomAccessService;

    public List<EcomSupplierResponse> findAll() {
        ecomAccessService.requireEcommerceAccess();
        Organization org = ecomAccessService.resolveOrganization();
        return supplierRepository.findByOrganizationIdOrderByNameAsc(org.getId()).stream()
                .map(EcomSupplierResponse::from).toList();
    }

    public EcomSupplierResponse findById(Long id) {
        ecomAccessService.requireEcommerceAccess();
        return EcomSupplierResponse.from(getInOrg(id));
    }

    public EcomSupplierResponse create(EcomSupplierRequest request) {
        ecomAccessService.requireEcommerceAccess();
        Organization org = ecomAccessService.resolveOrganization();
        EcomSupplier supplier = EcomSupplier.builder()
                .organization(org)
                .name(request.getName())
                .description(request.getDescription())
                .countries(request.getCountries())
                .website(request.getWebsite())
                .priceListUrl(request.getPriceListUrl())
                .build();
        return EcomSupplierResponse.from(supplierRepository.save(supplier));
    }

    public EcomSupplierResponse update(Long id, EcomSupplierRequest request) {
        ecomAccessService.requireEcommerceAccess();
        EcomSupplier supplier = getInOrg(id);
        if (request.getName() != null) supplier.setName(request.getName());
        if (request.getDescription() != null) supplier.setDescription(request.getDescription());
        if (request.getCountries() != null) supplier.setCountries(request.getCountries());
        if (request.getWebsite() != null) supplier.setWebsite(request.getWebsite());
        if (request.getPriceListUrl() != null) supplier.setPriceListUrl(request.getPriceListUrl());
        if (request.getActive() != null) supplier.setActive(request.getActive());
        return EcomSupplierResponse.from(supplierRepository.save(supplier));
    }

    public void delete(Long id) {
        ecomAccessService.requireEcommerceAccess();
        supplierRepository.delete(getInOrg(id));
    }

    private EcomSupplier getInOrg(Long id) {
        Organization org = ecomAccessService.resolveOrganization();
        EcomSupplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
        if (!supplier.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return supplier;
    }
}
