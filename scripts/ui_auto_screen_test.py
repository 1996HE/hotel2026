#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Safari を使ってローカル画面の自動クリック・入力テストを実行する。"""

from __future__ import annotations

import json
import subprocess
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


BASE_DIR = Path("/Users/heniantong/Documents/codex/hotel-management")
EVIDENCE_DIR = BASE_DIR / "evidence"
REPORT_PATH = EVIDENCE_DIR / "ui-screen-test-2026-06-18.txt"
BASE_URL = "http://localhost:8000"


def apple_escape(text: str) -> str:
    """AppleScript の文字列用にエスケープする。"""
    return text.replace("\\", "\\\\").replace("\r", " ").replace("\n", " ").replace('"', '\\"')


def run_safari_js(url: str, js: str, delay_seconds: int = 3) -> subprocess.CompletedProcess[str]:
    """Safari でページを開いて JavaScript を実行する。"""
    script = f"""
tell application "Safari"
  activate
  open location "{url}"
  delay {delay_seconds}
  do JavaScript "{apple_escape(js)}" in current tab of front window
end tell
"""
    return subprocess.run(
        ["osascript", "-e", script],
        capture_output=True,
        text=True,
        timeout=90,
        check=False,
    )


def run_safari_current_tab(js: str) -> subprocess.CompletedProcess[str]:
    """現在の Safari タブで JavaScript を実行する。"""
    script = f"""
tell application "Safari"
  activate
  do JavaScript "{apple_escape(js)}" in current tab of front window
end tell
"""
    return subprocess.run(
        ["osascript", "-e", script],
        capture_output=True,
        text=True,
        timeout=90,
        check=False,
    )


@dataclass
class StepResult:
    name: str
    ok: bool
    details: str


def parse_json(text: str) -> dict:
    """Safari から返った JSON を辞書にする。"""
    return json.loads(text.strip() or "{}")


def dashboard_test() -> StepResult:
    js = r"""
(() => {
  const text = document.body.innerText;
  return JSON.stringify({
    title: document.title,
    hasBrand: text.includes('白馬樹海'),
    hasReservationList: text.includes('予約一覧'),
    nav: Array.from(document.querySelectorAll('.nav a')).map((a) => a.textContent.trim())
  });
})()
"""
    result = run_safari_js(f"{BASE_URL}/dashboard", js)
    if result.returncode != 0:
      return StepResult("dashboard", False, result.stderr.strip())
    data = parse_json(result.stdout)
    ok = data.get("hasBrand") and data.get("hasReservationList") and data.get("title") == "白馬樹海 予約一覧"
    return StepResult("dashboard", ok, json.dumps(data, ensure_ascii=False))


def rooms_test() -> StepResult:
    room_no = f"99{datetime.now().strftime('%H%M%S')}"
    room_name = "画面自動テスト部屋"

    js_create = f"""
(() => {{
  const form = document.querySelector('form.form');
  const set = (selector, value, eventName = 'input') => {{
    const el = document.querySelector(selector);
    if (!el) return false;
    if (el.type === 'checkbox') {{
      el.checked = Boolean(value);
    }} else {{
      el.value = value;
    }}
    el.dispatchEvent(new Event(eventName, {{ bubbles: true }}));
    return true;
  }};
  set('input[name="roomNumber"]', '{room_no}');
  set('input[name="roomName"]', '{room_name}');
  set('select[name="roomType"]', 'family', 'change');
  set('input[name="capacity"]', '3');
  set('input[name="basePricePerPerson"]', '14500');
  set('input[name="privateBath"]', true, 'change');
  set('textarea[name="note"]', '画面テスト用');
  const button = form.querySelector('button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{
    roomNo: '{room_no}',
    roomName: '{room_name}',
    roomTypeOptions: Array.from(document.querySelectorAll('select[name="roomType"] option')).map((o) => o.textContent.trim())
  }});
}})()
"""
    result = run_safari_js(f"{BASE_URL}/rooms", js_create)
    if result.returncode != 0:
        return StepResult("rooms:create", False, result.stderr.strip())

    js_update = f"""
(() => {{
  const row = Array.from(document.querySelectorAll('.room-table tbody tr')).find((tr) => tr.innerText.includes('{room_no}'));
  if (!row) return JSON.stringify({{ found: false }});
  const occupancy = row.querySelector('select[name="occupancyStatus"]');
  const cleaning = row.querySelector('select[name="cleaningStatus"]');
  if (occupancy) occupancy.value = 'reserved';
  if (cleaning) cleaning.value = 'needs_cleaning';
  if (occupancy) occupancy.dispatchEvent(new Event('change', {{ bubbles: true }}));
  if (cleaning) cleaning.dispatchEvent(new Event('change', {{ bubbles: true }}));
  const button = row.querySelector('button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{
    found: true,
    roomNo: '{room_no}',
    occupancyValue: occupancy ? occupancy.value : null,
    cleaningValue: cleaning ? cleaning.value : null
  }});
}})()
"""
    result = run_safari_current_tab(js_update)
    if result.returncode != 0:
        return StepResult("rooms:update", False, result.stderr.strip())

    js_verify_update = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    hasRoom: text.includes('{room_no}'),
    hasRoomName: text.includes('{room_name}'),
    hasReserved: text.includes('予約済'),
    hasCleaning: text.includes('清掃待ち'),
    options: Array.from(document.querySelectorAll('select[name="roomType"] option')).map((o) => o.textContent.trim())
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_update)
    if result.returncode != 0:
        return StepResult("rooms:verify-update", False, result.stderr.strip())
    data = parse_json(result.stdout)
    update_ok = all(
        [
            data.get("hasRoom"),
            data.get("hasRoomName"),
            data.get("hasReserved"),
            data.get("hasCleaning"),
            data.get("options") == ["大号床间", "双人间", "三人间", "家庭间"],
        ]
    )

    js_delete = f"""
(() => {{
  const row = Array.from(document.querySelectorAll('.room-table tbody tr')).find((tr) => tr.innerText.includes('{room_no}'));
  if (!row) return JSON.stringify({{ found: false }});
  const button = row.querySelector('button.danger[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{ found: true, roomNo: '{room_no}' }});
}})()
"""
    result = run_safari_current_tab(js_delete)
    if result.returncode != 0:
        return StepResult("rooms:delete", False, result.stderr.strip())

    js_verify_delete = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    activeMissing: !text.includes('{room_no}') || !text.includes('{room_name}'),
    deletedShown: text.includes('{room_no}') && text.includes('{room_name}'),
    hasDeletedHeading: text.includes('削除済み部屋一覧')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_delete)
    if result.returncode != 0:
        return StepResult("rooms:verify-delete", False, result.stderr.strip())
    data = parse_json(result.stdout)
    ok = update_ok and data.get("hasDeletedHeading") and data.get("deletedShown")
    return StepResult("rooms", ok, json.dumps({"roomNo": room_no, **data}, ensure_ascii=False))


def reservations_test() -> StepResult:
    guest_name = "画面テスト太郎"
    companion_name = "画面同行者花子"
    no_contact_guest_name = "画面連絡なし太郎"

    js_fill = f"""
(() => {{
  const form = document.querySelector('.form');
  const set = (selector, value, eventName = 'input') => {{
    const el = document.querySelector(selector);
    if (!el) return false;
    if (el.type === 'checkbox') {{
      el.checked = Boolean(value);
    }} else {{
      el.value = value;
    }}
    el.dispatchEvent(new Event(eventName, {{ bubbles: true }}));
    return true;
  }};
  const room = document.getElementById('roomId');
  if (room && room.options.length > 1) {{
    room.value = room.options[1].value;
    room.dispatchEvent(new Event('change', {{ bubbles: true }}));
  }}
  set('#checkInDate', '2026-09-25');
  set('#checkOutDate', '2026-09-27');
  set('#guestName', '{guest_name}');
  set('#guestKana', 'ガメンテストタロウ');
  set('#guestGender', '男性', 'change');
  set('#guestAge', '31');
  set('#guestPhone', '09000000000');
  set('#guestEmail', 'screen.test@example.com');
  set('#guestCountInput', '2');
  set('#reservationForm', '電話', 'change');
  set('#paymentStatus', 'unpaid', 'change');
  set('#note', '画面テスト用');
  const invalidHint = form.querySelector('[data-hint-for="guestPhone"]')?.textContent || '';
  set('#guestPhone', '090-1234-5678');
  const row = document.querySelector('.companion-row');
  if (row) {{
    const fill = (selector, value, eventName = 'input') => {{
      const el = row.querySelector(selector);
      if (!el) return false;
      el.value = value;
      el.dispatchEvent(new Event(eventName, {{ bubbles: true }}));
      return true;
    }};
    fill('input[name="companionNames"]', '{companion_name}');
    fill('input[name="companionKanas"]', 'ガメンドウコウシャハナコ');
    fill('select[name="companionGenders"]', '女性', 'change');
    fill('input[name="companionAges"]', '28');
    fill('input[name="companionPhones"]', '080-1234-5678');
  }}
  const button = form.querySelector('button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{
    invalidHint,
    companionVisible: !document.getElementById('companionPanel').hidden,
    phoneHintAfterFix: form.querySelector('[data-hint-for="guestPhone"]')?.textContent || ''
  }});
}})()
"""
    result = run_safari_js(f"{BASE_URL}/reservations", js_fill)
    if result.returncode != 0:
        return StepResult("reservations:create", False, result.stderr.strip())
    data = parse_json(result.stdout)
    fill_ok = data.get("companionVisible") and "000-0000-0000" in data.get("invalidHint", "")

    js_verify_create = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    hasSuccess: text.includes('予約を登録しました。'),
    hasGuest: text.includes('{guest_name}'),
    hasCompanion: text.includes('{companion_name}'),
    hasReservationList: text.includes('予約一覧')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_create)
    if result.returncode != 0:
        return StepResult("reservations:verify-create", False, result.stderr.strip())
    create_data = parse_json(result.stdout)
    create_ok = create_data.get("hasSuccess") and create_data.get("hasGuest") and create_data.get("hasCompanion")

    js_payment = f"""
(() => {{
  const row = Array.from(document.querySelectorAll('.reservation-table tbody tr')).find((tr) => tr.innerText.includes('{guest_name}'));
  if (!row) return JSON.stringify({{ found: false }});
  const select = row.querySelector('form[action$="/payment"] select[name="paymentStatus"]');
  const button = row.querySelector('form[action$="/payment"] button[type="submit"]');
  if (select) select.value = 'paid';
  if (select) select.dispatchEvent(new Event('change', {{ bubbles: true }}));
  if (button) button.click();
  return JSON.stringify({{ found: true, payment: select ? select.value : null }});
}})()
"""
    result = run_safari_current_tab(js_payment)
    if result.returncode != 0:
        return StepResult("reservations:payment", False, result.stderr.strip())
    js_verify_payment = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    hasPaymentMessage: text.includes('支払い状況を更新しました。'),
    hasPaid: text.includes('支払済')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_payment)
    if result.returncode != 0:
        return StepResult("reservations:verify-payment", False, result.stderr.strip())
    payment_data = parse_json(result.stdout)
    payment_ok = payment_data.get("hasPaymentMessage") and payment_data.get("hasPaid")

    js_cancel = f"""
(() => {{
  const row = Array.from(document.querySelectorAll('.reservation-table tbody tr')).find((tr) => tr.innerText.includes('{guest_name}'));
  if (!row) return JSON.stringify({{ found: false }});
  const button = row.querySelector('form[action$="/cancel"] button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{ found: true }});
}})()
"""
    result = run_safari_current_tab(js_cancel)
    if result.returncode != 0:
        return StepResult("reservations:cancel", False, result.stderr.strip())
    js_verify_cancel = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    hasCancelMessage: text.includes('予約をキャンセルしました。'),
    hasCancelledHeading: text.includes('取消予約一覧'),
    cancelledShown: text.includes('{guest_name}')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_cancel)
    if result.returncode != 0:
        return StepResult("reservations:verify-cancel", False, result.stderr.strip())
    cancel_data = parse_json(result.stdout)
    cancel_ok = cancel_data.get("hasCancelMessage") and cancel_data.get("hasCancelledHeading") and cancel_data.get("cancelledShown")

    js_no_contact = f"""
(() => {{
  const form = document.querySelector('.form');
  const set = (selector, value, eventName = 'input') => {{
    const el = document.querySelector(selector);
    if (!el) return false;
    if (el.type === 'checkbox') {{
      el.checked = Boolean(value);
    }} else {{
      el.value = value;
    }}
    el.dispatchEvent(new Event(eventName, {{ bubbles: true }}));
    return true;
  }};
  const room = document.getElementById('roomId');
  if (room && room.options.length > 1) {{
    room.value = room.options[1].value;
    room.dispatchEvent(new Event('change', {{ bubbles: true }}));
  }}
  set('#checkInDate', '2026-09-28');
  set('#checkOutDate', '2026-09-29');
  set('#guestName', '{no_contact_guest_name}');
  set('#guestKana', 'ガメンレンラクナシタロウ');
  set('#guestGender', '男性', 'change');
  set('#guestAge', '40');
  set('#guestCountInput', '1');
  set('#noPhoneInfo', true, 'change');
  set('#noEmailInfo', true, 'change');
  const phone = document.getElementById('guestPhone');
  const email = document.getElementById('guestEmail');
  const phoneDisabled = phone ? phone.disabled : false;
  const emailDisabled = email ? email.disabled : false;
  const button = form.querySelector('button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{
    roomValue: room ? room.value : null,
    phoneDisabled,
    emailDisabled,
    noContactInfo: phoneDisabled && emailDisabled
  }});
}})()
"""
    result = run_safari_current_tab(js_no_contact)
    if result.returncode != 0:
        return StepResult("reservations:no-contact-create", False, result.stderr.strip())
    no_contact_data = parse_json(result.stdout)
    time.sleep(2)

    js_verify_no_contact = f"""
(() => {{
  const text = document.body.innerText;
  const row = Array.from(document.querySelectorAll('.reservation-table tbody tr')).find((tr) => tr.innerText.includes('{no_contact_guest_name}'));
    return JSON.stringify({{
      hasSuccess: text.includes('予約を登録しました。'),
      hasGuest: text.includes('{no_contact_guest_name}'),
      rowHasGuest: Boolean(row),
      rowHasPhoneLabel: row ? row.innerText.includes('電話') : false,
      rowHasEmail: row ? row.innerText.includes('email') : false,
      bodyPreview: text.slice(0, 200)
    }});
}})()
"""
    result = run_safari_current_tab(js_verify_no_contact)
    if result.returncode != 0:
        return StepResult("reservations:no-contact-verify", False, result.stderr.strip())
    no_contact_verify = parse_json(result.stdout)

    js_no_contact_cleanup = f"""
(() => {{
  const row = Array.from(document.querySelectorAll('.reservation-table tbody tr')).find((tr) => tr.innerText.includes('{no_contact_guest_name}'));
  if (!row) return JSON.stringify({{ found: false }});
  const button = row.querySelector('form[action$="/cancel"] button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{ found: true }});
}})()
"""
    result = run_safari_current_tab(js_no_contact_cleanup)
    if result.returncode != 0:
        return StepResult("reservations:no-contact-cleanup", False, result.stderr.strip())

    ok = fill_ok and create_ok and payment_ok and cancel_ok and no_contact_data.get("phoneDisabled") and no_contact_data.get("emailDisabled") and no_contact_verify.get("hasSuccess") and no_contact_verify.get("hasGuest") and no_contact_verify.get("rowHasGuest")
    return StepResult("reservations", ok, json.dumps({
        "invalidHint": data.get("invalidHint"),
        "create": create_data,
        "payment": payment_data,
        "cancel": cancel_data,
        "noContact": {
            "input": no_contact_data,
            "verify": no_contact_verify,
        },
    }, ensure_ascii=False))


def prices_test() -> StepResult:
    rule_a = f"自動料金A-{datetime.now().strftime('%H%M%S')}"
    rule_b = f"自動料金B-{datetime.now().strftime('%H%M%S')}"

    def create_rule(name: str, start: str, end: str, price: str, priority: str) -> subprocess.CompletedProcess[str]:
        js = f"""
(() => {{
  const form = document.querySelector('form.form');
  const set = (selector, value, eventName = 'input') => {{
    const el = document.querySelector(selector);
    if (!el) return false;
    if (el.type === 'checkbox') {{
      el.checked = Boolean(value);
    }} else {{
      el.value = value;
    }}
    el.dispatchEvent(new Event(eventName, {{ bubbles: true }}));
    return true;
  }};
  const room = document.querySelector('select[name="roomId"]');
  if (room && room.options.length > 1) {{
    room.value = room.options[1].value;
    room.dispatchEvent(new Event('change', {{ bubbles: true }}));
  }}
  set('input[name="ruleName"]', '{name}');
  set('input[name="startDate"]', '{start}');
  set('input[name="endDate"]', '{end}');
  set('input[name="pricePerPerson"]', '{price}');
  set('input[name="priority"]', '{priority}');
  set('textarea[name="note"]', '画面テスト用');
  const button = form.querySelector('button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{ name: '{name}', roomValue: room ? room.value : null }});
}})()
"""
        return run_safari_js(f"{BASE_URL}/prices", js)

    result = create_rule(rule_a, "2026-09-01", "2026-09-07", "12000", "1")
    if result.returncode != 0:
        return StepResult("prices:create-a", False, result.stderr.strip())
    result = create_rule(rule_b, "2026-09-08", "2026-09-14", "13500", "2")
    if result.returncode != 0:
        return StepResult("prices:create-b", False, result.stderr.strip())

    js_verify = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    hasA: text.includes('{rule_a}'),
    hasB: text.includes('{rule_b}'),
    hasBulkDelete: text.includes('選択削除')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify)
    if result.returncode != 0:
        return StepResult("prices:verify-create", False, result.stderr.strip())
    create_data = parse_json(result.stdout)
    create_ok = create_data.get("hasA") and create_data.get("hasB") and create_data.get("hasBulkDelete")

    js_bulk_delete = f"""
(() => {{
  const rows = Array.from(document.querySelectorAll('.price-table tbody tr')).filter((tr) => tr.innerText.includes('{rule_a}') || tr.innerText.includes('{rule_b}'));
  rows.forEach((row) => {{
    const checkbox = row.querySelector('input[type="checkbox"][name="ids"]');
    if (checkbox) checkbox.checked = true;
  }});
  const button = document.querySelector('.price-bulk-form button[type="submit"]');
  if (button) button.click();
  return JSON.stringify({{ selected: rows.length }});
}})()
"""
    result = run_safari_current_tab(js_bulk_delete)
    if result.returncode != 0:
        return StepResult("prices:bulk-delete", False, result.stderr.strip())

    js_verify_delete = f"""
(() => {{
  const text = document.body.innerText;
  return JSON.stringify({{
    missingA: !text.includes('{rule_a}'),
    missingB: !text.includes('{rule_b}'),
    stillHasHeading: text.includes('料金ルール一覧')
  }});
}})()
"""
    result = run_safari_current_tab(js_verify_delete)
    if result.returncode != 0:
        return StepResult("prices:verify-delete", False, result.stderr.strip())
    delete_data = parse_json(result.stdout)
    ok = create_ok and delete_data.get("missingA") and delete_data.get("missingB") and delete_data.get("stillHasHeading")
    return StepResult("prices", ok, json.dumps({
        "create": create_data,
        "delete": delete_data,
        "rules": [rule_a, rule_b],
    }, ensure_ascii=False))


def main() -> int:
    EVIDENCE_DIR.mkdir(exist_ok=True)
    steps = [
        dashboard_test(),
        rooms_test(),
        reservations_test(),
        prices_test(),
    ]
    lines = [
        "画面自動テスト証跡",
        f"実行日時: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"対象URL: {BASE_URL}",
        "",
    ]
    overall_ok = True
    for step in steps:
        overall_ok = overall_ok and step.ok
        lines.append(f"[{'OK' if step.ok else 'NG'}] {step.name}")
        lines.append(step.details)
        lines.append("")
    lines.append(f"総合結果: {'OK' if overall_ok else 'NG'}")
    REPORT_PATH.write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))
    return 0 if overall_ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
