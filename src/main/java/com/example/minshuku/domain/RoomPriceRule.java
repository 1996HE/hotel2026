package com.example.minshuku.domain; // 宣言料金ルールエンティティ所属のドメインモデルパッケージ。

import java.math.BigDecimal; // 読み込み金額フィールド使用の高精度数字型。
import java.time.LocalDate; // 読み込み日付范围フィールド使用のローカル日付型。
import java.time.OffsetDateTime; // 読み込みデータベースタイムスタンプフィールド使用の時間型。

public class RoomPriceRule { // 定義部屋时令料金ルールエンティティ，対応 room_price_rules テーブル。
  private Integer id; // 保存料金ルール主キー番号。
  private Integer roomId; // 保存料金ルール所属部屋番号。
  private String roomNumber; // 保存結合検索得への部屋番号。
  private String roomName; // 保存結合検索得への部屋名称。
  private String ruleName; // 保存料金ルール名称。
  private LocalDate startDate; // 保存ルール開始日付。
  private LocalDate endDate; // 保存ルール終了日付。
  private BigDecimal pricePerPerson; // 保存ルール期内每人单价。
  private Integer priority; // 保存ルール优先级，数值越小优先级越高。
  private Boolean active; // 標记该料金ルール是否有効。
  private String note; // 保存料金ルールメモ。
  private OffsetDateTime createdAt; // 保存作成時間。
  private OffsetDateTime updatedAt; // 保存更新時間。

  public Integer getId() { return id; } // 返却料金ルール主キー番号。
  public void setId(Integer id) { this.id = id; } // 設定料金ルール主キー番号。
  public Integer getRoomId() { return roomId; } // 返却所属部屋番号。
  public void setRoomId(Integer roomId) { this.roomId = roomId; } // 設定所属部屋番号。
  public String getRoomNumber() { return roomNumber; } // 返却部屋番号。
  public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; } // 設定部屋番号。
  public String getRoomName() { return roomName; } // 返却部屋名称。
  public void setRoomName(String roomName) { this.roomName = roomName; } // 設定部屋名称。
  public String getRuleName() { return ruleName; } // 返却料金ルール名称。
  public void setRuleName(String ruleName) { this.ruleName = ruleName; } // 設定料金ルール名称。
  public LocalDate getStartDate() { return startDate; } // 返却ルール開始日付。
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; } // 設定ルール開始日付。
  public LocalDate getEndDate() { return endDate; } // 返却ルール終了日付。
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; } // 設定ルール終了日付。
  public BigDecimal getPricePerPerson() { return pricePerPerson; } // 返却每人单价。
  public void setPricePerPerson(BigDecimal pricePerPerson) { this.pricePerPerson = pricePerPerson; } // 設定每人单价。
  public Integer getPriority() { return priority; } // 返却ルール优先级。
  public void setPriority(Integer priority) { this.priority = priority; } // 設定ルール优先级。
  public Boolean getActive() { return active; } // 返却料金ルール是否有効。
  public void setActive(Boolean active) { this.active = active; } // 設定料金ルール是否有効。
  public String getNote() { return note; } // 返却料金ルールメモ。
  public void setNote(String note) { this.note = note; } // 設定料金ルールメモ。
  public OffsetDateTime getCreatedAt() { return createdAt; } // 返却作成時間。
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; } // 設定作成時間。
  public OffsetDateTime getUpdatedAt() { return updatedAt; } // 返却更新時間。
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; } // 設定更新時間。
}
