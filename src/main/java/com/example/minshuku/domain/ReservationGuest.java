package com.example.minshuku.domain;

import java.time.OffsetDateTime;

/**
 * 予約に紐づく同行者情報を保持するドメインオブジェクト。
 * <p>
 * 宿泊者本体とは分離し、人数の増減や一覧表示の柔軟性を保つための明細として扱う。
 */
public class ReservationGuest {
    // 同行者明細の識別情報。reservationId で予約本体へ紐づく。
    private Integer id;
    private Integer reservationId;

    // 同行者本人の宿泊者情報。
    private String guestName;
    private String guestKana;
    private String guestGender;
    private Integer guestAge;
    private String guestPhone;

    // 同行者登録日時。
    private OffsetDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
