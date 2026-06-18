package com.example.minshuku.mapper; // 宣言同行者 Mapper インターフェース所属パッケージ。

import com.example.minshuku.domain.ReservationGuest; // 読み込み同行者エンティティ型。
import org.apache.ibatis.annotations.Mapper; // 読み込み MyBatis Mapper 標记アノテーション。

@Mapper // 標记该インターフェース由 MyBatis 生成代理实现。
public interface ReservationGuestMapper { // 定義 reservation_guests テーブルのデータ访问インターフェース。
  int insert(ReservationGuest guest); // 新規登録予約同行者レコード。
}
