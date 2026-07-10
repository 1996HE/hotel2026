package com.example.minshuku.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC 画面操作の業務エラーを flash message に変換する共通ハンドラー。
 */
@ControllerAdvice(assignableTypes = {
        RoomController.class,
        ReservationController.class,
        PriceRuleController.class
})
public class MvcExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + redirectPath(request.getRequestURI(), request.getContextPath());
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(
            RuntimeException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String redirectPath = redirectPath(request.getRequestURI(), request.getContextPath());
        redirectAttributes.addFlashAttribute("error", genericMessage(redirectPath));
        return "redirect:" + redirectPath;
    }

    private String redirectPath(String requestUri, String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        if (path.startsWith("/rooms")) {
            return "/rooms";
        }
        if (path.startsWith("/reservations")) {
            return "/reservations";
        }
        if (path.startsWith("/prices")) {
            return "/prices";
        }
        return "/dashboard";
    }

    private String genericMessage(String redirectPath) {
        if ("/rooms".equals(redirectPath)) {
            return "部屋登録に失敗しました。部屋番号が重複していないか確認してください。";
        }
        return "処理に失敗しました。入力内容を確認してください。";
    }
}
