package com.example.minshuku.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.support.LoggedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@LoggedTest
@DisplayName("部屋サービス")
/**
 * 客室サービスの削除制御と状態更新を検証する単体テスト。
 */
class RoomServiceTest {
    @Mock
    private RoomMapper roomMapper;

    /**
     * テストケース名：test_01 delete Rejects Room With Active Reservation
     * テスト条件：削除対象客室に予約中データが存在する。
     * テスト要望：有効予約が残る客室は論理削除できないこと。
     * テスト結果：期待したエラーになり、削除 SQL が実行されないこと。
     */
    @DisplayName("test_01 delete Rejects Room With Active Reservation")
    @Test
    void deleteRejectsRoomWithActiveReservation() {
        RoomService roomService = new RoomService(roomMapper);
        when(roomMapper.countBookedReservations(1)).thenReturn(1);

        assertThatThrownBy(() -> roomService.delete(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("予約中の部屋は削除できません。先に予約を取消してください。");
        verify(roomMapper, never()).deactivate(1);
    }

    /**
     * テストケース名：test_02 delete Deactivates Room Without Active Reservation
     * テスト条件：削除対象客室に予約中データが存在しない。
     * テスト要望：予約中でない客室は論理削除できること。
     * テスト結果：削除 SQL が実行されること。
     */
    @DisplayName("test_02 delete Deactivates Room Without Active Reservation")
    @Test
    void deleteDeactivatesRoomWithoutActiveReservation() {
        RoomService roomService = new RoomService(roomMapper);
        when(roomMapper.countBookedReservations(1)).thenReturn(0);
        when(roomMapper.deactivate(1)).thenReturn(1);

        roomService.delete(1);

        verify(roomMapper).deactivate(1);
    }

    /**
     * テストケース名：test_03 restore Activates Deleted Room Normally
     * テスト条件：復元対象の論理削除済み部屋が存在する。
     * テスト要望：削除済み部屋は有効状態へ戻せること。
     * テスト結果：復元 SQL が実行されること。
     */
    @DisplayName("test_03 restore Activates Deleted Room Normally")
    @Test
    void restoreActivatesDeletedRoomNormally() {
        RoomService roomService = new RoomService(roomMapper);
        Room room = new Room();
        room.setId(1);
        room.setActive(false);
        when(roomMapper.findById(1)).thenReturn(room);
        when(roomMapper.restore(1)).thenReturn(1);

        roomService.restore(1);

        verify(roomMapper).restore(1);
    }

    /**
     * テストケース名：test_04 delete Permanently Rejects Room With Reservation History
     * テスト条件：完全削除対象の論理削除済み部屋に予約履歴が残っている。
     * テスト要望：履歴がある部屋は完全削除できないこと。
     * テスト結果：削除 SQL が実行されないこと。
     */
    @DisplayName("test_04 delete Permanently Rejects Room With Reservation History")
    @Test
    void deletePermanentlyRejectsRoomWithReservationHistory() {
        RoomService roomService = new RoomService(roomMapper);
        Room room = new Room();
        room.setId(1);
        room.setActive(false);
        when(roomMapper.findById(1)).thenReturn(room);
        when(roomMapper.countReservationsByRoomId(1)).thenReturn(1);

        assertThatThrownBy(() -> roomService.deletePermanently(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("予約履歴がある部屋は完全削除できません。");
        verify(roomMapper, never()).deletePermanently(1);
    }
}
