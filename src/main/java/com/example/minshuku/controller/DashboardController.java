package com.example.minshuku.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ダッシュボード画面の表示を担当するコントローラー。
 */
@Controller
public class DashboardController {
    /**
     * React の初期ページを返す。集計データは /api/dashboard で取得する。
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "app";
    }
}
