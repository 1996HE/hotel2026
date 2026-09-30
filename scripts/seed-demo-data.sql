\set ON_ERROR_STOP on

-- 本文件只填充可安全识别的演示业务数据，不修改管理员、备份历史或 Flyway 系统记录。
-- 所有演示记录使用固定编号，重复执行时不会重复插入。
BEGIN;

-- 30 间演示客房；未来预订所用的后 10 间标记为已预订。
INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
)
SELECT
  'D' || LPAD(series_no::text, 3, '0'),
  '演示客室 ' || LPAD(series_no::text, 2, '0'),
  (ARRAY['washitsu', 'yoshitsu', 'suite', 'family'])[((series_no - 1) % 4) + 1],
  2 + (series_no % 4),
  7800 + series_no * 300,
  series_no % 3 = 0,
  CASE WHEN series_no >= 21 THEN 'reserved' ELSE 'vacant' END,
  CASE WHEN series_no <= 10 AND series_no % 3 = 0 THEN 'needs_cleaning' ELSE 'cleaned' END,
  true,
  '画面・検索・集計確認用の演示データ'
FROM generate_series(1, 30) AS demo(series_no)
ON CONFLICT (room_number) DO NOTHING;

-- 各演示客房に 1 件ずつ期間料金を設定する。
INSERT INTO room_price_rules (
  room_id, rule_name, start_date, end_date, price_per_person,
  priority, active, note
)
SELECT
  room.id,
  '演示季节料金 ' || LPAD(series_no::text, 2, '0'),
  DATE '2026-09-01',
  DATE '2026-12-31',
  room.base_price_per_person + 1500,
  5,
  true,
  '料金规则表示确认用'
FROM generate_series(1, 30) AS demo(series_no)
JOIN rooms room ON room.room_number = 'D' || LPAD(series_no::text, 3, '0')
ON CONFLICT (room_id, rule_name, start_date, end_date) DO NOTHING;

-- 30 名顾客；编号、电话和邮箱固定，便于搜索及重复执行。
INSERT INTO customers (customer_no, name, phone, email)
SELECT
  'C' || LPAD(series_no::text, 6, '0'),
  '演示顾客' || LPAD(series_no::text, 2, '0'),
  '090-7000-' || LPAD(series_no::text, 4, '0'),
  'demo' || LPAD(series_no::text, 2, '0') || '@example.local'
FROM generate_series(1, 30) AS demo(series_no)
ON CONFLICT (customer_no) DO NOTHING;

-- 每位顾客建立 1 笔住宿订单：10 笔已退房、10 笔已取消、10 笔未来预订。
-- 付款状态覆盖未付款、已付款、部分退款和全额退款，便于验证财务界面。
INSERT INTO reservations (
  reservation_no, room_id, customer_id, check_in_date, check_out_date,
  checked_in_at, checked_out_at, guest_name, guest_kana, guest_gender,
  guest_age, guest_phone, guest_email, guest_count, reservation_form,
  payment_status, reservation_status, total_amount, note
)
SELECT
  'R' || LPAD(series_no::text, 6, '0'),
  room.id,
  customer.id,
  CASE
    WHEN series_no <= 10 THEN DATE '2026-05-01' + (series_no - 1) * 3
    WHEN series_no <= 20 THEN DATE '2026-07-01' + (series_no - 11) * 2
    ELSE DATE '2026-09-01' + (series_no - 21) * 3
  END,
  CASE
    WHEN series_no <= 10 THEN DATE '2026-05-03' + (series_no - 1) * 3
    WHEN series_no <= 20 THEN DATE '2026-07-03' + (series_no - 11) * 2
    ELSE DATE '2026-09-03' + (series_no - 21) * 3
  END,
  CASE WHEN series_no <= 10
    THEN (DATE '2026-05-01' + (series_no - 1) * 3)::timestamp + TIME '15:00'
    ELSE NULL
  END,
  CASE WHEN series_no <= 10
    THEN (DATE '2026-05-03' + (series_no - 1) * 3)::timestamp + TIME '10:00'
    ELSE NULL
  END,
  customer.name,
  'デモゲスト' || LPAD(series_no::text, 2, '0'),
  CASE WHEN series_no % 2 = 0 THEN '女性' ELSE '男性' END,
  24 + series_no,
  customer.phone,
  customer.email,
  2,
  (ARRAY['公式サイト', '電話', '予約サイト', 'メール'])[((series_no - 1) % 4) + 1],
  CASE
    WHEN series_no BETWEEN 5 AND 7 THEN 'partially_refunded'
    WHEN series_no BETWEEN 8 AND 10 OR series_no BETWEEN 16 AND 20 THEN 'refunded'
    WHEN series_no BETWEEN 11 AND 15 OR series_no BETWEEN 26 AND 30 THEN 'unpaid'
    ELSE 'paid'
  END,
  CASE
    WHEN series_no <= 10 THEN 'checked_out'
    WHEN series_no <= 20 THEN 'cancelled'
    ELSE 'booked'
  END,
  18000 + series_no * 1000,
  '关联客房、顾客、同行者及财务记录的演示订单'
FROM generate_series(1, 30) AS demo(series_no)
JOIN rooms room ON room.room_number = 'D' || LPAD(series_no::text, 3, '0')
JOIN customers customer ON customer.customer_no = 'C' || LPAD(series_no::text, 6, '0')
ON CONFLICT (reservation_no) DO NOTHING;

-- 每笔住宿订单添加 1 名同行者，共 30 条同行者记录。
INSERT INTO reservation_guests (
  reservation_id, guest_name, guest_kana, guest_gender, guest_age,
  guest_phone
)
SELECT
  reservation.id,
  '演示同行者' || LPAD(series_no::text, 2, '0'),
  'デモドウコウシャ' || LPAD(series_no::text, 2, '0'),
  CASE WHEN series_no % 2 = 0 THEN '男性' ELSE '女性' END,
  20 + series_no,
  '080-7100-' || LPAD(series_no::text, 4, '0')
FROM generate_series(1, 30) AS demo(series_no)
JOIN reservations reservation
  ON reservation.reservation_no = 'R' || LPAD(series_no::text, 6, '0')
WHERE NOT EXISTS (
  SELECT 1
  FROM reservation_guests existing
  WHERE existing.reservation_id = reservation.id
    AND existing.guest_name = '演示同行者' || LPAD(series_no::text, 2, '0')
);

-- 财务记录与订单一一对应；退款金额永远不超过实收金额。
INSERT INTO reservation_finances (
  reservation_id, received_amount, payment_method, received_at,
  refund_amount, refunded_at
)
SELECT
  reservation.id,
  CASE
    WHEN series_no BETWEEN 11 AND 15 OR series_no BETWEEN 26 AND 30 THEN 0
    ELSE reservation.total_amount
  END,
  CASE
    WHEN series_no BETWEEN 11 AND 15 OR series_no BETWEEN 26 AND 30 THEN NULL
    ELSE (ARRAY['cash', 'card', 'transfer', 'platform'])[((series_no - 1) % 4) + 1]
  END,
  CASE
    WHEN series_no BETWEEN 11 AND 15 OR series_no BETWEEN 26 AND 30 THEN NULL
    ELSE reservation.check_in_date::timestamp + TIME '14:00'
  END,
  CASE
    WHEN series_no BETWEEN 5 AND 7 THEN 3000 + series_no * 100
    WHEN series_no BETWEEN 8 AND 10 OR series_no BETWEEN 16 AND 20 THEN reservation.total_amount
    ELSE 0
  END,
  CASE
    WHEN series_no BETWEEN 5 AND 10 OR series_no BETWEEN 16 AND 20
      THEN reservation.check_out_date::timestamp + TIME '12:00'
    ELSE NULL
  END
FROM generate_series(1, 30) AS demo(series_no)
JOIN reservations reservation
  ON reservation.reservation_no = 'R' || LPAD(series_no::text, 6, '0')
ON CONFLICT (reservation_id) DO NOTHING;

-- 保持业务编号序列与演示数据中的最大编号一致，避免后续新增发生冲突。
SELECT setval(
  'customer_no_seq',
  GREATEST(1, (SELECT COALESCE(MAX(SUBSTRING(customer_no FROM 2)::bigint), 0) FROM customers)),
  (SELECT COUNT(*) > 0 FROM customers)
);

SELECT setval(
  'reservation_no_seq',
  GREATEST(1, (SELECT COALESCE(MAX(SUBSTRING(reservation_no FROM 2)::bigint), 0) FROM reservations)),
  (SELECT COUNT(*) > 0 FROM reservations)
);

COMMIT;
