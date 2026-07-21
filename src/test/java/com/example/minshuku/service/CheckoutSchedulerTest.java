package com.example.minshuku.service;

import static org.mockito.Mockito.verify;

import com.example.minshuku.support.LoggedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@LoggedTest
@DisplayName("チェックアウト定期同期")
/** 期限到来チェックアウトの定期処理が予約サービスへ委譲されることを確認する。 */
class CheckoutSchedulerTest {
    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private CheckoutScheduler checkoutScheduler;

    /**
     * テストケース名：test_01 scheduled Sync Delegates To Reservation Service
     * テスト条件：ReservationService を mock 化した CheckoutScheduler を準備する。
     * テスト要望：定期処理の実行時に期限到来チェックアウト同期を1回委譲すること。
     * テスト結果：ReservationService.syncDueCheckouts が1回呼び出されること。
     */
    @DisplayName("test_01 scheduled Sync Delegates To Reservation Service")
    @Test
    void scheduledSyncDelegatesToReservationService() {
        checkoutScheduler.syncDueCheckouts();

        verify(reservationService).syncDueCheckouts();
    }
}
