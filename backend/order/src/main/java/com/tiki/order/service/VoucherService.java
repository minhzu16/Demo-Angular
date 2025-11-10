package com.tiki.order.service;

import com.tiki.order.dto.*;
import com.tiki.order.entity.VoucherEntity;
import com.tiki.order.exception.ResourceNotFoundException;
import com.tiki.order.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    /**
     * Get all vouchers
     */
    public List<VoucherDTO> getAllVouchers() {
        return voucherRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active vouchers only
     */
    public List<VoucherDTO> getActiveVouchers() {
        return voucherRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get currently valid vouchers (active, not expired, has usage left)
     */
    public List<VoucherDTO> getValidVouchers() {
        return voucherRepository.findActiveAndValidVouchers(LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get voucher by ID
     */
    public VoucherDTO getVoucherById(Integer id) {
        VoucherEntity voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));
        return convertToDTO(voucher);
    }

    /**
     * Get voucher by code
     */
    public VoucherDTO getVoucherByCode(String code) {
        VoucherEntity voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã: " + code));
        return convertToDTO(voucher);
    }

    /**
     * Validate voucher and calculate discount
     */
    public VoucherValidationResponse validateVoucher(ValidateVoucherRequest request) {
        // Find voucher
        VoucherEntity voucher = voucherRepository.findByCodeIgnoreCase(request.getCode())
                .orElse(null);

        if (voucher == null) {
            return VoucherValidationResponse.error("Voucher code not found");
        }

        // Check if active
        if (!voucher.getIsActive()) {
            return VoucherValidationResponse.error("Voucher is inactive");
        }

        // Check if started
        if (!voucher.hasStarted()) {
            return VoucherValidationResponse.error("Voucher has not started yet");
        }

        // Check if expired
        if (voucher.isExpired()) {
            return VoucherValidationResponse.error("Voucher has expired");
        }

        // Check max usage
        if (voucher.hasReachedMaxUsage()) {
            return VoucherValidationResponse.error("Voucher has reached maximum usage");
        }

        // Check min order value
        if (request.getOrderTotal().compareTo(voucher.getMinOrderValue()) < 0) {
            return VoucherValidationResponse.error(
                    String.format("Minimum order value is %s", voucher.getMinOrderValue())
            );
        }

        // Calculate discount
        BigDecimal discountAmount = voucher.calculateDiscount(request.getOrderTotal());

        return VoucherValidationResponse.success(discountAmount, convertToDTO(voucher));
    }

    /**
     * Create new voucher
     */
    @Transactional
    public VoucherDTO createVoucher(CreateVoucherRequest request) {
        // Check if code already exists
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại: " + request.getCode());
        }

        // Validate percentage value
        if (request.getType() == VoucherEntity.DiscountType.PERCENTAGE) {
            if (request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Giá trị phần trăm không được vượt quá 100");
            }
        }

        // Create entity
        VoucherEntity voucher = new VoucherEntity();
        voucher.setCode(request.getCode().toUpperCase());
        voucher.setType(request.getType());
        voucher.setValue(request.getValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setMaxUsage(request.getMaxUsage());
        voucher.setIsActive(request.getIsActive());

        VoucherEntity savedVoucher = voucherRepository.save(voucher);
        return convertToDTO(savedVoucher);
    }

    /**
     * Update voucher
     */
    @Transactional
    public VoucherDTO updateVoucher(Integer id, UpdateVoucherRequest request) {
        VoucherEntity voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        // Update fields if provided
        if (request.getValue() != null) {
            // Validate percentage
            if (voucher.getType() == VoucherEntity.DiscountType.PERCENTAGE) {
                if (request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Giá trị phần trăm không được vượt quá 100");
                }
            }
            voucher.setValue(request.getValue());
        }

        if (request.getMinOrderValue() != null) {
            voucher.setMinOrderValue(request.getMinOrderValue());
        }

        if (request.getEndDate() != null) {
            voucher.setEndDate(request.getEndDate());
        }

        if (request.getMaxUsage() != null) {
            voucher.setMaxUsage(request.getMaxUsage());
        }

        if (request.getIsActive() != null) {
            voucher.setIsActive(request.getIsActive());
        }

        VoucherEntity updatedVoucher = voucherRepository.save(voucher);
        return convertToDTO(updatedVoucher);
    }

    /**
     * Delete voucher
     */
    @Transactional
    public void deleteVoucher(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id);
        }
        voucherRepository.deleteById(id);
    }

    /**
     * Deactivate voucher (soft delete)
     */
    @Transactional
    public VoucherDTO deactivateVoucher(Integer id) {
        VoucherEntity voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        voucher.setIsActive(false);
        VoucherEntity updatedVoucher = voucherRepository.save(voucher);
        return convertToDTO(updatedVoucher);
    }

    /**
     * Apply voucher (increment used count)
     */
    @Transactional
    public void applyVoucher(String code) {
        VoucherEntity voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + code));

        voucher.incrementUsedCount();
        voucherRepository.save(voucher);
    }

    /**
     * Get vouchers expiring soon
     */
    public List<VoucherDTO> getVouchersExpiringSoon(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(days);

        return voucherRepository.findVouchersExpiringBetween(now, expiryDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get vouchers with low remaining usage
     */
    public List<VoucherDTO> getVouchersWithLowUsage(int threshold) {
        return voucherRepository.findVouchersWithLowUsage(threshold, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search vouchers by code pattern
     */
    public List<VoucherDTO> searchVouchers(String pattern) {
        return voucherRepository.searchByCodePattern(pattern).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private VoucherDTO convertToDTO(VoucherEntity voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(voucher.getId());
        dto.setCode(voucher.getCode());
        dto.setType(voucher.getType());
        dto.setValue(voucher.getValue());
        dto.setMinOrderValue(voucher.getMinOrderValue());
        dto.setStartDate(voucher.getStartDate());
        dto.setEndDate(voucher.getEndDate());
        dto.setMaxUsage(voucher.getMaxUsage());
        dto.setUsedCount(voucher.getUsedCount());
        dto.setRemainingUsage(voucher.getMaxUsage() - voucher.getUsedCount());
        dto.setIsActive(voucher.getIsActive());
        dto.setIsExpired(voucher.isExpired());
        dto.setHasStarted(voucher.hasStarted());
        dto.setCreatedAt(voucher.getCreatedAt());
        return dto;
    }
}
