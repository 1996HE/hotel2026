package com.example.minshuku.integration;

import com.example.minshuku.support.TestSetData;
import com.example.minshuku.support.TestSetData.RoomSetData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ローカル DB 結合テストで共通利用するデータ準備・投入ユーティリティ。
 */
abstract class LocalDbTestSupport {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected int bookableRoomId;
    protected int occupiedRoomId;
    protected int dirtyRoomId;
    protected int inactiveRoomId;
    protected int spareRoomId;
    protected int ruleRoomId;

    protected void resetTables() {
        // 各テストを独立させるため、関連テーブルとシーケンスを初期化する。
        jdbcTemplate.execute("DELETE FROM reservation_guests");
        jdbcTemplate.execute("DELETE FROM room_price_rules");
        jdbcTemplate.execute("DELETE FROM reservations");
        jdbcTemplate.execute("DELETE FROM rooms");
        jdbcTemplate.execute("ALTER TABLE reservation_guests ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE room_price_rules ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE reservations ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE rooms ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE reservation_no_seq RESTART WITH 1");
    }

    protected void seedRooms() {
        // 予約可否や状態遷移を確認しやすいよう、役割の異なる客室を複数投入する。
        bookableRoomId = insertRoom(TestSetData.room("bookable"));
        occupiedRoomId = insertRoom(TestSetData.room("occupied"));
        dirtyRoomId = insertRoom(TestSetData.room("dirty"));
        inactiveRoomId = insertRoom(TestSetData.room("inactive"));
        spareRoomId = insertRoom(TestSetData.room("spare"));
        ruleRoomId = insertRoom(TestSetData.room("rule"));
    }

    protected int insertRoom(RoomSetData room) {
        return insertRoom(room.roomNumber(), room.roomName(), room.roomType(), room.capacity(),
                room.basePricePerPerson(), room.privateBath(), room.occupancyStatus(), room.cleaningStatus(),
                room.active(), room.note());
    }

    protected int insertRoom(String roomNumber, String roomName, String roomType, int capacity,
            BigDecimal basePricePerPerson, boolean privateBath, String occupancyStatus, String cleaningStatus,
            boolean active, String note) {
        // テスト用の素データを直接投入し、サービスの前提状態を作る。
        jdbcTemplate.update("""
                INSERT INTO rooms (
                  room_number, room_name, room_type, capacity, base_price_per_person,
                  private_bath, occupancy_status, cleaning_status, active, note
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, roomNumber, roomName, roomType, capacity, basePricePerPerson, privateBath, occupancyStatus,
                cleaningStatus, active, note);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM rooms", Integer.class);
    }

    protected int insertPriceRule(Integer roomId, String ruleName, LocalDate startDate, LocalDate endDate,
            BigDecimal pricePerPerson, int priority, boolean active, String note) {
        // 料金ルールの重複や優先順を検証するための初期データを投入する。
        jdbcTemplate.update("""
                INSERT INTO room_price_rules (
                  room_id, rule_name, start_date, end_date, price_per_person,
                  priority, active, note
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, roomId, ruleName, startDate, endDate, pricePerPerson, priority, active, note);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM room_price_rules", Integer.class);
    }

    protected int insertReservation(Integer roomId, String reservationNo, LocalDate checkInDate, LocalDate checkOutDate,
            String guestName, String guestKana, String guestGender, Integer guestAge, String guestPhone,
            String guestEmail, Integer guestCount, String reservationForm, String paymentStatus,
            String reservationStatus, BigDecimal totalAmount, String note) {
        // 予約本体の状態同期テストで使う直挿しレコード。
        jdbcTemplate.update("""
                INSERT INTO reservations (
                  reservation_no, room_id, check_in_date, check_out_date, guest_name,
                  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
                  guest_count, reservation_form, payment_status, reservation_status,
                  total_amount, note
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, reservationNo, roomId, checkInDate, checkOutDate, guestName, guestKana, guestGender, guestAge,
                guestPhone, guestEmail, guestCount, reservationForm, paymentStatus, reservationStatus, totalAmount,
                note);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservations", Integer.class);
    }

    protected int insertReservationGuest(Integer reservationId, String guestName, String guestKana, String guestGender,
            Integer guestAge, String guestPhone) {
        // 同行者明細を直接投入し、予約本体との関連を確認できるようにする。
        jdbcTemplate.update("""
                INSERT INTO reservation_guests (
                  reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, reservationId, guestName, guestKana, guestGender, guestAge, guestPhone);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation_guests", Integer.class);
    }

    protected void printComparison(String label, Object expected, Object actual) {
        // テストの期待値・実測値をログに残し、差分確認しやすくする。
        String result = Objects.equals(expected, actual) ? "一致" : "不一致";
        System.out.print("  結果比較：" + label + " / 期待値=" + expected + " / 実際値=" + actual + " / " + result
                + System.lineSeparator());
    }
}
