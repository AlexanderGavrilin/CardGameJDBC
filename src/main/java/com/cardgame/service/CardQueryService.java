package com.cardgame.service;

import com.cardgame.dao.CardDao;
import com.cardgame.db.ConnectionManager;
import com.cardgame.model.Ability;
import com.cardgame.model.Card;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * Бизнес-запросы по картам: поиск (цвет/редкость/тип/художник),
 * просмотр карты со способностями и сводная статистика.
 */
public class CardQueryService {

    private final CardDao cardDao = new CardDao();

    public void findByColor(String color) throws SQLException {
        printCards("Карты цвета «" + color + "»", cardDao.findByColor(color));
    }

    public void findByRarity(int rarity) throws SQLException {
        printCards("Карты редкости " + rarity, cardDao.findByRarity(rarity));
    }

    public void findByType(String type) throws SQLException {
        printCards("Карты типа «" + type + "»", cardDao.findByType(type));
    }

    public void findByPainter(int painterId) throws SQLException {
        printCards("Карты художника #" + painterId, cardDao.findByPainter(painterId));
    }

    public void listAll() throws SQLException {
        printCards("Все карты", cardDao.findAll());
    }

    /** Просмотр карты вместе со всеми её способностями. */
    public void showCardWithAbilities(long cardId) throws SQLException {
        Optional<Card> card = cardDao.findWithAbilities(cardId);
        if (card.isEmpty()) {
            System.out.println("Карта #" + cardId + " не найдена.");
            return;
        }
        Card c = card.get();
        System.out.println("=== Карта #" + c.getId() + " ===");
        System.out.println("Цвет:       " + c.getMagicColor());
        System.out.println("Тип:        " + c.getType());
        System.out.println("Редкость:   " + c.getRarity());
        System.out.println("Стоимость:  " + c.getActivationCost());
        System.out.println("Художник:   " + c.getPainterName());
        System.out.println("Колода:     " + (c.getDeckId() == null ? "— (не назначена)" : "#" + c.getDeckId()));
        System.out.println("Описание:   " + (c.getDescription() == null ? "—" : c.getDescription()));
        System.out.println("Способности (" + c.getAbilities().size() + "):");
        if (c.getAbilities().isEmpty()) {
            System.out.println("  (нет)");
        } else {
            for (Ability a : c.getAbilities()) {
                System.out.println("  • [" + a.getId() + "] " + a.getDescription());
            }
        }
        System.out.println();
    }

    // ── Статистика ─────────────────────────────────────────────────────────────

    public void statsByColor() throws SQLException {
        runStatGroup("Статистика по цветам магии", "magic_color");
    }

    public void statsByType() throws SQLException {
        runStatGroup("Статистика по типам", "type");
    }

    public void statsByRarity() throws SQLException {
        System.out.println("=== Статистика по редкостям ===");
        String sql = """
                SELECT rarity,
                       COUNT(*) AS cards,
                       ROUND(AVG(activation_cost), 2) AS avg_cost
                FROM card
                GROUP BY rarity
                ORDER BY rarity
                """;
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-10s %-8s %-12s%n", "Редкость", "Карт", "Ср. цена");
            while (rs.next()) {
                System.out.printf("%-10d %-8d %-12.2f%n",
                        rs.getInt("rarity"), rs.getInt("cards"), rs.getBigDecimal("avg_cost"));
            }
        }
        System.out.println();
    }

    /** Отчёт по художникам и числу их карт. */
    public void painterReport() throws SQLException {
        System.out.println("=== Отчёт по художникам ===");
        String sql = """
                SELECT p.id,
                       (p.surname || ' ' || p.name) AS painter,
                       COUNT(c.id) AS cards,
                       ROUND(AVG(c.rarity), 2) AS avg_rarity
                FROM painter p
                LEFT JOIN card c ON p.id = c.painter_id
                GROUP BY p.id, painter
                ORDER BY cards DESC, painter
                """;
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-26s %-8s %-12s%n", "ID", "Художник", "Карт", "Ср. редк.");
            while (rs.next()) {
                System.out.printf("%-5d %-26s %-8d %-12.2f%n",
                        rs.getInt("id"), rs.getString("painter"),
                        rs.getInt("cards"), rs.getBigDecimal("avg_rarity"));
            }
        }
        System.out.println();
    }

    private void runStatGroup(String title, String column) throws SQLException {
        System.out.println("=== " + title + " ===");
        String sql = "SELECT " + column + " AS grp, COUNT(*) AS cards, "
                + "ROUND(AVG(activation_cost), 2) AS avg_cost, "
                + "ROUND(AVG(rarity), 2) AS avg_rarity "
                + "FROM card GROUP BY " + column + " ORDER BY cards DESC";
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-14s %-8s %-12s %-12s%n", "Группа", "Карт", "Ср. цена", "Ср. редк.");
            while (rs.next()) {
                System.out.printf("%-14s %-8d %-12.2f %-12.2f%n",
                        rs.getString("grp"), rs.getInt("cards"),
                        rs.getBigDecimal("avg_cost"), rs.getBigDecimal("avg_rarity"));
            }
        }
        System.out.println();
    }

    private void printCards(String title, List<Card> cards) {
        System.out.println("=== " + title + " (" + cards.size() + ") ===");
        if (cards.isEmpty()) {
            System.out.println("Ничего не найдено.");
            System.out.println();
            return;
        }
        System.out.printf("%-5s %-8s %-12s %-6s %-6s %-8s %-26s %s%n",
                "ID", "Цвет", "Тип", "Редк.", "Цена", "Колода", "Художник", "Описание");
        for (Card c : cards) {
            System.out.printf("%-5d %-8s %-12s %-6d %-6d %-8s %-26s %s%n",
                    c.getId(), c.getMagicColor(), c.getType(), c.getRarity(),
                    c.getActivationCost(),
                    c.getDeckId() == null ? "—" : ("#" + c.getDeckId()),
                    truncate(c.getPainterName(), 25),
                    truncate(c.getDescription(), 40));
        }
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
