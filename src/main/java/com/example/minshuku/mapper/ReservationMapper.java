package com.example.minshuku.mapper; // 宣言予約 Mapper インターフェース所属パッケージ。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ型。
import java.time.LocalDate; // 読み込み日付パラメータ型。
import java.util.List; // 読み込み一覧返却型。
import org.apache.ibatis.annotations.Mapper; // 読み込み MyBatis Mapper 標记アノテーション。
import org.apache.ibatis.annotations.Param; // 読み込み MyBatis パラメータ命名アノテーション。

@Mapper // 標记该インターフェース由 MyBatis 生成代理实现。
public interface ReservationMapper { // 定義 reservations テーブルのデータ访问インターフェース。
  List<Reservation> findRecent(); // 検索近期予約と結合带出部屋情報。
  List<Reservation> findCancelled(); // 検索完了取消予約と結合带出部屋情報。
  List<Reservation> findCheckedOut(); // 検索完了チェックアウト予約と結合带出部屋情報。
  List<Reservation> findRecentPage(@Param("limit") int limit, @Param("offset") int offset); // ページング検索近期予約。
  List<Reservation> findCancelledPage(@Param("limit") int limit, @Param("offset") int offset); // ページング検索取消予約。
  List<Reservation> findCheckedOutPage(@Param("limit") int limit, @Param("offset") int offset); // ページング検索完了チェックアウト予約。
  int countRecent(); // 集計近期有効予約件数。
  int countCancelled(); // 集計取消予約件数。
  int countCheckedOut(); // 集計完了チェックアウト予約件数。
  List<Reservation> findDueCheckouts(); // 検索へ期必要要自動チェックアウトの予約。
  Reservation findById(@Param("id") Integer id); // 根据主キー検索单个予約。
  int insert(Reservation reservation); // 新規登録予約レコード。
  int countOverlapping(@Param("roomId") Integer roomId, @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate); // 集計同部屋日付重叠の有効予約件数。
  int countBooked(); // 集計現在有効予約件数。
  int updatePaymentStatus(@Param("id") Integer id, @Param("paymentStatus") String paymentStatus); // 更新予約支払い状態。
  int updateReservationStatus(@Param("id") Integer id, @Param("reservationStatus") String reservationStatus); // 更新予約状態。
  int markCheckedOut(@Param("id") Integer id); // を予約標记にチェックアウト完成。
  int cancel(@Param("id") Integer id); // を予約状態改に取消。
}
