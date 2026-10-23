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

  document.getElementById("open-password-modal").addEventListener("click", () => modal.showModal());
  document.getElementById("request-password-code").addEventListener("click", async () => {
    message.textContent = "Sending verification...";
    const body = new URLSearchParams();
    body.append(csrfName, csrfToken);
    const response = await fetch("/customer/profile/password/request", {method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"}, body});
    const result = await response.json();
    message.textContent = result.message;
    if (!response.ok) return;
    otpField.hidden = result.firstGoogleSetup === true;
    requestStep.hidden = true;
    updateStep.hidden = false;
  });

  document.getElementById("update-password").addEventListener("click", async () => {
    const newPassword = document.getElementById("new-password").value;
    const confirmPassword = document.getElementById("confirm-password").value;
    if (newPassword !== confirmPassword) {
      message.textContent = "Password confirmation does not match.";
      return;
    }
    const body = new URLSearchParams({
      otp: document.getElementById("password-otp").value,
      newPassword,
      confirmPassword
    });
    body.append(csrfName, csrfToken);
    const response = await fetch("/customer/profile/password/update", {method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"}, body});
    const result = await response.json();
    message.textContent = result.message;
    if (response.ok) {
      updateStep.hidden = true;
      setTimeout(() => modal.close(), 900);
    }
  });
});
