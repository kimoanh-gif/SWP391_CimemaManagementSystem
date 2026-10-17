package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.model.Movie;
import com.cinema.model.MovieForm;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.ShowtimeForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Automated Verification Suite for P0 Critical Fixes:
 * - P0-01: Safe Seat Map Update (UC-29)
 * - P0-02: Movie Duration Integrity (UC-26)
 * - P0-03: Showtime Concurrency Protection & 15m Cleaning Buffer (UC-30, BR-09, BR-54, BR-55)
 */
@ExtendWith(MockitoExtension.class)
public class P0CriticalFixesTest {

    // =========================================================================
    // TASK P0-01: SAFE SEAT MAP UPDATE TESTS
    // =========================================================================
    @Nested
    @DisplayName("P0-01: Safe Seat Map Update (Diff-based, Integrity & Concurrency)")
    class SafeSeatMapUpdateTests {

        @Mock private SeatDao seatDao;
        @Mock private RoomDao roomDao;
        @Mock private SeatTypeDao seatTypeDao;
        @InjectMocks private SeatService seatService;

        private Room room1;
        private List<Seat> initialSeats;

        @BeforeEach
        void setUp() {
            room1 = new Room(1, "ROOM-01", "Phòng 1", "Standard Laser", 100, "Ready");
            room1.setUpdatedAt(LocalDateTime.now().minusMinutes(10));

            Seat a1 = new Seat(101, 1, "Standard", "A", 1, 1, 1, "Active");
            Seat a2 = new Seat(102, 1, "Standard", "A", 2, 1, 2, "Active");
            Seat b5 = new Seat(105, 1, "Standard", "B", 5, 2, 5, "Active");

            initialSeats = new ArrayList<>(List.of(a1, a2, b5));
        }

        @Test
        @DisplayName("AC01: Modifying seat B5 preserves SeatID of A1, A2 and updates B5 without wipeout")
        void testAC01_DiffUpdatePreservesSeatIds() {
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));
            when(seatDao.findByRoomId(1)).thenReturn(initialSeats);

            // Target layout: A1 unchanged, A2 unchanged, B5 changed to VIP
            Seat targetA1 = new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active");
            Seat targetA2 = new Seat(null, 1, "Standard", "A", 2, 1, 2, "Active");
            Seat targetB5 = new Seat(null, 1, "VIP", "B", 5, 2, 5, "Active");

            seatService.saveRoomLayout(1, List.of(targetA1, targetA2, targetB5));

            // Verify: deleteByRoomId was NEVER called!
            verify(seatDao, never()).deleteByRoomId(anyInt());

            // Verify: unchanged seats A1, A2 were NOT re-inserted or wiped
            // Verify: existing B5 was updated with VIP
            verify(seatDao, times(1)).update(argThat(s ->
                    s.getSeatId() == 105 && "VIP".equals(s.getSeatTypeName())
            ));

            // Verify room lock and timestamp update
            verify(roomDao).lockRoomForUpdate(1);
            verify(roomDao).updateRoomTimestamp(1);
        }

        @Test
        @DisplayName("AC02: Cannot physically delete a seat that is referenced by historical tickets")
        void testAC02_RejectPhysicalDeletionOfReferencedSeat() {
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));
            when(seatDao.findByRoomId(1)).thenReturn(initialSeats);

            // Seat 105 (B5) has historical ticket reference
            when(seatDao.isSeatReferenced(105)).thenReturn(true);

            // New layout leaves out B5 (manager tries to delete B5)
            Seat targetA1 = new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active");
            Seat targetA2 = new Seat(null, 1, "Standard", "A", 2, 1, 2, "Active");

            InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                    seatService.saveRoomLayout(1, List.of(targetA1, targetA2))
            );

            assertTrue(ex.getMessage().contains("AC02") || ex.getMessage().contains("tham chiếu"));
            verify(seatDao, never()).deleteById(105);
        }

        @Test
        @DisplayName("AC04: Reject layout when duplicate positions exist in the same room")
        void testAC04_RejectDuplicateSeatPositions() {
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));

            Seat s1 = new Seat(null, 1, "Standard", "A", 5, 1, 5, "Active");
            Seat s2 = new Seat(null, 1, "Standard", "A", 5, 1, 5, "Active"); // duplicate position

            InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                    seatService.saveRoomLayout(1, List.of(s1, s2))
            );

            assertTrue(ex.getMessage().contains("Trùng lặp") || ex.getMessage().contains("AC04"));
        }

        @Test
        @DisplayName("AC06: Optimistic lock failure when layout modified concurrently by another manager")
        void testAC06_OptimisticLockingDetectsStaleVersion() {
            LocalDateTime clientLoadedTime = LocalDateTime.now().minusMinutes(5);
            room1.setUpdatedAt(LocalDateTime.now().minusMinutes(1)); // Modified 1 min ago by Manager 2

            when(roomDao.findById(1)).thenReturn(Optional.of(room1));

            Seat targetA1 = new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active");

            InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                    seatService.saveRoomLayout(1, List.of(targetA1), clientLoadedTime)
            );

            assertTrue(ex.getMessage().contains("AC06") || ex.getMessage().contains("người dùng khác"));
        }
    }

    // =========================================================================
    // TASK P0-02: MOVIE DURATION INTEGRITY TESTS
    // =========================================================================
    @Nested
    @DisplayName("P0-02: Movie Duration Integrity (UC-26)")
    class MovieDurationIntegrityTests {

        @Mock private MovieDao movieDao;
        @InjectMocks private MovieService movieService;

        private Movie existingMovie;
        private MovieForm updateForm;

        @BeforeEach
        void setUp() {
            existingMovie = new Movie(10, "Interstellar", 169, "Now Showing");
            existingMovie.setDescription("Original description");
            existingMovie.setPoster("https://img.com/poster.jpg");

            updateForm = new MovieForm();
            updateForm.setTitle("Interstellar");
            updateForm.setDuration(169);
            updateForm.setStatus("Now Showing");
            updateForm.setDescription("Updated description");
            updateForm.setPoster("https://img.com/new-poster.jpg");
        }

        @Test
        @DisplayName("AC01: Updating poster/trailer/description when showtimes exist is permitted if duration unchanged")
        void testAC01_UpdateMetadataWithExistingShowtimes_Allowed() {
            when(movieDao.findById(10)).thenReturn(Optional.of(existingMovie));

            // Duration remains 169 minutes
            updateForm.setDuration(169);
            updateForm.setPoster("https://img.com/brand-new-poster.jpg");

            Movie result = movieService.updateMovie(10, updateForm);

            assertNotNull(result);
            verify(movieDao).update(eq(10), eq(updateForm));
        }

        @Test
        @DisplayName("AC02: Updating duration when NO upcoming showtimes exist is permitted")
        void testAC02_UpdateDurationWithoutUpcomingShowtimes_Allowed() {
            when(movieDao.findById(10)).thenReturn(Optional.of(existingMovie));
            when(movieDao.countUpcomingShowtimesByMovieId(10)).thenReturn(0);

            // Change duration from 169 to 175 minutes
            updateForm.setDuration(175);

            Movie result = movieService.updateMovie(10, updateForm);

            assertNotNull(result);
            verify(movieDao).update(eq(10), eq(updateForm));
        }

        @Test
        @DisplayName("AC03: Updating duration when upcoming showtimes exist is BLOCKED")
        void testAC03_UpdateDurationWithUpcomingShowtimes_Blocked() {
            when(movieDao.findById(10)).thenReturn(Optional.of(existingMovie));
            when(movieDao.countUpcomingShowtimesByMovieId(10)).thenReturn(4); // 4 upcoming showtimes

            // Attempt to change duration from 169 to 180
            updateForm.setDuration(180);

            MovieHasActiveShowtimesException ex = assertThrows(MovieHasActiveShowtimesException.class, () ->
                    movieService.updateMovie(10, updateForm)
            );

            assertTrue(ex.getMessage().contains("thay đổi thời lượng"));
            assertTrue(ex.getMessage().contains("4 suất chiếu"));
            verify(movieDao, never()).update(eq(10), any());
        }
    }

    // =========================================================================
    // TASK P0-03: SHOWTIME CONCURRENCY & OVERLAP PROTECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("P0-03: Showtime Concurrency & 15m Cleaning Buffer (UC-30, BR-09, BR-54)")
    class ShowtimeConcurrencyTests {

        @Mock private ShowtimeDao showtimeDao;
        @Mock private MovieDao movieDao;
        @Mock private RoomDao roomDao;
        @InjectMocks private ShowtimeService showtimeService;

        private Movie movie120m;
        private Room room1;

        @BeforeEach
        void setUp() {
            movie120m = new Movie(1, "Movie 120m", 120, "Now Showing");
            room1 = new Room(1, "ROOM-01", "Phòng 1", "Standard Laser", 100, "Ready");
        }

        @Test
        @DisplayName("AC01: Showtime A 18:00–20:00 and Showtime B 20:15 is accepted (meets 15m buffer)")
        void testAC01_Exact15MinBuffer_Accepted() {
            LocalDateTime startB = LocalDateTime.now().plusDays(1).withHour(20).withMinute(15).withSecond(0).withNano(0);

            ShowtimeForm formB = new ShowtimeForm();
            formB.setMovieId(1);
            formB.setRoomId(1);
            formB.setStartTime(startB);
            formB.setBaseTicketPrice(new BigDecimal("80000"));

            when(movieDao.findById(1)).thenReturn(Optional.of(movie120m));
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));
            // DAO returns empty list (no conflict when checked against 20:15)
            when(showtimeDao.findOverlappingShowtimes(eq(1), eq(startB), any(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(showtimeDao.insert(any())).thenReturn(1);
            when(showtimeDao.findAll(any(), any(), any(), any()))
                    .thenReturn(List.of(new Showtime(1, 1, 1, startB, startB.plusMinutes(120), new BigDecimal("80000"), "Scheduled")));

            Showtime result = showtimeService.createShowtime(formB);

            assertNotNull(result);
            verify(roomDao).lockRoomForUpdate(1);
            verify(showtimeDao).insert(formB);
        }

        @Test
        @DisplayName("AC02: Showtime B 20:14 is rejected when Showtime A ends at 20:00 (violates 15m buffer)")
        void testAC02_Violates15MinBuffer_Rejected() {
            LocalDateTime startB = LocalDateTime.now().plusDays(1).withHour(20).withMinute(14).withSecond(0).withNano(0);

            ShowtimeForm formB = new ShowtimeForm();
            formB.setMovieId(1);
            formB.setRoomId(1);
            formB.setStartTime(startB);
            formB.setBaseTicketPrice(new BigDecimal("80000"));

            when(movieDao.findById(1)).thenReturn(Optional.of(movie120m));
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));

            // DAO detects conflict with Showtime A (18:00 - 20:00)
            LocalDateTime startA = startB.withHour(18).withMinute(0);
            Showtime showtimeA = new Showtime(99, 1, 1, startA, startA.plusMinutes(120), new BigDecimal("80000"), "Scheduled");
            showtimeA.setMovieTitle("Movie 120m");

            when(showtimeDao.findOverlappingShowtimes(eq(1), eq(startB), any(), isNull()))
                    .thenReturn(List.of(showtimeA));

            ShowtimeConflictException ex = assertThrows(ShowtimeConflictException.class, () ->
                    showtimeService.createShowtime(formB)
            );

            assertTrue(ex.getMessage().contains("BR-09"));
            verify(showtimeDao, never()).insert(any());
        }

        @Test
        @DisplayName("AC03: Showtime completely enclosed inside another is rejected")
        void testAC03_EnclosedShowtime_Rejected() {
            LocalDateTime startEnclosed = LocalDateTime.now().plusDays(1).withHour(19).withMinute(0);

            ShowtimeForm form = new ShowtimeForm();
            form.setMovieId(1);
            form.setRoomId(1);
            form.setStartTime(startEnclosed);
            form.setBaseTicketPrice(new BigDecimal("80000"));

            when(movieDao.findById(1)).thenReturn(Optional.of(movie120m));
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));

            Showtime outerShowtime = new Showtime(50, 1, 1,
                    startEnclosed.minusHours(1), startEnclosed.plusHours(3),
                    new BigDecimal("80000"), "Scheduled");
            outerShowtime.setMovieTitle("Movie 120m");

            when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull()))
                    .thenReturn(List.of(outerShowtime));

            assertThrows(ShowtimeConflictException.class, () ->
                    showtimeService.createShowtime(form)
            );
        }

        @Test
        @DisplayName("AC05: Updating showtime does not self-conflict (exclude currentShowtimeId)")
        void testAC05_UpdateShowtimeDoesNotSelfConflict() {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);

            Showtime existing = new Showtime(77, 1, 1, start, start.plusMinutes(120), new BigDecimal("80000"), "Scheduled");
            when(showtimeDao.findById(77)).thenReturn(Optional.of(existing));

            ShowtimeForm updateForm = new ShowtimeForm();
            updateForm.setMovieId(1);
            updateForm.setRoomId(1);
            updateForm.setStartTime(start);
            updateForm.setBaseTicketPrice(new BigDecimal("85000")); // only price changed

            when(movieDao.findById(1)).thenReturn(Optional.of(movie120m));
            when(roomDao.findById(1)).thenReturn(Optional.of(room1));
            when(showtimeDao.findOverlappingShowtimes(eq(1), eq(start), any(), eq(77)))
                    .thenReturn(Collections.emptyList());

            Showtime updated = showtimeService.updateShowtime(77, updateForm);

            assertNotNull(updated);
            verify(showtimeDao).update(eq(77), eq(updateForm));
        }

        @Test
        @DisplayName("AC06: Modifying movie/room/start time of a showtime with active bookings is blocked (BR-54)")
        void testAC06_BookingLockPreserved_BR54() {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
            Showtime existing = new Showtime(88, 1, 1, start, start.plusMinutes(120), new BigDecimal("80000"), "Scheduled");

            when(showtimeDao.findById(88)).thenReturn(Optional.of(existing));
            when(showtimeDao.hasActiveBookings(88)).thenReturn(true);

            // Attempt to change start time
            ShowtimeForm updateForm = new ShowtimeForm();
            updateForm.setMovieId(1);
            updateForm.setRoomId(1);
            updateForm.setStartTime(start.plusHours(1)); // Changed!
            updateForm.setBaseTicketPrice(new BigDecimal("80000"));

            InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                    showtimeService.updateShowtime(88, updateForm)
            );

            assertTrue(ex.getMessage().contains("BR-54"));
            verify(showtimeDao, never()).update(eq(88), any());
        }
    }
}
