package com.example.minshuku.mapper;

import com.example.minshuku.domain.Reservation;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 予約テーブルの検索・登録・状態更新を担当する MyBatis Mapper。
 * <p>
 * 予約番号の発番、状態遷移、一覧ページング、チェックアウト同期を担う。
 */
@Mapper
public interface ReservationMapper {
    /**
     * DB 側の現在日付を取得し、予約可否やチェックアウト判定の基準日に使う。
     */
    LocalDate currentDate();

    /**
     * 業務表示用予約番号の元になる連番を採番する。
     */
    long nextReservationSequence();

    /**
     * ダッシュボード向けの直近予約を取得する。
     */
    List<Reservation> findRecent();

    /**
     * 取消済み予約の直近データを取得する。
     */
    List<Reservation> findCancelled();

    /**
     * チェックアウト済み予約の直近データを取得する。
     */
    List<Reservation> findCheckedOut();

    /**
     * 予約一覧のページング表示用データを取得する。
     */
    List<Reservation> findRecentPage(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 取消済み予約一覧のページング表示用データを取得する。
     */
    List<Reservation> findCancelledPage(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * チェックアウト一覧のページング表示用データを取得する。
     */
    List<Reservation> findCheckedOutPage(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 予約一覧の総件数を取得する。
     */
    int countRecent();

    /**
     * 取消済み予約一覧の総件数を取得する。
     */
    int countCancelled();

    /**
     * チェックアウト一覧の総件数を取得する。
     */
    int countCheckedOut();

    /**
     * 宿泊終了日を過ぎ、チェックアウト同期が必要な予約を取得する。
     */
    List<Reservation> findDueCheckouts();

    /**
     * 状態更新や取消の対象予約を取得する。
     */
    Reservation findById(@Param("id") Integer id);

    /**
     * 検証済みの予約本体を登録する。
     */
    int insert(Reservation reservation);

    /**
     * 同一客室・重複宿泊期間の既存予約数を取得し、二重予約を防ぐ。
     */
    int countOverlapping(
            @Param("roomId") Integer roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * ダッシュボード集計用の予約中件数を取得する。
     */
    int countBooked();

    /**
     * 同一客室に残る他の予約中件数を取得し、客室状態の巻き戻し可否に使う。
     */
    int countOtherBookedByRoomId(
            @Param("roomId") Integer roomId,
            @Param("excludedReservationId") Integer excludedReservationId);

    /**
     * 入金状態のみを更新する。
     */
    int updatePaymentStatus(@Param("id") Integer id, @Param("paymentStatus") String paymentStatus);

    /**
     * 予約状態を予約済み・チェックアウト済み・取消済みへ更新する。
     */
    int updateReservationStatus(@Param("id") Integer id, @Param("reservationStatus") String reservationStatus);

    /**
     * 期限到来予約をチェックアウト済みにする。
     */
    int markCheckedOut(@Param("id") Integer id);

    /**
     * 予約を取消済みにする。
     */
    int cancel(@Param("id") Integer id);

    /**
     * 取消済み予約を一覧から完全削除する。
     */
    int deleteCancelled(@Param("id") Integer id);

    /**
     * チェックアウト済み予約を一覧から完全削除する。
     */
    int deleteCheckedOut(@Param("id") Integer id);
}
