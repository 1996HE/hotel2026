package com.example.minshuku.integration; // 実DBテスト共通処理の所属パッケージ。

import java.math.BigDecimal; // 金額フィールド初期化に使う高精度数値型。
import java.time.LocalDate; // 日付フィールド初期化に使う日付型。
import org.springframework.beans.factory.annotation.Autowired; // Spring の依存注入アノテーション。
import org.springframework.jdbc.core.JdbcTemplate; // 実DBへ直接SQLを流すためのテンプレート。

abstract class LocalDbTestSupport { // 実DBテストで使う共通基底クラス。
  @Autowired protected JdbcTemplate jdbcTemplate; // テスト用の JDBC テンプレート。

  protected int bookableRoomId; // 予約可能な部屋のIDを保持する。
  protected int occupiedRoomId; // 利用中の部屋のIDを保持する。
  protected int dirtyRoomId; // 清掃待ちの部屋のIDを保持する。
  protected int inactiveRoomId; // 無効化した部屋のIDを保持する。
  protected int spareRoomId; // 追加の予約可能部屋のIDを保持する。
  protected int ruleRoomId; // 料金ルール用の部屋IDを保持する。

  protected void resetTables() { // 各テスト前にテーブルを空にする。
    jdbcTemplate.execute("DELETE FROM reservation_guests"); // 同行者データを消す。
    jdbcTemplate.execute("DELETE FROM room_price_rules"); // 料金ルールデータを消す。
    jdbcTemplate.execute("DELETE FROM reservations"); // 予約データを消す。
    jdbcTemplate.execute("DELETE FROM rooms"); // 部屋データを消す。
    jdbcTemplate.execute("ALTER TABLE reservation_guests ALTER COLUMN id RESTART WITH 1"); // 同行者ID採番を戻す。
    jdbcTemplate.execute("ALTER TABLE room_price_rules ALTER COLUMN id RESTART WITH 1"); // 料金ルールID採番を戻す。
    jdbcTemplate.execute("ALTER TABLE reservations ALTER COLUMN id RESTART WITH 1"); // 予約ID採番を戻す。
    jdbcTemplate.execute("ALTER TABLE rooms ALTER COLUMN id RESTART WITH 1"); // 部屋ID採番を戻す。
  }

  protected void seedRooms() { // テストに必要な部屋データを投入する。
    bookableRoomId = insertRoom("101", "桜の間", "washitsu", 2, BigDecimal.valueOf(8800), false, "vacant", "cleaned", true, "予約可能な部屋"); // 予約可能な部屋を作る。
    occupiedRoomId = insertRoom("102", "竹の間", "washitsu", 3, BigDecimal.valueOf(9800), false, "occupied", "cleaned", true, "利用中の部屋"); // 利用中の部屋を作る。
    dirtyRoomId = insertRoom("103", "海の間", "yoshitsu", 2, BigDecimal.valueOf(11800), true, "vacant", "needs_cleaning", true, "清掃待ちの部屋"); // 清掃待ちの部屋を作る。
    inactiveRoomId = insertRoom("104", "山の間", "family", 5, BigDecimal.valueOf(12800), true, "vacant", "cleaned", false, "無効化済みの部屋"); // 無効化済みの部屋を作る。
    spareRoomId = insertRoom("105", "川の間", "family", 4, BigDecimal.valueOf(15000), false, "vacant", "cleaned", true, "予備の部屋"); // 追加の予約可能部屋を作る。
    ruleRoomId = insertRoom("106", "花の間", "washitsu", 2, BigDecimal.valueOf(16000), false, "vacant", "cleaned", true, "料金ルール確認用の部屋"); // 料金ルール確認用の部屋を作る。
  }

  protected int insertRoom(String roomNumber, String roomName, String roomType, int capacity, BigDecimal basePricePerPerson, boolean privateBath, String occupancyStatus, String cleaningStatus, boolean active, String note) { // 部屋1件を挿入してIDを返す。
    jdbcTemplate.update("""
      INSERT INTO rooms (
        room_number, room_name, room_type, capacity, base_price_per_person,
        private_bath, occupancy_status, cleaning_status, active, note
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """, roomNumber, roomName, roomType, capacity, basePricePerPerson, privateBath, occupancyStatus, cleaningStatus, active, note); // 部屋を挿入する。
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM rooms", Integer.class); // 追加した部屋のIDを返す。
  }

  protected int insertPriceRule(Integer roomId, String ruleName, LocalDate startDate, LocalDate endDate, BigDecimal pricePerPerson, int priority, boolean active, String note) { // 料金ルール1件を挿入してIDを返す。
    jdbcTemplate.update("""
      INSERT INTO room_price_rules (
        room_id, rule_name, start_date, end_date, price_per_person,
        priority, active, note
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """, roomId, ruleName, startDate, endDate, pricePerPerson, priority, active, note); // 料金ルールを挿入する。
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM room_price_rules", Integer.class); // 追加した料金ルールのIDを返す。
  }

  protected int insertReservation(Integer roomId, String reservationNo, LocalDate checkInDate, LocalDate checkOutDate, String guestName, String guestKana, String guestGender, Integer guestAge, String guestPhone, String guestEmail, Integer guestCount, String reservationForm, String paymentStatus, String reservationStatus, BigDecimal totalAmount, String note) { // 予約1件を挿入してIDを返す。
    jdbcTemplate.update("""
      INSERT INTO reservations (
        reservation_no, room_id, check_in_date, check_out_date, guest_name,
        guest_kana, guest_gender, guest_age, guest_phone, guest_email,
        guest_count, reservation_form, payment_status, reservation_status,
        total_amount, note
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """, reservationNo, roomId, checkInDate, checkOutDate, guestName, guestKana, guestGender, guestAge, guestPhone, guestEmail, guestCount, reservationForm, paymentStatus, reservationStatus, totalAmount, note); // 予約を挿入する。
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservations", Integer.class); // 追加した予約のIDを返す。
  }

  protected int insertReservationGuest(Integer reservationId, String guestName, String guestKana, String guestGender, Integer guestAge, String guestPhone) { // 同行者1件を挿入してIDを返す。
    jdbcTemplate.update("""
      INSERT INTO reservation_guests (
        reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone
      ) VALUES (?, ?, ?, ?, ?, ?)
      """, reservationId, guestName, guestKana, guestGender, guestAge, guestPhone); // 同行者を挿入する。
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation_guests", Integer.class); // 追加した同行者のIDを返す。
  }
}
