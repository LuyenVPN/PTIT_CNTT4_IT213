package com.bai3.service;

import com.bai3.exception.ResourceNotFoundException;
import com.bai3.model.Order;
import com.bai3.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {

        orderService = new OrderServiceImpl();

        orderService.save(
                new Order(
                        null,
                        "Nguyen Van A",
                        "Laptop",
                        2,
                        2000.0
                )
        );
    }

    // 1
    @Test
    void getAllOrders_ReturnNonEmptyList() {

        List<Order> orders = orderService.findAll();

        assertFalse(orders.isEmpty());
    }

    // 2
    @Test
    void getOrderById_Found() {

        Order order = orderService.findById(1L);

        assertEquals("Laptop", order.getProduct());
    }

    // 3
    @Test
    void getOrderById_NotFound_ThrowException() {

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.findById(99L)
        );
    }

    // 4
    @Test
    void addOrder_Success() {

        Order order = new Order(
                null,
                "Tran Van B",
                "Mouse",
                1,
                100.0
        );

        Order savedOrder = orderService.save(order);

        assertNotNull(savedOrder.getId());
    }

    // 5
    @Test
    void updateOrder_Success() {

        Order updatedOrder = new Order(
                null,
                "Tran Van C",
                "Keyboard",
                3,
                300.0
        );

        Order result = orderService.update(1L, updatedOrder);

        assertEquals("Keyboard", result.getProduct());
    }

    // 6
    @Test
    void deleteOrder_RemovesElement() {

        orderService.delete(1L);

        assertEquals(0, orderService.findAll().size());
    }
}