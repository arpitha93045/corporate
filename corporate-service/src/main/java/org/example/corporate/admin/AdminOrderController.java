package org.example.corporate.admin;

import jakarta.validation.Valid;
import org.example.corporate.order.OrderDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
