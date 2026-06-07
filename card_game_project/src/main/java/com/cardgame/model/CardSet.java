package com.cardgame.model;

public class CardSet {
    private int id;
    private String code;
    private String name;
    private Short releaseYear;

    public CardSet() {}

    public CardSet(int id, String code, String name, Short releaseYear) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.releaseYear = releaseYear;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Short getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Short releaseYear) { this.releaseYear = releaseYear; }

    @Override
    public String toString() {
        return "CardSet{id=" + id + ", code=" + code + ", name=" + name + ", year=" + releaseYear + "}";
    }
}
