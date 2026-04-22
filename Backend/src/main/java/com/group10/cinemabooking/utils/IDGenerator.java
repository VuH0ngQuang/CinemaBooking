package com.group10.cinemabooking.utils;

import java.security.SecureRandom;

public class IDGenerator {

    /**
     * THIS IS A PREFIX NUMBER TO DEFINE WHICH ID IS USED BY WHICH ENTITY.
     *
     *  1  USERS
     *  2  CINEMAS
     *  3  BOOKINGS
     *  4  BookingSeats
     *  5  MOVIES
     *  6  PAYMENTS
     *  7  SCREENINGROOMS
     *  8  SEATS
     *  9  SHOWTIMES
     * 10  SHOWTIMESEATS
     * 11  TICKETS
     * 12  TICKETSVALIDATIONS
     * 13  AUTHSESSIONS
     *
     * All generated IDs have the format: [PREFIX][RANDOM_PART],
     * where RANDOM_PART is always 8 random digits (e.g. prefix=7 -> 7xxxxxxxx).
     */

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Base method: generate an ID as long with a numeric prefix plus 8 random digits.
     *
     * @param prefix numeric prefix that identifies the entity type (1–10)
     * @return ID as long (e.g. prefix=1 -> 1xxxxxxxx, prefix=10 -> 10xxxxxxxx)
     */
    public static long generateWithPrefix(int prefix) {
        String prefixStr = String.valueOf(prefix);

        if (prefix <= 0) {
            throw new IllegalArgumentException("Prefix must be positive");
        }

        // Always generate an 8-digit random part: 10_000_000 to 99_999_999
        int min = 10_000_000;
        int max = 99_999_999;

        int randomPart = min + RANDOM.nextInt((max - min) + 1);

        String idStr = prefixStr + randomPart;                    // concatenate prefix + random
        return Long.parseLong(idStr);
    }

    // Convenience methods for each entity type

    public static long generateUserId() {
        return generateWithPrefix(1);
    }

    public static long generateCinemaId() {
        return generateWithPrefix(2);
    }

    public static long generateBookingId() {
        return generateWithPrefix(3);
    }

    public static long generateBookingSeatId() {
        return generateWithPrefix(4);
    }

    public static long generateMovieId() {
        return generateWithPrefix(5);
    }

    public static long generatePaymentId() {
        return generateWithPrefix(6);
    }

    public static long generateScreeningRoomId() {
        return generateWithPrefix(7);
    }

    public static long generateSeatId() {
        return generateWithPrefix(8);
    }

    public static long generateShowtimeId() {
        return generateWithPrefix(9);
    }

    public static long generateShowtimeSeatId() {
        return generateWithPrefix(10);
    }

    public static long generateTicketId() {
        return generateWithPrefix(11);
    }

    public static long generateTicketValidationId() {
        return generateWithPrefix(12);
    }

    public static long generateAuthSessionId() {
        return generateWithPrefix(13);
    }
}