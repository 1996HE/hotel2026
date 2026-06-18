package com.example.minshuku.integration; // 実DBを使う料金ルールサービステストの所属パッケージ。

import static org.assertj.core.api.Assertions.assertThat; // AssertJ の通常断言を使う。
import static org.assertj.core.api.Assertions.assertThatThrownBy; // AssertJ の例外断言を使う。

import com.example.minshuku.domain.RoomPriceRule; // 料金ルールエンティティを使う。
import com.example.minshuku.mapper.RoomPriceRuleMapper; // 料金ルール Mapper を使う。
import com.example.minshuku.service.RoomPriceRuleService; // テスト対象の料金ルールサービスを使う。
import java.math.BigDecimal; // 金額比較に使う。
import java.time.LocalDate; // 日付指定に使う。
import java.util.List; // 一覧比較に使う。
import org.junit.jupiter.api.BeforeEach; // テスト前準備に使う。
import org.junit.jupiter.api.Test; // テスト定義に使う。
import org.springframework.beans.factory.annotation.Autowired; // DI に使う。
import org.springframework.boot.test.context.SpringBootTest; // Spring 全体を起動する。
import org.springframework.transaction.annotation.Transactional; // 各テストをロールバックする。

@SpringBootTest // 実DB付きで Spring コンテキストを起動する。
@Transactional // 各テストの変更をロールバックする。
class RoomPriceRuleServiceLocalDbTest extends LocalDbTestSupport { // 実DBで料金ルールサービスを検証する。
  @Autowired private RoomPriceRuleService roomPriceRuleService; // テスト対象の料金ルールサービスを注入する。
  @Autowired private RoomPriceRuleMapper roomPriceRuleMapper; // 結果確認用の料金ルール Mapper を注入する。

  @BeforeEach // 各テストの前に実行する。
  void setUp() { // 初期データを準備する。
    resetTables(); // 既存データを消す。
    seedRooms(); // 部屋データを投入する。
  }

  @Test // 正常系の料金ルール登録を検証する。
  void createPersistsRuleNormally() { // 重複しないルールは登録できる。
    RoomPriceRule rule = baseRule(ruleRoomId, "9月後半料金", LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 30), BigDecimal.valueOf(17000)); // ルール雛形を作る。
    roomPriceRuleService.create(rule); // 登録処理を実行する。
    List<RoomPriceRule> rules = roomPriceRuleService.findAllWithRoom(); // 一覧を取り出す。
    assertThat(rules).anySatisfy(saved -> { // 一覧の中身を確認する。
      assertThat(saved.getRuleName()).isEqualTo("9月後半料金"); // ルール名を確認する。
      assertThat(saved.getRoomNumber()).isEqualTo("106"); // 部屋番号を確認する。
      assertThat(saved.getPricePerPerson()).isEqualByComparingTo("17000"); // 単価を確認する。
    }); // 対象ルールが保存されたことを確認する。
  }

  @Test // 異常系の重複期間登録を検証する。
  void createRejectsOverlappingRule() { // 同じ部屋の重複期間は拒否される。
    insertPriceRule(ruleRoomId, "9月料金", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), BigDecimal.valueOf(16000), 1, true, "事前登録"); // 先に同期間ルールを入れる。
    RoomPriceRule rule = baseRule(ruleRoomId, "重複料金", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20), BigDecimal.valueOf(18000)); // 重複ルールを作る。
    assertThatThrownBy(() -> roomPriceRuleService.create(rule)) // 登録処理を実行して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("指定期間はすでに設定されています。"); // 重複エラーメッセージを確認する。
  }

  @Test // 正常系の単体削除を検証する。
  void deleteRemovesRuleNormally() { // 1件削除できることを確認する。
    int ruleId = insertPriceRule(ruleRoomId, "削除対象", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), BigDecimal.valueOf(15000), 1, true, "削除確認"); // 削除対象を作る。
    roomPriceRuleService.delete(ruleId); // 削除処理を実行する。
    assertThat(roomPriceRuleMapper.findAllWithRoom()).noneMatch(rule -> rule.getId() != null && rule.getId().intValue() == ruleId); // DBから消えたことを確認する。
  }

  @Test // 異常系の単体削除失敗を検証する。
  void deleteRejectsMissingRule() { // 存在しないIDは拒否される。
    assertThatThrownBy(() -> roomPriceRuleService.delete(9999)) // 存在しないIDで削除する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("料金ルールが見つかりません。"); // 不存在メッセージを確認する。
  }

  @Test // 正常系の一括削除を検証する。
  void deleteByIdsRemovesRulesNormally() { // 複数件削除できることを確認する。
    int firstId = insertPriceRule(ruleRoomId, "一括1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), BigDecimal.valueOf(15000), 1, true, "一括削除"); // 1件目を作る。
    int secondId = insertPriceRule(ruleRoomId, "一括2", LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 10), BigDecimal.valueOf(15500), 1, true, "一括削除"); // 2件目を作る。
    roomPriceRuleService.deleteByIds(List.of(firstId, secondId)); // 一括削除を実行する。
    assertThat(roomPriceRuleMapper.findAllWithRoom()).noneMatch(rule -> (rule.getId() != null && rule.getId().intValue() == firstId) || (rule.getId() != null && rule.getId().intValue() == secondId)); // 両方消えたことを確認する。
  }

  @Test // 異常系の一括削除空選択を検証する。
  void deleteByIdsRejectsEmptySelection() { // 何も選んでいない場合は拒否される。
    assertThatThrownBy(() -> roomPriceRuleService.deleteByIds(List.of())) // 空一覧で削除する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("料金ルールを1件以上選択してください。"); // 空選択メッセージを確認する。
  }

  private RoomPriceRule baseRule(Integer roomId, String ruleName, LocalDate startDate, LocalDate endDate, BigDecimal pricePerPerson) { // 料金ルール雛形を作る。
    RoomPriceRule rule = new RoomPriceRule(); // 料金ルールオブジェクトを作る。
    rule.setRoomId(roomId); // 部屋IDを設定する。
    rule.setRuleName(ruleName); // ルール名を設定する。
    rule.setStartDate(startDate); // 開始日を設定する。
    rule.setEndDate(endDate); // 終了日を設定する。
    rule.setPricePerPerson(pricePerPerson); // 単価を設定する。
    rule.setPriority(1); // 優先度を設定する。
    rule.setActive(true); // 有効状態を設定する。
    rule.setNote("DBテスト用"); // メモを設定する。
    return rule; // 雛形を返す。
  }
}
