
package org.example.td5.repository;

import org.example.td5.datasource.DataSource;
import org.example.td5.entity.Dish;
import org.example.td5.entity.Ingredient;
import org.example.td5.entity.CreateDishDTO;
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
        return findAllWithFilters(null, null, null);
    }

    public Dish findById(int id) {
        String sql = "SELECT * FROM dish WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDish(rs, conn);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du plat " + id, e);
        }
        return null;
    }

    private List<Ingredient> findIngredientsByDishId(int dishId, Connection conn) throws SQLException {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT i.* FROM ingredient i " +
                "JOIN dish_ingredient di ON i.id = di.id_ingredient " +
                "WHERE di.id_dish = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("category")
                    ));
                }
            }
        }
        return ingredients;
    }

    public void updateAssociations(int dishId, List<Ingredient> ingredients) {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        String insertSql = "INSERT INTO dish_ingredient (id_dish, id_ingredient) VALUES (?, ?)";

        String updateIngSql = "UPDATE ingredient SET category = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                    delPs.setInt(1, dishId);
                    delPs.executeUpdate();
                }

                if (ingredients != null && !ingredients.isEmpty()) {
                    try (PreparedStatement insPs = conn.prepareStatement(insertSql);
                         PreparedStatement updPs = conn.prepareStatement(updateIngSql)) {

                        for (Ingredient ing : ingredients) {

                            updPs.setString(1, ing.category());
                            updPs.setInt(2, ing.id());
                            updPs.addBatch();

                            insPs.setInt(1, dishId);
                            insPs.setInt(2, ing.id());
                            insPs.addBatch();
                        }

                        updPs.executeBatch();
                        insPs.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la mise à jour : " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Problème de connexion BDD", e);
        }
    }

    public List<Dish> createDishes(List<CreateDishDTO> dtos) throws SQLException {
        List<Dish> createdDishes = new ArrayList<>();
        String checkSql = "SELECT COUNT(*) FROM dish WHERE name = ?";
        String insertSql = "INSERT INTO dish (name, unit_price, category) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (CreateDishDTO dto : dtos) {

                    try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                        checkPs.setString(1, dto.name());
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                throw new RuntimeException("Dish.name=" + dto.name() + " already exists");
                            }
                        }
                    }


                    try (PreparedStatement insPs = conn.prepareStatement(insertSql)) {
                        insPs.setString(1, dto.name());
                        insPs.setDouble(2, dto.price());
                        insPs.setString(3, dto.category().name());

                        try (ResultSet rs = insPs.executeQuery()) {
                            if (rs.next()) {
                                int newId = rs.getInt(1);
                                createdDishes.add(new Dish(newId, dto.name(), dto.price(), new ArrayList<>()));
                            }
                        }
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        return createdDishes;
    }

    public List<Dish> findAllWithFilters(Double pMax, Double pMin, String name) {
        List<Dish> dishes = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM dish WHERE 1=1");

        if (pMax != null) sql.append(" AND unit_price < ?");
        if (pMin != null) sql.append(" AND unit_price > ?");
        if (name != null && !name.isBlank()) sql.append(" AND name ILIKE ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (pMax != null) ps.setDouble(paramIndex++, pMax);
            if (pMin != null) ps.setDouble(paramIndex++, pMin);
            if (name != null && !name.isBlank()) ps.setString(paramIndex++, "%" + name + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dishes.add(mapResultSetToDish(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des plats", e);
        }
        return dishes;
    }

    private Dish mapResultSetToDish(ResultSet rs, Connection conn) throws SQLException {
        int id = rs.getInt("id");
        return new Dish(
                id,
                rs.getString("name"),
                rs.getDouble("unit_price"),
                findIngredientsByDishId(id, conn)
        );
    }
}