package com.tiki.order.service;

import com.tiki.order.dto.*;
import com.tiki.order.entity.ShippingZoneEntity;
import com.tiki.order.exception.ResourceNotFoundException;
import com.tiki.order.repository.ShippingZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingService {

    @Autowired
    private ShippingZoneRepository shippingZoneRepository;

    /**
     * Get all shipping zones
     */
    public List<ShippingZoneDTO> getAllZones() {
        return shippingZoneRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active shipping zones only
     */
    public List<ShippingZoneDTO> getActiveZones() {
        return shippingZoneRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get shipping zone by ID
     */
    public ShippingZoneDTO getZoneById(Integer id) {
        ShippingZoneEntity zone = shippingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực vận chuyển với ID: " + id));
        return convertToDTO(zone);
    }

    /**
     * Calculate shipping fee for a province
     */
    public ShippingOptionDTO calculateShipping(String province) {
        if (province == null || province.trim().isEmpty()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố là bắt buộc");
        }

        String normalizedProvince = normalizeProvinceName(province.trim());
        
        // Try to find by exact match first
        ShippingZoneEntity zone = shippingZoneRepository.findByProvince(normalizedProvince)
                .orElse(null);
        
        // If not found, try flexible matching
        if (zone == null) {
            zone = findZoneByFlexibleMatch(normalizedProvince);
        }
        
        if (zone == null) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy khu vực vận chuyển cho tỉnh: " + province
            );
        }

        return ShippingOptionDTO.fromZone(convertToDTO(zone));
    }
    
    /**
     * Normalize province name for better matching
     */
    private String normalizeProvinceName(String province) {
        if (province == null) return "";
        
        // Common variations mapping
        String normalized = province.trim();
        
        // Handle common abbreviations and variations
        if (normalized.equalsIgnoreCase("HN") || 
            normalized.equalsIgnoreCase("Ha Noi") || 
            normalized.equalsIgnoreCase("Hanoi")) {
            return "Hà Nội";
        }
        
        if (normalized.equalsIgnoreCase("HCM") || 
            normalized.equalsIgnoreCase("TPHCM") ||
            normalized.equalsIgnoreCase("Ho Chi Minh") ||
            normalized.equalsIgnoreCase("Sai Gon") ||
            normalized.equalsIgnoreCase("Saigon")) {
            return "TP.HCM";
        }
        
        if (normalized.equalsIgnoreCase("Da Nang") || 
            normalized.equalsIgnoreCase("Danang")) {
            return "Đà Nẵng";
        }
        
        if (normalized.equalsIgnoreCase("Can Tho") || 
            normalized.equalsIgnoreCase("Cantho")) {
            return "Cần Thơ";
        }
        
        if (normalized.equalsIgnoreCase("Hai Phong") || 
            normalized.equalsIgnoreCase("Haiphong")) {
            return "Hải Phòng";
        }
        
        return normalized;
    }
    
    /**
     * Find zone by flexible matching (case-insensitive, accent-insensitive)
     */
    private ShippingZoneEntity findZoneByFlexibleMatch(String province) {
        List<ShippingZoneEntity> activeZones = shippingZoneRepository.findByIsActiveTrue();
        
        String searchLower = removeAccents(province.toLowerCase());
        
        for (ShippingZoneEntity zone : activeZones) {
            for (String zoneProvince : zone.getProvinces()) {
                String zoneProvinceLower = removeAccents(zoneProvince.toLowerCase());
                if (zoneProvinceLower.equals(searchLower)) {
                    return zone;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Remove Vietnamese accents for flexible matching
     */
    private String removeAccents(String text) {
        if (text == null) return "";
        
        String result = text;
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("đ", "d");
        
        return result;
    }

    /**
     * Calculate shipping with request object
     */
    public ShippingOptionDTO calculateShipping(CalculateShippingRequest request) {
        return calculateShipping(request.getProvince());
    }

    /**
     * Get all available provinces (from all active zones)
     */
    public List<String> getAvailableProvinces() {
        return shippingZoneRepository.findByIsActiveTrue().stream()
                .flatMap(zone -> zone.getProvinces().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Create new shipping zone
     */
    @Transactional
    public ShippingZoneDTO createZone(CreateShippingZoneRequest request) {
        // Check if name already exists
        if (shippingZoneRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Tên khu vực vận chuyển đã tồn tại: " + request.getName());
        }

        ShippingZoneEntity zone = new ShippingZoneEntity();
        zone.setName(request.getName());
        zone.setProvinces(request.getProvinces());
        zone.setFee(request.getFee());
        zone.setEstimatedDays(request.getEstimatedDays());
        zone.setIsActive(request.getIsActive());

        ShippingZoneEntity savedZone = shippingZoneRepository.save(zone);
        return convertToDTO(savedZone);
    }

    /**
     * Update shipping zone
     */
    @Transactional
    public ShippingZoneDTO updateZone(Integer id, UpdateShippingZoneRequest request) {
        ShippingZoneEntity zone = shippingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực vận chuyển với ID: " + id));

        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            // Check if new name conflicts with existing zone
            if (!request.getName().equalsIgnoreCase(zone.getName()) &&
                    shippingZoneRepository.existsByNameIgnoreCase(request.getName())) {
                throw new IllegalArgumentException("Tên khu vực vận chuyển đã tồn tại: " + request.getName());
            }
            zone.setName(request.getName());
        }

        if (request.getProvinces() != null && !request.getProvinces().isEmpty()) {
            zone.setProvinces(request.getProvinces());
        }

        if (request.getFee() != null) {
            zone.setFee(request.getFee());
        }

        if (request.getEstimatedDays() != null) {
            zone.setEstimatedDays(request.getEstimatedDays());
        }

        if (request.getIsActive() != null) {
            zone.setIsActive(request.getIsActive());
        }

        ShippingZoneEntity updatedZone = shippingZoneRepository.save(zone);
        return convertToDTO(updatedZone);
    }

    /**
     * Delete shipping zone
     */
    @Transactional
    public void deleteZone(Integer id) {
        if (!shippingZoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy khu vực vận chuyển với ID: " + id);
        }
        shippingZoneRepository.deleteById(id);
    }

    /**
     * Deactivate shipping zone (soft delete)
     */
    @Transactional
    public ShippingZoneDTO deactivateZone(Integer id) {
        ShippingZoneEntity zone = shippingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực vận chuyển với ID: " + id));

        zone.setIsActive(false);
        ShippingZoneEntity updatedZone = shippingZoneRepository.save(zone);
        return convertToDTO(updatedZone);
    }

    /**
     * Get zones with fee less than or equal to max fee
     */
    public List<ShippingZoneDTO> getZonesByMaxFee(BigDecimal maxFee) {
        return shippingZoneRepository.findByMaxFee(maxFee).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a province is covered by any active zone
     */
    public boolean isProvinceCovered(String province) {
        return shippingZoneRepository.findByProvince(province).isPresent();
    }

    /**
     * Get statistics
     */
    public ShippingStatisticsDTO getStatistics() {
        long totalZones = shippingZoneRepository.count();
        long activeZones = shippingZoneRepository.countByIsActiveTrue();
        List<String> provinces = getAvailableProvinces();

        ShippingStatisticsDTO stats = new ShippingStatisticsDTO();
        stats.setTotalZones((int) totalZones);
        stats.setActiveZones((int) activeZones);
        stats.setTotalProvincesCovered(provinces.size());
        stats.setCoveredProvinces(provinces);

        return stats;
    }

    /**
     * Convert entity to DTO
     */
    private ShippingZoneDTO convertToDTO(ShippingZoneEntity zone) {
        ShippingZoneDTO dto = new ShippingZoneDTO();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        dto.setProvinces(zone.getProvinces());
        dto.setFee(zone.getFee());
        dto.setEstimatedDays(zone.getEstimatedDays());
        dto.setDisplayName(zone.getDisplayName());
        dto.setIsActive(zone.getIsActive());
        return dto;
    }
}
