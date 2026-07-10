package com.example.minshuku.controller;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.service.ReservationService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 予約管理画面の表示、登録、状態更新を担当するコントローラー。
 */
@Controller
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * 予約一覧、取消済一覧、チェックアウト済一覧をページングして表示する。
     */
    @GetMapping("/reservations")
    public String reservations() {
        return "app";
    }

    /**
     * 宿泊者と同行者の入力内容をもとに予約を登録する。
     */
    @PostMapping("/reservations")
    public String create(
            @ModelAttribute Reservation reservation,
            @RequestParam(required = false, defaultValue = "false") boolean noPhoneInfo,
            @RequestParam(required = false, defaultValue = "false") boolean noEmailInfo,
            @RequestParam(required = false) List<String> companionNames,
            @RequestParam(required = false) List<String> companionKanas,
            @RequestParam(required = false) List<String> companionGenders,
            @RequestParam(required = false) List<Integer> companionAges,
            @RequestParam(required = false) List<String> companionPhones,
            RedirectAttributes redirectAttributes) {
        // 連絡先を保持しない運用では、電話・メールの両方を同時に非保持へ寄せる。
        reservationService.create(
                reservation,
                noPhoneInfo && noEmailInfo,
                companionNames,
                companionKanas,
                companionGenders,
                companionAges,
                companionPhones);
        redirectAttributes.addFlashAttribute("message", "予約を登録しました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/payment")
    public String updatePayment(
            @PathVariable Integer id,
            @RequestParam String paymentStatus,
            RedirectAttributes redirectAttributes) {
        // 支払い状態は一覧表示や集計に直結するため、単独更新でも業務状態を確定する。
        reservationService.updatePaymentStatus(id, paymentStatus);
        redirectAttributes.addFlashAttribute("message", "支払い状況を更新しました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 取消は予約だけでなく客室の占有状態も巻き戻す。
        reservationService.cancel(id);
        redirectAttributes.addFlashAttribute("message", "予約をキャンセルしました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/delete")
    public String deleteCancelled(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 取消済み予約だけを一覧から完全に消す。
        reservationService.deleteCancelled(id);
        redirectAttributes.addFlashAttribute("message", "取消済み予約を削除しました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/delete-checked-out")
    public String deleteCheckedOut(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // チェックアウト済み予約だけを一覧から完全に消す。
        reservationService.deleteCheckedOut(id);
        redirectAttributes.addFlashAttribute("message", "チェックアウト済み予約を削除しました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/status")
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam String reservationStatus,
            RedirectAttributes redirectAttributes) {
        // 予約状態の遷移は客室状態と分離しない。
        reservationService.updateReservationStatus(id, reservationStatus);
        redirectAttributes.addFlashAttribute("message", "予約状態を更新しました。");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/cleaning")
    public String updateCleaning(
            @PathVariable Integer id,
            @RequestParam String cleaningStatus,
            RedirectAttributes redirectAttributes) {
        // 清掃業務用の更新は、チェックアウト後の客室運用に限定する。
        reservationService.updateCheckoutCleaningStatus(id, cleaningStatus);
        redirectAttributes.addFlashAttribute("message", "清掃状態を更新しました。");
        return "redirect:/reservations";
    }
}
