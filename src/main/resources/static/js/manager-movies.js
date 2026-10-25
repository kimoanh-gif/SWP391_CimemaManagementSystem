/**
 * Cinema Management System - SWP391
 * Screen 54: Movie Management Interactive Client Script
 * Handles REST API interactions, modal dialogs, validations, and UI states.
 */

// =====================================================================
// Global Helper Utilities
// =====================================================================
function setValue(id, val) {
    var el = document.getElementById(id);
    if (el) el.value = (val !== null && val !== undefined) ? val : "";
}

function getValue(id) {
    var el = document.getElementById(id);
    return el ? el.value : "";
}

function setText(id, text) {
    var el = document.getElementById(id);
    if (el) el.textContent = (text !== null && text !== undefined) ? text : "";
}

function showError(inputId, feedbackId, msg) {
    var input = document.getElementById(inputId);
    if (input) input.classList.add("is-invalid");
    var fb = document.getElementById(feedbackId);
    if (fb) fb.textContent = msg;
}

function clearError(inputId, feedbackId) {
    var input = document.getElementById(inputId);
    if (input) input.classList.remove("is-invalid");
    clearFeedback(feedbackId);
}

function clearFeedback(id) {
    var fb = document.getElementById(id);
    if (fb) fb.textContent = "";
}

function showModalAlert(msg) {
    var el = document.getElementById("modalErrorAlert");
    if (el) {
        el.textContent = msg;
        el.style.display = "block";
    }
}

function hideModalAlert() {
    var el = document.getElementById("modalErrorAlert");
    if (el) {
        el.textContent = "";
        el.style.display = "none";
    }
}

function setLoading(btn, isLoading) {
    if (!btn) return;
    btn.disabled = isLoading;
    if (isLoading) {
        btn.setAttribute("data-orig-text", btn.innerHTML);
        btn.innerHTML = '<span class="spinner"></span> Saving...';
    } else {
        var orig = btn.getAttribute("data-orig-text");
        if (orig) btn.innerHTML = orig;
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function getCsrfToken() {
    var match = document.cookie.match(new RegExp('(^|;\\s*)XSRF-TOKEN=([^;]*)'));
    return match ? decodeURIComponent(match[2]) : "";
}

function getTodayIso() {
    var today = new Date();
    var yyyy = today.getFullYear();
    var mm = String(today.getMonth() + 1).padStart(2, '0');
    var dd = String(today.getDate()).padStart(2, '0');
    return yyyy + '-' + mm + '-' + dd;
}

function validateMovieDates() {
    var releaseInput = document.getElementById("movieReleaseDate");
    var endInput = document.getElementById("movieEndDate");
    if (!releaseInput || !endInput) return true;

    var releaseDate = releaseInput.value;
    var endDate = endInput.value;
    var id = getValue("movieId").trim();
    var isNew = !id || parseInt(id, 10) <= 0;
    var todayStr = getTodayIso();
    var origReleaseDate = releaseInput.getAttribute("data-original-date") || "";
    var origEndDate = endInput.getAttribute("data-original-date") || "";

    var isValid = true;

    // Validate Release Date: cannot be in past for new movie or if modified
    if (releaseDate) {
        if (releaseDate < todayStr && (isNew || releaseDate !== origReleaseDate)) {
            showError("movieReleaseDate", "releaseDateFeedback", "Release date must be from today onwards, cannot be in the past.");
            isValid = false;
        } else {
            clearError("movieReleaseDate", "releaseDateFeedback");
        }
    } else {
        clearError("movieReleaseDate", "releaseDateFeedback");
    }

    // Dynamic min attribute for Expected End Date
    var effectiveMin = (releaseDate && releaseDate > todayStr) ? releaseDate : todayStr;
    if (isNew || (endDate && endDate >= effectiveMin)) {
        endInput.min = effectiveMin;
    }

    // Validate Expected End Date: cannot be in past or before release date
    if (endDate) {
        if (endDate < todayStr && (isNew || endDate !== origEndDate)) {
            showError("movieEndDate", "dateFeedback", "Expected end date must be from today onwards, cannot be in the past.");
            isValid = false;
        } else if (releaseDate && endDate < releaseDate) {
            showError("movieEndDate", "dateFeedback", "Expected end date cannot be earlier than release date.");
            isValid = false;
        } else {
            clearError("movieEndDate", "dateFeedback");
        }
    } else {
        clearError("movieEndDate", "dateFeedback");
    }

    return isValid;
}

// =====================================================================
// Global Modal Controls (available immediately to inline onclick)
// =====================================================================
function openAddMovieModal() {
    resetMovieForm();
    var title = document.getElementById("movieModalTitle");
    if (title) title.textContent = "Add new movie";

    var status = document.getElementById("movieStatus");
    if (status) status.value = "Upcoming";

    var todayStr = getTodayIso();
    var releaseInput = document.getElementById("movieReleaseDate");
    var endInput = document.getElementById("movieEndDate");
    if (releaseInput) {
        releaseInput.min = todayStr;
        releaseInput.removeAttribute("data-original-date");
    }
    if (endInput) {
        endInput.min = todayStr;
        endInput.removeAttribute("data-original-date");
    }

    var backdrop = document.getElementById("movieModalBackdrop");
    if (backdrop) backdrop.classList.add("open");

    var titleInput = document.getElementById("movieTitle");
    if (titleInput) titleInput.focus();
}

function openEditMovieModal(button) {
    resetMovieForm();
    var id = button.getAttribute("data-id");
    var title = button.getAttribute("data-title");
    var originalTitle = button.getAttribute("data-original-title") || "";
    var duration = button.getAttribute("data-duration") || "120";
    var ageRating = button.getAttribute("data-age-rating") || "P";
    var status = button.getAttribute("data-status") || "Upcoming";
    var releaseDate = button.getAttribute("data-release-date") || "";
    var endDate = button.getAttribute("data-end-date") || "";
    var director = button.getAttribute("data-director") || "";
    var actors = button.getAttribute("data-actors") || "";
    var language = button.getAttribute("data-language") || "";
    var description = button.getAttribute("data-description") || "";
    var poster = button.getAttribute("data-poster") || "";
    var trailer = button.getAttribute("data-trailer") || "";
    var genreIdsStr = button.getAttribute("data-genre-ids") || "";

    var modalTitle = document.getElementById("movieModalTitle");
    if (modalTitle) modalTitle.textContent = "Edit movie: " + title;

    setValue("movieId", id);
    setValue("movieTitle", title);
    setValue("movieOriginalTitle", originalTitle);
    setValue("movieDuration", duration);
    setValue("movieAgeRating", ageRating);
    setValue("movieStatus", status);
    setValue("movieReleaseDate", releaseDate);
    setValue("movieEndDate", endDate);
    setValue("movieDirector", director);
    setValue("movieActors", actors);
    setValue("movieLanguage", language);
    setValue("movieDescription", description);
    setValue("moviePoster", poster);
    setValue("movieTrailer", trailer);

    var todayStr = getTodayIso();
    var releaseInput = document.getElementById("movieReleaseDate");
    var endInput = document.getElementById("movieEndDate");
    if (releaseInput) {
        releaseInput.setAttribute("data-original-date", releaseDate);
        if (releaseDate && releaseDate < todayStr) {
            releaseInput.min = releaseDate;
        } else {
            releaseInput.min = todayStr;
        }
    }
    if (endInput) {
        endInput.setAttribute("data-original-date", endDate);
        var baseMin = (releaseDate && releaseDate > todayStr) ? releaseDate : todayStr;
        if (endDate && endDate < baseMin) {
            endInput.min = endDate;
        } else {
            endInput.min = baseMin;
        }
    }

    var counter = document.getElementById("descCharCounter");
    if (counter) counter.textContent = description.length + "/1000";

    // Check selected genres
    var genreIds = genreIdsStr.split(",").map(function (s) { return s.trim(); });
    var checkboxes = document.querySelectorAll(".genre-checkbox-item input[type='checkbox']");
    checkboxes.forEach(function (cb) {
        cb.checked = genreIds.includes(cb.value);
    });

    var backdrop = document.getElementById("movieModalBackdrop");
    if (backdrop) backdrop.classList.add("open");

    var titleInput = document.getElementById("movieTitle");
    if (titleInput) titleInput.focus();
}

function openQuickViewModal(button) {
    var id = button.getAttribute("data-id");
    var title = button.getAttribute("data-title");
    var originalTitle = button.getAttribute("data-original-title") || "";
    var duration = button.getAttribute("data-duration") || "120";
    var ageRating = button.getAttribute("data-age-rating") || "P";
    var status = button.getAttribute("data-status") || "Upcoming";
    var releaseDate = button.getAttribute("data-release-date") || "Not set";
    var director = button.getAttribute("data-director") || "Not set";
    var actors = button.getAttribute("data-actors") || "Not set";
    var language = button.getAttribute("data-language") || "Vietnamese";
    var description = button.getAttribute("data-description") || "No synopsis for this movie yet.";
    var poster = button.getAttribute("data-poster") || "";
    var trailer = button.getAttribute("data-trailer") || "";
    var genres = button.getAttribute("data-genres") || "Unclassified";
    var showtimes = button.getAttribute("data-showtimes") || "0";

    setText("qvTitle", title);
    setText("qvOriginalTitle", originalTitle ? "(" + originalTitle + ")" : "");
    setText("qvAgeRating", ageRating);
    setText("qvDuration", duration + " min");
    setText("qvStatus", status);
    setText("qvGenres", genres);
    setText("qvReleaseDate", releaseDate);
    setText("qvDirector", director);
    setText("qvActors", actors);
    setText("qvLanguage", language);
    setText("qvDescription", description);
    setText("qvShowtimes", showtimes + " showtimes");

    var posterEl = document.getElementById("qvPoster");
    if (posterEl) {
        if (poster && poster.trim()) {
            posterEl.innerHTML = '<img src="' + escapeHtml(poster) + '" alt="' + escapeHtml(title) + '" style="width:100%;height:100%;object-fit:cover;border-radius:4px;">';
        } else {
            posterEl.innerHTML = '🎬';
        }
    }

    var trailerLink = document.getElementById("qvTrailerLink");
    if (trailerLink) {
        if (trailer && trailer.trim()) {
            trailerLink.href = trailer;
            trailerLink.style.display = "inline-flex";
        } else {
            trailerLink.style.display = "none";
        }
    }

    var backdrop = document.getElementById("movieDetailModalBackdrop");
    if (backdrop) backdrop.classList.add("open");
}

function closeMovieModal() {
    var backdrop = document.getElementById("movieModalBackdrop");
    if (backdrop) backdrop.classList.remove("open");
    resetMovieForm();
}

function closeQuickViewModal() {
    var backdrop = document.getElementById("movieDetailModalBackdrop");
    if (backdrop) backdrop.classList.remove("open");
}

function resetMovieForm() {
    var form = document.getElementById("movieModalForm");
    if (form) form.reset();

    setValue("movieId", "");
    clearFeedback("titleFeedback");
    clearFeedback("durationFeedback");
    clearFeedback("releaseDateFeedback");
    clearFeedback("dateFeedback");

    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) {
        releaseInput.removeAttribute("min");
        releaseInput.removeAttribute("data-original-date");
        releaseInput.classList.remove("is-invalid");
    }

    var endInput = document.getElementById("movieEndDate");
    if (endInput) {
        endInput.removeAttribute("min");
        endInput.removeAttribute("data-original-date");
        endInput.classList.remove("is-invalid");
    }

    var errorAlert = document.getElementById("modalErrorAlert");
    if (errorAlert) {
        errorAlert.textContent = "";
        errorAlert.style.display = "none";
    }

    var titleInput = document.getElementById("movieTitle");
    if (titleInput) titleInput.classList.remove("is-invalid");

    var durationInput = document.getElementById("movieDuration");
    if (durationInput) durationInput.classList.remove("is-invalid");

    var counter = document.getElementById("descCharCounter");
    if (counter) counter.textContent = "0/1000";

    var checkboxes = document.querySelectorAll(".genre-checkbox-item input[type='checkbox']");
    checkboxes.forEach(function (cb) { cb.checked = false; });
}

// =====================================================================
// Deactivate Movie Action (BR-37 soft delete to Archived)
// =====================================================================
async function deactivateMovie(button) {
    var id = button.getAttribute("data-id");
    var title = button.getAttribute("data-title");
    var showtimes = parseInt(button.getAttribute("data-showtimes") || "0", 10);

    var confirmMsg = "Are you sure you want to stop screening and archive movie '" + title + "'?\n" +
        "The movie will be marked as Archived and no new showtimes can be created.";
    if (showtimes > 0) {
        confirmMsg += "\n\n(Note: This movie currently has " + showtimes + " showtimes in the system. If there are upcoming showtimes, the system will prevent deactivation to protect scheduled bookings).";
    }

    if (!confirm(confirmMsg)) {
        return;
    }

    var originalText = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="spinner"></span>';

    try {
        var headers = { "Accept": "application/json" };
        var csrf = getCsrfToken();
        if (csrf) headers["X-XSRF-TOKEN"] = csrf;

        var response = await fetch("/api/manager/movies/" + id + "/deactivate", {
            method: "PATCH",
            headers: headers
        });

        var result = await response.json();

        if (response.ok && result.success) {
            window.location.reload();
        } else {
            var errorMsg = (result && result.message) ? result.message : "Unable to stop movie screening.";
            alert("System constraint:\n" + errorMsg);
            button.disabled = false;
            button.innerHTML = originalText;
        }
    } catch (err) {
        alert("Connection or processing error: " + err.message);
        button.disabled = false;
        button.innerHTML = originalText;
    }
}

// =====================================================================
// Reactivate Movie Action (Reopen Archived/Ended to Active)
// =====================================================================
async function reactivateMovie(button) {
    var id = button.getAttribute("data-id");
    var title = button.getAttribute("data-title");

    var confirmMsg = "Are you sure you want to reactivate movie '" + title + "'?\n" +
        "The movie will return to active status and can be scheduled for new showtimes.";

    if (!confirm(confirmMsg)) {
        return;
    }

    var originalText = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="spinner"></span>';

    try {
        var headers = { "Accept": "application/json" };
        var csrf = getCsrfToken();
        if (csrf) headers["X-XSRF-TOKEN"] = csrf;

        var response = await fetch("/api/manager/movies/" + id + "/reactivate", {
            method: "PATCH",
            headers: headers
        });

        var result = await response.json();

        if (response.ok && result.success) {
            window.location.reload();
        } else {
            alert("Unable to reactivate movie: " + (result.message || "Unknown error"));
            button.disabled = false;
            button.innerHTML = originalText;
        }
    } catch (err) {
        alert("Connection error: " + err.message);
        button.disabled = false;
        button.innerHTML = originalText;
    }
}

// Assign explicitly to window object
window.openAddMovieModal = openAddMovieModal;
window.openEditMovieModal = openEditMovieModal;
window.openQuickViewModal = openQuickViewModal;
window.closeMovieModal = closeMovieModal;
window.closeQuickViewModal = closeQuickViewModal;
window.resetMovieForm = resetMovieForm;
window.deactivateMovie = deactivateMovie;
window.reactivateMovie = reactivateMovie;
window.validateMovieDates = validateMovieDates;
window.setValue = setValue;
window.getValue = getValue;
window.setText = setText;

// =====================================================================
// Event Listeners on DOM Ready
// =====================================================================
document.addEventListener("DOMContentLoaded", function () {
    var movieForm = document.getElementById("movieModalForm");
    var descInput = document.getElementById("movieDescription");
    var charCounter = document.getElementById("descCharCounter");
    var submitBtn = document.getElementById("modalSubmitBtn");

    if (descInput && charCounter) {
        descInput.addEventListener("input", function () {
            charCounter.textContent = this.value.length + "/1000";
        });
    }

    var durationInput = document.getElementById("movieDuration");
    if (durationInput) {
        // Prevent typing exponential, negative, decimal signs
        durationInput.addEventListener("keydown", function (e) {
            if (["e", "E", "+", "-", "."].indexOf(e.key) !== -1) {
                e.preventDefault();
            }
        });

        // Restrict input to maximum 3 digits and validate range 1 - 600
        durationInput.addEventListener("input", function () {
            // Strip any non-digit character
            this.value = this.value.replace(/\D/g, "");

            // Limit maximum length to 3 digits (e.g. 1 to 600)
            if (this.value.length > 3) {
                this.value = this.value.slice(0, 3);
            }

            var val = parseInt(this.value, 10);
            if (!this.value || isNaN(val)) {
                showError("movieDuration", "durationFeedback", "Duration is required (1 to 600 minutes).");
            } else if (val < 1 || val > 600) {
                showError("movieDuration", "durationFeedback", "Duration must be between 1 and 600 minutes.");
            } else {
                clearError("movieDuration", "durationFeedback");
            }
        });

        // On blur, auto-correct if empty or out of bounds
        durationInput.addEventListener("blur", function () {
            var val = parseInt(this.value, 10);
            if (!this.value || isNaN(val) || val < 1) {
                this.value = "1";
                clearError("movieDuration", "durationFeedback");
            } else if (val > 600) {
                this.value = "600";
                clearError("movieDuration", "durationFeedback");
            }
        });
    }

    // Real-time validation for release date and expected end date
    var releaseDateInput = document.getElementById("movieReleaseDate");
    var endDateInput = document.getElementById("movieEndDate");
    if (releaseDateInput) {
        releaseDateInput.addEventListener("input", validateMovieDates);
        releaseDateInput.addEventListener("change", validateMovieDates);
    }
    if (endDateInput) {
        endDateInput.addEventListener("input", validateMovieDates);
        endDateInput.addEventListener("change", validateMovieDates);
    }

    // Close on backdrop click
    ["movieModalBackdrop", "movieDetailModalBackdrop"].forEach(function (id) {
        var backdrop = document.getElementById(id);
        if (backdrop) {
            backdrop.addEventListener("click", function (e) {
                if (e.target === this) {
                    this.classList.remove("open");
                }
            });
        }
    });

    // Form submission via AJAX API with fallback
    if (movieForm) {
        movieForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            var title = getValue("movieTitle").trim();
            var duration = parseInt(getValue("movieDuration"), 10);
            var releaseDate = getValue("movieReleaseDate");
            var endDate = getValue("movieEndDate");
            var id = getValue("movieId").trim();

            var hasError = false;

            if (!title) {
                showError("movieTitle", "titleFeedback", "Movie title is required.");
                hasError = true;
            } else if (title.length > 150) {
                showError("movieTitle", "titleFeedback", "Movie title must not exceed 150 characters.");
                hasError = true;
            } else {
                clearError("movieTitle", "titleFeedback");
            }

            if (!duration || isNaN(duration) || duration <= 0 || duration > 600) {
                showError("movieDuration", "durationFeedback", "Duration must be between 1 and 600 minutes.");
                hasError = true;
            } else {
                clearError("movieDuration", "durationFeedback");
            }

            if (!validateMovieDates()) {
                hasError = true;
            }

            if (hasError) return;

            // Collect selected genres
            var selectedGenreIds = [];
            var checkedBoxes = document.querySelectorAll(".genre-checkbox-item input[type='checkbox']:checked");
            checkedBoxes.forEach(function (cb) {
                selectedGenreIds.push(parseInt(cb.value, 10));
            });

            var payload = {
                title: title,
                originalTitle: getValue("movieOriginalTitle").trim(),
                duration: duration,
                releaseDate: releaseDate || null,
                endDate: endDate || null,
                language: getValue("movieLanguage").trim(),
                director: getValue("movieDirector").trim(),
                actors: getValue("movieActors").trim(),
                ageRating: getValue("movieAgeRating"),
                description: getValue("movieDescription").trim(),
                poster: getValue("moviePoster").trim(),
                trailer: getValue("movieTrailer").trim(),
                status: getValue("movieStatus"),
                genreIds: selectedGenreIds
            };

            if (id && parseInt(id, 10) > 0) {
                payload.id = parseInt(id, 10);
            }

            setLoading(submitBtn, true);
            hideModalAlert();

            try {
                var url = (id && parseInt(id, 10) > 0) ? "/api/manager/movies/" + id : "/api/manager/movies";
                var method = (id && parseInt(id, 10) > 0) ? "PUT" : "POST";

                var headers = {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                };
                var csrf = getCsrfToken();
                if (csrf) headers["X-XSRF-TOKEN"] = csrf;

                var response = await fetch(url, {
                    method: method,
                    headers: headers,
                    body: JSON.stringify(payload)
                });

                var result = await response.json();

                if (response.ok && result.success) {
                    closeMovieModal();
                    window.location.reload();
                } else {
                    var message = result.message || (result.errors ? result.errors.join(", ") : "Something went wrong");
                    showModalAlert(message);
                }
            } catch (err) {
                movieForm.submit(); // fallback to standard POST
            } finally {
                setLoading(submitBtn, false);
            }
        });
    }

    // Event delegation for movie delete confirmation and constraint alert
    document.addEventListener("submit", function (e) {
        var deleteForm = e.target.closest(".delete-movie-form");
        if (deleteForm) {
            var title = deleteForm.getAttribute("data-title") || "this movie";
            var showtimes = parseInt(deleteForm.getAttribute("data-showtimes") || "0", 10);
            if (showtimes > 0) {
                e.preventDefault();
                alert("Data constraint prevents deletion:\n\nMovie '" + title + "' has " + showtimes + " linked showtimes in the system.\n\nTo ensure ticket and scheduling integrity, this movie cannot be deleted. You can use the 'Stop' button (⏹) to archive the movie.");
                return;
            }
            if (!confirm("Are you sure you want to permanently delete movie '" + title + "'?\nThis will remove the movie from the database and cannot be undone.")) {
                e.preventDefault();
            }
        }
    });
});
