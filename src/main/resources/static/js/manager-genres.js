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
    if (title) title.textContent = "Thêm Thể Loại Mới";

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
    if (title) title.textContent = "Chỉnh Sửa Thể Loại: " + name;

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
            "Thể loại '" + name + "' hiện đang được gán cho " + movieCount + " phim.\n" +
            "Hủy kích hoạt sẽ ẩn thể loại này khỏi danh sách chọn phim mới. Bạn có chắc chắn muốn hủy kích hoạt?"
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
            alert("Không thể thay đổi trạng thái: " + (result.message || "Lỗi không xác định"));
            button.disabled = false;
            button.innerHTML = originalText;
        }
    } catch (err) {
        alert("Lỗi kết nối: " + err.message);
        button.disabled = false;
        button.innerHTML = originalText;
    }
}
window.toggleGenreStatus = toggleGenreStatus;

// =====================================================================
// View Associated Movies
// =====================================================================
async function viewAssociatedMovies(genreId, genreName) {
    var nameEl = document.getElementById("moviesModalGenreName");
    if (nameEl) nameEl.textContent = genreName;

    var container = document.getElementById("moviesListContainer");
    if (container) {
        container.innerHTML = '<div style="text-align:center; padding: 2rem;"><span class="spinner"></span> Đang tải danh sách phim...</div>';
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
                container.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 1.5rem;">Chưa có phim nào liên kết với thể loại này.</p>';
            } else {
                container.innerHTML = '<div style="display:flex;flex-direction:column;gap:8px;">' +
                    movies.map(function (m) {
                        var statusBadge = m.status === 'Now Showing' ? '<span class="badge badge-green" style="font-size:11px;padding:3px 8px;">Đang chiếu</span>' :
                            (m.status === 'Upcoming' ? '<span class="badge badge-orange" style="font-size:11px;padding:3px 8px;">Sắp chiếu</span>' :
                            '<span class="badge badge-gray" style="font-size:11px;padding:3px 8px;">' + escapeHtml(m.status || 'Chưa mở') + '</span>');
                        return '<div class="movie-chip-item" style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;background:#F8FAFC;border:1px solid #E2E8F0;border-radius:8px;">' +
                            '<span class="movie-chip-title" style="font-weight:700;color:var(--text-900);">🎬 ' + escapeHtml(m.title) + '</span>' +
                            statusBadge +
                            '</div>';
                    }).join("") +
                    '</div>' +
                    '<div style="margin-top:16px;text-align:center;">' +
                    '<a href="/manager/movies?genreId=' + genreId + '" class="btn btn-primary btn-sm" style="text-decoration:none;display:inline-block;">🔍 Đi tới Màn Hình Quản Lý Phim (Lọc theo thể loại này) ➔</a>' +
                    '</div>';
            }
        } else {
            container.innerHTML = '<p style="color: var(--danger); text-align: center;">' + escapeHtml(result.message || "Không thể tải danh sách phim.") + '</p>';
        }
    } catch (err) {
        container.innerHTML = '<p style="color: var(--danger); text-align: center;">Lỗi tải dữ liệu: ' + escapeHtml(err.message) + '</p>';
    }
}
window.viewAssociatedMovies = viewAssociatedMovies;

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
    var modalSubmitBtn = document.getElementById("modalSubmitBtn");
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
                showInputError(genreNameInput, nameFeedback, "Tên thể loại không được để trống.");
                hasError = true;
            } else if (name.length < 2 || name.length > 100) {
                showInputError(genreNameInput, nameFeedback, "Tên thể loại phải có độ dài từ 2 đến 100 ký tự.");
                hasError = true;
            } else {
                clearInputError(genreNameInput, nameFeedback);
            }

            if (desc.length > 500) {
                showInputError(genreDescInput, descFeedback, "Mô tả không được vượt quá 500 ký tự.");
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
                    var message = result.message || (result.errors ? result.errors.join(", ") : "Đã xảy ra lỗi");
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

    // Event delegation for movie count badge clicks
    document.addEventListener("click", function (e) {
        var badge = e.target.closest(".btn-view-movies");
        if (badge) {
            var id = badge.getAttribute("data-id");
            var name = badge.getAttribute("data-name");
            viewAssociatedMovies(id, name);
        }
    });

    // Event delegation for safe delete forms
    document.addEventListener("submit", function (e) {
        var deleteForm = e.target.closest(".delete-genre-form");
        if (deleteForm) {
            var name = deleteForm.getAttribute("data-name") || "thể loại này";
            if (!confirm("Bạn có chắc chắn muốn xóa thể loại '" + name + "'?")) {
                e.preventDefault();
            }
        }
    });

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
            button.innerHTML = '<span class="spinner"></span> Đang lưu...';
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
