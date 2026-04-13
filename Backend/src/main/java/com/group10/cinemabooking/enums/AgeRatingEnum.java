package com.group10.cinemabooking.enums;

public enum AgeRatingEnum {
    G("G/All Ages"),
    PG("PG/Parental Guidance"),
    PG13("PG-13/Parents Strongly Cautioned"),
    R("R/Restricted"),
    NC17("NC-17/Adults Only");

    private final String displayName;

    AgeRatingEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
