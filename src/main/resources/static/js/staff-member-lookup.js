(() => {
  const dialog = document.getElementById("memberLookupDialog");
  const openButton = document.getElementById("openMemberLookup");
  const closeButton = document.getElementById("closeMemberLookup");
  const form = document.getElementById("memberLookupForm");
  const input = document.getElementById("memberLookupValue");
  const result = document.getElementById("memberLookupResult");
  const message = document.getElementById("memberLookupMessage");
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
  const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });

  openButton.addEventListener("click", () => {
    dialog.showModal();
    input.focus();
  });
  closeButton.addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  function addField(parent, labelText, value) {
    const item = document.createElement("div");
    item.className = "member-detail";
    const label = document.createElement("span");
    label.textContent = labelText;
    const text = document.createElement("strong");
    text.textContent = value || "Chưa cập nhật";
    item.append(label, text);
    parent.append(item);
  }

  function showMember(member) {
    result.replaceChildren();
    const card = document.createElement("section");
    card.className = "member-result-card";
    const heading = document.createElement("h3");
    heading.textContent = member.fullName;
    card.append(heading);
    addField(card, "Hạng thành viên", member.membershipRank || "Chưa xếp hạng");
    addField(card, "Điểm tích lũy", String(member.availablePoints ?? 0));
    addField(card, "Điểm hạng", String(member.tierPoints ?? 0));
    addField(card, "Điện thoại", member.phone);
    addField(card, "Email", member.email);

    const vouchersHeading = document.createElement("h4");
    vouchersHeading.textContent = `Voucher khả dụng (${member.vouchers.length})`;
    card.append(vouchersHeading);
    if (member.vouchers.length === 0) {
      const empty = document.createElement("p");
      empty.className = "staff-muted";
      empty.textContent = "Khách hàng hiện không có voucher khả dụng.";
      card.append(empty);
    } else {
      const vouchers = document.createElement("ul");
      vouchers.className = "member-voucher-list";
      member.vouchers.forEach((voucher) => {
        const item = document.createElement("li");
        const title = document.createElement("strong");
        title.textContent = `${voucher.code} · ${voucher.promotionName}`;
        const detail = document.createElement("span");
        const discount = voucher.discountType.toUpperCase() === "PERCENTAGE"
          ? `${voucher.discountValue}%`
          : money.format(voucher.discountValue);
        detail.textContent = `${discount}${voucher.description ? ` — ${voucher.description}` : ""}`;
        item.append(title, detail);
        vouchers.append(item);
      });
      card.append(vouchers);
    }
    result.append(card);
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    result.replaceChildren();
    message.textContent = "Đang tìm kiếm...";
    message.classList.remove("is-error");
    const value = input.value.trim();
    const key = value.includes("@") ? "email" : "phone";
    const params = new URLSearchParams({ [key]: value });
    try {
      const response = await fetch(`/api/staff/members/search?${params}`, {
        headers: csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {}
      });
      const body = await response.json();
      if (!response.ok) {
        throw new Error(body.detail || body.message || body.error || "Không thể tra cứu thành viên.");
      }
      showMember(body);
      message.textContent = "";
    } catch (error) {
      message.textContent = error.message;
      message.classList.add("is-error");
    }
  });
})();
