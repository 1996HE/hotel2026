package com.example.minshuku.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 客室マスタ情報を保持するドメインオブジェクト。
 * <p>
 * 予約可能条件や清掃状態の基礎情報を持ち、予約・清掃・管理画面から共通利用される。
 */
public class Room {
    // 客室マスタの識別情報と画面表示名。
    private Integer id;
    private String roomNumber;
    private String roomName;

    // 客室の販売条件。予約時の定員チェックと基本料金計算に使う。
    private String roomType;
    private Integer capacity;
    private BigDecimal basePricePerPerson;
    private Boolean privateBath;

    // 予約可否は宿泊状態と清掃状態の組み合わせで判定する。
    private String occupancyStatus;
    private String cleaningStatus;

    // 論理削除フラグと管理用メモ。
    private Boolean active;
    private String note;

    // 登録・更新日時は管理画面の履歴確認に使う。
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getBasePricePerPerson() {
        return basePricePerPerson;
    }

    public void setBasePricePerPerson(BigDecimal basePricePerPerson) {
        this.basePricePerPerson = basePricePerPerson;
    }

    public Boolean getPrivateBath() {
        return privateBath;
    }

    public void setPrivateBath(Boolean privateBath) {
        this.privateBath = privateBath;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public String getCleaningStatus() {
        return cleaningStatus;
    }

    public void setCleaningStatus(String cleaningStatus) {
        this.cleaningStatus = cleaningStatus;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
