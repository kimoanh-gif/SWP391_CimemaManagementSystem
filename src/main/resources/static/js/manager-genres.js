/**
 * Cinema Management System - SWP391
 * Screen 55: Genre Management Interactive Client Script
 * Handles REST API interactions, modal dialogs, validations, and UI states.
 */

// =====================================================================
// Global Modal Controls (available immediately to inline onclick)
// =====================================================================
function openAddGenreModal() {
    resetGenreModalForm();
    var title = document.getElementById("genreModalTitle");
    if (title) title.textContent = "Add New Film Genre";

    var status = document.getElementById("genreStatus");
    if (status) status.value = "ACTIVE";

    var backdrop = document.getElementById("genreModalBackdrop");
    if (backdrop) backdrop.classList.add("open");

    var nameInput = document.getElementById("genreName");
    if (nameInput) nameInput.focus();
}

function openEditGenreModal(button) {
    resetGenreModalForm();
    var id = button.getAttribute("data-id");
    var name = button.getAttribute("data-name");
    var desc = button.getAttribute("data-description") || "";
    var status = button.getAttribute("data-status") || "ACTIVE";

    var title = document.getElementById("genreModalTitle");
    if (title) title.textContent = "Edit Genre: " + name;

    var idInput = document.getElementById("genreId");
    if (idInput) idInput.value = id;

    var nameInput = document.getElementById("genreName");
    if (nameInput) nameInput.value = name;

    var descInput = document.getElementById("genreDescription");
    if (descInput) descInput.value = desc;

    var statusInput = document.getElementById("genreStatus");
    if (statusInput) statusInput.value = status;

    var counter = document.getElementById("descCharCounter");
    if (counter) counter.textContent = desc.length + "/500";

    var backdrop = document.getElementById("genreModalBackdrop");
    if (backdrop) backdrop.classList.add("open");

    if (nameInput) nameInput.focus();
}

function closeGenreModal() {
    var backdrop = document.getElementById("genreModalBackdrop");
    if (backdrop) backdrop.classList.remove("open");
    resetGenreModalForm();
}

function closeMoviesModal() {
    var backdrop = document.getElementById("moviesModalBackdrop");
    if (backdrop) backdrop.classList.remove("open");
}

function resetGenreModalForm() {
    var form = document.getElementById("genreModalForm");
    if (form) form.reset();

    var idInput = document.getElementById("genreId");
    if (idInput) idInput.value = "";

    var nameFeedback = document.getElementById("nameFeedback");
    if (nameFeedback) nameFeedback.textContent = "";

    var descFeedback = document.getElementById("descFeedback");
    if (descFeedback) descFeedback.textContent = "";

    var errorAlert = document.getElementById("modalErrorAlert");
    if (errorAlert) {
        errorAlert.textContent = "";
        errorAlert.style.display = "none";
    }

    var nameInput = document.getElementById("genreName");
    if (nameInput) nameInput.classList.remove("is-invalid");

    var descInput = document.getElementById("genreDescription");
    if (descInput) descInput.classList.remove("is-invalid");

    var counter = document.getElementById("descCharCounter");
    if (counter) counter.textContent = "0/500";
}

// Assign explicitly to window object
window.openAddGenreModal = openAddGenreModal;
window.openEditGenreModal = openEditGenreModal;
window.closeGenreModal = closeGenreModal;
window.closeMoviesModal = closeMoviesModal;

// =====================================================================
// Toggle Status Action
// =====================================================================
async function toggleGenreStatus(button) {
    var id = button.getAttribute("data-id");
    var name = button.getAttribute("data-name");
    var currentStatus = button.getAttribute("data-status");
    var movieCount = parseInt(button.getAttribute("data-movies") || "0", 10);

    if (currentStatus === "ACTIVE" && movieCount > 0) {
        var confirmDeactivate = confirm(
            "Genre '" + name + "' is currently assigned to " + movieCount + " movies.\n" +
            "Deactivating will hide this genre from new movie selection. Are you sure you want to proceed?"
        );
        if (!confirmDeactivate) return;
    }

    var originalText = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="spinner"></span>';

    try {
        var response = await fetch("/api/manager/genres/" + id + "/toggle-status", {
            method: "PATCH",
            headers: {
                "Accept": "application/json"
            }
        });

        var result = await response.json();

        if (response.ok && result.success) {
            window.location.reload();
        } else {
            alert("Could not update genre status: " + (result.message || "Unknown error"));
            button.disabled = false;
            button.innerHTML = originalText;
        }
    } catch (err) {
        alert("Network error: " + err.message);
        button.disabled = false;
        button.innerHTML = originalText;
    }
}
window.toggleGenreStatus = toggleGenreStatus;

// =====================================================================
// View Associated Movies
// =====================================================================
async function viewAssociatedMovies(genreId, genreName) {
    var nameEl = document.getElementById("moviesModalTitle");
    if (nameEl) nameEl.textContent = "Movies in Genre: " + genreName;

    var container = document.getElementById("moviesModalList");
    if (container) {
        container.innerHTML = '<div style="text-align:center; padding: 2rem;"><span class="spinner"></span> Loading associated movies...</div>';
    }

    var backdrop = document.getElementById("moviesModalBackdrop");
    if (backdrop) backdrop.classList.add("open");

    try {
        var response = await fetch("/api/manager/genres/" + genreId + "/movies", {
            headers: { "Accept": "application/json" }
        });
        var result = await response.json();

        if (response.ok && result.success) {
            var movies = result.data || [];
            if (movies.length === 0) {
                container.innerHTML = '<p class="text-sm text-secondary text-center py-6">No movies currently linked to this genre.</p>';
            } else {
                container.innerHTML = '<div class="flex flex-col gap-2 w-full">' +
                    movies.map(function (m) {
                        var statusBadge = m.status === 'Now Showing' ? '<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-green-100 text-green-800">Now Showing</span>' :
                            (m.status === 'Upcoming' ? '<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-100 text-amber-800">Upcoming</span>' :
                            '<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-gray-100 text-gray-700">' + escapeHtml(m.status || 'Ended') + '</span>');
                        return '<div class="flex items-center justify-between p-3 bg-stone-50 border border-stone-200 rounded-lg">' +
                            '<span class="font-semibold text-text-charcoal text-sm">' + escapeHtml(m.title) + '</span>' +
                            statusBadge +
                            '</div>';
                    }).join("") +
                    '</div>' +
                    '<div class="mt-4 text-center">' +
                    '<a href="/manager/movies?genreId=' + genreId + '" class="inline-flex items-center gap-1.5 px-3.5 py-2 bg-cinema-red text-white text-xs font-semibold rounded-lg hover:bg-[#BF1B29] transition-colors"><span>View in Movie Catalog</span> &rarr;</a>' +
                    '</div>';
            }
        } else {
            container.innerHTML = '<p class="text-sm text-red-600 text-center py-4">' + escapeHtml(result.message || "Failed to load movie catalog.") + '</p>';
        }
    } catch (err) {
        container.innerHTML = '<p class="text-sm text-red-600 text-center py-4">Error loading data: ' + escapeHtml(err.message) + '</p>';
    }
}
window.viewAssociatedMovies = viewAssociatedMovies;
window.showAssociatedMovies = function(button) {
    var id = button.getAttribute("data-id");
    var name = button.getAttribute("data-name");
    viewAssociatedMovies(id, name);
};

// =====================================================================
// Delete Genre Action
// =====================================================================
async function deleteGenre(button) {
    var id = button.getAttribute("data-id");
    var name = button.getAttribute("data-name");
    var count = parseInt(button.getAttribute("data-count") || "0", 10);

    if (count > 0) {
        alert("Không thể xóa thể loại '" + name + "' vì đang có " + count + " phim liên kết. Vui lòng gỡ thể loại khỏi các phim trước hoặc chuyển sang trạng thái Ngưng hoạt động.");
        return;
    }

    if (!confirm("Bạn có chắc chắn muốn xóa vĩnh viễn thể loại '" + name + "' không?")) {
        return;
    }

    try {
        var headers = { "Accept": "application/json" };
        var csrf = typeof getCsrfToken === "function" ? getCsrfToken() : null;
        if (csrf) headers["X-XSRF-TOKEN"] = csrf;

        var response = await fetch("/api/manager/genres/" + id, {
            method: "DELETE",
            headers: headers
        });
        var result = await response.json();
        if (response.ok && result.success) {
            window.location.reload();
        } else {
            alert("Không thể xóa thể loại: " + (result.message || "Lỗi không xác định"));
        }
    } catch (err) {
        alert("Lỗi mạng: " + err.message);
    }
}
window.deleteGenre = deleteGenre;

// =====================================================================
// Event Listeners on DOM Ready
// =====================================================================
document.addEventListener("DOMContentLoaded", function () {
    var genreForm = document.getElementById("genreModalForm");
    var genreIdInput = document.getElementById("genreId");
    var genreNameInput = document.getElementById("genreName");
    var genreDescInput = document.getElementById("genreDescription");
    var genreStatusInput = document.getElementById("genreStatus");
    var nameFeedback = document.getElementById("nameFeedback");
    var descFeedback = document.getElementById("descFeedback");
    var modalErrorAlert = document.getElementById("modalErrorAlert");
    var modalSubmitBtn = document.getElementById("genreModalSubmitBtn");
    var charCounter = document.getElementById("descCharCounter");

    // Live description char counter
    if (genreDescInput && charCounter) {
        genreDescInput.addEventListener("input", function () {
            charCounter.textContent = this.value.length + "/500";
        });
    }

    // Close modal when clicking backdrop outside modal box
    var genreModalBackdrop = document.getElementById("genreModalBackdrop");
    var moviesModalBackdrop = document.getElementById("moviesModalBackdrop");
    [genreModalBackdrop, moviesModalBackdrop].forEach(function (backdrop) {
        if (backdrop) {
            backdrop.addEventListener("click", function (e) {
                if (e.target === this) {
                    this.classList.remove("open");
                }
            });
        }
    });

    // Form submission via AJAX API with fallback
    if (genreForm) {
        genreForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            var name = genreNameInput ? genreNameInput.value.trim() : "";
            var desc = genreDescInput ? genreDescInput.value.trim() : "";
            var status = genreStatusInput ? genreStatusInput.value : "ACTIVE";
            var id = genreIdInput ? genreIdInput.value.trim() : "";

            var hasError = false;
            if (!name) {
                showInputError(genreNameInput, nameFeedback, "Genre name is required.");
                hasError = true;
            } else if (name.length < 2 || name.length > 100) {
                showInputError(genreNameInput, nameFeedback, "Genre name must be between 2 and 100 characters.");
                hasError = true;
            } else {
                clearInputError(genreNameInput, nameFeedback);
            }

            if (desc.length > 500) {
                showInputError(genreDescInput, descFeedback, "Description cannot exceed 500 characters.");
                hasError = true;
            } else {
                clearInputError(genreDescInput, descFeedback);
            }

            if (hasError) return;

            var payload = {
                name: name,
                description: desc,
                status: status
            };
            if (id && parseInt(id, 10) > 0) {
                payload.id = parseInt(id, 10);
            }

            setButtonLoading(modalSubmitBtn, true);
            hideModalAlert();

            try {
                var url = (id && parseInt(id, 10) > 0) ? "/api/manager/genres/" + id : "/api/manager/genres";
                var method = (id && parseInt(id, 10) > 0) ? "PUT" : "POST";

                var response = await fetch(url, {
                    method: method,
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify(payload)
                });

                var result = await response.json();

                if (response.ok && result.success) {
                    closeGenreModal();
                    window.location.reload();
                } else {
                    var message = result.message || (result.errors ? result.errors.join(", ") : "An error occurred");
                    showModalAlert(message);
                }
            } catch (err) {
                // If fetch fails, fallback to normal form submit
                genreForm.submit();
            } finally {
                setButtonLoading(modalSubmitBtn, false);
            }
        });
    }

    function showInputError(input, feedbackEl, message) {
        if (input) input.classList.add("is-invalid");
        if (feedbackEl) feedbackEl.textContent = message;
    }

    function clearInputError(input, feedbackEl) {
        if (input) input.classList.remove("is-invalid");
        if (feedbackEl) feedbackEl.textContent = "";
    }

    function showModalAlert(message) {
        if (modalErrorAlert) {
            modalErrorAlert.textContent = message;
            modalErrorAlert.style.display = "block";
        }
    }

    function hideModalAlert() {
        if (modalErrorAlert) {
            modalErrorAlert.textContent = "";
            modalErrorAlert.style.display = "none";
        }
    }

    function setButtonLoading(button, isLoading) {
        if (!button) return;
        button.disabled = isLoading;
        if (isLoading) {
            button.setAttribute("data-orig-text", button.innerHTML);
            button.innerHTML = '<span class="spinner"></span> Saving...';
        } else {
            var orig = button.getAttribute("data-orig-text");
            if (orig) button.innerHTML = orig;
        }
    }
});

function escapeHtml(str) {
    if (!str) return "";
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
