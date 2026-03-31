package org.example.td5.entity;

public record CreateDishDTO(
        String name,
        DishCategory category,
        Double price
) {}