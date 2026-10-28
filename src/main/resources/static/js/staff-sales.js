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
  }

  // Counts down to an ISO deadline; onDone runs once when it reaches zero.
  function countdown(element, deadlineIso, onDone) {
    if (!element || !deadlineIso) return;
    const deadline = new Date(deadlineIso).getTime();
    const tick = () => {
      const remaining = Math.max(0, Math.floor((deadline - Date.now()) / 1000));
      element.textContent = `${String(Math.floor(remaining / 60)).padStart(2, "0")}:${String(remaining % 60).padStart(2, "0")}`;
      if (remaining === 0) {
        window.clearInterval(timer);
        element.classList.add("staff-expired");
        if (onDone) onDone();
      }
    };
    const timer = window.setInterval(tick, 1000);
    tick();
  }

  if (page === "foods" || page === "payment") {
    countdown(document.getElementById("staffHoldCountdown"), document.body.dataset.heldUntil);
  }

  if (page === "payment") {
    const form = document.getElementById("counterPaymentForm");
    const cashFields = document.getElementById("cashFields");
    const cashInput = document.getElementById("cashReceived");
    const changeDue = document.getElementById("changeDue");
    const cashError = document.getElementById("cashError");
    const payButton = document.getElementById("payButton");
    const total = Number(document.body.dataset.total || 0);
    const method = () => form?.querySelector('input[name="method"]:checked')?.value || "CASH";

    function updatePaymentForm() {
      const isCash = method() === "CASH";
      if (cashFields) cashFields.hidden = !isCash;
      if (cashInput) cashInput.required = isCash;
      const cash = Number(cashInput?.value || 0);
      const short = isCash && cashInput?.value !== "" && cash < total;
      if (changeDue) changeDue.textContent = currency(Math.max(0, cash - total));
      if (cashError) cashError.hidden = !short;
      if (payButton) payButton.textContent = isCash ? "Confirm & Paid" : "Display QR Code";
    }
    form?.querySelectorAll('input[name="method"]').forEach((radio) => radio.addEventListener("change", updatePaymentForm));
    cashInput?.addEventListener("input", updatePaymentForm);
    form?.addEventListener("submit", (event) => {
      if (method() === "CASH" && Number(cashInput?.value || 0) < total) {
        event.preventDefault();
        updatePaymentForm();
        if (cashError) cashError.hidden = false;
        cashInput?.focus();
        return;
      }
      if (payButton) payButton.disabled = true;
    });
    updatePaymentForm();

    // An expired QR is marked on the server; reloading shows the "display a new QR" state.
    countdown(document.getElementById("paymentCountdown"), document.body.dataset.paymentExpires,
      () => window.setTimeout(() => window.location.reload(), 800));

    const qr = document.getElementById("vnpayQr");
    if (qr && qr.dataset.paymentUrl && window.QRCode) {
      new window.QRCode(qr, { text: qr.dataset.paymentUrl, width: 200, height: 200 });
    }

    const statusUrl = document.body.dataset.statusUrl;
    const doneUrl = document.body.dataset.doneUrl;
    const gatewayStatus = document.getElementById("gatewayStatus");
    if (statusUrl && doneUrl) {
      const poll = window.setInterval(async () => {
        try {
          const response = await fetch(statusUrl, { credentials: "same-origin", headers: { Accept: "application/json" } });
          if (!response.ok) return;
          const { status } = await response.json();
          if (status === "PAID") {
            window.clearInterval(poll);
            if (gatewayStatus) gatewayStatus.textContent = "Payment received. Opening the booking…";
            window.location.href = doneUrl;
          } else if (["FAILED", "CANCELLED", "EXPIRED"].includes(status)) {
            window.clearInterval(poll);
            if (gatewayStatus) gatewayStatus.textContent = `Payment ${status.toLowerCase()}. Display a new QR code or take cash.`;
          }
        } catch (_) {
          // A dropped poll is retried on the next tick.
        }
      }, 1500);
    }
  }
})();
