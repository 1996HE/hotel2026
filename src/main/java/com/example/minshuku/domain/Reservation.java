package com.example.minshuku.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 予約情報を保持するドメインオブジェクト。
 */
public class Reservation {
    // 予約本体の識別情報。
    private Integer id;
    private String reservationNo;
    private Integer roomId;
    private Integer customerId;

    // 一覧表示用に客室マスタから結合して取得する表示項目。
    private String roomNumber;
    private String roomName;

    // 宿泊期間。チェックアウト日は宿泊日数計算の終端として扱う。
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime checkedOutAt;

    // 代表宿泊者の基本情報。
    private String guestName;
    private String guestKana;
    private String guestGender;
    private Integer guestAge;
    private String guestPhone;
    private String guestEmail;
    private Integer guestCount;

    // 予約経路、支払い、予約状態など、一覧操作で更新される業務状態。
    private String reservationForm;
    private String paymentStatus;
    private String reservationStatus;
    private String roomCleaningStatus;

    // 登録時点で確定した宿泊金額と業務メモ。
    private BigDecimal totalAmount;
    private String note;

    // 同行者明細を一覧表示向けに集約したテキスト。
    private String companionSummary;

    // 登録・更新日時は監査と並び順に利用する。
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReservationNo() {
        return reservationNo;
    }

    public void setReservationNo(String reservationNo) {
        this.reservationNo = reservationNo;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public OffsetDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(OffsetDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public OffsetDateTime getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(OffsetDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestKana() {
        return guestKana;
    }

    public void setGuestKana(String guestKana) {
        this.guestKana = guestKana;
    }

    public String getGuestGender() {
        return guestGender;
    }

    public void setGuestGender(String guestGender) {
        this.guestGender = guestGender;
    }

    public Integer getGuestAge() {
        return guestAge;
    }

    public void setGuestAge(Integer guestAge) {
        this.guestAge = guestAge;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getReservationForm() {
        return reservationForm;
    }

    public void setReservationForm(String reservationForm) {
        this.reservationForm = reservationForm;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    /**
     * 画面表示用に、内部コードを日本語の業務ラベルへ変換する。
     */
    public String getReservationStatusLabel() {
        return switch (reservationStatus) {
            case "cancelled" -> "取消済";
            case "checked_in" -> "滞在中";
            case "checked_out" -> "チェックアウト完了待清掃";
            default -> "予約済";
        };
    }

    /**
     * 支払状況は UI 上では二値の表示に寄せる。
     */
    public String getPaymentStatusLabel() {
        return switch (paymentStatus) {
            case "paid" -> "支払済";
            case "partially_refunded" -> "一部返金";
            case "refunded" -> "全額返金";
            default -> "未払い";
        };
    }

    public String getRoomCleaningStatus() {
        return roomCleaningStatus;
    }

    public void setRoomCleaningStatus(String roomCleaningStatus) {
        this.roomCleaningStatus = roomCleaningStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCompanionSummary() {
        return companionSummary;
    }

    public void setCompanionSummary(String companionSummary) {
        this.companionSummary = companionSummary;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
