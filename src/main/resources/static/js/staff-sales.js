(() => {
  const page = document.body.dataset.page;
  // Same "90,000 VND" format the Thymeleaf templates render server-side.
  const currency = (amount) => `${new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(amount)} VND`;

  if (page === "seats") {
    const form = document.getElementById("staffSeatForm");
    const count = document.getElementById("selectedSeatCount");
    const labels = document.getElementById("selectedSeatLabels");
    const total = document.getElementById("selectedSeatTotal");
    const button = document.getElementById("holdSeatsButton");
    const limitMessage = document.getElementById("seatLimitMessage");
    if (!form || !count || !labels || !total || !button || !limitMessage) return;
    const max = Number(document.body.dataset.maxSeats || 10);
    const boxes = [...form.querySelectorAll('input[name="seatIds"]:not(:disabled)')];
    const seatLabel = (box) => box.closest(".staff-seat")?.querySelector(".seat-btn")?.textContent.trim() || box.value;
    const hint = document.getElementById("holdSeatsHint");
    const canSell = button.dataset.canSell !== "false";

    function updateSeatSummary() {
      const selected = boxes.filter((box) => box.checked);
      const overLimit = selected.length > max;
      count.textContent = String(selected.length);
      labels.textContent = selected.map(seatLabel).join(", ") || "None";
      total.textContent = currency(selected.reduce((sum, box) => sum + Number(box.dataset.price || 0), 0));
      button.disabled = !canSell || selected.length === 0 || overLimit;
      limitMessage.hidden = !overLimit;
      if (hint) {
        hint.textContent = !canSell ? "Only staff accounts can sell tickets."
          : selected.length === 0 ? "Select at least one seat to continue."
          : "Seats will be held for 10 minutes.";
      }
    }
    boxes.forEach((box) => box.addEventListener("change", updateSeatSummary));
    form.addEventListener("submit", (event) => {
      if (!canSell || !boxes.some((box) => box.checked) || boxes.filter((box) => box.checked).length > max) {
        event.preventDefault();
        updateSeatSummary();
      }
    });
    updateSeatSummary();
  }

  if (page === "foods") {
    const quantities = [...document.querySelectorAll('input[name="quantities"]')];
    const foodTotal = document.getElementById("staffFoodTotal");
    const grandTotal = document.getElementById("staffGrandTotal");
    if (!foodTotal || !grandTotal) return;
    const seatTotal = Number(grandTotal.dataset.seatTotal || 0);
    function updateFoodTotal() {
      const sum = quantities.reduce((total, input) => {
        const quantity = Math.max(0, Number.parseInt(input.value || "0", 10) || 0);
        input.value = String(Math.min(20, quantity));
        return total + Number(input.dataset.price || 0) * quantity;
      }, 0);
      foodTotal.textContent = currency(sum);
      grandTotal.textContent = currency(seatTotal + sum);
    }
    quantities.forEach((input) => input.addEventListener("input", updateFoodTotal));
    updateFoodTotal();

    const countdown = document.getElementById("staffHoldCountdown");
    const heldUntil = document.body.dataset.heldUntil;
    if (countdown && heldUntil) {
      const deadline = new Date(heldUntil).getTime();
      const timer = window.setInterval(() => {
        const remaining = Math.max(0, Math.floor((deadline - Date.now()) / 1000));
        countdown.textContent = `${String(Math.floor(remaining / 60)).padStart(2, "0")}:${String(remaining % 60).padStart(2, "0")}`;
        if (remaining === 0) {
          window.clearInterval(timer);
          countdown.classList.add("staff-expired");
        }
      }, 1000);
    }
  }
})();
