package com.example.minshuku.service; // 宣言部屋業務サービス所属パッケージ。

import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ型。
import com.example.minshuku.mapper.RoomMapper; // 読み込み部屋 Mapper。
import java.math.BigDecimal; // 読み込み金額フィールド初期値值型。
import java.util.List; // 読み込み一覧返却型。
import org.springframework.stereotype.Service; // 読み込み Spring サービスアノテーション。
import org.springframework.util.StringUtils; // 読み込み字符串検証工具。

@Service // 標记このクラスに Spring 管理の業務サービス。
public class RoomService { // 定義部屋相关業務逻辑。
  private final RoomMapper roomMapper; // 保存部屋データ访问依赖。

  public RoomService(RoomMapper roomMapper) { // 定義构造メソッド用依赖注入。
    this.roomMapper = roomMapper; // 保存注入の部屋 Mapper。
  }

  public List<Room> findAll() { // 定義検索所有部屋の業務メソッド。
    return roomMapper.findAll(); // 呼び出し Mapper 返却部屋一覧。
  }

  public List<Room> findInactive() { // 定義検索完了削除部屋の業務メソッド。
    return roomMapper.findInactive(); // 呼び出し Mapper 返却完了削除部屋一覧。
  }

  public List<Room> findActive() { // 定義検索启用部屋の業務メソッド。
    return roomMapper.findActive(); // 呼び出し Mapper 返却启用部屋一覧。
  }

  public List<Room> findBookable() { // 定義検索可能予約部屋の業務メソッド。
    return roomMapper.findBookable(); // 呼び出し Mapper 返却启用且空室の部屋一覧。
  }

  public Room findById(Integer id) { // 定義按番号検索部屋の業務メソッド。
    return roomMapper.findById(id); // 呼び出し Mapper 返却单个部屋。
  }

  public void create(Room room) { // 定義新規登録部屋の業務メソッド。
    if (!StringUtils.hasText(room.getRoomNumber())) { throw new IllegalArgumentException("房間番号を入力してください。"); } // 検証部屋番号非能に空。
    if (!StringUtils.hasText(room.getRoomName())) { throw new IllegalArgumentException("部屋名を入力してください。"); } // 検証部屋名称非能に空。
    if (room.getCapacity() == null || room.getCapacity() < 1) { throw new IllegalArgumentException("定員は1名以上にしてください。"); } // 検証宿泊人数上限。
    if (room.getBasePricePerPerson() == null) { room.setBasePricePerPerson(BigDecimal.ZERO); } // に缺失の基本料金設定初期値值。
    if (!StringUtils.hasText(room.getRoomType())) { room.setRoomType("washitsu"); } // に缺失の部屋タイプ設定初期値值。
    if (room.getPrivateBath() == null) { room.setPrivateBath(false); } // に缺失の独立浴室標记設定初期値值。
    if (!StringUtils.hasText(room.getOccupancyStatus())) { room.setOccupancyStatus("vacant"); } // に缺失の宿泊状態設定初期値值。
    if (!StringUtils.hasText(room.getCleaningStatus())) { room.setCleaningStatus("cleaned"); } // に缺失の清掃状態設定初期値值。
    if (room.getActive() == null) { room.setActive(true); } // に缺失の启用状態設定初期値值。
    Room existingRoom = roomMapper.findByRoomNumberIncludingInactive(room.getRoomNumber()); // 検索是否完了有相同部屋番号。
    if (existingRoom != null && Boolean.TRUE.equals(existingRoom.getActive())) { throw new IllegalArgumentException("部屋番号が重複しています。"); } // 阻止有効部屋番号重複。
    if (existingRoom != null) { roomMapper.reactivate(room); return; } // 完了削除の同番号部屋直接恢复と更新。
    roomMapper.insert(room); // 呼び出し Mapper 書き込み部屋记录。
  }

  public void updateStatuses(Integer id, String occupancyStatus, String cleaningStatus) { // 定義更新部屋状態の業務メソッド。
    roomMapper.updateStatuses(id, occupancyStatus, cleaningStatus); // 呼び出し Mapper 更新宿泊と清掃状態。
  }

  public void delete(Integer id) { // 定義削除部屋の業務メソッド。
    roomMapper.deactivate(id); // 呼び出し Mapper を部屋软削除に無効状態。
  }

  public int countAll() { // 定義集計全部部屋数量の業務メソッド。
    return roomMapper.countAll(); // 呼び出し Mapper 返却部屋总数。
  }

  public int countVacant() { // 定義集計空房数量の業務メソッド。
    return roomMapper.countVacant(); // 呼び出し Mapper 返却空房数量。
  }
}
