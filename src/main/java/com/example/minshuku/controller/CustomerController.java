package com.example.minshuku.controller;

import com.example.minshuku.domain.Customer;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 顧客検索、編集、宿泊履歴を提供する JSON API。 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Customer> search(@RequestParam(required = false) String query) {
        return service.search(query);
    }

    @GetMapping("/{id}")
    public Customer find(@PathVariable Integer id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/stays")
    public List<Reservation> stays(@PathVariable Integer id) {
        return service.stayHistory(id);
    }

    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return service.create(customer);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Integer id, @RequestBody Customer customer) {
        return service.update(id, customer);
    }
}
