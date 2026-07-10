package com.example.minshuku.mapper;

import com.example.minshuku.domain.RoomPriceRule;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客室別料金ルールテーブルの検索・登録・削除を担当する MyBatis Mapper。
 * <p>
 * 日別料金計算の元データになるため、重複期間の判定と最適ルール検索を提供する。
 */
@Mapper
public interface RoomPriceRuleMapper {
    /**
     * 料金管理画面に表示するため、客室情報付きでルールを取得する。
     */
    List<RoomPriceRule> findAllWithRoom();

    /**
     * 指定宿泊日に適用する最優先の料金ルールを取得する。
     */
    RoomPriceRule findBestRule(@Param("roomId") Integer roomId, @Param("stayDate") LocalDate stayDate);

    /**
     * 検証済みの料金ルールを登録する。
     */
    int insert(RoomPriceRule rule);

    /**
     * 指定した料金ルールを無効化する。
     */
    int delete(@Param("id") Integer id);

    /**
     * 一覧画面で選択された料金ルールをまとめて無効化する。
     */
    int deleteByIds(@Param("ids") List<Integer> ids);

    /**
     * 同一客室で期間が重なる料金ルール数を取得する。
     */
    int countOverlapping(
            @Param("roomId") Integer roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
