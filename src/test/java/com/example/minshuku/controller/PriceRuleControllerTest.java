package com.example.minshuku.controller; // 宣言料金ページ测试所属パッケージ。

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
import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ用构造ページデータ。
import com.example.minshuku.service.RoomPriceRuleService; // 読み込み被 mock の料金サービス。
import com.example.minshuku.service.RoomService; // 読み込み被 mock の部屋サービス。
import java.math.BigDecimal; // 読み込み金額型用测试データ。
import java.time.LocalDate; // 読み込み日付型用测试データ。
import java.util.List; // 読み込み一覧型用测试データ。
import org.junit.jupiter.api.Test; // 読み込み JUnit 测试アノテーション。
import org.springframework.beans.factory.annotation.Autowired; // 読み込み测试依赖注入アノテーション。
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // 読み込み MVC 切片测试アノテーション。
import org.springframework.boot.test.mock.mockito.MockBean; // 読み込み Spring Boot mock bean アノテーション。
import org.springframework.test.web.servlet.MockMvc; // 読み込み MockMvc 测试客户端。

@WebMvcTest(PriceRuleController.class) // のみ加载料金コントローラーと MVC 相关组件。
class PriceRuleControllerTest { // 料金ページコントローラーテストを定義。
  @Autowired private MockMvc mockMvc; // 注入 MockMvc 用模拟 HTTP リクエスト。
  @MockBean private RoomPriceRuleService priceRuleService; // 注入料金サービス mock。
  @MockBean private RoomService roomService; // 注入部屋サービス mock。

  @Test // 標记正常テーブル示料金管理ページの测试。
  void pricesPageShowsRulesNormally() throws Exception { // 测试料金ページ正常渲染。
    when(priceRuleService.findAllWithRoom()).thenReturn(List.of(sampleRule())); // 准备料金ルール一覧。
    when(roomService.findActive()).thenReturn(List.of(sampleRoom())); // 准备有効部屋一覧。
    mockMvc.perform(get("/prices")) // リクエスト料金管理ページ。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("prices")) // 断言返却 prices 模板。
      .andExpect(model().attributeExists("rules", "rooms", "rule")) // 断言モデルデータ齐全。
      .andExpect(content().string(containsString("料金ルール一覧"))) // 断言料金ルール一覧標題テーブル示。
      .andExpect(content().string(containsString("夏料金"))); // 断言料金ルール名称テーブル示。
  }

  @Test // 標记料金ルール登録正常パス测试。
  void createPriceRuleRedirectsWithSuccessMessage() throws Exception { // 测试料金ルール登録成功。
    mockMvc.perform(post("/prices") // 送信料金ルールフォーム。
        .param("roomId", "1") // 設定部屋番号。
        .param("ruleName", "夏料金") // 設定ルール名称。
        .param("startDate", "2026-07-01") // 設定開始日付。
        .param("endDate", "2026-08-31") // 設定終了日付。
        .param("pricePerPerson", "12000") // 設定单价。
        .param("priority", "1") // 設定优先度。
        .param("active", "true")) // 設定启用状態。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクトへ料金ページ。
      .andExpect(flash().attribute("message", "料金ルールを登録しました。")); // 断言成功メッセージ。
    verify(priceRuleService).create(any(RoomPriceRule.class)); // 検証呼び出し料金ルール登録サービス。
  }

  @Test // 標记料金ルール登録エラーパス测试。
  void createPriceRuleShowsValidationErrorWhenServiceRejects() throws Exception { // 测试料金ルール検証失败时テーブル示エラー。
    doThrow(new IllegalArgumentException("開始日は終了日以前にしてください。")).when(priceRuleService).create(any(RoomPriceRule.class)); // 准备サービス抛出検証异常。
    mockMvc.perform(post("/prices").param("roomId", "1")) // 送信会被拒绝の料金ルール。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクトへ料金ページ。
      .andExpect(flash().attribute("error", "開始日は終了日以前にしてください。")); // 断言エラーメッセージ。
  }

  @Test // 標记料金ルール削除成功测试。
  void deletePriceRuleRedirectsWithSuccessMessage() throws Exception { // 测试单条削除成功。
    mockMvc.perform(post("/prices/10/delete")) // 送信削除リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクト回ページ。
      .andExpect(flash().attribute("message", "料金ルールを削除しました。")); // 断言成功メッセージ。
    verify(priceRuleService).delete(10); // 検証削除サービス被呼び出し。
  }

  @Test // 標记料金ルール旧削除パス兼容测试。
  void deletePriceRuleAlsoWorksOnLegacyPath() throws Exception { // 测试旧版削除パス也能正常工作。
    mockMvc.perform(post("/prices/10")) // 送信旧版削除リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクト回ページ。
      .andExpect(flash().attribute("message", "料金ルールを削除しました。")); // 断言成功メッセージ。
    verify(priceRuleService).delete(10); // 検証削除サービス被呼び出し。
  }

  @Test // 標记料金ルール一括削除成功测试。
  void deleteSelectedPriceRulesRedirectsWithSuccessMessage() throws Exception { // 测试一括削除成功。
    mockMvc.perform(post("/prices/delete-selected") // 送信一括削除リクエスト。
        .param("ids", "10", "11")) // 選択两条ルール番号。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクト回ページ。
      .andExpect(flash().attribute("message", "料金ルールを削除しました。")); // 断言成功メッセージ。
    verify(priceRuleService).deleteByIds(List.of(10, 11)); // 検証一括削除サービス被呼び出し。
  }

  @Test // 標记料金ルール一括削除空選択测试。
  void deleteSelectedPriceRulesShowsValidationErrorWhenNothingSelected() throws Exception { // 测试未選択ルール时のエラーメッセージ。
    doThrow(new IllegalArgumentException("料金ルールを1件以上選択してください。")).when(priceRuleService).deleteByIds(any()); // 准备サービス抛出検証异常。
    mockMvc.perform(post("/prices/delete-selected")) // 送信空の一括削除リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/prices")) // 断言リダイレクト回ページ。
      .andExpect(flash().attribute("error", "料金ルールを1件以上選択してください。")); // 断言エラーメッセージ。
  }

  private RoomPriceRule sampleRule() { // 定義构造料金ルール测试データのメソッド。
    RoomPriceRule rule = new RoomPriceRule(); // 作成料金ルールオブジェクト。
    rule.setRuleName("夏料金"); // 設定ルール名称。
    rule.setRoomNumber("101"); // 設定部屋番号。
    rule.setRoomName("桜の間"); // 設定部屋名称。
    rule.setStartDate(LocalDate.of(2026, 7, 1)); // 設定開始日付。
    rule.setEndDate(LocalDate.of(2026, 8, 31)); // 設定終了日付。
    rule.setPricePerPerson(BigDecimal.valueOf(12000)); // 設定单价。
    rule.setPriority(1); // 設定优先度。
    rule.setActive(true); // 設定启用状態。
    return rule; // 返却料金ルールオブジェクト。
  }

  private Room sampleRoom() { // 定義构造部屋测试データのメソッド。
    Room room = new Room(); // 作成部屋オブジェクト。
    room.setId(1); // 設定部屋主キー。
    room.setRoomNumber("101"); // 設定部屋番号。
    room.setRoomName("桜の間"); // 設定部屋名称。
    return room; // 返却部屋オブジェクト。
  }
}
