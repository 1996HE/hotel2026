package com.example.minshuku.service;

import com.example.minshuku.mapper.ReservationFinanceMapper.FinanceReportRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** 予約、収入、返金および月次集計を一つの Excel ブックへ出力する。 */
@Service
public class ReportService {
    private final FinanceService financeService;

    public ReportService(FinanceService financeService) {
        this.financeService = financeService;
    }

    public byte[] createBusinessWorkbook(LocalDate startDate, LocalDate endDate, String language) {
        FinanceService.BusinessSummary summary = financeService.summary(startDate, endDate);
        boolean chinese = "zh".equalsIgnoreCase(language);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            createSummarySheet(workbook, summary, startDate, endDate, chinese, header);
            createReservationSheet(workbook, summary, chinese, header);
            createStaySheet(workbook, summary, chinese, header);
            createFinanceDetailSheet(workbook, summary, chinese, header);
            createPaymentMethodSheet(workbook, summary, chinese, header);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Excelレポートの作成に失敗しました。", ex);
        }
    }

    private void createSummarySheet(Workbook workbook, FinanceService.BusinessSummary summary,
            LocalDate startDate, LocalDate endDate, boolean zh, CellStyle header) {
        Sheet sheet = workbook.createSheet(zh ? "月度营业汇总" : "月次営業集計");
        String[][] values = {
                {zh ? "统计期间" : "集計期間", startDate + " ～ " + endDate},
                {zh ? "订单数量" : "予約件数", String.valueOf(summary.rows().size())},
                {zh ? "应收金额" : "売上予定", summary.receivable().toPlainString()},
                {zh ? "实收金额" : "入金額", summary.received().toPlainString()},
                {zh ? "退款金额" : "返金額", summary.refunded().toPlainString()},
                {zh ? "净收入" : "実収入", summary.netRevenue().toPlainString()}
        };
        for (int i = 0; i < values.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(values[i][0]);
            row.getCell(0).setCellStyle(header);
            row.createCell(1).setCellValue(values[i][1]);
        }
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 26 * 256);
    }

    private void createReservationSheet(Workbook workbook, FinanceService.BusinessSummary summary,
            boolean zh, CellStyle header) {
        Sheet sheet = workbook.createSheet(zh ? "预约列表" : "予約一覧");
        String[] headings = zh
                ? new String[]{"预约编号", "客户", "联系方式", "房间", "入住日", "退房日", "人数", "应收", "订单状态"}
                : new String[]{"予約番号", "顧客", "連絡先", "客室", "チェックイン", "チェックアウト", "人数", "売上予定", "予約状態"};
        Row heading = sheet.createRow(0);
        for (int i = 0; i < headings.length; i++) {
            heading.createCell(i).setCellValue(headings[i]);
            heading.getCell(i).setCellStyle(header);
        }
        int rowIndex = 1;
        for (FinanceReportRow item : summary.rows()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.reservationNo());
            row.createCell(1).setCellValue(item.guestName());
            row.createCell(2).setCellValue(item.guestPhone() == null ? "" : item.guestPhone());
            row.createCell(3).setCellValue(item.roomNumber());
            row.createCell(4).setCellValue(item.checkInDate().toString());
            row.createCell(5).setCellValue(item.checkOutDate().toString());
            row.createCell(6).setCellValue(item.guestCount());
            row.createCell(7).setCellValue(item.totalAmount().doubleValue());
            row.createCell(8).setCellValue(reservationStatusLabel(item.reservationStatus(), zh));
        }
        for (int i = 0; i < headings.length; i++)
            sheet.autoSizeColumn(i);
    }

    private void createStaySheet(Workbook workbook, FinanceService.BusinessSummary summary,
            boolean zh, CellStyle header) {
        Sheet sheet = workbook.createSheet(zh ? "住宿记录" : "宿泊記録");
        String[] headings = zh
                ? new String[]{"预约编号", "客户", "房间", "计划入住", "计划退房", "实际入住时间", "实际退房时间", "状态"}
                : new String[]{"予約番号", "顧客", "客室", "予定チェックイン", "予定チェックアウト", "実チェックイン", "実チェックアウト", "状態"};
        Row heading = sheet.createRow(0);
        for (int i = 0; i < headings.length; i++) {
            heading.createCell(i).setCellValue(headings[i]);
            heading.getCell(i).setCellStyle(header);
        }
        int rowIndex = 1;
        for (FinanceReportRow item : summary.rows()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.reservationNo());
            row.createCell(1).setCellValue(item.guestName());
            row.createCell(2).setCellValue(item.roomNumber());
            row.createCell(3).setCellValue(item.checkInDate().toString());
            row.createCell(4).setCellValue(item.checkOutDate().toString());
            row.createCell(5).setCellValue(item.checkedInAt() == null ? "" : item.checkedInAt().toString());
            row.createCell(6).setCellValue(item.checkedOutAt() == null ? "" : item.checkedOutAt().toString());
            row.createCell(7).setCellValue(reservationStatusLabel(item.reservationStatus(), zh));
        }
        for (int i = 0; i < headings.length; i++)
            sheet.autoSizeColumn(i);
    }

    private void createFinanceDetailSheet(Workbook workbook, FinanceService.BusinessSummary summary,
            boolean zh, CellStyle header) {
        Sheet sheet = workbook.createSheet(zh ? "收退款明细" : "入返金明細");
        String[] headings = zh
                ? new String[]{"预约编号", "客户", "应收", "实收", "收款时间", "退款", "退款时间", "净收入", "支付方式", "收款状态"}
                : new String[]{"予約番号", "顧客", "売上予定", "入金", "入金日時", "返金", "返金日時", "実収入", "支払方法", "入返金状態"};
        Row heading = sheet.createRow(0);
        for (int i = 0; i < headings.length; i++) {
            heading.createCell(i).setCellValue(headings[i]);
            heading.getCell(i).setCellStyle(header);
        }
        int rowIndex = 1;
        for (FinanceReportRow item : summary.rows()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.reservationNo());
            row.createCell(1).setCellValue(item.guestName());
            row.createCell(2).setCellValue(item.totalAmount().doubleValue());
            row.createCell(3).setCellValue(item.receivedAmount().doubleValue());
            row.createCell(4).setCellValue(item.receivedAt() == null ? "" : item.receivedAt().toString());
            row.createCell(5).setCellValue(item.refundAmount().doubleValue());
            row.createCell(6).setCellValue(item.refundedAt() == null ? "" : item.refundedAt().toString());
            row.createCell(7).setCellValue(item.receivedAmount().subtract(item.refundAmount()).doubleValue());
            row.createCell(8).setCellValue(paymentMethodLabel(item.paymentMethod(), zh));
            row.createCell(9).setCellValue(paymentStatusLabel(item.paymentStatus(), zh));
        }
        for (int i = 0; i < headings.length; i++)
            sheet.autoSizeColumn(i);
    }

    private void createPaymentMethodSheet(Workbook workbook, FinanceService.BusinessSummary summary,
            boolean zh, CellStyle header) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (FinanceReportRow row : summary.rows()) {
            String method = row.paymentMethod() == null ? "unpaid" : row.paymentMethod();
            totals.merge(method, row.receivedAmount(), BigDecimal::add);
        }
        Sheet sheet = workbook.createSheet(zh ? "支付方式汇总" : "支払方法集計");
        Row heading = sheet.createRow(0);
        heading.createCell(0).setCellValue(zh ? "支付方式" : "支払方法");
        heading.createCell(1).setCellValue(zh ? "实收金额" : "入金額");
        heading.getCell(0).setCellStyle(header);
        heading.getCell(1).setCellStyle(header);
        int index = 1;
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            Row row = sheet.createRow(index++);
            row.createCell(0).setCellValue(paymentMethodLabel(entry.getKey(), zh));
            row.createCell(1).setCellValue(entry.getValue().doubleValue());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private String paymentMethodLabel(String method, boolean zh) {
        if (method == null || "unpaid".equals(method))
            return zh ? "未收款" : "未入金";
        return switch (method) {
            case "cash" -> zh ? "现金" : "現金";
            case "card" -> zh ? "银行卡" : "カード";
            case "transfer" -> zh ? "转账" : "振込";
            case "platform" -> zh ? "平台收款" : "予約サイト";
            default -> zh ? "旧数据/未知" : "旧データ・不明";
        };
    }

    /** DB の安定した状態コードを、選択した帳票言語の表示名へ変換する。 */
    private String reservationStatusLabel(String status, boolean zh) {
        return switch (status) {
            case "booked" -> zh ? "已预订" : "予約済";
            case "checked_in" -> zh ? "已入住" : "チェックイン済";
            case "checked_out" -> zh ? "已退房" : "チェックアウト済";
            case "cancelled" -> zh ? "已取消" : "キャンセル";
            default -> zh ? "未知状态" : "不明な状態";
        };
    }

    private String paymentStatusLabel(String status, boolean zh) {
        return switch (status) {
            case "paid" -> zh ? "已收款" : "入金済";
            case "partially_refunded" -> zh ? "部分退款" : "一部返金";
            case "refunded" -> zh ? "已退款" : "返金済";
            default -> zh ? "未收款" : "未入金";
        };
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
