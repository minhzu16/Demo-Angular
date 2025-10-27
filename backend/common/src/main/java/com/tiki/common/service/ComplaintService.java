package com.tiki.common.service;

import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.entity.ComplaintEntity;
import com.tiki.common.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepo;

    private ComplaintDto toDto(ComplaintEntity e){
        ComplaintDto d = new ComplaintDto();
        d.setId(e.getId());
        d.setOrderId(e.getOrderId());
        d.setBuyerId(e.getBuyerId());
        d.setSellerId(e.getSellerId());
        d.setTitle(e.getTitle());
        d.setDescription(e.getDescription());
        d.setStatus(e.getStatus().name());
        d.setResolution(e.getResolution());
        d.setCreatedAt(e.getCreatedAt());
        d.setResolvedAt(e.getResolvedAt());
        return d;
    }

    public ComplaintDto create(ComplaintDto dto){
        ComplaintEntity e = new ComplaintEntity();
        e.setOrderId(dto.getOrderId());
        e.setBuyerId(dto.getBuyerId());
        e.setSellerId(dto.getSellerId());
        e.setTitle(dto.getTitle());
        e.setDescription(dto.getDescription());
        return toDto(complaintRepo.save(e));
    }

    public List<ComplaintDto> getAll(){
        return complaintRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<ComplaintDto> getById(Long id){
        return complaintRepo.findById(id).map(this::toDto);
    }

    public List<ComplaintDto> getByBuyer(Long buyerId){
        return complaintRepo.findByBuyerId(buyerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ComplaintDto> getBySeller(Long sellerId){
        return complaintRepo.findBySellerId(sellerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<ComplaintDto> resolve(Long id, String resolution, boolean accepted){
        return complaintRepo.findById(id).map(e -> {
            e.setResolution(resolution);
            e.setStatus(accepted ? ComplaintEntity.Status.RESOLVED : ComplaintEntity.Status.REJECTED);
            e.setResolvedAt(LocalDateTime.now());
            return toDto(complaintRepo.save(e));
        });
    }
}
