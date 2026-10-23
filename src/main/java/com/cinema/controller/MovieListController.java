package com.cinema.controller;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

/**
 * Screen: Movie List ( /movies )
 * Use case: UC01 — View Movie List
 *
 * Filters: movie status (All / Now Showing / Coming Soon), keyword, genre, age rating, sort.
 * When "All" is selected the result is grouped into Now Showing and Coming Soon sections,
 * each with its own empty state (BR-ML-02, BR-ML-04).
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class MovieListController {

    private static final int MAX_KEYWORD_LENGTH = 100;

    private final MovieCatalogService movieService;
    private final PublicPageModelHelper pageHelper;

    public MovieListController(MovieCatalogService movieService, PublicPageModelHelper pageHelper) {
        this.movieService = movieService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/movies")
    public String listMovies(
            @RequestParam(name = "type", required = false, defaultValue = "all") String type,
            @RequestParam(name = "genreId", required = false) Integer genreId,
            @RequestParam(name = "ageRating", required = false) String ageRating,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sortBy,
            HttpSession session,
            Model model
    ) {
        pageHelper.populateHeader(session, model, "movies");

        String selectedType = movieService.normalizeType(type);
        String selectedSort = movieService.normalizeSort(sortBy);
        String selectedAgeRating = movieService.normalizeAgeRating(ageRating);
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        if (cleanKeyword.length() > MAX_KEYWORD_LENGTH) {
            cleanKeyword = cleanKeyword.substring(0, MAX_KEYWORD_LENGTH);
        }

        List<Genre> genres = pageHelper.loadSafely(model, movieService::getActiveGenres, List.of());
        // A genre id that is not an active genre is ignored instead of returning an empty page.
        Integer selectedGenreId = genres.stream()
                .map(Genre::getGenreId)
                .filter(id -> Objects.equals(id, genreId))
                .findFirst()
                .orElse(null);

        final String searchKeyword = cleanKeyword;
        List<Movie> movies = pageHelper.loadSafely(model,
                () -> movieService.searchMovies(searchKeyword, selectedGenreId, selectedAgeRating, selectedType, selectedSort),
                List.of());

        model.addAttribute("movies", movies);
        model.addAttribute("nowShowingMovies", movies.stream().filter(Movie::isNowShowing).toList());
        model.addAttribute("comingSoonMovies", movies.stream().filter(Movie::isComingSoon).toList());

        model.addAttribute("genres", genres);
        model.addAttribute("ageRatings", movieService.getAgeRatings());
        model.addAttribute("selectedType", selectedType);
        model.addAttribute("selectedGenreId", selectedGenreId);
        model.addAttribute("selectedAgeRating", selectedAgeRating);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("sortBy", selectedSort);
        model.addAttribute("hasFilters", !cleanKeyword.isEmpty() || selectedGenreId != null
                || selectedAgeRating != null || !"all".equals(selectedType));

        return "customer/movies/list";
    }
}
