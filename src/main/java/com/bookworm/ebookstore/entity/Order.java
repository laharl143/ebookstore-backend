package com.bookworm.ebookstore.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A placed order. Prices, titles and the shipping address are snapshots taken at checkout and never change.
 * Totals: {@code totalAmount = subtotal + vatAmount + deliveryCharge - giftPointsAmount} (a CHECK enforces it).
 * Status changes only through guarded single statement updates.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(nullable = false, length = 100)
    private String shipFirstName;

    @Column(nullable = false, length = 100)
    private String shipLastName;

    @Column(nullable = false, length = 255)
    private String shipEmail;

    @Column(nullable = false, length = 20)
    private String shipPhone;

    @Column(nullable = false, length = 255)
    private String shipStreetAddress;

    @Column(length = 100)
    private String shipBarangay;

    @Column(nullable = false, length = 100)
    private String shipCity;

    @Column(nullable = false, length = 100)
    private String shipProvince;

    @Column(nullable = false, length = 4)
    private String shipZipCode;

    @Column(nullable = false, length = 60)
    private String shipCountry = "Philippines";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(nullable = false)
    private int giftPointsRedeemed = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal giftPointsAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private int giftPointsEarned = 0;

    @Column(nullable = false)
    private LocalDate estimatedDeliveryDate;

    @Column(nullable = false)
    private OffsetDateTime placedAt;

    private OffsetDateTime paidAt;

    private OffsetDateTime shippedAt;

    private OffsetDateTime deliveredAt;

    private OffsetDateTime cancelledAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items = new ArrayList<>();

    /** Adds a line and links it back to this order, so both sides stay in step. */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getShipFirstName() {
        return shipFirstName;
    }

    public void setShipFirstName(String shipFirstName) {
        this.shipFirstName = shipFirstName;
    }

    public String getShipLastName() {
        return shipLastName;
    }

    public void setShipLastName(String shipLastName) {
        this.shipLastName = shipLastName;
    }

    public String getShipEmail() {
        return shipEmail;
    }

    public void setShipEmail(String shipEmail) {
        this.shipEmail = shipEmail;
    }

    public String getShipPhone() {
        return shipPhone;
    }

    public void setShipPhone(String shipPhone) {
        this.shipPhone = shipPhone;
    }

    public String getShipStreetAddress() {
        return shipStreetAddress;
    }

    public void setShipStreetAddress(String shipStreetAddress) {
        this.shipStreetAddress = shipStreetAddress;
    }

    public String getShipBarangay() {
        return shipBarangay;
    }

    public void setShipBarangay(String shipBarangay) {
        this.shipBarangay = shipBarangay;
    }

    public String getShipCity() {
        return shipCity;
    }

    public void setShipCity(String shipCity) {
        this.shipCity = shipCity;
    }

    public String getShipProvince() {
        return shipProvince;
    }

    public void setShipProvince(String shipProvince) {
        this.shipProvince = shipProvince;
    }

    public String getShipZipCode() {
        return shipZipCode;
    }

    public void setShipZipCode(String shipZipCode) {
        this.shipZipCode = shipZipCode;
    }

    public String getShipCountry() {
        return shipCountry;
    }

    public void setShipCountry(String shipCountry) {
        this.shipCountry = shipCountry;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public BigDecimal getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(BigDecimal deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public int getGiftPointsRedeemed() {
        return giftPointsRedeemed;
    }

    public void setGiftPointsRedeemed(int giftPointsRedeemed) {
        this.giftPointsRedeemed = giftPointsRedeemed;
    }

    public BigDecimal getGiftPointsAmount() {
        return giftPointsAmount;
    }

    public void setGiftPointsAmount(BigDecimal giftPointsAmount) {
        this.giftPointsAmount = giftPointsAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getGiftPointsEarned() {
        return giftPointsEarned;
    }

    public void setGiftPointsEarned(int giftPointsEarned) {
        this.giftPointsEarned = giftPointsEarned;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public OffsetDateTime getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(OffsetDateTime placedAt) {
        this.placedAt = placedAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(OffsetDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
