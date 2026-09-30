import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  ArrowRight,
  Bed,
  Bus,
  CalendarBlank,
  Check,
  CheckCircle,
  Clock,
  Coffee,
  ForkKnife,
  List,
  MapPin,
  MagnifyingGlass,
  Mountains,
  Phone,
  ShieldCheck,
  Snowflake,
  Train,
  Users,
  WifiHigh,
  X,
} from "@phosphor-icons/react";

const guestScript = document.querySelector('script[src$="/js/guest.js"]')?.getAttribute("src") || "";
const contextPath = guestScript.endsWith("/js/guest.js") ? guestScript.slice(0, -"/js/guest.js".length) : "";
const url = (path) => `${contextPath}${path}`;
const asset = (name) => url(`/images/guest/${name}`);
const languageLabels = { ja: "日本語", zh: "中文", en: "EN" };
const pick = (language, values) => values[language] || values.ja;

async function guestApi(path, options = {}) {
  const response = await fetch(url(path), {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.error || "Request failed");
  return payload;
}

const navItems = [
  ["/stay/rooms", { ja: "客室", zh: "房间", en: "Rooms" }],
  ["/stay/rates", { ja: "料金", zh: "价格", en: "Rates" }],
  ["/stay/guide", { ja: "宿泊案内", zh: "入住须知", en: "Stay guide" }],
  ["/stay/access", { ja: "周辺・アクセス", zh: "景点·交通", en: "Access" }],
  ["/stay/ski", { ja: "スキー場", zh: "滑雪场", en: "Ski areas" }],
  ["/stay/booking", { ja: "予約確認", zh: "查询预约", en: "My booking" }],
];

const rooms = [
  {
    id: "twin",
    image: "room-twin.png",
    title: { ja: "マウンテン・ツイン", zh: "山景双床房", en: "Mountain twin" },
    description: {
      ja: "雪山を望む、明るく落ち着いたツインルーム。滑ったあとの休息にちょうどいい空間です。",
      zh: "可眺望雪山的明亮双床房，为一天的滑雪旅程带来安稳休息。",
      en: "A bright twin room with mountain views, made for a restful night after the slopes.",
    },
    capacity: { ja: "1〜2名", zh: "1–2人", en: "1–2 guests" },
    price: "¥18,000",
    features: {
      ja: ["シングルベッド2台", "雪山ビュー", "Wi-Fi"],
      zh: ["2张单人床", "雪山景观", "Wi-Fi"],
      en: ["2 single beds", "Mountain view", "Wi-Fi"],
    },
  },
  {
    id: "japanese",
    image: "room-japanese.png",
    title: { ja: "雪見の和室", zh: "雪景和室", en: "Snow-view tatami" },
    description: {
      ja: "畳と障子のやさしい和室。静かな時間と、日本らしい滞在を楽しめます。",
      zh: "榻榻米与障子的温柔空间，适合享受安静而地道的日式住宿。",
      en: "A calm tatami room with shoji details for a distinctly Japanese stay.",
    },
    capacity: { ja: "1〜3名", zh: "1–3人", en: "1–3 guests" },
    price: "¥16,000",
    features: {
      ja: ["布団3組まで", "畳8帖", "Wi-Fi"],
      zh: ["最多3套被褥", "8帖榻榻米", "Wi-Fi"],
      en: ["Up to 3 futons", "8-tatami room", "Wi-Fi"],
    },
  },
  {
    id: "loft",
    image: "room-family-loft.png",
    title: { ja: "ファミリー・ロフト", zh: "家庭阁楼房", en: "Family loft" },
    description: {
      ja: "大人も子どももわくわくするロフト付き。グループ旅行の拠点におすすめです。",
      zh: "带安全阁楼的趣味空间，适合家庭与朋友结伴旅行。",
      en: "A playful loft room for families and friends travelling together.",
    },
    capacity: { ja: "2〜4名", zh: "2–4人", en: "2–4 guests" },
    price: "¥20,000",
    features: {
      ja: ["クイーンベッド1台", "ロフト2名分", "Wi-Fi"],
      zh: ["1张大床", "阁楼可住2人", "Wi-Fi"],
      en: ["1 queen bed", "Loft for 2", "Wi-Fi"],
    },
  },
];

function useLanguage() {
  const [language, setLanguage] = useState(() => localStorage.getItem("guest-site-language") || "ja");
  useEffect(() => {
    localStorage.setItem("guest-site-language", language);
    document.documentElement.lang = language === "zh" ? "zh-CN" : language;
  }, [language]);
  return [language, setLanguage];
}

function LanguageSwitch({ language, onChange }) {
  return (
    <div className="guest-language" aria-label="Language">
      {Object.entries(languageLabels).map(([code, label]) => (
        <button
          type="button"
          key={code}
          className={language === code ? "active" : ""}
          aria-pressed={language === code}
          onClick={() => onChange(code)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}

function SiteHeader({ language, setLanguage, route }) {
  const [open, setOpen] = useState(false);
  return (
    <header className="guest-header">
      <a className="guest-brand" href={url("/stay")} aria-label="Hakuba Jukai home">
        <strong>白馬樹海</strong>
        <span>HAKUBA JUKAI</span>
      </a>
      <button
        className="guest-menu-button"
        type="button"
        aria-label={open ? "Close menu" : "Open menu"}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        {open ? <X size={26} /> : <List size={26} />}
      </button>
      <div className={`guest-nav-wrap ${open ? "open" : ""}`}>
        <nav className="guest-nav" aria-label="Main navigation">
          {navItems.map(([path, label]) => (
            <a key={path} className={route === path ? "active" : ""} href={url(path)}>
              {pick(language, label)}
            </a>
          ))}
        </nav>
        <LanguageSwitch language={language} onChange={setLanguage} />
        <a className="button coral compact" href={url("/stay/reserve")}>
          {pick(language, { ja: "空室を検索", zh: "查询空房", en: "Check rooms" })}
          <ArrowRight weight="bold" />
        </a>
      </div>
    </header>
  );
}

function BookingBar({ language, className = "" }) {
  const [checkIn, setCheckIn] = useState("2027-01-10");
  const [checkOut, setCheckOut] = useState("2027-01-14");
  const [guests, setGuests] = useState("2");
  const submit = (event) => {
    event.preventDefault();
    const query = new URLSearchParams({ checkIn, checkOut, guests });
    window.location.href = `${url("/stay/reserve")}?${query}`;
  };
  return (
    <form className={`booking-bar ${className}`} onSubmit={submit}>
      <label>
        <span>{pick(language, { ja: "チェックイン", zh: "入住", en: "Check-in" })}</span>
        <span className="booking-input">
          <CalendarBlank />
          <input type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} required />
        </span>
      </label>
      <label>
        <span>{pick(language, { ja: "チェックアウト", zh: "退房", en: "Check-out" })}</span>
        <span className="booking-input">
          <CalendarBlank />
          <input type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} required />
        </span>
      </label>
      <label>
        <span>{pick(language, { ja: "人数", zh: "人数", en: "Guests" })}</span>
        <span className="booking-input">
          <Users />
          <select value={guests} onChange={(e) => setGuests(e.target.value)}>
            {[1, 2, 3, 4].map((count) => (
              <option key={count} value={count}>
                {count}
              </option>
            ))}
          </select>
        </span>
      </label>
      <button className="button coral" type="submit">
        {pick(language, { ja: "空室を検索", zh: "查询空房", en: "Check rooms" })}
        <ArrowRight weight="bold" />
      </button>
    </form>
  );
}

function SectionHeading({ kicker, title, description, light = false }) {
  return (
    <div className={`section-heading ${light ? "light" : ""}`}>
      <p>{kicker}</p>
      <h2>{title}</h2>
      {description ? <span>{description}</span> : null}
    </div>
  );
}

function RoomCard({ room, language, detailed = false }) {
  return (
    <article className={`guest-room-card ${detailed ? "detailed" : ""}`}>
      <img src={asset(room.image)} alt={pick(language, room.title)} />
      <div className="room-card-body">
        <div className="room-card-title">
          <div>
            <p>{room.id.toUpperCase()}</p>
            <h3>{pick(language, room.title)}</h3>
          </div>
          <span>{pick(language, room.capacity)}</span>
        </div>
        <p>{pick(language, room.description)}</p>
        {detailed ? (
          <ul>
            {room.features[language].map((feature) => (
              <li key={feature}>
                <Check size={16} weight="bold" />
                {feature}
              </li>
            ))}
          </ul>
        ) : null}
        <div className="room-price">
          <span>{pick(language, { ja: "1室1泊〜", zh: "每间每晚起", en: "From / room / night" })}</span>
          <strong>{room.price}</strong>
        </div>
      </div>
    </article>
  );
}

function HomePage({ language }) {
  return (
    <>
      <section className="guest-hero">
        <img src={asset("hero-winter-lodge.png")} alt="Hakuba mountain lodge in winter" />
        <div className="hero-copy">
          <p>HAKUBA, NAGANO</p>
          <h1>
            H<span>A</span>KUBA
            <br />
            JUKAI
          </h1>
          <strong>
            {pick(language, {
              ja: "雪の一日を、ここから。",
              zh: "从这里，开启雪地的一天。",
              en: "Your day in the snow starts here.",
            })}
          </strong>
          <a className="hero-link" href="#rooms-preview">
            {pick(language, { ja: "滞在を見つける", zh: "发现你的住宿", en: "Find your stay" })}
            <ArrowRight />
          </a>
        </div>
      </section>
      <div className="booking-shell">
        <BookingBar language={language} />
      </div>
      <main>
        <section className="content-section rooms-preview" id="rooms-preview">
          <SectionHeading
            kicker="STAY YOUR WAY"
            title="ROOMS"
            description={pick(language, {
              ja: "窓の外は白馬の山。木のぬくもりに包まれて、次の一本へ備えよう。",
              zh: "窗外是白马群山，在木香与暖意中为下一次滑行充电。",
              en: "Mountain views outside, warm timber within — reset for your next run.",
            })}
          />
          <div className="room-grid">
            {rooms.map((room) => (
              <RoomCard key={room.id} room={room} language={language} />
            ))}
          </div>
          <a className="text-link" href={url("/stay/rooms")}>
            {pick(language, { ja: "すべての客室を見る", zh: "查看全部房间", en: "Explore all rooms" })}
            <ArrowRight />
          </a>
        </section>
        <section className="split-feature navy">
          <img src={asset("ski-powder.png")} alt="Skier in fresh powder" />
          <div>
            <SectionHeading
              light
              kicker="SKI AREAS"
              title={pick(language, {
                ja: "今日は、どの雪へ？",
                zh: "今天，去滑哪片雪？",
                en: "Which mountain today?",
              })}
            />
            <p>
              {pick(language, {
                ja: "白馬エリアの雪山を、その日の天気や気分に合わせて選ぶ。各スキー場の特徴と移動方法をまとめています。",
                zh: "根据天气与心情选择白马地区的雪场，快速了解各雪场特色与交通方式。",
                en: "Choose a mountain to match the weather and your mood, with simple guides to terrain and transport.",
              })}
            </p>
            <a className="button coral" href={url("/stay/ski")}>
              {pick(language, { ja: "スキー場ガイド", zh: "查看滑雪场指南", en: "See the ski guide" })}
              <ArrowRight />
            </a>
          </div>
        </section>
        <section className="content-section arrival-section">
          <div className="arrival-copy">
            <SectionHeading
              kicker="ACCESS & LOCAL"
              title={pick(language, {
                ja: "到着した瞬間から、白馬時間。",
                zh: "抵达的瞬间，白马时光开始。",
                en: "Hakuba time starts when you arrive.",
              })}
            />
            <p>
              {pick(language, {
                ja: "駅からの移動、村内シャトル、食事や温泉など、滞在前に知っておきたい情報をひとつに。",
                zh: "车站接驳、村内巴士、美食与温泉，出发前需要的信息都集中在这里。",
                en: "Station transfers, village shuttles, food and hot springs — all the essentials in one place.",
              })}
            </p>
            <a className="text-link" href={url("/stay/access")}>
              {pick(language, { ja: "周辺とアクセスを見る", zh: "查看周边与交通", en: "Explore access and the area" })}
              <ArrowRight />
            </a>
          </div>
          <img src={asset("hakuba-arrival.png")} alt="Arrival in a snowy mountain village" />
        </section>
        <CallToAction language={language} />
      </main>
    </>
  );
}

function PageIntro({ eyebrow, title, description, image }) {
  return (
    <section className="page-intro">
      <div>
        <p>{eyebrow}</p>
        <h1>{title}</h1>
        <span>{description}</span>
      </div>
      {image ? <img src={asset(image)} alt="" /> : null}
    </section>
  );
}

function RoomsPage({ language }) {
  return (
    <main>
      <PageIntro
        language={language}
        eyebrow="ROOMS"
        title={pick(language, { ja: "山を近くに感じる客室", zh: "住进雪山近处", en: "Rooms close to the mountains" })}
        description={pick(language, {
          ja: "木のぬくもり、心地よい寝具、雪景色。旅の人数と過ごし方に合う部屋を選べます。",
          zh: "木质温度、舒适寝具与窗外雪景，按人数与旅行方式挑选。",
          en: "Warm timber, comfortable bedding and snow views. Choose the room that fits your trip.",
        })}
        image="room-twin.png"
      />
      <section className="content-section">
        <div className="room-grid detailed-grid">
          {rooms.map((room) => (
            <RoomCard key={room.id} room={room} language={language} detailed />
          ))}
        </div>
      </section>
      <section className="amenity-strip">
        <SectionHeading
          kicker="SHARED AMENITIES"
          title={pick(language, {
            ja: "身軽に泊まれる設備",
            zh: "轻松入住的共享设施",
            en: "Shared comforts for an easy stay",
          })}
        />
        <div>
          {[
            [WifiHigh, { ja: "館内Wi-Fi", zh: "全馆Wi-Fi", en: "Property-wide Wi-Fi" }],
            [Coffee, { ja: "ラウンジ", zh: "共享休息室", en: "Guest lounge" }],
            [Snowflake, { ja: "乾燥室", zh: "滑雪装备干燥室", en: "Ski drying room" }],
            [ForkKnife, { ja: "共用キッチン", zh: "共享厨房", en: "Shared kitchen" }],
          ].map(([Icon, label]) => (
            <span key={label.en}>
              <Icon size={28} />
              <strong>{pick(language, label)}</strong>
            </span>
          ))}
        </div>
      </section>
      <CallToAction language={language} />
    </main>
  );
}

function RatesPage({ language }) {
  return (
    <main>
      <PageIntro
        eyebrow="RATES"
        title={pick(language, { ja: "シンプルで分かりやすい料金", zh: "简单清晰的价格", en: "Simple, clear rates" })}
        description={pick(language, {
          ja: "このページは客室タイプ別の参考料金です。予約画面では具体的な客室と日付の実料金を表示します。",
          zh: "本页为房型参考价，预约页会根据具体房间与日期显示实时价格。",
          en: "These are room-type guide rates. The booking page shows live prices for exact rooms and dates.",
        })}
      />
      <section className="content-section rates-layout">
        <div className="rate-table-wrap">
          <table className="rate-table">
            <caption>{pick(language, { ja: "客室別参考料金", zh: "各房型参考价", en: "Sample room rates" })}</caption>
            <thead>
              <tr>
                <th>{pick(language, { ja: "客室", zh: "房型", en: "Room" })}</th>
                <th>{pick(language, { ja: "定員", zh: "人数", en: "Guests" })}</th>
                <th>{pick(language, { ja: "通常期", zh: "普通季", en: "Regular" })}</th>
                <th>{pick(language, { ja: "冬季参考", zh: "冬季参考", en: "Winter sample" })}</th>
              </tr>
            </thead>
            <tbody>
              {rooms.map((room, index) => (
                <tr key={room.id}>
                  <td>{pick(language, room.title)}</td>
                  <td>{pick(language, room.capacity)}</td>
                  <td>{room.price}</td>
                  <td>{["¥24,000", "¥22,000", "¥28,000"][index]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <aside className="rate-notes">
          <h2>{pick(language, { ja: "料金に含まれるもの", zh: "价格包含", en: "What is included" })}</h2>
          <ul>
            <li>
              <CheckCircle />
              {pick(language, { ja: "消費税", zh: "消费税", en: "Consumption tax" })}
            </li>
            <li>
              <CheckCircle />
              {pick(language, { ja: "Wi-Fi・共用設備", zh: "Wi-Fi与共享设施", en: "Wi-Fi and shared facilities" })}
            </li>
            <li>
              <CheckCircle />
              {pick(language, { ja: "基本アメニティ", zh: "基础洗护用品", en: "Essential amenities" })}
            </li>
          </ul>
          <p>
            {pick(language, {
              ja: "食事、レンタル、送迎は別料金となる場合があります。正式料金は予約確認時にご案内します。",
              zh: "餐食、租赁与接送可能另行收费，确认预约时将说明正式价格。",
              en: "Meals, rentals and transfers may cost extra. Your final rate is confirmed with the reservation.",
            })}
          </p>
        </aside>
      </section>
      <CallToAction language={language} />
    </main>
  );
}

const guideItems = [
  [
    Clock,
    { ja: "チェックイン / アウト", zh: "入住 / 退房", en: "Check-in / out" },
    {
      ja: "チェックイン 15:00〜、チェックアウト 10:00まで（サンプル）",
      zh: "入住15:00起，退房10:00前（样例）",
      en: "Check-in from 15:00, check-out by 10:00 (sample)",
    },
  ],
  [
    Snowflake,
    { ja: "スキー用品", zh: "滑雪装备", en: "Ski gear" },
    {
      ja: "板・ブーツは客室へ持ち込まず、専用乾燥室をご利用ください。",
      zh: "雪板与雪靴请存放在专用干燥室，不要带入客房。",
      en: "Please leave skis and boots in the drying room, not in guest rooms.",
    },
  ],
  [
    ShieldCheck,
    { ja: "館内ルール", zh: "馆内规则", en: "House rules" },
    {
      ja: "館内は禁煙です。22:00以降は静かにお過ごしください。",
      zh: "馆内禁烟，22:00后请保持安静。",
      en: "The property is smoke-free. Please keep noise low after 22:00.",
    },
  ],
  [
    Bed,
    { ja: "お子さま", zh: "儿童入住", en: "Children" },
    {
      ja: "お子さまの年齢と寝具の希望を予約時にお知らせください。",
      zh: "预约时请告知儿童年龄与寝具需求。",
      en: "Tell us each child’s age and bedding needs when requesting a stay.",
    },
  ],
];

function GuidePage({ language }) {
  return (
    <main>
      <PageIntro
        eyebrow="STAY GUIDE"
        title={pick(language, {
          ja: "気持ちよく過ごすために",
          zh: "舒适入住小指南",
          en: "For an easy, comfortable stay",
        })}
        description={pick(language, {
          ja: "到着前に知っておくと安心なことをまとめました。内容は正式運用前に実施設情報へ更新します。",
          zh: "出发前值得了解的事项，正式上线前将替换为实际设施信息。",
          en: "A quick guide before you arrive. Details will be replaced with confirmed property information before launch.",
        })}
      />
      <section className="content-section guide-grid">
        {guideItems.map(([Icon, title, text]) => (
          <article key={title.en}>
            <Icon size={34} />
            <h2>{pick(language, title)}</h2>
            <p>{pick(language, text)}</p>
          </article>
        ))}
      </section>
      <section className="quiet-section">
        <div>
          <h2>{pick(language, { ja: "持ち物チェック", zh: "行李清单", en: "Packing checklist" })}</h2>
          <p>
            {pick(language, {
              ja: "防水の靴、手袋、保温着、常用薬、身分証明書。冬の白馬は天候が変わりやすいため、重ね着がおすすめです。",
              zh: "防水鞋、手套、保暖衣物、常用药与身份证件。白马冬季天气多变，建议分层穿着。",
              en: "Waterproof shoes, gloves, warm layers, medication and ID. Hakuba weather changes quickly, so pack layers.",
            })}
          </p>
        </div>
        <div>
          <h2>{pick(language, { ja: "連絡先", zh: "联系方式", en: "Contact" })}</h2>
          <p>
            <Phone />
            {pick(language, { ja: "電話：待設定", zh: "电话：待设置", en: "Phone: To be configured" })}
          </p>
          <p>{pick(language, { ja: "メール：待設定", zh: "邮箱：待设置", en: "Email: To be configured" })}</p>
        </div>
      </section>
    </main>
  );
}

function AccessPage({ language }) {
  const routes = [
    [
      Train,
      { ja: "長野方面から", zh: "从长野方向", en: "From Nagano" },
      {
        ja: "鉄道・高速バスで白馬エリアへ。最新の運行情報をご確認ください。",
        zh: "乘铁路或高速巴士前往白马地区，请提前确认最新班次。",
        en: "Travel to the Hakuba area by rail or highway bus. Check the latest timetable.",
      },
    ],
    [
      Bus,
      { ja: "村内の移動", zh: "村内交通", en: "Around the village" },
      {
        ja: "季節運行のシャトルや路線バスを利用できます。停留所は正式公開時にご案内します。",
        zh: "可乘季节接驳车与公交，正式上线时将公布具体站点。",
        en: "Seasonal shuttles and local buses are available. Exact stops will be confirmed at launch.",
      },
    ],
    [
      MapPin,
      { ja: "白馬樹海まで", zh: "前往白马树海", en: "To Hakuba Jukai" },
      {
        ja: "住所・送迎範囲・駐車案内：待設定",
        zh: "地址、接送范围与停车信息：待设置",
        en: "Address, pick-up area and parking: To be configured",
      },
    ],
  ];
  return (
    <main>
      <PageIntro
        eyebrow="ACCESS & LOCAL"
        title={pick(language, {
          ja: "白馬へ、迷わず到着。",
          zh: "顺利抵达白马",
          en: "Arrive in Hakuba with confidence",
        })}
        description={pick(language, {
          ja: "出発地から宿までの流れと、滞在中の移動方法を分かりやすくご案内します。",
          zh: "清楚了解从出发地到旅宿，以及入住期间的交通方式。",
          en: "A clear guide from your starting point to the lodge, plus local transport during your stay.",
        })}
        image="hakuba-arrival.png"
      />
      <section className="content-section route-list">
        {routes.map(([Icon, title, text], index) => (
          <article key={title.en}>
            <span>{String(index + 1).padStart(2, "0")}</span>
            <Icon size={34} />
            <div>
              <h2>{pick(language, title)}</h2>
              <p>{pick(language, text)}</p>
            </div>
          </article>
        ))}
      </section>
      <section className="local-grid">
        <SectionHeading
          kicker="AROUND HAKUBA"
          title={pick(language, { ja: "滑る以外の白馬も。", zh: "滑雪之外的白马", en: "Hakuba beyond the slopes" })}
        />
        <div>
          {[
            ["ONSEN", { ja: "温泉", zh: "温泉", en: "Hot springs" }],
            ["FOOD", { ja: "ローカルフード", zh: "当地美食", en: "Local food" }],
            ["SNOW WALK", { ja: "雪上散歩", zh: "雪地漫步", en: "Snow walks" }],
          ].map(([tag, title]) => (
            <article key={tag}>
              <p>{tag}</p>
              <h3>{pick(language, title)}</h3>
              <span>
                {pick(language, {
                  ja: "おすすめ情報は正式公開時に更新します。",
                  zh: "推荐信息将在正式上线时更新。",
                  en: "Recommendations will be confirmed before launch.",
                })}
              </span>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}

function SkiPage({ language }) {
  const areas = [
    [
      "HAKUBA HAPPO-ONE",
      { ja: "広い山岳景観と多彩なコース", zh: "壮阔山景与多样雪道", en: "Big mountain views and varied terrain" },
    ],
    [
      "HAKUBA 47 / GORYU",
      { ja: "幅広いレベルで楽しみやすい", zh: "适合不同水平的滑雪者", en: "Friendly terrain for mixed abilities" },
    ],
    [
      "HAKUBA IWATAKE",
      { ja: "景色と山頂時間も楽しめる", zh: "兼具滑行与山顶景观", en: "Runs, views and time at the summit" },
    ],
  ];
  return (
    <main>
      <section className="ski-hero">
        <img src={asset("ski-powder.png")} alt="Powder skier in Hakuba mountains" />
        <div>
          <p>SKI AREAS</p>
          <h1>
            {pick(language, {
              ja: "白馬の雪を、遊びつくす。",
              zh: "尽情驰骋白马雪山",
              en: "Make the most of Hakuba snow.",
            })}
          </h1>
          <span>
            {pick(language, {
              ja: "天候、雪質、レベルに合わせて、その日の山を選ぼう。",
              zh: "根据天气、雪质与水平，选择今天的雪场。",
              en: "Pick today’s mountain for the weather, snow and your level.",
            })}
          </span>
        </div>
      </section>
      <section className="content-section ski-area-list">
        {areas.map(([name, description], index) => (
          <article key={name}>
            <span>{String(index + 1).padStart(2, "0")}</span>
            <div>
              <h2>{name}</h2>
              <p>{pick(language, description)}</p>
            </div>
            <a href={url("/stay/reserve")}>
              {pick(language, { ja: "この周辺で泊まる", zh: "预约附近住宿", en: "Plan a stay" })}
              <ArrowRight />
            </a>
          </article>
        ))}
      </section>
      <section className="ski-note">
        <Mountains size={42} />
        <div>
          <h2>{pick(language, { ja: "安全に楽しむために", zh: "安全滑雪提示", en: "Ride safely" })}</h2>
          <p>
            {pick(language, {
              ja: "営業状況、リフト券、コース規制、交通は各スキー場の公式情報を当日にご確認ください。",
              zh: "开放情况、雪票、雪道限制与交通，请在当天查看各雪场官方信息。",
              en: "Check each resort’s official updates for lifts, tickets, trail restrictions and transport on the day.",
            })}
          </p>
        </div>
      </section>
    </main>
  );
}

function ReservePage({ language }) {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const [form, setForm] = useState({
    checkIn: params.get("checkIn") || "2027-01-10",
    checkOut: params.get("checkOut") || "2027-01-14",
    name: "",
    email: "",
    phone: "",
    country: "",
    notes: "",
    consent: false,
  });
  const [availableRooms, setAvailableRooms] = useState([]);
  const [selections, setSelections] = useState([]);
  const [quote, setQuote] = useState(null);
  const [configuration, setConfiguration] = useState({ turnstileRequired: false, turnstileSiteKey: "" });
  const [turnstileToken, setTurnstileToken] = useState("");
  const [result, setResult] = useState(null);
  const [state, setState] = useState({ loading: false, error: "" });
  const update = (event) =>
    setForm((value) => ({
      ...value,
      [event.target.name]: event.target.type === "checkbox" ? event.target.checked : event.target.value,
    }));

  useEffect(() => {
    guestApi("/api/stay/config")
      .then(setConfiguration)
      .catch((error) => setState({ error: error.message }));
  }, []);

  useEffect(() => {
    if (!form.checkIn || !form.checkOut || form.checkOut <= form.checkIn) {
      setAvailableRooms([]);
      return;
    }
    let active = true;
    const query = new URLSearchParams({ checkInDate: form.checkIn, checkOutDate: form.checkOut });
    guestApi(`/api/stay/rooms?${query}`)
      .then((items) => {
        if (!active) return;
        setAvailableRooms(items);
        setSelections((current) => current.filter((item) => items.some((room) => room.id === item.roomId)));
        setState({ loading: false, error: "" });
      })
      .catch((error) => active && setState({ loading: false, error: error.message }));
    return () => {
      active = false;
    };
  }, [form.checkIn, form.checkOut]);

  const quoteRooms = useMemo(
    () => selections.map(({ roomId, guestCount }) => ({ roomId, guestCount, guests: [] })),
    [selections]
  );
  useEffect(() => {
    if (!quoteRooms.length) {
      setQuote(null);
      return;
    }
    let active = true;
    guestApi("/api/stay/quote", {
      method: "POST",
      body: JSON.stringify({ checkInDate: form.checkIn, checkOutDate: form.checkOut, rooms: quoteRooms }),
    })
      .then((value) => active && setQuote(value))
      .catch((error) => active && setState({ loading: false, error: error.message }));
    return () => {
      active = false;
    };
  }, [form.checkIn, form.checkOut, quoteRooms]);

  const toggleRoom = (room) => {
    setSelections((current) =>
      current.some((item) => item.roomId === room.id)
        ? current.filter((item) => item.roomId !== room.id)
        : [...current, { roomId: room.id, guestCount: 1, guests: [{ name: "", category: "adult", age: "" }] }]
    );
  };
  const setGuestCount = (roomId, nextCount) => {
    setSelections((current) =>
      current.map((selection) => {
        if (selection.roomId !== roomId) return selection;
        const guests = Array.from(
          { length: nextCount },
          (_, index) => selection.guests[index] || { name: "", category: "adult", age: "" }
        );
        return { ...selection, guestCount: nextCount, guests };
      })
    );
  };
  const setGuest = (roomId, guestIndex, field, value) => {
    setSelections((current) =>
      current.map((selection) => {
        if (selection.roomId !== roomId) return selection;
        return {
          ...selection,
          guests: selection.guests.map((guest, index) => (index === guestIndex ? { ...guest, [field]: value } : guest)),
        };
      })
    );
  };
  const submit = async (event) => {
    event.preventDefault();
    if (!selections.length) {
      setState({
        error: pick(language, { ja: "客室を選択してください。", zh: "请选择房间。", en: "Choose at least one room." }),
      });
      return;
    }
    setState({ loading: true, error: "" });
    try {
      const response = await guestApi("/api/stay/bookings", {
        method: "POST",
        body: JSON.stringify({
          checkInDate: form.checkIn,
          checkOutDate: form.checkOut,
          rooms: selections.map((selection) => ({
            roomId: selection.roomId,
            guestCount: selection.guestCount,
            guests: selection.guests.map((guest) => ({
              name: guest.name,
              category: guest.category,
              age: guest.age === "" ? null : Number(guest.age),
            })),
          })),
          leadName: form.name,
          email: form.email,
          phone: form.phone,
          country: form.country,
          notes: form.notes,
          language,
          consent: form.consent,
          turnstileToken,
        }),
      });
      setResult(response);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setState({ loading: false, error: error.message });
    }
  };
  if (result)
    return (
      <main className="reservation-success">
        <CheckCircle size={64} weight="fill" />
        <p>REQUEST HELD FOR 24 HOURS</p>
        <h1>
          {pick(language, {
            ja: "予約申請を受け付けました",
            zh: "预约申请已提交",
            en: "Your booking request is submitted",
          })}
        </h1>
        <span>
          {pick(language, {
            ja: `申請番号 ${result.requestNo}。客室と料金は ${new Date(result.holdExpiresAt).toLocaleString()} まで保留されます。`,
            zh: `申请编号 ${result.requestNo}。房间和价格将保留至 ${new Date(result.holdExpiresAt).toLocaleString()}。`,
            en: `Request ${result.requestNo}. Rooms and price are held until ${new Date(result.holdExpiresAt).toLocaleString()}.`,
          })}
        </span>
        <strong className="success-total">¥{Number(result.totalAmount).toLocaleString()}</strong>
        <a className="text-link" href={result.cancellationUrl}>
          {pick(language, { ja: "キャンセル用リンクを保存", zh: "保存取消链接", en: "Save cancellation link" })}
        </a>
        <a className="text-link" href={url("/stay/booking")}>
          {pick(language, { ja: "申請状況を確認", zh: "查询预约状态", en: "Check booking status" })}
          <ArrowRight />
        </a>
        <a className="button navy-button" href={url("/stay")}>
          {pick(language, { ja: "トップへ戻る", zh: "返回首页", en: "Back to home" })}
        </a>
      </main>
    );
  return (
    <main>
      <PageIntro
        eyebrow="RESERVATION REQUEST"
        title={pick(language, { ja: "白馬での滞在を計画する", zh: "规划你的白马之旅", en: "Plan your Hakuba stay" })}
        description={pick(language, {
          ja: "日程と希望を送信後、スタッフからの確認をもって予約成立となります。",
          zh: "提交日期与需求后，将由工作人员确认，确认后预约才正式成立。",
          en: "Send your preferred dates and details. A stay is confirmed only after staff approval.",
        })}
      />
      <div className="demo-banner">
        <ShieldCheck size={24} />
        <span>
          {pick(language, {
            ja: "送信すると選択した客室と表示料金を24時間保留します。スタッフ確認後に予約確定となります。",
            zh: "提交后将锁定所选房间及显示价格24小时，工作人员确认后预约正式成立。",
            en: "Submitting holds the selected rooms and displayed price for 24 hours. Staff approval confirms the stay.",
          })}
        </span>
      </div>
      <form className="reservation-form" onSubmit={submit}>
        <section>
          <h2>
            <span>01</span>
            {pick(language, { ja: "日程と人数", zh: "日期与人数", en: "Dates and guests" })}
          </h2>
          <div className="form-grid two">
            <label>
              {pick(language, { ja: "チェックイン", zh: "入住日期", en: "Check-in" })}
              <input name="checkIn" type="date" value={form.checkIn} onChange={update} required />
            </label>
            <label>
              {pick(language, { ja: "チェックアウト", zh: "退房日期", en: "Check-out" })}
              <input name="checkOut" type="date" value={form.checkOut} onChange={update} required />
            </label>
          </div>
        </section>
        <section>
          <h2>
            <span>02</span>
            {pick(language, { ja: "客室と人数を選ぶ", zh: "选择房间与人数", en: "Choose rooms and guests" })}
          </h2>
          <div className="room-choice live-room-choice">
            {availableRooms.map((room, index) => {
              const selected = selections.find((item) => item.roomId === room.id);
              return (
                <label className={selected ? "selected" : ""} key={room.id}>
                  <input type="checkbox" checked={Boolean(selected)} onChange={() => toggleRoom(room)} />
                  <img src={asset(["room-twin.png", "room-japanese.png", "room-family-loft.png"][index % 3])} alt="" />
                  <span>
                    <strong>
                      {room.roomNumber} · {room.roomName}
                    </strong>
                    <small>
                      {pick(language, {
                        ja: `定員${room.capacity}名`,
                        zh: `最多${room.capacity}人`,
                        en: `Up to ${room.capacity}`,
                      })}
                    </small>
                  </span>
                </label>
              );
            })}
          </div>
          {!availableRooms.length ? (
            <p className="form-status">
              {pick(language, {
                ja: "この日程で選べる客室がありません。",
                zh: "该日期暂无可选房间。",
                en: "No rooms are available for these dates.",
              })}
            </p>
          ) : null}
          <div className="selected-room-details">
            {selections.map((selection) => {
              const room = availableRooms.find((item) => item.id === selection.roomId);
              if (!room) return null;
              return (
                <article key={selection.roomId} className="selected-room-card">
                  <div className="selected-room-head">
                    <h3>
                      {room.roomNumber} · {room.roomName}
                    </h3>
                    <label>
                      {pick(language, { ja: "人数", zh: "人数", en: "Guests" })}
                      <select
                        value={selection.guestCount}
                        onChange={(event) => setGuestCount(room.id, Number(event.target.value))}
                      >
                        {Array.from({ length: room.capacity }, (_, index) => (
                          <option key={index + 1}>{index + 1}</option>
                        ))}
                      </select>
                    </label>
                  </div>
                  <div className="guest-assignment-list">
                    {selection.guests.map((guest, guestIndex) => (
                      <div className="guest-assignment" key={guestIndex}>
                        <label>
                          {pick(language, {
                            ja: `宿泊者${guestIndex + 1} 氏名`,
                            zh: `入住者${guestIndex + 1} 姓名`,
                            en: `Guest ${guestIndex + 1} name`,
                          })}
                          <input
                            value={guest.name}
                            onChange={(event) => setGuest(room.id, guestIndex, "name", event.target.value)}
                            required
                          />
                        </label>
                        <label>
                          {pick(language, { ja: "区分", zh: "类型", en: "Type" })}
                          <select
                            value={guest.category}
                            onChange={(event) => setGuest(room.id, guestIndex, "category", event.target.value)}
                          >
                            <option value="adult">{pick(language, { ja: "大人", zh: "成人", en: "Adult" })}</option>
                            <option value="child">{pick(language, { ja: "子ども", zh: "儿童", en: "Child" })}</option>
                          </select>
                        </label>
                        {guest.category === "child" ? (
                          <label>
                            {pick(language, { ja: "年齢", zh: "年龄", en: "Age" })}
                            <input
                              type="number"
                              min="0"
                              max="17"
                              value={guest.age}
                              onChange={(event) => setGuest(room.id, guestIndex, "age", event.target.value)}
                              required
                            />
                          </label>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </article>
              );
            })}
          </div>
        </section>
        <section>
          <h2>
            <span>03</span>
            {pick(language, { ja: "代表者情報", zh: "联系人信息", en: "Lead guest" })}
          </h2>
          <div className="form-grid two">
            <label>
              {pick(language, { ja: "氏名", zh: "姓名", en: "Full name" })}
              <input name="name" value={form.name} onChange={update} required />
            </label>
            <label>
              {pick(language, { ja: "メール", zh: "邮箱", en: "Email" })}
              <input name="email" type="email" value={form.email} onChange={update} required />
            </label>
            <label>
              {pick(language, { ja: "電話番号", zh: "电话号码", en: "Phone" })}
              <input name="phone" value={form.phone} onChange={update} required />
            </label>
            <label>
              {pick(language, { ja: "国・地域", zh: "国家或地区", en: "Country / region" })}
              <input name="country" value={form.country} onChange={update} required />
            </label>
            <label className="full">
              {pick(language, { ja: "ご要望", zh: "备注与需求", en: "Notes" })}
              <textarea name="notes" rows="4" value={form.notes} onChange={update} />
            </label>
          </div>
        </section>
        {quote ? (
          <section className="booking-quote">
            <h2>
              <span>04</span>
              {pick(language, { ja: "料金明細", zh: "价格明细", en: "Price details" })}
            </h2>
            {quote.rooms.map((room) => (
              <div className="quote-room" key={room.roomId}>
                <strong>
                  {room.roomNumber} · {room.roomName}
                </strong>
                <ul>
                  {room.nights.map((night) => (
                    <li key={night.date}>
                      <span>
                        {night.date} · ¥{Number(night.pricePerPerson).toLocaleString()} × {night.guestCount}
                      </span>
                      <b>¥{Number(night.amount).toLocaleString()}</b>
                    </li>
                  ))}
                </ul>
                <p>
                  <span>{pick(language, { ja: "客室小計", zh: "房间小计", en: "Room subtotal" })}</span>
                  <strong>¥{Number(room.totalAmount).toLocaleString()}</strong>
                </p>
              </div>
            ))}
            <div className="quote-total">
              <span>{pick(language, { ja: "合計", zh: "总金额", en: "Total" })}</span>
              <strong>¥{Number(quote.totalAmount).toLocaleString()}</strong>
            </div>
          </section>
        ) : null}
        <label className="consent">
          <input type="checkbox" name="consent" checked={form.consent} onChange={update} required />
          <span>
            {pick(language, {
              ja: "入力内容は予約リクエスト確認のために使用されることに同意します。",
              zh: "同意将填写内容用于确认预约申请。",
              en: "I agree that these details may be used to review my reservation request.",
            })}
          </span>
        </label>
        <TurnstileField configuration={configuration} onToken={setTurnstileToken} language={language} />
        {state.error ? (
          <div className="form-error" role="alert">
            {state.error}
          </div>
        ) : null}
        <button className="button coral submit-request" type="submit">
          {state.loading
            ? pick(language, { ja: "送信中…", zh: "正在提交…", en: "Submitting…" })
            : pick(language, { ja: "24時間保留して申請", zh: "锁定24小时并提交", en: "Hold for 24 hours and submit" })}
          <ArrowRight />
        </button>
      </form>
    </main>
  );
}

function TurnstileField({ configuration, onToken, language }) {
  const ref = useRef(null);
  useEffect(() => {
    if (!configuration.turnstileRequired || !configuration.turnstileSiteKey || !ref.current) return undefined;
    const render = () => {
      if (!window.turnstile || !ref.current || ref.current.childNodes.length) return;
      window.turnstile.render(ref.current, { sitekey: configuration.turnstileSiteKey, callback: onToken });
    };
    let script = document.querySelector('script[data-turnstile="true"]');
    if (!script) {
      script = document.createElement("script");
      script.src = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
      script.async = true;
      script.dataset.turnstile = "true";
      document.head.appendChild(script);
    }
    script.addEventListener("load", render);
    render();
    return () => script.removeEventListener("load", render);
  }, [configuration, onToken]);
  if (!configuration.turnstileRequired) return null;
  return (
    <div className="turnstile-field">
      <p>{pick(language, { ja: "防机器人確認", zh: "防机器人验证", en: "Security check" })}</p>
      <div ref={ref} />
    </div>
  );
}

const bookingStatuses = {
  pending: { ja: "確認待ち", zh: "等待确认", en: "Pending approval" },
  confirmed: { ja: "予約確定", zh: "预约已确认", en: "Confirmed" },
  rejected: { ja: "申請却下", zh: "申请已拒绝", en: "Rejected" },
  expired: { ja: "保留期限切れ", zh: "保留已过期", en: "Hold expired" },
  cancelled: { ja: "キャンセル済み", zh: "已取消", en: "Cancelled" },
};

function bookingErrorMessage(language, error) {
  if (error?.message === "LOOKUP_RATE_LIMITED") {
    return pick(language, {
      ja: "確認回数が上限に達しました。15分後にもう一度お試しください。",
      zh: "查询次数过多，请在15分钟后重试。",
      en: "Too many attempts. Please try again in 15 minutes.",
    });
  }
  if (error?.message === "LOOKUP_FAILED") {
    return pick(language, {
      ja: "申請番号またはメールアドレスを確認できませんでした。入力内容をご確認ください。",
      zh: "无法确认预约编号或邮箱，请检查输入内容后重试。",
      en: "We could not verify that request number and email. Please check your details.",
    });
  }
  return pick(language, {
    ja: "処理を完了できませんでした。時間をおいてもう一度お試しください。",
    zh: "暂时无法完成操作，请稍后重试。",
    en: "We could not complete that request. Please try again shortly.",
  });
}

function LocalDateTime({ value, language }) {
  if (!value) return null;
  const locale = language === "zh" ? "zh-CN" : language === "en" ? "en" : "ja-JP";
  return <>{new Date(value).toLocaleString(locale)}</>;
}

function FinalCancellationConfirm({ language, requestNo, onBack, onConfirm, loading }) {
  const backButton = useRef(null);
  useEffect(() => {
    backButton.current?.focus();
  }, []);
  return (
    <div className="confirmation-overlay" onKeyDown={(event) => event.key === "Escape" && !loading && onBack()}>
      <section
        className="final-confirmation"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="final-cancellation-title"
        aria-describedby="final-cancellation-description"
      >
        <p>FINAL CHECK</p>
        <h2 id="final-cancellation-title">
          {pick(language, {
            ja: "予約全体をキャンセルしますか？",
            zh: "确定取消整笔预约吗？",
            en: "Cancel the entire booking?",
          })}
        </h2>
        <p id="final-cancellation-description">
          {pick(language, {
            ja: `${requestNo} に含まれるすべての客室をキャンセルします。この操作は取り消せません。`,
            zh: `预约 ${requestNo} 中的全部房间都会被取消，此操作无法撤销。`,
            en: `Every room in request ${requestNo} will be cancelled. This cannot be undone.`,
          })}
        </p>
        <div className="confirmation-actions">
          <button ref={backButton} type="button" className="button quiet-button" onClick={onBack} disabled={loading}>
            {pick(language, { ja: "予約を残す", zh: "保留预约", en: "Keep booking" })}
          </button>
          <button type="button" className="button coral" onClick={onConfirm} disabled={loading}>
            {loading
              ? pick(language, { ja: "処理中…", zh: "正在取消…", en: "Cancelling…" })
              : pick(language, { ja: "全室をキャンセル", zh: "取消全部房间", en: "Cancel all rooms" })}
          </button>
        </div>
      </section>
    </div>
  );
}

function CancellationControls({ language, requestNo, onConfirm }) {
  const [reason, setReason] = useState("");
  const [acknowledged, setAcknowledged] = useState(false);
  const [showFinalConfirm, setShowFinalConfirm] = useState(false);
  const [state, setState] = useState({ loading: false, error: "" });
  const proceed = (event) => {
    event.preventDefault();
    if (!reason.trim() || !acknowledged) return;
    setShowFinalConfirm(true);
  };
  const submit = async () => {
    setState({ loading: true, error: "" });
    try {
      await onConfirm(reason.trim());
      setShowFinalConfirm(false);
    } catch (error) {
      setShowFinalConfirm(false);
      setState({ loading: false, error: bookingErrorMessage(language, error) });
    }
  };
  return (
    <form className="cancellation-controls" onSubmit={proceed}>
      <h3>{pick(language, { ja: "予約全体をキャンセル", zh: "取消整笔预约", en: "Cancel entire booking" })}</h3>
      <label>
        {pick(language, { ja: "キャンセル理由", zh: "取消原因", en: "Reason for cancellation" })}
        <textarea
          rows="4"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          maxLength="1000"
          required
        />
      </label>
      <label className="consent cancellation-consent">
        <input
          type="checkbox"
          checked={acknowledged}
          onChange={(event) => setAcknowledged(event.target.checked)}
          required
        />
        <span>
          {pick(language, {
            ja: "この申請に含まれるすべての客室がキャンセルされることを確認しました。",
            zh: "我已确认本次申请中的全部房间都将被取消。",
            en: "I understand that every room in this request will be cancelled.",
          })}
        </span>
      </label>
      {state.error ? (
        <div className="form-error" role="alert">
          {state.error}
        </div>
      ) : null}
      <button className="button coral" type="submit" disabled={!reason.trim() || !acknowledged || state.loading}>
        {pick(language, { ja: "最終確認へ", zh: "进入最终确认", en: "Continue to final check" })}
        <ArrowRight />
      </button>
      {showFinalConfirm ? (
        <FinalCancellationConfirm
          language={language}
          requestNo={requestNo}
          onBack={() => setShowFinalConfirm(false)}
          onConfirm={submit}
          loading={state.loading}
        />
      ) : null}
    </form>
  );
}

function BookingDetails({ booking, language, onCancel }) {
  const status = String(booking.status || "").toLowerCase();
  return (
    <section className="booking-result" aria-live="polite">
      <div className="booking-result-heading">
        <div>
          <p>{booking.requestNo}</p>
          <h2>
            {booking.checkInDate} — {booking.checkOutDate}
          </h2>
        </div>
        <span className={`booking-status ${status}`}>
          {pick(language, bookingStatuses[status] || { ja: status, zh: status, en: status })}
        </span>
      </div>
      <dl className="booking-summary">
        <div>
          <dt>{pick(language, { ja: "宿泊人数", zh: "入住人数", en: "Guests" })}</dt>
          <dd>
            {booking.guestCount}{" "}
            {pick(language, { ja: "名", zh: "人", en: booking.guestCount === 1 ? "guest" : "guests" })}
          </dd>
        </div>
        <div>
          <dt>{pick(language, { ja: "合計金額", zh: "总金额", en: "Grand total" })}</dt>
          <dd>¥{Number(booking.totalAmount).toLocaleString()}</dd>
        </div>
        {booking.holdExpiresAt ? (
          <div>
            <dt>{pick(language, { ja: "客室保留期限", zh: "房间保留期限", en: "Room hold expires" })}</dt>
            <dd>
              <LocalDateTime value={booking.holdExpiresAt} language={language} />
            </dd>
          </div>
        ) : null}
      </dl>
      <div className="booking-room-list">
        <h3>{pick(language, { ja: "客室明細", zh: "房间明细", en: "Room details" })}</h3>
        {booking.rooms?.map((room) => (
          <article key={`${room.roomNumber}-${room.roomName}`}>
            <div>
              <Bed size={24} />
              <span>
                <strong>
                  {room.roomNumber} · {room.roomName}
                </strong>
                <small>
                  {room.guestCount}{" "}
                  {pick(language, { ja: "名", zh: "人", en: room.guestCount === 1 ? "guest" : "guests" })}
                </small>
              </span>
            </div>
            <strong>¥{Number(room.totalAmount).toLocaleString()}</strong>
          </article>
        ))}
      </div>
      {booking.rejectionReason ? (
        <div className="booking-message rejection-message">
          <strong>{pick(language, { ja: "申請却下の理由", zh: "拒绝原因", en: "Reason for rejection" })}</strong>
          <p>{booking.rejectionReason}</p>
        </div>
      ) : null}
      {booking.cancellationReason ? (
        <div className="booking-message">
          <strong>{pick(language, { ja: "キャンセル理由", zh: "取消原因", en: "Cancellation reason" })}</strong>
          <p>{booking.cancellationReason}</p>
        </div>
      ) : null}
      {booking.cancellable ? (
        <CancellationControls language={language} requestNo={booking.requestNo} onConfirm={onCancel} />
      ) : status === "pending" || status === "confirmed" ? (
        <p className="cancellation-closed">
          {pick(language, {
            ja: `オンライン取消はチェックイン${booking.cancellationCutoffHours || 72}時間前までです。宿泊施設へご連絡ください。`,
            zh: `在线取消仅限入住前${booking.cancellationCutoffHours || 72}小时以前，之后请联系住宿方。`,
            en: `Online cancellation closes ${booking.cancellationCutoffHours || 72} hours before check-in. Please contact the property after that.`,
          })}
        </p>
      ) : null}
    </section>
  );
}

function BookingLookupPage({ language }) {
  const [credentials, setCredentials] = useState({ requestNo: "", email: "" });
  const [verifiedCredentials, setVerifiedCredentials] = useState(null);
  const [booking, setBooking] = useState(null);
  const [state, setState] = useState({ loading: false, error: "" });
  const update = (event) => {
    setCredentials((value) => ({ ...value, [event.target.name]: event.target.value }));
    setVerifiedCredentials(null);
    setBooking(null);
    setState((value) => ({ ...value, error: "" }));
  };
  const lookup = async (event) => {
    event?.preventDefault();
    const submittedCredentials = {
      requestNo: credentials.requestNo.trim(),
      email: credentials.email.trim(),
    };
    setState({ loading: true, error: "" });
    setBooking(null);
    try {
      const response = await guestApi("/api/stay/bookings/lookup", {
        method: "POST",
        body: JSON.stringify(submittedCredentials),
      });
      setVerifiedCredentials(submittedCredentials);
      setBooking(response);
      setState({ loading: false, error: "" });
    } catch (error) {
      setState({ loading: false, error: bookingErrorMessage(language, error) });
    }
  };
  const cancel = async (reason) => {
    if (!verifiedCredentials) throw new Error("LOOKUP_FAILED");
    const response = await guestApi("/api/stay/bookings/lookup/cancel", {
      method: "POST",
      body: JSON.stringify({
        ...verifiedCredentials,
        reason,
        confirmed: true,
      }),
    });
    setBooking(response);
  };
  return (
    <main className="booking-lookup-page">
      <PageIntro
        eyebrow="MY BOOKING"
        title={pick(language, { ja: "予約申請を確認する", zh: "查询你的预约", en: "Check your booking" })}
        description={pick(language, {
          ja: "申請番号と予約時のメールアドレスで、現在の状況や客室明細を確認できます。",
          zh: "使用申请编号和预约时填写的邮箱，查看当前状态与房间明细。",
          en: "Use your request number and booking email to see the current status and room details.",
        })}
      />
      <section className="booking-lookup-shell">
        <form className="booking-lookup-form" onSubmit={lookup}>
          <div>
            <p>FIND YOUR REQUEST</p>
            <h2>{pick(language, { ja: "申請情報を入力", zh: "输入预约信息", en: "Enter your booking details" })}</h2>
          </div>
          <div className="form-grid two">
            <label>
              {pick(language, { ja: "予約申請番号", zh: "预约申请编号", en: "Request number" })}
              <input
                name="requestNo"
                value={credentials.requestNo}
                onChange={update}
                placeholder="BR00000001"
                autoComplete="off"
                spellCheck="false"
                maxLength="18"
                disabled={state.loading}
                required
              />
            </label>
            <label>
              {pick(language, { ja: "予約時のメール", zh: "预约时填写的邮箱", en: "Booking email" })}
              <input
                name="email"
                type="email"
                value={credentials.email}
                onChange={update}
                autoComplete="email"
                maxLength="255"
                disabled={state.loading}
                required
              />
            </label>
          </div>
          <p className="lookup-privacy-note">
            <ShieldCheck size={20} />
            {pick(language, {
              ja: "安全のため、申請番号とメールアドレスの両方が一致した場合のみ表示します。",
              zh: "为保护隐私，仅在申请编号和邮箱同时匹配时显示预约信息。",
              en: "For your privacy, details appear only when both entries match.",
            })}
          </p>
          {state.error ? (
            <div className="form-error" role="alert">
              {state.error}
            </div>
          ) : null}
          <button className="button coral" type="submit" disabled={state.loading}>
            <MagnifyingGlass size={20} weight="bold" />
            {state.loading
              ? pick(language, { ja: "確認中…", zh: "正在查询…", en: "Checking…" })
              : pick(language, { ja: "予約を確認", zh: "查询预约", en: "Check booking" })}
          </button>
        </form>
        {booking ? <BookingDetails booking={booking} language={language} onCancel={cancel} /> : null}
      </section>
    </main>
  );
}

function CancellationPage({ language }) {
  const token = useMemo(() => new URLSearchParams(window.location.search).get("token") || "", []);
  const [booking, setBooking] = useState(null);
  const [state, setState] = useState({ loading: true, error: "", cancelled: false });
  useEffect(() => {
    guestApi(`/api/stay/bookings/cancel/${encodeURIComponent(token)}`)
      .then((value) => {
        setBooking(value);
        setState({ loading: false, error: "", cancelled: false });
      })
      .catch((error) => setState({ loading: false, error: bookingErrorMessage(language, error), cancelled: false }));
  }, [language, token]);
  const cancel = async (reason) => {
    setState((value) => ({ ...value, loading: true, error: "" }));
    try {
      await guestApi(`/api/stay/bookings/cancel/${encodeURIComponent(token)}`, {
        method: "POST",
        body: JSON.stringify({ reason, confirmed: true }),
      });
      setState({ loading: false, error: "", cancelled: true });
    } catch (error) {
      setState((value) => ({ ...value, loading: false }));
      throw error;
    }
  };
  return (
    <main className="cancellation-page">
      <PageIntro
        eyebrow="CANCELLATION"
        title={pick(language, { ja: "予約をキャンセル", zh: "取消预约", en: "Cancel your booking" })}
        description={pick(language, {
          ja: "チェックイン72時間前まで、この安全なリンクから取消できます。",
          zh: "入住72小时前可以通过此安全链接自行取消。",
          en: "Use this secure link to cancel up to 72 hours before check-in.",
        })}
      />
      <section className="cancellation-card">
        {state.loading ? <p>{pick(language, { ja: "確認中…", zh: "正在查询…", en: "Checking…" })}</p> : null}
        {state.error ? (
          <div className="form-error" role="alert">
            {state.error}
          </div>
        ) : null}
        {state.cancelled ? (
          <>
            <CheckCircle size={52} weight="fill" />
            <h2>{pick(language, { ja: "キャンセルしました", zh: "预约已取消", en: "Booking cancelled" })}</h2>
          </>
        ) : null}
        {booking && !state.cancelled ? (
          <>
            <p>{booking.requestNo}</p>
            <h2>
              {booking.checkInDate} — {booking.checkOutDate}
            </h2>
            <strong>¥{Number(booking.totalAmount).toLocaleString()}</strong>
            {booking.cancellable ? (
              <CancellationControls language={language} requestNo={booking.requestNo} onConfirm={cancel} />
            ) : (
              <p>
                {pick(language, {
                  ja: "オンライン取消期限を過ぎています。宿泊施設へご連絡ください。",
                  zh: "已超过在线取消期限，请联系住宿方。",
                  en: "The online cancellation deadline has passed. Please contact the property.",
                })}
              </p>
            )}
          </>
        ) : null}
      </section>
    </main>
  );
}

function CallToAction({ language }) {
  return (
    <section className="guest-cta">
      <p>READY FOR HAKUBA?</p>
      <h2>
        {pick(language, {
          ja: "次の雪旅を、白馬樹海から。",
          zh: "下一次雪地旅行，从白马树海开始。",
          en: "Start your next snow trip at Hakuba Jukai.",
        })}
      </h2>
      <a className="button coral" href={url("/stay/reserve")}>
        {pick(language, { ja: "宿泊を計画する", zh: "规划住宿", en: "Plan your stay" })}
        <ArrowRight />
      </a>
    </section>
  );
}

function SiteFooter({ language }) {
  return (
    <footer className="guest-footer">
      <a className="guest-brand footer-brand" href={url("/stay")}>
        <strong>白馬樹海</strong>
        <span>HAKUBA JUKAI</span>
      </a>
      <div>
        <p>
          {pick(language, {
            ja: "長野県白馬村（詳細住所：待設定）",
            zh: "日本长野县白马村（详细地址：待设置）",
            en: "Hakuba, Nagano (full address: to be configured)",
          })}
        </p>
        <p>
          {pick(language, {
            ja: "電話・メール：待設定",
            zh: "电话与邮箱：待设置",
            en: "Phone and email: to be configured",
          })}
        </p>
      </div>
      <small>© 2026 HAKUBA JUKAI · PHASE 2 LIVE BOOKING</small>
    </footer>
  );
}

function GuestApp() {
  const [language, setLanguage] = useLanguage();
  const route = window.location.pathname.slice(contextPath.length) || "/stay";
  let page;
  if (route === "/stay/rooms") page = <RoomsPage language={language} />;
  else if (route === "/stay/rates") page = <RatesPage language={language} />;
  else if (route === "/stay/guide") page = <GuidePage language={language} />;
  else if (route === "/stay/access") page = <AccessPage language={language} />;
  else if (route === "/stay/ski") page = <SkiPage language={language} />;
  else if (route === "/stay/reserve") page = <ReservePage language={language} />;
  else if (route === "/stay/booking") page = <BookingLookupPage language={language} />;
  else if (route === "/stay/cancel") page = <CancellationPage language={language} />;
  else page = <HomePage language={language} />;
  return (
    <>
      <a className="guest-skip" href="#guest-main">
        {pick(language, { ja: "本文へ", zh: "跳到正文", en: "Skip to content" })}
      </a>
      <SiteHeader language={language} setLanguage={setLanguage} route={route} />
      <div id="guest-main">{page}</div>
      <SiteFooter language={language} />
    </>
  );
}

createRoot(document.getElementById("guest-root")).render(<GuestApp />);
