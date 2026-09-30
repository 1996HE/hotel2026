import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useI18n } from "./i18n.jsx";
import {
  bandPlacement,
  calendarEvents,
  daysForMonth,
  intervalHasConflict,
  normalizeMonth,
  normalizedRange,
  roomLabel,
  shiftMonth,
  todayIso,
} from "./roomCalendarModel.js";

const STATUS_LABELS = {
  pending: "確認待ち",
  booked: "予約確定",
  checked_in: "滞在中",
  checked_out: "退室済み",
  cancelled: "取消済み",
  stop_sale: "販売停止",
  maintenance: "メンテナンス",
};

const emptyReservation = {
  guestName: "",
  guestKana: "",
  guestGender: "",
  guestAge: "",
  guestPhone: "",
  guestEmail: "",
  guestCount: 1,
  reservationForm: "公式",
  note: "",
  noPhoneInfo: false,
  noEmailInfo: false,
  companions: [],
};

function actionAllowed(detail, action) {
  const value = detail?.allowedActions;
  const aliases = {
    confirm: ["confirm", "confirmGroup"],
    reject: ["reject", "rejectGroup"],
    groupCancel: ["groupCancel", "cancelGroup"],
    cancel: ["cancel", "cancelReservation"],
    checkIn: ["checkIn", "checkInRoom"],
    checkOut: ["checkOut", "checkOutRoom"],
    clean: ["clean", "cleanRoom"],
  };
  const candidates = aliases[action] || [action];
  if (Array.isArray(value)) return candidates.some((candidate) => value.includes(candidate));
  if (value && typeof value === "object") return Boolean(value[action]);
  return false;
}

function Modal({ title, onClose, children, footer }) {
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div
      className="calendar-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <section className="calendar-modal" role="dialog" aria-modal="true" aria-labelledby="calendar-modal-title">
        <header>
          <h2 id="calendar-modal-title">{title}</h2>
          <button type="button" className="icon-button" aria-label="閉じる" onClick={onClose}>
            ×
          </button>
        </header>
        <div className="calendar-modal-body">{children}</div>
        {footer ? <footer>{footer}</footer> : null}
      </section>
    </div>
  );
}

function CompanionInputs({ form, setForm }) {
  const count = Math.max(0, Number(form.guestCount || 1) - 1);
  useEffect(() => {
    setForm((current) => {
      const companions = Array.from({ length: count }, (_, index) => current.companions?.[index] || {});
      return companions.length === current.companions?.length ? current : { ...current, companions };
    });
  }, [count, setForm]);
  if (!count) return null;
  return (
    <div className="calendar-companions">
      <strong>同行者情報</strong>
      {Array.from({ length: count }, (_, index) => {
        const companion = form.companions?.[index] || {};
        const update = (field, value) =>
          setForm((current) => {
            const companions = [...(current.companions || [])];
            companions[index] = { ...(companions[index] || {}), [field]: value };
            return { ...current, companions };
          });
        return (
          <fieldset key={index}>
            <legend>同行者 {index + 1}</legend>
            <label>
              氏名
              <input required value={companion.name || ""} onChange={(event) => update("name", event.target.value)} />
            </label>
            <label>
              フリガナ
              <input value={companion.kana || ""} onChange={(event) => update("kana", event.target.value)} />
            </label>
          </fieldset>
        );
      })}
    </div>
  );
}

export default function RoomCalendar({ request }) {
  const { t } = useI18n();
  const [month, setMonth] = useState(() => normalizeMonth(new URLSearchParams(window.location.search).get("month")));
  const [data, setData] = useState({ rooms: [], stays: [], blocks: [], today: todayIso() });
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState({});
  const [selection, setSelection] = useState(null);
  const [rangeDialog, setRangeDialog] = useState(null);
  const [detailDialog, setDetailDialog] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [reservationForm, setReservationForm] = useState(emptyReservation);
  const [rangeMode, setRangeMode] = useState("reservation");
  const [blockReason, setBlockReason] = useState("");
  const [blockEditor, setBlockEditor] = useState(null);
  const [actionReason, setActionReason] = useState("");
  const [busy, setBusy] = useState(false);
  const requestSequence = useRef(0);

  const days = useMemo(() => daysForMonth(month), [month]);
  const events = useMemo(() => calendarEvents(data), [data]);
  const load = useCallback(
    async ({ quiet = false } = {}) => {
      const sequence = ++requestSequence.current;
      if (!quiet) setLoading(true);
      try {
        const result = await request(`/api/room-calendar?month=${encodeURIComponent(month)}`);
        if (sequence !== requestSequence.current) return;
        setData(result);
        setNotice({});
      } catch (error) {
        if (sequence === requestSequence.current) setNotice({ error: error.message });
      } finally {
        if (sequence === requestSequence.current && !quiet) setLoading(false);
      }
    },
    [month, request]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    params.set("month", month);
    window.history.replaceState({}, "", `${window.location.pathname}?${params.toString()}`);
    setSelection(null);
    load();
  }, [load, month]);

  useEffect(() => {
    const refresh = () => document.visibilityState === "visible" && load({ quiet: true });
    const timer = window.setInterval(refresh, 30_000);
    const onVisibility = () => document.visibilityState === "visible" && refresh();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [load]);

  const closeRangeDialog = useCallback(() => {
    setRangeDialog(null);
    setSelection(null);
    setReservationForm(emptyReservation);
    setRangeMode("reservation");
    setBlockReason("");
  }, []);

  const closeDetailDialog = useCallback(() => {
    setDetailDialog(null);
    setBlockEditor(null);
    setActionReason("");
  }, []);

  const selectDate = (room, date) => {
    if (date < data.today) return;
    if (!selection || String(selection.roomId) !== String(room.id)) {
      setSelection({ roomId: room.id, firstDate: date });
      return;
    }
    if (date <= selection.firstDate) {
      setNotice({ error: "終了日は開始日の翌日以降を選択してください。" });
      if (date < selection.firstDate) {
        setSelection({ roomId: room.id, firstDate: date });
      }
      return;
    }
    const range = normalizedRange(selection.firstDate, date);
    if (intervalHasConflict(events, room.id, range.startDate, range.endDateExclusive)) {
      setNotice({ error: "選択期間には予約、販売停止、またはメンテナンスがあります。" });
      setSelection(null);
      return;
    }
    setRangeDialog({ room, ...range });
  };

  const openStay = async (stay) => {
    setDetailLoading(true);
    setDetailDialog({ kind: "stay", event: stay, detail: null });
    try {
      const detail = await request(`/api/room-calendar/reservations/${stay.id}`);
      setDetailDialog({ kind: "stay", event: stay, detail });
    } catch (error) {
      setNotice({ error: error.message });
      setDetailDialog(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const openBlock = (block) => {
    const editor = {
      blockType: block.blockType,
      startDate: block.startDate,
      endDateExclusive: block.endDateExclusive,
      reason: block.reason,
      version: block.version,
      releaseStart: block.startDate,
      releaseEndExclusive: block.endDateExclusive,
      releaseReason: "",
    };
    setBlockEditor(editor);
    setDetailDialog({ kind: "block", event: block });
  };

  const runMutation = async (operation, successMessage, after = closeDetailDialog) => {
    setBusy(true);
    try {
      await operation();
      setNotice({ message: successMessage });
      after?.();
      await load({ quiet: true });
    } catch (error) {
      setNotice({ error: error.message });
      if (error.status === 409) await load({ quiet: true });
    } finally {
      setBusy(false);
    }
  };

  const submitRange = (event) => {
    event.preventDefault();
    if (rangeMode === "reservation") {
      const companions = reservationForm.companions || [];
      const reservation = {
        roomId: Number(rangeDialog.room.id),
        checkInDate: rangeDialog.startDate,
        checkOutDate: rangeDialog.endDateExclusive,
        guestName: reservationForm.guestName,
        guestKana: reservationForm.guestKana,
        guestGender: reservationForm.guestGender,
        guestAge: Number(reservationForm.guestAge || 0) || null,
        guestPhone: reservationForm.noPhoneInfo ? null : reservationForm.guestPhone,
        guestEmail: reservationForm.noEmailInfo ? null : reservationForm.guestEmail,
        guestCount: Number(reservationForm.guestCount),
        reservationForm: reservationForm.reservationForm,
        note: reservationForm.note,
      };
      runMutation(
        () =>
          request("/api/reservations", {
            method: "POST",
            body: JSON.stringify({
              reservation,
              noPhoneInfo: reservationForm.noPhoneInfo,
              noEmailInfo: reservationForm.noEmailInfo,
              companionNames: companions.map((item) => item.name || ""),
              companionKanas: companions.map((item) => item.kana || ""),
              companionGenders: companions.map((item) => item.gender || ""),
              companionAges: companions.map((item) => Number(item.age || 0) || null),
              companionPhones: companions.map((item) => item.phone || ""),
            }),
          }),
        "予約を登録しました。",
        closeRangeDialog
      );
      return;
    }
    runMutation(
      () =>
        request("/api/room-calendar/blocks", {
          method: "POST",
          body: JSON.stringify({
            roomId: Number(rangeDialog.room.id),
            blockType: rangeMode,
            startDate: rangeDialog.startDate,
            endDateExclusive: rangeDialog.endDateExclusive,
            reason: blockReason,
          }),
        }),
      rangeMode === "maintenance" ? "メンテナンス期間を登録しました。" : "販売停止期間を登録しました。",
      closeRangeDialog
    );
  };

  const stayAction = (action) => {
    const { event, detail } = detailDialog;
    const bookingId = detail?.bookingRequest?.id || event.bookingRequestId;
    const endpoint = {
      confirm: `/api/booking-requests/${bookingId}/confirm`,
      reject: `/api/booking-requests/${bookingId}/reject`,
      groupCancel: `/api/booking-requests/${bookingId}/cancel`,
      checkIn: `/api/reservations/${event.id}/check-in`,
      checkOut: `/api/reservations/${event.id}/check-out`,
      cancel: `/api/reservations/${event.id}/cancel`,
      clean: `/api/rooms/${event.roomId}/cleaning-status`,
    }[action];
    const needsReason = ["reject", "groupCancel", "cancel"].includes(action);
    if (needsReason && !actionReason.trim()) {
      setNotice({ error: "理由を入力してください。" });
      return;
    }
    const body =
      action === "clean" ? { cleaningStatus: "cleaned" } : needsReason ? { reason: actionReason.trim() } : undefined;
    runMutation(
      () => request(endpoint, { method: "POST", ...(body ? { body: JSON.stringify(body) } : {}) }),
      "処理が完了しました。"
    );
  };

  const updateBlock = (event) => {
    event.preventDefault();
    const block = detailDialog.event;
    runMutation(
      () =>
        request(`/api/room-calendar/blocks/${block.id}`, {
          method: "PUT",
          body: JSON.stringify({
            roomId: Number(block.roomId),
            blockType: blockEditor.blockType,
            startDate: blockEditor.startDate,
            endDateExclusive: blockEditor.endDateExclusive,
            reason: blockEditor.reason,
            version: blockEditor.version,
          }),
        }),
      "期間を更新しました。"
    );
  };

  const releaseBlock = () => {
    const block = detailDialog.event;
    runMutation(
      () =>
        request(`/api/room-calendar/blocks/${block.id}/release`, {
          method: "POST",
          body: JSON.stringify({
            startDate: blockEditor.releaseStart,
            endDateExclusive: blockEditor.releaseEndExclusive,
            reason: blockEditor.releaseReason,
            version: blockEditor.version,
          }),
        }),
      "指定期間を解除しました。"
    );
  };

  return (
    <main className="page room-calendar-page" id="main-content">
      <header className="page-header">
        <div>
          <p className="eyebrow">ROOM TAPE CHART</p>
          <h1>{t("房態カレンダー", "房态日历")}</h1>
          <p className="page-description">
            {t("客室ごとの予約・販売停止・メンテナンスを月単位で管理します。", "按月管理各房间的预约、停售和维修。")}
          </p>
        </div>
      </header>
      {notice.error || notice.message ? (
        <div
          className={`calendar-notice ${notice.error ? "error" : "success"}`}
          role={notice.error ? "alert" : "status"}
        >
          <span>{notice.error || notice.message}</span>
          <button type="button" onClick={() => setNotice({})} aria-label="閉じる">
            ×
          </button>
        </div>
      ) : null}
      <section className="panel calendar-panel">
        <div className="calendar-toolbar">
          <div className="calendar-month-controls">
            <button type="button" onClick={() => setMonth((value) => shiftMonth(value, -1))}>
              ← 前月
            </button>
            <button type="button" onClick={() => setMonth(normalizeMonth(data.today?.slice(0, 7), month))}>
              今日
            </button>
            <button type="button" onClick={() => setMonth((value) => shiftMonth(value, 1))}>
              翌月 →
            </button>
            <input
              aria-label="表示月"
              type="month"
              value={month}
              onChange={(event) => setMonth(normalizeMonth(event.target.value, month))}
            />
          </div>
          <div className="calendar-legend" aria-label="状態凡例">
            {[
              ["pending", "確認待ち"],
              ["booked", "予約確定"],
              ["checked_in", "滞在中"],
              ["stop_sale", "販売停止"],
              ["maintenance", "メンテナンス"],
            ].map(([status, label]) => (
              <span key={status}>
                <i className={`calendar-dot is-${status}`} />
                {label}
              </span>
            ))}
          </div>
        </div>
        <p className="calendar-help">
          空いている開始日と退房日／販売再開日を順にクリックします。2回目の日付は宿泊・停止期間に含まれません。
        </p>
        <div className={`calendar-viewport ${loading ? "is-loading" : ""}`} aria-busy={loading}>
          <div className="calendar-grid-header" style={{ "--calendar-days": days.length }}>
            <div className="calendar-room-heading">客室</div>
            {days.map((day) => (
              <div
                key={day.iso}
                className={`calendar-day-heading ${day.weekend ? "is-weekend" : ""} ${day.iso === data.today ? "is-today" : ""}`}
              >
                <strong>{day.day}</strong>
                <small>{["日", "月", "火", "水", "木", "金", "土"][day.weekday]}</small>
              </div>
            ))}
          </div>
          {(data.rooms || []).map((room) => {
            const roomEvents = events.filter((item) => String(item.roomId) === String(room.id));
            return (
              <div className="calendar-room-row" style={{ "--calendar-days": days.length }} key={room.id}>
                <div className="calendar-room-label">
                  <strong>{roomLabel(room)}</strong>
                  <small>定員 {room.capacity}名</small>
                  {room.cleaningStatus === "needs_cleaning" ? <span className="cleaning-badge">清掃待ち</span> : null}
                </div>
                {days.map((day) => {
                  const selected =
                    selection && String(selection.roomId) === String(room.id) && selection.firstDate === day.iso;
                  return (
                    <button
                      type="button"
                      key={day.iso}
                      className={`calendar-cell ${day.weekend ? "is-weekend" : ""} ${day.iso === data.today ? "is-today" : ""} ${day.iso < data.today ? "is-past" : ""} ${selected ? "is-selected" : ""}`}
                      style={{ gridColumn: day.day + 1, gridRow: 1 }}
                      disabled={day.iso < data.today}
                      aria-label={`${roomLabel(room)} ${day.iso}`}
                      onClick={() => selectDate(room, day.iso)}
                    />
                  );
                })}
                {roomEvents.map((event) => {
                  const placement = bandPlacement(event.startDate, event.endDateExclusive, month);
                  if (!placement) return null;
                  const status = event.kind === "block" ? event.blockType : event.status;
                  const label =
                    event.kind === "block"
                      ? STATUS_LABELS[event.blockType]
                      : `${event.guestName || event.reservationNo} · ${STATUS_LABELS[event.status] || event.status}`;
                  return (
                    <button
                      type="button"
                      key={`${event.kind}-${event.id}`}
                      className={`calendar-band is-${status} ${event.conflict ? "has-conflict" : ""}`}
                      style={{ gridColumn: `${placement.gridColumnStart} / ${placement.gridColumnEnd}`, gridRow: 1 }}
                      title={`${label} ${event.startDate}–${event.endDateExclusive}`}
                      onClick={() => (event.kind === "block" ? openBlock(event) : openStay(event))}
                    >
                      {placement.clippedAtStart ? "← " : ""}
                      {label}
                      {placement.clippedAtEnd ? " →" : ""}
                    </button>
                  );
                })}
              </div>
            );
          })}
          {!loading && !(data.rooms || []).length ? (
            <p className="calendar-empty">表示できる客室がありません。</p>
          ) : null}
        </div>
        <footer className="calendar-footer">最終更新: {data.generatedAt || "—"} · 画面表示中は30秒ごとに更新</footer>
      </section>

      {rangeDialog ? (
        <Modal
          title={`${roomLabel(rangeDialog.room)} · ${rangeDialog.startDate}〜${rangeDialog.endDateExclusive}`}
          onClose={closeRangeDialog}
        >
          <form className="calendar-dialog-form" onSubmit={submitRange}>
            <label>
              登録内容
              <select value={rangeMode} onChange={(event) => setRangeMode(event.target.value)}>
                <option value="reservation">予約を登録</option>
                <option value="stop_sale">販売停止</option>
                <option value="maintenance">メンテナンス</option>
              </select>
            </label>
            {rangeMode === "reservation" ? (
              <>
                <label>
                  宿泊者名
                  <input
                    required
                    value={reservationForm.guestName}
                    onChange={(event) => setReservationForm({ ...reservationForm, guestName: event.target.value })}
                  />
                </label>
                <label>
                  フリガナ
                  <input
                    value={reservationForm.guestKana}
                    onChange={(event) => setReservationForm({ ...reservationForm, guestKana: event.target.value })}
                  />
                </label>
                <div className="form-grid form-grid-2">
                  <label>
                    人数
                    <input
                      required
                      min="1"
                      max={rangeDialog.room.capacity || 10}
                      type="number"
                      value={reservationForm.guestCount}
                      onChange={(event) =>
                        setReservationForm({ ...reservationForm, guestCount: Number(event.target.value) })
                      }
                    />
                  </label>
                  <label>
                    予約形式
                    <select
                      value={reservationForm.reservationForm}
                      onChange={(event) =>
                        setReservationForm({ ...reservationForm, reservationForm: event.target.value })
                      }
                    >
                      <option>公式</option>
                      <option>電話</option>
                      <option>メール</option>
                      <option>予約サイト</option>
                      <option>現地</option>
                    </select>
                  </label>
                  <label>
                    電話
                    <input
                      type="tel"
                      disabled={reservationForm.noPhoneInfo}
                      value={reservationForm.guestPhone}
                      onChange={(event) => setReservationForm({ ...reservationForm, guestPhone: event.target.value })}
                    />
                  </label>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={reservationForm.noPhoneInfo}
                      onChange={(event) =>
                        setReservationForm({ ...reservationForm, noPhoneInfo: event.target.checked, guestPhone: "" })
                      }
                    />
                    電話なし
                  </label>
                  <label>
                    メール
                    <input
                      type="email"
                      disabled={reservationForm.noEmailInfo}
                      value={reservationForm.guestEmail}
                      onChange={(event) => setReservationForm({ ...reservationForm, guestEmail: event.target.value })}
                    />
                  </label>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={reservationForm.noEmailInfo}
                      onChange={(event) =>
                        setReservationForm({ ...reservationForm, noEmailInfo: event.target.checked, guestEmail: "" })
                      }
                    />
                    メールなし
                  </label>
                </div>
                <CompanionInputs form={reservationForm} setForm={setReservationForm} />
                <label>
                  メモ
                  <textarea
                    value={reservationForm.note}
                    onChange={(event) => setReservationForm({ ...reservationForm, note: event.target.value })}
                  />
                </label>
              </>
            ) : (
              <label>
                理由
                <textarea
                  required
                  minLength="2"
                  value={blockReason}
                  onChange={(event) => setBlockReason(event.target.value)}
                />
              </label>
            )}
            <div className="calendar-form-actions">
              <button type="button" onClick={closeRangeDialog}>
                取消
              </button>
              <button className="form-submit" type="submit" disabled={busy}>
                {busy ? "処理中..." : "登録"}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {detailDialog?.kind === "stay" ? (
        <Modal title="予約詳細" onClose={closeDetailDialog}>
          {detailLoading || !detailDialog.detail ? (
            <p>読み込み中...</p>
          ) : (
            <div className="calendar-detail">
              <dl>
                <dt>予約番号</dt>
                <dd>{detailDialog.detail.reservation?.reservationNo || detailDialog.event.reservationNo}</dd>
                <dt>宿泊者</dt>
                <dd>{detailDialog.detail.reservation?.guestName || detailDialog.event.guestName}</dd>
                <dt>期間</dt>
                <dd>
                  {detailDialog.event.checkInDate} 〜 {detailDialog.event.checkOutDate}
                </dd>
                <dt>状態</dt>
                <dd>{STATUS_LABELS[detailDialog.event.status] || detailDialog.event.status}</dd>
              </dl>
              {(detailDialog.detail.groupReservations || []).length > 1 ? (
                <ul>
                  {detailDialog.detail.groupReservations.map((item) => (
                    <li key={item.id}>
                      {item.roomNumber || `Room ${item.roomId}`} ·{" "}
                      {STATUS_LABELS[item.reservationStatus || item.status] || item.reservationStatus}
                    </li>
                  ))}
                </ul>
              ) : null}
              {["reject", "groupCancel", "cancel"].some((action) => actionAllowed(detailDialog.detail, action)) ? (
                <label>
                  理由
                  <textarea required value={actionReason} onChange={(event) => setActionReason(event.target.value)} />
                </label>
              ) : null}
              <div className="calendar-detail-actions">
                {actionAllowed(detailDialog.detail, "confirm") ? (
                  <button onClick={() => stayAction("confirm")} disabled={busy}>
                    全体を確認
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "reject") ? (
                  <button className="danger" onClick={() => stayAction("reject")} disabled={busy}>
                    全体を拒否
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "groupCancel") ? (
                  <button className="danger" onClick={() => stayAction("groupCancel")} disabled={busy}>
                    全体を取消
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "cancel") ? (
                  <button className="danger" onClick={() => stayAction("cancel")} disabled={busy}>
                    予約を取消
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "checkIn") ? (
                  <button onClick={() => stayAction("checkIn")} disabled={busy}>
                    この部屋をチェックイン
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "checkOut") ? (
                  <button onClick={() => stayAction("checkOut")} disabled={busy}>
                    この部屋をチェックアウト
                  </button>
                ) : null}
                {actionAllowed(detailDialog.detail, "clean") ? (
                  <button onClick={() => stayAction("clean")} disabled={busy}>
                    清掃完了
                  </button>
                ) : null}
              </div>
            </div>
          )}
        </Modal>
      ) : null}

      {detailDialog?.kind === "block" && blockEditor ? (
        <Modal title={STATUS_LABELS[detailDialog.event.blockType]} onClose={closeDetailDialog}>
          <form className="calendar-dialog-form" onSubmit={updateBlock}>
            <label>
              種別
              <select
                value={blockEditor.blockType}
                onChange={(event) => setBlockEditor({ ...blockEditor, blockType: event.target.value })}
              >
                <option value="stop_sale">販売停止</option>
                <option value="maintenance">メンテナンス</option>
              </select>
            </label>
            <div className="form-grid form-grid-2">
              <label>
                開始日
                <input
                  type="date"
                  min={data.today}
                  value={blockEditor.startDate}
                  onChange={(event) => setBlockEditor({ ...blockEditor, startDate: event.target.value })}
                />
              </label>
              <label>
                販売再開日
                <input
                  type="date"
                  min={data.today}
                  value={blockEditor.endDateExclusive}
                  onChange={(event) => setBlockEditor({ ...blockEditor, endDateExclusive: event.target.value })}
                />
              </label>
            </div>
            <label>
              理由
              <textarea
                required
                value={blockEditor.reason}
                onChange={(event) => setBlockEditor({ ...blockEditor, reason: event.target.value })}
              />
            </label>
            <button className="form-submit" type="submit" disabled={busy}>
              変更を保存
            </button>
          </form>
          <section className="calendar-release-panel">
            <h3>全部／一部解除</h3>
            <div className="form-grid form-grid-2">
              <label>
                解除開始日
                <input
                  type="date"
                  value={blockEditor.releaseStart}
                  onChange={(event) => setBlockEditor({ ...blockEditor, releaseStart: event.target.value })}
                />
              </label>
              <label>
                解除終了日
                <input
                  type="date"
                  value={blockEditor.releaseEndExclusive}
                  onChange={(event) => setBlockEditor({ ...blockEditor, releaseEndExclusive: event.target.value })}
                />
              </label>
            </div>
            <label>
              解除理由
              <textarea
                required
                value={blockEditor.releaseReason}
                onChange={(event) => setBlockEditor({ ...blockEditor, releaseReason: event.target.value })}
              />
            </label>
            <button
              type="button"
              className="danger"
              disabled={busy || !blockEditor.releaseReason.trim()}
              onClick={releaseBlock}
            >
              指定期間を解除
            </button>
          </section>
        </Modal>
      ) : null}
    </main>
  );
}
