package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.LookupRequest;
import com.gonerp.taskmanager.dto.LookupResponse;
import com.gonerp.taskmanager.model.Niche;
import com.gonerp.taskmanager.model.Occasion;
import com.gonerp.taskmanager.model.ProductType;
import com.gonerp.taskmanager.repository.NicheRepository;
import com.gonerp.taskmanager.repository.OccasionRepository;
import com.gonerp.taskmanager.repository.ProductTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LookupService {

    private final ProductTypeRepository productTypeRepository;
    private final NicheRepository nicheRepository;
    private final OccasionRepository occasionRepository;

    // --- Product Types ---

    public List<LookupResponse> findAllProductTypes() {
        return productTypeRepository.findAll().stream().map(LookupResponse::from).toList();
    }

    public LookupResponse createProductType(LookupRequest request) {
        ProductType pt = ProductType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return LookupResponse.from(productTypeRepository.save(pt));
    }

    public LookupResponse updateProductType(Long id, LookupRequest request) {
        ProductType pt = productTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product type not found: " + id));
        pt.setName(request.getName());
        pt.setDescription(request.getDescription());
        return LookupResponse.from(productTypeRepository.save(pt));
    }

    public void deleteProductType(Long id) {
        if (!productTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("Product type not found: " + id);
        }
        productTypeRepository.deleteById(id);
    }

    // --- Niches ---

    public List<LookupResponse> findAllNiches() {
        return nicheRepository.findAll().stream().map(LookupResponse::from).toList();
    }

    public LookupResponse createNiche(LookupRequest request) {
        Niche n = Niche.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return LookupResponse.from(nicheRepository.save(n));
    }

    public LookupResponse updateNiche(Long id, LookupRequest request) {
        Niche n = nicheRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Niche not found: " + id));
        n.setName(request.getName());
        n.setDescription(request.getDescription());
        return LookupResponse.from(nicheRepository.save(n));
    }

    public void deleteNiche(Long id) {
        if (!nicheRepository.existsById(id)) {
            throw new EntityNotFoundException("Niche not found: " + id);
        }
        nicheRepository.deleteById(id);
    }

    // --- Occasions ---

    public List<LookupResponse> findAllOccasions() {
        return occasionRepository.findAll().stream().map(LookupResponse::from).toList();
    }

    public LookupResponse createOccasion(LookupRequest request) {
        Occasion o = Occasion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return LookupResponse.from(occasionRepository.save(o));
    }

    public LookupResponse updateOccasion(Long id, LookupRequest request) {
        Occasion o = occasionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Occasion not found: " + id));
        o.setName(request.getName());
        o.setDescription(request.getDescription());
        return LookupResponse.from(occasionRepository.save(o));
    }

    public void deleteOccasion(Long id) {
        if (!occasionRepository.existsById(id)) {
            throw new EntityNotFoundException("Occasion not found: " + id);
        }
        occasionRepository.deleteById(id);
    }
}
