package com.example.minshuku.service;

import com.example.minshuku.domain.Customer;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.mapper.CustomerMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 顧客検索、編集および予約スナップショットとの関連付けを扱う。 */
@Service
public class CustomerService {
    private static final int MAX_SEARCH_RESULTS = 100;
    private final CustomerMapper mapper;

    public CustomerService(CustomerMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Customer> search(String query) {
        String normalized = StringUtils.hasText(query) ? query.trim() : null;
        return mapper.search(normalized, MAX_SEARCH_RESULTS);
    }

    @Transactional(readOnly = true)
    public Customer findById(Integer id) {
        Customer customer = mapper.findById(id);
        if (customer == null)
            throw new IllegalArgumentException("顧客が見つかりません。");
        return customer;
    }

    @Transactional(readOnly = true)
    public List<Reservation> stayHistory(Integer id) {
        findById(id);
        return mapper.findStayHistory(id);
    }

    @Transactional
    public Customer create(Customer customer) {
        normalizeAndValidate(customer);
        customer.setCustomerNo("C" + String.format("%06d", mapper.nextCustomerSequence()));
        mapper.insert(customer);
        return customer;
    }

    @Transactional
    public Customer update(Integer id, Customer customer) {
        findById(id);
        customer.setId(id);
        normalizeAndValidate(customer);
        mapper.update(customer);
        return findById(id);
    }

    /**
     * 予約には当時の氏名・連絡先を残しつつ顧客IDを関連付ける。
     * 完全一致時だけ既存顧客を再利用し、似た名前だけでは自動統合しない。
     */
    @Transactional
    public Customer resolveForReservation(Reservation reservation) {
        if (reservation.getCustomerId() != null)
            return findById(reservation.getCustomerId());
        String phone = normalizeOptional(reservation.getGuestPhone());
        String email = normalizeEmail(reservation.getGuestEmail());
        Customer existing = mapper.findExact(reservation.getGuestName().trim(), phone, email);
        if (existing != null) {
            reservation.setCustomerId(existing.getId());
            return existing;
        }
        Customer customer = new Customer();
        customer.setName(reservation.getGuestName());
        customer.setPhone(phone);
        customer.setEmail(email);
        create(customer);
        reservation.setCustomerId(customer.getId());
        return customer;
    }

    private void normalizeAndValidate(Customer customer) {
        if (customer == null || !StringUtils.hasText(customer.getName())) {
            throw new IllegalArgumentException("顧客名を入力してください。");
        }
        customer.setName(customer.getName().trim());
        customer.setPhone(normalizeOptional(customer.getPhone()));
        customer.setEmail(normalizeEmail(customer.getEmail()));
        // 従来の「連絡先なし」予約も移行できるよう、氏名のみの顧客登録を許可する。
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
