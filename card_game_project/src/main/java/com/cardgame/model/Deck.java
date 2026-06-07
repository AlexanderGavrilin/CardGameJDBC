package com.cardgame.model;

public class Deck {
    private int id;
    private int collectorId;
    private String name;

    // Заполняются при чтении с агрегатами/JOIN-ами (могут быть 0/null)
    private int totalCards;
    private String collectorName;

    public Deck() {}

    public Deck(int id, int collectorId, String name) {
        this.id = id;
        this.collectorId = collectorId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCollectorId() { return collectorId; }
    public void setCollectorId(int collectorId) { this.collectorId = collectorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }

    @Override
    public String toString() {
        return "Deck{id=" + id + ", name=" + name + ", cards=" + totalCards + "}";
    }
}
