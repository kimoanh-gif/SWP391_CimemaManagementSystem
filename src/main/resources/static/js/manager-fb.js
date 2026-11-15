function getCsrfToken() {
    var metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken && metaToken.getAttribute("content")) return metaToken.getAttribute("content");
    var inputToken = document.querySelector('input[name="_csrf"]');
    if (inputToken && inputToken.value) return inputToken.value;
    var match = document.cookie.match(new RegExp('(^|;\\s*)XSRF-TOKEN=([^;]*)'));
    return match ? decodeURIComponent(match[2]) : "";
}

function updateImagePreview(url) {
    const container = document.getElementById("imagePreviewContainer");
    const img = document.getElementById("imagePreviewImg");
    if (!container || !img) return;
    if (url && url.trim()) {
        img.src = url.trim();
        container.style.display = "flex";
    } else {
        img.src = "";
        container.style.display = "none";
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("fbModal");
    const openCreateBtn = document.getElementById("btnCreateFB");

    const catSelect = document.getElementById("formCategory");
    const comboSection = document.getElementById("comboComponentsSection");

    const formImageInput = document.getElementById("formImage");
    const fileInput = document.getElementById("formImageFile");

    if (formImageInput) {
        formImageInput.addEventListener("input", () => {
            updateImagePreview(formImageInput.value);
        });
    }

    if (fileInput) {
        fileInput.addEventListener("change", async () => {
            const file = fileInput.files && fileInput.files[0];
            if (!file) return;

            if (file.size > 5 * 1024 * 1024) {
                alert("Image file size exceeds 5MB. Please choose a smaller image.");
                fileInput.value = "";
                return;
            }

            const objectUrl = URL.createObjectURL(file);
            updateImagePreview(objectUrl);

            const textSpan = fileInput.parentElement ? fileInput.parentElement.querySelector(".upload-text") : null;
            const oldText = textSpan ? textSpan.textContent : "Upload image";
            if (textSpan) textSpan.textContent = "Uploading...";

            const formData = new FormData();
            formData.append("file", file);

            try {
                const headers = {};
                const csrf = getCsrfToken();
                if (csrf) headers["X-XSRF-TOKEN"] = csrf;

                const response = await fetch("/api/manager/fb/upload-image", {
                    method: "POST",
                    headers: headers,
                    body: formData
                });

                const result = await response.json();
                if (response.ok && result.success && result.data && result.data.url) {
                    if (formImageInput) {
                        formImageInput.value = result.data.url;
                    }
                    updateImagePreview(result.data.url);
                } else {
                    const msg = (result && result.message) ? result.message : "Image upload failed";
                    alert("Image upload error: " + msg);
                }
            } catch (err) {
                alert("Connection error during upload: " + err.message);
            } finally {
                if (textSpan) textSpan.textContent = oldText;
            }
        });
    }

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
            if (fileInput) fileInput.value = "";
            updateImagePreview(document.getElementById("formImage").value);
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

    // Form submit relies on centralized backend validation
    const fbFormEl = modal ? modal.querySelector("form") : null;
    if (fbFormEl) {
        fbFormEl.addEventListener("submit", () => {
            const modalAlert = document.getElementById("modalFbError");
            if (modalAlert) {
                modalAlert.style.display = "none";
                modalAlert.innerText = "";
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

            if (fileInput) fileInput.value = "";
            updateImagePreview(image);

            toggleComboSection();
            modal.classList.add("open");
        });
    });
});
