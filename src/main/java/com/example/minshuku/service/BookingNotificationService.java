package com.example.minshuku.service;

import com.example.minshuku.config.PublicBookingProperties;
import com.example.minshuku.domain.PublicBookingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/** 予約申請の状態変化を、客人の選択言語と管理者宛てにメール通知する。 */
@Service
public class BookingNotificationService {
    private static final Logger log = LoggerFactory.getLogger(BookingNotificationService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PublicBookingProperties properties;

    public BookingNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
            PublicBookingProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingNotification(BookingNotificationEvent event) {
        if (!properties.isMailEnabled()) {
            log.info("Booking mail is disabled; notification {} for {} was not sent.", event.type(),
                    event.request().getRequestNo());
            return;
        }
        requireConfiguration();
        try {
            sendGuest(event);
            if (StringUtils.hasText(properties.getAdminMail())) {
                sendAdmin(event);
            }
        } catch (RuntimeException ex) {
            log.error("Booking notification failed for {}.", event.request().getRequestNo(), ex);
        }
    }

    private void sendGuest(BookingNotificationEvent event) {
        PublicBookingRequest request = event.request();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(request.getLeadEmail());
        message.setSubject(subject(request.getLocale(), event.type(), request.getRequestNo()));
        message.setText(body(request, event.type(), event.cancellationUrl()));
        requireMailSender().send(message);
    }

    private void sendAdmin(BookingNotificationEvent event) {
        PublicBookingRequest request = event.request();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(properties.getAdminMail());
        message.setSubject("[白馬樹海] " + event.type() + " " + request.getRequestNo());
        message.setText(request.getLeadName() + " / " + request.getLeadEmail() + "\n"
                + request.getCheckInDate() + " - " + request.getCheckOutDate() + "\n"
                + request.getGuestCount() + "名 / ¥" + request.getTotalAmount());
        requireMailSender().send(message);
    }

    private String subject(String locale, String type, String requestNo) {
        return switch (locale) {
            case "zh" -> "白马树海预约通知 " + requestNo + "（" + localizedType(locale, type) + "）";
            case "en" -> "Hakuba Jukai booking " + requestNo + " — " + localizedType(locale, type);
            default -> "白馬樹海 予約申請 " + requestNo + "（" + localizedType(locale, type) + "）";
        };
    }

    private String body(PublicBookingRequest request, String type, String cancellationUrl) {
        String status = localizedType(request.getLocale(), type);
        String reason = StringUtils.hasText(request.getRejectionReason()) ? "\n" + request.getRejectionReason() : "";
        return switch (request.getLocale()) {
            case "zh" -> request.getLeadName() + " 您好：\n预约申请 " + request.getRequestNo() + " 状态：" + status
                    + reason + "\n入住：" + request.getCheckInDate() + "\n退房：" + request.getCheckOutDate()
                    + "\n金额：¥" + request.getTotalAmount() + cancelLine("取消链接：", cancellationUrl);
            case "en" -> "Hello " + request.getLeadName() + ",\nBooking request " + request.getRequestNo()
                    + " is " + status + "." + reason + "\nCheck-in: " + request.getCheckInDate()
                    + "\nCheck-out: " + request.getCheckOutDate() + "\nTotal: ¥" + request.getTotalAmount()
                    + cancelLine("Cancellation link: ", cancellationUrl);
            default -> request.getLeadName() + " 様\n予約申請 " + request.getRequestNo() + " は「" + status + "」です。"
                    + reason + "\nチェックイン：" + request.getCheckInDate() + "\nチェックアウト："
                    + request.getCheckOutDate() + "\n合計：¥" + request.getTotalAmount()
                    + cancelLine("キャンセル用リンク：", cancellationUrl);
        };
    }

    private String cancelLine(String label, String url) {
        return StringUtils.hasText(url) ? "\n" + label + url : "";
    }

    private String localizedType(String locale, String type) {
        if ("zh".equals(locale)) {
            return switch (type) {
                case "confirmed" -> "已确认";
                case "rejected" -> "已拒绝";
                case "cancelled" -> "已取消";
                case "expired" -> "已超时";
                default -> "等待确认";
            };
        }
        if ("en".equals(locale)) {
            return switch (type) {
                case "confirmed" -> "confirmed";
                case "rejected" -> "declined";
                case "cancelled" -> "cancelled";
                case "expired" -> "expired";
                default -> "pending review";
            };
        }
        return switch (type) {
            case "confirmed" -> "確定";
            case "rejected" -> "お断り";
            case "cancelled" -> "取消済み";
            case "expired" -> "保留期限切れ";
            default -> "確認待ち";
        };
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(properties.getMailFrom())) {
            throw new IllegalStateException("BOOKING_MAIL_FROM is required when booking mail is enabled.");
        }
    }

    private JavaMailSender requireMailSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Mail sender is not configured.");
        }
        return mailSender;
    }
}
