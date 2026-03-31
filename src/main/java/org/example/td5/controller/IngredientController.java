package org.example.td5.controller;

import org.example.td5.entity.Ingredient;
import org.example.td5.repository.IngredientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {
    private final IngredientRepository repository;

    public IngredientController(IngredientRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Ingredient> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        Ingredient ing = repository.findById(id);
        if (ing == null) {
            return ResponseEntity.status(404).body("Ingredient.id=" + id + " is not found");
        }
        return ResponseEntity.ok(ing);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStock(
            @PathVariable int id,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) String unit) {

        if (at == null || unit == null) {
            return ResponseEntity.status(400).body("Either mandatory query parameter `at` or `unit` is not provided.");
        }

        if (repository.findById(id) == null) {
            return ResponseEntity.status(404).body("Ingredient.id=" + id + " is not found");
        }

        Double value = repository.getStockValue(id, at, unit);
        return ResponseEntity.ok(Map.of("unit", unit, "value", value));
    }
}