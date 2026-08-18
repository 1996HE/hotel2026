package com.example.minshuku.mapper;

import com.example.minshuku.domain.ReservationGuest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 予約同行者テーブルへの永続化を担当する MyBatis Mapper。
 * <p>
 * 予約本体と独立して保存し、同行者の人数変動や明細表示に対応する。
 */
@Mapper
public interface ReservationGuestMapper {
    /**
     * 予約本体登録後、人数分の同行者明細を保存する。
     */
    int insert(ReservationGuest guest);

    /** 予約編集時に同行者明細を入れ替える。 */
    int deleteByReservationId(Integer reservationId);
}
