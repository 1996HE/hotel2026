package com.example.minshuku.controller; // 宣言予約コントローラー所属パッケージ。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ型。
import com.example.minshuku.service.ReservationService; // 読み込み予約業務サービス。
import com.example.minshuku.service.RoomService; // 読み込み部屋業務サービス。
import java.util.List; // 読み込み同行者フォーム数组使用の一覧型。
import org.springframework.stereotype.Controller; // 読み込み Spring MVC ページコントローラーアノテーション。
import org.springframework.ui.Model; // 読み込みページモデルオブジェクト。
import org.springframework.web.bind.annotation.GetMapping; // 読み込み GET ルーティングアノテーション。
import org.springframework.web.bind.annotation.ModelAttribute; // 読み込みフォームオブジェクト绑定アノテーション。
import org.springframework.web.bind.annotation.PathVariable; // 読み込みパス変数绑定アノテーション。
import org.springframework.web.bind.annotation.PostMapping; // 読み込み POST ルーティングアノテーション。
import org.springframework.web.bind.annotation.RequestParam; // 読み込みリクエストパラメータ绑定アノテーション。
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 読み込みリダイレクト临时メッセージオブジェクト。

@Controller // 標记このクラス処理 Thymeleaf ページリクエスト。
public class ReservationController { // 定義予約管理コントローラー。
  private final ReservationService reservationService; // 保存予約業務サービス依赖。
  private final RoomService roomService; // 保存部屋業務サービス依赖。

  public ReservationController(ReservationService reservationService, RoomService roomService) { // 定義構築メソッド用依赖注入。
    this.reservationService = reservationService; // 保存注入の予約業務サービス。
    this.roomService = roomService; // 保存注入の部屋業務サービス。
  }

  @GetMapping("/reservations") // を予約管理パス映射へページ処理メソッド。
  public String reservations(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "1") int cancelledPage, @RequestParam(defaultValue = "1") int checkedOutPage, Model model) { // 定義予約管理ページ処理メソッド。
    int safePage = Math.max(1, page); // 兜底有効予約ページ番号。
    int safeCancelledPage = Math.max(1, cancelledPage); // 兜底取消予約ページ番号。
    int safeCheckedOutPage = Math.max(1, checkedOutPage); // 兜底チェックアウト予約ページ番号。
    int pageSize = 5; // 定義一页表示の最大条数。
    reservationService.syncDueCheckouts(); // 先同期へ期チェックアウト状態。
    model.addAttribute("reservations", reservationService.findRecentPage(safePage, pageSize)); // 向ページ传递ページング後の近期予約一覧。
    model.addAttribute("cancelledReservations", reservationService.findCancelledPage(safeCancelledPage, pageSize)); // 向ページ传递ページング後の取消予約一覧。
    model.addAttribute("checkedOutReservations", reservationService.findCheckedOutPage(safeCheckedOutPage, pageSize)); // 向ページ传递ページング後のチェックアウト予約一覧。
    model.addAttribute("reservationPage", safePage); // 向ページ传递現在有効予約ページ番号。
    model.addAttribute("reservationTotalPages", Math.max(1, (reservationService.countRecent() + pageSize - 1) / pageSize)); // 向ページ传递有効予約总页数。
    model.addAttribute("cancelledPage", safeCancelledPage); // 向ページ传递現在取消予約ページ番号。
    model.addAttribute("cancelledTotalPages", Math.max(1, (reservationService.countCancelled() + pageSize - 1) / pageSize)); // 向ページ传递取消予約总页数。
    model.addAttribute("checkedOutPage", safeCheckedOutPage); // 向ページ传递現在チェックアウト予約ページ番号。
    model.addAttribute("checkedOutTotalPages", Math.max(1, (reservationService.countCheckedOut() + pageSize - 1) / pageSize)); // 向ページ传递チェックアウト予約总页数。
    model.addAttribute("rooms", roomService.findBookable()); // 向ページ传递空室且可能予約の部屋一覧。
    model.addAttribute("reservation", new Reservation()); // 向ページ传递新規登録予約フォームオブジェクト。
    return "reservations"; // 返却 reservations.html 模板。
  }

  @PostMapping("/reservations") // を新規登録予約フォーム送信パス映射へ処理メソッド。
  public String create(@ModelAttribute Reservation reservation, @RequestParam(required = false, defaultValue = "false") boolean noPhoneInfo, @RequestParam(required = false, defaultValue = "false") boolean noEmailInfo, @RequestParam(required = false) List<String> companionNames, @RequestParam(required = false) List<String> companionKanas, @RequestParam(required = false) List<String> companionGenders, @RequestParam(required = false) List<Integer> companionAges, @RequestParam(required = false) List<String> companionPhones, RedirectAttributes redirectAttributes) { // 定義新規登録予約処理メソッド。
  try { // 捕获業務検証例外と转成ページメッセージ。
      reservationService.create(reservation, noPhoneInfo && noEmailInfo, companionNames, companionKanas, companionGenders, companionAges, companionPhones); // 呼び出し業務サービス新規登録予約と同行者。
      redirectAttributes.addFlashAttribute("message", "予約を登録しました。"); // 設定新規登録成功メッセージ。
    } catch (IllegalArgumentException ex) { // 捕获フォーム検証例外。
      redirectAttributes.addFlashAttribute("error", ex.getMessage()); // 設定エラーメッセージメッセージ。
    }
    return "redirect:/reservations"; // リダイレクト回予約管理页避免重複送信。
  }

  @PostMapping("/reservations/{id}/payment") // を支払い状態更新パス映射へ処理メソッド。
  public String updatePayment(@PathVariable Integer id, @RequestParam String paymentStatus, RedirectAttributes redirectAttributes) { // 定義支払い状態更新処理メソッド。
    reservationService.updatePaymentStatus(id, paymentStatus); // 呼び出し業務サービス更新支払い状態。
    redirectAttributes.addFlashAttribute("message", "支払い状況を更新しました。"); // 設定支払い状態更新成功メッセージ。
    return "redirect:/reservations"; // リダイレクト回予約管理页。
  }

  @PostMapping("/reservations/{id}/cancel") // を取消予約パス映射へ処理メソッド。
  public String cancel(@PathVariable Integer id, RedirectAttributes redirectAttributes) { // 定義取消予約処理メソッド。
    reservationService.cancel(id); // 呼び出し業務サービス取消予約。
    redirectAttributes.addFlashAttribute("message", "予約をキャンセルしました。"); // 設定取消成功メッセージ。
    return "redirect:/reservations"; // リダイレクト回予約管理页。
  }

  @PostMapping("/reservations/{id}/status") // を予約状態更新パス映射へ処理メソッド。
  public String updateStatus(@PathVariable Integer id, @RequestParam String reservationStatus, RedirectAttributes redirectAttributes) { // 定義予約状態更新処理メソッド。
    reservationService.updateReservationStatus(id, reservationStatus); // 呼び出し業務サービス更新予約状態。
    redirectAttributes.addFlashAttribute("message", "予約状態を更新しました。"); // 設定予約状態更新成功メッセージ。
    return "redirect:/reservations"; // リダイレクト回予約管理页。
  }

  @PostMapping("/reservations/{id}/cleaning") // を清掃状態更新パス映射へ処理メソッド。
  public String updateCleaning(@PathVariable Integer id, @RequestParam String cleaningStatus, RedirectAttributes redirectAttributes) { // 定義清掃状態更新処理メソッド。
    reservationService.updateCheckoutCleaningStatus(id, cleaningStatus); // 呼び出し業務サービス更新清掃状態。
    redirectAttributes.addFlashAttribute("message", "清掃状態を更新しました。"); // 設定清掃状態更新成功メッセージ。
    return "redirect:/reservations"; // リダイレクト回予約管理页。
  }
}
