(() => {
  const currency = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });
  const formatCurrency = (amount) => `${currency.format(amount)} ₫`;

  const setupSeats = () => {
    const checkboxes = [...document.querySelectorAll('input[name="seatIds"]')];
    const count = document.querySelector("#selected-count");
    const labels = document.querySelector("#selected-labels");
    const total = document.querySelector("#seat-total");
    const continueButton = document.querySelector("#continue-button");
    const limitMessage = document.querySelector("#seat-limit-message");
    const maxSeats = Number(document.body.dataset.maxSeats || 10);

    const refresh = (changed) => {
      let selected = checkboxes.filter((input) => input.checked);
      if (selected.length > maxSeats && changed) {
        changed.checked = false;
        if (limitMessage) limitMessage.hidden = false;
        selected = checkboxes.filter((input) => input.checked);
      } else if (limitMessage) {
        limitMessage.hidden = true;
      }
      const seatLabels = selected.map((input) =>
        input.nextElementSibling.textContent.trim(),
      );
      const amount = selected.reduce(
        (sum, input) => sum + Number(input.dataset.price || 0),
        0,
      );
      count.textContent = String(selected.length);
      labels.textContent = seatLabels.length ? seatLabels.join(", ") : "None";
      total.textContent = formatCurrency(amount);
      continueButton.disabled = selected.length === 0;
    };

    checkboxes.forEach((input) =>
      input.addEventListener("change", () => refresh(input)),
    );
    refresh();
  };

  const setupFoods = () => {
    const quantities = [...document.querySelectorAll(".quantity")];
    const foodTotal = document.querySelector("#food-total");
    const grandTotal = document.querySelector("#grand-total");
    const seatTotal = Number(grandTotal?.dataset.seatTotal || 0);

    const refresh = () => {
      const amount = quantities.reduce((sum, input) => {
        const quantity = Math.max(0, Math.min(20, Number(input.value) || 0));
        input.value = String(quantity);
        return sum + quantity * Number(input.dataset.price || 0);
      }, 0);
      if (foodTotal) foodTotal.textContent = formatCurrency(amount);
      if (grandTotal)
        grandTotal.textContent = formatCurrency(seatTotal + amount);
    };

    document.querySelectorAll(".quantity-minus").forEach((button) => {
      button.addEventListener("click", () => {
        const input = button.parentElement.querySelector(".quantity");
        input.value = String(Math.max(0, Number(input.value || 0) - 1));
        refresh();
      });
    });
    document.querySelectorAll(".quantity-plus").forEach((button) => {
      button.addEventListener("click", () => {
        const input = button.parentElement.querySelector(".quantity");
        input.value = String(Math.min(20, Number(input.value || 0) + 1));
        refresh();
      });
    });
    quantities.forEach((input) => input.addEventListener("change", refresh));
    refresh();

    const countdown = document.querySelector("#hold-countdown");
    const heldUntil = document.body.dataset.heldUntil;
    if (!countdown || !heldUntil) return;

    const updateTimer = () => {
      const remaining = Math.max(0, new Date(heldUntil).getTime() - Date.now());
      const minutes = Math.floor(remaining / 60000);
      const seconds = Math.floor((remaining % 60000) / 1000);
      countdown.textContent = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
      if (remaining === 0) {
        clearInterval(timer);
        window.location.href =
          document.body.dataset.expiredUrl || "/customer/booking/seats";
      }
    };
    const timer = window.setInterval(updateTimer, 1000);
    updateTimer();
  };

  if (document.body.dataset.page === "seats") setupSeats();
  if (["foods", "promotions", "checkout"].includes(document.body.dataset.page))
    setupFoods();

  const vatToggle = document.querySelector("#vat-toggle");
  const vatFields = document.querySelector("#vat-fields");
  if (vatToggle && vatFields) {
    vatToggle.addEventListener("change", () => {
      vatFields.hidden = !vatToggle.checked;
      vatFields
        .querySelectorAll("input")
        .forEach((input) => (input.required = vatToggle.checked));
    });
  }

  const voucherButton = document.querySelector("#preview-voucher");
  const voucherMessage = document.querySelector("#voucher-message");
  if (voucherButton && voucherMessage) {
    voucherButton.addEventListener("click", () => {
      voucherMessage.textContent =
        "The code will be verified when the promotion service is connected.";
    });
  }
})();
