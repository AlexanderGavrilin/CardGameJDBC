package com.cardgame;

import com.cardgame.db.AppConfig;
import com.cardgame.db.ConnectionManager;
import com.cardgame.service.CardQueryService;
import com.cardgame.service.DeckService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Точка входа: консольное меню реестра карточной игры.
 * При недоступной БД приложение завершает работу с понятным сообщением,
 * а не падает со stacktrace.
 */
public class Main {

    private static final CardQueryService cardService = new CardQueryService();
    private static final DeckService deckService = new DeckService();

    public static void main(String[] args) {
        System.out.println("=== Реестр карточной игры (Java 21 · PostgreSQL · JDBC · HikariCP) ===\n");

        if (!ensureDatabase()) {
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMainMenu();
            try {
                switch (read(scanner)) {
                    case "1" -> runSearchMenu(scanner);
                    case "2" -> runStatsMenu(scanner);
                    case "3" -> runDeckMenu(scanner);
                    case "4" -> runDemo();
                    case "0" -> running = false;
                    default  -> System.out.println("Неверный выбор.\n");
                }
            } catch (SQLException e) {
                System.err.println("Ошибка SQL: " + e.getMessage() + "\n");
            }
        }

        System.out.println("До свидания!");
        ConnectionManager.close();
    }

    /**
     * Проверяет доступность уже существующей БД и схемы card_game.
     * @return true, если БД готова к работе
     */
    private static boolean ensureDatabase() {
        try {
            if (!schemaExists()) {
                System.err.println("Схема " + ConnectionManager.schema()
                        + " не найдена.");
                ConnectionManager.close();
                return false;
            }
            System.out.println("БД готова.\n");
            return true;
        } catch (SQLException e) {
            printConnectionHelp(e.getMessage());
            return false;
        } catch (RuntimeException e) {
            // HikariCP оборачивает ошибки соединения в RuntimeException
            printConnectionHelp(e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, что схема уже инициализирована (есть таблица card).
     */
    private static boolean schemaExists() throws SQLException {
        String sql = "SELECT to_regclass(?) IS NOT NULL AS present";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ConnectionManager.schema() + ".card");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("present");
            }
        }
    }

    private static void printConnectionHelp(String detail) {
        System.err.println("Не удалось подключиться к PostgreSQL.");
        System.err.println("Причина: " + detail);
        System.err.println();
        System.err.println("Проверьте, что:");
        System.err.println("  • PostgreSQL запущен на " + AppConfig.get("db.url"));
        System.err.println("  • БД card_game_bd существует");
        System.err.println("  • логин/пароль в src/main/resources/application.properties верны");
        System.err.println("Подробности — в README.md.");
        ConnectionManager.close();
    }

    // ── Меню ────────────────────────────────────────────────────────────────────

    private static void printMainMenu() {
        System.out.print("""
                ┌─────────────────────────────────────────────┐
                │ [1] Поиск карт                              │
                │ [2] Статистика                              │
                │ [3] Колоды                                  │
                │ [4] Демонстрация всех запросов              │
                │ [0] Выход                                   │
                └─────────────────────────────────────────────┘
                > """);
    }

    private static void runSearchMenu(Scanner sc) throws SQLException {
        while (true) {
            System.out.print("""
                    -- Поиск карт --
                    [1] По цвету      [2] По редкости   [3] По типу
                    [4] По художнику  [5] Все карты
                    [6] Карта со способностями          [0] Назад
                    > """);
            switch (read(sc)) {
                case "1" -> { System.out.print("Цвет [black/white/blue/red/green]: ");
                              cardService.findByColor(read(sc)); }
                case "2" -> { System.out.print("Редкость (1-10): ");
                              cardService.findByRarity(parseInt(read(sc), 1)); }
                case "3" -> { System.out.print("Тип [offensive/defensive/additional]: ");
                              cardService.findByType(read(sc)); }
                case "4" -> { System.out.print("ID художника: ");
                              cardService.findByPainter(parseInt(read(sc), 1)); }
                case "5" -> cardService.listAll();
                case "6" -> { System.out.print("ID карты: ");
                              cardService.showCardWithAbilities(parseLong(read(sc), 1)); }
                case "0" -> { return; }
                default  -> System.out.println("Неверный выбор.\n");
            }
        }
    }

    private static void runStatsMenu(Scanner sc) throws SQLException {
        while (true) {
            System.out.print("""
                    -- Статистика --
                    [1] По цветам   [2] По редкостям   [3] По типам
                    [4] По художникам                  [0] Назад
                    > """);
            switch (read(sc)) {
                case "1" -> cardService.statsByColor();
                case "2" -> cardService.statsByRarity();
                case "3" -> cardService.statsByType();
                case "4" -> cardService.painterReport();
                case "0" -> { return; }
                default  -> System.out.println("Неверный выбор.\n");
            }
        }
    }

    private static void runDeckMenu(Scanner sc) throws SQLException {
        while (true) {
            System.out.print("""
                    -- Колоды --
                    [1] Список колод            [2] Состав колоды
                    [3] Создать колоду          [4] Назначить карту в колоду
                    [5] Снять карту с колоды     [6] Проверить правило (ровно N)
                    [7] Обновить cards_quantity  [8] Колода карты
                    [0] Назад
                    > """);
            switch (read(sc)) {
                case "1" -> deckService.listDecks();
                case "2" -> { System.out.print("ID колоды: ");
                              deckService.deckCompositionReport(parseInt(read(sc), 1)); }
                case "3" -> {
                    System.out.print("ID коллекционера: ");
                    int cid = parseInt(read(sc), 1);
                    System.out.print("Заявленное число карт (cards_quantity): ");
                    int q = parseInt(read(sc), 0);
                    deckService.createDeck(cid, q);
                }
                case "4" -> {
                    System.out.print("ID колоды: "); int d = parseInt(read(sc), 1);
                    System.out.print("ID карты: ");   long c = parseLong(read(sc), 1);
                    deckService.assignCard(d, c);
                }
                case "5" -> {
                    System.out.print("ID карты: "); long c = parseLong(read(sc), 1);
                    deckService.unassignCard(c);
                }
                case "6" -> { System.out.print("ID колоды: ");
                              deckService.validateDeckSize(parseInt(read(sc), 1)); }
                case "7" -> {
                    System.out.print("ID колоды: "); int d = parseInt(read(sc), 1);
                    System.out.print("Новое cards_quantity: "); int q = parseInt(read(sc), 0);
                    deckService.updateCardsQuantity(d, q);
                }
                case "8" -> { System.out.print("ID карты: ");
                              deckService.deckOfCard(parseLong(read(sc), 1)); }
                case "0" -> { return; }
                default  -> System.out.println("Неверный выбор.\n");
            }
        }
    }

    /** Прогон ключевых бизнес-запросов на демо-данных. */
    private static void runDemo() throws SQLException {
        System.out.println("\n########## ДЕМОНСТРАЦИЯ ##########\n");
        cardService.listAll();
        cardService.findByColor("black");
        cardService.findByRarity(5);
        cardService.findByType("offensive");
        cardService.findByPainter(1);
        cardService.showCardWithAbilities(1);
        cardService.statsByColor();
        cardService.statsByRarity();
        cardService.statsByType();
        cardService.painterReport();
        deckService.listDecks();
        deckService.deckCompositionReport(1);
        deckService.validateDeckSize(1);
        deckService.validateDeckSize(2);
        deckService.deckOfCard(1);
        System.out.println("########## КОНЕЦ ДЕМОНСТРАЦИИ ##########\n");
    }

    // ── ввод ──────────────────────────────────────────────────────────────────

    private static String read(Scanner sc) {
        return sc.hasNextLine() ? sc.nextLine().trim() : "0";
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
