package com.example.minshuku.service;

import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客室別料金ルールの登録、重複期間チェック、削除を扱うサービス。
 * <p>
 * 料金は予約金額の根拠になるため、期間の重複や金額の不正値をここで必ず排除する。
 */
@Service
public class RoomPriceRuleService {
    private final RoomPriceRuleMapper priceRuleMapper;

    public RoomPriceRuleService(RoomPriceRuleMapper priceRuleMapper) {
        this.priceRuleMapper = priceRuleMapper;
    }

    @Transactional(readOnly = true)
    public List<RoomPriceRule> findAllWithRoom() {
        return priceRuleMapper.findAllWithRoom();
    }

    /**
     * 料金ルールを登録する。同一客室で期間が重複するルールは登録しない。
     */
    @Transactional
    public void create(RoomPriceRule rule) {
        // 料金ルールは、対象客室と対象期間が明確であることを前提に登録する。
        validateRule(rule);
        if (priceRuleMapper.countOverlapping(rule.getRoomId(), rule.getStartDate(), rule.getEndDate()) > 0) {
            // 同一客室・同一期間の競合を許さない。
            throw new IllegalArgumentException("指定期間はすでに設定されています。");
        }

        priceRuleMapper.insert(rule);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("料金ルールを選択してください。");
        }
        if (priceRuleMapper.delete(id) == 0) {
            throw new IllegalArgumentException("料金ルールが見つかりません。");
        }
    }

    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("料金ルールを1件以上選択してください。");
        }

        // 画面の重複選択や空値を除き、削除対象を一意化してからまとめて消す。
        List<Integer> targetIds = ids.stream().filter(Objects::nonNull).distinct().toList();

        if (targetIds.isEmpty()) {
            throw new IllegalArgumentException("料金ルールを1件以上選択してください。");
        }
        if (priceRuleMapper.deleteByIds(targetIds) == 0) {
            throw new IllegalArgumentException("料金ルールが見つかりません。");
        }
    }

    private void validateRule(RoomPriceRule rule) {
        // 料金計算の前提となる必須項目と金額範囲を検証する。
        if (rule.getRoomId() == null) {
            throw new IllegalArgumentException("部屋を選択してください。");
        }
        if (!StringUtils.hasText(rule.getRuleName())) {
            throw new IllegalArgumentException("料金ルール名を入力してください。");
        }
        if (rule.getStartDate() == null || rule.getEndDate() == null) {
            throw new IllegalArgumentException("開始日と終了日を入力してください。");
        }
        if (rule.getStartDate().isAfter(rule.getEndDate())) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください。");
        }
        if (rule.getPricePerPerson() == null || rule.getPricePerPerson().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("料金は0円以上にしてください。");
        }
        if (rule.getPriority() == null) {
            // 優先順位未指定は業務標準の10を使う。
            rule.setPriority(10);
        }
        if (rule.getActive() == null) {
            // 登録時は有効扱いにして、画面上ですぐ利用できる状態にする。
            rule.setActive(true);
        }
    }
}
