package com.example.minshuku.controller; // 宣言料金ルールコントローラー所属パッケージ。

import com.example.minshuku.domain.RoomPriceRule; // 読み込み料金ルールエンティティ型。
import com.example.minshuku.service.RoomPriceRuleService; // 読み込み料金ルール業務サービス。
import com.example.minshuku.service.RoomService; // 読み込み部屋業務サービス。
import java.util.List; // 読み込み料金ルール番号一覧型。
import org.springframework.stereotype.Controller; // 読み込み Spring MVC ページコントローラーアノテーション。
import org.springframework.ui.Model; // 読み込みページモデルオブジェクト。
import org.springframework.web.bind.annotation.GetMapping; // 読み込み GET ルーティングアノテーション。
import org.springframework.web.bind.annotation.ModelAttribute; // 読み込みフォームオブジェクト绑定アノテーション。
import org.springframework.web.bind.annotation.PostMapping; // 読み込み POST ルーティングアノテーション。
import org.springframework.web.bind.annotation.PathVariable; // 読み込みパス変数绑定アノテーション。
import org.springframework.web.bind.annotation.RequestParam; // 読み込みリクエストパラメータ绑定アノテーション。
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 読み込みリダイレクト临时メッセージオブジェクト。

@Controller // 標记このクラス処理 Thymeleaf ページリクエスト。
public class PriceRuleController { // 定義时令料金管理コントローラー。
  private final RoomPriceRuleService priceRuleService; // 保存料金ルール業務サービス依赖。
  private final RoomService roomService; // 保存部屋業務サービス依赖。

  public PriceRuleController(RoomPriceRuleService priceRuleService, RoomService roomService) { // 定義构造メソッド用依赖注入。
    this.priceRuleService = priceRuleService; // 保存注入の料金ルール業務サービス。
    this.roomService = roomService; // 保存注入の部屋業務サービス。
  }

  @GetMapping("/prices") // を料金管理パス映射へページ処理メソッド。
  public String prices(Model model) { // 料金管理ページ処理を定義。
    model.addAttribute("rules", priceRuleService.findAllWithRoom()); // 向ページ传递料金ルール一覧。
    model.addAttribute("rooms", roomService.findActive()); // 向ページ传递启用部屋一覧。
    model.addAttribute("rule", new RoomPriceRule()); // 向ページ传递新規登録料金ルールフォームオブジェクト。
    return "prices"; // 返却 prices.html 模板。
  }

  @PostMapping("/prices") // を新規登録料金ルールフォーム送信パス映射へ処理メソッド。
  public String create(@ModelAttribute RoomPriceRule rule, RedirectAttributes redirectAttributes) { // 料金ルール追加処理を定義。
    try { // 捕获業務検証异常と转成ページメッセージ。
      priceRuleService.create(rule); // 呼び出し業務サービス新規登録料金ルール。
      redirectAttributes.addFlashAttribute("message", "料金ルールを登録しました。"); // 設定新規登録成功メッセージ。
    } catch (IllegalArgumentException ex) { // 捕获フォーム検証异常。
      redirectAttributes.addFlashAttribute("error", ex.getMessage()); // 設定エラーメッセージメッセージ。
    }
    return "redirect:/prices"; // リダイレクト回料金管理页避免重複送信。
  }

  @PostMapping({"/prices/{id}", "/prices/{id}/delete"}) // を单条削除パス映射へ処理メソッド，と兼容旧ページパス。
  public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) { // 料金ルール削除処理を定義。
    try { // 捕获業務検証异常と转成ページメッセージ。
      priceRuleService.delete(id); // 呼び出し業務サービス削除单条料金ルール。
      redirectAttributes.addFlashAttribute("message", "料金ルールを削除しました。"); // 設定削除成功メッセージ。
    } catch (IllegalArgumentException ex) { // 捕获フォーム検証异常。
      redirectAttributes.addFlashAttribute("error", ex.getMessage()); // 設定エラーメッセージメッセージ。
    }
    return "redirect:/prices"; // リダイレクト回料金管理页。
  }

  @PostMapping("/prices/delete-selected") // を一括削除パス映射へ処理メソッド。
  public String deleteSelected(@RequestParam(name = "ids", required = false) List<Integer> ids, RedirectAttributes redirectAttributes) { // 料金ルール一括削除処理を定義。
    try { // 捕获業務検証异常と转成ページメッセージ。
      priceRuleService.deleteByIds(ids); // 呼び出し業務サービス一括削除料金ルール。
      redirectAttributes.addFlashAttribute("message", "料金ルールを削除しました。"); // 設定削除成功メッセージ。
    } catch (IllegalArgumentException ex) { // 捕获フォーム検証异常。
      redirectAttributes.addFlashAttribute("error", ex.getMessage()); // 設定エラーメッセージメッセージ。
    }
    return "redirect:/prices"; // リダイレクト回料金管理页。
  }
}
