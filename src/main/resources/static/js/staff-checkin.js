// Age Eligibility Verification (UC22): opens the age check modal as soon as a restricted ticket is scanned.
(() => {
  const dialog = document.getElementById("ageCheckDialog");
  if (!dialog) return;

  dialog.showModal();
  document.querySelectorAll("[data-open-age-check]").forEach((button) =>
    button.addEventListener("click", () => dialog.showModal()));
  dialog.querySelector("[data-close-age-check]").addEventListener("click", () => dialog.close());
})();
