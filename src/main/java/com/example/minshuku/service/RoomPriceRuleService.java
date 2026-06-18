package com.example.minshuku.service; // 宣言料金ルール業務サービス所属パッケージ。

import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ型。
import com.example.minshuku.mapper.RoomPriceRuleMapper; // 読み込み料金ルール Mapper。
import java.math.BigDecimal; // 読み込み金額型用検証と比较。
import java.util.List; // 読み込み一覧返却型。
import java.util.Objects; // 読み込みオブジェクト工具用清理空值。
import org.springframework.stereotype.Service; // 読み込み Spring サービスアノテーション。
import org.springframework.util.StringUtils; // 読み込み字符串検証工具。

@Service // 標记このクラスに Spring 管理の業務サービス。
public class RoomPriceRuleService { // 部屋料金ルール関連の業務ロジックを定義。
  private final RoomPriceRuleMapper priceRuleMapper; // 保存料金ルールデータ访问依赖。

  public RoomPriceRuleService(RoomPriceRuleMapper priceRuleMapper) { // 定義構築メソッド用依赖注入。
    this.priceRuleMapper = priceRuleMapper; // 保存注入の料金ルール Mapper。
  }

  public List<RoomPriceRule> findAllWithRoom() { // 料金ルール全件検索を定義の業務メソッド。
    return priceRuleMapper.findAllWithRoom(); // 呼び出し Mapper 返却带部屋情報の料金ルール一覧。
  }

  public void create(RoomPriceRule rule) { // 料金ルール追加の業務メソッドを定義。
    validateRule(rule); // 执行料金ルール基本検証。
    if (priceRuleMapper.countOverlapping(rule.getRoomId(), rule.getStartDate(), rule.getEndDate()) > 0) { throw new IllegalArgumentException("指定期間はすでに設定されています。"); } // 阻止同部屋同期间重複設定ルール。
    priceRuleMapper.insert(rule); // 呼び出し Mapper 書き込み料金ルール。
  }

  public void delete(Integer id) { // 料金ルール単体削除の業務メソッドを定義。
    if (id == null) { throw new IllegalArgumentException("料金ルールを選択してください。"); } // 検証必须提供主キー。
    if (priceRuleMapper.delete(id) == 0) { throw new IllegalArgumentException("料金ルールが見つかりません。"); } // 検証削除オブジェクト必须存で。
  }

  public void deleteByIds(List<Integer> ids) { // 料金ルール一括削除の業務メソッドを定義。
    if (ids == null || ids.isEmpty()) { throw new IllegalArgumentException("料金ルールを1件以上選択してください。"); } // 検証必须至少選択一条ルール。
    List<Integer> targetIds = ids.stream().filter(Objects::nonNull).distinct().toList(); // 去除空值と合と重複番号。
    if (targetIds.isEmpty()) { throw new IllegalArgumentException("料金ルールを1件以上選択してください。"); } // 検証清理後仍必要至少一条ルール。
    if (priceRuleMapper.deleteByIds(targetIds) == 0) { throw new IllegalArgumentException("料金ルールが見つかりません。"); } // 検証必须至少削除一条レコード。
  }

  private void validateRule(RoomPriceRule rule) { // 定義料金ルール基本検証メソッド。
    if (rule.getRoomId() == null) { throw new IllegalArgumentException("部屋を選択してください。"); } // 検証必须選択部屋。
    if (!StringUtils.hasText(rule.getRuleName())) { throw new IllegalArgumentException("料金ルール名を入力してください。"); } // 検証ルール名称非能に空。
    if (rule.getStartDate() == null || rule.getEndDate() == null) { throw new IllegalArgumentException("開始日と終了日を入力してください。"); } // 検証日付范围非能に空。
    if (rule.getStartDate().isAfter(rule.getEndDate())) { throw new IllegalArgumentException("開始日は終了日以前にしてください。"); } // 検証開始日付非能晚于終了日付。
    if (rule.getPricePerPerson() == null || rule.getPricePerPerson().compareTo(BigDecimal.ZERO) < 0) { throw new IllegalArgumentException("料金は0円以上にしてください。"); } // 検証单价非能に负数。
    if (rule.getPriority() == null) { rule.setPriority(10); } // に缺失の优先级設定初期値值。
    if (rule.getActive() == null) { rule.setActive(true); } // に缺失の有効状態設定初期値值。
  }
}
