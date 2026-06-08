package com.cardgame.service;

import com.cardgame.dao.CardDao;
import com.cardgame.dao.DeckDao;
import com.cardgame.db.AppConfig;
import com.cardgame.model.Card;
import com.cardgame.model.Deck;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Бизнес-логика управления колодами в рамках реальной схемы:
 * связь карта↔колода хранится в card.deck_id, а заявленный размер — в deck.cards_quantity.
 * Поддерживаются: создание колоды, назначение/снятие карты (транзакция),
 * обновление cards_quantity, проверка правила "ровно N", отчёт по составу.
 */
public class DeckService {

    private final DeckDao deckDao = new DeckDao();
    private final CardDao cardDao = new CardDao();
    private final int requiredSize = AppConfig.getInt("deck.requiredSize", 60);

    public int requiredSize() {
        return requiredSize;
    }

    public int createDeck(int collectorId, int cardsQuantity) throws SQLException {
        int id = deckDao.create(collectorId, cardsQuantity);
        System.out.printf("Создана колода #%d (коллекционер #%d, заявлено карт: %d)%n",
                id, collectorId, cardsQuantity);
        return id;
    }

    /** Назначает карту в колоду, обновляя card.deck_id (в транзакции). */
    public void assignCard(int deckId, long cardId) throws SQLException {
        if (deckDao.findById(deckId).isEmpty()) {
            System.out.println("Колода #" + deckId + " не найдена.");
            return;
        }
        boolean ok = deckDao.assignCard(deckId, cardId);
        if (ok) {
            System.out.printf("Карта #%d назначена в колоду #%d. Фактически карт в колоде: %d.%n",
                    cardId, deckId, deckDao.actualCardCount(deckId));
        } else {
            System.out.printf("Карта #%d не найдена — назначение не выполнено.%n", cardId);
        }
    }

    /** Снимает карту с колоды, обнуляя card.deck_id (в транзакции). */
    public void unassignCard(long cardId) throws SQLException {
        Optional<Deck> deck = deckDao.findDeckOfCard(cardId);
        boolean ok = deckDao.unassignCard(cardId);
        if (ok) {
            int deckId = deck.map(Deck::getId).orElse(-1);
            System.out.printf("Карта #%d снята с колоды%s.%n",
                    cardId, deckId > 0 ? " #" + deckId : "");
        } else {
            System.out.printf("Карта #%d не была назначена ни в одну колоду — ничего не изменено.%n", cardId);
        }
    }

    /** Обновляет хранимое поле deck.cards_quantity. */
    public void updateCardsQuantity(int deckId, int cardsQuantity) throws SQLException {
        boolean ok = deckDao.updateCardsQuantity(deckId, cardsQuantity);
        if (ok) {
            System.out.printf("У колоды #%d поле cards_quantity обновлено на %d.%n", deckId, cardsQuantity);
        } else {
            System.out.println("Колода #" + deckId + " не найдена.");
        }
    }

    /** Проверка правила "ровно N карт" по фактически назначенным картам (card.deck_id). */
    public void validateDeckSize(int deckId) throws SQLException {
        Optional<Deck> deck = deckDao.findById(deckId);
        if (deck.isEmpty()) {
            System.out.println("Колода #" + deckId + " не найдена.");
            return;
        }
        int actual = deck.get().getActualCards();
        String verdict;
        if (actual == requiredSize) {
            verdict = "OK — ровно " + requiredSize + " карт, колода готова к игре.";
        } else if (actual < requiredSize) {
            verdict = "НЕ ХВАТАЕТ " + (requiredSize - actual) + " карт (нужно ровно " + requiredSize + ").";
        } else {
            verdict = "ЛИШНИХ " + (actual - requiredSize) + " карт (нужно ровно " + requiredSize + ").";
        }
        System.out.printf("Колода #%d: фактически %d/%d карт (заявлено cards_quantity=%d). %s%n",
                deckId, actual, requiredSize, deck.get().getCardsQuantity(), verdict);
    }

    public void listDecks() throws SQLException {
        List<Deck> decks = deckDao.findAll();
        System.out.println("=== Колоды ===");
        System.out.printf("%-5s %-24s %-12s %-12s %-8s%n",
                "ID", "Владелец", "Заявлено", "Фактически", "Правило");
        for (Deck d : decks) {
            String rule = d.getActualCards() == requiredSize ? "OK" : (d.getActualCards() + "/" + requiredSize);
            System.out.printf("%-5d %-24s %-12d %-12d %-8s%n",
                    d.getId(), d.getCollectorName(), d.getCardsQuantity(), d.getActualCards(), rule);
        }
        System.out.println();
    }

    /** Отчёт по составу колоды: список карт, назначенных через card.deck_id. */
    public void deckCompositionReport(int deckId) throws SQLException {
        Optional<Deck> deck = deckDao.findById(deckId);
        if (deck.isEmpty()) {
            System.out.println("Колода #" + deckId + " не найдена.");
            return;
        }
        Deck d = deck.get();
        System.out.printf("=== Состав колоды #%d (владелец: %s) ===%n",
                d.getId(), d.getCollectorName());
        List<Card> cards = cardDao.findByDeck(deckId);
        if (cards.isEmpty()) {
            System.out.println("В колоду не назначено ни одной карты.");
            System.out.println();
            return;
        }
        System.out.printf("%-5s %-8s %-12s %-6s %-6s %s%n",
                "ID", "Цвет", "Тип", "Редк.", "Цена", "Описание");
        for (Card c : cards) {
            System.out.printf("%-5d %-8s %-12s %-6d %-6d %s%n",
                    c.getId(), c.getMagicColor(), c.getType(), c.getRarity(),
                    c.getActivationCost(), truncate(c.getDescription(), 40));
        }
        System.out.printf("Фактически карт: %d (заявлено cards_quantity=%d, требуется ровно %d)%s%n",
                cards.size(), d.getCardsQuantity(), requiredSize,
                cards.size() == requiredSize ? " — правило соблюдено" : " — правило НЕ соблюдено");
        System.out.println();
    }

    /** Показывает, в какую колоду назначена заданная карта (если есть). */
    public void deckOfCard(long cardId) throws SQLException {
        Optional<Deck> deck = deckDao.findDeckOfCard(cardId);
        if (deck.isEmpty()) {
            System.out.printf("Карта #%d не назначена ни в одну колоду.%n%n", cardId);
            return;
        }
        Deck d = deck.get();
        System.out.printf("Карта #%d находится в колоде #%d (владелец: %s, фактически карт: %d).%n%n",
                cardId, d.getId(), d.getCollectorName(), d.getActualCards());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
