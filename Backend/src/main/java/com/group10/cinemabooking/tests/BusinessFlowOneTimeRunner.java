package com.group10.cinemabooking.tests;

import com.group10.cinemabooking.dtos.BookingDto;
import com.group10.cinemabooking.dtos.BookingValidationRequestDto;
import com.group10.cinemabooking.dtos.BookingValidationResponseDto;
import com.group10.cinemabooking.dtos.BookingRequestDto;
import com.group10.cinemabooking.dtos.BookingSeatDto;
import com.group10.cinemabooking.dtos.BookingSeatRequestDto;
import com.group10.cinemabooking.dtos.CinemaDto;
import com.group10.cinemabooking.dtos.CinemaRequestDto;
import com.group10.cinemabooking.dtos.MovieDto;
import com.group10.cinemabooking.dtos.ScreeningRoomDto;
import com.group10.cinemabooking.dtos.ShowtimeDto;
import com.group10.cinemabooking.dtos.TicketDto;
import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.enums.AgeRatingEnum;
import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.enums.MovieGenreEnum;
import com.group10.cinemabooking.enums.MovieStatusEnum;
import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.enums.ShowtimeStatusEnum;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Payments;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.repository.BookingRepository;
import com.group10.cinemabooking.repository.PaymentRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.services.BookingSeatService;
import com.group10.cinemabooking.services.BookingService;
import com.group10.cinemabooking.services.CinemaService;
import com.group10.cinemabooking.services.MovieService;
import com.group10.cinemabooking.services.PaymentService;
import com.group10.cinemabooking.services.ScreeningRoomService;
import com.group10.cinemabooking.services.ShowtimeService;
import com.group10.cinemabooking.services.TicketService;
import com.group10.cinemabooking.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BusinessFlowOneTimeRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BusinessFlowOneTimeRunner.class);
    private static final String EMAIL = "quangminecraft616@gmail.com";
    private static final String NAME = "Vũ Hồng Quang";

    private final CinemaService cinemaService;
    private final ScreeningRoomService screeningRoomService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final UserService userService;
    private final BookingService bookingService;
    private final BookingSeatService bookingSeatService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${app.test.run-business-flow-once:false}")
    private boolean runBusinessFlowOnce;

    @Override
    public void run(String... args) throws Exception {
        if (!runBusinessFlowOnce) {
            return;
        }

        String marker = "flow-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("Starting one-time business flow test [{}]", marker);

        try {
            CinemaDto cinema = cinemaService.createCinema(CinemaRequestDto.builder()
                    .name("Flow Cinema " + marker)
                    .address("123 Test Street")
                    .build());
            log.info("Created cinema id={}", cinema.getCinemas_id());

            ScreeningRoomDto roomRequest = new ScreeningRoomDto();
            roomRequest.setRoom_name("Room A " + marker);
            roomRequest.setAmount_rows(3);
            roomRequest.setAmount_cols(3);
            roomRequest.setCinema_id(cinema.getCinemas_id());
            ScreeningRoomDto room = screeningRoomService.createScreeningRoom(roomRequest);
            log.info("Created room id={}", room.getRoom_id());

            MovieDto movie = new MovieDto();
            movie.setTitle("Flow Movie " + marker);
            movie.setDescription("One-time end-to-end flow validation");
            movie.setGenre(MovieGenreEnum.ACTION);
            movie.setAge_rating(AgeRatingEnum.PG13);
            movie.setStatus(MovieStatusEnum.NOW_SHOWING);
            movie.setDuration_minutes(120);
            movie.setRelease_date(new Date(System.currentTimeMillis() - 86_400_000L));
            movieService.createMovie(movie);
            MovieDto createdMovie = movieService.getMovieById(movie.getMovie_id());
            log.info("Created movie id={}", createdMovie.getMovie_id());

            Date start = new Date(System.currentTimeMillis() + 3_600_000L);
            Date end = new Date(start.getTime() + 7_200_000L);
            ShowtimeDto showtime = new ShowtimeDto();
            showtime.setStatus(ShowtimeStatusEnum.SCHEDULED);
            showtime.setStart_time(start);
            showtime.setEnd_time(end);
            showtime.setBuffer_time(15);
            showtime.setMovie_id(createdMovie.getMovie_id());
            showtime.setScreening_room_id(room.getRoom_id());
            showtime.setSeat_price(100_000L);
            ShowtimeDto createdShowtime = showtimeService.createShowtime(showtime);
            log.info("Created showtime id={}", createdShowtime.getShowtime_id());

            UsersPair usersPair = prepareUsers(marker);
            log.info("Using primary user email={}", usersPair.user1().getEmail());

            BookingDto booking1 = bookingService.createBooking(BookingRequestDto.builder()
                    .userId(usersPair.user1().getUser_id())
                    .showtimeId(createdShowtime.getShowtime_id())
                    .build());
            BookingDto booking2 = bookingService.createBooking(BookingRequestDto.builder()
                    .userId(usersPair.user2().getUser_id())
                    .showtimeId(createdShowtime.getShowtime_id())
                    .build());
            log.info("Created bookings booking1={}, booking2={}", booking1.getBookingId(), booking2.getBookingId());

            List<Seats> seats = seatRepository.findByRoomId(room.getRoom_id()).stream()
                    .sorted(Comparator.comparingInt(Seats::getSeat_row).thenComparingInt(Seats::getSeat_col))
                    .toList();
            if (seats.size() < 2) {
                throw new InvalidRequestException("Need at least 2 seats in room to run flow");
            }

            Seats seatA = seats.get(0);
            Seats seatB = seats.get(1);

            BookingSeatDto seat1 = bookingSeatService.createBookingSeat(BookingSeatRequestDto.builder()
                    .bookingId(booking1.getBookingId())
                    .seatId(seatA.getSeat_id())
                    .price(100_000L)
                    .build());
            BookingSeatDto seat2 = bookingSeatService.createBookingSeat(BookingSeatRequestDto.builder()
                    .bookingId(booking1.getBookingId())
                    .seatId(seatB.getSeat_id())
                    .price(100_000L)
                    .build());
            if (seat1.getStatus() != BookingSeatStatusEnum.LOCKED || seat2.getStatus() != BookingSeatStatusEnum.LOCKED) {
                throw new InvalidRequestException("Expected booking seats to be LOCKED after selection");
            }
            log.info("User1 selected seats {} and {}", seatA.getSeat_id(), seatB.getSeat_id());

            try {
                bookingSeatService.createBookingSeat(BookingSeatRequestDto.builder()
                        .bookingId(booking2.getBookingId())
                        .seatId(seatA.getSeat_id())
                        .price(100_000L)
                        .build());
                throw new InvalidRequestException("Seat lock test failed: user2 unexpectedly booked user1 seat");
            } catch (InvalidRequestException ex) {
                log.info("Seat lock verified: user2 cannot book seat {}. message={}", seatA.getSeat_id(), ex.getMessage());
            }

            Payments payment = Payments.builder()
                    .booking(bookingRepository.findById(booking1.getBookingId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Booking not found with id: " + booking1.getBookingId()
                            )))
                    .amount(200_000L)
                    .ref("FLOW-" + marker.toUpperCase())
                    .status(PaymentStatusEnum.PENDING)
                    .build();
            payment = paymentRepository.save(payment);
            log.info("Created local pending payment paymentId={} (PayOS skipped)", payment.getPayment_id());

            paymentService.markPaymentSuccess(payment.getPayment_id());
            log.info("Marked payment success paymentId={}", payment.getPayment_id());

            List<TicketDto> generatedTickets = ticketService.generateTicketsAfterSuccessfulPayment(payment.getPayment_id());
            if (generatedTickets.isEmpty()) {
                throw new InvalidRequestException("Expected generated tickets after successful payment");
            }
            log.info("Generated {} tickets", generatedTickets.size());

            String bookingCode = bookingRepository.findById(booking1.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found with id: " + booking1.getBookingId()
                    ))
                    .getBooking_code();

            BookingValidationResponseDto validation = ticketService.validateBookingCode(
                    BookingValidationRequestDto.builder()
                            .bookingCode(bookingCode)
                            .build()
            );
            if (!validation.isSuccess()) {
                throw new InvalidRequestException("Booking validation failed: " + validation.getMessage());
            }
            boolean allUsed = validation.getTickets() != null
                    && !validation.getTickets().isEmpty()
                    && validation.getTickets().stream().allMatch(t -> "USED".equals(t.getStatus().name()));
            if (!allUsed) {
                throw new InvalidRequestException("Expected all booking tickets to be USED after one booking-code scan");
            }
            log.info("Validated booking {} successfully. All seats marked USED in one scan", bookingCode);

            log.warn("Business flow completed [{}]. Booking confirmation email should be sent to {}", marker, usersPair.user1().getEmail());
        } catch (Exception ex) {
            log.error("Business flow failed [{}]: {}", marker, ex.getMessage(), ex);
            throw ex;
        }
    }

    private UsersPair prepareUsers(String marker) {
        UserDto user1;
        try {
            var existing = userService.getUserByEmail(EMAIL);
            user1 = new UserDto();
            user1.setUser_id(existing.getUser_id());
            user1.setEmail(existing.getEmail());
            user1.setFull_name(existing.getFull_name());
            if (!NAME.equals(existing.getFull_name())) {
                UserDto update = new UserDto();
                update.setFull_name(NAME);
                userService.updateUser(existing.getUser_id(), update);
                user1.setFull_name(NAME);
            }
        } catch (ResourceNotFoundException ex) {
            UserDto create = new UserDto();
            create.setEmail(EMAIL);
            create.setPassword("Password123!");
            create.setFull_name(NAME);
            user1 = userService.createUser(create);
        }

        UserDto user2 = new UserDto();
        user2.setEmail("flow-user2-" + marker + "@test.local");
        user2.setPassword("Password123!");
        user2.setFull_name("Flow User 2");
        user2 = userService.createUser(user2);

        return new UsersPair(user1, user2);
    }
    private record UsersPair(UserDto user1, UserDto user2) {}
}
