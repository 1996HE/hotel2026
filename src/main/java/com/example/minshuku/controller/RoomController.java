package com.example.minshuku.controller;

import com.example.minshuku.domain.Room;
import com.example.minshuku.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 客室管理画面の表示、登録、ステータス更新、論理削除を担当するコントローラー。
 */
@Controller
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * 有効な客室と削除済み客室を一覧表示する。
     */
    @GetMapping("/rooms")
    public String rooms() {
        return "app";
    }

    /**
     * 客室を新規登録する。削除済みの同一客室番号がある場合は再有効化される。
     */
    @PostMapping("/rooms")
    public String create(@ModelAttribute Room room, RedirectAttributes redirectAttributes) {
        // 新規登録と既存無効部屋の再有効化は、サービス側で同一業務として扱う。
        roomService.create(room);
        redirectAttributes.addFlashAttribute("message", "部屋を登録しました。");
        return "redirect:/rooms";
    }

    @PostMapping("/rooms/{id}/statuses")
    public String updateStatuses(
            @PathVariable Integer id,
            @RequestParam String occupancyStatus,
            @RequestParam String cleaningStatus,
            RedirectAttributes redirectAttributes) {
        // 宿泊状態と清掃状態は組み合わせで運用されるため、同時更新にする。
        roomService.updateStatuses(id, occupancyStatus, cleaningStatus);
        redirectAttributes.addFlashAttribute("message", "部屋ステータスを更新しました。");
        return "redirect:/rooms";
    }

    @PostMapping("/rooms/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 物理削除ではなく論理削除にして、過去予約の参照整合を保つ。
        roomService.delete(id);
        redirectAttributes.addFlashAttribute("message", "部屋を削除しました。");
        return "redirect:/rooms";
    }

    @PostMapping("/rooms/{id}/restore")
    public String restore(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 削除済み一覧から戻す処理は、論理削除フラグを反転するだけに留める。
        roomService.restore(id);
        redirectAttributes.addFlashAttribute("message", "部屋を復元しました。");
        return "redirect:/rooms";
    }

    @PostMapping("/rooms/{id}/delete-permanently")
    public String deletePermanently(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 予約履歴がない削除済み部屋だけを完全削除し、一覧からも消す。
        roomService.deletePermanently(id);
        redirectAttributes.addFlashAttribute("message", "部屋を完全削除しました。");
        return "redirect:/rooms";
    }
}
