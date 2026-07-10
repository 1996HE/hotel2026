package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import com.example.minshuku.service.RoomPriceRuleService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("料金ルールサービスDB連携")
/**
 * 料金ルールの登録、重複抑止、削除、一覧取得を確認する結合テスト。
 */
class RoomPriceRuleServiceLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private RoomPriceRuleService roomPriceRuleService;
    @Autowired
    private RoomPriceRuleMapper roomPriceRuleMapper;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
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
        RoomPriceRule rule = baseRule("late-september");
        roomPriceRuleService.create(rule);
        List<RoomPriceRule> rules = roomPriceRuleService.findAllWithRoom();
        assertThat(rules).anySatisfy(saved -> {
            assertThat(saved.getRuleName()).isEqualTo("9月後半料金");
            assertThat(saved.getRoomNumber()).isEqualTo("106");
            assertThat(saved.getPricePerPerson()).isEqualByComparingTo("17000");
        });
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
        var priceRule = TestSetData.priceRule("september");
        insertPriceRule(ruleRoomId, priceRule.ruleName(), priceRule.startDate(), priceRule.endDate(),
                priceRule.pricePerPerson(), priceRule.priority(), priceRule.active(), "事前登録");
        RoomPriceRule rule = baseRule("late-september");
        rule.setRuleName("重複料金");
        rule.setStartDate(LocalDate.of(2026, 9, 10));
        rule.setEndDate(LocalDate.of(2026, 9, 20));
        assertThatThrownBy(() -> roomPriceRuleService.create(rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("指定期間はすでに設定されています。");
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
        int ruleId = insertPriceRule(ruleRoomId, "削除対象", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                TestSetData.priceRule("september").pricePerPerson(), 1, true, "削除確認");
        roomPriceRuleService.delete(ruleId);
        assertThat(roomPriceRuleMapper.findAllWithRoom())
                .noneMatch(rule -> rule.getId() != null && rule.getId().intValue() == ruleId);
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
        assertThatThrownBy(() -> roomPriceRuleService.delete(9999))
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
        int firstId = insertPriceRule(ruleRoomId, "一括1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                TestSetData.priceRule("september").pricePerPerson(), 1, true, "一括削除");
        int secondId = insertPriceRule(ruleRoomId, "一括2", LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 10),
                TestSetData.priceRule("late-september").pricePerPerson(), 1, true, "一括削除");
        roomPriceRuleService.deleteByIds(List.of(firstId, secondId));
        assertThat(roomPriceRuleMapper.findAllWithRoom())
                .noneMatch(rule -> (rule.getId() != null && rule.getId().intValue() == firstId)
                        || (rule.getId() != null && rule.getId().intValue() == secondId));
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
        assertThatThrownBy(() -> roomPriceRuleService.deleteByIds(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("料金ルールを1件以上選択してください。");
    }

    /**
     * テストケース名：test_07 query Price Rules Compares Expected And Actual Results
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_07 query Price Rules Compares Expected And Actual Results")
    @Test
    void queryPriceRulesComparesExpectedAndActualResults() {
        var september = TestSetData.priceRule("september");
        var lateSeptember = TestSetData.priceRule("late-september");
        insertPriceRule(ruleRoomId, september.ruleName(), september.startDate(), september.endDate(),
                september.pricePerPerson(), september.priority(), september.active(), september.note());
        insertPriceRule(spareRoomId, lateSeptember.ruleName(), lateSeptember.startDate(), lateSeptember.endDate(),
                lateSeptember.pricePerPerson(), lateSeptember.priority(), lateSeptember.active(), lateSeptember.note());

        List<String> actualRules = roomPriceRuleService.findAllWithRoom().stream()
                .map(rule -> rule.getRoomNumber() + ":" + rule.getRuleName() + ":"
                        + amountText(rule.getPricePerPerson()))
                .toList();
        List<String> expectedRules = List.of("105:9月後半料金:17000", "106:9月料金:12000");
        printComparison("正常系検索：料金ルール一覧", expectedRules, actualRules);
        assertThat(actualRules).containsExactlyElementsOf(expectedRules);
    }

    /**
     * テストケース名：test_08 query Price Rules Returns Empty When No Data Exists
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_08 query Price Rules Returns Empty When No Data Exists")
    @Test
    void queryPriceRulesReturnsEmptyWhenNoDataExists() {
        List<String> actualRules = roomPriceRuleService.findAllWithRoom().stream()
                .map(RoomPriceRule::getRuleName)
                .toList();
        printComparison("範囲外データ：料金ルールなし", List.of(), actualRules);
        assertThat(actualRules).isEmpty();
    }

    /**
     * テストケース名：test_09 create Price Rule Rejects Negative Price As Abnormal Case
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_09 create Price Rule Rejects Negative Price As Abnormal Case")
    @Test
    void createPriceRuleRejectsNegativePriceAsAbnormalCase() {
        RoomPriceRule rule = baseRule("september");
        rule.setPricePerPerson(BigDecimal.valueOf(-1));
        String actualMessage = null;
        try {
            roomPriceRuleService.create(rule);
        } catch (IllegalArgumentException ex) {
            actualMessage = ex.getMessage();
        }
        printComparison("異常系：料金が負数", "料金は0円以上にしてください。", actualMessage);
        assertThat(actualMessage).isEqualTo("料金は0円以上にしてください。");
    }

    private RoomPriceRule baseRule(String key) {
        RoomPriceRule rule = TestSetData.priceRule(key).toDomain();
        rule.setRoomId(ruleRoomId);
        return rule;
    }

    private String amountText(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
