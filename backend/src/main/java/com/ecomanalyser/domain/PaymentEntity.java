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

    @Column(name = "order_id", nullable = false)
    private String orderId;

    // Denormalized fields for faster analytics joins
    @Column(name = "sku")
    private String sku;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date_time", nullable = false)
    private LocalDateTime paymentDateTime;

    @Column(name = "order_date_time")
    private LocalDateTime orderDateTime;
    
    @Column(name = "order_status", nullable = false)
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

    @Column
    private BigDecimal waivers;

    @Column(name = "net_other_support_service_charges")
    private BigDecimal netOtherSupportServiceCharges;

    @Column(name = "gst_on_net_other_support_service_charges")
    private BigDecimal gstOnNetOtherSupportServiceCharges;

    @Column
    private BigDecimal tcs;

    @Column(name = "tds_rate_percentage")
    private BigDecimal tdsRatePercentage;

    @Column
    private BigDecimal tds;

    @Column
    private BigDecimal compensation;

    @Column
    private BigDecimal claims;

    @Column
    private BigDecimal recovery;

    @Column(name = "compensation_reason")
    private String compensationReason;

    @Column(name = "claims_reason")
    private String claimsReason;

    @Column(name = "recovery_reason")
    private String recoveryReason;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "product_gst_percentage")
    private BigDecimal productGstPercentage;

    @Column(name = "listing_price_incl_taxes")
    private BigDecimal listingPriceInclTaxes;
}


