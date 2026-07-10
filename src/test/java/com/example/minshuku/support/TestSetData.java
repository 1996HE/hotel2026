package com.example.minshuku.support;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomPriceRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CSV ベースのテストフィクスチャをドメインオブジェクトへ変換する共通ローダー。
 */
public final class TestSetData {
    private static final Map<String, RoomSetData> ROOMS = load("setdata/rooms.csv", RoomSetData::from);
    private static final Map<String, ReservationSetData> RESERVATIONS = load("setdata/reservations.csv",
            ReservationSetData::from);
    private static final Map<String, PriceRuleSetData> PRICE_RULES = load("setdata/price-rules.csv",
            PriceRuleSetData::from);

    private TestSetData() {
    }

    public static RoomSetData room(String key) {
        // 部屋テスト用の固定データを取得する。
        return require(ROOMS, key, "room");
    }

    public static ReservationSetData reservation(String key) {
        // 予約テスト用の固定データを取得する。
        return require(RESERVATIONS, key, "reservation");
    }

    public static PriceRuleSetData priceRule(String key) {
        // 料金ルールテスト用の固定データを取得する。
        return require(PRICE_RULES, key, "price rule");
    }

    private static <T> T require(Map<String, T> fixtures, String key, String type) {
        T fixture = fixtures.get(key);
        if (fixture == null) {
            throw new IllegalArgumentException("Unknown " + type + " fixture: " + key);
        }
        return fixture;
    }

    private static <T> Map<String, T> load(String path, Function<Map<String, String>, T> mapper) {
        // CSV 1行目をヘッダとして読み込み、key 列をキーにして管理する。
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource(path), StandardCharsets.UTF_8))) {
            String[] headers = split(reader.readLine());
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> row(headers, line))
                    .collect(Collectors.toMap(row -> row.get("key"), mapper, (left, right) -> right,
                            LinkedHashMap::new));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load test fixture: " + path, ex);
        }
    }

    private static java.io.InputStream resource(String path) {
        java.io.InputStream input = TestSetData.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Test fixture not found: " + path);
        }
        return input;
    }

    private static Map<String, String> row(String[] headers, String line) {
        String[] values = split(line);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i += 1) {
            row.put(headers[i], i < values.length ? values[i] : "");
        }
        return row;
    }

    private static String[] split(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::trim).toArray(String[]::new);
    }

    private static Integer integer(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private static BigDecimal decimal(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static LocalDate date(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private static Boolean bool(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : Boolean.valueOf(value);
    }

    public record RoomSetData(String key, String roomNumber, String roomName, String roomType, int capacity,
            BigDecimal basePricePerPerson, boolean privateBath, String occupancyStatus, String cleaningStatus,
            boolean active, String note) {
        static RoomSetData from(Map<String, String> row) {
            // 部屋テスト用の CSV 1 行をそのまま record に変換する。
            return new RoomSetData(row.get("key"), row.get("roomNumber"), row.get("roomName"), row.get("roomType"),
                    integer(row, "capacity"), decimal(row, "basePricePerPerson"), bool(row, "privateBath"),
                    row.get("occupancyStatus"), row.get("cleaningStatus"), bool(row, "active"), row.get("note"));
        }

        public Room toDomain() {
            // ドメインへ変換してサービス/コントローラーの入力値として流用する。
            Room room = new Room();
            room.setRoomNumber(roomNumber);
            room.setRoomName(roomName);
            room.setRoomType(roomType);
            room.setCapacity(capacity);
            room.setBasePricePerPerson(basePricePerPerson);
            room.setPrivateBath(privateBath);
            room.setOccupancyStatus(occupancyStatus);
            room.setCleaningStatus(cleaningStatus);
            room.setActive(active);
            room.setNote(note);
            return room;
        }
    }

    public record ReservationSetData(String key, String reservationNo, Integer roomId, String roomNumber,
            String roomName, LocalDate checkInDate, LocalDate checkOutDate, String guestName, String guestKana,
            String guestGender, Integer guestAge, String guestPhone, String guestEmail, Integer guestCount,
            String reservationForm, String paymentStatus, String reservationStatus, BigDecimal totalAmount,
            String note, String companionSummary) {
        static ReservationSetData from(Map<String, String> row) {
            // 予約テスト用フィクスチャをそのまま record に変換する。
            return new ReservationSetData(row.get("key"), row.get("reservationNo"), integer(row, "roomId"),
                    row.get("roomNumber"), row.get("roomName"), date(row, "checkInDate"), date(row, "checkOutDate"),
                    row.get("guestName"), row.get("guestKana"), row.get("guestGender"), integer(row, "guestAge"),
                    row.get("guestPhone"), row.get("guestEmail"), integer(row, "guestCount"),
                    row.get("reservationForm"),
                    row.get("paymentStatus"), row.get("reservationStatus"), decimal(row, "totalAmount"),
                    row.get("note"), row.get("companionSummary"));
        }

        public Reservation toDomain() {
            // 予約ドメインへ変換し、画面・サービス・結合テストで共通利用する。
            Reservation reservation = new Reservation();
            reservation.setReservationNo(reservationNo);
            reservation.setRoomId(roomId);
            reservation.setRoomNumber(roomNumber);
            reservation.setRoomName(roomName);
            reservation.setCheckInDate(checkInDate);
            reservation.setCheckOutDate(checkOutDate);
            reservation.setGuestName(guestName);
            reservation.setGuestKana(guestKana);
            reservation.setGuestGender(guestGender);
            reservation.setGuestAge(guestAge);
            reservation.setGuestPhone(guestPhone);
            reservation.setGuestEmail(guestEmail);
            reservation.setGuestCount(guestCount);
            reservation.setReservationForm(reservationForm);
            reservation.setPaymentStatus(paymentStatus);
            reservation.setReservationStatus(reservationStatus);
            reservation.setTotalAmount(totalAmount);
            reservation.setNote(note);
            reservation.setCompanionSummary(companionSummary);
            return reservation;
        }
    }

    public record PriceRuleSetData(String key, Integer roomId, String roomNumber, String roomName, String ruleName,
            LocalDate startDate, LocalDate endDate, BigDecimal pricePerPerson, Integer priority, Boolean active,
            String note) {
        static PriceRuleSetData from(Map<String, String> row) {
            // 料金ルール用の CSV 行を record に変換する。
            return new PriceRuleSetData(row.get("key"), integer(row, "roomId"), row.get("roomNumber"),
                    row.get("roomName"), row.get("ruleName"), date(row, "startDate"), date(row, "endDate"),
                    decimal(row, "pricePerPerson"), integer(row, "priority"), bool(row, "active"), row.get("note"));
        }

        public RoomPriceRule toDomain() {
            // 料金ルールドメインへ変換する。
            RoomPriceRule rule = new RoomPriceRule();
            rule.setRoomId(roomId);
            rule.setRoomNumber(roomNumber);
            rule.setRoomName(roomName);
            rule.setRuleName(ruleName);
            rule.setStartDate(startDate);
            rule.setEndDate(endDate);
            rule.setPricePerPerson(pricePerPerson);
            rule.setPriority(priority);
            rule.setActive(active);
            rule.setNote(note);
            return rule;
        }
    }
}
