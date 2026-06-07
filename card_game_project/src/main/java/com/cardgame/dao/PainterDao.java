package com.cardgame.dao;

import com.cardgame.db.ConnectionManager;
import com.cardgame.model.Painter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PainterDao {

    public List<Painter> findAll() throws SQLException {
        String sql = "SELECT id, name, surname, patronymic FROM painter ORDER BY id";
        List<Painter> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public Optional<Painter> findById(int id) throws SQLException {
        String sql = "SELECT id, name, surname, patronymic FROM painter WHERE id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private Painter mapRow(ResultSet rs) throws SQLException {
        return new Painter(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("surname"),
                rs.getString("patronymic")
        );
    }
}
