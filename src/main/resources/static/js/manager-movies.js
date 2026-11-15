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
    var metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken && metaToken.getAttribute("content")) return metaToken.getAttribute("content");
    var inputToken = document.querySelector('input[name="_csrf"]');
    if (inputToken && inputToken.value) return inputToken.value;
    var match = document.cookie.match(new RegExp('(^|;\\s*)XSRF-TOKEN=([^;]*)'));
    return match ? decodeURIComponent(match[2]) : "";
}

function getTomorrowDateStr() {
    var d = new Date();
    d.setDate(d.getDate() + 1);
    var y = d.getFullYear();
    var m = String(d.getMonth() + 1).padStart(2, "0");
    var day = String(d.getDate()).padStart(2, "0");
    return y + "-" + m + "-" + day;
}

var getTodayIso = getTodayDateStr;
function getTodayDateStr() {
    var d = new Date();
    var y = d.getFullYear();
    var m = String(d.getMonth() + 1).padStart(2, "0");
    var day = String(d.getDate()).padStart(2, "0");
    return y + "-" + m + "-" + day;
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

    // Enforce future dates (from tomorrow onwards)
    var tomorrowStr = getTomorrowDateStr();
    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) releaseInput.min = tomorrowStr;

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
    var banner = button.getAttribute("data-banner") || "";
    var trailer = button.getAttribute("data-trailer") || "";
    var genreIdsStr = button.getAttribute("data-genre-ids") || "";

    var modalTitle = document.getElementById("movieModalTitle");
    if (modalTitle) modalTitle.textContent = "Edit movie: " + title;

    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) releaseInput.removeAttribute("min");

    setValue("movieId", id);
    setValue("movieTitle", title);
    setValue("movieOriginalTitle", originalTitle);
    setValue("movieDuration", duration);
    setValue("movieAgeRating", ageRating);
    setValue("movieStatus", status);
    setValue("movieReleaseDate", releaseDate);
    setValue("movieDirector", director);
    setValue("movieActors", actors);
    setValue("movieLanguage", language);
    setValue("movieDescription", description);
    setValue("moviePoster", poster);
    setValue("movieBanner", banner);
    setValue("movieTrailer", trailer);
    updatePosterPreview(poster);
    updateBannerPreview(banner);

    var todayStr = getTodayDateStr();
    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) {
        releaseInput.setAttribute("data-original-date", releaseDate);
        if (releaseDate && releaseDate < todayStr) {
            releaseInput.min = releaseDate;
        } else {
            releaseInput.min = todayStr;
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

    var errorAlert = document.getElementById("modalErrorAlert");
    if (errorAlert) {
        errorAlert.textContent = "";
        errorAlert.style.display = "none";
    }

    var titleInput = document.getElementById("movieTitle");
    if (titleInput) titleInput.classList.remove("is-invalid");

    var durationInput = document.getElementById("movieDuration");
    if (durationInput) durationInput.classList.remove("is-invalid");

    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) releaseInput.classList.remove("is-invalid");

    var counter = document.getElementById("descCharCounter");
    if (counter) counter.textContent = "0/1000";
    updatePosterPreview("");
    updateBannerPreview("");

    var checkboxes = document.querySelectorAll(".genre-checkbox-item input[type='checkbox']");
    checkboxes.forEach(function (cb) { cb.checked = false; });
}

function validateMovieDuration() {
    clearError("movieDuration", "durationFeedback");
    return true;
}

function validateMovieDates() {
    clearError("movieReleaseDate", "releaseDateFeedback");
    return true;
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
window.updatePosterPreview = updatePosterPreview;
window.updateBannerPreview = updateBannerPreview;
window.uploadImageFile = uploadImageFile;
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

    var titleInput = document.getElementById("movieTitle");
    if (titleInput) {
        titleInput.addEventListener("input", function () {
            clearError("movieTitle", "titleFeedback");
            hideModalAlert();
        });
    }

    var durationInput = document.getElementById("movieDuration");
    if (durationInput) {
        durationInput.addEventListener("input", function () {
            clearError("movieDuration", "durationFeedback");
            hideModalAlert();
        });
    }

    var releaseInput = document.getElementById("movieReleaseDate");
    if (releaseInput) {
        releaseInput.addEventListener("input", function () {
            clearError("movieReleaseDate", "releaseDateFeedback");
            hideModalAlert();
        });
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

    // Wireup image upload and preview
    var posterFile = document.getElementById("moviePosterFile");
    if (posterFile) {
        posterFile.addEventListener("change", function () {
            uploadImageFile(this, "moviePoster", updatePosterPreview, "poster");
        });
    }

    var bannerFile = document.getElementById("movieBannerFile");
    if (bannerFile) {
        bannerFile.addEventListener("change", function () {
            uploadImageFile(this, "movieBanner", updateBannerPreview, "banner");
        });
    }

    var posterInput = document.getElementById("moviePoster");
    if (posterInput) {
        posterInput.addEventListener("input", function () {
            updatePosterPreview(this.value);
        });
    }

    var bannerInput = document.getElementById("movieBanner");
    if (bannerInput) {
        bannerInput.addEventListener("input", function () {
            updateBannerPreview(this.value);
        });
    }

    // Form submission via AJAX API with fallback
    if (movieForm) {
        movieForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            var title = getValue("movieTitle").trim();
            var durationRaw = getValue("movieDuration").trim();
            var duration = durationRaw ? parseInt(durationRaw, 10) : null;
            if (isNaN(duration)) {
                duration = null;
            }
            var releaseDate = getValue("movieReleaseDate");
            var id = getValue("movieId").trim();

            clearError("movieTitle", "titleFeedback");
            clearError("movieDuration", "durationFeedback");
            clearError("movieReleaseDate", "releaseDateFeedback");
            hideModalAlert();

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
                endDate: null,
                language: getValue("movieLanguage").trim(),
                director: getValue("movieDirector").trim(),
                actors: getValue("movieActors").trim(),
                ageRating: getValue("movieAgeRating"),
                description: getValue("movieDescription").trim(),
                poster: getValue("moviePoster").trim(),
                banner: getValue("movieBanner").trim(),
                trailer: getValue("movieTrailer").trim(),
                status: getValue("movieStatus"),
                genreIds: selectedGenreIds
            };

            if (id && parseInt(id, 10) > 0) {
                payload.id = parseInt(id, 10);
            }

            setLoading(submitBtn, true);

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
                    var errorMessages = [];
                    if (result.errors && Array.isArray(result.errors) && result.errors.length > 0) {
                        errorMessages = result.errors;
                    } else if (result.message) {
                        errorMessages = [result.message];
                    } else {
                        errorMessages = ["An error occurred while saving the movie."];
                    }

                    // Map errors back to corresponding form fields
                    errorMessages.forEach(function (msg) {
                        var lower = msg.toLowerCase();
                        if (lower.indexOf("title") !== -1) {
                            showError("movieTitle", "titleFeedback", msg);
                        } else if (lower.indexOf("duration") !== -1) {
                            showError("movieDuration", "durationFeedback", msg);
                        } else if (lower.indexOf("date") !== -1 || lower.indexOf("release") !== -1) {
                            showError("movieReleaseDate", "releaseDateFeedback", msg);
                        }
                    });

                    showModalAlert(errorMessages.join("; "));
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


// =====================================================================
// Poster & Banner Live Preview & Upload Handlers
// =====================================================================
function updatePosterPreview(url) {
    var c = document.getElementById("posterPreviewContainer");
    var img = document.getElementById("posterPreviewImg");
    if (!c || !img) return;
    if (url && url.trim()) {
        img.src = url.trim();
        c.style.display = "inline-flex";
    } else {
        img.src = "";
        c.style.display = "none";
    }
}

function updateBannerPreview(url) {
    var c = document.getElementById("bannerPreviewContainer");
    var img = document.getElementById("bannerPreviewImg");
    if (!c || !img) return;
    if (url && url.trim()) {
        img.src = url.trim();
        c.style.display = "inline-flex";
    } else {
        img.src = "";
        c.style.display = "none";
    }
}

async function uploadImageFile(fileInput, targetInputId, previewFn, type) {
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) return;
    var file = fileInput.files[0];
    if (file.size > 5 * 1024 * 1024) {
        alert("Image file size cannot exceed 5MB.");
        fileInput.value = "";
        return;
    }
    var formData = new FormData();
    formData.append("file", file);
    formData.append("type", type);

    var textSpan = fileInput.parentElement ? fileInput.parentElement.querySelector(".upload-text") : null;
    var oldText = textSpan ? textSpan.textContent : "Upload image";
    if (textSpan) textSpan.textContent = "Uploading...";

    try {
        var headers = {};
        var csrf = getCsrfToken();
        if (csrf) headers["X-XSRF-TOKEN"] = csrf;

        var response = await fetch("/api/manager/movies/upload-image", {
            method: "POST",
            headers: headers,
            body: formData
        });

        var result = await response.json();
        if (response.ok && result.success && result.data && result.data.url) {
            setValue(targetInputId, result.data.url);
            previewFn(result.data.url);
        } else {
            var msg = (result && result.message) ? result.message : "Image upload failed";
            alert("Image upload error: " + msg);
        }
    } catch (err) {
        alert("Connection error during upload: " + err.message);
    } finally {
        if (textSpan) textSpan.textContent = oldText;
        fileInput.value = "";
    }
}
