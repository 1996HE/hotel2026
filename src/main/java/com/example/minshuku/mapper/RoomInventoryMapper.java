package com.example.minshuku.mapper;

import com.example.minshuku.domain.RoomInventoryBlock;
import com.example.minshuku.domain.RoomInventoryBlockEvent;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Date-scoped room inventory blocks and their append-only audit events. */
@Mapper
public interface RoomInventoryMapper {
    List<RoomInventoryBlock> findForCalendar(
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive);

    RoomInventoryBlock findById(@Param("id") Integer id);

    RoomInventoryBlock findByIdForUpdate(@Param("id") Integer id);

    int countOverlapping(
            @Param("roomId") Integer roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive,
            @Param("excludedId") Integer excludedId);

    int insert(RoomInventoryBlock block);

    int update(RoomInventoryBlock block);

    int cancel(
            @Param("id") Integer id,
            @Param("version") Long version,
            @Param("actor") String actor,
            @Param("note") String note);

    int insertEvent(RoomInventoryBlockEvent event);

    List<RoomInventoryBlockEvent> findEvents(@Param("blockId") Integer blockId);
}
