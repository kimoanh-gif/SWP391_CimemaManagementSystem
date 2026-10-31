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

    const modalDeleteBtn = document.getElementById("btnModalDeleteFB");

    if (openCreateBtn) {
        openCreateBtn.addEventListener("click", () => {
            if (modalDeleteBtn) modalDeleteBtn.style.display = "none";
            document.getElementById("formItemId").value = "";
            document.getElementById("formItemCode").value = "";
            document.getElementById("formItemName").value = "";
            document.getElementById("formCategory").value = "Combo";
            document.getElementById("formDescription").value = "";
            document.getElementById("formSellingPrice").value = "79000";
            document.getElementById("formCostPrice").value = "25000";
            document.getElementById("formImage").value = "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=400";
            document.getElementById("modalTitle").innerText = "Add new product";
            toggleComboSection();
            modal.classList.add("open");
        });
    }

    if (modalDeleteBtn) {
        modalDeleteBtn.addEventListener("click", () => {
            const id = document.getElementById("formItemId").value;
            const name = document.getElementById("formItemName").value;
            if (!id) return;
            if (confirm("Are you sure you want to delete '" + (name || id) + "' from the menu?")) {
                const form = document.createElement("form");
                form.method = "POST";
                form.action = "/manager/fb/" + encodeURIComponent(id) + "/delete";

                const csrfInput = document.querySelector('input[name="_csrf"]');
                if (csrfInput) {
                    const token = document.createElement("input");
                    token.type = "hidden";
                    token.name = "_csrf";
                    token.value = csrfInput.value;
                    form.appendChild(token);
                }

                document.body.appendChild(form);
                form.submit();
            }
        });
    }

    document.querySelectorAll(".modal-close, .btn-cancel").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modal) modal.classList.remove("open");
        });
    });

    // Quick edit product
    const fbFormEl = modal ? modal.querySelector("form") : null;
    if (fbFormEl) {
        fbFormEl.addEventListener("submit", (e) => {
            const code = (document.getElementById("formItemCode")?.value || "").trim();
            const name = (document.getElementById("formItemName")?.value || "").trim();
            const sellingPrice = parseFloat(document.getElementById("formSellingPrice")?.value || "0");
            const costPrice = parseFloat(document.getElementById("formCostPrice")?.value || "0");
            const category = document.getElementById("formCategory")?.value || "Combo";
            const itemId = document.getElementById("formItemId")?.value || "";
            if (!/^[A-Za-z0-9_-]{2,50}$/.test(code)) {
                e.preventDefault();
                alert("Mã sản phẩm (SKU) chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới (2-50 ký tự).");
                return;
            }
            if (!name || name.length > 150) {
                e.preventDefault();
                alert("Tên sản phẩm không được để trống và tối đa 150 ký tự.");
                return;
            }
            if (isNaN(sellingPrice) || sellingPrice < 0 || isNaN(costPrice) || costPrice < 0) {
                e.preventDefault();
                alert("Giá bán và giá vốn không được là số âm.");
                return;
            }
            if (costPrice > sellingPrice) {
                e.preventDefault();
                alert("Giá vốn không được lớn hơn giá bán.");
                return;
            }
            if (category === "Combo" && !itemId) {
                const checked = fbFormEl.querySelectorAll(".combo-comp-cb:checked");
                if (checked.length === 0) {
                    e.preventDefault();
                    alert("Gói Combo phải chọn ít nhất 1 mặt hàng thành phần.");
                }
            }
        });
    }
    document.querySelectorAll(".btn-edit-fb").forEach(btn => {
        btn.addEventListener("click", () => {
            if (modalDeleteBtn) modalDeleteBtn.style.display = "inline-flex";
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
            document.getElementById("modalTitle").innerText = "Edit product #" + code;

            toggleComboSection();
            modal.classList.add("open");
        });
    });
});
