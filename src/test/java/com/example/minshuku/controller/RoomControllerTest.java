package com.example.minshuku.controller; // 宣言部屋ページ测试所属パッケージ。

import static org.hamcrest.Matchers.containsString; // 読み込み字符串パッケージ含断言工具。
import static org.mockito.ArgumentMatchers.any; // 読み込み Mockito 任意パラメータ匹配器。
import static org.mockito.Mockito.doThrow; // 読み込み Mockito 异常行に设定メソッド。
import static org.mockito.Mockito.verify; // 読み込み Mockito 呼び出し検証メソッド。
import static org.mockito.Mockito.when; // 読み込み Mockito 行に设定メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // 読み込み GET リクエスト构造メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // 読み込み POST リクエスト构造メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // 読み込みレスポンスコンテンツ断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash; // 読み込み flash 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model; // 読み込みモデル断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl; // 読み込みリダイレクト URL 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // 読み込み HTTP 状態断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view; // 読み込み視图名称断言メソッド。

import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ用构造ページデータ。
import com.example.minshuku.service.RoomService; // 読み込み被 mock の部屋サービス。
import java.math.BigDecimal; // 読み込み金額型用测试データ。
import java.util.List; // 読み込み一覧型用测试データ。
import org.junit.jupiter.api.Test; // 読み込み JUnit 测试アノテーション。
import org.springframework.beans.factory.annotation.Autowired; // 読み込み测试依赖注入アノテーション。
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // 読み込み MVC 切片测试アノテーション。
import org.springframework.boot.test.mock.mockito.MockBean; // 読み込み Spring Boot mock bean アノテーション。
import org.springframework.test.web.servlet.MockMvc; // 読み込み MockMvc 测试客户端。

@WebMvcTest(RoomController.class) // のみ加载部屋コントローラーと MVC 相关组件。
class RoomControllerTest { // 部屋ページコントローラーテストを定義。
  @Autowired private MockMvc mockMvc; // 注入 MockMvc 用模拟 HTTP リクエスト。
  @MockBean private RoomService roomService; // 注入部屋サービス mock。

  @Test // 標记正常テーブル示部屋管理ページの测试。
  void roomsPageShowsActiveAndDeletedRoomsNormally() throws Exception { // 测试部屋ページ正常渲染。
    when(roomService.findAll()).thenReturn(List.of(sampleRoom(true))); // 准备有効部屋一覧。
    when(roomService.findInactive()).thenReturn(List.of(sampleRoom(false))); // 准备削除部屋一覧。
    mockMvc.perform(get("/rooms")) // リクエスト部屋管理ページ。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("rooms")) // 断言返却 rooms 模板。
      .andExpect(model().attributeExists("rooms", "deletedRooms", "room")) // 断言モデルパッケージ含ページ必要要のデータ。
      .andExpect(content().string(containsString("部屋一覧"))) // 断言有効部屋一覧標題テーブル示。
      .andExpect(content().string(containsString("削除済み部屋一覧"))); // 断言削除部屋一覧標題テーブル示。
  }

  @Test // 標记部屋新规登録正常パス测试。
  void createRoomRedirectsWithSuccessMessage() throws Exception { // 测试部屋登録成功时リダイレクトとテーブル示成功メッセージ。
    mockMvc.perform(post("/rooms") // 送信部屋登録フォーム。
        .param("roomNumber", "501") // 設定部屋番号。
        .param("roomName", "雪の間") // 設定部屋名称。
        .param("roomType", "washitsu") // 設定部屋タイプ。
        .param("capacity", "2") // 設定定員。
        .param("basePricePerPerson", "12000") // 設定基本料金。
        .param("occupancyStatus", "vacant") // 設定宿泊状態。
        .param("cleaningStatus", "cleaned") // 設定清掃状態。
        .param("active", "true")) // 設定启用状態。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋を登録しました。")); // 断言成功メッセージ。
    verify(roomService).create(any(Room.class)); // 検証呼び出し部屋登録サービス。
  }

  @Test // 標记部屋新规登録業務エラーパス测试。
  void createRoomShowsValidationErrorWhenServiceRejects() throws Exception { // 测试業務検証失败时返却エラーメッセージ。
    doThrow(new IllegalArgumentException("部屋番号を入力してください。")).when(roomService).create(any(Room.class)); // 准备サービス抛出検証异常。
    mockMvc.perform(post("/rooms").param("roomNumber", "")) // 送信非完整部屋フォーム。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("error", "部屋番号を入力してください。")); // 断言エラーメッセージ。
  }

  @Test // 標记部屋新规登録システムエラーパス测试。
  void createRoomShowsGenericErrorWhenRuntimeExceptionOccurs() throws Exception { // 测试データベース重複等运行时エラーメッセージ。
    doThrow(new RuntimeException("duplicate")).when(roomService).create(any(Room.class)); // 准备サービス抛出运行时异常。
    mockMvc.perform(post("/rooms").param("roomNumber", "101")) // 送信重複部屋番号。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("error", "部屋登録に失敗しました。部屋番号が重複していないか確認してください。")); // 断言通用エラーメッセージ。
  }

  @Test // 標记部屋状態更新正常パス测试。
  void updateRoomStatusRedirectsWithSuccessMessage() throws Exception { // 测试部屋状態更新成功。
    mockMvc.perform(post("/rooms/1/statuses").param("occupancyStatus", "vacant").param("cleaningStatus", "cleaned")) // 送信部屋状態更新。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋ステータスを更新しました。")); // 断言成功メッセージ。
    verify(roomService).updateStatuses(1, "vacant", "cleaned"); // 検証呼び出し状態更新サービス。
  }

  @Test // 標记部屋削除正常パス测试。
  void deleteRoomRedirectsWithSuccessMessage() throws Exception { // 测试部屋削除成功。
    mockMvc.perform(post("/rooms/1/delete")) // 送信部屋削除リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/rooms")) // 断言リダイレクトへ部屋ページ。
      .andExpect(flash().attribute("message", "部屋を削除しました。")); // 断言成功メッセージ。
    verify(roomService).delete(1); // 検証呼び出し削除サービス。
  }

  private Room sampleRoom(boolean active) { // 定義构造部屋测试データのメソッド。
    Room room = new Room(); // 作成部屋オブジェクト。
    room.setId(active ? 1 : 2); // 設定部屋番号。
    room.setRoomNumber(active ? "101" : "102"); // 設定部屋号。
    room.setRoomName(active ? "桜の間" : "竹の間"); // 設定部屋名称。
    room.setRoomType("washitsu"); // 設定部屋タイプ。
    room.setCapacity(2); // 設定定員。
    room.setBasePricePerPerson(BigDecimal.valueOf(8800)); // 設定基本料金。
    room.setOccupancyStatus("vacant"); // 設定宿泊状態。
    room.setCleaningStatus("cleaned"); // 設定清掃状態。
    room.setActive(active); // 設定启用状態。
    return room; // 返却部屋オブジェクト。
  }
}
