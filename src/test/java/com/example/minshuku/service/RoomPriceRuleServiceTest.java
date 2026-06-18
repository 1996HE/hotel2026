package com.example.minshuku.service; // 宣言料金ルールサービス测试所属パッケージ。

import static org.assertj.core.api.Assertions.assertThatThrownBy; // 読み込み异常断言工具。
import static org.mockito.ArgumentMatchers.any; // 読み込み任意パラメータ匹配器。
import static org.mockito.Mockito.never; // 読み込み未呼び出し検証工具。
import static org.mockito.Mockito.verify; // 読み込み呼び出し検証工具。
import static org.mockito.Mockito.when; // 読み込み行に设定工具。

import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ。
import com.example.minshuku.mapper.RoomPriceRuleMapper; // 読み込み料金ルール Mapper mock 型。
import java.math.BigDecimal; // 読み込み金額型。
import java.time.LocalDate; // 読み込み日付型。
import java.util.List; // 読み込み一覧型。
import org.junit.jupiter.api.BeforeEach; // 読み込み测试前置アノテーション。
import org.junit.jupiter.api.Test; // 読み込み测试アノテーション。
import org.junit.jupiter.api.extension.ExtendWith; // 読み込み Mockito 扩展アノテーション。
import org.mockito.Mock; // 読み込み mock アノテーション。
import org.mockito.junit.jupiter.MockitoExtension; // 読み込み Mockito JUnit 扩展。

@ExtendWith(MockitoExtension.class) // 启用 Mockito mock。
class RoomPriceRuleServiceTest { // 料金ルールサービステストを定義。
  @Mock private RoomPriceRuleMapper priceRuleMapper; // 作成料金ルール Mapper mock。
  private RoomPriceRuleService service; // 保存被测试サービス实例。

  @BeforeEach // 定義每个测试前执行の初始化。
  void setUp() { // 作成サービス实例。
    service = new RoomPriceRuleService(priceRuleMapper); // 用 mock 依赖初始化サービス。
  }

  @Test // 標记正常新規登録测试。
  void createPersistsRuleNormally() { // 测试新規登録料金ルールの正常流程。
    RoomPriceRule rule = sampleRule(); // 构造料金ルール入力。
    when(priceRuleMapper.countOverlapping(1, rule.getStartDate(), rule.getEndDate())).thenReturn(0); // 准备無重叠結果。
    service.create(rule); // 执行新規登録。
    verify(priceRuleMapper).insert(rule); // 検証执行插入。
  }

  @Test // 標记重複期间新規登録测试。
  void createRejectsOverlappingRule() { // 测试新規登録时发现日付冲突。
    RoomPriceRule rule = sampleRule(); // 构造料金ルール入力。
    when(priceRuleMapper.countOverlapping(1, rule.getStartDate(), rule.getEndDate())).thenReturn(1); // 准备重叠結果。
    assertThatThrownBy(() -> service.create(rule)) // 执行新規登録と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型。
      .hasMessage("指定期間はすでに設定されています。"); // 断言冲突メッセージ。
    verify(priceRuleMapper, never()).insert(any(RoomPriceRule.class)); // 断言非会書き込み新規登録。
  }

  @Test // 標记单条削除测试。
  void deleteRemovesRuleNormally() { // 测试削除单条料金ルールの正常流程。
    when(priceRuleMapper.delete(10)).thenReturn(1); // 准备削除成功結果。
    service.delete(10); // 执行削除。
    verify(priceRuleMapper).delete(10); // 検証削除呼び出し。
  }

  @Test // 標记削除失败测试。
  void deleteRejectsMissingRule() { // 测试削除非存でのルール。
    when(priceRuleMapper.delete(10)).thenReturn(0); // 准备削除失败結果。
    assertThatThrownBy(() -> service.delete(10)) // 执行削除と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型。
      .hasMessage("料金ルールが見つかりません。"); // 断言メッセージ信息。
  }

  @Test // 標记一括削除测试。
  void deleteByIdsRemovesRulesNormally() { // 测试一括削除料金ルールの正常流程。
    when(priceRuleMapper.deleteByIds(List.of(10, 11))).thenReturn(2); // 准备一括削除成功結果。
    service.deleteByIds(List.of(10, 11)); // 执行一括削除。
    verify(priceRuleMapper).deleteByIds(List.of(10, 11)); // 検証一括削除呼び出し。
  }

  @Test // 標记一括削除空選択测试。
  void deleteByIdsRejectsEmptySelection() { // 测试一括削除时未選択任何ルール。
    assertThatThrownBy(() -> service.deleteByIds(List.of())) // 执行空一覧削除と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型。
      .hasMessage("料金ルールを1件以上選択してください。"); // 断言メッセージ信息。
  }

  private RoomPriceRule sampleRule() { // 定義料金ルール测试データ构造メソッド。
    RoomPriceRule rule = new RoomPriceRule(); // 作成料金ルールオブジェクト。
    rule.setRoomId(1); // 設定部屋番号。
    rule.setRuleName("夏料金"); // 設定ルール名称。
    rule.setStartDate(LocalDate.of(2026, 7, 1)); // 設定開始日付。
    rule.setEndDate(LocalDate.of(2026, 8, 31)); // 設定終了日付。
    rule.setPricePerPerson(BigDecimal.valueOf(12000)); // 設定每人单价。
    rule.setPriority(1); // 設定优先度。
    rule.setActive(true); // 設定启用状態。
    return rule; // 返却构造好のルール。
  }
}
