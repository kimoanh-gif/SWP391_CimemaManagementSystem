(() => {
  const dialog = document.getElementById("memberLookupDialog");
  const openButton = document.getElementById("openMemberLookup");
  const closeButton = document.getElementById("closeMemberLookup");
  const form = document.getElementById("memberLookupForm");
  const input = document.getElementById("memberLookupValue");
  const message = document.getElementById("memberLookupMessage");
  const result = document.getElementById("memberLookupResult");

  if (!dialog || !openButton || !closeButton || !form || !input || !message || !result) return;

  // On the seat page the dialog can attach the member to the order being created.
  const memberInput = document.getElementById("memberIdentifier");
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const phonePattern = /^\+?[0-9]{9,15}$/;
  const money = (amount) => `${new Intl.NumberFormat("en-US").format(Number(amount) || 0)} VND`;

  openButton.addEventListener("click", () => {
    if (memberInput && memberInput.value.trim()) input.value = memberInput.value.trim();
    setMessage("");
    result.replaceChildren();
    dialog.showModal();
    input.focus();
  });
  closeButton.addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const value = input.value.trim();
    result.replaceChildren();

    const isEmail = value.includes("@");
    if (!value) return setMessage("Enter a phone number or email address.", true);
    if (isEmail && !emailPattern.test(value)) return setMessage("Enter a valid email address.", true);
    if (!isEmail && !phonePattern.test(value.replace(/[\s.\-]/g, ""))) {
      return setMessage("Enter a valid phone number (9-15 digits).", true);
    }

    setMessage("Searching...");
    const submit = form.querySelector('button[type="submit"]');
    if (submit) submit.disabled = true;
    try {
      const response = await fetch(`/api/staff/members/search?q=${encodeURIComponent(value)}`, {
        headers: { Accept: "application/json" },
        credentials: "same-origin"
      });
      if (response.redirected || response.status === 401) {
        return setMessage("Your session has expired. Please sign in again.", true);
      }
      const payload = await response.json().catch(() => null);
      if (!response.ok || !payload?.success || !payload.data) {
        return setMessage(payload?.message || "No matching member was found.", true);
      }
      setMessage("");
      renderMember(payload.data, value);
    } catch (error) {
      setMessage("Unable to connect. Please try again.", true);
    } finally {
      if (submit) submit.disabled = false;
    }
  });

  function setMessage(text, isError = false) {
    message.textContent = text;
    message.classList.toggle("is-error", isError);
  }

  function renderMember(member, searchedValue) {
    const card = el("article", "member-card");

    const head = el("div", "member-card-head");
    head.append(el("h3", "", member.fullName || "Member"),
      el("span", "badge badge-confirmed", member.membershipRank || "MEMBER"));
    card.append(head);

    const details = el("dl", "member-details");
    addDetail(details, "Phone", member.phone);
    addDetail(details, "Email", member.email);
    addDetail(details, "Available Points", formatNumber(member.availablePoints));
    addDetail(details, "Tier Points", formatNumber(member.tierPoints));
    card.append(details);

    card.append(el("p", "member-section-title", "Available Vouchers"));
    const vouchers = Array.isArray(member.vouchers) ? member.vouchers : [];
    if (vouchers.length === 0) {
      card.append(el("p", "member-muted", "This member has no available vouchers."));
    } else {
      const list = el("ul", "member-voucher-list");
      vouchers.forEach((voucher) => {
        const item = document.createElement("li");
        // Promotion names/descriptions are stored in Vietnamese, so the staff UI builds an English summary instead.
        item.append(el("strong", "", voucher.code || ""), el("span", "", voucherSummary(voucher)));
        list.append(item);
      });
      card.append(list);
    }

    if (memberInput) {
      const attach = el("button", "btn btn-primary", "Attach Member to Order");
      attach.type = "button";
      attach.style.width = "100%";
      attach.style.justifyContent = "center";
      attach.addEventListener("click", () => {
        // Keep what staff typed when it identifies this member; fall back to the stored phone/email.
        memberInput.value = searchedValue || member.phone || member.email || "";
        memberInput.dispatchEvent(new Event("change", { bubbles: true }));
        dialog.close();
      });
      card.append(attach);
    }
    result.replaceChildren(card);
  }

  function voucherSummary(voucher) {
    const type = String(voucher.discountType || "").toUpperCase();
    const parts = [type.startsWith("PERCENT")
      ? `${Number(voucher.discountValue) || 0}% off`
      : `${money(voucher.discountValue)} off`];
    if (voucher.maxDiscountAmount != null) parts.push(`up to ${money(voucher.maxDiscountAmount)}`);
    if (voucher.minBookingAmount != null) parts.push(`min. order ${money(voucher.minBookingAmount)}`);
    if (voucher.validTo) {
      const validTo = new Date(voucher.validTo);
      if (!Number.isNaN(validTo.getTime())) {
        parts.push(`valid until ${validTo.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })}`);
      }
    }
    return parts.join(" · ");
  }

  function formatNumber(value) {
    return value == null ? null : new Intl.NumberFormat("en-US").format(value);
  }

  function addDetail(list, label, value) {
    list.append(el("dt", "", label), el("dd", "", value == null || value === "" ? "—" : String(value)));
  }

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }
})();
