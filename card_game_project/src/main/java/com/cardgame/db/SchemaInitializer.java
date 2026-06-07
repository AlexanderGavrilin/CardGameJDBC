package com.cardgame.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Создаёт схему card_game из schema.sql и заполняет её демо-данными.
 * Все вставки идут через PreparedStatement; данные одной колоды заливаются
 * в одной транзакции.
 */
public final class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private SchemaInitializer() {}

    public static void initialize() throws SQLException {
        log.info("Инициализация схемы card_game...");
        executeSqlFile("schema.sql");
        seed();
        log.info("Схема card_game создана и заполнена демо-данными");
    }

    private static void executeSqlFile(String fileName) throws SQLException {
        String sql;
        try (InputStream is = SchemaInitializer.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) throw new IllegalStateException("SQL-файл не найден: " + fileName);
            sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка чтения SQL-файла: " + fileName, e);
        }
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Выполнен SQL-файл: {}", fileName);
        }
    }

    private static void seed() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                seedCollectors(conn);
                seedPainters(conn);
                seedSets(conn);
                seedAbilities(conn);
                seedCards(conn);
                seedCardAbilities(conn);
                seedDecks(conn);
                seedDeckCards(conn);
                conn.commit();
                log.info("Демо-данные загружены");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void seedCollectors(Connection conn) throws SQLException {
        String sql = "INSERT INTO collector (name, surname, patronymic) VALUES (?, ?, ?)";
        String[][] rows = {
                {"Александр", "Гаврилин", "Сергеевич"},
                {"Данила", "Половинкин", "Владимирович"},
                {"Иван", "Иванов", "Иванович"},
                {"Сергей", "Сергеев", "Сергеевич"},
                {"София", "Сонная", "Николаевна"}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : rows) {
                ps.setString(1, r[0]);
                ps.setString(2, r[1]);
                ps.setString(3, r[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedPainters(Connection conn) throws SQLException {
        String sql = "INSERT INTO painter (name, surname, patronymic) VALUES (?, ?, ?)";
        String[][] rows = {
                {"Егор", "Кузнецов", "Михайлович"},
                {"Олег", "Олегов", "Олегович"},
                {"Арсений", "Арсеньев", "Арсеньевич"},
                {"Мария", "Красная", "Владимировна"},
                {"Анна", "Стольная", "Даниловна"}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : rows) {
                ps.setString(1, r[0]);
                ps.setString(2, r[1]);
                ps.setString(3, r[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedSets(Connection conn) throws SQLException {
        String sql = "INSERT INTO card_set (code, name, release_year) VALUES (?, ?, ?)";
        Object[][] rows = {
                {"ALP", "Alpha Origins", (short) 2018},
                {"DRK", "Dark Tides", (short) 2020},
                {"LGT", "Light Ascendant", (short) 2021},
                {"AZR", "Azure Depths", (short) 2023}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setShort(3, (Short) r[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedAbilities(Connection conn) throws SQLException {
        String sql = "INSERT INTO ability (description) VALUES (?)";
        String[] rows = {
                "Наносит 10 урона здоровью",
                "Восстанавливает 10 единиц здоровья",
                "Наносит 5 урона магии",
                "Позволяет положить две карты одновременно с этой",
                "Наносит 20 урона здоровью противника и 5 урона себе",
                "Блокирует следующую атаку противника",
                "Добирает одну карту из колоды"
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String d : rows) {
                ps.setString(1, d);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedCards(Connection conn) throws SQLException {
        String sql = "INSERT INTO card (painter_id, set_id, name, rarity, activation_cost, magic_color, type, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        // painterId, setId, name, rarity, cost, color, type, description
        Object[][] rows = {
                {2, 2, "Теневой клинок",     (short) 3, (short) 4, "black", "offensive",  "Наносит 5 урона магии"},
                {1, 3, "Светлый страж",      (short) 2, (short) 5, "white", "defensive",  "Восстанавливает 10 единиц здоровья"},
                {5, 4, "Лазурный мудрец",    (short) 6, (short) 6, "blue",  "additional", "Позволяет положить две карты одновременно"},
                {4, 2, "Тёмный палач",       (short) 5, (short) 3, "black", "offensive",  "Наносит 20 урона противнику и 5 себе"},
                {3, 1, "Алый берсерк",       (short) 4, (short) 2, "red",   "offensive",  "Яростная атака ближнего боя"},
                {1, 3, "Лесной хранитель",   (short) 2, (short) 3, "green", "defensive",  "Регенерация в начале хода"},
                {5, 4, "Морской оракул",     (short) 7, (short) 7, "blue",  "additional", "Предсказывает верхнюю карту колоды"},
                {2, 1, "Пепельный демон",    (short) 8, (short) 8, "black", "offensive",  "Призывает огненный шторм"},
                {4, 3, "Небесный целитель",  (short) 3, (short) 4, "white", "defensive",  "Лечит всех союзников"},
                {3, 2, "Багровый дракон",    (short) 9, (short) 9, "red",   "offensive",  "Дыхание дракона по площади"},
                {1, 4, "Изумрудный голем",   (short) 5, (short) 6, "green", "defensive",  "Высокая прочность"},
                {5, 1, "Кристальный архимаг",(short) 10,(short) 10,"blue",  "additional", "Удваивает следующий эффект"}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] r : rows) {
                ps.setInt(1, (Integer) r[0]);
                ps.setInt(2, (Integer) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setShort(4, (Short) r[3]);
                ps.setShort(5, (Short) r[4]);
                ps.setString(6, (String) r[5]);
                ps.setString(7, (String) r[6]);
                ps.setString(8, (String) r[7]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedCardAbilities(Connection conn) throws SQLException {
        String sql = "INSERT INTO card_ability (card_id, ability_id) VALUES (?, ?)";
        // Несколько эффектов у некоторых карт
        int[][] rows = {
                {1, 3}, {1, 1},
                {2, 2}, {2, 6},
                {3, 4}, {3, 7},
                {4, 5},
                {5, 1},
                {6, 2}, {6, 6},
                {7, 7}, {7, 4},
                {8, 5}, {8, 1},
                {9, 2},
                {10, 1}, {10, 5},
                {11, 6},
                {12, 4}, {12, 7}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int[] r : rows) {
                ps.setLong(1, r[0]);
                ps.setInt(2, r[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedDecks(Connection conn) throws SQLException {
        String sql = "INSERT INTO deck (collector_id, name) VALUES (?, ?)";
        Object[][] rows = {
                {1, "Тьма и пепел"},        // станет ровно 60 карт
                {2, "Светлый легион"},      // неполная
                {5, "Лазурный прилив"}      // неполная
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] r : rows) {
                ps.setInt(1, (Integer) r[0]);
                ps.setString(2, (String) r[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedDeckCards(Connection conn) throws SQLException {
        String sql = "INSERT INTO deck_card (deck_id, card_id, quantity) VALUES (?, ?, ?)";
        // Колода 1 "Тьма и пепел": ровно 60 карт (15+10+12+8+8+7)
        // Колода 2/3: неполные — чтобы показать проверку правила 60.
        int[][] rows = {
                {1, 1, 15}, {1, 4, 10}, {1, 8, 12}, {1, 5, 8}, {1, 10, 8}, {1, 2, 7},
                {2, 2, 20}, {2, 9, 18}, {2, 6, 10},
                {3, 3, 12}, {3, 7, 9}, {3, 12, 6}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int[] r : rows) {
                ps.setInt(1, r[0]);
                ps.setLong(2, r[1]);
                ps.setInt(3, r[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Проверяет, доступна ли уже инициализированная схема (есть ли таблица card).
     */
    public static boolean schemaExists() {
        String sql = "SELECT to_regclass(?) IS NOT NULL AS present";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ConnectionManager.schema() + ".card");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("present");
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
