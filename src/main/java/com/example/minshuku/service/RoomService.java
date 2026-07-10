package com.example.minshuku.service;

import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.RoomMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客室マスタの登録、再有効化、ステータス更新、件数集計を扱うサービス。
 * <p>
 * 予約業務と清掃業務の両方から参照されるため、客室の状態遷移はこのサービスで統一する。
 */
@Service
public class RoomService {
    private static final Set<String> ROOM_TYPES = Set.of("washitsu", "yoshitsu", "suite", "family");
    private static final Set<String> OCCUPANCY_STATUSES = Set.of("vacant", "reserved", "occupied");
    private static final Set<String> CLEANING_STATUSES = Set.of("cleaned", "needs_cleaning");
    private static final String MESSAGE_INVALID_OCCUPANCY_STATUS = "部屋の宿泊状態が正しくありません。";

    private final RoomMapper roomMapper;

    public RoomService(RoomMapper roomMapper) {
        this.roomMapper = roomMapper;
    }

    @Transactional(readOnly = true)
    public List<Room> findAll() {
        return roomMapper.findAll();
    }

    @Transactional(readOnly = true)
    public List<Room> findInactive() {
        return roomMapper.findInactive();
    }

    @Transactional(readOnly = true)
    public List<Room> findActive() {
        return roomMapper.findActive();
    }

    @Transactional(readOnly = true)
    public List<Room> findBookable() {
        return roomMapper.findBookable();
    }

    @Transactional(readOnly = true)
    public Room findById(Integer id) {
        return roomMapper.findById(id);
    }

    /**
     * 客室を登録する。論理削除済みの同一客室番号が存在する場合は再有効化する。
     */
    @Transactional
    public void create(Room room) {
        // 客室登録は、番号・名称・定員・種別・状態の業務条件を先に確定する。
        if (!StringUtils.hasText(room.getRoomNumber())) {
            throw new IllegalArgumentException("部屋番号を入力してください。");
        }
        if (!StringUtils.hasText(room.getRoomName())) {
            throw new IllegalArgumentException("部屋名を入力してください。");
        }
        if (room.getCapacity() == null || room.getCapacity() < 1) {
            throw new IllegalArgumentException("定員は1名以上にしてください。");
        }
        if (room.getBasePricePerPerson() == null) {
            // 基本単価が未入力の場合はゼロ円として扱う。
            room.setBasePricePerPerson(BigDecimal.ZERO);
        }
        if (!StringUtils.hasText(room.getRoomType())) {
            // 種別未指定は和室を初期値にする。
            room.setRoomType("washitsu");
        }

        requireAllowed(room.getRoomType(), ROOM_TYPES, "部屋タイプが正しくありません。");

        if (room.getPrivateBath() == null) {
            room.setPrivateBath(false);
        }
        if (!StringUtils.hasText(room.getOccupancyStatus())) {
            // 新規登録時は空室から開始する。
            room.setOccupancyStatus("vacant");
        }
        if (!StringUtils.hasText(room.getCleaningStatus())) {
            // 新規登録時は清掃済みを初期状態にする。
            room.setCleaningStatus("cleaned");
        }

        requireAllowed(room.getOccupancyStatus(), OCCUPANCY_STATUSES, MESSAGE_INVALID_OCCUPANCY_STATUS);
        requireAllowed(room.getCleaningStatus(), CLEANING_STATUSES, "部屋の清掃状態が正しくありません。");

        if (room.getActive() == null) {
            room.setActive(true);
        }

        Room existingRoom = roomMapper.findByRoomNumberIncludingInactive(room.getRoomNumber());

        if (existingRoom != null && Boolean.TRUE.equals(existingRoom.getActive())) {
            throw new IllegalArgumentException("部屋番号が重複しています。");
        }
        if (existingRoom != null) {
            // 退役済み客室が同番号で存在する場合は、新規作成ではなく再有効化する。
            roomMapper.reactivate(room);
            return;
        }

        roomMapper.insert(room);
    }

    /**
     * 予約・チェックアウト業務から客室の宿泊状態と清掃状態を更新する。
     */
    @Transactional
    public void updateStatuses(Integer id, String occupancyStatus, String cleaningStatus) {
        // 予約・チェックアウト・清掃の各業務が使うため、状態値は許可値だけに制限する。
        requireAllowed(occupancyStatus, OCCUPANCY_STATUSES, MESSAGE_INVALID_OCCUPANCY_STATUS);
        requireAllowed(cleaningStatus, CLEANING_STATUSES, "部屋の清掃状態が正しくありません。");

        if (roomMapper.updateStatuses(id, occupancyStatus, cleaningStatus) == 0) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
    }

    @Transactional
    public void delete(Integer id) {
        // 実データは削除せず、論理削除にして履歴参照を維持する。
        if (roomMapper.countBookedReservations(id) > 0) {
            throw new IllegalArgumentException("予約中の部屋は削除できません。先に予約を取消してください。");
        }
        if (roomMapper.deactivate(id) == 0) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
    }

    /**
     * 論理削除済みの部屋を有効状態へ戻す。
     */
    @Transactional
    public void restore(Integer id) {
        Room room = roomMapper.findById(id);
        if (room == null) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
        if (Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("有効な部屋は復元できません。");
        }
        if (roomMapper.restore(id) == 0) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
    }

    /**
     * 予約履歴のない論理削除済みの部屋だけを完全削除する。
     */
    @Transactional
    public void deletePermanently(Integer id) {
        Room room = roomMapper.findById(id);
        if (room == null) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
        if (Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("有効な部屋は完全削除できません。先に削除してください。");
        }
        if (roomMapper.countReservationsByRoomId(id) > 0) {
            throw new IllegalArgumentException("予約履歴がある部屋は完全削除できません。");
        }
        if (roomMapper.deletePermanently(id) == 0) {
            throw new IllegalArgumentException("部屋が見つかりません。");
        }
    }

    @Transactional(readOnly = true)
    public int countAll() {
        return roomMapper.countAll();
    }

    @Transactional(readOnly = true)
    public int countVacant() {
        return roomMapper.countVacant();
    }

    private void requireAllowed(String value, Set<String> allowedValues, String message) {
        if (!StringUtils.hasText(value) || !allowedValues.contains(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
