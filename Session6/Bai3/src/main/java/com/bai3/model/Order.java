package com.bai3.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;

    private String customerName;

    private String product;

    private Integer quantity;

    private Double totalAmount;
}