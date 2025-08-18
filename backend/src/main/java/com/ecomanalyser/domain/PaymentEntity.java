package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    // sub_order_no – VARCHAR / BIGINT
    @Column(name = "sub_order_no", nullable = false)
    private String orderId;

    // supplier_sku – VARCHAR
    @Column(name = "supplier_sku")
    private String sku;

    // final_settlement_amount – DECIMAL(12,2)
    @Column(name = "final_settlement_amount", precision = 12, scale = 2)
    private BigDecimal amount;

    // payment_date – DATE / TIMESTAMP
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDateTime;

    // order_date – DATE / TIMESTAMP
    @Column(name = "order_date")
    private LocalDateTime orderDateTime;

    // live_order_status – VARCHAR
    @Column(name = "live_order_status", nullable = false)
    private String orderStatus;

    // quantity – INT
    @Column(name = "quantity")
    private Integer quantity;

    // product_name – TEXT
    @Column(name = "product_name", columnDefinition = "TEXT")
    private String productName;

    @Column(name = "transaction_id")
    private String transactionId;

    // price_type – VARCHAR
    @Column(name = "price_type")
    private String priceType;

    // total_sale_amount_incl_shipping_gst – DECIMAL(12,2)
    @Column(name = "total_sale_amount_incl_shipping_gst", precision = 12, scale = 2)
    private BigDecimal totalSaleAmount;

    // total_sale_return_amount_incl_shipping_gst – DECIMAL(12,2)
    @Column(name = "total_sale_return_amount_incl_shipping_gst", precision = 12, scale = 2)
    private BigDecimal totalSaleReturnAmount;

    // fixed_fee_incl_gst – DECIMAL(12,2)
    @Column(name = "fixed_fee_incl_gst", precision = 12, scale = 2)
    private BigDecimal fixedFee;

    // warehousing_fee_incl_gst – DECIMAL(12,2)
    @Column(name = "warehousing_fee_incl_gst", precision = 12, scale = 2)
    private BigDecimal warehousingFee;

    // return_premium_incl_gst – DECIMAL(12,2)
    @Column(name = "return_premium_incl_gst", precision = 12, scale = 2)
    private BigDecimal returnPremium;

    // return_premium_incl_gst_of_return – DECIMAL(12,2)
    @Column(name = "return_premium_incl_gst_of_return", precision = 12, scale = 2)
    private BigDecimal returnPremiumOfReturn;

    // meesho_commission_percent – DECIMAL(5,2)
    @Column(name = "meesho_commission_percent", precision = 5, scale = 2)
    private BigDecimal meeshoCommissionPercentage;

    // meesho_commission_incl_gst – DECIMAL(12,2)
    @Column(name = "meesho_commission_incl_gst", precision = 12, scale = 2)
    private BigDecimal meeshoCommission;

    // meesho_gold_platform_fee_incl_gst – DECIMAL(12,2)
    @Column(name = "meesho_gold_platform_fee_incl_gst", precision = 12, scale = 2)
    private BigDecimal meeshoGoldPlatformFee;

    // meesho_mall_platform_fee_incl_gst – DECIMAL(12,2)
    @Column(name = "meesho_mall_platform_fee_incl_gst", precision = 12, scale = 2)
    private BigDecimal meeshoMallPlatformFee;

    // fixed_fee_incl_gst_2 – DECIMAL(12,2)
    @Column(name = "fixed_fee_incl_gst_2", precision = 12, scale = 2)
    private BigDecimal fixedFee2;

    // warehousing_fee_incl_gst_2 – DECIMAL(12,2)
    @Column(name = "warehousing_fee_incl_gst_2", precision = 12, scale = 2)
    private BigDecimal warehousingFee2;

    // return_shipping_charge_incl_gst – DECIMAL(12,2)
    @Column(name = "return_shipping_charge_incl_gst", precision = 12, scale = 2)
    private BigDecimal returnShippingCharge;

    // gst_compensation_prp_shipping – DECIMAL(12,2)
    @Column(name = "gst_compensation_prp_shipping", precision = 12, scale = 2)
    private BigDecimal gstCompensation;

    // shipping_charge_incl_gst – DECIMAL(12,2)
    @Column(name = "shipping_charge_incl_gst", precision = 12, scale = 2)
    private BigDecimal shippingCharge;

    // other_support_service_charges_excl_gst – DECIMAL(12,2)
    @Column(name = "other_support_service_charges_excl_gst", precision = 12, scale = 2)
    private BigDecimal otherSupportServiceCharges;

    // waivers_excl_gst – DECIMAL(12,2)
    @Column(name = "waivers_excl_gst", precision = 12, scale = 2)
    private BigDecimal waivers;

    // net_other_support_service_charges_excl_gst – DECIMAL(12,2)
    @Column(name = "net_other_support_service_charges_excl_gst", precision = 12, scale = 2)
    private BigDecimal netOtherSupportServiceCharges;

    @Column(name = "gst_on_net_other_support_service_charges", precision = 12, scale = 2)
    private BigDecimal gstOnNetOtherSupportServiceCharges;

    @Column(name = "tcs", precision = 12, scale = 2)
    private BigDecimal tcs;

    @Column(name = "tds_rate_percent", precision = 5, scale = 2)
    private BigDecimal tdsRatePercentage;

    @Column(name = "tds", precision = 12, scale = 2)
    private BigDecimal tds;

    @Column(name = "compensation", precision = 12, scale = 2)
    private BigDecimal compensation;

    @Column(name = "claims", precision = 12, scale = 2)
    private BigDecimal claims;

    @Column(name = "recovery", precision = 12, scale = 2)
    private BigDecimal recovery;

    @Column(name = "compensation_reason", columnDefinition = "TEXT")
    private String compensationReason;

    @Column(name = "claims_reason", columnDefinition = "TEXT")
    private String claimsReason;

    @Column(name = "recovery_reason", columnDefinition = "TEXT")
    private String recoveryReason;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "product_gst_percent", precision = 5, scale = 2)
    private BigDecimal productGstPercentage;

    @Column(name = "listing_price_incl_taxes", precision = 12, scale = 2)
    private BigDecimal listingPriceInclTaxes;
}


