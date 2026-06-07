package com.cardgame.service;

import com.cardgame.dao.DeckDao;
import com.cardgame.db.AppConfig;
import com.cardgame.model.Card;
import com.cardgame.model.Deck;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Бизнес-логика управления колодами: создание, добавление/удаление экземпляров
 * карт, проверка правила "ровно N карт", отчёты по составу и стоимости.
 */
public class DeckService {

    private final DeckDao deckDao = new DeckDao();
    private final int requiredSize = AppConfig.getInt("deck.requiredSize", 60);

    public int requiredSize() {
        return requiredSize;
    }

    public int createDeck(int collectorId, String name) throws SQLException {
        int id = deckDao.create(collectorId, name);
        System.out.println("Создана колода #" + id + " «" + name + "»");
        return id;
    }

    public void addCards(int deckId, long cardId, int quantity) throws SQLException {
        deckDao.addCards(deckId, cardId, quantity);
        System.out.printf("В колоду #%d добавлено %d экз. карты #%d. Теперь в колоде: %d карт.%n",
                deckId, quantity, cardId, deckDao.totalCardCount(deckId));
    }

    public void removeCards(int deckId, long cardId, int quantity) throws SQLException {
        boolean changed = deckDao.removeCards(deckId, cardId, quantity);
        if (changed) {
            System.out.printf("Из колоды #%d удалено до %d экз. карты #%d. Теперь в колоде: %d карт.%n",
                    deckId, quantity, cardId, deckDao.totalCardCount(deckId));
        } else {
            System.out.printf("Карты #%d нет в колоде #%d — ничего не удалено.%n", cardId, deckId);
        }
    }

    /** Проверка правила "ровно N карт". */
    public void validateDeckSize(int deckId) throws SQLException {
        Optional<Deck> deck = deckDao.findById(deckId);
        if (deck.isEmpty()) {
            System.out.println("Колода #" + deckId + " не найдена.");
            return;
        }
        int total = deckDao.totalCardCount(deckId);
        String verdict;
        if (total == requiredSize) {
            verdict = "OK — ровно " + requiredSize + " карт, колода готова к игре.";
        } else if (total < requiredSize) {
            verdict = "НЕ ХВАТАЕТ " + (requiredSize - total) + " карт (нужно ровно " + requiredSize + ").";
        } else {
            verdict = "ЛИШНИХ " + (total - requiredSize) + " карт (нужно ровно " + requiredSize + ").";
        }
        System.out.printf("Колода «%s»: %d/%d карт. %s%n",
                deck.get().getName(), total, requiredSize, verdict);
    }

    public void listDecks() throws SQLException {
        List<Deck> decks = deckDao.findAll();
        System.out.println("=== Колоды ===");
        System.out.printf("%-5s %-24s %-22s %-8s %-8s%n", "ID", "Название", "Владелец", "Карт", "Правило");
        for (Deck d : decks) {
            String rule = d.getTotalCards() == requiredSize ? "OK" : (d.getTotalCards() + "/" + requiredSize);
            System.out.printf("%-5d %-24s %-22s %-8d %-8s%n",
                    d.getId(), d.getName(), d.getCollectorName(), d.getTotalCards(), rule);
        }
        System.out.println();
    }

    /** Отчёт по составу колоды. */
    public void deckCompositionReport(int deckId) throws SQLException {
        Optional<Deck> deck = deckDao.findById(deckId);
        if (deck.isEmpty()) {
            System.out.println("Колода #" + deckId + " не найдена.");
            return;
        }
        System.out.println("=== Состав колоды «" + deck.get().getName() + "» (владелец: "
                + deck.get().getCollectorName() + ") ===");
        Map<Card, Integer> comp = deckDao.composition(deckId);
        if (comp.isEmpty()) {
            System.out.println("Колода пуста.");
            System.out.println();
            return;
        }
        System.out.printf("%-6s %-22s %-8s %-8s %-10s %-6s%n",
                "Кол-во", "Карта", "Цвет", "Тип", "Сет", "Цена");
        int total = 0;
        for (Map.Entry<Card, Integer> e : comp.entrySet()) {
            Card c = e.getKey();
            int qty = e.getValue();
            total += qty;
            System.out.printf("%-6d %-22s %-8s %-8s %-10s %-6d%n",
                    qty, truncate(c.getName(), 21), c.getMagicColor(),
                    c.getType(), truncate(c.getSetName(), 9), c.getActivationCost());
        }
        System.out.println("Итого карт: " + total + " / " + requiredSize
                + (total == requiredSize ? " (правило соблюдено)" : " (правило НЕ соблюдено)"));
        averageActivationCost(deckId);
        System.out.println();
    }

    /** Анализ средней стоимости активации колоды. */
    public void averageActivationCost(int deckId) throws SQLException {
        Optional<Double> avg = deckDao.averageActivationCost(deckId);
        if (avg.isEmpty()) {
            System.out.println("Средняя стоимость активации: нет карт.");
        } else {
            System.out.printf("Средняя стоимость активации (взвешенная): %.2f%n", avg.get());
        }
    }

    /** Поиск колод, содержащих заданную карту. */
    public void decksContainingCard(long cardId) throws SQLException {
        System.out.println("=== Колоды, содержащие карту #" + cardId + " ===");
        List<Deck> decks = deckDao.findDecksContainingCard(cardId);
        if (decks.isEmpty()) {
            System.out.println("Эта карта не входит ни в одну колоду.");
            System.out.println();
            return;
        }
        System.out.printf("%-5s %-24s %-22s %-8s%n", "ID", "Колода", "Владелец", "Экз.");
        for (Deck d : decks) {
            System.out.printf("%-5d %-24s %-22s %-8d%n",
                    d.getId(), d.getName(), d.getCollectorName(), d.getTotalCards());
        }
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
