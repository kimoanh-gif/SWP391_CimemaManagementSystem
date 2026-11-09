document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("showtimeModal");
    const movieSelect = document.getElementById("formMovieId");
    const startTimeInput = document.getElementById("formStartTime");
    const endTimeDisplay = document.getElementById("endTimeDisplay");
    const errorEl = document.getElementById("startTimeError");
    const showtimeForm = document.querySelector("#showtimeModal form");

    const pad = (n) => String(n).padStart(2, '0');

    function clearStartTimeValidation() {
        if (errorEl) {
            errorEl.innerText = "";
            errorEl.style.display = "none";
        }
        if (startTimeInput) startTimeInput.style.borderColor = "";
    }

    function validateStartTime() {
        const isCreate = !document.getElementById("formShowtimeId").value;
        if (!isCreate) {
            clearStartTimeValidation();
            return true;
        }

        if (!startTimeInput || !startTimeInput.value) {
            clearStartTimeValidation();
            return false;
        }

        const selectedDate = new Date(startTimeInput.value);
        const todayEnd = new Date();
        todayEnd.setHours(23, 59, 59, 999);

        if (selectedDate <= todayEnd) {
            if (errorEl) {
                errorEl.innerText = "Cannot select past or today date. Please select a date from tomorrow onward.";
                errorEl.style.display = "block";
            }
            startTimeInput.style.borderColor = "var(--danger, #BA1A1A)";
            return false;
        } else {
            clearStartTimeValidation();
            return true;
        }
    }

    document.querySelectorAll(".modal-close, .btn-cancel").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modal) modal.classList.remove("open");
            clearStartTimeValidation();
        });
    });

    // Auto-calculate expected end time: Film End = Start + Movie Duration; Cleaning = 15m (BR-55)
    function updateEndTimePreview() {
        if (!movieSelect || !startTimeInput || !endTimeDisplay) return;
        const selectedOption = movieSelect.options[movieSelect.selectedIndex];
        if (!selectedOption || !startTimeInput.value) return;

        const duration = parseInt(selectedOption.getAttribute("data-duration") || "120", 10);
        const start = new Date(startTimeInput.value);
        if (isNaN(start.getTime())) return;

        const filmEnd = new Date(start.getTime() + duration * 60000);
        const roomAvail = new Date(filmEnd.getTime() + 15 * 60000);

        const endStr = filmEnd.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) +
            " (Film End) • Room ready at " + roomAvail.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        endTimeDisplay.innerText = endStr;
    }

    if (movieSelect) movieSelect.addEventListener("change", updateEndTimePreview);
    if (startTimeInput) {
        startTimeInput.addEventListener("input", () => {
            validateStartTime();
            updateEndTimePreview();
        });
        startTimeInput.addEventListener("change", () => {
            validateStartTime();
            updateEndTimePreview();
        });
    }

    if (showtimeForm) {
        showtimeForm.addEventListener("submit", (e) => {
            const isCreate = !document.getElementById("formShowtimeId").value;
            if (isCreate && !validateStartTime()) {
                e.preventDefault();
                startTimeInput.focus();
                return false;
            }
        });
    }

    // Edit button click
    document.querySelectorAll(".btn-edit-showtime").forEach(btn => {
        btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-id");
            const movieId = btn.getAttribute("data-movieid");
            const roomId = btn.getAttribute("data-roomid");
            const startTime = btn.getAttribute("data-starttime");
            const price = btn.getAttribute("data-price");

            document.getElementById("formShowtimeId").value = id;
            document.getElementById("formMovieId").value = movieId;
            document.getElementById("formRoomId").value = roomId;
            startTimeInput.removeAttribute("min");
            document.getElementById("formStartTime").value = startTime;
            document.getElementById("formBaseTicketPrice").value = price;
            document.getElementById("modalTitle").innerText = "Edit showtime #" + id;

            clearStartTimeValidation();
            updateEndTimePreview();
            modal.classList.add("open");
        });
    });
});
