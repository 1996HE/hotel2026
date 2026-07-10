package com.example.minshuku.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@LoggedTest
@DisplayName("料金ルールサービス")
/**
 * 料金ルール登録時の重複抑止と削除処理を検証する単体テスト。
 */
class RoomPriceRuleServiceTest {
    @Mock
    private RoomPriceRuleMapper priceRuleMapper;
    private RoomPriceRuleService service;

    @BeforeEach
    void setUp() {
        service = new RoomPriceRuleService(priceRuleMapper);
    }

    /**
     * テストケース名：test_01 create Persists Rule Normally
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_01 create Persists Rule Normally")
    @Test
    void createPersistsRuleNormally() {
        RoomPriceRule rule = sampleRule();
        when(priceRuleMapper.countOverlapping(1, rule.getStartDate(), rule.getEndDate())).thenReturn(0);
        service.create(rule);
        verify(priceRuleMapper).insert(rule);
    }

    /**
     * テストケース名：test_02 create Rejects Overlapping Rule
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_02 create Rejects Overlapping Rule")
    @Test
    void createRejectsOverlappingRule() {
        RoomPriceRule rule = sampleRule();
        when(priceRuleMapper.countOverlapping(1, rule.getStartDate(), rule.getEndDate())).thenReturn(1);
        assertThatThrownBy(() -> service.create(rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("指定期間はすでに設定されています。");
        verify(priceRuleMapper, never()).insert(any(RoomPriceRule.class));
    }

  /**
   * テストケース名：test_03 delete Removes Rule Normally
   * テスト条件：削除または取消対象データを準備する。
   * テスト要望：対象データが削除・取消済みとして正しく処理されること。
   * テスト結果：処理結果が期待値と一致し、テストが成功すること。
   */
  @DisplayName("test_03 delete Removes Rule Normally")
  @Test
  void deleteRemovesRuleNormally() {
    when(priceRuleMapper.delete(10)).thenReturn(1);
    service.delete(10);
    verify(priceRuleMapper).delete(10);
  }

  /**
   * テストケース名：test_04 delete Rejects Missing Rule
   * テスト条件：削除または取消対象データを準備する。
   * テスト要望：対象データが削除・取消済みとして正しく処理されること。
   * テスト結果：期待したエラー、拒否結果、または空結果になること。
   */
  @DisplayName("test_04 delete Rejects Missing Rule")
  @Test
  void deleteRejectsMissingRule() {
    when(priceRuleMapper.delete(10)).thenReturn(0);
    assertThatThrownBy(() -> service.delete(10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("料金ルールが見つかりません。");
  }

  /**
   * テストケース名：test_05 delete By Ids Removes Rules Normally
   * テスト条件：削除または取消対象データを準備する。
   * テスト要望：対象データが削除・取消済みとして正しく処理されること。
   * テスト結果：処理結果が期待値と一致し、テストが成功すること。
   */
  @DisplayName("test_05 delete By Ids Removes Rules Normally")
  @Test
  void deleteByIdsRemovesRulesNormally() {
    when(priceRuleMapper.deleteByIds(List.of(10, 11))).thenReturn(2);
    service.deleteByIds(List.of(10, 11));
    verify(priceRuleMapper).deleteByIds(List.of(10, 11));
  }

    /**
     * テストケース名：test_06 delete By Ids Rejects Empty Selection
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_06 delete By Ids Rejects Empty Selection")
    @Test
    void deleteByIdsRejectsEmptySelection() {
        assertThatThrownBy(() -> service.deleteByIds(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("料金ルールを1件以上選択してください。");
    }

    private RoomPriceRule sampleRule() {
        return TestSetData.priceRule("summer").toDomain();
    }
}
