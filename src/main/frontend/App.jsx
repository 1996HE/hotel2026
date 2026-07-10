import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
const appScript = document.querySelector('script[src$="/js/app.js"]')?.getAttribute("src") || "";
const contextPath = appScript.endsWith("/js/app.js") ? appScript.slice(0, -"/js/app.js".length) : "";
const withContext = (path) => `${contextPath}${path}`;
const currentRoute = () => {
  const path = window.location.pathname;
  const route = contextPath && path.startsWith(contextPath) ? path.slice(contextPath.length) : path;
  return route === "/" || route === "" ? "/dashboard" : route;
};

async function api(path, options = {}) {
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (options.body && !(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }
  if (csrfToken && options.method && options.method !== "GET") {
    headers[csrfHeader] = csrfToken;
  }
  const response = await fetch(withContext(path), { ...options, headers });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || "処理に失敗しました。");
  }
  return data;
}

const yen = (value) => `¥${value ?? 0}`;
const text = (value, fallback) => (value === null || value === undefined || value === "" ? fallback : value);
const PAGE_SIZE = 5;
const roomTypeLabel = (value) =>
  ({ washitsu: "和室", yoshitsu: "洋室", suite: "スイート", family: "ファミリー" })[value] || "その他";
const occupancyLabel = (value) => ({ vacant: "空室", reserved: "予約済", occupied: "滞在中" })[value] || value;
const cleaningLabel = (value) => (value === "cleaned" ? "清掃済" : "清掃待ち");
const totalPages = (count) => Math.max(1, Math.ceil((count || 0) / PAGE_SIZE));
const slicePage = (items, page) => items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
const padPage = (items) => Array.from({ length: PAGE_SIZE }, (_, index) => items[index] || null);

function Nav({ route }) {
  const items = [
    ["/dashboard", "予約一覧"],
    ["/rooms", "部屋"],
    ["/reservations", "予約"],
    ["/prices", "料金"],
  ];
  return (
    <header className="topbar">
      <a className="brand" href={withContext("/dashboard")}>
        民宿管理
      </a>
      <nav className="nav">
        {items.map(([href, label]) => (
          <a
            key={href}
            href={withContext(href)}
            className={route === href || (route === "/" && href === "/dashboard") ? "active" : ""}
          >
            {label}
          </a>
        ))}
      </nav>
    </header>
  );
}

function Tag({ children, status }) {
  return <span className={`tag ${status || ""}`}>{children}</span>;
}

function Notice({ message, error, onClose }) {
  useEffect(() => {
    if (!message && !error) {
      return undefined;
    }
    const timeout = window.setTimeout(
      () => {
        onClose?.();
      },
      error ? 4500 : 2800
    );
    return () => window.clearTimeout(timeout);
  }, [message, error, onClose]);

  if (!message && !error) return null;
  return (
    <div className={`notice notice-toast ${error ? "error" : "success"}`} role="status" aria-live="polite">
      {error || message}
    </div>
  );
}

function Pager({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="pager">
      <button
        type="button"
        className="pager-link"
        onClick={() => onPageChange(Math.max(1, page - 1))}
        disabled={page <= 1}
      >
        前へ
      </button>
      <span className="pager-state">
        {page} / {totalPages}
      </span>
      <button
        type="button"
        className="pager-link"
        onClick={() => onPageChange(Math.min(totalPages, page + 1))}
        disabled={page >= totalPages}
      >
        次へ
      </button>
    </div>
  );
}

async function loadData(request, setData, setNotice) {
  try {
    setData(await request());
  } catch (error) {
    setNotice({ error: error.message });
  }
}

async function runAction(action, setNotice) {
  try {
    await action();
  } catch (error) {
    setNotice({ error: error.message });
  }
}

function Dashboard() {
  const [data, setData] = useState(null);
  const [notice, setNotice] = useState({});
  useEffect(() => {
    loadData(() => api("/api/dashboard"), setData, setNotice);
  }, []);
  if (!data)
    return (
      <main className="page">
        <Notice {...notice} onClose={() => setNotice({})} />
        <section className="panel">読み込み中...</section>
      </main>
    );
  return (
    <main className="page">
      <section className="hero">
        <p className="eyebrow">民宿管理システム</p>
        <h1>予約一覧</h1>
        <p>宿泊予約、支払い状況、部屋割りを確認します。</p>
      </section>
      <section className="metrics">
        <Metric label="部屋数" value={data.roomCount} />
        <Metric label="空室" value={data.vacantCount} />
        <Metric label="有効予約" value={data.bookedCount} />
      </section>
      <section className="panel">
        <div className="section-title pager-title">
          <h2>予約一覧</h2>
          <a className="button" href={withContext("/reservations")}>
            予約登録
          </a>
        </div>
        <ReservationTable reservations={data.recentReservations.items} compact />
      </section>
    </main>
  );
}

function Metric({ label, value }) {
  return (
    <article className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function Rooms() {
  const [data, setData] = useState({ rooms: [], deletedRooms: [] });
  const [roomPage, setRoomPage] = useState(1);
  const [deletedRoomPage, setDeletedRoomPage] = useState(1);
  const [form, setForm] = useState({
    roomType: "washitsu",
    capacity: 2,
    basePricePerPerson: 8800,
    privateBath: false,
    occupancyStatus: "vacant",
    cleaningStatus: "cleaned",
    active: true,
  });
  const [notice, setNotice] = useState({});
  const load = () => loadData(() => api("/api/rooms"), setData, setNotice);
  useEffect(() => {
    load();
  }, []);
  useEffect(() => {
    setRoomPage((page) => Math.min(page, totalPages(data.rooms.length)));
  }, [data.rooms.length]);
  useEffect(() => {
    setDeletedRoomPage((page) => Math.min(page, totalPages(data.deletedRooms.length)));
  }, [data.deletedRooms.length]);
  const submit = async (event) => {
    event.preventDefault();
    try {
      const res = await api("/api/rooms", { method: "POST", body: JSON.stringify(form) });
      setNotice({ message: res.message });
      setForm({
        roomType: "washitsu",
        capacity: 2,
        basePricePerPerson: 8800,
        privateBath: false,
        occupancyStatus: "vacant",
        cleaningStatus: "cleaned",
        active: true,
      });
      load();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };
  const updateStatus = async (room, occupancyStatus, cleaningStatus) => {
    runAction(async () => {
      const res = await api(`/api/rooms/${room.id}/statuses`, {
        method: "POST",
        body: JSON.stringify({ occupancyStatus, cleaningStatus }),
      });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const deleteRoom = async (room) => {
    runAction(async () => {
      const res = await api(`/api/rooms/${room.id}/delete`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const restoreRoom = async (room) => {
    runAction(async () => {
      const res = await api(`/api/rooms/${room.id}/restore`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const deletePermanently = async (room) => {
    runAction(async () => {
      const res = await api(`/api/rooms/${room.id}/delete-permanently`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const visibleRooms = slicePage(data.rooms, roomPage);
  const visibleDeletedRooms = slicePage(data.deletedRooms, deletedRoomPage);
  const paddedRooms = data.rooms.length ? padPage(visibleRooms) : [];
  const paddedDeletedRooms = data.deletedRooms.length ? padPage(visibleDeletedRooms) : [];
  const roomTotalPages = totalPages(data.rooms.length);
  const deletedRoomTotalPages = totalPages(data.deletedRooms.length);
  return (
    <main className="page grid-page">
      <section className="panel">
        <h1>部屋登録</h1>
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form" onSubmit={submit}>
          <label>
            部屋番号
            <input
              value={form.roomNumber || ""}
              onChange={(e) => setForm({ ...form, roomNumber: e.target.value })}
              placeholder="101"
            />
          </label>
          <label>
            部屋名
            <input
              value={form.roomName || ""}
              onChange={(e) => setForm({ ...form, roomName: e.target.value })}
              placeholder="桜の間"
            />
          </label>
          <label>
            部屋タイプ
            <select value={form.roomType} onChange={(e) => setForm({ ...form, roomType: e.target.value })}>
              <option value="washitsu">和室</option>
              <option value="yoshitsu">洋室</option>
              <option value="suite">スイート</option>
              <option value="family">ファミリー</option>
            </select>
          </label>
          <label>
            定員
            <input
              type="number"
              min="1"
              value={form.capacity || ""}
              onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })}
            />
          </label>
          <label>
            基本料金
            <input
              type="number"
              min="0"
              step="100"
              value={form.basePricePerPerson || ""}
              onChange={(e) => setForm({ ...form, basePricePerPerson: Number(e.target.value) })}
            />
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={form.privateBath}
              onChange={(e) => setForm({ ...form, privateBath: e.target.checked })}
            />{" "}
            専用バス付き
          </label>
          <label>
            メモ
            <textarea value={form.note || ""} onChange={(e) => setForm({ ...form, note: e.target.value })} rows="3" />
          </label>
          <button type="submit">登録</button>
        </form>
      </section>
      <section className="panel wide">
        <h1>部屋一覧</h1>
        <div className="table-scroll">
          <table className="table room-table">
            <colgroup>
              <col className="room-col-no" />
              <col className="room-col-name" />
              <col className="room-col-type" />
              <col className="room-col-capacity" />
              <col className="room-col-price" />
              <col className="room-col-status" />
              <col className="room-col-cleaning" />
              <col className="room-col-update" />
              <col className="room-col-delete" />
            </colgroup>
            <thead>
              <tr>
                <th>番号</th>
                <th>名前</th>
                <th>タイプ</th>
                <th>定員</th>
                <th>料金</th>
                <th>状態</th>
                <th>清掃</th>
                <th>状態更新</th>
                <th>削除</th>
              </tr>
            </thead>
            <tbody>
              {visibleRooms.length ? (
                paddedRooms.map((room, index) =>
                  room ? (
                    <RoomRow key={room.id} room={room} onUpdate={updateStatus} onDelete={deleteRoom} />
                  ) : (
                    <tr key={`room-placeholder-${index}`} className="table-placeholder-row">
                      <td colSpan="9">&nbsp;</td>
                    </tr>
                  )
                )
              ) : (
                <tr>
                  <td colSpan="9" className="empty">
                    部屋データがありません。
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={roomPage} totalPages={roomTotalPages} onPageChange={setRoomPage} />
        <h1 className="subsection-title">削除済み部屋一覧</h1>
        <div className="table-scroll">
          <table className="table deleted-room-table">
            <colgroup>
              <col className="room-col-no" />
              <col className="room-col-name" />
              <col className="room-col-type" />
              <col className="room-col-capacity" />
              <col className="room-col-price" />
              <col className="room-col-updated" />
              <col className="room-col-actions" />
            </colgroup>
            <thead>
              <tr>
                <th>番号</th>
                <th>名前</th>
                <th>タイプ</th>
                <th>定員</th>
                <th>料金</th>
                <th>最終更新</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {visibleDeletedRooms.length ? (
                paddedDeletedRooms.map((room, index) =>
                  room ? (
                    <tr key={room.id}>
                      <td>{room.roomNumber}</td>
                      <td>{room.roomName}</td>
                      <td>{roomTypeLabel(room.roomType)}</td>
                      <td>{room.capacity}</td>
                      <td>{yen(room.basePricePerPerson)}</td>
                      <td>{room.updatedAt}</td>
                      <td>
                        <div className="room-actions">
                          <button type="button" onClick={() => restoreRoom(room)}>
                            復元
                          </button>
                          <button type="button" className="danger" onClick={() => deletePermanently(room)}>
                            完全削除
                          </button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    <tr key={`deleted-room-placeholder-${index}`} className="table-placeholder-row">
                      <td colSpan="7">&nbsp;</td>
                    </tr>
                  )
                )
              ) : (
                <tr>
                  <td colSpan="7" className="empty">
                    削除済み部屋はありません。
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={deletedRoomPage} totalPages={deletedRoomTotalPages} onPageChange={setDeletedRoomPage} />
      </section>
    </main>
  );
}

function RoomRow({ room, onUpdate, onDelete }) {
  const [occupancyStatus, setOccupancyStatus] = useState(room.occupancyStatus);
  const [cleaningStatus, setCleaningStatus] = useState(room.cleaningStatus);
  return (
    <tr>
      <td>{room.roomNumber}</td>
      <td>{room.roomName}</td>
      <td>{roomTypeLabel(room.roomType)}</td>
      <td>{room.capacity}</td>
      <td>{yen(room.basePricePerPerson)}</td>
      <td>
        <Tag status={room.occupancyStatus === "vacant" ? "is-vacant" : "is-booked"}>
          {occupancyLabel(room.occupancyStatus)}
        </Tag>
      </td>
      <td>
        <Tag status={room.cleaningStatus === "cleaned" ? "is-cleaned" : "is-cleaning"}>
          {cleaningLabel(room.cleaningStatus)}
        </Tag>
      </td>
      <td>
        <div className="room-status-form">
          <div className="room-status-fields">
            <select value={occupancyStatus} onChange={(e) => setOccupancyStatus(e.target.value)}>
              <option value="vacant">空室</option>
              <option value="reserved">予約済</option>
              <option value="occupied">滞在中</option>
            </select>
            <select value={cleaningStatus} onChange={(e) => setCleaningStatus(e.target.value)}>
              <option value="cleaned">清掃済</option>
              <option value="needs_cleaning">清掃待ち</option>
            </select>
          </div>
          <button type="button" onClick={() => onUpdate(room, occupancyStatus, cleaningStatus)}>
            更新
          </button>
        </div>
      </td>
      <td>
        <button type="button" className="danger" onClick={() => onDelete(room)}>
          削除
        </button>
      </td>
    </tr>
  );
}

function Prices() {
  const [data, setData] = useState({ rules: [], rooms: [] });
  const [ids, setIds] = useState([]);
  const [rulePage, setRulePage] = useState(1);
  const [notice, setNotice] = useState({});
  const [form, setForm] = useState({ priority: 10, active: true });
  const load = () => loadData(() => api("/api/prices"), setData, setNotice);
  useEffect(() => {
    load();
  }, []);
  useEffect(() => {
    setRulePage((page) => Math.min(page, totalPages(data.rules.length)));
  }, [data.rules.length]);
  const submit = async (event) => {
    event.preventDefault();
    try {
      const res = await api("/api/prices", {
        method: "POST",
        body: JSON.stringify({
          ...form,
          roomId: Number(form.roomId),
          pricePerPerson: Number(form.pricePerPerson),
          priority: Number(form.priority),
          active: true,
        }),
      });
      setNotice({ message: res.message });
      setForm({ priority: 10, active: true });
      load();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };
  const remove = async (id) => {
    runAction(async () => {
      const res = await api(`/api/prices/${id}/delete`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const removeSelected = async () => {
    try {
      const res = await api("/api/prices/delete-selected", { method: "POST", body: JSON.stringify({ ids }) });
      setNotice({ message: res.message });
      setIds([]);
      load();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };
  const visibleRules = slicePage(data.rules, rulePage);
  const paddedRules = data.rules.length ? padPage(visibleRules) : [];
  const ruleTotalPages = totalPages(data.rules.length);
  return (
    <main className="page grid-page">
      <section className="panel">
        <h1>料金ルール登録</h1>
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form" onSubmit={submit}>
          <label>
            部屋
            <select value={form.roomId || ""} onChange={(e) => setForm({ ...form, roomId: e.target.value })}>
              <option value="">選択してください</option>
              {data.rooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.roomNumber} {room.roomName}
                </option>
              ))}
            </select>
          </label>
          <label>
            ルール名
            <input value={form.ruleName || ""} onChange={(e) => setForm({ ...form, ruleName: e.target.value })} />
          </label>
          <label>
            開始日
            <input
              type="date"
              value={form.startDate || ""}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
            />
          </label>
          <label>
            終了日
            <input
              type="date"
              value={form.endDate || ""}
              onChange={(e) => setForm({ ...form, endDate: e.target.value })}
            />
          </label>
          <label>
            一人料金
            <input
              type="number"
              min="0"
              step="100"
              value={form.pricePerPerson || ""}
              onChange={(e) => setForm({ ...form, pricePerPerson: e.target.value })}
            />
          </label>
          <label>
            優先度
            <input
              type="number"
              min="1"
              value={form.priority || ""}
              onChange={(e) => setForm({ ...form, priority: e.target.value })}
            />
          </label>
          <label>
            メモ
            <textarea value={form.note || ""} onChange={(e) => setForm({ ...form, note: e.target.value })} rows="3" />
          </label>
          <button type="submit">登録</button>
        </form>
      </section>
      <section className="panel wide">
        <h1>料金ルール一覧</h1>
        <div className="price-bulk-actions">
          <button className="danger" onClick={removeSelected}>
            選択削除
          </button>
        </div>
        <div className="table-scroll">
          <table className="table price-table">
            <thead>
              <tr>
                <th>選択</th>
                <th>部屋</th>
                <th>ルール</th>
                <th>開始日</th>
                <th>終了日</th>
                <th>料金</th>
                <th>優先度</th>
                <th>状態</th>
                <th>メモ</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {visibleRules.length ? (
                paddedRules.map((rule, index) =>
                  rule ? (
                    <tr key={rule.id}>
                      <td>
                        <input
                          type="checkbox"
                          checked={ids.includes(rule.id)}
                          onChange={(e) =>
                            setIds(e.target.checked ? [...ids, rule.id] : ids.filter((id) => id !== rule.id))
                          }
                        />
                      </td>
                      <td>
                        {rule.roomNumber} {rule.roomName}
                      </td>
                      <td>{rule.ruleName}</td>
                      <td>{rule.startDate}</td>
                      <td>{rule.endDate}</td>
                      <td>{rule.pricePerPerson}</td>
                      <td>{rule.priority}</td>
                      <td>
                        <Tag status={rule.active ? "is-active" : "is-muted"}>{rule.active ? "有効" : "無効"}</Tag>
                      </td>
                      <td>{rule.note}</td>
                      <td>
                        <button type="button" className="danger" onClick={() => remove(rule.id)}>
                          削除
                        </button>
                      </td>
                    </tr>
                  ) : (
                    <tr key={`price-placeholder-${index}`} className="table-placeholder-row">
                      <td colSpan="10">&nbsp;</td>
                    </tr>
                  )
                )
              ) : (
                <tr>
                  <td colSpan="10" className="empty">
                    料金ルールがありません。
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={rulePage} totalPages={ruleTotalPages} onPageChange={setRulePage} />
      </section>
    </main>
  );
}

function Reservations() {
  const [data, setData] = useState({
    reservations: { items: [] },
    cancelledReservations: { items: [] },
    checkedOutReservations: { items: [] },
    rooms: [],
  });
  const [notice, setNotice] = useState({});
  const [form, setForm] = useState({ guestCount: 1, reservationForm: "公式", paymentStatus: "unpaid" });
  const load = () => loadData(() => api("/api/reservations"), setData, setNotice);
  useEffect(() => {
    load();
  }, []);
  const companions = useMemo(() => Math.max(0, Number(form.guestCount || 1) - 1), [form.guestCount]);
  const submit = async (event) => {
    event.preventDefault();
    const companionNames = Array.from({ length: companions }, (_, index) => form[`companionName${index}`] || "");
    const companionKanas = Array.from({ length: companions }, (_, index) => form[`companionKana${index}`] || "");
    const companionGenders = Array.from({ length: companions }, (_, index) => form[`companionGender${index}`] || "");
    const companionAges = Array.from(
      { length: companions },
      (_, index) => Number(form[`companionAge${index}`] || 0) || null
    );
    const companionPhones = Array.from({ length: companions }, (_, index) => form[`companionPhone${index}`] || "");
    const reservation = {
      roomId: Number(form.roomId),
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      guestName: form.guestName,
      guestKana: form.guestKana,
      guestGender: form.guestGender,
      guestAge: Number(form.guestAge || 0) || null,
      guestPhone: form.noPhoneInfo ? null : form.guestPhone,
      guestEmail: form.noEmailInfo ? null : form.guestEmail,
      guestCount: Number(form.guestCount),
      reservationForm: form.reservationForm,
      paymentStatus: form.paymentStatus,
      note: form.note,
    };
    try {
      const res = await api("/api/reservations", {
        method: "POST",
        body: JSON.stringify({
          reservation,
          noPhoneInfo: Boolean(form.noPhoneInfo),
          noEmailInfo: Boolean(form.noEmailInfo),
          companionNames,
          companionKanas,
          companionGenders,
          companionAges,
          companionPhones,
        }),
      });
      setNotice({ message: res.message });
      setForm({ guestCount: 1, reservationForm: "公式", paymentStatus: "unpaid" });
      load();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };
  const payment = async (item, paymentStatus) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/payment`, {
        method: "POST",
        body: JSON.stringify({ paymentStatus }),
      });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const cancel = async (item) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/cancel`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const deleteCancelled = async (item) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/delete`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const deleteCheckedOut = async (item) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/delete-checked-out`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const cleaning = async (item, cleaningStatus) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/cleaning`, {
        method: "POST",
        body: JSON.stringify({ cleaningStatus }),
      });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  return (
    <main className="page grid-page">
      <section className="panel">
        <h1>予約登録</h1>
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form" onSubmit={submit}>
          <label>
            部屋
            <select value={form.roomId || ""} onChange={(e) => setForm({ ...form, roomId: e.target.value })}>
              <option value="">空室・清掃済の部屋を選択してください</option>
              {data.rooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.roomNumber} {room.roomName} / 定員{room.capacity}名
                </option>
              ))}
            </select>
          </label>
          <div className="form-grid form-grid-2">
            <label>
              チェックイン
              <input
                type="date"
                min={data.today}
                value={form.checkInDate || ""}
                onChange={(e) => setForm({ ...form, checkInDate: e.target.value })}
              />
            </label>
            <label>
              チェックアウト
              <input
                type="date"
                min={data.today}
                value={form.checkOutDate || ""}
                onChange={(e) => setForm({ ...form, checkOutDate: e.target.value })}
              />
            </label>
          </div>
          <label>
            宿泊者名
            <input value={form.guestName || ""} onChange={(e) => setForm({ ...form, guestName: e.target.value })} />
          </label>
          <div className="form-grid form-grid-2">
            <label>
              フリガナ
              <input value={form.guestKana || ""} onChange={(e) => setForm({ ...form, guestKana: e.target.value })} />
            </label>
            <label>
              性別
              <select
                value={form.guestGender || ""}
                onChange={(e) => setForm({ ...form, guestGender: e.target.value })}
              >
                <option value="">選択してください</option>
                <option>男性</option>
                <option>女性</option>
                <option>その他</option>
                <option>未回答</option>
              </select>
            </label>
            <label>
              年齢
              <input
                type="number"
                min="0"
                max="130"
                value={form.guestAge || ""}
                onChange={(e) => setForm({ ...form, guestAge: e.target.value })}
              />
            </label>
            <label>
              人数
              <input
                type="number"
                min="1"
                max="10"
                value={form.guestCount || 1}
                onChange={(e) => setForm({ ...form, guestCount: e.target.value })}
              />
            </label>
          </div>
          <div className="form-grid form-grid-2">
            <label>
              電話
              <input
                disabled={form.noPhoneInfo}
                value={form.guestPhone || ""}
                onChange={(e) => setForm({ ...form, guestPhone: e.target.value })}
                placeholder="090-0000-0000"
              />
            </label>
            <label className="check contact-toggle">
              <input
                type="checkbox"
                checked={Boolean(form.noPhoneInfo)}
                onChange={(e) => setForm({ ...form, noPhoneInfo: e.target.checked, guestPhone: "" })}
              />{" "}
              電話なし
            </label>
            <label>
              メール
              <input
                disabled={form.noEmailInfo}
                value={form.guestEmail || ""}
                onChange={(e) => setForm({ ...form, guestEmail: e.target.value })}
              />
            </label>
            <label className="check contact-toggle">
              <input
                type="checkbox"
                checked={Boolean(form.noEmailInfo)}
                onChange={(e) => setForm({ ...form, noEmailInfo: e.target.checked, guestEmail: "" })}
              />{" "}
              メールなし
            </label>
          </div>
          <div className="form-grid form-grid-2">
            <label>
              予約形式
              <select
                value={form.reservationForm}
                onChange={(e) => setForm({ ...form, reservationForm: e.target.value })}
              >
                <option>公式</option>
                <option>電話</option>
                <option>メール</option>
                <option>予約サイト</option>
                <option>現地</option>
              </select>
            </label>
            <label>
              支払い
              <select value={form.paymentStatus} onChange={(e) => setForm({ ...form, paymentStatus: e.target.value })}>
                <option value="unpaid">未払い</option>
                <option value="paid">支払済</option>
              </select>
            </label>
          </div>
          {companions > 0 && (
            <section className="companion-panel">
              <div className="section-eyebrow">同行者情報</div>
              {Array.from({ length: companions }, (_, index) => (
                <div className="companion-card" key={index}>
                  <div className="companion-card-head">
                    <span className="companion-card-index">同行者{index + 1}</span>
                    <span className="companion-card-badge">必須</span>
                  </div>
                  <label>
                    氏名
                    <input
                      value={form[`companionName${index}`] || ""}
                      onChange={(e) => setForm({ ...form, [`companionName${index}`]: e.target.value })}
                    />
                  </label>
                  <div className="form-grid form-grid-2">
                    <label>
                      フリガナ
                      <input
                        value={form[`companionKana${index}`] || ""}
                        onChange={(e) => setForm({ ...form, [`companionKana${index}`]: e.target.value })}
                      />
                    </label>
                    <label>
                      性別
                      <select
                        value={form[`companionGender${index}`] || ""}
                        onChange={(e) => setForm({ ...form, [`companionGender${index}`]: e.target.value })}
                      >
                        <option value="">選択してください</option>
                        <option>男性</option>
                        <option>女性</option>
                        <option>その他</option>
                        <option>未回答</option>
                      </select>
                    </label>
                    <label>
                      年齢
                      <input
                        type="number"
                        min="0"
                        max="130"
                        value={form[`companionAge${index}`] || ""}
                        onChange={(e) => setForm({ ...form, [`companionAge${index}`]: e.target.value })}
                      />
                    </label>
                    <label>
                      電話
                      <input
                        value={form[`companionPhone${index}`] || ""}
                        onChange={(e) => setForm({ ...form, [`companionPhone${index}`]: e.target.value })}
                      />
                    </label>
                  </div>
                </div>
              ))}
            </section>
          )}
          <label>
            メモ
            <textarea value={form.note || ""} onChange={(e) => setForm({ ...form, note: e.target.value })} rows="3" />
          </label>
          <button className="form-submit" type="submit">
            予約登録
          </button>
        </form>
      </section>
      <section className="panel wide">
        <h1>予約一覧</h1>
        <ReservationTable reservations={data.reservations.items} onPayment={payment} onCancel={cancel} />
        <h1 className="subsection-title">取消予約一覧</h1>
        <ReservationTable
          reservations={data.cancelledReservations.items}
          readonly
          onDelete={deleteCancelled}
          showDelete
        />
        <h1 className="subsection-title">チェックアウト一覧</h1>
        <CheckoutTable
          reservations={data.checkedOutReservations.items}
          onCleaning={cleaning}
          onDelete={deleteCheckedOut}
        />
      </section>
    </main>
  );
}

function ReservationTable({
  reservations = [],
  onPayment,
  onCancel,
  onDelete,
  readonly = false,
  compact = false,
  showDelete = false,
}) {
  // Keep a shared column definition so active, cancelled, and dashboard reservation tables stay aligned.
  return (
    <div className="table-scroll">
      <table className={`table reservation-table ${compact ? "dashboard-table" : "reservation-table-full"}`}>
        <ReservationColGroup includeActions={!readonly && !compact} includeDelete={showDelete} />
        <thead>
          <tr>
            <th>予約番号</th>
            <th>部屋</th>
            <th>予約者</th>
            <th>人数</th>
            <th>電話</th>
            <th>メール</th>
            <th>予約形式</th>
            <th>同行者</th>
            <th>日程</th>
            <th>金額</th>
            <th>予約状態</th>
            <th>支払い</th>
            {!readonly && !compact && <th>操作</th>}
            {showDelete && <th>削除</th>}
          </tr>
        </thead>
        <tbody>
          {reservations.length ? (
            reservations.map((item) => (
              <ReservationRow
                key={item.id}
                item={item}
                onPayment={onPayment}
                onCancel={onCancel}
                onDelete={onDelete}
                readonly={readonly || compact}
                compact={compact}
                showDelete={showDelete}
              />
            ))
          ) : (
            <tr>
              <td colSpan={compact ? 12 : 13} className="empty">
                予約データがありません。
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function ReservationColGroup({ includeActions, includeDelete }) {
  return (
    <colgroup>
      <col className="col-no" />
      <col className="col-room" />
      <col className="col-guest" />
      <col className="col-count" />
      <col className="col-phone" />
      <col className="col-email" />
      <col className="col-form" />
      <col className="col-companion" />
      <col className="col-date" />
      <col className="col-amount" />
      <col className="col-reservation-status" />
      <col className="col-payment-status" />
      {includeActions && <col className="col-actions" />}
      {includeDelete && <col className="col-delete-actions" />}
    </colgroup>
  );
}

function ReservationRow({ item, onPayment, onCancel, onDelete, readonly, compact, showDelete }) {
  const [paymentStatus, setPaymentStatus] = useState(item.paymentStatus);
  const allowActions = !readonly && !compact;
  return (
    <tr>
      <td>{item.reservationNo}</td>
      <td>
        {item.roomNumber} {item.roomName}
      </td>
      <td>
        <PersonSummary
          name={item.guestName}
          metaParts={[
            text(item.guestKana, "フリガナ未入力"),
            text(item.guestGender, "性別未入力"),
            item.guestAge ? `${item.guestAge}歳` : "年齢未入力",
          ]}
        />
      </td>
      <td>{item.guestCount}名</td>
      <td>{text(item.guestPhone, "電話未入力")}</td>
      <td>{text(item.guestEmail, "メール未入力")}</td>
      <td>{text(item.reservationForm, "公式")}</td>
      <td className="companion-summary">
        <CompanionSummary value={item.companionSummary} />
      </td>
      <td>
        {item.checkInDate} - {item.checkOutDate}
      </td>
      <td>{yen(item.totalAmount)}</td>
      <td>
        <Tag
          status={
            item.reservationStatus === "checked_out"
              ? "is-checkout"
              : item.reservationStatus === "cancelled"
                ? "is-cancelled"
                : "is-booked"
          }
        >
          {item.reservationStatusLabel}
        </Tag>
      </td>
      <td>
        <Tag status={item.paymentStatus === "paid" ? "is-paid" : "is-unpaid"}>{item.paymentStatusLabel}</Tag>
      </td>
      {allowActions && (
        <td className="actions">
          <div className="reservation-actions">
            <div className="inline-form">
              <select value={paymentStatus} onChange={(e) => setPaymentStatus(e.target.value)}>
                <option value="unpaid">未払い</option>
                <option value="paid">支払済</option>
              </select>
              <button type="button" onClick={() => onPayment(item, paymentStatus)}>
                更新
              </button>
            </div>
            <button type="button" className="danger" onClick={() => onCancel(item)}>
              取消
            </button>
          </div>
        </td>
      )}
      {showDelete && (
        <td className="actions">
          <button type="button" className="danger" onClick={() => onDelete(item)}>
            削除
          </button>
        </td>
      )}
    </tr>
  );
}

function CompanionSummary({ value }) {
  // The backend returns companion details as newline-separated text; render each line separately for readability.
  const lines = String(value || "")
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length === 0) {
    return <span className="companion-empty">同行者なし</span>;
  }
  return (
    <ul className="companion-list">
      {lines.map((line, index) => {
        const match = line.match(/^(.*)（(.*)）$/);
        const name = match ? match[1] : line;
        const meta = match ? match[2] : "";
        return (
          <li key={`${line}-${index}`}>
            <div className="companion-name">{name}</div>
            {meta ? (
              <div className="companion-meta">
                {meta.split("・").map((part) => (
                  <span key={`${part}-${index}`} className="companion-meta-item">
                    {part}
                  </span>
                ))}
              </div>
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}

function PersonSummary({ name, metaParts, emptyLabel = "情報なし" }) {
  if (!name) {
    return <span className="companion-empty">{emptyLabel}</span>;
  }
  return (
    <div className="person-summary">
      <div className="person-name">{name}</div>
      {metaParts?.length ? (
        <div className="person-meta">
          {metaParts.map((part, index) => (
            <span key={`${part}-${index}`} className="person-meta-item">
              {part}
            </span>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function CheckoutTable({ reservations = [], onCleaning, onDelete }) {
  // Checkout rows use fewer columns than reservation rows, so they need their own width map.
  return (
    <div className="table-scroll">
      <table className="table checkout-table">
        <colgroup>
          <col className="col-no" />
          <col className="col-room" />
          <col className="col-guest" />
          <col className="col-count" />
          <col className="col-phone" />
          <col className="col-email" />
          <col className="col-companion" />
          <col className="col-reservation-status" />
          <col className="col-cleaning-status" />
          <col className="col-checkout-actions" />
        </colgroup>
        <thead>
          <tr>
            <th>予約番号</th>
            <th>部屋番号</th>
            <th>予約者</th>
            <th>人数</th>
            <th>電話</th>
            <th>メール</th>
            <th>同行者</th>
            <th>予約状態</th>
            <th>清掃</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {reservations.length ? (
            reservations.map((item) => (
              <CheckoutRow key={item.id} item={item} onCleaning={onCleaning} onDelete={onDelete} />
            ))
          ) : (
            <tr>
              <td colSpan="10" className="empty">
                チェックアウトデータがありません。
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function CheckoutRow({ item, onCleaning, onDelete }) {
  const [cleaningStatus, setCleaningStatus] = useState(item.roomCleaningStatus || "needs_cleaning");
  return (
    <tr>
      <td>{item.reservationNo}</td>
      <td>{item.roomNumber}</td>
      <td>
        <PersonSummary
          name={item.guestName}
          metaParts={[
            text(item.guestKana, "フリガナ未入力"),
            text(item.guestGender, "性別未入力"),
            item.guestAge ? `${item.guestAge}歳` : "年齢未入力",
          ]}
        />
      </td>
      <td>{item.guestCount}名</td>
      <td>{text(item.guestPhone, "電話未入力")}</td>
      <td>{text(item.guestEmail, "メール未入力")}</td>
      <td className="companion-summary">
        <CompanionSummary value={item.companionSummary} />
      </td>
      <td>
        <Tag status="is-checkout">{item.reservationStatusLabel}</Tag>
      </td>
      <td>
        <Tag status={item.roomCleaningStatus === "cleaned" ? "is-cleaned" : "is-cleaning"}>
          {cleaningLabel(item.roomCleaningStatus)}
        </Tag>
      </td>
      <td>
        <div className="inline-form">
          <select value={cleaningStatus} onChange={(e) => setCleaningStatus(e.target.value)}>
            <option value="needs_cleaning">清掃待ち</option>
            <option value="cleaned">清掃済</option>
          </select>
        </div>
        <div className="checkout-actions">
          <button type="button" onClick={() => onCleaning(item, cleaningStatus)}>
            更新
          </button>
          <button type="button" className="danger" onClick={() => onDelete?.(item)}>
            削除
          </button>
        </div>
      </td>
    </tr>
  );
}

function App() {
  const route = currentRoute();
  return (
    <>
      <Nav route={route} />
      {route === "/rooms" ? (
        <Rooms />
      ) : route === "/reservations" ? (
        <Reservations />
      ) : route === "/prices" ? (
        <Prices />
      ) : (
        <Dashboard />
      )}
    </>
  );
}

createRoot(document.getElementById("root")).render(<App />);
