package com.example.minshuku.service;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationGuest;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.mapper.ReservationGuestMapper;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 予約登録、状態更新、料金計算、同行者情報の保存を扱うサービス。
 * <p>
 * このサービスは予約業務の中核であり、入力検証だけでなく、
 * 予約番号の発番、客室状態の同期、チェックアウト後の清掃状態更新まで一貫して扱う。
 */
@Service
public class ReservationService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern KANA_PATTERN = Pattern.compile("^[ァ-ヶー\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> PAYMENT_STATUSES = Set.of("unpaid", "paid");
    private static final Set<String> RESERVATION_STATUSES = Set.of("booked", "checked_out", "cancelled");
    private static final Set<String> CLEANING_STATUSES = Set.of("needs_cleaning", "cleaned");
    private static final String MESSAGE_INVALID_KANA = "フリガナは全角カタカナで入力してください。";
    private static final String MESSAGE_INVALID_PHONE = "電話番号は000-0000-0000の形式で入力してください。";
    private static final String MESSAGE_INVALID_EMAIL = "メールアドレスの形式が正しくありません。";
    private static final String MESSAGE_INVALID_CLEANING_STATUS = "清掃状態は清掃待ちまたは清掃済のみ選択できます。";
    private static final String MESSAGE_PAST_CHECK_IN = "チェックイン日は本日以降を選択してください。";
    private static final String MESSAGE_INVALID_STAY_RANGE = "チェックアウト日はチェックイン日より後にしてください。";
    private static final int MAX_GUEST_COUNT = 10;

    private final ReservationMapper reservationMapper;
    private final ReservationGuestMapper reservationGuestMapper;
    private final RoomMapper roomMapper;
    private final RoomPriceRuleMapper priceRuleMapper;

    public ReservationService(
            ReservationMapper reservationMapper,
            ReservationGuestMapper reservationGuestMapper,
            RoomMapper roomMapper,
            RoomPriceRuleMapper priceRuleMapper) {
        this.reservationMapper = reservationMapper;
        this.reservationGuestMapper = reservationGuestMapper;
        this.roomMapper = roomMapper;
        this.priceRuleMapper = priceRuleMapper;
    }

    @Transactional(readOnly = true)
    public LocalDate currentDate() {
        return reservationMapper.currentDate();
    }

    @Transactional(readOnly = true)
    public List<Reservation> findRecent() {
        return reservationMapper.findRecentPage(5, 0);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findCancelled() {
        return reservationMapper.findCancelledPage(5, 0);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findCheckedOut() {
        return reservationMapper.findCheckedOutPage(5, 0);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findRecentPage(int page, int pageSize) {
        int safePageSize = safePageSize(pageSize);
        return reservationMapper.findRecentPage(safePageSize, pageOffset(page, safePageSize));
    }

    @Transactional(readOnly = true)
    public List<Reservation> findCancelledPage(int page, int pageSize) {
        int safePageSize = safePageSize(pageSize);
        return reservationMapper.findCancelledPage(safePageSize, pageOffset(page, safePageSize));
    }

    @Transactional(readOnly = true)
    public List<Reservation> findCheckedOutPage(int page, int pageSize) {
        int safePageSize = safePageSize(pageSize);
        return reservationMapper.findCheckedOutPage(safePageSize, pageOffset(page, safePageSize));
    }

    @Transactional(readOnly = true)
    public int countRecent() {
        return reservationMapper.countRecent();
    }

    @Transactional(readOnly = true)
    public int countCancelled() {
        return reservationMapper.countCancelled();
    }

    @Transactional(readOnly = true)
    public int countCheckedOut() {
        return reservationMapper.countCheckedOut();
    }

    @Transactional
    public void syncDueCheckouts() {
        // 予約日時点で宿泊終了に達している予約を回収し、客室状態を予約業務側で同期する。
        List<Reservation> dueReservations = reservationMapper.findDueCheckouts();
        for (Reservation dueReservation : dueReservations) {
            reservationMapper.markCheckedOut(dueReservation.getId());
            // チェックアウト済みにした客室は「空室 + 清掃待ち」に戻す。
            roomMapper.updateStatuses(dueReservation.getRoomId(), "vacant", "needs_cleaning");
        }
    }

    /**
     * 予約登録に必要な入力チェック、客室ロック、重複予約確認、料金計算を一つのトランザクションで実行する。
     */
    @Transactional
    public void create(
            Reservation reservation,
            boolean noContactInfo,
            List<String> companionNames,
            List<String> companionKanas,
            List<String> companionGenders,
            List<Integer> companionAges,
            List<String> companionPhones) {
        // 予約作成は、入力検証 -> 競合確認 -> 番号発番 -> 保存 -> 同行者保存 -> 客室状態更新の順で行う。
        validateReservation(reservation, noContactInfo);
        validateCompanions(reservation, companionNames);
        validateCompanionContacts(reservation, companionKanas, companionPhones);

        // 予約対象の客室はロック付きで取得し、並行更新による二重予約を防ぐ。
        Room room = roomMapper.findByIdForUpdate(reservation.getRoomId());
        validateRoomForReservation(reservation, room);
        validateNoOverlappingReservation(reservation);

        reservation.setReservationNo(nextReservationNo());
        reservation.setReservationStatus("booked");
        if (!StringUtils.hasText(reservation.getPaymentStatus())) {
            // 入金情報が未入力の場合は未入金を初期値とする。
            reservation.setPaymentStatus("unpaid");
        }
        if (!StringUtils.hasText(reservation.getReservationForm())) {
            // 予約経路が未指定の場合は公式予約として扱う。
            reservation.setReservationForm("公式");
        }

        // 料金は宿泊日ごとに単価を積み上げて算出し、予約保存時点で確定する。
        reservation.setTotalAmount(calculateTotalAmount(reservation, room));
        reservationMapper.insert(reservation);
        // 予約本体のID確定後に同行者を保存する。同行者は予約人数から1名分を差し引いて管理する。
        saveCompanions(
                reservation.getId(),
                reservation.getGuestCount(),
                companionNames,
                companionKanas,
                companionGenders,
                companionAges,
                companionPhones);
        // 予約成立後は客室を予約済みに切り替える。
        roomMapper.updateStatuses(room.getId(), "reserved", room.getCleaningStatus());
    }

    @Transactional(readOnly = true)
    public int countBooked() {
        return reservationMapper.countBooked();
    }

    @Transactional
    public void updatePaymentStatus(Integer id, String paymentStatus) {
        // 支払い状態は業務上の許可値だけを受け付ける。
        requireAllowed(paymentStatus, PAYMENT_STATUSES, "支払い状態が正しくありません。");
        if (reservationMapper.updatePaymentStatus(id, paymentStatus) == 0) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
    }

    /**
     * 予約状態を更新し、客室の宿泊状態・清掃状態を予約状態に合わせて同期する。
     */
    @Transactional
    public void updateReservationStatus(Integer id, String reservationStatus) {
        // 予約状態の変更は、予約本体と客室状態の整合を必ずセットで保つ。
        requireAllowed(reservationStatus, RESERVATION_STATUSES, "予約状態が正しくありません。");

        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
        if (reservationMapper.updateReservationStatus(id, reservationStatus) == 0) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
        if ("booked".equals(reservationStatus)) {
            // 予約中に戻す場合は、客室を予約済みに復帰させる。
            roomMapper.updateStatuses(reservation.getRoomId(), "reserved", "cleaned");
        }
        if ("checked_out".equals(reservationStatus)) {
            // 他の予約中データが残る場合は、客室を空室に戻さず予約済み状態を維持する。
            updateRoomAfterReservationRelease(reservation, "needs_cleaning");
        }
    }

    @Transactional
    public void updateCheckoutCleaningStatus(Integer id, String cleaningStatus) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }

        // 清掃状態の更新は、チェックアウト後の客室運用に限定する。
        if (!"checked_out".equals(reservation.getReservationStatus())) {
            throw new IllegalArgumentException("チェックアウト済み予約のみ清掃状態を更新できます。");
        }
        requireAllowed(cleaningStatus, CLEANING_STATUSES, MESSAGE_INVALID_CLEANING_STATUS);
        roomMapper.updateStatuses(reservation.getRoomId(), "vacant", cleaningStatus);
    }

    @Transactional
    public void cancel(Integer id) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservationMapper.cancel(id) == 0) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
        if (reservation != null) {
            // 取消済みにした客室は、他の予約がなければ通常の空室・清掃済み状態へ戻す。
            updateRoomAfterReservationRelease(reservation, "cleaned");
        }
    }

    /**
     * 取消済み予約を一覧から完全削除する。
     */
    @Transactional
    public void deleteCancelled(Integer id) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
        if (!"cancelled".equals(reservation.getReservationStatus())) {
            throw new IllegalArgumentException("取消済み予約のみ削除できます。");
        }
        if (reservationMapper.deleteCancelled(id) == 0) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
    }

    /**
     * チェックアウト済み予約を一覧から完全削除する。
     */
    @Transactional
    public void deleteCheckedOut(Integer id) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
        if (!"checked_out".equals(reservation.getReservationStatus())) {
            throw new IllegalArgumentException("チェックアウト済み予約のみ削除できます。");
        }
        if (reservationMapper.deleteCheckedOut(id) == 0) {
            throw new IllegalArgumentException("予約が見つかりません。");
        }
    }

    private void updateRoomAfterReservationRelease(Reservation reservation, String cleaningStatusWhenVacant) {
        int otherBookedReservations = reservationMapper.countOtherBookedByRoomId(
                reservation.getRoomId(),
                reservation.getId());
        if (otherBookedReservations > 0) {
            roomMapper.updateStatuses(reservation.getRoomId(), "reserved", "cleaned");
            return;
        }
        roomMapper.updateStatuses(reservation.getRoomId(), "vacant", cleaningStatusWhenVacant);
    }

    private String nextReservationNo() {
        return "R" + String.format("%06d", reservationMapper.nextReservationSequence());
    }

    private void requireAllowed(String value, Set<String> allowedValues, String message) {
        if (!StringUtils.hasText(value) || !allowedValues.contains(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private int safePageSize(int pageSize) {
        // 画面やAPIから極端なページサイズを受け取ってもDB負荷を一定範囲に抑える。
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private int pageOffset(int page, int safePageSize) {
        // 1未満のページ指定は1ページ目として扱う。
        int safePage = Math.max(1, page);
        return (safePage - 1) * safePageSize;
    }

    private void validateReservation(Reservation reservation, boolean noContactInfo) {
        // 予約登録の入口で、業務上の必須条件を先に止める。
        if (reservation.getRoomId() == null) {
            throw new IllegalArgumentException("部屋を選択してください。");
        }
        if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
            throw new IllegalArgumentException("宿泊日を入力してください。");
        }
        if (reservation.getCheckInDate().isBefore(currentDate())) {
            // システム日付を基準に、過去日付の予約を拒否する。
            throw new IllegalArgumentException(MESSAGE_PAST_CHECK_IN);
        }
        if (!reservation.getCheckInDate().isBefore(reservation.getCheckOutDate())) {
            throw new IllegalArgumentException(MESSAGE_INVALID_STAY_RANGE);
        }
        if (!StringUtils.hasText(reservation.getGuestName())) {
            throw new IllegalArgumentException("宿泊者名を入力してください。");
        }

        validateOptionalContact(reservation.getGuestKana(), KANA_PATTERN, MESSAGE_INVALID_KANA);

        if (!noContactInfo) {
            // 連絡先を提供する運用では電話・メールの形式を両方確認する。
            validateOptionalContact(reservation.getGuestPhone(), PHONE_PATTERN, MESSAGE_INVALID_PHONE);
            validateOptionalContact(reservation.getGuestEmail(), EMAIL_PATTERN, MESSAGE_INVALID_EMAIL);
        } else {
            // 連絡先非保持モードでは、DBに空文字ではなく NULL を保存する。
            reservation.setGuestPhone(null);
            reservation.setGuestEmail(null);
        }
        if (reservation.getGuestCount() == null || reservation.getGuestCount() < 1) {
            throw new IllegalArgumentException("宿泊人数は1名以上にしてください。");
        }
        if (reservation.getGuestCount() > MAX_GUEST_COUNT) {
            throw new IllegalArgumentException("宿泊人数は10名以下にしてください。");
        }
    }

    private void validateRoomForReservation(Reservation reservation, Room room) {
        // 予約対象の客室は、営業中・空室・清掃済みの3条件をすべて満たす必要がある。
        if (room == null || !Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("利用可能な部屋を選択してください。");
        }
        if (!"vacant".equals(room.getOccupancyStatus())) {
            throw new IllegalArgumentException("空室の部屋のみ予約できます。");
        }
        if (!"cleaned".equals(room.getCleaningStatus())) {
            throw new IllegalArgumentException("清掃済みの部屋のみ予約できます。");
        }
        if (reservation.getGuestCount() > room.getCapacity()) {
            throw new IllegalArgumentException("宿泊人数が部屋の定員を超えています。");
        }
    }

    private void validateNoOverlappingReservation(Reservation reservation) {
        // 同一客室で宿泊期間が重なる予約を防止する。
        int overlaps = reservationMapper.countOverlapping(
                reservation.getRoomId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate());

        if (overlaps > 0) {
            throw new IllegalArgumentException("指定期間はすでに予約されています。");
        }
    }

    private void validateCompanions(Reservation reservation, List<String> companionNames) {
        // 宿泊人数が2名以上の場合、1名を超える人数分の同行者を必須入力とする。
        int requiredCount = Math.max(0, reservation.getGuestCount() - 1);
        if (requiredCount == 0) {
            return;
        }
        if (companionNames == null || companionNames.size() < requiredCount) {
            throw new IllegalArgumentException("同行者情報を入力してください。");
        }

        for (int i = 0; i < requiredCount; i++) {
            if (!StringUtils.hasText(companionNames.get(i))) {
                throw new IllegalArgumentException("同行者名を入力してください。");
            }
        }
    }

    private void validateCompanionContacts(
            Reservation reservation,
            List<String> companionKanas,
            List<String> companionPhones) {
        // 同行者ごとに、入力された項目だけ業務形式チェックを行う。
        int requiredCount = Math.max(0, reservation.getGuestCount() - 1);
        for (int i = 0; i < requiredCount; i++) {
            validateOptionalContact(valueAt(companionKanas, i), KANA_PATTERN, MESSAGE_INVALID_KANA);
            validateOptionalContact(valueAt(companionPhones, i), PHONE_PATTERN, MESSAGE_INVALID_PHONE);
        }
    }

    private void validateOptionalContact(String value, Pattern pattern, String message) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void saveCompanions(
            Integer reservationId,
            Integer guestCount,
            List<String> companionNames,
            List<String> companionKanas,
            List<String> companionGenders,
            List<Integer> companionAges,
            List<String> companionPhones) {
        // 同行者は予約本体とは別テーブルへ分割保存し、一覧表示や将来の拡張に備える。
        int companionCount = Math.max(0, guestCount - 1);
        for (int i = 0; i < companionCount; i++) {
            ReservationGuest guest = new ReservationGuest();
            guest.setReservationId(reservationId);
            guest.setGuestName(valueAt(companionNames, i));
            guest.setGuestKana(valueAt(companionKanas, i));
            guest.setGuestGender(valueAt(companionGenders, i));
            guest.setGuestAge(integerAt(companionAges, i));
            guest.setGuestPhone(valueAt(companionPhones, i));
            reservationGuestMapper.insert(guest);
        }
    }

    private String valueAt(List<String> values, int index) {
        return values == null || values.size() <= index ? null : values.get(index);
    }

    private Integer integerAt(List<Integer> values, int index) {
        return values == null || values.size() <= index ? null : values.get(index);
    }

    private BigDecimal calculateTotalAmount(Reservation reservation, Room room) {
        // 日別の最優先料金ルールを積み上げる。未設定日は客室基本単価を採用する。
        BigDecimal total = BigDecimal.ZERO;
        LocalDate stayDate = reservation.getCheckInDate();
        while (stayDate.isBefore(reservation.getCheckOutDate())) {
            RoomPriceRule rule = priceRuleMapper.findBestRule(reservation.getRoomId(), stayDate);
            BigDecimal price = rule == null ? room.getBasePricePerPerson() : rule.getPricePerPerson();
            total = total.add(price.multiply(BigDecimal.valueOf(reservation.getGuestCount())));
            stayDate = stayDate.plus(1, ChronoUnit.DAYS);
        }
        return total;
    }
}
