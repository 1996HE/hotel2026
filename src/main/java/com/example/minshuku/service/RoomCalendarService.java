package com.example.minshuku.service;

import com.example.minshuku.domain.PublicBookingRequest;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomCalendarStay;
import com.example.minshuku.domain.RoomInventoryBlock;
import com.example.minshuku.domain.RoomInventoryBlockEvent;
import com.example.minshuku.mapper.PublicBookingMapper;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomInventoryMapper;
import com.example.minshuku.mapper.RoomMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Admin room calendar and date-scoped inventory operations. */
@Service
public class RoomCalendarService {
    private static final Set<String> BLOCK_TYPES = Set.of("stop_sale", "maintenance");

    private final RoomMapper roomMapper;
    private final ReservationMapper reservationMapper;
    private final PublicBookingMapper publicBookingMapper;
    private final RoomInventoryMapper inventoryMapper;

    public RoomCalendarService(
            RoomMapper roomMapper,
            ReservationMapper reservationMapper,
            PublicBookingMapper publicBookingMapper,
            RoomInventoryMapper inventoryMapper) {
        this.roomMapper = roomMapper;
        this.reservationMapper = reservationMapper;
        this.publicBookingMapper = publicBookingMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional(readOnly = true)
    public CalendarView calendar(YearMonth month) {
        YearMonth safeMonth = month == null ? YearMonth.from(reservationMapper.currentDate()) : month;
        LocalDate start = safeMonth.atDay(1);
        LocalDate end = safeMonth.plusMonths(1).atDay(1);
        return new CalendarView(
                safeMonth.toString(),
                start,
                end,
                reservationMapper.currentDate(),
                OffsetDateTime.now(),
                roomMapper.findActive(),
                reservationMapper.findCalendarStays(start, end),
                inventoryMapper.findForCalendar(start, end));
    }

    @Transactional(readOnly = true)
    public ReservationDetail reservationDetail(Integer id) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null) {
            throw new NoSuchElementException("予約が見つかりません。");
        }
        PublicBookingRequest request = reservation.getBookingRequestId() == null
                ? null
                : publicBookingMapper.findById(reservation.getBookingRequestId());
        List<Reservation> group = reservation.getBookingRequestId() == null
                ? List.of(reservation)
                : publicBookingMapper.findReservations(reservation.getBookingRequestId());
        return new ReservationDetail(reservation, request, group, allowedActions(reservation, request, group));
    }

    @Transactional(readOnly = true)
    public BlockDetail blockDetail(Integer id) {
        RoomInventoryBlock block = inventoryMapper.findById(id);
        if (block == null) {
            throw new NoSuchElementException("在庫ブロックが見つかりません。");
        }
        return new BlockDetail(block, inventoryMapper.findEvents(id));
    }

    @Transactional
    public RoomInventoryBlock createBlock(BlockWrite request, String actor) {
        validateBlockInput(request.roomId(), request.blockType(), request.startDate(),
                request.endDateExclusive(), request.reason());
        Room room = roomMapper.findByIdForUpdate(request.roomId());
        requireActiveRoom(room);
        ensureBlockRangeFree(request.roomId(), request.startDate(), request.endDateExclusive(), null);

        RoomInventoryBlock block = new RoomInventoryBlock();
        block.setRoomId(request.roomId());
        block.setBlockType(request.blockType());
        block.setStartDate(request.startDate());
        block.setEndDateExclusive(request.endDateExclusive());
        block.setReason(request.reason().trim());
        block.setCreatedBy(actor);
        block.setUpdatedBy(actor);
        inventoryMapper.insert(block);
        audit(UUID.randomUUID().toString(), "created", null, block, null, actor);
        return inventoryMapper.findById(block.getId());
    }

    @Transactional
    public RoomInventoryBlock updateBlock(Integer id, BlockUpdate request, String actor) {
        RoomInventoryBlock existing = requireEffectiveBlock(id, request.version());
        if (request.roomId() != null && !existing.getRoomId().equals(request.roomId())) {
            throw new IllegalArgumentException("在庫ブロックの部屋は変更できません。");
        }
        validateBlockInput(existing.getRoomId(), request.blockType(), request.startDate(),
                request.endDateExclusive(), request.reason());
        roomMapper.findByIdForUpdate(existing.getRoomId());
        ensureBlockRangeFree(existing.getRoomId(), request.startDate(), request.endDateExclusive(), id);

        RoomInventoryBlock updated = snapshot(existing);
        updated.setBlockType(request.blockType());
        updated.setStartDate(request.startDate());
        updated.setEndDateExclusive(request.endDateExclusive());
        updated.setReason(request.reason().trim());
        updated.setUpdatedBy(actor);
        if (inventoryMapper.update(updated) == 0) {
            throw new InventoryConflictException("他の管理者が更新しました。再読み込みしてください。");
        }
        updated.setVersion(existing.getVersion() + 1);
        audit(UUID.randomUUID().toString(), "updated", existing, updated, null, actor);
        return inventoryMapper.findById(id);
    }

    @Transactional
    public ReleaseResult releaseBlock(Integer id, BlockRelease request, String actor) {
        RoomInventoryBlock existing = requireEffectiveBlock(id, request.version());
        validateRelease(existing, request);
        roomMapper.findByIdForUpdate(existing.getRoomId());
        String operationId = UUID.randomUUID().toString();
        LocalDate releaseStart = request.startDate();
        LocalDate releaseEnd = request.endDateExclusive();

        if (releaseStart.equals(existing.getStartDate()) && releaseEnd.equals(existing.getEndDateExclusive())) {
            cancelBlock(existing, actor, request.reason(), operationId, "released");
            return new ReleaseResult(List.of(), true);
        }

        RoomInventoryBlock leftOrRight = snapshot(existing);
        leftOrRight.setUpdatedBy(actor);
        RoomInventoryBlock split = null;
        if (releaseStart.equals(existing.getStartDate())) {
            leftOrRight.setStartDate(releaseEnd);
        } else {
            leftOrRight.setEndDateExclusive(releaseStart);
            if (!releaseEnd.equals(existing.getEndDateExclusive())) {
                split = snapshot(existing);
                split.setId(null);
                split.setStartDate(releaseEnd);
                split.setSplitFromId(existing.getId());
                split.setVersion(0L);
                split.setCreatedBy(actor);
                split.setUpdatedBy(actor);
            }
        }
        if (inventoryMapper.update(leftOrRight) == 0) {
            throw new InventoryConflictException("他の管理者が更新しました。再読み込みしてください。");
        }
        leftOrRight.setVersion(existing.getVersion() + 1);
        audit(operationId, "partially_released", existing, leftOrRight, request.reason(), actor);
        if (split != null) {
            inventoryMapper.insert(split);
            audit(operationId, "split_created", null, split, request.reason(), actor);
        }
        return new ReleaseResult(
                inventoryMapper.findForCalendar(existing.getStartDate(), existing.getEndDateExclusive())
                        .stream().filter(block -> block.getRoomId().equals(existing.getRoomId())).toList(),
                false);
    }

    @Transactional
    public void cancelBookingRequest(Integer id, String reason, String actor) {
        requireText(reason, "取消理由を入力してください。");
        PublicBookingRequest request = publicBookingMapper.findByIdForUpdate(id);
        if (request == null || !"confirmed".equals(request.getStatus())) {
            throw new IllegalArgumentException("確認済みの予約申請のみ取消できます。");
        }
        List<Reservation> reservations = publicBookingMapper.findReservations(id);
        if (reservations.isEmpty() || reservations.stream().anyMatch(r -> !"booked".equals(r.getReservationStatus()))) {
            throw new IllegalArgumentException("全室が予約中の場合のみ一括取消できます。");
        }
        reservations.stream().map(Reservation::getRoomId).distinct().sorted()
                .forEach(roomMapper::findByIdForUpdate);
        int changed = publicBookingMapper.cancelReservations(id, "booked", reason.trim(), actor);
        if (changed != reservations.size()) {
            throw new InventoryConflictException("予約状態が変更されました。再読み込みしてください。");
        }
        if (publicBookingMapper.updateStatus(id, "confirmed", "cancelled", reason.trim(), OffsetDateTime.now()) == 0) {
            throw new InventoryConflictException("予約状態が変更されました。再読み込みしてください。");
        }
    }

    @Transactional
    public void updateCleaningStatus(Integer roomId, String cleaningStatus) {
        if (!"cleaned".equals(cleaningStatus)) {
            throw new IllegalArgumentException("この操作では清掃済みのみ指定できます。");
        }
        Room room = roomMapper.findByIdForUpdate(roomId);
        requireActiveRoom(room);
        if (!"needs_cleaning".equals(room.getCleaningStatus())) {
            throw new IllegalArgumentException("この部屋は清掃待ちではありません。");
        }
        List<String> statuses = reservationMapper.findOtherActiveStatusesByRoomIdOnDate(
                roomId, 0, reservationMapper.currentDate());
        String occupancy = statuses.contains("checked_in")
                ? "occupied"
                : statuses.contains("booked") ? "reserved" : "vacant";
        roomMapper.updateStatuses(roomId, occupancy, "cleaned");
    }

    private List<String> allowedActions(
            Reservation reservation, PublicBookingRequest request, List<Reservation> groupReservations) {
        return switch (reservation.getReservationStatus()) {
            case "pending" -> request != null && "pending".equals(request.getStatus())
                    ? List.of("confirmGroup", "rejectGroup")
                    : List.of();
            case "booked" -> request != null
                    && "confirmed".equals(request.getStatus())
                    && groupReservations.stream().allMatch(item -> "booked".equals(item.getReservationStatus()))
                            ? List.of("checkInRoom", "cancelGroup")
                            : request == null ? List.of("checkInRoom", "cancelReservation") : List.of("checkInRoom");
            case "checked_in" -> List.of("checkOutRoom");
            case "checked_out" -> "needs_cleaning".equals(reservation.getRoomCleaningStatus())
                    ? List.of("cleanRoom")
                    : List.of();
            default -> List.of();
        };
    }

    private RoomInventoryBlock requireEffectiveBlock(Integer id, Long version) {
        RoomInventoryBlock snapshot = inventoryMapper.findById(id);
        if (snapshot == null || !"effective".equals(snapshot.getRecordStatus())) {
            throw new NoSuchElementException("有効な在庫ブロックが見つかりません。");
        }
        roomMapper.findByIdForUpdate(snapshot.getRoomId());
        RoomInventoryBlock block = inventoryMapper.findByIdForUpdate(id);
        if (block == null || !"effective".equals(block.getRecordStatus())) {
            throw new InventoryConflictException("在庫ブロックが変更されました。再読み込みしてください。");
        }
        if (version == null || !version.equals(block.getVersion())) {
            throw new InventoryConflictException("他の管理者が更新しました。再読み込みしてください。");
        }
        return block;
    }

    private void validateBlockInput(Integer roomId, String blockType, LocalDate start, LocalDate end, String reason) {
        if (roomId == null || !BLOCK_TYPES.contains(blockType)) {
            throw new IllegalArgumentException("部屋とブロック種別を正しく指定してください。");
        }
        LocalDate today = reservationMapper.currentDate();
        if (start == null || end == null || start.isBefore(today) || !end.isAfter(start)) {
            throw new IllegalArgumentException("開始日は本日以降、終了日は開始日より後にしてください。");
        }
        requireText(reason, "理由を入力してください。");
        if (reason.trim().length() > 1000) {
            throw new IllegalArgumentException("理由は1000文字以内で入力してください。");
        }
    }

    private void validateRelease(RoomInventoryBlock block, BlockRelease request) {
        requireText(request.reason(), "解除理由を入力してください。");
        if (request.startDate() == null || request.endDateExclusive() == null
                || request.startDate().isBefore(reservationMapper.currentDate())
                || !request.endDateExclusive().isAfter(request.startDate())
                || request.startDate().isBefore(block.getStartDate())
                || request.endDateExclusive().isAfter(block.getEndDateExclusive())) {
            throw new IllegalArgumentException("解除範囲は有効期間内の本日以降を指定してください。");
        }
    }

    private void ensureBlockRangeFree(Integer roomId, LocalDate start, LocalDate end, Integer excludedBlockId) {
        if (reservationMapper.countOverlapping(roomId, start, end) > 0
                || inventoryMapper.countOverlapping(roomId, start, end, excludedBlockId) > 0) {
            throw new InventoryConflictException("指定期間には予約または在庫ブロックがあります。");
        }
    }

    private void cancelBlock(RoomInventoryBlock block, String actor, String note, String operationId,
            String eventType) {
        if (inventoryMapper.cancel(block.getId(), block.getVersion(), actor, note.trim()) == 0) {
            throw new InventoryConflictException("他の管理者が更新しました。再読み込みしてください。");
        }
        RoomInventoryBlock cancelled = snapshot(block);
        cancelled.setRecordStatus("cancelled");
        cancelled.setVersion(block.getVersion() + 1);
        audit(operationId, eventType, block, cancelled, note, actor);
    }

    private void audit(String operationId, String eventType, RoomInventoryBlock before,
            RoomInventoryBlock after, String note, String actor) {
        RoomInventoryBlock source = after == null ? before : after;
        RoomInventoryBlockEvent event = new RoomInventoryBlockEvent();
        event.setOperationId(operationId);
        event.setBlockId(source.getId());
        event.setRoomId(source.getRoomId());
        event.setEventType(eventType);
        if (before != null) {
            event.setBeforeType(before.getBlockType());
            event.setBeforeStartDate(before.getStartDate());
            event.setBeforeEndDate(before.getEndDateExclusive());
            event.setBeforeReason(before.getReason());
            event.setBeforeStatus(before.getRecordStatus());
        }
        if (after != null) {
            event.setAfterType(after.getBlockType());
            event.setAfterStartDate(after.getStartDate());
            event.setAfterEndDate(after.getEndDateExclusive());
            event.setAfterReason(after.getReason());
            event.setAfterStatus(after.getRecordStatus() == null ? "effective" : after.getRecordStatus());
        }
        event.setActionNote(note == null ? null : note.trim());
        event.setActorUsername(actor);
        inventoryMapper.insertEvent(event);
    }

    private RoomInventoryBlock snapshot(RoomInventoryBlock value) {
        RoomInventoryBlock copy = new RoomInventoryBlock();
        copy.setId(value.getId());
        copy.setRoomId(value.getRoomId());
        copy.setRoomNumber(value.getRoomNumber());
        copy.setRoomName(value.getRoomName());
        copy.setBlockType(value.getBlockType());
        copy.setStartDate(value.getStartDate());
        copy.setEndDateExclusive(value.getEndDateExclusive());
        copy.setReason(value.getReason());
        copy.setRecordStatus(value.getRecordStatus());
        copy.setSplitFromId(value.getSplitFromId());
        copy.setVersion(value.getVersion());
        copy.setCreatedBy(value.getCreatedBy());
        copy.setUpdatedBy(value.getUpdatedBy());
        return copy;
    }

    private void requireActiveRoom(Room room) {
        if (room == null || !Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("有効な部屋が見つかりません。");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    public record CalendarView(String month, LocalDate monthStart, LocalDate nextMonthStart, LocalDate today,
            OffsetDateTime generatedAt, List<Room> rooms, List<RoomCalendarStay> stays,
            List<RoomInventoryBlock> blocks) {
    }
    public record ReservationDetail(Reservation reservation, PublicBookingRequest bookingRequest,
            List<Reservation> groupReservations, List<String> allowedActions) {
    }
    public record BlockDetail(RoomInventoryBlock block, List<RoomInventoryBlockEvent> events) {
    }
    public record BlockWrite(Integer roomId, String blockType, LocalDate startDate, LocalDate endDateExclusive,
            String reason) {
    }
    public record BlockUpdate(Integer roomId, String blockType, LocalDate startDate, LocalDate endDateExclusive,
            String reason, Long version) {
    }
    public record BlockRelease(LocalDate startDate, LocalDate endDateExclusive, String reason, Long version) {
    }
    public record ReleaseResult(List<RoomInventoryBlock> remainingBlocks, boolean fullyReleased) {
    }
}
