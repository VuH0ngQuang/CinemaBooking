package com.group10.cinemabooking.configurations;

import com.group10.cinemabooking.models.BookingSeats;
import com.group10.cinemabooking.models.Bookings;
import com.group10.cinemabooking.models.Cinemas;
import com.group10.cinemabooking.models.Movies;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.models.ScreeningRooms;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.models.Showtimes;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.models.Tickets;
import com.group10.cinemabooking.utils.InAppCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConf {

    @Bean
    public InAppCache<Long, Users> userCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<String, String> tokenCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<String, Long> emailCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Movies> movieCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Showtimes> showtimeCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, ScreeningRooms> screeningRoomCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Cinemas> cinemaCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Seats> seatCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Bookings> bookingCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, BookingSeats> bookingSeatCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Payments> paymentCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<Long, Tickets> ticketCache() {
        return new InAppCache<>();
    }
}