// Ticket & Receipt Printing (UC24): renders entry QR codes and drives browser printing.
(() => {
  const printAll = document.getElementById("printAll");
  if (!printAll) return;

  const renderQr = (box, url) => {
    if (typeof QRCode === "undefined") {
      box.textContent = "QR";
      return;
    }
    box.replaceChildren();
    box.dataset.qr = url;
    box.title = url;
    // Drawn at 300px and scaled down on screen by CSS, so the enlarged printed QR stays sharp.
    new QRCode(box, { text: url, width: 300, height: 300, correctLevel: QRCode.CorrectLevel.M });
  };
  document.querySelectorAll(".ticket-qr[data-qr]").forEach((box) => renderQr(box, box.dataset.qr));

  // Same behaviour as the customer ticket page: follow the current Cloudflare Quick Tunnel URL,
  // so a phone can open /ticket/verify/{code} even if the tunnel started after this page loaded.
  const refreshTunnelQrs = async () => {
    try {
      const response = await fetch("/staff/qr-base-url", { cache: "no-store" });
      if (!response.ok) return;
      const { baseUrl } = await response.json();
      if (!baseUrl) return;
      document.querySelectorAll(".ticket-qr[data-ticket-code]").forEach((box) => {
        const url = `${baseUrl}/ticket/verify/counter/${encodeURIComponent(box.dataset.ticketCode)}`;
        if (box.dataset.qr !== url) renderQr(box, url);
      });
    } catch (_) {
      // Keep the most recently rendered QR while cloudflared is restarting.
    }
  };
  refreshTunnelQrs();
  window.setInterval(refreshTunnelQrs, 5000);

  printAll.addEventListener("click", () => {
    delete document.body.dataset.printTicket;
    window.print();
  });

  // Re-print a single ticket: only that ticket is shown in the print output.
  document.querySelectorAll(".print-reprint").forEach((button) => {
    button.addEventListener("click", () => {
      const ticket = document.getElementById(`ticket-${button.dataset.ticketId}`);
      if (!ticket) return;
      document.querySelectorAll(".print-ticket.print-selected")
        .forEach((node) => node.classList.remove("print-selected"));
      ticket.classList.add("print-selected");
      document.body.dataset.printTicket = button.dataset.ticketId;
      window.print();
    });
  });

  window.addEventListener("afterprint", () => {
    delete document.body.dataset.printTicket;
    document.querySelectorAll(".print-ticket.print-selected")
      .forEach((node) => node.classList.remove("print-selected"));
  });
})();
