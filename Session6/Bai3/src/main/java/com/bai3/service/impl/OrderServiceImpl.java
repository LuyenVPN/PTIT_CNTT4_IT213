package com.bai3.service.impl;

import com.bai3.exception.ResourceNotFoundException;
import com.bai3.model.Order;
import com.bai3.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderServiceImpl implements OrderService {

    private final List<Order> orders = new ArrayList<>();

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public List<Order> findAll() {

        return orders;
    }

    @Override
    public Order findById(Long id) {

        return orders.stream()
                .filter(order -> order.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy Order với id = " + id
                        ));
    }

    @Override
    public Order save(Order order) {

        order.setId(counter.incrementAndGet());

        orders.add(order);

        return order;
    }

    @Override
    public Order update(Long id, Order updatedOrder) {

        Order existingOrder = findById(id);

        existingOrder.setCustomerName(updatedOrder.getCustomerName());
        existingOrder.setProduct(updatedOrder.getProduct());
        existingOrder.setQuantity(updatedOrder.getQuantity());
        existingOrder.setTotalAmount(updatedOrder.getTotalAmount());

        return existingOrder;
    }

    @Override
    public void delete(Long id) {

        Order order = findById(id);

        orders.remove(order);
    }
}