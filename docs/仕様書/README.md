# 民宿管理システム 設計書案内

現行コードを基準にした最新版は v1.9（2026-07-13）です。正式ファイルは本フォルダへ同期し、版付き成果物は `outputs/20260713-spec-v1.9` に保存しています。

## 正式ファイル

- `民宿管理システム.xlsx`
- `民宿管理システム_フロントエンド設計書.xlsx`

## v1.9 の主な更新

- 予約中・取消済み・チェックアウト済みの3一覧を、各5件・独立ページ・総件数付きのサーバーページング仕様へ更新
- `GET /api/dashboard` と `GET /api/reservations` を読取専用とし、期限到来チェックアウトは `CheckoutScheduler` が毎時実行する構成を明記
- `PageResponse` の `items / page / totalPages / totalCount`、ページ範囲補正、0件時 `totalPages=1` を API・DTO・テストへ反映
- 和モダン旅館管理 UI、236px固定サイドバー、PageHeader、NavIcon、4段階レスポンシブ、表の固有 caption、skip link、live region、focus 表示を前端設計へ反映
- ブランド表示は「樹」徽記と「白馬樹海」のみとし、副題を置かない現行仕様へ更新
- Flyway、H2 PostgreSQL compatibility mode テスト、Docker、CI、Actuator、CSP、バックアップ、アプリ内認証なしの運用境界を現状どおり記載
- Maven 106件成功、npm format・lint・build 成功の確認結果を反映
- 全14シートで列幅、折返し、行高、固定枠を統一し、長文をセル内で確認できるレイアウトへ調整

## シート構成

両ブックは、表紙、変更履歴、概要、業務要件、機能一覧、コード行別処理仕様、画面一覧、DB設計、API一覧、非機能要件、テスト仕様、スケジュール、参考資料に加え、後端は「運用・基盤仕様」、前端は「品質・配備仕様」を収録しています。

## 前提

- 対象 URL は `http://localhost:8000/jukai-internal` です。
- 現行の Spring Boot、React、MyBatis、PostgreSQL 実装を優先しています。
- アプリ内の認証・認可機能は設けず、社内ネットワークまたはリバースプロキシで利用範囲を制御する前提です。
- 現行 CI の DB 連携テストは H2 PostgreSQL compatibility mode です。PostgreSQL 実機と Flyway を使う専用テストは残課題として明記しています。
