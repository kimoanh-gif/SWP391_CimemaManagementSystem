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
            document.getElementById("modalTitle").innerText = "Tạo Quy Tắc Định Giá Mới";
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
            document.getElementById("modalTitle").innerText = "Chỉnh Sửa Quy Tắc Định Giá";
            modal.classList.add("open");
        });
    });
});
