import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";

const STORAGE_KEY = "minshuku-language";
const I18nContext = createContext(null);

// The original UI predates i18n. Keeping the translation catalogue here lets every legacy screen
// switch languages immediately while new components can use t() directly.
const JA_TO_ZH = {
  "予約、空室、支払い状況をひと目で確認できます。": "一目了然地查看订单、空房和收款状态。",
  "客室の基本情報、販売状況、清掃状態を管理します。": "管理客房基本信息、销售状态和清扫状态。",
  "客室ごとの期間料金と適用優先度を管理します。": "管理各客房的期间价格和应用优先级。",
  "新しい予約の登録と、宿泊・支払い・清掃状況を管理します。": "登记新订单并管理住宿、收款和清扫状态。",
  "空室・清掃済の部屋を選択してください": "请选择空房且已清扫的客房",
  有効な客室一覧: "有效客房列表",
  削除済み客室一覧: "已删除客房列表",
  "部屋を登録しました。": "客房已登记。",
  "宿泊日を入力してください。": "请输入住宿日期。",
  "部屋登録に失敗しました。部屋番号が重複していないか確認してください。": "客房登记失败，请确认房间号是否重复。",
  "電話番号は000-0000-0000の形式で入力してください。": "电话号码格式应为000-0000-0000。",
  "指定期間はすでに予約されています。": "该期间已经有订单。",
  "チェックアウト済み予約を削除しました。": "已删除退房订单。",
  "チェックアウト済み予約のみ清掃状態を更新できます。": "只有已退房订单可以更新清扫状态。",
  "チェックインしました。": "已办理入住。",
  "チェックアウトしました。": "已办理退房。",
  チェックアウト完了待清掃: "已退房待清扫",
  "管理者はすでに登録されています。": "管理员已经登记。",
  "ユーザー名またはパスワードが正しくありません。": "用户名或密码不正确。",
  "ユーザー名は3〜64文字の英数字、._-で入力してください。": "用户名请输入3至64位英数字或._-。",
  "パスワードは10文字以上で入力してください。": "密码至少输入10个字符。",
  "返金額は入金額を超えられません。": "退款金额不能超过实收金额。",
  "入金額を返金額より少なくできません。": "实收金额不能小于退款金额。",
  "支払方法が正しくありません。": "支付方式不正确。",
  "集計期間が正しくありません。": "统计期间不正确。",
  "チェックアウトデータがありません。": "没有退房数据。",
  "料金ルールを1件以上選択してください。": "请至少选择一条价格规则。",
  チェックアウト済み予約一覧: "已退房订单列表",
  取消済み予約一覧: "已取消订单列表",
  宿泊予約一覧: "住宿订单列表",
  宿泊予約帳: "住宿订单簿",
  直近の予約一覧: "近期订单列表",
  チェックアウト済み: "已退房",
  チェックアウト: "退房",
  チェックイン: "入住",
  "予約・入返金明細": "订单及收退款明细",
  月次営業集計: "月度营业汇总",
  支払方法集計: "支付方式汇总",
  "バックアップが完了しました。": "备份已完成。",
  "バックアップが中断されました。": "备份已中断。",
  バックアップ: "备份",
  保存先フォルダ: "保存文件夹",
  手動バックアップ: "立即备份",
  宿泊履歴: "住宿记录",
  顧客台帳: "客户档案",
  顧客管理: "客户管理",
  顧客を登録: "登记客户",
  顧客一覧: "客户列表",
  "営業・会計": "营业与收款",
  営業集計: "营业汇总",
  入返金管理: "收退款管理",
  Excel出力: "导出Excel",
  予約番号: "订单编号",
  部屋番号: "房间号",
  宿泊者名: "住客姓名",
  予約者: "预订人",
  同行者情報: "同行人员信息",
  同行者なし: "无同行人员",
  同行者: "同行人员",
  フリガナ未入力: "未填读音",
  性別未入力: "未填性别",
  年齢未入力: "未填年龄",
  電話未入力: "未填电话",
  メール未入力: "未填邮箱",
  情報なし: "无信息",
  新規予約: "新建订单",
  予約登録: "登记订单",
  予約管理: "订单管理",
  予約一覧: "订单列表",
  予約形式: "预订方式",
  予約状態: "订单状态",
  有効予約: "有效订单",
  取消済み予約: "已取消订单",
  "予約データがありません。": "没有订单数据。",
  "予約をキャンセルしました。": "订单已取消。",
  "予約を登録しました。": "订单已登记。",
  "予約を更新しました。": "订单已更新。",
  客室を登録: "登记客房",
  "客室を登録しました。": "客房已登记。",
  客室一覧: "客房列表",
  客室台帳: "客房台账",
  客室管理: "客房管理",
  客室: "客房",
  部屋: "客房",
  削除済み客室: "已删除客房",
  "削除済み部屋はありません。": "没有已删除的客房。",
  "部屋データがありません。": "没有客房数据。",
  部屋タイプ: "客房类型",
  部屋名: "客房名称",
  全客室: "全部客房",
  販売可能: "可销售",
  季節の料金帳: "季节价格表",
  料金ルールを登録: "登记价格规则",
  料金ルール一覧: "价格规则列表",
  "料金ルールがありません。": "没有价格规则。",
  料金設定: "价格设置",
  料金を登録: "登记价格",
  一人料金: "每人价格",
  基本料金: "基础价格",
  本日の帳場: "今日前台",
  宿泊状況: "住宿概况",
  直近のご予約: "近期订单",
  すべて見る: "查看全部",
  営業中: "营业中",
  管理画面サイドバー: "管理菜单",
  主要メニュー: "主菜单",
  ホーム: "首页",
  本文へ移動: "跳到正文",
  "読み込み中...": "加载中...",
  選択してください: "请选择",
  選択削除: "删除所选",
  完全削除: "彻底删除",
  状態更新: "更新状态",
  "支払い状況を更新しました。": "收款状态已更新。",
  支払済: "已收款",
  未払い: "未收款",
  部分返金: "部分退款",
  一部返金: "部分退款",
  全額返金: "全额退款",
  支払い: "收款",
  支払方法: "支付方式",
  売上予定: "应收金额",
  入金額: "实收金额",
  返金額: "退款金额",
  実収入: "净收入",
  現金: "现金",
  カード: "银行卡",
  振込: "转账",
  予約サイト: "平台收款",
  "清掃状態を更新しました。": "清扫状态已更新。",
  清掃待ち: "待清扫",
  清掃済: "已清扫",
  清掃: "清扫",
  予約済: "已预订",
  滞在中: "住宿中",
  空室: "空房",
  専用バス付き: "带独立浴室",
  ファミリー: "家庭房",
  スイート: "套房",
  洋室: "西式房",
  和室: "日式房",
  開始日: "开始日",
  終了日: "结束日",
  優先度: "优先级",
  ルール名: "规则名称",
  最終更新: "最后更新",
  新規顧客: "新建客户",
  顧客名: "客户姓名",
  連絡先: "联系方式",
  検索: "搜索",
  編集: "编辑",
  保存: "保存",
  戻る: "返回",
  閉じる: "关闭",
  ログアウト: "退出登录",
  ログイン: "登录",
  初期設定: "初始设置",
  管理者を登録: "登记管理员",
  ユーザー名: "用户名",
  パスワード: "密码",
  日本語: "日语",
  中国語: "中文",
  電話なし: "无电话",
  メールなし: "无邮箱",
  フリガナ: "读音",
  メール: "邮箱",
  電話: "电话",
  性別: "性别",
  男性: "男",
  女性: "女",
  未回答: "未回答",
  年齢: "年龄",
  人数: "人数",
  定員: "定员",
  金額: "金额",
  日程: "日期",
  名前: "名称",
  氏名: "姓名",
  メモ: "备注",
  状態: "状态",
  操作: "操作",
  削除: "删除",
  取消: "取消",
  復元: "恢复",
  更新: "更新",
  登録: "登记",
  有効: "有效",
  無効: "无效",
  必須: "必填",
  前へ: "上一页",
  次へ: "下一页",
  現在: "当前",
  公式: "官网",
  現地: "现场",
  その他: "其他",
  番号: "编号",
  タイプ: "类型",
  料金: "价格",
  一覧: "列表",
  件表示: "条显示",
};

const TRANSLATIONS = Object.entries(JA_TO_ZH).sort((left, right) => right[0].length - left[0].length);

export function translateJapanese(value) {
  if (typeof value !== "string") return value;
  const translated = TRANSLATIONS.reduce((result, [japanese, chinese]) => result.split(japanese).join(chinese), value);
  // Translate the Japanese people counter only after a number; never alter a person's name.
  return translated.replace(/(\d+)名/g, "$1人").replace(/(\d+)件/g, "$1条");
}

export function I18nProvider({ children }) {
  const [language, setLanguageState] = useState(() => localStorage.getItem(STORAGE_KEY) || "ja");
  const value = useMemo(
    () => ({
      language,
      setLanguage(next) {
        const safeLanguage = next === "zh" ? "zh" : "ja";
        localStorage.setItem(STORAGE_KEY, safeLanguage);
        setLanguageState(safeLanguage);
      },
      t(japanese, chinese) {
        return language === "zh" ? chinese || translateJapanese(japanese) : japanese;
      },
    }),
    [language]
  );

  useEffect(() => {
    document.documentElement.lang = language === "zh" ? "zh-CN" : "ja";
  }, [language]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const value = useContext(I18nContext);
  if (!value) throw new Error("I18nProvider is missing.");
  return value;
}

export function LanguageToggle({ compact = false }) {
  const { language, setLanguage, t } = useI18n();
  return (
    <div className={`language-toggle ${compact ? "compact" : ""}`} aria-label={t("表示言語", "显示语言")}>
      <button type="button" className={language === "ja" ? "active" : ""} onClick={() => setLanguage("ja")}>
        日本語
      </button>
      <button type="button" className={language === "zh" ? "active" : ""} onClick={() => setLanguage("zh")}>
        中文
      </button>
    </div>
  );
}

export function TranslationBoundary({ children }) {
  const { language } = useI18n();
  const rootRef = useRef(null);
  const textState = useRef(new WeakMap());
  const attributeState = useRef(new WeakMap());

  useEffect(() => {
    const root = rootRef.current;
    if (!root) return undefined;

    const localizeText = (node) => {
      const current = node.nodeValue || "";
      let state = textState.current.get(node);
      if (!state || current !== state.last) state = { source: current, last: current };
      const next = language === "zh" ? translateJapanese(state.source) : state.source;
      state.last = next;
      textState.current.set(node, state);
      if (current !== next) node.nodeValue = next;
    };

    const localizeAttributes = (element) => {
      const names = ["aria-label", "placeholder", "title"];
      let states = attributeState.current.get(element) || {};
      names.forEach((name) => {
        if (!element.hasAttribute(name)) return;
        const current = element.getAttribute(name) || "";
        let state = states[name];
        if (!state || current !== state.last) state = { source: current, last: current };
        const next = language === "zh" ? translateJapanese(state.source) : state.source;
        state.last = next;
        states[name] = state;
        if (current !== next) element.setAttribute(name, next);
      });
      attributeState.current.set(element, states);
    };

    const localize = (node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        localizeText(node);
        return;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) return;
      localizeAttributes(node);
      node.childNodes.forEach(localize);
    };

    localize(root);
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === "characterData") localizeText(mutation.target);
        mutation.addedNodes.forEach(localize);
        if (mutation.type === "attributes") localizeAttributes(mutation.target);
      });
    });
    observer.observe(root, { subtree: true, childList: true, characterData: true, attributes: true });
    return () => observer.disconnect();
  }, [language]);

  return <div ref={rootRef}>{children}</div>;
}
