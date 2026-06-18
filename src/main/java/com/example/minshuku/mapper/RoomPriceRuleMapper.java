package com.example.minshuku.mapper; // 宣言料金ルール Mapper インターフェース所属パッケージ。

import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ型。
import java.time.LocalDate; // 読み込み日付パラメータ型。
import java.util.List; // 読み込み一覧パラメータ型。
import org.apache.ibatis.annotations.Mapper; // 読み込み MyBatis Mapper 標记アノテーション。
import org.apache.ibatis.annotations.Param; // 読み込み MyBatis パラメータ命名アノテーション。

@Mapper // 標记该インターフェース由 MyBatis 生成代理实现。
public interface RoomPriceRuleMapper { // 定義 room_price_rules テーブルのデータ访问インターフェース。
  List<RoomPriceRule> findAllWithRoom(); // 検索料金ルールと結合带出部屋情報。
  RoomPriceRule findBestRule(@Param("roomId") Integer roomId, @Param("stayDate") LocalDate stayDate); // 検索某部屋某日付命中の最高优先级料金ルール。
  int insert(RoomPriceRule rule); // 新規登録料金ルールレコード。
  int delete(@Param("id") Integer id); // 削除单条料金ルールレコード。
  int deleteByIds(@Param("ids") List<Integer> ids); // 一括削除选中の料金ルールレコード。
  int countOverlapping(@Param("roomId") Integer roomId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate); // 集計同部屋日付重叠の料金ルール件数。
}
