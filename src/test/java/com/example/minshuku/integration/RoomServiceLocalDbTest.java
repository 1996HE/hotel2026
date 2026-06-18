package com.example.minshuku.integration; // 実DBを使う部屋サービステストの所属パッケージ。

import static org.assertj.core.api.Assertions.assertThat; // AssertJ の通常断言を使う。
import static org.assertj.core.api.Assertions.assertThatThrownBy; // AssertJ の例外断言を使う。

import com.example.minshuku.domain.Room; // 部屋エンティティを使う。
import com.example.minshuku.mapper.RoomMapper; // 実DB参照用の部屋 Mapper を使う。
import com.example.minshuku.service.RoomService; // テスト対象の部屋サービスを使う。
import java.math.BigDecimal; // 金額設定に使う。
import java.util.List; // 一覧比較に使う。
import org.junit.jupiter.api.BeforeEach; // テスト前準備に使う。
import org.junit.jupiter.api.Test; // テスト定義に使う。
import org.springframework.beans.factory.annotation.Autowired; // DI に使う。
import org.springframework.boot.test.context.SpringBootTest; // Spring 全体を起動する。
import org.springframework.transaction.annotation.Transactional; // 各テストをロールバックする。

@SpringBootTest // 実DB付きで Spring コンテキストを起動する。
@Transactional // 各テストの変更をロールバックする。
class RoomServiceLocalDbTest extends LocalDbTestSupport { // 実DBで部屋サービスを検証する。
  @Autowired private RoomService roomService; // テスト対象の部屋サービスを注入する。
  @Autowired private RoomMapper roomMapper; // 結果確認用の部屋 Mapper を注入する。

  @BeforeEach // 各テストの前に実行する。
  void setUp() { // 初期データを準備する。
    resetTables(); // 既存データを消す。
    seedRooms(); // 部屋データを投入する。
  }

  @Test // 正常系の新規登録を検証する。
  void createPersistsRoomNormally() { // 新しい部屋をそのまま登録できることを確認する。
    Room room = new Room(); // 登録用の部屋オブジェクトを作る。
    room.setRoomNumber("201"); // 部屋番号を設定する。
    room.setRoomName("月の間"); // 部屋名を設定する。
    room.setRoomType("family"); // 部屋タイプを設定する。
    room.setCapacity(4); // 定員を設定する。
    room.setBasePricePerPerson(BigDecimal.valueOf(18000)); // 基本料金を設定する。
    room.setPrivateBath(true); // 露天風呂有無を設定する。
    room.setOccupancyStatus("vacant"); // 空室状態を設定する。
    room.setCleaningStatus("cleaned"); // 清掃状態を設定する。
    room.setActive(true); // 有効状態を設定する。
    room.setNote("テスト用の新規部屋"); // メモを設定する。
    roomService.create(room); // 実際に登録する。
    Room saved = roomMapper.findById(room.getId()); // DBから登録結果を取り出す。
    assertThat(saved).isNotNull(); // 保存されていることを確認する。
    assertThat(saved.getRoomNumber()).isEqualTo("201"); // 部屋番号が入っていることを確認する。
    assertThat(saved.getRoomName()).isEqualTo("月の間"); // 部屋名が入っていることを確認する。
    assertThat(saved.getActive()).isTrue(); // 有効状態が維持されていることを確認する。
    assertThat(roomService.countAll()).isEqualTo(6); // 部屋数が1件増えていることを確認する。
  }

  @Test // 異常系の重複登録を検証する。
  void createRejectsDuplicateActiveRoomNumber() { // 有効部屋番号の重複は拒否される。
    Room room = new Room(); // 登録用の部屋オブジェクトを作る。
    room.setRoomNumber("101"); // 既存の有効部屋番号を入れる。
    room.setRoomName("重複の間"); // 部屋名を設定する。
    room.setCapacity(2); // 定員を設定する。
    room.setBasePricePerPerson(BigDecimal.valueOf(10000)); // 基本料金を設定する。
    room.setActive(true); // 有効状態を設定する。
    assertThatThrownBy(() -> roomService.create(room)) // 登録処理を実行して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("部屋番号が重複しています。"); // 重複エラーメッセージを確認する。
  }

  @Test // 正常系の再有効化を検証する。
  void createReactivatesDeletedRoomWithSameRoomNumber() { // 休眠部屋を同番号で再利用できることを確認する。
    insertRoom("401", "旧月の間", "washitsu", 2, BigDecimal.valueOf(9000), false, "vacant", "cleaned", false, "無効化済み"); // 無効化済みの部屋を事前投入する。
    Room room = new Room(); // 再登録用の部屋オブジェクトを作る。
    room.setRoomNumber("401"); // 同じ部屋番号を指定する。
    room.setRoomName("新月の間"); // 新しい部屋名を入れる。
    room.setRoomType("family"); // 新しいタイプを入れる。
    room.setCapacity(3); // 新しい定員を入れる。
    room.setBasePricePerPerson(BigDecimal.valueOf(13000)); // 新しい基本料金を入れる。
    room.setPrivateBath(true); // 新しい属性を入れる。
    room.setOccupancyStatus("vacant"); // 空室状態を入れる。
    room.setCleaningStatus("cleaned"); // 清掃状態を入れる。
    room.setNote("再有効化"); // メモを入れる。
    roomService.create(room); // 再有効化処理を実行する。
    Room saved = roomMapper.findByRoomNumberIncludingInactive("401"); // 再取得して状態を確認する。
    assertThat(saved.getActive()).isTrue(); // 有効化されていることを確認する。
    assertThat(saved.getRoomName()).isEqualTo("新月の間"); // 内容が更新されていることを確認する。
    assertThat(saved.getCapacity()).isEqualTo(3); // 定員が更新されていることを確認する。
  }

  @Test // 正常系の状態更新を検証する。
  void updateStatusesUpdatesRoomStateNormally() { // 宿泊状態と清掃状態を同時に更新できることを確認する。
    roomService.updateStatuses(bookableRoomId, "occupied", "needs_cleaning"); // 状態更新を実行する。
    Room updated = roomMapper.findById(bookableRoomId); // 更新後の部屋を再取得する。
    assertThat(updated.getOccupancyStatus()).isEqualTo("occupied"); // 宿泊状態が反映されていることを確認する。
    assertThat(updated.getCleaningStatus()).isEqualTo("needs_cleaning"); // 清掃状態が反映されていることを確認する。
  }

  @Test // 正常系の削除を検証する。
  void deleteMarksRoomInactiveNormally() { // 削除で無効化されることを確認する。
    roomService.delete(bookableRoomId); // 削除処理を実行する。
    Room deleted = roomMapper.findById(bookableRoomId); // 削除後の部屋を再取得する。
    assertThat(deleted.getActive()).isFalse(); // 無効化されていることを確認する。
    assertThat(roomService.findInactive()).hasSize(2); // 無効化一覧に既存分と削除分が入ることを確認する。
    assertThat(roomService.findInactive()).extracting(Room::getId).contains(bookableRoomId, inactiveRoomId); // 対象部屋と既存無効化部屋が表示されることを確認する。
  }

  @Test // 一覧抽出の正常系を検証する。
  void findBookableReturnsOnlyVacantCleanedActiveRooms() { // 予約可能部屋だけが返ることを確認する。
    List<Room> bookableRooms = roomService.findBookable(); // 予約可能部屋を取得する。
    assertThat(bookableRooms).extracting(Room::getRoomNumber).containsExactly("101", "105", "106"); // 条件一致の部屋だけ返ることを確認する。
  }
}
