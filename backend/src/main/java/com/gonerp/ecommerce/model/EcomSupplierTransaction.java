package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ecom_supplier_transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"supplier_id", "supplier_order_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomSupplierTransaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private EcomSupplier supplier;

    @Column(name = "supplier_order_id", nullable = false, length = 100)
    private String supplierOrderId;

    @Column(name = "external_number", length = 100)
    private String externalNumber;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency;

    private int quantity;

    @Column(length = 50)
    private String status;

    @Column(name = "full_name", length = 500)
    private String fullName;

    @Column(length = 100)
    private String country;

    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(length = 200)
    private String city;

    @Column(name = "state_region", length = 200)
    private String stateRegion;

    @Column(name = "postal_code", length = 50)
    private String postalCode;

    @Column(name = "phone_number", length = 100)
    private String phoneNumber;

    @Column(name = "ship_method", length = 200)
    private String shipMethod;

    @Column(name = "tracking_id", length = 200)
    private String trackingId;

    @Column(name = "ship_date")
    private LocalDateTime shipDate;

    @Column(length = 200)
    private String sku;

    @Column(length = 200)
    private String size;

    // Link to matched ecom order (null if not matched)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_order_id")
    private EcomOrder matchedOrder;

    @Column(name = "matched", nullable = false)
    @Builder.Default
    private boolean matched = false;
}
