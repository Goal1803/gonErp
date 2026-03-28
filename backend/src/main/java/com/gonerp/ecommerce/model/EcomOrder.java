package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.model.enums.SalesChannel;
import com.gonerp.ecommerce.model.enums.TrackingStatus;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.model.Card;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "ecom_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomOrder extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private EcomStore store;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", length = 50)
    private SalesChannel salesChannel;

    @Column(name = "platform_order_id", length = 100)
    private String platformOrderId;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "number_of_items")
    @Builder.Default
    private int numberOfItems = 1;

    // === Customer & Shipping ===
    @Column(name = "customer_name", length = 500)
    private String customerName;

    @Column(name = "buyer_user_id", length = 200)
    private String buyerUserId;

    @Column(name = "customer_email", length = 500)
    private String customerEmail;

    @Column(name = "customer_phone", length = 100)
    private String customerPhone;

    @Column(name = "ship_street1", length = 500)
    private String shipStreet1;

    @Column(name = "ship_street2", length = 500)
    private String shipStreet2;

    @Column(name = "ship_city", length = 200)
    private String shipCity;

    @Column(name = "ship_state", length = 200)
    private String shipState;

    @Column(name = "ship_zipcode", length = 50)
    private String shipZipcode;

    @Column(name = "ship_country", length = 100)
    private String shipCountry;

    // === Financials (order-level) ===
    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "order_value", precision = 12, scale = 2)
    private BigDecimal orderValue;

    @Column(name = "shipping_price", precision = 12, scale = 2)
    private BigDecimal shippingPrice;

    @Column(name = "tax", precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(name = "discount", precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "processing_fees", precision = 12, scale = 2)
    private BigDecimal processingFees;

    @Column(name = "order_total", precision = 12, scale = 2)
    private BigDecimal orderTotal;

    @Column(name = "order_net", precision = 12, scale = 2)
    private BigDecimal orderNet;

    // === Costs & Profit (manually entered) ===
    @Column(name = "fulfillment_cost", precision = 12, scale = 2)
    private BigDecimal fulfillmentCost;

    @Column(name = "other_cost", precision = 12, scale = 2)
    private BigDecimal otherCost;

    @Column(name = "gross_profit", precision = 12, scale = 2)
    private BigDecimal grossProfit;

    // === Status & Fulfillment ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW_ORDER;

    @Column(name = "tracking_number", length = 200)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_status", length = 50)
    private TrackingStatus trackingStatus;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "refunded", nullable = false)
    @Builder.Default
    private boolean refunded = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sku", length = 500)
    private String sku;

    // === Items ===
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @Builder.Default
    private List<EcomOrderItem> items = new ArrayList<>();

    // === Raw data ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, String> rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "platform_data", columnDefinition = "jsonb")
    private Map<String, Object> platformData;

    // === Board link ===
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;
}
