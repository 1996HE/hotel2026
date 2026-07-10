package com.example.minshuku.controller;

import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.service.RoomPriceRuleService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 客室別料金ルール画面の表示と登録・削除を担当するコントローラー。
 */
@Controller
public class PriceRuleController {
    private final RoomPriceRuleService priceRuleService;

    public PriceRuleController(RoomPriceRuleService priceRuleService) {
        this.priceRuleService = priceRuleService;
    }

    /**
     * 料金ルール一覧と登録フォームの初期値を表示する。
     */
    @GetMapping("/prices")
    public String prices() {
        return "app";
    }

    /**
     * 料金ルールを登録する。入力エラーは画面表示用の flash attribute に変換する。
     */
    @PostMapping("/prices")
    public String create(@ModelAttribute RoomPriceRule rule, RedirectAttributes redirectAttributes) {
        // 料金設定はそのまま予約金額に反映されるため、入力エラーは即座に返す。
        priceRuleService.create(rule);
        redirectAttributes.addFlashAttribute("message", "料金ルールを登録しました。");
        return "redirect:/prices";
    }

    @PostMapping({"/prices/{id}", "/prices/{id}/delete"})
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // 旧URL互換を残しつつ、単一削除を同じ業務処理へ寄せる。
        priceRuleService.delete(id);
        redirectAttributes.addFlashAttribute("message", "料金ルールを削除しました。");
        return "redirect:/prices";
    }

    @PostMapping("/prices/delete-selected")
    public String deleteSelected(
            @RequestParam(name = "ids", required = false) List<Integer> ids,
            RedirectAttributes redirectAttributes) {
        // 一覧画面の複数選択削除は、空入力や重複選択をサービス側で正規化する。
        priceRuleService.deleteByIds(ids);
        redirectAttributes.addFlashAttribute("message", "料金ルールを削除しました。");
        return "redirect:/prices";
    }
}
