package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@LoggedTest
@DisplayName("部屋サービスDB連携")
/**
 * 客室の登録、再有効化、論理削除、状態更新を確認する結合テスト。
 */
class RoomServiceLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomMapper roomMapper;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
    }

    /**
     * テストケース名：test_01 create Persists Room Normally
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_01 create Persists Room Normally")
    @Test
    void createPersistsRoomNormally() {
        Room room = TestSetData.room("new-room").toDomain();
        roomService.create(room);
        Room saved = roomMapper.findById(room.getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getRoomNumber()).isEqualTo(room.getRoomNumber());
        assertThat(saved.getRoomName()).isEqualTo(room.getRoomName());
        assertThat(saved.getActive()).isTrue();
        assertThat(roomService.countAll()).isEqualTo(6);
    }

    /**
     * テストケース名：test_02 create Rejects Duplicate Active Room Number
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_02 create Rejects Duplicate Active Room Number")
    @Test
    void createRejectsDuplicateActiveRoomNumber() {
        Room room = new Room();
        room.setRoomNumber("101");
        room.setRoomName("重複の間");
        room.setCapacity(2);
        room.setBasePricePerPerson(BigDecimal.valueOf(10000));
        room.setActive(true);
        assertThatThrownBy(() -> roomService.create(room))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("部屋番号が重複しています。");
    }

    /**
     * テストケース名：test_03 create Reactivates Deleted Room With Same Room Number
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_03 create Reactivates Deleted Room With Same Room Number")
    @Test
    void createReactivatesDeletedRoomWithSameRoomNumber() {
        insertRoom("401", "旧月の間", "washitsu", 2, BigDecimal.valueOf(9000), false, "vacant", "cleaned", false, "無効化済み");
        Room room = new Room();
        room.setRoomNumber("401");
        room.setRoomName("新月の間");
        room.setRoomType("family");
        room.setCapacity(3);
        room.setBasePricePerPerson(BigDecimal.valueOf(13000));
        room.setPrivateBath(true);
        room.setOccupancyStatus("vacant");
        room.setCleaningStatus("cleaned");
        room.setNote("再有効化");
        roomService.create(room);
        Room saved = roomMapper.findByRoomNumberIncludingInactive("401");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getRoomName()).isEqualTo("新月の間");
        assertThat(saved.getCapacity()).isEqualTo(3);
    }

    /**
     * テストケース名：test_04 update Statuses Updates Room State Normally
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_04 update Statuses Updates Room State Normally")
    @Test
    void updateStatusesUpdatesRoomStateNormally() {
        roomService.updateStatuses(bookableRoomId, "occupied", "needs_cleaning");
        Room updated = roomMapper.findById(bookableRoomId);
        assertThat(updated.getOccupancyStatus()).isEqualTo("occupied");
        assertThat(updated.getCleaningStatus()).isEqualTo("needs_cleaning");
    }

    /**
     * テストケース名：test_05 delete Marks Room Inactive Normally
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_05 delete Marks Room Inactive Normally")
    @Test
    void deleteMarksRoomInactiveNormally() {
        roomService.delete(bookableRoomId);
        Room deleted = roomMapper.findById(bookableRoomId);
        assertThat(deleted.getActive()).isFalse();
        assertThat(roomService.findInactive()).hasSize(2);
        assertThat(roomService.findInactive()).extracting(Room::getId).contains(bookableRoomId, inactiveRoomId);
    }

    /**
     * テストケース名：test_06 restore Reactivates Deleted Room Normally
     * テスト条件：復元対象の論理削除済み部屋を準備する。
     * テスト要望：削除済み一覧から復元できること。
     * テスト結果：対象部屋が有効化されること。
     */
    @DisplayName("test_06 restore Reactivates Deleted Room Normally")
    @Test
    void restoreReactivatesDeletedRoomNormally() {
        roomService.restore(inactiveRoomId);
        Room restored = roomMapper.findById(inactiveRoomId);
        assertThat(restored.getActive()).isTrue();
        assertThat(roomService.findInactive()).extracting(Room::getId).doesNotContain(inactiveRoomId);
        assertThat(roomService.findAll()).extracting(Room::getId).contains(inactiveRoomId);
    }

    /**
     * テストケース名：test_07 delete Permanently Removes Deleted Room Normally
     * テスト条件：予約履歴のない論理削除済み部屋を準備する。
     * テスト要望：削除済み一覧から完全に消せること。
     * テスト結果：対象部屋がDBから消えること。
     */
    @DisplayName("test_07 delete Permanently Removes Deleted Room Normally")
    @Test
    void deletePermanentlyRemovesDeletedRoomNormally() {
        roomService.deletePermanently(inactiveRoomId);
        Room removed = roomMapper.findById(inactiveRoomId);
        assertThat(removed).isNull();
        assertThat(roomService.findInactive()).extracting(Room::getId).doesNotContain(inactiveRoomId);
    }

    /**
     * テストケース名：test_08 find Bookable Returns Only Vacant Cleaned Active Rooms
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_08 find Bookable Returns Only Vacant Cleaned Active Rooms")
    @Test
    void findBookableReturnsOnlyVacantCleanedActiveRooms() {
        List<Room> bookableRooms = roomService.findBookable();
        assertThat(bookableRooms).extracting(Room::getRoomNumber).containsExactly("101", "105", "106");
    }

    /**
     * テストケース名：test_09 query Rooms Compares Expected And Actual Results
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_09 query Rooms Compares Expected And Actual Results")
    @Test
    void queryRoomsComparesExpectedAndActualResults() {
        List<String> expectedActiveRooms = List.of("101", "102", "103", "105", "106");
        List<String> actualActiveRooms = roomService.findAll().stream()
                .map(Room::getRoomNumber)
                .toList();
        printComparison("正常系検索：有効部屋番号一覧", expectedActiveRooms, actualActiveRooms);
        assertThat(actualActiveRooms).containsExactlyElementsOf(expectedActiveRooms);

        List<String> expectedInactiveRooms = List.of("104");
        List<String> actualInactiveRooms = roomService.findInactive().stream()
                .map(Room::getRoomNumber)
                .toList();
        printComparison("正常系検索：無効部屋番号一覧", expectedInactiveRooms, actualInactiveRooms);
        assertThat(actualInactiveRooms).containsExactlyElementsOf(expectedInactiveRooms);

        printComparison("正常系検索：有効部屋数", 5, roomService.countAll());
        assertThat(roomService.countAll()).isEqualTo(5);
        printComparison("正常系検索：空室部屋数", 4, roomService.countVacant());
        assertThat(roomService.countVacant()).isEqualTo(4);
    }

    /**
     * テストケース名：test_10 query Room By Missing Id Returns Null For Out Of Range Data
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_10 query Room By Missing Id Returns Null For Out Of Range Data")
    @Test
    void queryRoomByMissingIdReturnsNullForOutOfRangeData() {
        Room actualRoom = roomService.findById(9999);
        printComparison("範囲外データ：存在しない部屋ID", null, actualRoom);
        assertThat(actualRoom).isNull();
    }

    /**
     * テストケース名：test_11 update Statuses Rejects Invalid Status As Abnormal Case
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_11 update Statuses Rejects Invalid Status As Abnormal Case")
    @Test
    void updateStatusesRejectsInvalidStatusAsAbnormalCase() {
        String actualMessage = null;
        try {
            roomService.updateStatuses(bookableRoomId, "broken", "cleaned");
        } catch (IllegalArgumentException ex) {
            actualMessage = ex.getMessage();
        }
        printComparison("異常系：不正な宿泊状態", "部屋の宿泊状態が正しくありません。", actualMessage);
        assertThat(actualMessage).isEqualTo("部屋の宿泊状態が正しくありません。");
    }
}
