package com.example.minshuku.controller; // 宣言仪テーブル盘コントローラー所属パッケージ。

import com.example.minshuku.service.ReservationService; // 読み込み予約業務サービス。
import com.example.minshuku.service.RoomService; // 読み込み部屋業務サービス。
import org.springframework.stereotype.Controller; // 読み込み Spring MVC ページコントローラーアノテーション。
import org.springframework.ui.Model; // 読み込みページモデルオブジェクト。
import org.springframework.web.bind.annotation.GetMapping; // 読み込み GET ルーティングアノテーション。
import org.springframework.web.bind.annotation.RequestParam; // 読み込みリクエストパラメータ绑定アノテーション。

@Controller // 標记このクラス処理 Thymeleaf ページリクエスト。
public class DashboardController { // 定義システムトップページコントローラー。
  private final RoomService roomService; // 保存部屋業務サービス依赖。
  private final ReservationService reservationService; // 保存予約業務サービス依赖。

  public DashboardController(RoomService roomService, ReservationService reservationService) { // 定義構築メソッド用依赖注入。
    this.roomService = roomService; // 保存注入の部屋業務サービス。
    this.reservationService = reservationService; // 保存注入の予約業務サービス。
  }

  @GetMapping({"/", "/dashboard"}) // を根パスと仪テーブル盘パス映射へ同一个ページ。
  public String dashboard(@RequestParam(defaultValue = "1") int page, Model model) { // 定義仪テーブル盘ページ処理メソッド。
    int safePage = Math.max(1, page); // 兜底ページ番号至少に 1。
    int pageSize = 5; // 定義一页最多表示 5 条予約。
    reservationService.syncDueCheckouts(); // 先同期へ期チェックアウト状態，保证トップページ一覧与部屋状態一致。
    model.addAttribute("roomCount", roomService.countAll()); // 向ページ传递部屋総数。
    model.addAttribute("vacantCount", roomService.countVacant()); // 向ページ传递空室件数。
    model.addAttribute("bookedCount", reservationService.countBooked()); // 向ページ传递有効予約件数。
    model.addAttribute("recentReservations", reservationService.findRecentPage(safePage, pageSize)); // 向ページ传递近期予約ページング一覧。
    model.addAttribute("reservationPage", safePage); // 向ページ传递現在ページ番号。
    model.addAttribute("reservationTotalPages", Math.max(1, (reservationService.countRecent() + pageSize - 1) / pageSize)); // 向ページ传递总页数。
    return "dashboard"; // 返却 dashboard.html 模板。
  }
}
