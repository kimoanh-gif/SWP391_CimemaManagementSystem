document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("fbModal");
    const openCreateBtn = document.getElementById("btnCreateFB");

    const catSelect = document.getElementById("formCategory");
    const comboSection = document.getElementById("comboComponentsSection");

    function toggleComboSection() {
        if (comboSection && catSelect) {
            comboSection.style.display = (catSelect.value === "Combo") ? "block" : "none";
        }
    }

    if (catSelect) {
        catSelect.addEventListener("change", toggleComboSection);
    }

    if (openCreateBtn) {
        openCreateBtn.addEventListener("click", () => {
            document.getElementById("formItemId").value = "";
            document.getElementById("formItemCode").value = "";
            document.getElementById("formItemName").value = "";
            document.getElementById("formCategory").value = "Combo";
            document.getElementById("formDescription").value = "";
            document.getElementById("formSellingPrice").value = "79000";
            document.getElementById("formCostPrice").value = "25000";
            document.getElementById("formImage").value = "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=400";
            document.getElementById("modalTitle").innerText = "Thêm Món / Combo F&B Mới";
            toggleComboSection();
            modal.classList.add("open");
        });
    }

    document.querySelectorAll(".modal-close, .btn-cancel").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modal) modal.classList.remove("open");
        });
    });

    // Quick edit product
    document.querySelectorAll(".btn-edit-fb").forEach(btn => {
        btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-id");
            const code = btn.getAttribute("data-code");
            const name = btn.getAttribute("data-name");
            const category = btn.getAttribute("data-category");
            const desc = btn.getAttribute("data-desc");
            const sellingPrice = btn.getAttribute("data-sellingprice");
            const costPrice = btn.getAttribute("data-costprice");
            const image = btn.getAttribute("data-image");

            document.getElementById("formItemId").value = id;
            document.getElementById("formItemCode").value = code;
            document.getElementById("formItemName").value = name;
            document.getElementById("formCategory").value = category;
            document.getElementById("formDescription").value = desc;
            document.getElementById("formSellingPrice").value = sellingPrice;
            document.getElementById("formCostPrice").value = costPrice;
            document.getElementById("formImage").value = image;
            document.getElementById("modalTitle").innerText = "Chỉnh Sửa Sản Phẩm #" + code;

            toggleComboSection();
            modal.classList.add("open");
        });
    });
});
