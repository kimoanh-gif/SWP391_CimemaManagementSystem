// Ticket & Receipt Printing (UC24): renders entry QR codes and drives browser printing.
(() => {
  const printAll = document.getElementById("printAll");
  if (!printAll) return;

  document.querySelectorAll(".ticket-qr[data-qr]").forEach((box) => {
    if (typeof QRCode === "undefined") {
      box.textContent = "QR";
      return;
    }
    new QRCode(box, { text: box.dataset.qr, width: 104, height: 104, correctLevel: QRCode.CorrectLevel.M });
  });

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
