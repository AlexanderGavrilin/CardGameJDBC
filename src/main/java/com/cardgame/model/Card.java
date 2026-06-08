package com.cardgame.model;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private long id;
    private int painterId;
    private Integer deckId; // nullable: карта может не входить ни в одну колоду
    private short rarity;
    private short activationCost;
    private String magicColor;
    private String type;
    private String description;

    // Заполняются при чтении с JOIN-ами (могут быть null)
    private String painterName;
    private final List<Ability> abilities = new ArrayList<>();

    public Card() {}

    public Card(long id, int painterId, Integer deckId, short rarity,
                short activationCost, String magicColor, String type, String description) {
        this.id = id;
        this.painterId = painterId;
        this.deckId = deckId;
        this.rarity = rarity;
        this.activationCost = activationCost;
        this.magicColor = magicColor;
        this.type = type;
        this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getPainterId() { return painterId; }
    public void setPainterId(int painterId) { this.painterId = painterId; }
    public Integer getDeckId() { return deckId; }
    public void setDeckId(Integer deckId) { this.deckId = deckId; }
    public short getRarity() { return rarity; }
    public void setRarity(short rarity) { this.rarity = rarity; }
    public short getActivationCost() { return activationCost; }
    public void setActivationCost(short activationCost) { this.activationCost = activationCost; }
    public String getMagicColor() { return magicColor; }
    public void setMagicColor(String magicColor) { this.magicColor = magicColor; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPainterName() { return painterName; }
    public void setPainterName(String painterName) { this.painterName = painterName; }
    public List<Ability> getAbilities() { return abilities; }

    @Override
    public String toString() {
        return "Card{id=" + id + ", color=" + magicColor + ", type=" + type
                + ", rarity=" + rarity + ", cost=" + activationCost
                + ", deckId=" + deckId + "}";
    }
}
