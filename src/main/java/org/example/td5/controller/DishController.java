
package org.example.td5.controller;

import org.example.td5.entity.Dish;
import org.example.td5.entity.Ingredient;
import org.example.td5.entity.CreateDishDTO;
import org.example.td5.repository.DishRepository;
import org.springframework.http.HttpStatus;
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
    public List<Dish> getDishes(
            @RequestParam(required = false) Double priceUnder,
            @RequestParam(required = false) Double priceOver,
            @RequestParam(required = false) String name
    ) {
        return dishRepository.findAllWithFilters(priceUnder, priceOver, name);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getDishById(@PathVariable int id) {
        Dish dish = dishRepository.findById(id);
        if (dish == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Le plat avec l'ID " + id + " n'existe pas.");
        }
        return ResponseEntity.ok(dish);
    }


    @PostMapping
    public ResponseEntity<?> createDishes(@RequestBody List<CreateDishDTO> dtos) {
        try {
            List<Dish> result = dishRepository.createDishes(dtos);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (RuntimeException e) {

            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }


    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateIngredients(@PathVariable int id, @RequestBody List<Ingredient> ingredients) {
        Dish dish = dishRepository.findById(id);
        if (dish == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dish not found");
        }

        dishRepository.updateAssociations(id, ingredients);


        Dish updatedDish = dishRepository.findById(id);
        return ResponseEntity.ok(updatedDish);
    }
}