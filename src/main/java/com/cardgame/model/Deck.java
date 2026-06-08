package com.cardgame.model;

public class Deck {
    private int id;
    private int collectorId;
    private int cardsQuantity; // хранимое поле deck.cards_quantity из схемы

    // Заполняется при чтении с JOIN-ами (может быть null)
    private String collectorName;

    // Фактическое число назначенных карт (COUNT по card.deck_id), вычисляется при чтении
    private int actualCards;

    public Deck() {}

    public Deck(int id, int collectorId, int cardsQuantity) {
        this.id = id;
        this.collectorId = collectorId;
        this.cardsQuantity = cardsQuantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCollectorId() { return collectorId; }
    public void setCollectorId(int collectorId) { this.collectorId = collectorId; }
    public int getCardsQuantity() { return cardsQuantity; }
    public void setCardsQuantity(int cardsQuantity) { this.cardsQuantity = cardsQuantity; }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
    public int getActualCards() { return actualCards; }
    public void setActualCards(int actualCards) { this.actualCards = actualCards; }

    @Override
    public String toString() {
        return "Deck{id=" + id + ", collectorId=" + collectorId
                + ", cardsQuantity=" + cardsQuantity + ", actualCards=" + actualCards + "}";
    }
}
