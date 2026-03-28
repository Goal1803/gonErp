package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomStoreRequest;
import com.gonerp.ecommerce.dto.EcomStoreResponse;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.enums.SalesChannel;
import com.gonerp.ecommerce.repository.EcomStoreRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EcomStoreService {

    private final EcomStoreRepository ecomStoreRepository;
    private final EcomAccessService ecomAccessService;

    public List<EcomStoreResponse> findAll() {
        ecomAccessService.requireEcommerceAccess();
        Organization org = ecomAccessService.resolveOrganization();
        return ecomStoreRepository.findByOrganizationIdOrderByNameAsc(org.getId()).stream()
                .map(EcomStoreResponse::from)
                .toList();
    }

    public EcomStoreResponse findById(Long id) {
        ecomAccessService.requireEcommerceAccess();
        EcomStore store = getStoreInOrg(id);
        return EcomStoreResponse.from(store);
    }

    public EcomStoreResponse create(EcomStoreRequest request) {
        ecomAccessService.requireEcommerceAccess();
        Organization org = ecomAccessService.resolveOrganization();

        EcomStore store = EcomStore.builder()
                .organization(org)
                .name(request.getName())
                .salesChannel(request.getSalesChannel() != null ? SalesChannel.valueOf(request.getSalesChannel()) : null)
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .storeUrl(request.getStoreUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return EcomStoreResponse.from(ecomStoreRepository.save(store));
    }

    public EcomStoreResponse update(Long id, EcomStoreRequest request) {
        ecomAccessService.requireEcommerceAccess();
        EcomStore store = getStoreInOrg(id);

        store.setName(request.getName());
        if (request.getSalesChannel() != null) store.setSalesChannel(SalesChannel.valueOf(request.getSalesChannel()));
        if (request.getCurrency() != null) store.setCurrency(request.getCurrency());
        store.setStoreUrl(request.getStoreUrl());
        if (request.getActive() != null) store.setActive(request.getActive());

        return EcomStoreResponse.from(ecomStoreRepository.save(store));
    }

    public void delete(Long id) {
        ecomAccessService.requireEcommerceAccess();
        EcomStore store = getStoreInOrg(id);
        ecomStoreRepository.delete(store);
    }

    private EcomStore getStoreInOrg(Long id) {
        Organization org = ecomAccessService.resolveOrganization();
        EcomStore store = ecomStoreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + id));
        if (!store.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return store;
    }
}
