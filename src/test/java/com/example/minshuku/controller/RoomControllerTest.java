package com.example.minshuku.controller; // 宣言部屋ページテスト所属パッケージ。

import static org.hamcrest.Matchers.containsString; // 読み込み字符串パッケージ含断言工具。
import static org.mockito.ArgumentMatchers.any; // 読み込み Mockito 任意パラメータ匹配器。
import static org.mockito.Mockito.doThrow; // 読み込み Mockito 例外行に设定メソッド。
import static org.mockito.Mockito.verify; // 読み込み Mockito 呼び出し検証メソッド。
import static org.mockito.Mockito.when; // 読み込み Mockito 行に设定メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // 読み込み GET リクエスト構築メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // 読み込み POST リクエスト構築メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // 読み込みレスポンスコンテンツ断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash; // 読み込み flash 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model; // 読み込みモデル断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl; // 読み込みリダイレクト URL 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // 読み込み HTTP 状態断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view; // 読み込み視图名称断言メソッド。

import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ用構築ページデータ。
import com.example.minshuku.service.RoomService; // 読み込み被 mock の部屋サービス。
import java.math.BigDecimal; // 読み込み金額型用テストデータ。
import java.util.List; // 読み込み一覧型用テストデータ。
import org.junit.jupiter.api.Test; // 読み込み JUnit テストアノテーション。
import org.springframework.beans.factory.annotation.Autowired; // 読み込みテスト依赖注入アノテーション。
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // 読み込み MVC 切り出しテストアノテーション。
import org.springframework.boot.test.mock.mockito.MockBean; // 読み込み Spring Boot mock bean アノテーション。
import org.springframework.test.web.servlet.MockMvc; // 読み込み MockMvc テスト宿泊者端。

@WebMvcTest(RoomController.class) // のみ加载部屋コントローラーと MVC 相关组件。
class RoomControllerTest { // 部屋ページコントローラーテストを定義。
  @Autowired private MockMvc mockMvc; // 注入 MockMvc 用模拟 HTTP リクエスト。
  @MockBean private RoomService roomService; // 注入部屋サービス mock。

  @Test // 標记正常表示部屋管理ページのテスト。
  void roomsPageShowsActiveAndDeletedRoomsNormally() throws Exception { // テスト部屋ページ正常渲染。
    when(roomService.findAll()).thenReturn(List.of(sampleRoom(true))); // 准备有効部屋一覧。
    when(roomService.findInactive()).thenReturn(List.of(sampleRoom(false))); // 准备削除部屋一覧。
    mockMvc.perform(get("/rooms")) // リクエスト部屋管理ページ。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("rooms")) // 断言返却 rooms 模板。
      .andExpect(model().attributeExists("rooms", "deletedRooms", "room")) // 断言モデルパッケージ含ページ必要要のデータ。
      .andExpect(content().string(containsString("部屋一覧"))) // 断言有効部屋一覧標題表示。
      .andExpect(content().string(containsString("削除済み部屋一覧"))); // 断言削除部屋一覧標題表示。
  }

  @Test // 標记部屋新規登録正常パステスト。
  void createRoomRedirectsWithSuccessMessage() throws Exception { // テスト部屋登録成功时リダイレクトと表示成功メッセージ。
    mockMvc.perform(post("/rooms") // 送信部屋登録フォーム。
        .param("roomNumber", "501") // 設定部屋番号。
        .param("roomName", "雪の間") // 設定部屋名称。
        .param("roomType", "washitsu") // 設定部屋タイプ。
        .param("capacity", "2") // 設定定員。
        .param("basePricePerPerson", "12000") // 設定基本料金。
        .param("occupancyStatus", "vacant") // 設定宿泊状態。
        .param("cleaningStatus", "cleaned") // 設定清掃状態。
        .param("active", "true")) // 設定有効状態。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋を登録しました。")); // 断言成功メッセージ。
    verify(roomService).create(any(Room.class)); // 検証呼び出し部屋登録サービス。
  }

  @Test // 標记部屋新規登録業務エラーパステスト。
  void createRoomShowsValidationErrorWhenServiceRejects() throws Exception { // テスト業務検証失败时返却エラーメッセージ。
    doThrow(new IllegalArgumentException("部屋番号を入力してください。")).when(roomService).create(any(Room.class)); // 准备サービス抛出検証例外。
    mockMvc.perform(post("/rooms").param("roomNumber", "")) // 送信非完整部屋フォーム。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("error", "部屋番号を入力してください。")); // 断言エラーメッセージ。
  }

  @Test // 標记部屋新規登録システムエラーパステスト。
  void createRoomShowsGenericErrorWhenRuntimeExceptionOccurs() throws Exception { // テストデータベース重複等起動时エラーメッセージ。
    doThrow(new RuntimeException("duplicate")).when(roomService).create(any(Room.class)); // 准备サービス抛出起動时例外。
    mockMvc.perform(post("/rooms").param("roomNumber", "101")) // 送信重複部屋番号。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("error", "部屋登録に失敗しました。部屋番号が重複していないか確認してください。")); // 断言通用エラーメッセージ。
  }

  @Test // 標记部屋状態更新正常パステスト。
  void updateRoomStatusRedirectsWithSuccessMessage() throws Exception { // テスト部屋状態更新成功。
    mockMvc.perform(post("/rooms/1/statuses").param("occupancyStatus", "vacant").param("cleaningStatus", "cleaned")) // 送信部屋状態更新。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋ステータスを更新しました。")); // 断言成功メッセージ。
    verify(roomService).updateStatuses(1, "vacant", "cleaned"); // 検証呼び出し状態更新サービス。
  }

  @Test // 標记部屋削除正常パステスト。
  void deleteRoomRedirectsWithSuccessMessage() throws Exception { // テスト部屋削除成功。
    mockMvc.perform(post("/rooms/1/delete")) // 送信部屋削除リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋を削除しました。")); // 断言成功メッセージ。
    verify(roomService).delete(1); // 検証呼び出し削除サービス。
  }

  private Room sampleRoom(boolean active) { // 定義構築部屋テストデータのメソッド。
    Room room = new Room(); // 作成部屋オブジェクト。
    room.setId(active ? 1 : 2); // 設定部屋番号。
    room.setRoomNumber(active ? "101" : "102"); // 設定部屋号。
    room.setRoomName(active ? "桜の間" : "竹の間"); // 設定部屋名称。
    room.setRoomType("washitsu"); // 設定部屋タイプ。
    room.setCapacity(2); // 設定定員。
    room.setBasePricePerPerson(BigDecimal.valueOf(8800)); // 設定基本料金。
    room.setOccupancyStatus("vacant"); // 設定宿泊状態。
    room.setCleaningStatus("cleaned"); // 設定清掃状態。
    room.setActive(active); // 設定有効状態。
    return room; // 返却部屋オブジェクト。
  }
}
