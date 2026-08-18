package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.minshuku.domain.Customer;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationFinance;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.service.AdminUserService;
import com.example.minshuku.service.CustomerService;
import com.example.minshuku.service.FinanceService;
import com.example.minshuku.service.ReportService;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.support.LoggedTest;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

/** 顧客、管理者、入返金、Excel 出力を実 DB マッピング込みで確認する。 */
@SpringBootTest
@Transactional
@LoggedTest
@DisplayName("追加業務機能DB連携")
class BusinessModulesLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private FinanceService financeService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
    }

    @Test
    @DisplayName("test_01 予約から顧客を作成し宿泊履歴を取得できる")
    void reservationCreatesCustomerAndStayHistory() {
        Reservation reservation = reservation(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        reservationService.create(reservation, false, List.of(), List.of(), List.of(), List.of(), List.of());

        Customer customer = customerService.search("山田").get(0);
        assertThat(customer.getCustomerNo()).isEqualTo("C000001");
        assertThat(customer.getStayCount()).isEqualTo(1);
        assertThat(customerService.stayHistory(customer.getId())).extracting(Reservation::getReservationNo)
                .containsExactly(reservation.getReservationNo());
    }

    @Test
    @DisplayName("test_02 一件の入金と部分返金を更新できる")
    void recordsOnePaymentAndPartialRefund() {
        Reservation reservation = reservation(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        reservationService.create(reservation, false, List.of(), List.of(), List.of(), List.of(), List.of());

        financeService.recordPayment(reservation.getId(), new BigDecimal("24000"), "cash", null);
        ReservationFinance finance = financeService.recordRefund(reservation.getId(), new BigDecimal("4000"), null);

        assertThat(finance.getReceivedAmount()).isEqualByComparingTo("24000");
        assertThat(finance.getRefundAmount()).isEqualByComparingTo("4000");
        assertThat(reservationMapper.findById(reservation.getId()).getPaymentStatus())
                .isEqualTo("partially_refunded");
        assertThatThrownBy(() -> financeService.recordRefund(
                reservation.getId(), new BigDecimal("24001"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("返金額は入金額を超えられません。");
    }

    @Test
    @DisplayName("test_03 日中両言語の営業Excelを出力できる")
    void createsJapaneseAndChineseExcelReports() throws Exception {
        Reservation reservation = reservation(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        reservationService.create(reservation, false, List.of(), List.of(), List.of(), List.of(), List.of());
        financeService.recordPayment(reservation.getId(), new BigDecimal("24000"), "card", null);

        byte[] japanese = reportService.createBusinessWorkbook(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "ja");
        byte[] chinese = reportService.createBusinessWorkbook(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "zh");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(japanese))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("月次営業集計");
            assertThat(workbook.getNumberOfSheets()).isEqualTo(5);
            assertThat(workbook.getSheet("予約一覧").getLastRowNum()).isEqualTo(1);
            assertThat(workbook.getSheet("予約一覧").getRow(1).getCell(8).getStringCellValue()).isEqualTo("予約済");
            assertThat(workbook.getSheet("宿泊記録")).isNotNull();
            assertThat(workbook.getSheet("入返金明細")).isNotNull();
            assertThat(workbook.getSheet("入返金明細").getRow(1).getCell(9).getStringCellValue()).isEqualTo("入金済");
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(chinese))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("月度营业汇总");
            assertThat(workbook.getSheet("预约列表").getRow(1).getCell(8).getStringCellValue()).isEqualTo("已预订");
            assertThat(workbook.getSheet("收退款明细").getRow(1).getCell(9).getStringCellValue()).isEqualTo("已收款");
        }
    }

    @Test
    @DisplayName("test_04 初回だけ管理者を作成し暗号化済み認証情報を読み込む")
    void createsOnlyOneAdministratorWithEncryptedPassword() {
        assertThat(adminUserService.setupRequired()).isTrue();
        adminUserService.setup("Owner.Admin", "strong-pass-2026");

        UserDetails user = adminUserService.loadUserByUsername("owner.admin");
        assertThat(user.getUsername()).isEqualTo("owner.admin");
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(adminUserService.setupRequired()).isFalse();
        assertThatThrownBy(() -> adminUserService.setup("second", "strong-pass-2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("管理者はすでに登録されています。");
    }

    private Reservation reservation(LocalDate checkIn, LocalDate checkOut) {
        Reservation reservation = new Reservation();
        reservation.setRoomId(bookableRoomId);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setGuestName("山田太郎");
        reservation.setGuestKana("ヤマダタロウ");
        reservation.setGuestPhone("090-0000-0000");
        reservation.setGuestEmail("guest@example.com");
        reservation.setGuestCount(1);
        return reservation;
    }
}
