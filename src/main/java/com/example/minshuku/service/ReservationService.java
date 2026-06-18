package com.example.minshuku.service; // 宣言予約業務サービス所属パッケージ。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ型。
import com.example.minshuku.domain.ReservationGuest; // 読み込み予約同行者エンティティ型。
import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ型。
import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ型。
import com.example.minshuku.mapper.ReservationGuestMapper; // 読み込み予約同行者 Mapper。
import com.example.minshuku.mapper.ReservationMapper; // 読み込み予約 Mapper。
import com.example.minshuku.mapper.RoomMapper; // 読み込み部屋 Mapper。
import com.example.minshuku.mapper.RoomPriceRuleMapper; // 読み込み料金ルール Mapper。
import java.math.BigDecimal; // 読み込み金額計算使用の高精度数字型。
import java.time.LocalDate; // 読み込み日付計算使用の本地日付型。
import java.time.format.DateTimeFormatter; // 読み込み予約番号日付格式化工具。
import java.time.temporal.ChronoUnit; // 読み込み住数計算工具。
import java.util.List; // 読み込み一覧返却型。
import java.util.regex.Pattern; // 読み込み正则テーブル达式工具用格式検証。
import org.springframework.stereotype.Service; // 読み込み Spring サービスアノテーション。
import org.springframework.util.StringUtils; // 読み込み字符串検証工具。

@Service // 標记このクラスに Spring 管理の業務サービス。
public class ReservationService { // 定義予約相关業務逻辑。
  private static final Pattern KANA_PATTERN = Pattern.compile("^[ァ-ヶー\\s]+$"); // 定義全角片假名与空白の検証ルール。
  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$"); // 定義电话号码の検証ルール。
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); // 定義邮件地址の基本検証ルール。
  private final ReservationMapper reservationMapper; // 保存予約データ访问依赖。
  private final ReservationGuestMapper reservationGuestMapper; // 保存予約同行者データ访问依赖。
  private final RoomMapper roomMapper; // 保存部屋データ访问依赖。
  private final RoomPriceRuleMapper priceRuleMapper; // 保存料金ルールデータ访问依赖。

  public ReservationService(ReservationMapper reservationMapper, ReservationGuestMapper reservationGuestMapper, RoomMapper roomMapper, RoomPriceRuleMapper priceRuleMapper) { // 定義构造メソッド用依赖注入。
    this.reservationMapper = reservationMapper; // 保存注入の予約 Mapper。
    this.reservationGuestMapper = reservationGuestMapper; // 保存注入の予約同行者 Mapper。
    this.roomMapper = roomMapper; // 保存注入の部屋 Mapper。
    this.priceRuleMapper = priceRuleMapper; // 保存注入の料金ルール Mapper。
  }

  public List<Reservation> findRecent() { // 定義検索近期予約の業務メソッド。
    return reservationMapper.findRecentPage(5, 0); // 呼び出し Mapper 返却首页展示の 5 条近期予約。
  }

  public List<Reservation> findCancelled() { // 定義検索取消予約の業務メソッド。
    return reservationMapper.findCancelledPage(5, 0); // 呼び出し Mapper 返却首页展示の 5 条取消予約。
  }

  public List<Reservation> findCheckedOut() { // 定義検索完了退房予約の業務メソッド。
    return reservationMapper.findCheckedOutPage(5, 0); // 呼び出し Mapper 返却首页展示の 5 条退房记录。
  }

  public List<Reservation> findRecentPage(int page, int pageSize) { // 定義分页検索近期予約の業務メソッド。
    int safePage = Math.max(1, page); // 兜底页码至少に 1。
    int safePageSize = Math.max(1, pageSize); // 兜底每页条数至少に 1。
    return reservationMapper.findRecentPage(safePageSize, (safePage - 1) * safePageSize); // 按页返却近期予約。
  }

  public List<Reservation> findCancelledPage(int page, int pageSize) { // 定義分页検索取消予約の業務メソッド。
    int safePage = Math.max(1, page); // 兜底页码至少に 1。
    int safePageSize = Math.max(1, pageSize); // 兜底每页条数至少に 1。
    return reservationMapper.findCancelledPage(safePageSize, (safePage - 1) * safePageSize); // 按页返却取消予約。
  }

  public List<Reservation> findCheckedOutPage(int page, int pageSize) { // 定義分页検索完了退房予約の業務メソッド。
    int safePage = Math.max(1, page); // 兜底页码至少に 1。
    int safePageSize = Math.max(1, pageSize); // 兜底每页条数至少に 1。
    return reservationMapper.findCheckedOutPage(safePageSize, (safePage - 1) * safePageSize); // 按页返却完了退房予約。
  }

  public int countRecent() { // 定義集計近期予約总数の業務メソッド。
    return reservationMapper.countRecent(); // 呼び出し Mapper 返却近期予約总数。
  }

  public int countCancelled() { // 定義集計取消予約总数の業務メソッド。
    return reservationMapper.countCancelled(); // 呼び出し Mapper 返却取消予約总数。
  }

  public int countCheckedOut() { // 定義集計完了退房予約总数の業務メソッド。
    return reservationMapper.countCheckedOut(); // 呼び出し Mapper 返却完了退房予約总数。
  }

  public void syncDueCheckouts() { // 定義自动同步へ期退房状態のメソッド。
    List<Reservation> dueReservations = reservationMapper.findDueCheckouts(); // 検索へ期必要要退房の予約。
    for (Reservation dueReservation : dueReservations) { // 反復所有へ期予約。
      reservationMapper.markCheckedOut(dueReservation.getId()); // を予約状態更新に退房完成。
      roomMapper.updateStatuses(dueReservation.getRoomId(), "vacant", "needs_cleaning"); // を対応部屋同步に空室且待清掃。
    } // 終了へ期予約反復。
  } // 終了自动同步へ期退房状態のメソッド。

  public void create(Reservation reservation, boolean noContactInfo, List<String> companionNames, List<String> companionKanas, List<String> companionGenders, List<Integer> companionAges, List<String> companionPhones) { // 定義新規登録予約と同行者の業務メソッド。
    validateReservation(reservation, noContactInfo); // 执行予約フォーム基本検証。
    validateCompanions(reservation, companionNames); // 执行同行者人数と姓名検証。
    validateCompanionContacts(reservation, companionKanas, companionPhones); // 执行同行者联系信息検証。
    Room room = roomMapper.findById(reservation.getRoomId()); // 根据部屋番号検索部屋详情。
    if (room == null || !Boolean.TRUE.equals(room.getActive())) { throw new IllegalArgumentException("利用可能能な部屋を選択してください。"); } // 検証部屋必须存で且启用。
    if (!"vacant".equals(room.getOccupancyStatus())) { throw new IllegalArgumentException("空室の部屋のみ予約できます。"); } // 検証のみ有空室部屋可能以予約。
    if (!"cleaned".equals(room.getCleaningStatus())) { throw new IllegalArgumentException("清掃済みの部屋のみ予約できます。"); } // 検証のみ有清掃済部屋可能以予約。
    if (reservation.getGuestCount() > room.getCapacity()) { throw new IllegalArgumentException("宿泊人数が部屋の定員を超えています。"); } // 検証宿泊人数非能超过容量。
    int overlaps = reservationMapper.countOverlapping(reservation.getRoomId(), reservation.getCheckInDate(), reservation.getCheckOutDate()); // 検索同部屋日付重叠の予約数量。
    if (overlaps > 0) { throw new IllegalArgumentException("指定期間はすでに予約されています。"); } // 阻止重複予約。
    reservation.setReservationNo(buildReservationNo(reservation)); // 生成と設定予約番号。
    reservation.setReservationStatus("booked"); // 設定新予約に完了预订状態。
    if (!StringUtils.hasText(reservation.getPaymentStatus())) { reservation.setPaymentStatus("unpaid"); } // に缺失の付款状態設定初期値值。
    if (!StringUtils.hasText(reservation.getReservationForm())) { reservation.setReservationForm("公式"); } // に缺失の予約形式設定初期値值。
    reservation.setTotalAmount(calculateTotalAmount(reservation, room)); // 計算と設定预计总金額。
    reservationMapper.insert(reservation); // 呼び出し Mapper 書き込み予約记录。
    saveCompanions(reservation.getId(), reservation.getGuestCount(), companionNames, companionKanas, companionGenders, companionAges, companionPhones); // 保存同行者记录。
    roomMapper.updateStatuses(room.getId(), "reserved", room.getCleaningStatus()); // 予約成功後を部屋状態更新に予約済。
  }

  public int countBooked() { // 定義集計有効予約数量の業務メソッド。
    return reservationMapper.countBooked(); // 呼び出し Mapper 返却有効予約数量。
  }

  public void updatePaymentStatus(Integer id, String paymentStatus) { // 定義更新付款状態の業務メソッド。
    reservationMapper.updatePaymentStatus(id, paymentStatus); // 呼び出し Mapper 書き込み付款状態。
  }

  public void updateReservationStatus(Integer id, String reservationStatus) { // 定義更新予約状態の業務メソッド。
    Reservation reservation = reservationMapper.findById(id); // 検索予約以便同步対応部屋状態。
    if (reservation == null) { throw new IllegalArgumentException("予約が見つかりません。"); } // 検証予約必须存で。
    reservationMapper.updateReservationStatus(id, reservationStatus); // 更新予約状態。
    if ("booked".equals(reservationStatus)) { roomMapper.updateStatuses(reservation.getRoomId(), "reserved", "cleaned"); } // 恢复に有効予約时同步部屋状態。
    if ("checked_out".equals(reservationStatus)) { roomMapper.updateStatuses(reservation.getRoomId(), "vacant", "needs_cleaning"); } // 標记退房时同步部屋状態。
  }

  public void updateCheckoutCleaningStatus(Integer id, String cleaningStatus) { // 定義更新退房一覧中の清掃状態業務メソッド。
    Reservation reservation = reservationMapper.findById(id); // 検索予約以便同步対応部屋状態。
    if (reservation == null) { throw new IllegalArgumentException("予約が見つかりません。"); } // 検証予約必须存で。
    if (!"needs_cleaning".equals(cleaningStatus) && !"cleaned".equals(cleaningStatus)) { throw new IllegalArgumentException("清掃状態は清掃待ちまたは清掃済のみ選択できます。"); } // 仅允许两个清掃状態。
    roomMapper.updateStatuses(reservation.getRoomId(), "vacant", cleaningStatus); // 仅同步対応部屋の清掃状態。
  }

  public void cancel(Integer id) { // 定義取消予約の業務メソッド。
    Reservation reservation = reservationMapper.findById(id); // 検索取消オブジェクト用释放部屋状態。
    reservationMapper.cancel(id); // 呼び出し Mapper を予約状態改に取消。
    if (reservation != null) { roomMapper.updateStatuses(reservation.getRoomId(), "vacant", "cleaned"); } // 取消後を対応部屋恢复に空室と清掃済。
  }

  private void validateReservation(Reservation reservation, boolean noContactInfo) { // 定義予約フォーム基本検証メソッド。
    if (reservation.getRoomId() == null) { throw new IllegalArgumentException("部屋を選択してください。"); } // 検証必须選択部屋。
    if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) { throw new IllegalArgumentException("宿泊日を入力してください。"); } // 検証宿泊と退房日付非能に空。
    if (!reservation.getCheckInDate().isBefore(reservation.getCheckOutDate())) { throw new IllegalArgumentException("チェックアウト日はチェックイン日より後にしてください。"); } // 検証退房日付必须晚于宿泊日付。
    if (!StringUtils.hasText(reservation.getGuestName())) { throw new IllegalArgumentException("宿泊者名を入力してください。"); } // 検証住客姓名非能に空。
    validateOptionalContact(reservation.getGuestKana(), KANA_PATTERN, "フリガナは全角カタカナで入力してください。"); // 検証予約人フリガナ格式。
    if (!noContactInfo) { // 電話とメールの入力が必要な場合だけ検証する。
      validateOptionalContact(reservation.getGuestPhone(), PHONE_PATTERN, "電話番号は000-0000-0000の形式で入力してください。"); // 検証予約人电话格式。
      validateOptionalContact(reservation.getGuestEmail(), EMAIL_PATTERN, "メールアドレスの形式が正しくありません。"); // 検証予約人邮件格式。
    } else { // 連絡先なし予約の場合は空値を明示する。
      reservation.setGuestPhone(null); // 電話番号を未設定にする。
      reservation.setGuestEmail(null); // メールアドレスを未設定にする。
    } // 終了連絡先なし予約の処理。
    if (reservation.getGuestCount() == null || reservation.getGuestCount() < 1) { throw new IllegalArgumentException("宿泊人数は1名以上にしてください。"); } // 検証宿泊人数必须大于零。
  }

  private void validateCompanions(Reservation reservation, List<String> companionNames) { // 定義同行者基本検証メソッド。
    int requiredCount = Math.max(0, reservation.getGuestCount() - 1); // 計算应填写の同行者人数。
    if (requiredCount == 0) { return; } // 单人予約非必要要同行者信息。
    if (companionNames == null || companionNames.size() < requiredCount) { throw new IllegalArgumentException("同行者情報を入力してください。"); } // 検証同行者入力行数是否足够。
    for (int i = 0; i < requiredCount; i++) { // 反復每一位必填同行者。
      if (!StringUtils.hasText(companionNames.get(i))) { throw new IllegalArgumentException("同行者名を入力してください。"); } // 検証同行者姓名非能に空。
    }
  }

  private void validateCompanionContacts(Reservation reservation, List<String> companionKanas, List<String> companionPhones) { // 定義同行者联系信息検証メソッド。
    int requiredCount = Math.max(0, reservation.getGuestCount() - 1); // 計算应検証の同行者人数。
    for (int i = 0; i < requiredCount; i++) { // 反復每一位同行者入力。
      validateOptionalContact(valueAt(companionKanas, i), KANA_PATTERN, "フリガナは全角カタカナで入力してください。"); // 検証同行者フリガナ格式。
      validateOptionalContact(valueAt(companionPhones, i), PHONE_PATTERN, "電話番号は000-0000-0000の形式で入力してください。"); // 検証同行者电话格式。
    }
  }

  private void validateOptionalContact(String value, Pattern pattern, String message) { // 定義可能空联系フィールド検証メソッド。
    if (!StringUtils.hasText(value)) { return; } // 空值直接放行。
    if (!pattern.matcher(value).matches()) { throw new IllegalArgumentException(message); } // 非空时按ルール検証。
  }

  private void saveCompanions(Integer reservationId, Integer guestCount, List<String> companionNames, List<String> companionKanas, List<String> companionGenders, List<Integer> companionAges, List<String> companionPhones) { // 定義保存同行者のメソッド。
    int companionCount = Math.max(0, guestCount - 1); // 計算必要要保存の同行者数量。
    for (int i = 0; i < companionCount; i++) { // 反復每一位同行者入力。
      ReservationGuest guest = new ReservationGuest(); // 作成同行者エンティティオブジェクト。
      guest.setReservationId(reservationId); // 設定同行者所属予約番号。
      guest.setGuestName(valueAt(companionNames, i)); // 設定同行者姓名。
      guest.setGuestKana(valueAt(companionKanas, i)); // 設定同行者假名。
      guest.setGuestGender(valueAt(companionGenders, i)); // 設定同行者性别。
      guest.setGuestAge(integerAt(companionAges, i)); // 設定同行者年龄。
      guest.setGuestPhone(valueAt(companionPhones, i)); // 設定同行者电话。
      reservationGuestMapper.insert(guest); // 呼び出し Mapper 書き込み同行者记录。
    }
  }

  private String valueAt(List<String> values, int index) { // 定義安全読み込み一覧元素のメソッド。
    return values == null || values.size() <= index ? null : values.get(index); // 返却指定下標の值，缺失时返却空值。
  }

  private Integer integerAt(List<Integer> values, int index) { // 定義安全読み込み整数一覧元素のメソッド。
    return values == null || values.size() <= index ? null : values.get(index); // 返却指定下標の整数，缺失时返却空值。
  }

  private String buildReservationNo(Reservation reservation) { // 定義予約番号生成メソッド。
    String datePart = reservation.getCheckInDate().format(DateTimeFormatter.BASIC_ISO_DATE); // を宿泊日付格式化に yyyyMMdd。
    String timePart = String.valueOf(System.nanoTime()).substring(5, 11); // から纳秒時間截取短序列减少冲突概率。
    return "RSV-" + datePart + "-" + timePart; // 拼接と返却予約番号。
  }

  private BigDecimal calculateTotalAmount(Reservation reservation, Room room) { // 定義住宿总金額計算メソッド。
    BigDecimal total = BigDecimal.ZERO; // 初始化累计金額に零。
    LocalDate stayDate = reservation.getCheckInDate(); // から宿泊日付開始逐晚計算。
    while (stayDate.isBefore(reservation.getCheckOutDate())) { // 循环覆盖每个实际住宿夜晚。
      RoomPriceRule rule = priceRuleMapper.findBestRule(reservation.getRoomId(), stayDate); // 検索現在日付命中の最高优先级料金ルール。
      BigDecimal price = rule == null ? room.getBasePricePerPerson() : rule.getPricePerPerson(); // 优先使用时令价，没有ルール则使用基本价。
      total = total.add(price.multiply(BigDecimal.valueOf(reservation.getGuestCount()))); // 按宿泊人数累加当晚金額。
      stayDate = stayDate.plus(1, ChronoUnit.DAYS); // 日付推进へ下一晚。
    }
    return total; // 返却累计後の住宿总金額。
  }
}
