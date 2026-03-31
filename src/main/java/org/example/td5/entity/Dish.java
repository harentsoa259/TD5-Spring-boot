package org.example.td5.entity;

import java.util.List;

public record Dish(int id, String name, double unitPrice, List<Ingredient> ingredients) {}