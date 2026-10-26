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
    const continueButton = document.querySelector("#food-continue-button");
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
      if (continueButton) continueButton.hidden = amount <= 0;
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

  const setupPayment = () => {
    const vnpayQr = document.querySelector("#vnpay-qr");
    if (vnpayQr && vnpayQr.dataset.paymentUrl && window.QRCode) {
      new window.QRCode(vnpayQr, {
        text: vnpayQr.dataset.paymentUrl,
        width: 230,
        height: 230,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: window.QRCode.CorrectLevel.M,
      });
    }

    const countdown = document.querySelector("#payment-countdown");
    const holdCountdown = document.querySelector("#hold-countdown");
    const paymentExpires = document.body.dataset.paymentExpires;
    const heldUntil = document.body.dataset.heldUntil;
    const formatRemaining = (dateValue) => {
      const remaining = Math.max(0, new Date(dateValue).getTime() - Date.now());
      return {
        remaining,
        text: `${String(Math.floor(remaining / 60000)).padStart(2, "0")}:${String(Math.floor((remaining % 60000) / 1000)).padStart(2, "0")}`,
      };
    };
    let reloading = false;
    const update = () => {
      if (countdown && paymentExpires) {
        const value = formatRemaining(paymentExpires);
        countdown.textContent = value.text;
        if (value.remaining === 0 && !reloading) {
          reloading = true;
          window.setTimeout(() => window.location.reload(), 500);
        }
      }
      if (holdCountdown && heldUntil) holdCountdown.textContent = formatRemaining(heldUntil).text;
    };
    window.setInterval(update, 1000);
    update();

    const statusUrl = document.body.dataset.paymentStatusUrl;
    const successUrl = document.body.dataset.paymentSuccessUrl;
    const gatewayStatus = document.querySelector("#gateway-status");
    const gatewayLink = document.querySelector("#open-vnpay");
    if (statusUrl && successUrl && gatewayLink) {
      if (window.sessionStorage.getItem("vnpayPopupPrepared") === "true") {
        window.sessionStorage.removeItem("vnpayPopupPrepared");
        const paymentWindow = window.open(
          gatewayLink.href,
          "vnpay-payment",
          "popup=yes,width=1000,height=780,resizable=yes,scrollbars=yes",
        );
        if (paymentWindow) paymentWindow.focus();
        else if (gatewayStatus) gatewayStatus.textContent = "Popup was blocked. Click Open VNPay Sandbox.";
      }

      const openSuccessPage = () => {
        if (gatewayStatus) gatewayStatus.textContent = "Payment confirmed. Opening your tickets…";
        window.location.href = successUrl;
      };
      window.addEventListener("message", (event) => {
        if (event.origin !== window.location.origin) return;
        if (event.data?.type === "vnpay-payment-success") openSuccessPage();
      });

      const checkStatus = async () => {
        try {
          const response = await fetch(statusUrl, {
            credentials: "same-origin",
            headers: { Accept: "application/json" },
          });
          if (!response.ok) return;
          const result = await response.json();
          if (result.status === "PAID") {
            openSuccessPage();
          } else if (["FAILED", "CANCELLED", "EXPIRED"].includes(result.status)) {
            if (gatewayStatus) gatewayStatus.textContent = `Payment ${result.status.toLowerCase()}.`;
          }
        } catch (_) {
          // A temporary polling failure should not interrupt the payment window.
        }
      };
      window.setInterval(checkStatus, 750);
      checkStatus();
    }
  };

  const setupCheckout = () => {
    const checkoutForm = document.querySelector("#checkout-form");
    if (!checkoutForm) return;
    checkoutForm.addEventListener("submit", () => {
      const method = checkoutForm.querySelector('input[name="paymentMethod"]:checked')?.value;
      if (method !== "VNPAY") return;
      const paymentWindow = window.open(
        "about:blank",
        "vnpay-payment",
        "popup=yes,width=1000,height=780,resizable=yes,scrollbars=yes",
      );
      if (paymentWindow) {
        paymentWindow.document.title = "Opening VNPay…";
        paymentWindow.document.body.innerHTML = "<p style='font:16px sans-serif;padding:24px'>Opening VNPay Sandbox…</p>";
        window.sessionStorage.setItem("vnpayPopupPrepared", "true");
      }
    });
  };

  const notifyPaymentOpener = () => {
    if (window.name !== "vnpay-payment" || !window.opener || window.opener.closed) return;
    window.opener.postMessage(
      { type: "vnpay-payment-success", bookingId: document.body.dataset.bookingId },
      window.location.origin,
    );
    window.opener.focus();
    window.setTimeout(() => window.close(), 700);
  };

  const setupTicketQrs = () => {
    if (!window.QRCode) return;
    document.querySelectorAll(".ticket-verification-qr").forEach((element) => {
      if (!element.dataset.ticketUrl) return;
      new window.QRCode(element, {
        text: element.dataset.ticketUrl,
        width: 142,
        height: 142,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: window.QRCode.CorrectLevel.M,
      });
    });
  };

  const setupBookingHistory = () => {
    const cards = [...document.querySelectorAll("[data-history-search]")];
    if (!cards.length) return;
    const search = document.querySelector("#history-search");
    const filters = [...document.querySelectorAll("[data-history-filter]")];
    const noResults = document.querySelector("#history-no-results");
    let activeFilter = "all";

    const categoryFor = (card) => {
      const status = (card.dataset.historyStatus || "").toLowerCase();
      if (status.includes("cancel") || status.includes("expire")) return "cancelled";
      const showtime = new Date(card.dataset.historyShowtime || "");
      return status === "confirmed" && showtime.getTime() > Date.now()
        ? "upcoming"
        : "completed";
    };
    const refresh = () => {
      const term = (search?.value || "").trim().toLowerCase();
      let visible = 0;
      cards.forEach((card) => {
        const matchesText = (card.dataset.historySearch || "").includes(term);
        const matchesStatus = activeFilter === "all" || categoryFor(card) === activeFilter;
        card.hidden = !(matchesText && matchesStatus);
        if (!card.hidden) visible += 1;
      });
      if (noResults) noResults.hidden = visible !== 0;
    };

    search?.addEventListener("input", refresh);
    filters.forEach((button) => button.addEventListener("click", () => {
      activeFilter = button.dataset.historyFilter || "all";
      filters.forEach((item) => item.classList.toggle("active", item === button));
      refresh();
    }));
    document.querySelectorAll("[data-copy-code]").forEach((button) => {
      button.addEventListener("click", async () => {
        const code = button.dataset.copyCode;
        if (!code || !navigator.clipboard) return;
        await navigator.clipboard.writeText(code);
        const icon = button.querySelector(".material-symbols-outlined");
        if (icon) icon.textContent = "check";
        window.setTimeout(() => { if (icon) icon.textContent = "content_copy"; }, 1400);
      });
    });
    document.querySelectorAll("[data-ticket-dialog]").forEach((button) => {
      button.addEventListener("click", () => {
        const dialog = document.getElementById(button.dataset.ticketDialog);
        if (dialog?.showModal) dialog.showModal();
      });
    });
    document.querySelectorAll(".history-ticket-dialog").forEach((dialog) => {
      dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();
      });
    });
    refresh();
  };

  if (document.body.dataset.page === "seats") setupSeats();
  if (["foods", "promotions", "checkout"].includes(document.body.dataset.page))
    setupFoods();
  if (document.body.dataset.page === "checkout") setupCheckout();
  if (document.body.dataset.page === "payment") setupPayment();
  if (document.body.dataset.page === "payment-success") notifyPaymentOpener();
  if (document.querySelector(".ticket-verification-qr")) setupTicketQrs();
  if (document.body.classList.contains("booking-history-page")) setupBookingHistory();

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
