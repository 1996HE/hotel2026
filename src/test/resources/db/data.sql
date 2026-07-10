INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
) VALUES
  ('101', '桜の間', 'washitsu', 2, 8800, false, 'vacant', 'cleaned', true, '庭に近い和室'),
  ('102', '竹の間', 'washitsu', 3, 9800, false, 'vacant', 'cleaned', true, '家族向けの広めの部屋'),
  ('201', '海風', 'yoshitsu', 2, 11800, true, 'reserved', 'cleaned', true, '専用バス付き'),
  ('202', '山景', 'family', 5, 12800, true, 'vacant', 'needs_cleaning', true, 'グループ宿泊向け');

INSERT INTO room_price_rules (room_id, rule_name, start_date, end_date, price_per_person, priority, active, note)
SELECT id, '夏休み料金', DATE '2026-07-20', DATE '2026-08-31', base_price_per_person + 2500, 1, true, '繁忙期'
FROM rooms
WHERE room_number IN ('101', '102', '201', '202');

INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
) VALUES (
  '309', '桜庭', 'family', 5, 13500, true, 'reserved', 'cleaned', true, '同行者多数のサンプル用'
);

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000031', id, DATE '2026-10-01', DATE '2026-10-03', '山田太郎',
  'ヤマダタロウ', '男性', 41, '090-1111-0001', 'yamada@example.com',
  5, '予約サイト', 'paid', 'booked',
  120000, '5名同行のサンプル'
FROM rooms
WHERE room_number = '309'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000031');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000032', id, DATE '2026-10-05', DATE '2026-10-07', '佐藤花子',
  'サトウハナコ', '女性', 36, '090-1111-0002', 'sato@example.com',
  5, '電話', 'unpaid', 'booked',
  120000, '5名同行のサンプル'
FROM rooms
WHERE room_number = '309'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000032');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000033', id, DATE '2026-10-09', DATE '2026-10-11', '鈴木一郎',
  'スズキイチロウ', '男性', 44, '090-1111-0003', 'suzuki@example.com',
  5, 'メール', 'paid', 'booked',
  120000, '5名同行のサンプル'
FROM rooms
WHERE room_number = '309'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000033');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000034', id, DATE '2026-10-13', DATE '2026-10-15', '高橋美咲',
  'タカハシミサキ', '女性', 39, '090-1111-0004', 'takahashi@example.com',
  5, '現地', 'unpaid', 'booked',
  120000, '5名同行のサンプル'
FROM rooms
WHERE room_number = '309'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000034');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000035', id, DATE '2026-10-17', DATE '2026-10-19', '田中健',
  'タナカケン', '男性', 42, '090-1111-0005', 'tanaka@example.com',
  5, '公式', 'paid', 'booked',
  120000, '5名同行のサンプル'
FROM rooms
WHERE room_number = '309'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000035');

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('佐藤花子', 'サトウハナコ', '女性', 36, '080-2000-0001'),
    ('鈴木花子', 'スズキハナコ', '女性', 34, '080-2000-0002'),
    ('田中一郎', 'タナカイチロウ', '男性', 38, '080-2000-0003'),
    ('高橋恵', 'タカハシケイ', '女性', 31, '080-2000-0004')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000031'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('山本彩', 'ヤマモトサヤ', '女性', 29, '080-2000-0011'),
    ('伊藤直樹', 'イトウナオキ', '男性', 33, '080-2000-0012'),
    ('小林里奈', 'コバヤシリナ', '女性', 27, '080-2000-0013'),
    ('渡辺誠', 'ワタナベマコト', '男性', 40, '080-2000-0014')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000032'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('松本悠', 'マツモトユウ', '男性', 35, '080-2000-0021'),
    ('中村玲奈', 'ナカムラレナ', '女性', 30, '080-2000-0022'),
    ('吉田拓也', 'ヨシダタクヤ', '男性', 37, '080-2000-0023'),
    ('加藤真央', 'カトウマオ', '女性', 28, '080-2000-0024')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000033'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('斎藤蓮', 'サイトウレン', '男性', 32, '080-2000-0031'),
    ('木村結衣', 'キムラユイ', '女性', 26, '080-2000-0032'),
    ('岡田大輝', 'オカダダイキ', '男性', 34, '080-2000-0033'),
    ('石井沙織', 'イシイサオリ', '女性', 29, '080-2000-0034')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000034'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('村上圭介', 'ムラカミケイスケ', '男性', 45, '080-2000-0041'),
    ('橋本奈々', 'ハシモトナナ', '女性', 41, '080-2000-0042'),
    ('清水涼', 'シミズリョウ', '男性', 39, '080-2000-0043'),
    ('藤田舞', 'フジタマイ', '女性', 37, '080-2000-0044')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000035'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

UPDATE rooms
SET occupancy_status = 'reserved',
    cleaning_status = 'cleaned'
WHERE room_number = '309';

ALTER SEQUENCE reservation_no_seq RESTART WITH 36;
