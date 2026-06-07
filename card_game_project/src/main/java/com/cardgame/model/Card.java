package com.cardgame.model;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private long id;
    private int painterId;
    private int setId;
    private String name;
    private short rarity;
    private short activationCost;
    private String magicColor;
    private String type;
    private String description;

    // Заполняются при чтении с JOIN-ами (могут быть null)
    private String painterName;
    private String setName;
    private final List<Ability> abilities = new ArrayList<>();

    public Card() {}

    public Card(long id, int painterId, int setId, String name, short rarity,
                short activationCost, String magicColor, String type, String description) {
        this.id = id;
        this.painterId = painterId;
        this.setId = setId;
        this.name = name;
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
    public int getSetId() { return setId; }
    public void setSetId(int setId) { this.setId = setId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public List<Ability> getAbilities() { return abilities; }

    @Override
    public String toString() {
        return "Card{id=" + id + ", name=" + name + ", color=" + magicColor
                + ", type=" + type + ", rarity=" + rarity + ", cost=" + activationCost + "}";
    }
}
