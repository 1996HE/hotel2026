package com.example.minshuku.controller; // 宣言部屋コントローラー所属パッケージ。

import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ型。
import com.example.minshuku.service.RoomService; // 読み込み部屋業務サービス。
import org.springframework.stereotype.Controller; // 読み込み Spring MVC ページコントローラーアノテーション。
import org.springframework.ui.Model; // 読み込みページモデルオブジェクト。
import org.springframework.web.bind.annotation.GetMapping; // 読み込み GET ルーティングアノテーション。
import org.springframework.web.bind.annotation.ModelAttribute; // 読み込みフォームオブジェクト绑定アノテーション。
import org.springframework.web.bind.annotation.PathVariable; // 読み込みパス変数绑定アノテーション。
import org.springframework.web.bind.annotation.PostMapping; // 読み込み POST ルーティングアノテーション。
import org.springframework.web.bind.annotation.RequestParam; // 読み込みリクエストパラメータ绑定アノテーション。
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 読み込みリダイレクト临时メッセージオブジェクト。

@Controller // 標记このクラス処理 Thymeleaf ページリクエスト。
public class RoomController { // 定義部屋管理コントローラー。
  private final RoomService roomService; // 保存部屋業務サービス依赖。

  public RoomController(RoomService roomService) { // 定義構築メソッド用依赖注入。
    this.roomService = roomService; // 保存注入の部屋業務サービス。
  }

  @GetMapping("/rooms") // を部屋一覧パス映射へページ処理メソッド。
  public String rooms(Model model) { // 定義部屋管理ページ処理メソッド。
    model.addAttribute("rooms", roomService.findAll()); // 向ページ传递部屋一覧。
    model.addAttribute("deletedRooms", roomService.findInactive()); // 向ページ传递削除済み部屋一覧。
    model.addAttribute("room", new Room()); // 向ページ传递新規登録部屋フォームオブジェクト。
    return "rooms"; // 返却 rooms.html 模板。
  }

  @PostMapping("/rooms") // を新規登録部屋フォーム送信パス映射へ処理メソッド。
  public String create(@ModelAttribute Room room, RedirectAttributes redirectAttributes) { // 定義新規登録部屋処理メソッド。
    try { // 捕获業務検証例外と转成ページメッセージ。
      roomService.create(room); // 呼び出し業務サービス新規登録部屋。
      redirectAttributes.addFlashAttribute("message", "部屋を登録しました。"); // 設定新規登録成功メッセージ。
    } catch (IllegalArgumentException ex) { // 捕获フォーム検証例外。
      redirectAttributes.addFlashAttribute("error", ex.getMessage()); // 設定エラーメッセージメッセージ。
    } catch (RuntimeException ex) { // 捕获データベース唯一约束等起動时例外。
      redirectAttributes.addFlashAttribute("error", "部屋登録に失敗しました。部屋番号が重複していないか確認してください。"); // 設定通用登録失败メッセージ。
    }
    return "redirect:/rooms"; // リダイレクト回部屋管理页避免重複送信。
  }

  @PostMapping("/rooms/{id}/statuses") // を部屋状態更新パス映射へ処理メソッド。
  public String updateStatuses(@PathVariable Integer id, @RequestParam String occupancyStatus, @RequestParam String cleaningStatus, RedirectAttributes redirectAttributes) { // 定義部屋状態更新処理メソッド。
    roomService.updateStatuses(id, occupancyStatus, cleaningStatus); // 呼び出し業務サービス更新部屋状態。
    redirectAttributes.addFlashAttribute("message", "部屋ステータスを更新しました。"); // 設定状態更新成功メッセージ。
    return "redirect:/rooms"; // リダイレクト回部屋管理页。
  }

  @PostMapping("/rooms/{id}/delete") // を部屋削除パス映射へ処理メソッド。
  public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) { // 定義部屋削除処理メソッド。
    roomService.delete(id); // 呼び出し業務サービス削除部屋。
    redirectAttributes.addFlashAttribute("message", "部屋を削除しました。"); // 設定削除成功メッセージ。
    return "redirect:/rooms"; // リダイレクト回部屋管理页。
  }
}
