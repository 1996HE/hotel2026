package com.example.minshuku.domain; // 宣言予約エンティティ所属のドメインモデルパッケージ。

import java.math.BigDecimal; // 読み込み金額フィールド使用の高精度数字型。
import java.time.LocalDate; // 読み込み宿泊と退房日付使用の本地日付型。
import java.time.OffsetDateTime; // 読み込みデータベースタイムスタンプフィールド使用の時間型。

public class Reservation { // 定義住宿予約エンティティ，対応 reservations テーブル。
  private Integer id; // 保存予約主キー番号。
  private String reservationNo; // 保存予約番号。
  private Integer roomId; // 保存予約部屋主キー番号。
  private String roomNumber; // 保存結合検索得への部屋番号。
  private String roomName; // 保存結合検索得への部屋名称。
  private LocalDate checkInDate; // 保存宿泊日付，パッケージ含当天。
  private LocalDate checkOutDate; // 保存退房日付，非パッケージ含当天。
  private String guestName; // 保存住客姓名。
  private String guestKana; // 保存住客假名读音。
  private String guestGender; // 保存予約客户性别。
  private Integer guestAge; // 保存予約客户年龄。
  private String guestPhone; // 保存住客电话。
  private String guestEmail; // 保存住客邮箱。
  private Integer guestCount; // 保存宿泊人数。
  private String reservationForm; // 保存予約形式テーブル示值。
  private String paymentStatus; // 保存付款状態，例如 unpaid、paid。
  private String reservationStatus; // 保存予約状態，例如 booked、cancelled。
  private String roomCleaningStatus; // 保存結合検索得への部屋清掃状態。
  private BigDecimal totalAmount; // 保存预计住宿总金額。
  private String note; // 保存予約メモ。
  private String companionSummary; // 保存同行者姓名、性别、年龄と联系方式摘要。
  private OffsetDateTime createdAt; // 保存作成時間。
  private OffsetDateTime updatedAt; // 保存更新時間。

  public Integer getId() { return id; } // 返却予約主キー番号。
  public void setId(Integer id) { this.id = id; } // 設定予約主キー番号。
  public String getReservationNo() { return reservationNo; } // 返却予約番号。
  public void setReservationNo(String reservationNo) { this.reservationNo = reservationNo; } // 設定予約番号。
  public Integer getRoomId() { return roomId; } // 返却予約部屋番号。
  public void setRoomId(Integer roomId) { this.roomId = roomId; } // 設定予約部屋番号。
  public String getRoomNumber() { return roomNumber; } // 返却部屋番号。
  public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; } // 設定部屋番号。
  public String getRoomName() { return roomName; } // 返却部屋名称。
  public void setRoomName(String roomName) { this.roomName = roomName; } // 設定部屋名称。
  public LocalDate getCheckInDate() { return checkInDate; } // 返却宿泊日付。
  public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; } // 設定宿泊日付。
  public LocalDate getCheckOutDate() { return checkOutDate; } // 返却退房日付。
  public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; } // 設定退房日付。
  public String getGuestName() { return guestName; } // 返却住客姓名。
  public void setGuestName(String guestName) { this.guestName = guestName; } // 設定住客姓名。
  public String getGuestKana() { return guestKana; } // 返却住客假名读音。
  public void setGuestKana(String guestKana) { this.guestKana = guestKana; } // 設定住客假名读音。
  public String getGuestGender() { return guestGender; } // 返却予約客户性别。
  public void setGuestGender(String guestGender) { this.guestGender = guestGender; } // 設定予約客户性别。
  public Integer getGuestAge() { return guestAge; } // 返却予約客户年龄。
  public void setGuestAge(Integer guestAge) { this.guestAge = guestAge; } // 設定予約客户年龄。
  public String getGuestPhone() { return guestPhone; } // 返却住客电话。
  public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; } // 設定住客电话。
  public String getGuestEmail() { return guestEmail; } // 返却住客邮箱。
  public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; } // 設定住客邮箱。
  public Integer getGuestCount() { return guestCount; } // 返却宿泊人数。
  public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; } // 設定宿泊人数。
  public String getReservationForm() { return reservationForm; } // 返却予約形式テーブル示值。
  public void setReservationForm(String reservationForm) { this.reservationForm = reservationForm; } // 設定予約形式テーブル示值。
  public String getPaymentStatus() { return paymentStatus; } // 返却付款状態。
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; } // 設定付款状態。
  public String getReservationStatus() { return reservationStatus; } // 返却予約状態。
  public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; } // 設定予約状態。
  public String getReservationStatusLabel() { return switch (reservationStatus) { case "cancelled" -> "取消済"; case "checked_out" -> "退房完了待清掃"; default -> "予約済"; }; } // 返却用画面テーブル示の予約状態標签。
  public String getPaymentStatusLabel() { return "paid".equals(paymentStatus) ? "支払済" : "未払い"; } // 返却用画面テーブル示の支付状態標签。
  public String getRoomCleaningStatus() { return roomCleaningStatus; } // 返却結合得への部屋清掃状態。
  public void setRoomCleaningStatus(String roomCleaningStatus) { this.roomCleaningStatus = roomCleaningStatus; } // 設定結合得への部屋清掃状態。
  public BigDecimal getTotalAmount() { return totalAmount; } // 返却预计总金額。
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; } // 設定预计总金額。
  public String getNote() { return note; } // 返却予約メモ。
  public void setNote(String note) { this.note = note; } // 設定予約メモ。
  public String getCompanionSummary() { return companionSummary; } // 返却同行者摘要。
  public void setCompanionSummary(String companionSummary) { this.companionSummary = companionSummary; } // 設定同行者摘要。
  public OffsetDateTime getCreatedAt() { return createdAt; } // 返却作成時間。
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; } // 設定作成時間。
  public OffsetDateTime getUpdatedAt() { return updatedAt; } // 返却更新時間。
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; } // 設定更新時間。
}
