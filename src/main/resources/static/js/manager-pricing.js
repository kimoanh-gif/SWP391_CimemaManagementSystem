document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("pricingModal");
    const multModal = document.getElementById("multiplierModal");
    const openCreateBtn = document.getElementById("btnCreatePricing");

    if (openCreateBtn) {
        openCreateBtn.addEventListener("click", () => {
            document.getElementById("formPricingId").value = "";
            document.getElementById("formPricingCode").value = "";
            document.getElementById("formRuleName").value = "";
            document.getElementById("formBasePrice").value = "85000";
            document.getElementById("formFormatSurcharge").value = "0";
            document.getElementById("formDayType").value = "WEEKDAY";
            document.getElementById("formTimeSlot").value = "STANDARD";
            document.getElementById("formExperienceType").value = "2D Digital";
            document.getElementById("modalTitle").innerText = "Add price configuration";
            modal.classList.add("open");
        });
    }

    document.querySelectorAll(".modal-close, .btn-cancel").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modal) modal.classList.remove("open");
            if (multModal) multModal.classList.remove("open");
        });
    });

    // Multiplier edit trigger
    document.querySelectorAll(".btn-edit-multiplier").forEach(btn => {
        btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-id");
            const name = btn.getAttribute("data-name");
            const currentMult = btn.getAttribute("data-multiplier");

            document.getElementById("multSeatTypeId").value = id;
            document.getElementById("multTypeName").innerText = name;
            document.getElementById("multInput").value = currentMult;
            multModal.classList.add("open");
        });
    });

    // Quick edit base price
    const pricingFormEl = modal ? modal.querySelector("form") : null;
    if (pricingFormEl) {
        pricingFormEl.addEventListener("submit", (e) => {
            const code = (document.getElementById("formPricingCode")?.value || "").trim();
            const name = (document.getElementById("formRuleName")?.value || "").trim();
            const basePrice = parseFloat(document.getElementById("formBasePrice")?.value || "0");
            const surcharge = parseFloat(document.getElementById("formFormatSurcharge")?.value || "0");
            if (!/^[A-Za-z0-9_-]{2,50}$/.test(code)) {
                e.preventDefault();
                alert("Rule code must contain only letters, numbers, hyphens or underscores (2-50 characters).");
                return;
            }
            if (!name || name.length > 150) {
                e.preventDefault();
                alert("Rule name cannot be empty and max 150 characters.");
                return;
            }
            if (isNaN(basePrice) || basePrice < 0 || basePrice > 10000000 || isNaN(surcharge) || surcharge < 0 || surcharge > 10000000) {
                e.preventDefault();
                alert("Base price and format surcharge must be between 0 and 10,000,000 VND.");
            }
        });
    }
    const multFormEl = multModal ? multModal.querySelector("form") : null;
    if (multFormEl) {
        multFormEl.addEventListener("submit", (e) => {
            const mult = parseFloat(document.getElementById("multInput")?.value || "0");
            if (isNaN(mult) || mult < 0.5 || mult > 3.0) {
                e.preventDefault();
                alert("Seat price multiplier must be between 0.50 and 3.00.");
            }
        });
    }
    document.querySelectorAll(".btn-edit-rule").forEach(btn => {
        btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-id");
            const code = btn.getAttribute("data-code");
            const name = btn.getAttribute("data-name");
            const basePrice = btn.getAttribute("data-baseprice");
            const surcharge = btn.getAttribute("data-surcharge");
            const dayType = btn.getAttribute("data-daytype");
            const timeSlot = btn.getAttribute("data-timeslot");
            const expType = btn.getAttribute("data-exptype");

            document.getElementById("formPricingId").value = id;
            document.getElementById("formPricingCode").value = code;
            document.getElementById("formRuleName").value = name;
            document.getElementById("formBasePrice").value = basePrice;
            document.getElementById("formFormatSurcharge").value = surcharge;
            document.getElementById("formDayType").value = dayType;
            document.getElementById("formTimeSlot").value = timeSlot;
            document.getElementById("formExperienceType").value = expType;
            document.getElementById("modalTitle").innerText = "Edit price configuration";
            modal.classList.add("open");
        });
    });
});
