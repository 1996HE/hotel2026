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
