package com.bai3.controller;

import com.bai3.model.Order;
import com.bai3.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {

        return ResponseEntity.ok(orderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    orderService.findById(id)
            );

        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody Order order
    ) {

        return new ResponseEntity<>(
                orderService.save(order),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable Long id,
            @RequestBody Order order
    ) {

        return ResponseEntity.ok(
                orderService.update(id, order)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id
    ) {

        orderService.delete(id);

        return ResponseEntity.noContent().build();
    }
}