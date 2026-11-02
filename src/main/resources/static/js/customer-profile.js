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

});
