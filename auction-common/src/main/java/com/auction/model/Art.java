package com.auction.model;

public class Art extends Item{
    private static final long serialVersionUID = 1L;
    private String artist;
    private int yearCreated;

    public Art(Integer id) {
        super(id);
        setCategory("ART");
    }
    public Art(Integer id, String name, String description, Seller owner, String artist, int yearCreated) {
        super(id, name, description, "ART", owner);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public String getDetails() {
        return String.format("Nghệ sĩ: %s | Năm sáng tác: %d", artist, yearCreated);
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }
}
