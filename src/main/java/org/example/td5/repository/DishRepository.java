package org.example.td5.repository;

import org.example.td5.datasource.DataSource;
import org.example.td5.entity.Dish;
import org.example.td5.entity.Ingredient;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {
    private final DataSource dataSource;

    public DishRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Dish> findAll() {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT * FROM dish";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int dishId = rs.getInt("id");
                dishes.add(new Dish(
                        dishId,
                        rs.getString("name"),
                        rs.getDouble("unit_price"),
                        findIngredientsByDishId(dishId)
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
        return dishes;
    }

    public Dish findById(int id) {
        String sql = "SELECT * FROM dish WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Dish(rs.getInt("id"), rs.getString("name"),
                            rs.getDouble("unit_price"), findIngredientsByDishId(id));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
        return null;
    }

    private List<Ingredient> findIngredientsByDishId(int dishId) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT i.* FROM ingredient i " +
                "JOIN dish_ingredient di ON i.id = di.id_ingredient " +
                "WHERE di.id_dish = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new Ingredient(
                            rs.getInt("id"), rs.getString("name"),
                            rs.getDouble("price"), rs.getString("category")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ingredients;
    }

    public void updateAssociations(int dishId, List<Ingredient> ingredients) {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        String insertSql = "INSERT INTO dish_ingredient (id_dish, id_ingredient) VALUES (?, ?)";
        String checkIngredientSql = "SELECT 1 FROM ingredient WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // DEBUT TRANSACTION

            try {
                try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                    delPs.setInt(1, dishId);
                    delPs.executeUpdate();
                }

                try (PreparedStatement insPs = conn.prepareStatement(insertSql);
                     PreparedStatement checkPs = conn.prepareStatement(checkIngredientSql)) {

                    for (Ingredient ing : ingredients) {
                        checkPs.setInt(1, ing.id());
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                insPs.setInt(1, dishId);
                                insPs.setInt(2, ing.id());
                                insPs.addBatch();
                            } else {

                                System.err.println("WARN: L'ingrédient ID " + ing.id() + " n'existe pas en BDD. Ignoré.");
                            }
                        }
                    }
                    insPs.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("ERROR: Erreur SQL lors de la mise à jour (Transaction annulée) : " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Impossible d'obtenir la connexion à la base de données : " + e.getMessage());
        }
    }
}