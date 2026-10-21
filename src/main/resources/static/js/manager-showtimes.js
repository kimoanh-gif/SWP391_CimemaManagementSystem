document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("showtimeModal");
    const openCreateBtn = document.getElementById("btnCreateShowtime");
    const movieSelect = document.getElementById("formMovieId");
    const startTimeInput = document.getElementById("formStartTime");
    const endTimeDisplay = document.getElementById("endTimeDisplay");

    if (openCreateBtn) {
        openCreateBtn.addEventListener("click", () => {
            document.getElementById("formShowtimeId").value = "";
            document.getElementById("formMovieId").value = "";
            document.getElementById("formRoomId").value = "";
            document.getElementById("formBaseTicketPrice").value = "85000";
            document.getElementById("modalTitle").innerText = "Create New Showtime (BR-07, BR-08, BR-09)";
            if (endTimeDisplay) endTimeDisplay.innerText = "--:--";

            // Default time to tomorrow at 10:00 AM
            const now = new Date();
            now.setDate(now.getDate() + 1);
            now.setHours(10, 0, 0, 0);
            const iso = now.toISOString().slice(0, 16);
            startTimeInput.value = iso;

            modal.classList.add("open");
        });
    }

    document.querySelectorAll(".modal-close, .btn-cancel").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modal) modal.classList.remove("open");
        });
    });

    // Auto-calculate expected end time using movie duration (BR-55)
    function updateEndTimePreview() {
        if (!movieSelect || !startTimeInput || !endTimeDisplay) return;
        const selectedOption = movieSelect.options[movieSelect.selectedIndex];
        if (!selectedOption || !startTimeInput.value) return;

        const duration = parseInt(selectedOption.getAttribute("data-duration") || "120", 10);
        const start = new Date(startTimeInput.value);
        if (isNaN(start.getTime())) return;

        const end = new Date(start.getTime() + duration * 60000);
        const endStr = end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) +
            " (" + duration + " min + 15 min cleaning)";
        endTimeDisplay.innerText = endStr;
    }

    if (movieSelect) movieSelect.addEventListener("change", updateEndTimePreview);
    if (startTimeInput) startTimeInput.addEventListener("input", updateEndTimePreview);

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
            document.getElementById("formStartTime").value = startTime;
            document.getElementById("formBaseTicketPrice").value = price;
            document.getElementById("modalTitle").innerText = "Edit Showtime #" + id;

            updateEndTimePreview();
            modal.classList.add("open");
        });
    });
});
