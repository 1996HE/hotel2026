import React, { useCallback, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import { I18nProvider, LanguageToggle, TranslationBoundary, useI18n } from "./i18n.jsx";

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
    const error = new Error(data.error || "処理に失敗しました。");
    error.status = response.status;
    throw error;
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

function Nav({ route, onLogout }) {
  const items = [
    ["/dashboard", "ホーム", "dashboard"],
    ["/rooms", "客室管理", "rooms"],
    ["/reservations", "予約管理", "calendar"],
    ["/prices", "料金設定", "price"],
    ["/customers", "顧客管理", "customers"],
    ["/finance", "営業・会計", "finance"],
  ];
  return (
    <aside className="topbar" aria-label="管理画面サイドバー">
      <a className="brand" href={withContext("/dashboard")} aria-label="白馬樹海 ホーム">
        <span className="brand-mark" aria-hidden="true">
          樹
        </span>
        <span className="brand-copy">
          <strong>白馬樹海</strong>
        </span>
      </a>
      <nav className="nav" aria-label="主要メニュー">
        <span className="nav-caption">MAIN MENU</span>
        {items.map(([href, label, icon]) => {
          const active = route === href || (route === "/" && href === "/dashboard");
          return (
            <a
              key={href}
              href={withContext(href)}
              aria-label={label}
              className={active ? "active" : ""}
              aria-current={active ? "page" : undefined}
            >
              <NavIcon name={icon} />
              <span>{label}</span>
            </a>
          );
        })}
      </nav>
      <div className="mobile-utilities">
        <LanguageToggle compact />
        <button type="button" aria-label="ログアウト" onClick={onLogout}>
          ↪
        </button>
      </div>
      <div className="sidebar-footer">
        <LanguageToggle compact />
        <span className="system-status">
          <i aria-hidden="true" />
          営業中
        </span>
        <button type="button" className="sidebar-logout" onClick={onLogout}>
          ログアウト
        </button>
        <small>Hakuba Jukai Ryokan</small>
      </div>
    </aside>
  );
}

function NavIcon({ name }) {
  const paths = {
    dashboard: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </>
    ),
    rooms: (
      <>
        <path d="M4 21V5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v16" />
        <path d="M2 21h20" />
        <path d="M8 7h8v14H8z" />
        <path d="M13 14h.01" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M16 3v4M8 3v4M3 10h18" />
        <path d="m8 15 2 2 5-5" />
      </>
    ),
    price: (
      <>
        <path d="M20.6 13.6 11 23.2 1.8 14 11.4 4.4H20v8.6Z" transform="scale(.88) translate(1 -1)" />
        <circle cx="16" cy="7" r="1.2" />
        <path d="M8 11h7M8 14h7M11.5 11v7" />
      </>
    ),
    customers: (
      <>
        <circle cx="9" cy="8" r="4" />
        <path d="M3 21v-2a6 6 0 0 1 12 0v2M17 11h4M19 9v4" />
      </>
    ),
    finance: (
      <>
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M3 10h18M7 15h3" />
      </>
    ),
  };
  return (
    <svg
      className="nav-icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name]}
    </svg>
  );
}

function PageHeader({ eyebrow, title, description, action }) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="page-description">{description}</p>
      </div>
      {action ? <div className="page-header-action">{action}</div> : null}
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
    <div
      className={`notice notice-toast ${error ? "error" : "success"}`}
      role={error ? "alert" : "status"}
      aria-live={error ? "assertive" : "polite"}
    >
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

function AuthenticationScreen({ authStatus, onAuthenticated }) {
  const { t } = useI18n();
  const [form, setForm] = useState({ username: "", password: "" });
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  const setup = Boolean(authStatus.setupRequired);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setNotice({});
    try {
      if (setup) {
        await api("/api/auth/setup", { method: "POST", body: JSON.stringify(form) });
      }
      await api("/api/auth/login", { method: "POST", body: JSON.stringify(form) });
      onAuthenticated(form.username);
    } catch (error) {
      setNotice({ error: error.message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page" id="main-content">
      <section className="auth-card">
        <div className="auth-brand" aria-hidden="true">
          樹
        </div>
        <p className="eyebrow">HAKUBA JUKAI</p>
        <h1>{setup ? t("初期設定", "初始设置") : t("管理者ログイン", "管理员登录")}</h1>
        <p className="page-description">
          {setup
            ? t("最初に、この端末で使用する管理者を登録してください。", "请先登记本机使用的管理员。")
            : t("登録済みの管理者情報を入力してください。", "请输入已登记的管理员信息。")}
        </p>
        <LanguageToggle />
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form auth-form" onSubmit={submit}>
          <label>
            {t("ユーザー名", "用户名")}
            <input
              required
              minLength="3"
              maxLength="64"
              autoComplete="username"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
            />
          </label>
          <label>
            {t("パスワード", "密码")}
            <input
              required
              minLength="10"
              type="password"
              autoComplete={setup ? "new-password" : "current-password"}
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
            />
          </label>
          <button className="form-submit" type="submit" disabled={busy}>
            {busy
              ? t("処理中...", "处理中...")
              : setup
                ? t("管理者を登録して開始", "登记管理员并开始")
                : t("ログイン", "登录")}
          </button>
        </form>
      </section>
    </main>
  );
}

function Dashboard() {
  const [data, setData] = useState(null);
  const [notice, setNotice] = useState({});
  useEffect(() => {
    loadData(() => api("/api/dashboard"), setData, setNotice);
  }, []);
  if (!data)
    return (
      <main className="page" id="main-content">
        <Notice {...notice} onClose={() => setNotice({})} />
        <section className="panel" role="status" aria-live="polite">
          読み込み中...
        </section>
      </main>
    );
  return (
    <main className="page" id="main-content">
      <PageHeader
        eyebrow="本日の帳場"
        title="宿泊状況"
        description="予約、空室、支払い状況をひと目で確認できます。"
        action={
          <a className="button header-button" href={withContext("/reservations")}>
            <span aria-hidden="true">＋</span> 新規予約
          </a>
        }
      />
      <section className="metrics">
        <Metric label="全客室" value={data.roomCount} icon="rooms" tone="indigo" />
        <Metric label="販売可能" value={data.vacantCount} icon="dashboard" tone="green" />
        <Metric label="有効予約" value={data.bookedCount} icon="calendar" tone="gold" />
      </section>
      <section className="panel dashboard-panel">
        <div className="section-title pager-title">
          <div>
            <span className="section-kicker">RESERVATIONS</span>
            <h2>直近のご予約</h2>
          </div>
          <a className="text-link" href={withContext("/reservations")}>
            すべて見る <span aria-hidden="true">→</span>
          </a>
        </div>
        <ReservationTable reservations={data.recentReservations.items} compact />
      </section>
    </main>
  );
}

function Metric({ label, value, icon, tone, unit = "室", detail = "現在" }) {
  return (
    <article className={`metric metric-${tone || "green"}`}>
      <span className="metric-icon">
        <NavIcon name={icon} />
      </span>
      <div className="metric-copy">
        <span>{label}</span>
        <strong>
          {value}
          {unit ? <small>{unit}</small> : null}
        </strong>
      </div>
      <span className="metric-detail">{detail}</span>
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
    <main className="page grid-page" id="main-content">
      <PageHeader eyebrow="客室台帳" title="客室管理" description="客室の基本情報、販売状況、清掃状態を管理します。" />
      <section className="panel form-panel">
        <span className="section-kicker">NEW ROOM</span>
        <h2>客室を登録</h2>
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
          <button className="form-submit" type="submit">
            客室を登録
          </button>
        </form>
      </section>
      <section className="panel wide data-panel">
        <div className="section-title">
          <div>
            <span className="section-kicker">ROOMS</span>
            <h2>客室一覧</h2>
          </div>
          <span className="record-count">{data.rooms.length} 室</span>
        </div>
        <div className="table-scroll">
          <table className="table room-table">
            <caption className="visually-hidden">有効な客室一覧</caption>
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
        <h2 className="subsection-title">削除済み客室</h2>
        <div className="table-scroll">
          <table className="table deleted-room-table">
            <caption className="visually-hidden">削除済み客室一覧</caption>
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
            <select
              aria-label={`${room.roomNumber}号室の宿泊状態`}
              value={occupancyStatus}
              onChange={(e) => setOccupancyStatus(e.target.value)}
            >
              <option value="vacant">空室</option>
              <option value="reserved">予約済</option>
              <option value="occupied">滞在中</option>
            </select>
            <select
              aria-label={`${room.roomNumber}号室の清掃状態`}
              value={cleaningStatus}
              onChange={(e) => setCleaningStatus(e.target.value)}
            >
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
    <main className="page grid-page" id="main-content">
      <PageHeader eyebrow="季節の料金帳" title="料金設定" description="客室ごとの期間料金と適用優先度を管理します。" />
      <section className="panel form-panel">
        <span className="section-kicker">NEW RATE</span>
        <h2>料金ルールを登録</h2>
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
          <button className="form-submit" type="submit">
            料金を登録
          </button>
        </form>
      </section>
      <section className="panel wide data-panel">
        <div className="section-title">
          <div>
            <span className="section-kicker">RATE PLANS</span>
            <h2>料金ルール一覧</h2>
          </div>
          <div className="price-bulk-actions">
            <span className="record-count">{data.rules.length} 件</span>
            <button className="danger button-quiet" onClick={removeSelected} disabled={ids.length === 0}>
              選択削除 {ids.length ? `(${ids.length})` : ""}
            </button>
          </div>
        </div>
        <div className="table-scroll">
          <table className="table price-table">
            <caption className="visually-hidden">料金ルール一覧</caption>
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
                          aria-label={`${rule.roomNumber} ${rule.ruleName}を選択`}
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

function Customers() {
  const { t } = useI18n();
  const emptyForm = { name: "", phone: "", email: "" };
  const [customers, setCustomers] = useState([]);
  const [query, setQuery] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [selected, setSelected] = useState(null);
  const [stays, setStays] = useState([]);
  const [notice, setNotice] = useState({});

  const load = useCallback(async (search) => {
    try {
      const params = new URLSearchParams();
      if (search.trim()) params.set("query", search.trim());
      setCustomers(await api(`/api/customers?${params.toString()}`));
    } catch (error) {
      setNotice({ error: error.message });
    }
  }, []);

  useEffect(() => {
    load("");
  }, [load]);

  const selectCustomer = async (customer) => {
    setSelected(customer);
    setForm({
      name: customer.name || "",
      phone: customer.phone || "",
      email: customer.email || "",
    });
    try {
      setStays(await api(`/api/customers/${customer.id}/stays`));
    } catch (error) {
      setNotice({ error: error.message });
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      const saved = await api(selected ? `/api/customers/${selected.id}` : "/api/customers", {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      setNotice({
        message: selected
          ? t("顧客情報を更新しました。", "客户信息已更新。")
          : t("顧客を登録しました。", "客户已登记。"),
      });
      setSelected(saved);
      setForm({ name: saved.name || "", phone: saved.phone || "", email: saved.email || "" });
      await load(query);
    } catch (error) {
      setNotice({ error: error.message });
    }
  };

  const startNew = () => {
    setSelected(null);
    setForm(emptyForm);
    setStays([]);
  };

  return (
    <main className="page grid-page customers-page" id="main-content">
      <PageHeader
        eyebrow="GUEST DIRECTORY"
        title={t("顧客管理", "客户管理")}
        description={t(
          "氏名と連絡先を管理し、過去の宿泊記録を確認できます。",
          "管理姓名和联系方式，并可查看历史住宿记录。"
        )}
        action={<button onClick={startNew}>{t("新規顧客", "新建客户")}</button>}
      />
      <section className="panel form-panel">
        <span className="section-kicker">CUSTOMER</span>
        <h2>{selected ? t("顧客を編集", "编辑客户") : t("顧客を登録", "登记客户")}</h2>
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form" onSubmit={submit}>
          <label>
            {t("顧客名", "客户姓名")}
            <input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>
          <label>
            {t("電話", "电话")}
            <input
              type="tel"
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
            />
          </label>
          <label>
            {t("メール", "邮箱")}
            <input
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
            />
          </label>
          <button className="form-submit" type="submit">
            {selected ? t("保存", "保存") : t("登録", "登记")}
          </button>
        </form>
      </section>
      <section className="panel wide data-panel">
        <div className="section-title customer-search-title">
          <div>
            <span className="section-kicker">CUSTOMERS</span>
            <h2>{t("顧客一覧", "客户列表")}</h2>
          </div>
          <form
            className="search-form"
            onSubmit={(event) => {
              event.preventDefault();
              load(query);
            }}
          >
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t("氏名・電話・メール", "姓名、电话、邮箱")}
            />
            <button type="submit">{t("検索", "搜索")}</button>
          </form>
        </div>
        <div className="table-scroll">
          <table className="table customer-table">
            <thead>
              <tr>
                <th>{t("顧客番号", "客户编号")}</th>
                <th>{t("氏名", "姓名")}</th>
                <th>{t("電話", "电话")}</th>
                <th>{t("メール", "邮箱")}</th>
                <th>{t("宿泊回数", "住宿次数")}</th>
                <th>{t("操作", "操作")}</th>
              </tr>
            </thead>
            <tbody>
              {customers.length ? (
                customers.map((customer) => (
                  <tr key={customer.id} className={selected?.id === customer.id ? "selected-row" : ""}>
                    <td>{customer.customerNo}</td>
                    <td>{customer.name}</td>
                    <td>{text(customer.phone, "—")}</td>
                    <td>{text(customer.email, "—")}</td>
                    <td>{customer.stayCount || 0}</td>
                    <td>
                      <button type="button" onClick={() => selectCustomer(customer)}>
                        {t("詳細", "详情")}
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" className="empty">
                    {t("顧客データがありません。", "没有客户数据。")}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {selected ? (
          <section className="stay-history">
            <h2>
              {t("宿泊履歴", "住宿记录")} · {selected.name}
            </h2>
            <div className="table-scroll">
              <table className="table compact-table">
                <thead>
                  <tr>
                    <th>{t("予約番号", "订单编号")}</th>
                    <th>{t("客室", "客房")}</th>
                    <th>{t("日程", "日期")}</th>
                    <th>{t("人数", "人数")}</th>
                    <th>{t("金額", "金额")}</th>
                    <th>{t("状態", "状态")}</th>
                  </tr>
                </thead>
                <tbody>
                  {stays.length ? (
                    stays.map((stay) => (
                      <tr key={stay.id}>
                        <td>{stay.reservationNo}</td>
                        <td>
                          {stay.roomNumber} {stay.roomName}
                        </td>
                        <td>
                          {stay.checkInDate} → {stay.checkOutDate}
                        </td>
                        <td>{stay.guestCount}</td>
                        <td>{yen(stay.totalAmount)}</td>
                        <td>{stay.reservationStatusLabel}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="6" className="empty">
                        {t("宿泊履歴がありません。", "没有住宿记录。")}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}
      </section>
    </main>
  );
}

function Finance() {
  const { language, t } = useI18n();
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
  const [range, setRange] = useState({ startDate: monthStart, endDate: today });
  const [summary, setSummary] = useState({ receivable: 0, received: 0, refunded: 0, netRevenue: 0, rows: [] });
  const [selected, setSelected] = useState(null);
  const [finance, setFinance] = useState(null);
  const [payment, setPayment] = useState({ amount: "", method: "cash" });
  const [refund, setRefund] = useState({ amount: "" });
  const [backups, setBackups] = useState({ directory: "", history: [] });
  const [notice, setNotice] = useState({});

  const loadSummary = useCallback(async () => {
    try {
      const params = new URLSearchParams(range);
      setSummary(await api(`/api/finance?${params.toString()}`));
    } catch (error) {
      setNotice({ error: error.message });
    }
  }, [range]);

  const loadBackups = useCallback(async () => {
    try {
      setBackups(await api("/api/backups"));
    } catch (error) {
      setNotice({ error: error.message });
    }
  }, []);

  useEffect(() => {
    loadSummary();
    loadBackups();
  }, [loadBackups, loadSummary]);

  const choose = async (row) => {
    setSelected(row);
    try {
      const current = await api(`/api/finance/${row.reservationId || row.id}`);
      setFinance(current);
      setPayment({
        amount: String(current.receivedAmount || row.totalAmount || 0),
        method: current.paymentMethod || "cash",
      });
      setRefund({ amount: String(current.refundAmount || 0) });
    } catch (error) {
      // Report rows contain reservationId in the updated API; this guard keeps old cached responses understandable.
      setNotice({ error: error.message });
    }
  };

  const submitPayment = async (event) => {
    event.preventDefault();
    try {
      const saved = await api(`/api/finance/${selected.reservationId}/payment`, {
        method: "POST",
        body: JSON.stringify({ amount: Number(payment.amount), method: payment.method }),
      });
      setFinance(saved);
      setNotice({ message: t("入金を保存しました。", "收款已保存。") });
      await loadSummary();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };

  const submitRefund = async (event) => {
    event.preventDefault();
    try {
      const saved = await api(`/api/finance/${selected.reservationId}/refund`, {
        method: "POST",
        body: JSON.stringify({ amount: Number(refund.amount) }),
      });
      setFinance(saved);
      setNotice({ message: t("返金を保存しました。", "退款已保存。") });
      await loadSummary();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };

  const runBackup = async () => {
    try {
      const result = await api("/api/backups", { method: "POST" });
      setNotice(
        result.status === "success"
          ? { message: t("バックアップが完了しました。", "备份已完成。") }
          : { error: result.message }
      );
      await loadBackups();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };

  const exportUrl = withContext(
    `/api/reports/business.xlsx?${new URLSearchParams({ ...range, lang: language }).toString()}`
  );
  return (
    <main className="page finance-page" id="main-content">
      <PageHeader
        eyebrow="BUSINESS & FINANCE"
        title={t("営業・会計", "营业与收款")}
        description={t(
          "予約ごとの応収・実収・返金と月次営業状況を確認します。",
          "查看每笔订单的应收、实收、退款和月度营业情况。"
        )}
      />
      <Notice {...notice} onClose={() => setNotice({})} />
      <section className="panel finance-toolbar">
        <div className="date-range-form">
          <label>
            {t("開始日", "开始日")}
            <input
              type="date"
              value={range.startDate}
              onChange={(event) => setRange({ ...range, startDate: event.target.value })}
            />
          </label>
          <label>
            {t("終了日", "结束日")}
            <input
              type="date"
              value={range.endDate}
              onChange={(event) => setRange({ ...range, endDate: event.target.value })}
            />
          </label>
          <button type="button" onClick={loadSummary}>
            {t("集計", "统计")}
          </button>
          <a className="button" href={exportUrl}>
            {t("Excel出力", "导出Excel")}
          </a>
        </div>
      </section>
      <section className="metrics finance-metrics">
        <Metric
          label={t("売上予定", "应收金额")}
          value={yen(summary.receivable)}
          icon="calendar"
          tone="indigo"
          unit=""
          detail={t("期間内", "期间内")}
        />
        <Metric
          label={t("入金額", "实收金额")}
          value={yen(summary.received)}
          icon="dashboard"
          tone="green"
          unit=""
          detail={t("期間内", "期间内")}
        />
        <Metric
          label={t("返金額", "退款金额")}
          value={yen(summary.refunded)}
          icon="price"
          tone="gold"
          unit=""
          detail={t("期間内", "期间内")}
        />
        <Metric
          label={t("実収入", "净收入")}
          value={yen(summary.netRevenue)}
          icon="dashboard"
          tone="green"
          unit=""
          detail={t("期間内", "期间内")}
        />
      </section>
      <div className="finance-layout">
        <section className="panel data-panel finance-list-panel">
          <div className="section-title">
            <div>
              <span className="section-kicker">PAYMENTS</span>
              <h2>{t("入返金管理", "收退款管理")}</h2>
            </div>
            <span className="record-count">
              {summary.rows.length} {t("件", "条")}
            </span>
          </div>
          <div className="table-scroll">
            <table className="table finance-table">
              <thead>
                <tr>
                  <th>{t("予約番号", "订单编号")}</th>
                  <th>{t("顧客", "客户")}</th>
                  <th>{t("日程", "日期")}</th>
                  <th>{t("売上予定", "应收金额")}</th>
                  <th>{t("入金", "实收")}</th>
                  <th>{t("返金", "退款")}</th>
                  <th>{t("方法", "方式")}</th>
                  <th>{t("操作", "操作")}</th>
                </tr>
              </thead>
              <tbody>
                {summary.rows.length ? (
                  summary.rows.map((row) => (
                    <tr
                      key={row.reservationNo}
                      className={selected?.reservationNo === row.reservationNo ? "selected-row" : ""}
                    >
                      <td>{row.reservationNo}</td>
                      <td>{row.guestName}</td>
                      <td>
                        {row.checkInDate} → {row.checkOutDate}
                      </td>
                      <td>{yen(row.totalAmount)}</td>
                      <td>{yen(row.receivedAmount)}</td>
                      <td>{yen(row.refundAmount)}</td>
                      <td>{paymentMethodLabel(row.paymentMethod, t)}</td>
                      <td>
                        <button type="button" onClick={() => choose(row)}>
                          {t("記録", "记录")}
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="8" className="empty">
                      {t("対象データがありません。", "没有符合条件的数据。")}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
        {selected && finance ? (
          <aside className="panel finance-editor">
            <span className="section-kicker">{selected.reservationNo}</span>
            <h2>{selected.guestName}</h2>
            <form className="form" onSubmit={submitPayment}>
              <h3>{t("入金", "收款")}</h3>
              <label>
                {t("入金額", "实收金额")}
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={payment.amount}
                  onChange={(event) => setPayment({ ...payment, amount: event.target.value })}
                />
              </label>
              <label>
                {t("支払方法", "支付方式")}
                <select
                  value={payment.method}
                  onChange={(event) => setPayment({ ...payment, method: event.target.value })}
                >
                  <option value="cash">{t("現金", "现金")}</option>
                  <option value="card">{t("カード", "银行卡")}</option>
                  <option value="transfer">{t("振込", "转账")}</option>
                  <option value="platform">{t("予約サイト", "平台收款")}</option>
                </select>
              </label>
              <button type="submit">{t("入金を保存", "保存收款")}</button>
            </form>
            <form className="form refund-form" onSubmit={submitRefund}>
              <h3>{t("返金", "退款")}</h3>
              <label>
                {t("返金額", "退款金额")}
                <input
                  type="number"
                  min="0"
                  max={finance.receivedAmount || 0}
                  step="1"
                  value={refund.amount}
                  onChange={(event) => setRefund({ ...refund, amount: event.target.value })}
                />
              </label>
              <button type="submit" className="danger">
                {t("返金を保存", "保存退款")}
              </button>
            </form>
          </aside>
        ) : null}
      </div>
      <section className="panel backup-panel">
        <div className="section-title">
          <div>
            <span className="section-kicker">LOCAL BACKUP</span>
            <h2>{t("バックアップ", "备份")}</h2>
          </div>
          <button type="button" onClick={runBackup}>
            {t("手動バックアップ", "立即备份")}
          </button>
        </div>
        <p>
          <strong>{t("保存先フォルダ", "保存文件夹")}:</strong> <code>{backups.directory}</code>
        </p>
        <p className="muted">
          {t("毎日自動保存します。古いバックアップは自動削除しません。", "每天自动保存，不会自动删除旧备份。")}
        </p>
        <div className="backup-history">
          {backups.history.slice(0, 5).map((item) => (
            <div key={item.id}>
              <Tag
                status={item.status === "success" ? "is-paid" : item.status === "failed" ? "is-cancelled" : "is-booked"}
              >
                {item.status}
              </Tag>
              <span>{item.startedAt?.replace("T", " ")}</span>
              <span>{item.fileSizeBytes ? `${Math.round(item.fileSizeBytes / 1024)} KB` : item.message}</span>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}

function paymentMethodLabel(method, t) {
  return (
    {
      cash: t("現金", "现金"),
      card: t("カード", "银行卡"),
      transfer: t("振込", "转账"),
      platform: t("予約サイト", "平台收款"),
      unknown: t("不明", "未知"),
    }[method] || t("未入金", "未收款")
  );
}

function Reservations() {
  const { language, t } = useI18n();
  const [data, setData] = useState({
    reservations: { items: [], page: 1, totalPages: 1, totalCount: 0 },
    cancelledReservations: { items: [], page: 1, totalPages: 1, totalCount: 0 },
    checkedOutReservations: { items: [], page: 1, totalPages: 1, totalCount: 0 },
    rooms: [],
  });
  const [reservationPage, setReservationPage] = useState(1);
  const [cancelledPage, setCancelledPage] = useState(1);
  const [checkedOutPage, setCheckedOutPage] = useState(1);
  const [notice, setNotice] = useState({});
  const [form, setForm] = useState({ guestCount: 1, reservationForm: "公式" });
  const load = useCallback(async () => {
    const params = new URLSearchParams({
      page: String(reservationPage),
      cancelledPage: String(cancelledPage),
      checkedOutPage: String(checkedOutPage),
    });
    try {
      const response = await api(`/api/reservations?${params.toString()}`);
      setData(response);
      setReservationPage(response.reservations.page);
      setCancelledPage(response.cancelledReservations.page);
      setCheckedOutPage(response.checkedOutReservations.page);
    } catch (error) {
      setNotice({ error: error.message });
    }
  }, [reservationPage, cancelledPage, checkedOutPage]);
  useEffect(() => {
    load();
  }, [load]);
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
      setForm({ guestCount: 1, reservationForm: "公式" });
      load();
    } catch (error) {
      setNotice({ error: error.message });
    }
  };
  const checkIn = async (item) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/check-in`, { method: "POST" });
      setNotice({ message: res.message });
      load();
    }, setNotice);
  };
  const checkOut = async (item) => {
    runAction(async () => {
      const res = await api(`/api/reservations/${item.id}/check-out`, { method: "POST" });
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
    <main className="page grid-page reservations-page" id="main-content">
      <PageHeader
        eyebrow="宿泊予約帳"
        title="予約管理"
        description="新しい予約の登録と、宿泊・支払い・清掃状況を管理します。"
      />
      <section className="panel form-panel reservation-form-panel">
        <span className="section-kicker">NEW RESERVATION</span>
        <h2>新規予約</h2>
        <Notice {...notice} onClose={() => setNotice({})} />
        <form className="form" onSubmit={submit}>
          <label>
            部屋
            <select value={form.roomId || ""} onChange={(e) => setForm({ ...form, roomId: e.target.value })}>
              <option value="">空室・清掃済の部屋を選択してください</option>
              {data.rooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.roomNumber} {room.roomName} / {t("定員", "定员")}
                  {room.capacity}
                  {language === "zh" ? "人" : "名"}
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
                type="tel"
                autoComplete="tel"
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
                type="email"
                autoComplete="email"
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
      <section className="panel wide data-panel reservation-data-panel">
        <div className="section-title">
          <div>
            <span className="section-kicker">ACTIVE STAYS</span>
            <h2>予約一覧</h2>
          </div>
          <span className="record-count">
            {language === "zh"
              ? `共${data.reservations.totalCount}条・显示${data.reservations.items.length}条`
              : `全${data.reservations.totalCount}件・${data.reservations.items.length}件表示`}
          </span>
        </div>
        <ReservationTable
          reservations={data.reservations.items}
          onCheckIn={checkIn}
          onCheckOut={checkOut}
          onCancel={cancel}
          caption="宿泊予約一覧"
        />
        <Pager
          page={data.reservations.page}
          totalPages={data.reservations.totalPages}
          onPageChange={setReservationPage}
        />
        <h2 className="subsection-title">取消済み予約</h2>
        <ReservationTable
          reservations={data.cancelledReservations.items}
          readonly
          onDelete={deleteCancelled}
          showDelete
          caption="取消済み予約一覧"
        />
        <Pager
          page={data.cancelledReservations.page}
          totalPages={data.cancelledReservations.totalPages}
          onPageChange={setCancelledPage}
        />
        <h2 className="subsection-title">チェックアウト済み</h2>
        <CheckoutTable
          reservations={data.checkedOutReservations.items}
          onCleaning={cleaning}
          onDelete={deleteCheckedOut}
        />
        <Pager
          page={data.checkedOutReservations.page}
          totalPages={data.checkedOutReservations.totalPages}
          onPageChange={setCheckedOutPage}
        />
      </section>
    </main>
  );
}

function ReservationTable({
  reservations = [],
  onCheckIn,
  onCheckOut,
  onCancel,
  onDelete,
  readonly = false,
  compact = false,
  showDelete = false,
  caption,
}) {
  // Keep a shared column definition so active, cancelled, and dashboard reservation tables stay aligned.
  return (
    <div className="table-scroll">
      <table className={`table reservation-table ${compact ? "dashboard-table" : "reservation-table-full"}`}>
        <caption className="visually-hidden">{caption || (compact ? "直近の予約一覧" : "宿泊予約一覧")}</caption>
        <ReservationColGroup compact={compact} includeActions={!readonly && !compact} includeDelete={showDelete} />
        <thead>
          <tr>
            <th>予約番号</th>
            <th>部屋</th>
            <th>予約者</th>
            <th>人数</th>
            {!compact && <th>電話</th>}
            {!compact && <th>メール</th>}
            {!compact && <th>予約形式</th>}
            {!compact && <th>同行者</th>}
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
                onCheckIn={onCheckIn}
                onCheckOut={onCheckOut}
                onCancel={onCancel}
                onDelete={onDelete}
                readonly={readonly || compact}
                compact={compact}
                showDelete={showDelete}
              />
            ))
          ) : (
            <tr>
              <td colSpan={compact ? 8 : 13} className="empty">
                予約データがありません。
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function ReservationColGroup({ compact, includeActions, includeDelete }) {
  return (
    <colgroup>
      <col className="col-no" />
      <col className="col-room" />
      <col className="col-guest" />
      <col className="col-count" />
      {!compact && <col className="col-phone" />}
      {!compact && <col className="col-email" />}
      {!compact && <col className="col-form" />}
      {!compact && <col className="col-companion" />}
      <col className="col-date" />
      <col className="col-amount" />
      <col className="col-reservation-status" />
      <col className="col-payment-status" />
      {includeActions && <col className="col-actions" />}
      {includeDelete && <col className="col-delete-actions" />}
    </colgroup>
  );
}

function ReservationRow({ item, onCheckIn, onCheckOut, onCancel, onDelete, readonly, compact, showDelete }) {
  const { language } = useI18n();
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
            item.guestAge ? `${item.guestAge}${language === "zh" ? "岁" : "歳"}` : "年齢未入力",
          ]}
        />
      </td>
      <td>
        {item.guestCount}
        {language === "zh" ? "人" : "名"}
      </td>
      {!compact && <td>{text(item.guestPhone, "電話未入力")}</td>}
      {!compact && <td>{text(item.guestEmail, "メール未入力")}</td>}
      {!compact && <td>{text(item.reservationForm, "公式")}</td>}
      {!compact && (
        <td className="companion-summary">
          <CompanionSummary value={item.companionSummary} />
        </td>
      )}
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
        <Tag
          status={
            item.paymentStatus === "paid"
              ? "is-paid"
              : item.paymentStatus === "refunded" || item.paymentStatus === "partially_refunded"
                ? "is-cancelled"
                : "is-unpaid"
          }
        >
          {item.paymentStatusLabel}
        </Tag>
      </td>
      {allowActions && (
        <td className="actions">
          <div className="reservation-actions">
            {item.reservationStatus === "booked" ? (
              <button type="button" onClick={() => onCheckIn(item)}>
                チェックイン
              </button>
            ) : null}
            {item.reservationStatus === "checked_in" ? (
              <button type="button" onClick={() => onCheckOut(item)}>
                チェックアウト
              </button>
            ) : null}
            <a className="button button-quiet" href={withContext(`/finance?reservation=${item.id}`)}>
              入返金
            </a>
            {item.reservationStatus === "booked" ? (
              <button type="button" className="danger" onClick={() => onCancel(item)}>
                取消
              </button>
            ) : null}
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
        <caption className="visually-hidden">チェックアウト済み予約一覧</caption>
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
  const { language } = useI18n();
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
            item.guestAge ? `${item.guestAge}${language === "zh" ? "岁" : "歳"}` : "年齢未入力",
          ]}
        />
      </td>
      <td>
        {item.guestCount}
        {language === "zh" ? "人" : "名"}
      </td>
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
          <select
            aria-label={`${item.reservationNo}の清掃状態`}
            value={cleaningStatus}
            onChange={(e) => setCleaningStatus(e.target.value)}
          >
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

function ProtectedApplication({ onLogout }) {
  const route = currentRoute();
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        本文へ移動
      </a>
      <Nav route={route} onLogout={onLogout} />
      <div className="app-content">
        {route === "/rooms" ? (
          <Rooms />
        ) : route === "/reservations" ? (
          <Reservations />
        ) : route === "/prices" ? (
          <Prices />
        ) : route === "/customers" ? (
          <Customers />
        ) : route === "/finance" || route === "/reports" ? (
          <Finance />
        ) : (
          <Dashboard />
        )}
      </div>
    </div>
  );
}

function App() {
  const [authStatus, setAuthStatus] = useState(null);
  const [fatalError, setFatalError] = useState("");

  useEffect(() => {
    api("/api/auth/status")
      .then(setAuthStatus)
      .catch((error) => setFatalError(error.message));
  }, []);

  const authenticated = (username) => {
    window.history.replaceState({}, "", withContext("/dashboard"));
    setAuthStatus({ authenticated: true, setupRequired: false, username });
  };

  const logout = async () => {
    try {
      await api("/api/auth/logout", { method: "POST" });
    } finally {
      window.history.replaceState({}, "", withContext("/login"));
      setAuthStatus({ authenticated: false, setupRequired: false, username: null });
    }
  };

  let content;
  if (fatalError) {
    content = (
      <main className="auth-page">
        <section className="auth-card">
          <h1>起動エラー</h1>
          <p>{fatalError}</p>
        </section>
      </main>
    );
  } else if (!authStatus) {
    content = (
      <main className="auth-page">
        <section className="auth-card" role="status">
          読み込み中...
        </section>
      </main>
    );
  } else if (!authStatus.authenticated) {
    content = <AuthenticationScreen authStatus={authStatus} onAuthenticated={authenticated} />;
  } else {
    content = <ProtectedApplication onLogout={logout} />;
  }

  return (
    <I18nProvider>
      <TranslationBoundary>{content}</TranslationBoundary>
    </I18nProvider>
  );
}

createRoot(document.getElementById("root")).render(<App />);
