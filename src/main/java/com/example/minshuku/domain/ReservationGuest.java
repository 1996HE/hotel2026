package com.example.minshuku.domain; // 宣言同行者エンティティ所属のドメインモデルパッケージ。

import java.time.OffsetDateTime; // 読み込みデータベースタイムスタンプフィールド使用の時間型。

public class ReservationGuest { // 定義予約同行者エンティティ，対応 reservation_guests テーブル。
  private Integer id; // 保存同行者主キー番号。
  private Integer reservationId; // 保存所属予約主キー番号。
  private String guestName; // 保存同行者姓名。
  private String guestKana; // 保存同行者假名读音。
  private String guestGender; // 保存同行者性别。
  private Integer guestAge; // 保存同行者年龄。
  private String guestPhone; // 保存同行者电话。
  private OffsetDateTime createdAt; // 保存作成時間。

  public Integer getId() { return id; } // 返却同行者主キー番号。
  public void setId(Integer id) { this.id = id; } // 設定同行者主キー番号。
  public Integer getReservationId() { return reservationId; } // 返却所属予約番号。
  public void setReservationId(Integer reservationId) { this.reservationId = reservationId; } // 設定所属予約番号。
  public String getGuestName() { return guestName; } // 返却同行者姓名。
  public void setGuestName(String guestName) { this.guestName = guestName; } // 設定同行者姓名。
  public String getGuestKana() { return guestKana; } // 返却同行者假名读音。
  public void setGuestKana(String guestKana) { this.guestKana = guestKana; } // 設定同行者假名读音。
  public String getGuestGender() { return guestGender; } // 返却同行者性别。
  public void setGuestGender(String guestGender) { this.guestGender = guestGender; } // 設定同行者性别。
  public Integer getGuestAge() { return guestAge; } // 返却同行者年龄。
  public void setGuestAge(Integer guestAge) { this.guestAge = guestAge; } // 設定同行者年龄。
  public String getGuestPhone() { return guestPhone; } // 返却同行者电话。
  public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; } // 設定同行者电话。
  public OffsetDateTime getCreatedAt() { return createdAt; } // 返却作成時間。
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; } // 設定作成時間。
}
