package com.example.minshuku.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationGuest;
import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.ReservationGuestMapper;
import com.example.minshuku.mapper.ReservationFinanceMapper;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@LoggedTest
@DisplayName("予約サービス")
/**
 * 予約サービスの境界値、状態遷移、料金計算を検証する単体テスト。
 */
class ReservationServiceTest {
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 8);

    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private ReservationGuestMapper reservationGuestMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RoomPriceRuleMapper priceRuleMapper;
    @Mock
    private CustomerService customerService;
    @Mock
    private ReservationFinanceMapper financeMapper;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationMapper, reservationGuestMapper, roomMapper,
                priceRuleMapper, customerService, financeMapper);
        lenient().when(reservationMapper.currentDate()).thenReturn(BUSINESS_DATE);
        lenient().when(reservationMapper.nextReservationSequence()).thenReturn(1L);
    }

    /**
     * テストケース名：test_01 current Date Uses System Date From Mapper Instead Of Local Environment Date
     * テスト条件：システム日付とローカル環境日付が異なる状態を準備する。
     * テスト要望：端末側のローカル時刻に依存せず、システム日付基準で予約可否を判定すること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_01 current Date Uses System Date From Mapper Instead Of Local Environment Date")
    @Test
    void currentDateUsesSystemDateFromMapperInsteadOfLocalEnvironmentDate() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        LocalDate systemDate = LocalDate.of(2099, 1, 1);
        when(reservationMapper.currentDate()).thenReturn(systemDate);

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Pago_Pago"));
            LocalDate actual = reservationService.currentDate();
            assertThat(actual).isEqualTo(systemDate);
            assertThat(actual).isNotEqualTo(BUSINESS_DATE);
            verify(reservationMapper).currentDate();
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    /**
     * テストケース名：test_02 create Rejects Check In Date Before System Date Even When Local Environment Date Is Earlier
     * テスト条件：システム日付とローカル環境日付が異なる状態を準備する。
     * テスト要望：端末側のローカル時刻に依存せず、システム日付基準で予約可否を判定すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_02 create Rejects Check In Date Before System Date Even When Local Environment Date Is Earlier")
    @Test
    void createRejectsCheckInDateBeforeSystemDateEvenWhenLocalEnvironmentDateIsEarlier() {
        LocalDate localEnvironmentDate = BUSINESS_DATE;
        LocalDate systemDate = localEnvironmentDate.plusDays(30);
        Reservation reservation = sampleReservation();
        reservation.setCheckInDate(localEnvironmentDate.plusDays(1));
        reservation.setCheckOutDate(localEnvironmentDate.plusDays(2));
        when(reservationMapper.currentDate()).thenReturn(systemDate);

        System.out.print("  文字列結果：createRejectsCheckInDateBeforeSystemDateEvenWhenLocalEnvironmentDateIsEarlier"
                + " / systemInput.currentDate=" + systemDate
                + " / localEnvironmentDate=" + localEnvironmentDate
                + " / input.checkInDate=" + reservation.getCheckInDate()
                + " / input.checkOutDate=" + reservation.getCheckOutDate()
                + System.lineSeparator());
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("チェックイン日は本日以降を選択してください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_03 create Registers Reservation And Companion Normally
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_03 create Registers Reservation And Companion Normally")
    @Test
    void createRegistersReservationAndCompanionNormally() {
        Reservation reservation = sampleReservation();
        Room room = sampleBookableRoom();
        when(roomMapper.findByIdForUpdate(1)).thenReturn(room);
        when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate()))
                .thenReturn(0);
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        ArgumentCaptor<ReservationGuest> guestCaptor = ArgumentCaptor.forClass(ReservationGuest.class);
        verify(reservationMapper).insert(reservationCaptor.capture());
        verify(reservationGuestMapper).insert(guestCaptor.capture());
        verify(roomMapper).findByIdForUpdate(1);
        verify(roomMapper, never()).updateStatuses(any(), any(), any());
        assertThat(reservationCaptor.getValue().getReservationStatus()).isEqualTo("booked");
        assertThat(reservationCaptor.getValue().getReservationNo()).isEqualTo("R000001");
        assertThat(reservationCaptor.getValue().getReservationNo()).hasSize(7);
        assertThat(reservationCaptor.getValue().getReservationStatusLabel()).isEqualTo("予約済");
        assertThat(reservationCaptor.getValue().getPaymentStatus()).isEqualTo("unpaid");
        assertThat(reservationCaptor.getValue().getPaymentStatusLabel()).isEqualTo("未払い");
        assertThat(reservationCaptor.getValue().getReservationForm()).isEqualTo("公式");
        assertThat(reservationCaptor.getValue().getTotalAmount()).isEqualByComparingTo("24000");
        assertThat(guestCaptor.getValue().getGuestName()).isEqualTo("佐藤花子");
        assertThat(guestCaptor.getValue().getGuestKana()).isEqualTo("サトウハナコ");
        assertThat(guestCaptor.getValue().getGuestGender()).isEqualTo("女性");
        assertThat(guestCaptor.getValue().getGuestAge()).isEqualTo(28);
        assertThat(guestCaptor.getValue().getGuestPhone()).isEqualTo("080-0000-0000");
    }

    /**
     * テストケース名：test_04 create Rejects Duplicate Reservation For Same Room And Date
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_04 create Rejects Duplicate Reservation For Same Room And Date")
    @Test
    void createRejectsDuplicateReservationForSameRoomAndDate() {
        Reservation reservation = sampleReservation();
        when(roomMapper.findByIdForUpdate(1)).thenReturn(sampleBookableRoom());
        when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate()))
                .thenReturn(1);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("指定期間はすでに予約されています。");
        verify(reservationMapper, never()).insert(any(Reservation.class));
        verify(reservationGuestMapper, never()).insert(any(ReservationGuest.class));
        verify(roomMapper, never()).updateStatuses(any(), any(), any());
    }

    /**
     * テストケース名：test_05 create Rejects Room That Is Not Vacant
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_05 create Rejects Room That Is Not Vacant")
    @Test
    void createRejectsRoomThatIsNotVacant() {
        Reservation reservation = sampleReservation();
        reservation.setCheckInDate(BUSINESS_DATE);
        reservation.setCheckOutDate(BUSINESS_DATE.plusDays(1));
        Room room = sampleBookableRoom();
        room.setOccupancyStatus("occupied");
        when(roomMapper.findByIdForUpdate(1)).thenReturn(room);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("当日予約は空室の部屋のみ登録できます。");
        verify(reservationMapper, never()).countOverlapping(any(), any(), any());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    /**
     * テストケース名：test_06 create Rejects Room That Is Not Cleaned
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_06 create Rejects Room That Is Not Cleaned")
    @Test
    void createRejectsRoomThatIsNotCleaned() {
        Reservation reservation = sampleReservation();
        reservation.setCheckInDate(BUSINESS_DATE);
        reservation.setCheckOutDate(BUSINESS_DATE.plusDays(1));
        Room room = sampleBookableRoom();
        room.setCleaningStatus("needs_cleaning");
        when(roomMapper.findByIdForUpdate(1)).thenReturn(room);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("当日予約は清掃済みの部屋のみ登録できます。");
        verify(reservationMapper, never()).countOverlapping(any(), any(), any());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    /**
     * テストケース名：test_07 create Rejects Invalid Guest Kana
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_07 create Rejects Invalid Guest Kana")
    @Test
    void createRejectsInvalidGuestKana() {
        Reservation reservation = sampleReservation();
        reservation.setGuestKana("やまだたろう");
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("フリガナは全角カタカナで入力してください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_08 create Rejects Past Check In Date
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_08 create Rejects Past Check In Date")
    @Test
    void createRejectsPastCheckInDate() {
        Reservation reservation = sampleReservation();
        reservation.setCheckInDate(BUSINESS_DATE.minusDays(1));
        reservation.setCheckOutDate(BUSINESS_DATE.plusDays(1));

        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("チェックイン日は本日以降を選択してください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_09 create Rejects Invalid Guest Phone
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_09 create Rejects Invalid Guest Phone")
    @Test
    void createRejectsInvalidGuestPhone() {
        Reservation reservation = sampleReservation();
        reservation.setGuestPhone("090-0000-0000x");
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("電話番号は000-0000-0000の形式で入力してください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_10 create Rejects Invalid Guest Email
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_10 create Rejects Invalid Guest Email")
    @Test
    void createRejectsInvalidGuestEmail() {
        Reservation reservation = sampleReservation();
        reservation.setGuestEmail("guest.example.com");
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("メールアドレスの形式が正しくありません。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_11 create Rejects Guest Count Over Ten
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_11 create Rejects Guest Count Over Ten")
    @Test
    void createRejectsGuestCountOverTen() {
        Reservation reservation = sampleReservation();
        reservation.setGuestCount(11);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("宿泊人数は10名以下にしてください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_12 create Rejects Invalid Companion Phone
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_12 create Rejects Invalid Companion Phone")
    @Test
    void createRejectsInvalidCompanionPhone() {
        Reservation reservation = sampleReservation();
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("電話番号は000-0000-0000の形式で入力してください。");
        verify(roomMapper, never()).findByIdForUpdate(any());
    }

    /**
     * テストケース名：test_13 create Allows Reservation Without Phone And Email When No Contact Info Is Selected
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_13 create Allows Reservation Without Phone And Email When No Contact Info Is Selected")
    @Test
    void createAllowsReservationWithoutPhoneAndEmailWhenNoContactInfoIsSelected() {
        Reservation reservation = sampleReservation();
        reservation.setGuestCount(1);
        reservation.setGuestPhone(null);
        reservation.setGuestEmail(null);
        Room room = sampleBookableRoom();
        when(roomMapper.findByIdForUpdate(1)).thenReturn(room);
        when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate()))
                .thenReturn(0);
        reservationService.create(reservation, true, List.of(), List.of(), List.of(), List.of(), List.of());
        verify(reservationMapper).insert(any(Reservation.class));
        verify(roomMapper, never()).updateStatuses(any(), any(), any());
    }

    /**
     * テストケース名：test_14 delete Cancelled Reservation Removes Row Normally
     * テスト条件：取消済み予約を準備する。
     * テスト要望：取消済み予約のみ削除できること。
     * テスト結果：削除 SQL が実行されること。
     */
    @DisplayName("test_14 delete Cancelled Reservation Removes Row Normally")
    @Test
    void deleteCancelledReservationRemovesRowNormally() {
        Reservation reservation = sampleReservation();
        reservation.setId(1);
        reservation.setReservationStatus("cancelled");
        when(reservationMapper.findById(1)).thenReturn(reservation);
        when(reservationMapper.deleteCancelled(1)).thenReturn(1);

        reservationService.deleteCancelled(1);

        verify(reservationMapper).deleteCancelled(1);
    }

    /**
     * テストケース名：test_15 delete Cancelled Reservation Rejects Active Reservation
     * テスト条件：取消済みではない予約を準備する。
     * テスト要望：取消済み予約以外は削除できないこと。
     * テスト結果：削除 SQL が実行されないこと。
     */
    @DisplayName("test_15 delete Cancelled Reservation Rejects Active Reservation")
    @Test
    void deleteCancelledReservationRejectsActiveReservation() {
        Reservation reservation = sampleReservation();
        reservation.setId(1);
        reservation.setReservationStatus("booked");
        when(reservationMapper.findById(1)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationService.deleteCancelled(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("取消済み予約のみ削除できます。");
        verify(reservationMapper, never()).deleteCancelled(1);
    }

    private Reservation sampleReservation() {
        Reservation reservation = TestSetData.reservation("standard").toDomain();
        reservation.setRoomId(1);
        reservation.setCheckInDate(BUSINESS_DATE.plusDays(60));
        reservation.setCheckOutDate(BUSINESS_DATE.plusDays(61));
        return reservation;
    }

    private Room sampleBookableRoom() {
        Room room = TestSetData.room("bookable").toDomain();
        room.setId(1);
        room.setBasePricePerPerson(BigDecimal.valueOf(12000));
        return room;
    }

    /**
     * テストケース名：test_16 reservation Labels Reflect Status Codes
     * テスト条件：対象処理に必要な入力値、mock、または DB 状態を準備する。
     * テスト要望：対象処理の期待仕様を満たすこと。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_16 reservation Labels Reflect Status Codes")
    @Test
    void reservationLabelsReflectStatusCodes() {
        Reservation reservation = new Reservation();
        reservation.setReservationStatus("cancelled");
        reservation.setPaymentStatus("paid");
        assertThat(reservation.getReservationStatusLabel()).isEqualTo("取消済");
        assertThat(reservation.getPaymentStatusLabel()).isEqualTo("支払済");
    }

    /**
     * テストケース名：test_17 update Checkout Cleaning Status Rejects Non Checked Out Reservation
     * テスト条件：予約済み状態の予約を準備する。
     * テスト要望：チェックアウト済みでない予約から清掃状態を更新できないこと。
     * テスト結果：期待したエラーになり、客室状態が更新されないこと。
     */
    @DisplayName("test_17 update Checkout Cleaning Status Rejects Non Checked Out Reservation")
    @Test
    void updateCheckoutCleaningStatusRejectsNonCheckedOutReservation() {
        Reservation reservation = sampleReservation();
        reservation.setId(1);
        reservation.setReservationStatus("booked");
        when(reservationMapper.findById(1)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationService.updateCheckoutCleaningStatus(1, "cleaned"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("チェックアウト済み予約のみ清掃状態を更新できます。");
        verify(roomMapper, never()).updateStatuses(any(), any(), any());
    }

    @DisplayName("test_18 update Reservation Status Rejects Invalid Transition")
    @Test
    void updateReservationStatusRejectsInvalidTransition() {
        Reservation reservation = sampleReservation();
        reservation.setId(1);
        reservation.setReservationStatus("checked_out");
        when(reservationMapper.findById(1)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1, "booked"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("指定された予約状態へは変更できません。");
        verify(reservationMapper, never()).updateReservationStatus(any(), any());
    }

    @DisplayName("test_19 update Reservation Status Rejects Checkout Before Check In")
    @Test
    void updateReservationStatusRejectsCheckoutBeforeCheckIn() {
        Reservation reservation = sampleReservation();
        reservation.setId(1);
        reservation.setReservationStatus("booked");
        when(reservationMapper.findById(1)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1, "checked_out"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("チェックイン前の予約はチェックアウトできません。");
        verify(reservationMapper, never()).updateReservationStatus(any(), any());
    }
}
