package org.example.td5.repository;

import org.example.td5.datasource.DataSource;
import org.example.td5.entity.Ingredient;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {
    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Ingredient> findAll() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredient";

        try (Connection conn = dataSource.getConnection()) {
            if (conn == null) return ingredients;

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(mapResultSetToIngredient(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR findAll: " + e.getMessage());
        }
        return ingredients;
    }

    public Ingredient findById(int id) {
        String sql = "SELECT * FROM ingredient WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToIngredient(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR findById: " + e.getMessage());
        }
        return null;
    }

    public Double getStockValue(int id, String at, String unit) {
        String sql = "SELECT SUM(quantity) FROM stock_movement " +
                "WHERE id_ingredient = ? AND movement_date <= CAST(? AS TIMESTAMP)";

        try (Connection conn = dataSource.getConnection()) {
            if (conn == null) return 0.0;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setString(2, at);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR getStockValue: " + e.getMessage());
        }
        return 0.0;
    }

    private Ingredient mapResultSetToIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getString("category")
        );
    }
}