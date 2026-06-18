package com.example.minshuku.mapper; // 宣言部屋 Mapper インターフェース所属パッケージ。

import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ型。
import java.util.List; // 読み込み一覧返却型。
import org.apache.ibatis.annotations.Mapper; // 読み込み MyBatis Mapper 標记アノテーション。
import org.apache.ibatis.annotations.Param; // 読み込み MyBatis パラメータ命名アノテーション。

@Mapper // 標记该インターフェース由 MyBatis 生成代理实现。
public interface RoomMapper { // 定義 rooms テーブルのデータ访问インターフェース。
  List<Room> findAll(); // 検索所有部屋と按部屋番号排序。
  List<Room> findInactive(); // 検索完了削除部屋と按部屋番号排序。
  List<Room> findActive(); // 検索仍で启用の部屋。
  List<Room> findBookable(); // 検索启用且空室の可能予約部屋。
  Room findById(@Param("id") Integer id); // 根据主キー検索单个部屋。
  Room findByRoomNumberIncludingInactive(@Param("roomNumber") String roomNumber); // 根据部屋番号検索パッケージ含完了削除部屋の记录。
  int insert(Room room); // 新規登録部屋记录。
  int reactivate(Room room); // 恢复完了削除部屋と更新基本信息。
  int updateStatuses(@Param("id") Integer id, @Param("occupancyStatus") String occupancyStatus, @Param("cleaningStatus") String cleaningStatus); // 更新宿泊と清掃状態。
  int deactivate(@Param("id") Integer id); // を部屋設定に無効来完成削除操作。
  int countAll(); // 集計全部部屋数量。
  int countVacant(); // 集計現在空房数量。
}
