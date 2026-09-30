package com.example.minshuku.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 客向け公開サイトの各ページを同じ React エントリーポイントへ案内する。 */
@Controller
public class GuestSiteController {
    @GetMapping({
            "/stay", "/stay/rooms", "/stay/rates", "/stay/guide", "/stay/access", "/stay/ski",
            "/stay/reserve", "/stay/booking", "/stay/cancel"
    })
    public String guestSite() {
        return "guest";
    }
}
