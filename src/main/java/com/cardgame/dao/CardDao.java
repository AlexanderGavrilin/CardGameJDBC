package com.cardgame.dao;

import com.cardgame.db.ConnectionManager;
import com.cardgame.model.Ability;
import com.cardgame.model.Card;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для таблицы card и связи card_ability.
 */
public class CardDao {

    private static final String BASE_SELECT = """
            SELECT c.id, c.painter_id, c.deck_id, c.rarity, c.activation_cost,
                   c.magic_color, c.type, c.description,
                   (p.surname || ' ' || p.name) AS painter_name
            FROM card c
            JOIN painter p ON c.painter_id = p.id
            """;

    public List<Card> findAll() throws SQLException {
        return query(BASE_SELECT + " ORDER BY c.id");
    }

    public Optional<Card> findById(long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Card> findByColor(String color) throws SQLException {
        return queryByStringField("c.magic_color", color);
    }

    public List<Card> findByType(String type) throws SQLException {
        return queryByStringField("c.type", type);
    }

    public List<Card> findByRarity(int rarity) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.rarity = ? ORDER BY c.id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rarity);
            return collect(ps);
        }
    }

    public List<Card> findByPainter(int painterId) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.painter_id = ? ORDER BY c.id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, painterId);
            return collect(ps);
        }
    }

    /** Карты, назначенные в заданную колоду (по card.deck_id). */
    public List<Card> findByDeck(int deckId) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.deck_id = ? ORDER BY c.id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            return collect(ps);
        }
    }

    /** Карта вместе со списком её способностей. */
    public Optional<Card> findWithAbilities(long id) throws SQLException {
        Optional<Card> card = findById(id);
        if (card.isPresent()) {
            card.get().getAbilities().addAll(findAbilities(id));
        }
        return card;
    }

    public List<Ability> findAbilities(long cardId) throws SQLException {
        String sql = """
                SELECT a.id, a.description
                FROM ability a
                JOIN card_ability ca ON a.id = ca.ability_id
                WHERE ca.card_id = ?
                ORDER BY a.id
                """;
        List<Ability> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Ability(rs.getInt("id"), rs.getString("description")));
                }
            }
        }
        return result;
    }

    public long insert(Card card) throws SQLException {
        String sql = "INSERT INTO card (painter_id, deck_id, rarity, activation_cost, magic_color, type, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, card.getPainterId());
            if (card.getDeckId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, card.getDeckId());
            }
            ps.setShort(3, card.getRarity());
            ps.setShort(4, card.getActivationCost());
            ps.setString(5, card.getMagicColor());
            ps.setString(6, card.getType());
            ps.setString(7, card.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    card.setId(id);
                    return id;
                }
            }
            throw new SQLException("Не удалось получить ключ вставленной карты");
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM card WHERE id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── вспомогательные ──────────────────────────────────────────────────────

    private List<Card> queryByStringField(String column, String value) throws SQLException {
        String sql = BASE_SELECT + " WHERE " + column + " = ? ORDER BY c.id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            return collect(ps);
        }
    }

    private List<Card> query(String sql) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return collect(ps);
        }
    }

    private List<Card> collect(PreparedStatement ps) throws SQLException {
        List<Card> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    private Card mapRow(ResultSet rs) throws SQLException {
        int deckId = rs.getInt("deck_id");
        Integer deck = rs.wasNull() ? null : deckId;
        Card c = new Card(
                rs.getLong("id"),
                rs.getInt("painter_id"),
                deck,
                rs.getShort("rarity"),
                rs.getShort("activation_cost"),
                rs.getString("magic_color"),
                rs.getString("type"),
                rs.getString("description")
        );
        c.setPainterName(rs.getString("painter_name"));
        return c;
    }
}
