package com.corporate.controller;

import jakarta.validation.Valid;
import com.corporate.dto.OrderDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.corporate.dto.AdminOrderSummaryDto;
import com.corporate.dto.OrderStatusUpdateRequest;
import com.corporate.service.AdminOrderService;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService service;

    public AdminOrderController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminOrderSummaryDto> list() {
        return service.listAll();
    }

    @GetMapping("/{orderNumber}")
    public OrderDto get(@PathVariable String orderNumber) {
        return service.get(orderNumber);
    }

    @PatchMapping("/{orderNumber}/status")
    public OrderDto updateStatus(@PathVariable String orderNumber,
                                 @Valid @RequestBody OrderStatusUpdateRequest req) {
        return service.updateStatus(orderNumber, req.status());
    }
}
