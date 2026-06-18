package com.example.minshuku.domain; // 宣言部屋エンティティ所属のドメインモデルパッケージ。

import java.math.BigDecimal; // 読み込み金額フィールド使用の高精度数字型。
import java.time.OffsetDateTime; // 読み込みデータベースタイムスタンプフィールド使用の時間型。

public class Room { // 定義民宿客室エンティティ，対応 rooms テーブル。
  private Integer id; // 保存部屋主キー番号。
  private String roomNumber; // 保存部屋番号，例如 101。
  private String roomName; // 保存日式部屋名称，例如 桜の間。
  private String roomType; // 保存部屋タイプ代码，例如 washitsu、yoshitsu、family。
  private Integer capacity; // 保存可能宿泊人数上限。
  private BigDecimal basePricePerPerson; // 保存每人基本単価。
  private Boolean privateBath; // 標记是否有独立浴室。
  private String occupancyStatus; // 保存宿泊状態，例如 vacant、reserved、occupied。
  private String cleaningStatus; // 保存清掃状態，例如 cleaned、needs_cleaning。
  private Boolean active; // 標记该部屋是否继续对外使用。
  private String note; // 保存部屋メモ。
  private OffsetDateTime createdAt; // 保存作成時間。
  private OffsetDateTime updatedAt; // 保存更新時間。

  public Integer getId() { return id; } // 返却部屋主キー番号。
  public void setId(Integer id) { this.id = id; } // 設定部屋主キー番号。
  public String getRoomNumber() { return roomNumber; } // 返却部屋番号。
  public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; } // 設定部屋番号。
  public String getRoomName() { return roomName; } // 返却部屋名称。
  public void setRoomName(String roomName) { this.roomName = roomName; } // 設定部屋名称。
  public String getRoomType() { return roomType; } // 返却部屋タイプ代码。
  public void setRoomType(String roomType) { this.roomType = roomType; } // 設定部屋タイプ代码。
  public Integer getCapacity() { return capacity; } // 返却可能宿泊人数上限。
  public void setCapacity(Integer capacity) { this.capacity = capacity; } // 設定可能宿泊人数上限。
  public BigDecimal getBasePricePerPerson() { return basePricePerPerson; } // 返却每人基本単価。
  public void setBasePricePerPerson(BigDecimal basePricePerPerson) { this.basePricePerPerson = basePricePerPerson; } // 設定每人基本単価。
  public Boolean getPrivateBath() { return privateBath; } // 返却是否有独立浴室。
  public void setPrivateBath(Boolean privateBath) { this.privateBath = privateBath; } // 設定是否有独立浴室。
  public String getOccupancyStatus() { return occupancyStatus; } // 返却宿泊状態。
  public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; } // 設定宿泊状態。
  public String getCleaningStatus() { return cleaningStatus; } // 返却清掃状態。
  public void setCleaningStatus(String cleaningStatus) { this.cleaningStatus = cleaningStatus; } // 設定清掃状態。
  public Boolean getActive() { return active; } // 返却部屋是否启用。
  public void setActive(Boolean active) { this.active = active; } // 設定部屋是否启用。
  public String getNote() { return note; } // 返却部屋メモ。
  public void setNote(String note) { this.note = note; } // 設定部屋メモ。
  public OffsetDateTime getCreatedAt() { return createdAt; } // 返却作成時間。
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; } // 設定作成時間。
  public OffsetDateTime getUpdatedAt() { return updatedAt; } // 返却更新時間。
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; } // 設定更新時間。
}
