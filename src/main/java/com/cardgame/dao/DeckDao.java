package com.cardgame.dao;

import com.cardgame.db.ConnectionManager;
import com.cardgame.model.Card;
import com.cardgame.model.Deck;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для таблицы deck.
 * Связь карта↔колода хранится в card.deck_id (одна карта принадлежит одной колоде).
 * Поле deck.cards_quantity — хранимое значение из схемы; actual_cards вычисляется
 * как COUNT по card.deck_id. Назначение/снятие карты выполняется в транзакции.
 */
public class DeckDao {

    private static final String BASE_SELECT = """
            SELECT d.id, d.collector_id, d.cards_quantity,
                   (col.surname || ' ' || col.name) AS collector_name,
                   (SELECT COUNT(*) FROM card c WHERE c.deck_id = d.id) AS actual_cards
            FROM deck d
            JOIN collector col ON d.collector_id = col.id
            """;

    public List<Deck> findAll() throws SQLException {
        List<Deck> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY d.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public Optional<Deck> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE d.id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Создаёт колоду с коллекционером и заявленным числом карт (cards_quantity). */
    public int create(int collectorId, int cardsQuantity) throws SQLException {
        String sql = "INSERT INTO deck (collector_id, cards_quantity) VALUES (?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, collectorId);
            ps.setInt(2, cardsQuantity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new SQLException("Не удалось получить ключ созданной колоды");
        }
    }

    public boolean delete(int deckId) throws SQLException {
        String sql = "DELETE FROM deck WHERE id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Обновляет хранимое поле deck.cards_quantity. */
    public boolean updateCardsQuantity(int deckId, int cardsQuantity) throws SQLException {
        if (cardsQuantity < 0) throw new IllegalArgumentException("cards_quantity должно быть >= 0");
        String sql = "UPDATE deck SET cards_quantity = ? WHERE id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cardsQuantity);
            ps.setInt(2, deckId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Назначает карту в колоду (card.deck_id = deckId). Выполняется в транзакции.
     * @return true, если карта существует и была обновлена
     */
    public boolean assignCard(int deckId, long cardId) throws SQLException {
        String sql = "UPDATE card SET deck_id = ? WHERE id = ?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, deckId);
                ps.setLong(2, cardId);
                int changed = ps.executeUpdate();
                conn.commit();
                return changed > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Снимает карту с колоды (card.deck_id = NULL). Выполняется в транзакции.
     * @return true, если карта была назначена и снята
     */
    public boolean unassignCard(long cardId) throws SQLException {
        String sql = "UPDATE card SET deck_id = NULL WHERE id = ? AND deck_id IS NOT NULL";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cardId);
                int changed = ps.executeUpdate();
                conn.commit();
                return changed > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Фактическое число карт, назначенных в колоду (COUNT по card.deck_id). */
    public int actualCardCount(int deckId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM card WHERE deck_id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    /** Колода, в которую назначена заданная карта (если есть). */
    public Optional<Deck> findDeckOfCard(long cardId) throws SQLException {
        String sql = BASE_SELECT + " WHERE d.id = (SELECT deck_id FROM card WHERE id = ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private Deck mapRow(ResultSet rs) throws SQLException {
        Deck d = new Deck(rs.getInt("id"), rs.getInt("collector_id"), rs.getInt("cards_quantity"));
        d.setCollectorName(rs.getString("collector_name"));
        d.setActualCards(rs.getInt("actual_cards"));
        return d;
    }
}
