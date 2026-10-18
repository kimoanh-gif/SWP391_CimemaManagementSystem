(() => {
  const dialog = document.getElementById("memberLookupDialog");
  const openButton = document.getElementById("openMemberLookup");
  const closeButton = document.getElementById("closeMemberLookup");
  const form = document.getElementById("memberLookupForm");
  const input = document.getElementById("memberLookupValue");
  const message = document.getElementById("memberLookupMessage");
  const result = document.getElementById("memberLookupResult");

  if (!dialog || !openButton || !closeButton || !form || !input || !message || !result) return;

  openButton.addEventListener("click", () => dialog.showModal());
  closeButton.addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const value = input.value.trim();
    result.replaceChildren();
    if (!value) {
      message.textContent = "Enter a phone number or email address.";
      return;
    }

    message.textContent = "Searching...";
    const parameter = value.includes("@") ? "email" : "phone";
    try {
      const response = await fetch(`/api/staff/members/search?${parameter}=${encodeURIComponent(value)}`, {
        headers: { Accept: "application/json" }
      });
      const payload = await response.json().catch(() => null);
      if (!response.ok) {
        message.textContent = payload?.detail || payload?.message || "No matching member was found.";
        return;
      }
      message.textContent = "";
      renderMember(payload, result);
    } catch (error) {
      message.textContent = "Unable to connect. Please try again.";
    }
  });

  function renderMember(member, container) {
    const card = document.createElement("article");
    card.className = "staff-member-card";
    const name = document.createElement("h3");
    name.textContent = member.fullName || "Member";
    card.append(name);

    const details = document.createElement("dl");
    addDetail(details, "Phone", member.phone);
    addDetail(details, "Email", member.email);
    addDetail(details, "Membership Rank", member.membershipRank);
    addDetail(details, "Available Points", member.availablePoints);
    addDetail(details, "Tier Points", member.tierPoints);
    card.append(details);

    const voucherTitle = document.createElement("h4");
    voucherTitle.textContent = "Available Vouchers";
    card.append(voucherTitle);
    const vouchers = Array.isArray(member.vouchers) ? member.vouchers : [];
    if (vouchers.length === 0) {
      const empty = document.createElement("p");
      empty.className = "staff-muted";
      empty.textContent = "This member has no available vouchers.";
      card.append(empty);
    } else {
      const list = document.createElement("ul");
      list.className = "staff-voucher-list";
      vouchers.forEach((voucher) => {
        const item = document.createElement("li");
        const code = document.createElement("strong");
        code.textContent = voucher.code || "";
        const description = document.createElement("span");
        description.textContent = ` ${voucher.promotionName || voucher.description || ""}`;
        item.append(code, description);
        list.append(item);
      });
      card.append(list);
    }
    const memberInput = document.getElementById("memberIdentifier");
    if (memberInput) {
      const selectButton = document.createElement("button");
      selectButton.type = "button";
      selectButton.className = "staff-button staff-button-primary staff-full";
      selectButton.textContent = "Attach Member to Order";
      selectButton.addEventListener("click", () => {
        memberInput.value = member.phone || member.email || "";
        dialog.close();
      });
      card.append(selectButton);
    }
    container.replaceChildren(card);
  }

  function addDetail(list, label, value) {
    const term = document.createElement("dt");
    term.textContent = label;
    const description = document.createElement("dd");
    description.textContent = value == null || value === "" ? "—" : String(value);
    list.append(term, description);
  }
})();
