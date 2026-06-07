package com.cardgame.dao;

import com.cardgame.db.ConnectionManager;
import com.cardgame.model.Card;
import com.cardgame.model.Deck;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO для таблиц deck и deck_card.
 * Изменения состава колоды (add/remove экземпляров карты) выполняются в транзакции.
 */
public class DeckDao {

    public List<Deck> findAll() throws SQLException {
        String sql = """
                SELECT d.id, d.collector_id, d.name,
                       (c.surname || ' ' || c.name) AS collector_name,
                       COALESCE(SUM(dc.quantity), 0) AS total_cards
                FROM deck d
                JOIN collector c ON d.collector_id = c.id
                LEFT JOIN deck_card dc ON d.id = dc.deck_id
                GROUP BY d.id, d.collector_id, d.name, collector_name
                ORDER BY d.id
                """;
        List<Deck> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public Optional<Deck> findById(int id) throws SQLException {
        String sql = """
                SELECT d.id, d.collector_id, d.name,
                       (c.surname || ' ' || c.name) AS collector_name,
                       COALESCE(SUM(dc.quantity), 0) AS total_cards
                FROM deck d
                JOIN collector c ON d.collector_id = c.id
                LEFT JOIN deck_card dc ON d.id = dc.deck_id
                WHERE d.id = ?
                GROUP BY d.id, d.collector_id, d.name, collector_name
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public int create(int collectorId, String name) throws SQLException {
        String sql = "INSERT INTO deck (collector_id, name) VALUES (?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, collectorId);
            ps.setString(2, name);
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

    /**
     * Добавляет указанное число экземпляров карты в колоду (UPSERT).
     * Выполняется в транзакции.
     */
    public void addCards(int deckId, long cardId, int quantity) throws SQLException {
        if (quantity <= 0) throw new IllegalArgumentException("Количество должно быть > 0");
        String sql = """
                INSERT INTO deck_card (deck_id, card_id, quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (deck_id, card_id)
                DO UPDATE SET quantity = deck_card.quantity + EXCLUDED.quantity
                """;
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, deckId);
                ps.setLong(2, cardId);
                ps.setInt(3, quantity);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Удаляет указанное число экземпляров карты из колоды. Если остаток
     * становится <= 0 — строка удаляется целиком. Выполняется в транзакции.
     *
     * @return true, если что-то было изменено
     */
    public boolean removeCards(int deckId, long cardId, int quantity) throws SQLException {
        if (quantity <= 0) throw new IllegalArgumentException("Количество должно быть > 0");
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int current = currentQuantity(conn, deckId, cardId);
                if (current == 0) {
                    conn.rollback();
                    return false;
                }
                if (current <= quantity) {
                    deleteDeckCard(conn, deckId, cardId);
                } else {
                    decrementDeckCard(conn, deckId, cardId, quantity);
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private int currentQuantity(Connection conn, int deckId, long cardId) throws SQLException {
        String sql = "SELECT quantity FROM deck_card WHERE deck_id = ? AND card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setLong(2, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("quantity") : 0;
            }
        }
    }

    private void deleteDeckCard(Connection conn, int deckId, long cardId) throws SQLException {
        String sql = "DELETE FROM deck_card WHERE deck_id = ? AND card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setLong(2, cardId);
            ps.executeUpdate();
        }
    }

    private void decrementDeckCard(Connection conn, int deckId, long cardId, int quantity) throws SQLException {
        String sql = "UPDATE deck_card SET quantity = quantity - ? WHERE deck_id = ? AND card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, deckId);
            ps.setLong(3, cardId);
            ps.executeUpdate();
        }
    }

    /** Суммарное число карт в колоде (с учётом количества экземпляров). */
    public int totalCardCount(int deckId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) AS total FROM deck_card WHERE deck_id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    /** Состав колоды: карта -> количество экземпляров. */
    public Map<Card, Integer> composition(int deckId) throws SQLException {
        String sql = """
                SELECT c.id, c.painter_id, c.set_id, c.name, c.rarity, c.activation_cost,
                       c.magic_color, c.type, c.description,
                       (p.surname || ' ' || p.name) AS painter_name,
                       s.name AS set_name,
                       dc.quantity
                FROM deck_card dc
                JOIN card c ON dc.card_id = c.id
                JOIN painter p ON c.painter_id = p.id
                JOIN card_set s ON c.set_id = s.id
                WHERE dc.deck_id = ?
                ORDER BY dc.quantity DESC, c.name
                """;
        Map<Card, Integer> result = new LinkedHashMap<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card c = new Card(
                            rs.getLong("id"), rs.getInt("painter_id"), rs.getInt("set_id"),
                            rs.getString("name"), rs.getShort("rarity"), rs.getShort("activation_cost"),
                            rs.getString("magic_color"), rs.getString("type"), rs.getString("description"));
                    c.setPainterName(rs.getString("painter_name"));
                    c.setSetName(rs.getString("set_name"));
                    result.put(c, rs.getInt("quantity"));
                }
            }
        }
        return result;
    }

    /** Средняя стоимость активации карт в колоде (взвешенная по количеству). */
    public Optional<Double> averageActivationCost(int deckId) throws SQLException {
        String sql = """
                SELECT SUM(c.activation_cost * dc.quantity)::NUMERIC / SUM(dc.quantity) AS avg_cost
                FROM deck_card dc
                JOIN card c ON dc.card_id = c.id
                WHERE dc.deck_id = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble("avg_cost");
                    return rs.wasNull() ? Optional.empty() : Optional.of(v);
                }
                return Optional.empty();
            }
        }
    }

    /** Колоды, в которых присутствует заданная карта. */
    public List<Deck> findDecksContainingCard(long cardId) throws SQLException {
        String sql = """
                SELECT d.id, d.collector_id, d.name,
                       (c.surname || ' ' || c.name) AS collector_name,
                       dc.quantity AS total_cards
                FROM deck_card dc
                JOIN deck d ON dc.deck_id = d.id
                JOIN collector c ON d.collector_id = c.id
                WHERE dc.card_id = ?
                ORDER BY d.id
                """;
        List<Deck> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    private Deck mapRow(ResultSet rs) throws SQLException {
        Deck d = new Deck(rs.getInt("id"), rs.getInt("collector_id"), rs.getString("name"));
        d.setCollectorName(rs.getString("collector_name"));
        d.setTotalCards(rs.getInt("total_cards"));
        return d;
    }
}
