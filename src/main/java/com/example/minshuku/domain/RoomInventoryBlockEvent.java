package com.example.minshuku.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Append-only audit event for a room inventory block operation. */
public class RoomInventoryBlockEvent {
    private Long id;
    private String operationId;
    private Integer blockId;
    private Integer roomId;
    private String eventType;
    private String beforeType;
    private LocalDate beforeStartDate;
    private LocalDate beforeEndDate;
    private String beforeReason;
    private String beforeStatus;
    private String afterType;
    private LocalDate afterStartDate;
    private LocalDate afterEndDate;
    private String afterReason;
    private String afterStatus;
    private String actionNote;
    private String actorUsername;
    private OffsetDateTime occurredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBeforeType() {
        return beforeType;
    }

    public void setBeforeType(String beforeType) {
        this.beforeType = beforeType;
    }

    public LocalDate getBeforeStartDate() {
        return beforeStartDate;
    }

    public void setBeforeStartDate(LocalDate beforeStartDate) {
        this.beforeStartDate = beforeStartDate;
    }

    public LocalDate getBeforeEndDate() {
        return beforeEndDate;
    }

    public void setBeforeEndDate(LocalDate beforeEndDate) {
        this.beforeEndDate = beforeEndDate;
    }

    public String getBeforeReason() {
        return beforeReason;
    }

    public void setBeforeReason(String beforeReason) {
        this.beforeReason = beforeReason;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterType() {
        return afterType;
    }

    public void setAfterType(String afterType) {
        this.afterType = afterType;
    }

    public LocalDate getAfterStartDate() {
        return afterStartDate;
    }

    public void setAfterStartDate(LocalDate afterStartDate) {
        this.afterStartDate = afterStartDate;
    }

    public LocalDate getAfterEndDate() {
        return afterEndDate;
    }

    public void setAfterEndDate(LocalDate afterEndDate) {
        this.afterEndDate = afterEndDate;
    }

    public String getAfterReason() {
        return afterReason;
    }

    public void setAfterReason(String afterReason) {
        this.afterReason = afterReason;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getActionNote() {
        return actionNote;
    }

    public void setActionNote(String actionNote) {
        this.actionNote = actionNote;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
