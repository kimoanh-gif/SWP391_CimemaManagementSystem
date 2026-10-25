/* M07 News Management – dialogs, quick reasons, character counter. Assignee: Nguyễn Đức Anh */
(function () {
  "use strict";

  // Keep current filters after an action (the controller strips "view").
  var query = window.location.search.replace(/^\?/, "");
  document.querySelectorAll(".mn-return-query").forEach(function (input) {
    input.value = query;
  });

  function open(id, trigger) {
    var dialog = document.getElementById(id);
    if (!dialog) return;
    var form = dialog.querySelector("form[data-action-template]");
    if (form && trigger && trigger.dataset.id) {
      form.action = form.dataset.actionTemplate.replace("__ID__", trigger.dataset.id);
    }
    dialog.querySelectorAll("[data-fill]").forEach(function (el) {
      var key = el.dataset.fill;
      if (trigger && trigger.dataset[key] !== undefined) el.textContent = trigger.dataset[key];
    });
    var detail = document.getElementById("detail-modal");
    if (detail && id !== "detail-modal") detail.classList.add("is-behind");
    dialog.hidden = false;
    dialog.classList.add("is-open");
    var focusable = dialog.querySelector("textarea, input[type=radio]:checked, button[type=submit]");
    if (focusable) focusable.focus();
  }

  function close(dialog) {
    dialog.classList.remove("is-open");
    dialog.hidden = true;
    var detail = document.getElementById("detail-modal");
    if (detail) detail.classList.remove("is-behind");
  }

  document.addEventListener("click", function (event) {
    var opener = event.target.closest("[data-open]");
    if (opener) {
      open(opener.dataset.open, opener);
      return;
    }
    var closer = event.target.closest("[data-close]");
    if (closer) {
      close(closer.closest(".mn-backdrop"));
      return;
    }
    if (event.target.classList.contains("mn-backdrop") && !event.target.hidden && event.target.id !== "detail-modal") {
      close(event.target);
    }
  });

  document.addEventListener("keydown", function (event) {
    if (event.key !== "Escape") return;
    document.querySelectorAll(".mn-backdrop.is-open:not([hidden])").forEach(close);
  });

  // Confirm before archive.
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (event) {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
  });

  // Approve: schedule time is required only for "Approve and schedule".
  var approveForm = document.getElementById("approve-form");
  if (approveForm) {
    var when = approveForm.querySelector("input[name=publishAt]");
    approveForm.addEventListener("change", function () {
      var schedule = approveForm.querySelector("input[name=publishMode]:checked").value === "SCHEDULE";
      when.required = schedule;
      if (schedule && !when.value) {
        var d = new Date(Date.now() + 60 * 60 * 1000);
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        when.value = d.toISOString().slice(0, 16);
      }
    });
  }

  // Reject: quick reasons + counter + required check.
  var rejectForm = document.getElementById("reject-form");
  if (rejectForm) {
    var reason = rejectForm.querySelector("textarea[name=reason]");
    var counter = document.getElementById("reason-count");
    var error = rejectForm.querySelector(".mn-reason-error");
    var update = function () {
      counter.textContent = reason.value.length;
      if (reason.value.trim()) error.hidden = true;
    };
    reason.addEventListener("input", update);
    rejectForm.querySelectorAll("[data-reason]").forEach(function (chip) {
      chip.addEventListener("click", function () {
        var text = chip.dataset.reason;
        reason.value = reason.value.trim() ? reason.value.trim() + "; " + text : text;
        reason.value = reason.value.slice(0, Number(reason.getAttribute("maxlength")) || 300);
        update();
        reason.focus();
      });
    });
    rejectForm.addEventListener("submit", function (event) {
      if (!reason.value.trim()) {
        event.preventDefault();
        error.hidden = false;
        reason.focus();
      }
    });
  }
})();
