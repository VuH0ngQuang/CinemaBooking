package com.group10.cinemabooking.enums;

public enum MovieGenreEnum {
    ACTION("Action"),
    ADVENTURE("Adventure"),
    ANIMATION("Animation"),
    COMEDY("Comedy"),
    CRIME("Crime"),
    DOCUMENTARY("Documentary"),
    DRAMA("Drama"),
    FANTASY("Fantasy"),
    HORROR("Horror"),
    MYSTERY("Mystery"),
    ROMANCE("Romance"),
    SCI_FI("Science Fiction"),
    THRILLER("Thriller"),
    WAR("War"),
    WESTERN("Western"),
    MUSICAL("Musical"),
    BIOGRAPHY("Biography"),
    FAMILY("Family"),
    SUPERHERO("Superhero");

    private final String displayName;

    MovieGenreEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}