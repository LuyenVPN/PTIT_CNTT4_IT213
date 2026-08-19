package com.bai3.controller;

import com.bai3.model.Order;
import com.bai3.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllOrders() throws Exception {

        List<Order> orders = List.of(
                new Order(
                        1L,
                        "Nguyen Van A",
                        "Laptop",
                        2,
                        2000.0
                )
        );

        when(orderService.findAll()).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product")
                        .value("Laptop"));
    }

    @Test
    void testGetOrderById_Found() throws Exception {

        Order order = new Order(
                1L,
                "Nguyen Van A",
                "Laptop",
                2,
                2000.0
        );

        when(orderService.findById(1L))
                .thenReturn(order);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product")
                        .value("Laptop"));
    }
    @Test
    void testGetOrderById_NotFound() throws Exception {

        when(orderService.findById(99L))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }
    @Test
    void testCreateOrder() throws Exception {

        Order order = new Order(
                1L,
                "Nguyen Van A",
                "Laptop",
                2,
                2000.0
        );

        when(orderService.save(order))
                .thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(order)
                        ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(1L));
    }
}