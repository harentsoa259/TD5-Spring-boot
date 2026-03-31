package org.example.td5.controller;

import org.example.td5.entity.Dish;
import org.example.td5.entity.Ingredient;
import org.example.td5.repository.DishRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {
    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public List<Dish> getAllDishes() {
        return dishRepository.findAll();
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateIngredients(
            @PathVariable int id,
            @RequestBody(required = false) List<Ingredient> ingredients) {

        if (dishRepository.findById(id) == null) {
            return ResponseEntity.status(404).body("Dish.id=" + id + " is not found");
        }

        if (ingredients == null) {
            return ResponseEntity.status(400).body("The request body containing the list of ingredients is mandatory.");
        }

        dishRepository.updateAssociations(id, ingredients);
        return ResponseEntity.ok("Ingredients updated for dish " + id);
    }
}