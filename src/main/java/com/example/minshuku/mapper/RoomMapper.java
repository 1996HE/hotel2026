package com.example.minshuku.mapper;

import com.example.minshuku.domain.Room;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客室テーブルの検索・登録・ステータス更新を担当する MyBatis Mapper。
 * <p>
 * 予約可否や清掃状態の判定で広く使うため、状態取得と更新を集約する。
 */
@Mapper
public interface RoomMapper {
    /**
     * 有効客室を部屋一覧に表示する。
     */
    List<Room> findAll();

    /**
     * 論理削除済み客室を管理画面の削除済み一覧に表示する。
     */
    List<Room> findInactive();

    /**
     * 料金ルール登録など、有効客室だけを選択肢に出す場面で使う。
     */
    List<Room> findActive();

    /**
     * 予約登録時に選択できる「空室かつ清掃済み」の客室を取得する。
     */
    List<Room> findBookable();

    /**
     * 通常参照用に客室を1件取得する。
     */
    Room findById(@Param("id") Integer id);

    /**
     * 予約登録中の並行更新を防ぐため、対象客室をロックして取得する。
     */
    Room findByIdForUpdate(@Param("id") Integer id);

    /**
     * 重複登録判定と論理削除済み客室の再有効化判定に使う。
     */
    Room findByRoomNumberIncludingInactive(@Param("roomNumber") String roomNumber);

    /**
     * 新規客室を登録する。
     */
    int insert(Room room);

    /**
     * 論理削除済みの同一客室番号を再利用可能な状態へ戻す。
     */
    int reactivate(Room room);

    /**
     * 宿泊状態と清掃状態を業務上の1単位として更新する。
     */
    int updateStatuses(
            @Param("id") Integer id,
            @Param("occupancyStatus") String occupancyStatus,
            @Param("cleaningStatus") String cleaningStatus);

    /**
     * 客室を物理削除せず無効化する。
     */
    int deactivate(@Param("id") Integer id);

    /**
     * 有効予約が残る客室の削除を防ぐため、予約中件数を取得する。
     */
    int countBookedReservations(@Param("id") Integer id);

    /**
     * 完全削除前に、予約履歴が残っていないかを確認する。
     */
    int countReservationsByRoomId(@Param("id") Integer id);

    /**
     * 論理削除済みの部屋を有効状態へ戻す。
     */
    int restore(@Param("id") Integer id);

    /**
     * 予約履歴が残っていない論理削除済みの部屋を完全削除する。
     */
    int deletePermanently(@Param("id") Integer id);

    /**
     * ダッシュボード表示用の有効客室数を取得する。
     */
    int countAll();

    /**
     * ダッシュボード表示用の空室数を取得する。
     */
    int countVacant();
}
