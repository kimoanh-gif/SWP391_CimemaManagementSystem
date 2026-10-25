document.addEventListener("DOMContentLoaded", () => {
  const avatarInput = document.getElementById("avatar-input");
  if (avatarInput) {
    avatarInput.addEventListener("change", () => {
      const file = avatarInput.files && avatarInput.files[0];
      if (!file) return;
      if (file.size > 2 * 1024 * 1024) {
        alert("Choose an image up to 2 MB.");
        avatarInput.value = "";
        return;
      }
      let preview = document.getElementById("avatar-preview");
      if (!preview) {
        preview = document.createElement("img");
        preview.id = "avatar-preview";
        preview.alt = "New profile photo preview";
        const holder = document.querySelector(".profile-avatar-edit");
        holder.replaceChildren(preview);
      }
      preview.src = URL.createObjectURL(file);
    });
  }

  const modal = document.getElementById("password-modal");
  if (!modal) return;
  const message = document.getElementById("password-message");
  const requestStep = document.getElementById("password-request-step");
  const updateStep = document.getElementById("password-update-step");
  const otpField = document.getElementById("otp-field");
  const csrfName = document.getElementById("csrf-name").value;
  const csrfToken = document.getElementById("csrf-token").value;

  const showPasswordMessage = (text, type = "") => {
    message.textContent = text;
    message.classList.toggle("password-message-error", type === "error");
    message.classList.toggle("password-message-success", type === "success");
  };

  document.getElementById("open-password-modal").addEventListener("click", () => modal.showModal());
  document.getElementById("request-password-code").addEventListener("click", async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    showPasswordMessage("Sending verification...");
    try {
      const body = new URLSearchParams();
      body.append(csrfName, csrfToken);
      const response = await fetch("/customer/profile/password/request", {method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"}, body});
      const result = await response.json();
      showPasswordMessage(result.message, response.ok ? "success" : "error");
      if (!response.ok) return;
      otpField.hidden = result.firstGoogleSetup === true;
      requestStep.hidden = true;
      updateStep.hidden = false;
      (otpField.hidden ? document.getElementById("new-password") : document.getElementById("password-otp")).focus();
    } catch (error) {
      showPasswordMessage("Could not contact the server. Please try again.", "error");
    } finally {
      button.disabled = false;
    }
  });

  document.getElementById("update-password").addEventListener("click", async (event) => {
    const button = event.currentTarget;
    const newPassword = document.getElementById("new-password").value;
    const confirmPassword = document.getElementById("confirm-password").value;
    if (newPassword !== confirmPassword) {
      showPasswordMessage("Password confirmation does not match.", "error");
      return;
    }
    if (newPassword.length < 8) {
      showPasswordMessage("Password must contain at least 8 characters.", "error");
      return;
    }
    const body = new URLSearchParams({
      otp: document.getElementById("password-otp").value,
      newPassword,
      confirmPassword
    });
    body.append(csrfName, csrfToken);
    button.disabled = true;
    try {
      const response = await fetch("/customer/profile/password/update", {method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"}, body});
      const result = await response.json();
      showPasswordMessage(result.message, response.ok ? "success" : "error");
      if (response.ok) {
        updateStep.hidden = true;
        setTimeout(() => modal.close(), 900);
      }
    } catch (error) {
      showPasswordMessage("Could not contact the server. Please try again.", "error");
    } finally {
      button.disabled = false;
    }
  });
});
