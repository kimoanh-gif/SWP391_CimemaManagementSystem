package com.cinema.service;

import com.cinema.controller.SeatController;
import com.cinema.controller.ShowtimeController;
import com.cinema.controller.api.GenreApiController;
import com.cinema.controller.api.MovieApiController;
import com.cinema.controller.api.RoomApiController;
import com.cinema.controller.api.ShowtimeApiController;
import com.cinema.controller.api.TicketPricingApiController;
import com.cinema.dao.FBItemDao;
import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.dto.ApiResponse;
import com.cinema.model.FBItem;
import com.cinema.dto.FBItemForm;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Showtime;
import com.cinema.dto.ShowtimeForm;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

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
 * End-to-End Regression Test Suite for P2 Requirements:
 * - P2-01: F&B Business Consistency & Combo Rules
 * - P2-02: Movie Lifecycle & Showtime Scheduling Protection
 * - P2-03: Validation and HTTP Exception Standardization (400, 404, 409, 403)
 * - P2-04: Manager UX Workflow & Contextual Parameter Navigation
 */
@ExtendWith(MockitoExtension.class)
public class P2BusinessFlowTest {

    // =========================================================================
    // TASK P2-01: F&B BUSINESS CONSISTENCY & COMBO RULES
    // =========================================================================
    @Nested
    @DisplayName("P2-01: F&B Business Consistency & Combo Rules")
    class FBBusinessConsistencyTests {

        @Mock private FBItemDao fbItemDao;
        @InjectMocks private FBItemService fbItemService;

        private FBItem singleFood;
        private FBItem singleDrink;
        private FBItem existingCombo;
        private FBItem inactiveDrink;

        @BeforeEach
        void setUp() {
            singleFood = new FBItem(10, "PC-01", "Bắp Phô Mai", "Food", "Bắp ngọt", new BigDecimal("55000"), new BigDecimal("15000"), null, "Active");
            singleDrink = new FBItem(11, "DR-01", "Coca-Cola 32oz", "Drink", "Nước ngọt", new BigDecimal("32000"), new BigDecimal("8000"), null, "Active");
            inactiveDrink = new FBItem(12, "DR-02", "Sprite 32oz", "Drink", "Tạm hết", new BigDecimal("32000"), new BigDecimal("8000"), null, "Inactive");
            existingCombo = new FBItem(20, "CB-01", "Combo Solo", "Combo", "1 Bắp + 1 Nước", new BigDecimal("79000"), new BigDecimal("23000"), null, "Active");
        }

        @Test
        @DisplayName("AC01: Combo cannot contain itself")
        void testComboCannotContainItself() {
            FBItemForm form = new FBItemForm();
            form.setItemId(20);
            form.setItemCode("CB-01");
            form.setItemName("Combo Solo");
            form.setCategory("Combo");
            form.setSellingPrice(new BigDecimal("79000"));
            form.setCostPrice(new BigDecimal("23000"));
            form.setComponentItemIds(List.of(20)); // contains itself!
            form.setComponentQuantities(List.of(1));

            when(fbItemDao.findById(20)).thenReturn(Optional.of(existingCombo));

            InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.updateItem(20, form));
            assertTrue(ex.getMessage().contains("chính nó"));
        }

        @Test
        @DisplayName("AC02: Nested combo (Combo inside Combo) is strictly disallowed")
        void testNestedComboDisallowed() {
            FBItemForm form = new FBItemForm();
            form.setItemCode("CB-SUPER");
            form.setItemName("Super Combo");
            form.setCategory("Combo");
            form.setSellingPrice(new BigDecimal("150000"));
            form.setCostPrice(new BigDecimal("45000"));
            form.setComponentItemIds(List.of(20)); // 20 is already a combo
            form.setComponentQuantities(List.of(1));

            when(fbItemDao.findById(20)).thenReturn(Optional.of(existingCombo));

            InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.createItem(form));
            assertTrue(ex.getMessage().contains("không thể lồng Combo"));
        }

        @Test
        @DisplayName("AC03: Combo component quantity must be greater than 0")
        void testComboComponentQuantityMustBePositive() {
            FBItemForm form = new FBItemForm();
            form.setItemCode("CB-PAIR");
            form.setItemName("Combo Pair");
            form.setCategory("Combo");
            form.setSellingPrice(new BigDecimal("120000"));
            form.setCostPrice(new BigDecimal("30000"));
            form.setComponentItemIds(List.of(10));
            form.setComponentQuantities(List.of(0)); // 0 quantity

            InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.createItem(form));
            assertTrue(ex.getMessage().contains("lớn hơn 0"));
        }

        @Test
        @DisplayName("AC04: Cannot add an Inactive component to a Combo")
        void testCannotAddInactiveComponentToCombo() {
            FBItemForm form = new FBItemForm();
            form.setItemCode("CB-INVALID");
            form.setItemName("Combo Thiếu Món");
            form.setCategory("Combo");
            form.setSellingPrice(new BigDecimal("99000"));
            form.setCostPrice(new BigDecimal("25000"));
            form.setComponentItemIds(List.of(12)); // Inactive component
            form.setComponentQuantities(List.of(1));

            when(fbItemDao.findById(12)).thenReturn(Optional.of(inactiveDrink));

            InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.createItem(form));
            assertTrue(ex.getMessage().contains("đang ngừng bán"));
        }

        @Test
        @DisplayName("AC05: Combo with deactivated component is marked as not available for sale")
        void testComboAvailabilityWhenComponentDeactivated() {
            FBItem combo = new FBItem(30, "CB-DUO", "Combo Duo", "Combo", "2 Bắp + 2 Nước", new BigDecimal("120000"), new BigDecimal("35000"), null, "Active");
            FBItem.ComboComponent comp1 = new FBItem.ComboComponent(10, "Bắp Phô Mai", 2, "Active");
            FBItem.ComboComponent comp2 = new FBItem.ComboComponent(12, "Sprite 32oz", 2, "Inactive"); // Deactivated!
            combo.setComboComponents(List.of(comp1, comp2));

            assertTrue(combo.hasInactiveComponents());
            assertFalse(combo.isAvailableForSale());
        }

        @Test
        @DisplayName("AC06: Negative selling price is rejected with 400 Bad Request")
        void testNegativePriceRejected() {
            assertThrows(InvalidFBItemException.class, () -> fbItemService.updatePrice(10, new BigDecimal("-1000")));
        }
    }

    // =========================================================================
    // TASK P2-02: MOVIE LIFECYCLE & SHOWTIME SCHEDULING INTEGRITY
    // =========================================================================
    @Nested
    @DisplayName("P2-02: Movie Lifecycle & Showtime Scheduling Protection")
    class MovieLifecycleTests {

        @Mock private MovieDao movieDao;
        @Mock private ShowtimeDao showtimeDao;
        @Mock private RoomDao roomDao;
        @Mock private SeatDao seatDao;

        @InjectMocks private MovieService movieService;
        private ShowtimeService showtimeService;

        private Movie activeMovie;
        private Movie endedMovie;
        private Movie archivedMovie;
        private Room readyRoom;

        @BeforeEach
        void setUp() {
            showtimeService = new ShowtimeService(showtimeDao, movieDao, roomDao, seatDao);

            activeMovie = new Movie(1, "Oppenheimer", 180, "Now Showing");
            endedMovie = new Movie(2, "Avengers: Endgame", 181, "Ended");
            archivedMovie = new Movie(3, "Old Movie 1999", 120, "Archived");

            readyRoom = new Room(1, "ROOM-01", "Phòng 1", "IMAX", 150, "Ready");
        }

        @Test
        @DisplayName("AC01: Archived or Ended movie cannot have showtimes created (BR-07)")
        void testCannotCreateShowtimeForArchivedOrEndedMovie() {
            when(movieDao.findById(3)).thenReturn(Optional.of(archivedMovie));

            ShowtimeForm form = new ShowtimeForm();
            form.setMovieId(3);
            form.setRoomId(1);
            form.setStartTime(LocalDateTime.now().plusDays(2));
            form.setBaseTicketPrice(new BigDecimal("100000"));

            InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () -> showtimeService.validateShowtime(form, null));
            assertTrue(ex.getMessage().contains("BR-07"));
        }

        @Test
        @DisplayName("AC02: Movie with upcoming showtimes cannot be archived or deleted")
        void testCannotArchiveOrDeleteMovieWithUpcomingShowtimes() {
            when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
            when(movieDao.countUpcomingShowtimesByMovieId(1)).thenReturn(4); // 4 upcoming showtimes!

            MovieHasActiveShowtimesException ex = assertThrows(
                    MovieHasActiveShowtimesException.class,
                    () -> movieService.deactivateMovie(1)
            );
            assertTrue(ex.getMessage().contains("4 suất chiếu sắp diễn ra"));

            assertThrows(
                    MovieHasActiveShowtimesException.class,
                    () -> movieService.deleteMovie(1)
            );
        }

        @Test
        @DisplayName("AC03: Movie with zero upcoming showtimes can be safely archived")
        void testCanSafelyArchiveMovieWithNoUpcomingShowtimes() {
            when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
            when(movieDao.countUpcomingShowtimesByMovieId(1)).thenReturn(0);

            Movie result = movieService.deactivateMovie(1);
            verify(movieDao).updateStatus(1, "Archived");
            assertNotNull(result);
        }

        @Test
        @DisplayName("AC04: Customer-facing movies strictly exclude Ended and Archived movies")
        void testCustomerFacingMoviesExclusion() {
            when(movieDao.findMoviesAvailableForShowtimes()).thenReturn(List.of(activeMovie));

            List<Movie> customerMovies = movieService.getCustomerMovies();
            assertEquals(1, customerMovies.size());
            assertEquals("Now Showing", customerMovies.get(0).getStatus());
            assertTrue(customerMovies.stream().noneMatch(m -> "Archived".equalsIgnoreCase(m.getStatus()) || "Ended".equalsIgnoreCase(m.getStatus())));
        }
    }

    // =========================================================================
    // TASK P2-03: VALIDATION & EXCEPTION STATUS CODE STANDARDIZATION
    // =========================================================================
    @Nested
    @DisplayName("P2-03: Validation and HTTP Exception Standardization")
    class ExceptionStandardizationTests {

        @Mock private MovieService movieService;
        @Mock private RoomService roomService;
        @Mock private ShowtimeService showtimeService;
        @Mock private TicketPricingService pricingService;
        @Mock private GenreService genreService;

        @InjectMocks private MovieApiController movieApiController;
        @InjectMocks private RoomApiController roomApiController;
        @InjectMocks private ShowtimeApiController showtimeApiController;
        @InjectMocks private TicketPricingApiController pricingApiController;
        @InjectMocks private GenreApiController genreApiController;

        @Test
        @DisplayName("AC01: MovieHasActiveShowtimesException returns HTTP 409 CONFLICT")
        void testMovieHasActiveShowtimesReturnsConflict() {
            MovieHasActiveShowtimesException ex = new MovieHasActiveShowtimesException("Conflict showtimes");
            ResponseEntity<ApiResponse<Void>> response = movieApiController.handleActiveShowtimes(ex);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("AC02: RoomHasActiveShowtimesException returns HTTP 409 CONFLICT")
        void testRoomHasActiveShowtimesReturnsConflict() {
            RoomHasActiveShowtimesException ex = new RoomHasActiveShowtimesException(1);
            ResponseEntity<ApiResponse<Void>> response = roomApiController.handleActiveShowtimes(ex);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("AC03: ShowtimeConflictException returns HTTP 409 CONFLICT")
        void testShowtimeConflictReturnsConflict() {
            ShowtimeConflictException ex = new ShowtimeConflictException("Overlap showtime");
            ResponseEntity<ApiResponse<Void>> response = showtimeApiController.handleConflict(ex);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("AC04: DuplicatePricingRuleException returns HTTP 409 CONFLICT")
        void testDuplicatePricingReturnsConflict() {
            com.cinema.exception.DuplicatePricingRuleException ex = new com.cinema.exception.DuplicatePricingRuleException("Duplicate pricing");
            ResponseEntity<ApiResponse<Void>> response = pricingApiController.handleDuplicatePricingRule(ex);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("AC05: GenreAssociationException returns HTTP 409 CONFLICT")
        void testGenreAssociationReturnsConflict() {
            GenreAssociationException ex = new GenreAssociationException("Cannot delete genre with movies");
            ResponseEntity<ApiResponse<Void>> response = genreApiController.handleAssociation(ex);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }
    }

    // =========================================================================
    // TASK P2-04: MANAGER UX WORKFLOW & CONTEXTUAL NAVIGATION
    // =========================================================================
    @Nested
    @DisplayName("P2-04: Manager UX Workflow & Contextual Navigation")
    class ManagerUXWorkflowTests {

        @Mock private ShowtimeService showtimeService;
        @Mock private MovieService movieService;
        @Mock private RoomService roomService;
        @Mock private SeatService seatService;
        @Mock private TicketPricingService pricingService;
        @Mock private HttpServletRequest request;

        @InjectMocks private ShowtimeController showtimeController;
        @InjectMocks private SeatController seatController;

        @Test
        @DisplayName("AC01: Navigating to Showtime with movieId pre-fills showtimeForm")
        void testShowtimePrefillsMovieId() {
            when(showtimeService.getAllShowtimes(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(movieService.getAllMovies(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(roomService.getAll(any(), any())).thenReturn(Collections.emptyList());

            Model model = new ExtendedModelMap();
            String view = showtimeController.viewShowtimes(null, null, 42, "ALL", model, request);

            assertEquals("manager/showtimes", view);
            ShowtimeForm form = (ShowtimeForm) model.getAttribute("showtimeForm");
            assertNotNull(form);
            assertEquals(42, form.getMovieId());
        }

        @Test
        @DisplayName("AC02: Navigating to Seats with roomId selects that specific room layout")
        void testSeatControllerSelectsSpecifiedRoom() {
            Room r1 = new Room(101, "ROOM-101", "Phòng 101", "Laser", 100, "Ready");
            Room r2 = new Room(102, "ROOM-102", "Phòng 102", "Laser", 120, "Ready");
            when(roomService.getAll(any(), any())).thenReturn(List.of(r1, r2));
            when(seatService.getSeatsByRoomId(102)).thenReturn(Collections.emptyList());
            when(pricingService.getAllSeatTypes(any())).thenReturn(Collections.emptyList());

            Model model = new ExtendedModelMap();
            String view = seatController.viewSeatLayout(102, model, request);

            assertEquals("manager/seats", view);
            Room selectedRoom = (Room) model.getAttribute("selectedRoom");
            assertNotNull(selectedRoom);
            assertEquals(102, selectedRoom.getRoomId());
        }
    }
}
