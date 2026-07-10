INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
) VALUES
  ('101', '桜の間', 'washitsu', 2, 8800, false, 'vacant', 'cleaned', true, '庭に近い和室'),
  ('102', '竹の間', 'washitsu', 3, 9800, false, 'vacant', 'cleaned', true, '家族向けの広めの部屋'),
  ('201', '海風', 'yoshitsu', 2, 11800, true, 'reserved', 'cleaned', true, '専用バス付き'),
  ('202', '山景', 'family', 5, 12800, true, 'vacant', 'needs_cleaning', true, 'グループ宿泊向け')
ON CONFLICT (room_number) DO NOTHING;

INSERT INTO room_price_rules (room_id, rule_name, start_date, end_date, price_per_person, priority, active, note)
SELECT id, '夏休み料金', DATE '2026-07-20', DATE '2026-08-31', base_price_per_person + 2500, 1, true, '繁忙期'
FROM rooms
WHERE room_number IN ('101', '102', '201', '202')
ON CONFLICT DO NOTHING;

INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
) VALUES (
  '309', '桜庭', 'family', 5, 13500, true, 'reserved', 'cleaned', true, '同行者多数のサンプル用'
)
ON CONFLICT (room_number) DO NOTHING;

INSERT INTO rooms (
  room_number, room_name, room_type, capacity, base_price_per_person,
  private_bath, occupancy_status, cleaning_status, active, note
) VALUES
  ('203', '若葉', 'yoshitsu', 2, 11200, true, 'vacant', 'cleaned', true, '予約一覧の表示確認用'),
  ('204', '千鳥', 'family', 4, 14800, false, 'vacant', 'cleaned', true, '一覧2ページ目確認用'),
  ('205', '霞', 'washitsu', 3, 9900, false, 'vacant', 'cleaned', true, '予約作成確認用'),
  ('206', '霧', 'yoshitsu', 2, 8700, false, 'vacant', 'cleaned', false, '削除済みの表示確認用'),
  ('207', '梢', 'family', 6, 18000, true, 'vacant', 'needs_cleaning', false, '削除済みかつ清掃待ちの確認用'),
  ('208', '楓', 'yoshitsu', 2, 12200, false, 'vacant', 'cleaned', true, '料金ルール確認用'),
  ('209', '椿', 'washitsu', 3, 13200, false, 'vacant', 'cleaned', true, '料金ルール確認用'),
  ('210', '藤', 'family', 4, 14000, true, 'vacant', 'cleaned', false, '削除済み一覧確認用'),
  ('211', '梅', 'washitsu', 2, 9100, false, 'vacant', 'cleaned', false, '削除済み一覧確認用'),
  ('212', '菊', 'yoshitsu', 2, 10400, true, 'vacant', 'needs_cleaning', false, '削除済み一覧確認用'),
  ('213', '萩', 'family', 5, 15000, false, 'vacant', 'cleaned', false, '削除済み一覧確認用'),
  ('214', '葵', 'washitsu', 3, 9600, false, 'vacant', 'cleaned', false, '削除済み一覧確認用'),
  ('215', '桐', 'yoshitsu', 2, 10900, true, 'vacant', 'needs_cleaning', false, '削除済み一覧確認用'),
  ('216', '笹', 'family', 4, 12600, false, 'vacant', 'cleaned', false, '削除済み一覧確認用'),
  ('217', '柿', 'washitsu', 2, 8900, false, 'vacant', 'cleaned', false, '削除済み一覧確認用')
ON CONFLICT (room_number) DO NOTHING;

INSERT INTO room_price_rules (room_id, rule_name, start_date, end_date, price_per_person, priority, active, note)
SELECT rooms.id, v.rule_name, v.start_date, v.end_date, v.price_per_person, v.priority, v.active, v.note
FROM rooms
JOIN (
  VALUES
    ('101', '早割料金', DATE '2026-09-01', DATE '2026-09-15', 9800, 2, true, '早期予約向け'),
    ('102', '連休料金', DATE '2026-09-16', DATE '2026-09-30', 12800, 1, true, '連休確認用'),
    ('201', '冬支度料金', DATE '2026-11-01', DATE '2026-11-30', 13800, 1, true, '冬前の確認用'),
    ('203', '年末料金', DATE '2026-12-20', DATE '2027-01-05', 16200, 1, true, '年末年始確認用'),
    ('204', '繁忙期料金', DATE '2026-08-01', DATE '2026-08-31', 17800, 1, true, '繁忙期確認用')
) AS v(room_number, rule_name, start_date, end_date, price_per_person, priority, active, note)
  ON rooms.room_number = v.room_number
ON CONFLICT DO NOTHING;

INSERT INTO room_price_rules (room_id, rule_name, start_date, end_date, price_per_person, priority, active, note)
SELECT rooms.id, v.rule_name, v.start_date, v.end_date, v.price_per_person, v.priority, v.active, v.note
FROM rooms
JOIN (
  VALUES
    ('208', '週末料金', DATE '2026-10-01', DATE '2026-10-31', 14200, 1, true, '週末表示確認用')
) AS v(room_number, rule_name, start_date, end_date, price_per_person, priority, active, note)
  ON rooms.room_number = v.room_number
ON CONFLICT DO NOTHING;

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

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000036', id, DATE '2026-10-21', DATE '2026-10-23', '渡辺陽子',
  'ワタナベヨウコ', '女性', 38, '090-1111-0006', 'watanabe@example.com',
  2, '電話', 'unpaid', 'booked',
  44800, '画面確認用'
FROM rooms
WHERE room_number = '203'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000036');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000037', id, DATE '2026-10-24', DATE '2026-10-26', '中村拓海',
  'ナカムラタクミ', '男性', 35, '090-1111-0007', 'nakamura@example.com',
  3, '予約サイト', 'paid', 'booked',
  89280, '同行者表示確認用'
FROM rooms
WHERE room_number = '204'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000037');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000038', id, DATE '2026-10-27', DATE '2026-10-29', '小林沙織',
  'コバヤシサオリ', '女性', 29, '090-1111-0008', 'kobayashi@example.com',
  4, 'メール', 'unpaid', 'booked',
  79200, '一覧2ページ目確認用'
FROM rooms
WHERE room_number = '205'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000038');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000039', id, DATE '2026-10-30', DATE '2026-11-01', '斎藤誠',
  'サイトウマコト', '男性', 47, '090-1111-0009', 'saito@example.com',
  2, '現地', 'paid', 'booked',
  34800, '削除済み部屋の履歴確認用'
FROM rooms
WHERE room_number = '206'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000039');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000040', id, DATE '2026-11-03', DATE '2026-11-05', '高橋由美',
  'タカハシユミ', '女性', 41, '090-1111-0010', 'takahashi-y@example.com',
  5, '公式', 'unpaid', 'booked',
  90000, '削除済み部屋の履歴確認用'
FROM rooms
WHERE room_number = '207'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000040');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT
  'R000041', id, DATE '2026-07-02', DATE '2026-07-04', '西村優子',
  'ニシムラユウコ', '女性', 39, '090-1111-0011', 'nishimura@example.com',
  2, '公式', 'paid', 'checked_out',
  22400, 'チェックアウト削除確認用'
FROM rooms
WHERE room_number = '206'
  AND NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = 'R000041');

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT v.reservation_no, rooms.id, v.check_in_date, v.check_out_date, v.guest_name, v.guest_kana,
  v.guest_gender, v.guest_age, v.guest_phone, v.guest_email, v.guest_count, v.reservation_form,
  v.payment_status, v.reservation_status, v.total_amount, v.note
FROM rooms
JOIN (
  VALUES
    ('R000042', '206', DATE '2026-11-01', DATE '2026-11-03', '鈴木陽菜', 'スズキヒナ', '女性', 34, '090-1111-0012', 'suzuki-hina@example.com', 2, '電話', 'paid', 'cancelled', 18400, '取消一覧確認用'),
    ('R000043', '207', DATE '2026-11-04', DATE '2026-11-06', '高橋悠斗', 'タカハシユウト', '男性', 36, '090-1111-0013', 'takahashi-y@example.com', 2, '予約サイト', 'unpaid', 'cancelled', 36000, '取消一覧確認用'),
    ('R000044', '210', DATE '2026-11-07', DATE '2026-11-09', '田中梨花', 'タナカリカ', '女性', 29, '090-1111-0014', 'tanaka-r@example.com', 2, '公式', 'paid', 'cancelled', 28000, '取消一覧確認用'),
    ('R000045', '211', DATE '2026-11-10', DATE '2026-11-12', '伊藤翔', 'イトウショウ', '男性', 31, '090-1111-0015', 'ito-sho@example.com', 2, '電話', 'unpaid', 'cancelled', 18200, '取消一覧確認用'),
    ('R000046', '212', DATE '2026-11-13', DATE '2026-11-15', '小林美咲', 'コバヤシミサキ', '女性', 27, '090-1111-0016', 'kobayashi-m@example.com', 2, 'メール', 'paid', 'cancelled', 20800, '取消一覧確認用'),
    ('R000047', '213', DATE '2026-11-16', DATE '2026-11-18', '山本蓮', 'ヤマモトレン', '男性', 39, '090-1111-0017', 'yamamoto-r@example.com', 2, '現地', 'unpaid', 'cancelled', 30000, '取消一覧確認用'),
    ('R000048', '214', DATE '2026-11-19', DATE '2026-11-21', '渡辺さくら', 'ワタナベサクラ', '女性', 33, '090-1111-0018', 'watanabe-s@example.com', 2, '予約サイト', 'paid', 'cancelled', 19200, '取消一覧確認用'),
    ('R000049', '215', DATE '2026-11-22', DATE '2026-11-24', '佐藤健', 'サトウケン', '男性', 42, '090-1111-0019', 'sato-k@example.com', 2, '電話', 'unpaid', 'cancelled', 21800, '取消一覧確認用'),
    ('R000050', '216', DATE '2026-11-25', DATE '2026-11-27', '中村彩', 'ナカムラアヤ', '女性', 28, '090-1111-0020', 'nakamura-a@example.com', 2, '公式', 'paid', 'cancelled', 25200, '取消一覧確認用'),
    ('R000051', '217', DATE '2026-11-28', DATE '2026-11-30', '松本拓也', 'マツモトタクヤ', '男性', 37, '090-1111-0021', 'matsumoto-t@example.com', 2, 'メール', 'unpaid', 'cancelled', 17800, '取消一覧確認用')
) AS v(reservation_no, room_number, check_in_date, check_out_date, guest_name, guest_kana, guest_gender,
       guest_age, guest_phone, guest_email, guest_count, reservation_form, payment_status,
       reservation_status, total_amount, note)
  ON rooms.room_number = v.room_number
WHERE NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = v.reservation_no);

INSERT INTO reservations (
  reservation_no, room_id, check_in_date, check_out_date, guest_name,
  guest_kana, guest_gender, guest_age, guest_phone, guest_email,
  guest_count, reservation_form, payment_status, reservation_status,
  total_amount, note
)
SELECT v.reservation_no, rooms.id, v.check_in_date, v.check_out_date, v.guest_name, v.guest_kana,
  v.guest_gender, v.guest_age, v.guest_phone, v.guest_email, v.guest_count, v.reservation_form,
  v.payment_status, v.reservation_status, v.total_amount, v.note
FROM rooms
JOIN (
  VALUES
    ('R000052', '206', DATE '2026-07-05', DATE '2026-07-07', '西村誠', 'ニシムラマコト', '男性', 38, '090-1111-0022', 'nishimura-m@example.com', 2, '公式', 'paid', 'checked_out', 18400, '退室一覧確認用'),
    ('R000053', '207', DATE '2026-07-08', DATE '2026-07-10', '杉本花', 'スギモトハナ', '女性', 30, '090-1111-0023', 'sugimoto-h@example.com', 2, '電話', 'unpaid', 'checked_out', 36000, '退室一覧確認用'),
    ('R000054', '210', DATE '2026-07-11', DATE '2026-07-13', '森田颯', 'モリタハヤテ', '男性', 35, '090-1111-0024', 'morita-h@example.com', 2, '予約サイト', 'paid', 'checked_out', 28000, '退室一覧確認用'),
    ('R000055', '211', DATE '2026-07-14', DATE '2026-07-16', '木村彩乃', 'キムラアヤノ', '女性', 26, '090-1111-0025', 'kimura-a@example.com', 2, 'メール', 'unpaid', 'checked_out', 18200, '退室一覧確認用'),
    ('R000056', '212', DATE '2026-07-17', DATE '2026-07-19', '岡田裕貴', 'オカダユウキ', '男性', 41, '090-1111-0026', 'okada-y@example.com', 2, '現地', 'paid', 'checked_out', 20800, '退室一覧確認用'),
    ('R000057', '213', DATE '2026-07-20', DATE '2026-07-22', '石井結衣', 'イシイユイ', '女性', 32, '090-1111-0027', 'ishii-y@example.com', 2, '公式', 'unpaid', 'checked_out', 30000, '退室一覧確認用'),
    ('R000058', '214', DATE '2026-07-23', DATE '2026-07-25', '吉田亮', 'ヨシダリョウ', '男性', 29, '090-1111-0028', 'yoshida-r@example.com', 2, '予約サイト', 'paid', 'checked_out', 19200, '退室一覧確認用'),
    ('R000059', '215', DATE '2026-07-26', DATE '2026-07-28', '加藤茜', 'カトウアカネ', '女性', 37, '090-1111-0029', 'kato-a@example.com', 2, '電話', 'unpaid', 'checked_out', 21800, '退室一覧確認用'),
    ('R000060', '216', DATE '2026-07-29', DATE '2026-07-31', '藤田大輝', 'フジタダイキ', '男性', 44, '090-1111-0030', 'fujita-d@example.com', 2, 'メール', 'paid', 'checked_out', 25200, '退室一覧確認用'),
    ('R000061', '217', DATE '2026-08-01', DATE '2026-08-03', '前田美穂', 'マエダミホ', '女性', 35, '090-1111-0031', 'maeda-m@example.com', 2, '現地', 'unpaid', 'checked_out', 17800, '退室一覧確認用')
) AS v(reservation_no, room_number, check_in_date, check_out_date, guest_name, guest_kana, guest_gender,
       guest_age, guest_phone, guest_email, guest_count, reservation_form, payment_status,
       reservation_status, total_amount, note)
  ON rooms.room_number = v.room_number
WHERE NOT EXISTS (SELECT 1 FROM reservations WHERE reservation_no = v.reservation_no);

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('西村真一', 'ニシムラシンイチ', '男性', 42, '080-2000-0061')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000041'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

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

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('木下陽菜', 'キノシタヒナ', '女性', 33, '080-2000-0051')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000036'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('山口翔太', 'ヤマグチショウタ', '男性', 31, '080-2000-0052'),
    ('伊藤綾', 'イトウアヤ', '女性', 27, '080-2000-0053')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000037'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('松田紗季', 'マツダサキ', '女性', 30, '080-2000-0054'),
    ('石川翼', 'イシカワツバサ', '男性', 34, '080-2000-0055'),
    ('森田愛', 'モリタアイ', '女性', 25, '080-2000-0056')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no = 'R000038'
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('鈴木陽菜', 'スズキヒナ', '女性', 34, '080-2000-0062'),
    ('高橋悠斗', 'タカハシユウト', '男性', 36, '080-2000-0063'),
    ('田中梨花', 'タナカリカ', '女性', 29, '080-2000-0064'),
    ('伊藤翔', 'イトウショウ', '男性', 31, '080-2000-0065'),
    ('小林美咲', 'コバヤシミサキ', '女性', 27, '080-2000-0066'),
    ('山本蓮', 'ヤマモトレン', '男性', 39, '080-2000-0067'),
    ('渡辺さくら', 'ワタナベサクラ', '女性', 33, '080-2000-0068'),
    ('佐藤健', 'サトウケン', '男性', 42, '080-2000-0069'),
    ('中村彩', 'ナカムラアヤ', '女性', 28, '080-2000-0070'),
    ('松本拓也', 'マツモトタクヤ', '男性', 37, '080-2000-0071')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no IN ('R000042', 'R000043', 'R000044', 'R000045', 'R000046', 'R000047', 'R000048', 'R000049', 'R000050', 'R000051')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

INSERT INTO reservation_guests (reservation_id, guest_name, guest_kana, guest_gender, guest_age, guest_phone)
SELECT r.id, v.guest_name, v.guest_kana, v.guest_gender, v.guest_age, v.guest_phone
FROM reservations r
JOIN (
  VALUES
    ('西村真一', 'ニシムラシンイチ', '男性', 42, '080-2000-0082'),
    ('杉本彩', 'スギモトアヤ', '女性', 30, '080-2000-0083'),
    ('森田翼', 'モリタツバサ', '男性', 35, '080-2000-0084'),
    ('木村実央', 'キムラミオ', '女性', 26, '080-2000-0085'),
    ('岡田航', 'オカダワタル', '男性', 41, '080-2000-0086'),
    ('石井莉子', 'イシイリコ', '女性', 32, '080-2000-0087'),
    ('吉田海斗', 'ヨシダカイト', '男性', 29, '080-2000-0088'),
    ('加藤真央', 'カトウマオ', '女性', 37, '080-2000-0089'),
    ('藤田誠', 'フジタマコト', '男性', 44, '080-2000-0090'),
    ('前田紗季', 'マエダサキ', '女性', 35, '080-2000-0091')
) AS v(guest_name, guest_kana, guest_gender, guest_age, guest_phone) ON TRUE
WHERE r.reservation_no IN ('R000052', 'R000053', 'R000054', 'R000055', 'R000056', 'R000057', 'R000058', 'R000059', 'R000060', 'R000061')
  AND NOT EXISTS (
    SELECT 1 FROM reservation_guests g
    WHERE g.reservation_id = r.id AND g.guest_name = v.guest_name
  );

UPDATE rooms
SET occupancy_status = 'reserved',
    cleaning_status = 'cleaned'
WHERE room_number IN ('201', '203', '204', '205', '208', '209', '309');

SELECT setval(
  'reservation_no_seq',
  GREATEST(
    1,
    (SELECT COALESCE(MAX(CAST(SUBSTRING(reservation_no FROM 2) AS bigint)), 0) FROM reservations)
  ),
  (SELECT COUNT(*) > 0 FROM reservations)
);
