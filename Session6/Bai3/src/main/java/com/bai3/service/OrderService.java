package com.bai3.service;

import com.bai3.model.Order;

import java.util.List;

public interface OrderService {

    List<Order> findAll();

    Order findById(Long id);

    Order save(Order order);

    Order update(Long id, Order order);

    void delete(Long id);
}