package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merged_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergedOrderPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "payment_id")
    private String paymentId;

    // Order fields
    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "order_date_time")
    private LocalDateTime orderDateTime;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "customer_state")
    private String customerState;

    @Column(name = "size")
    private String size;

    @Column(name = "supplier_listed_price")
    private BigDecimal supplierListedPrice;

    @Column(name = "supplier_discounted_price")
    private BigDecimal supplierDiscountedPrice;

    @Column(name = "packet_id")
    private String packetId;

    @Column(name = "reason_for_credit_entry")
    private String reasonForCreditEntry;

    // Payment fields
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "payment_date_time")
    private LocalDateTime paymentDateTime;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "final_settlement_amount")
    private BigDecimal finalSettlementAmount;

    @Column(name = "price_type")
    private String priceType;

    @Column(name = "total_sale_amount")
    private BigDecimal totalSaleAmount;

    @Column(name = "total_sale_return_amount")
    private BigDecimal totalSaleReturnAmount;

    @Column(name = "fixed_fee")
    private BigDecimal fixedFee;

    @Column(name = "warehousing_fee")
    private BigDecimal warehousingFee;

    @Column(name = "return_premium")
    private BigDecimal returnPremium;

    @Column(name = "meesho_commission_percentage")
    private BigDecimal meeshoCommissionPercentage;

    @Column(name = "meesho_commission")
    private BigDecimal meeshoCommission;

    @Column(name = "meesho_gold_platform_fee")
    private BigDecimal meeshoGoldPlatformFee;

    @Column(name = "meesho_mall_platform_fee")
    private BigDecimal meeshoMallPlatformFee;

    @Column(name = "return_shipping_charge")
    private BigDecimal returnShippingCharge;

    @Column(name = "gst_compensation")
    private BigDecimal gstCompensation;

    @Column(name = "shipping_charge")
    private BigDecimal shippingCharge;

    @Column(name = "other_support_service_charges")
    private BigDecimal otherSupportServiceCharges;

    @Column(name = "waivers")
    private BigDecimal waivers;

    @Column(name = "net_other_support_service_charges")
    private BigDecimal netOtherSupportServiceCharges;

    @Column(name = "gst_on_net_other_support_service_charges")
    private BigDecimal gstOnNetOtherSupportServiceCharges;

    @Column(name = "tcs")
    private BigDecimal tcs;

    @Column(name = "tds_rate_percentage")
    private BigDecimal tdsRatePercentage;

    @Column(name = "tds")
    private BigDecimal tds;

    @Column(name = "compensation")
    private BigDecimal compensation;

    @Column(name = "compensation_reason")
    private String compensationReason;

    @Column(name = "claims")
    private BigDecimal claims;

    @Column(name = "claims_reason")
    private String claimsReason;

    @Column(name = "recovery")
    private BigDecimal recovery;

    @Column(name = "recovery_reason")
    private String recoveryReason;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "product_gst_percentage")
    private BigDecimal productGstPercentage;

    @Column(name = "listing_price_incl_taxes")
    private BigDecimal listingPriceInclTaxes;

    // Derived
    @Column(name = "final_status")
    private String finalStatus;
}


