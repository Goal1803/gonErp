package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "ecom_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomOrderItem extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private EcomOrder order;

    @Column(name = "platform_item_id", length = 100)
    private String platformItemId;

    @Column(name = "product_name", columnDefinition = "TEXT")
    private String productName;

    @Column(name = "sku", length = 200)
    private String sku;

    @Column(name = "listing_id", length = 100)
    private String listingId;

    @Column(name = "variations", columnDefinition = "TEXT")
    private String variations;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(name = "item_price", precision = 12, scale = 2)
    private BigDecimal itemPrice;

    @Column(name = "item_total", precision = 12, scale = 2)
    private BigDecimal itemTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, String> rawData;
}
