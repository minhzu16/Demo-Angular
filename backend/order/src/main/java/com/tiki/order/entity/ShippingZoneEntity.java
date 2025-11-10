package com.tiki.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shipping_zones")
public class ShippingZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provinces", nullable = false, columnDefinition = "JSON")
    private List<String> provinces;

    @Column(name = "fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal fee;

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Check if this zone covers a province
     */
    public boolean coversProvince(String province) {
        if (provinces == null || province == null) {
            return false;
        }
        return provinces.stream()
                .anyMatch(p -> p.equalsIgnoreCase(province.trim()));
    }

    /**
     * Get display name with estimated days
     */
    public String getDisplayName() {
        if (estimatedDays != null && estimatedDays > 0) {
            return String.format("%s (%d-%d ngày)", name, estimatedDays, estimatedDays + 1);
        }
        return name;
    }

    /**
     * Check if zone is available
     */
    public boolean isAvailable() {
        return isActive != null && isActive;
    }
}
