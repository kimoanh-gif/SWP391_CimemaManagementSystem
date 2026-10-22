/* M07 News Management - modals, drawer and small form helpers. All data changes are normal form POSTs. */
(function () {
    'use strict';

    var openOverlay = null;
    var lastFocus = null;

    function open(id) {
        var overlay = document.getElementById(id);
        if (!overlay) return;
        if (openOverlay && openOverlay !== overlay) openOverlay.hidden = true;
        lastFocus = document.activeElement;
        overlay.hidden = false;
        openOverlay = overlay;
        var focusable = overlay.querySelector('textarea, input[type="radio"]:checked, button[type="submit"], button');
        if (focusable) focusable.focus();
    }

    function close() {
        if (!openOverlay) return;
        openOverlay.hidden = true;
        openOverlay = null;
        if (lastFocus && lastFocus.focus) lastFocus.focus();
    }

    function setField(root, name, value) {
        var el = root.querySelector('[data-field="' + name + '"]');
        if (el) el.textContent = value || '';
    }

    function openApprove(btn) {
        var form = document.getElementById('approve-form');
        form.action = btn.getAttribute('data-approve');
        setField(form, 'code', btn.getAttribute('data-code'));
        setField(form, 'title', btn.getAttribute('data-title'));
        setField(form, 'author', btn.getAttribute('data-author') || 'Unknown staff');
        form.querySelector('input[name="mode"][value="publish"]').checked = true;
        open('modal-approve');
    }

    var reasonBox = document.getElementById('reject-reason');
    var reasonCount = document.getElementById('reject-count');
    var reasonError = document.getElementById('reject-error');

    function updateCount() {
        if (!reasonBox || !reasonCount) return;
        var max = reasonBox.getAttribute('maxlength') || 300;
        reasonCount.textContent = reasonBox.value.length + '/' + max + ' characters';
        if (reasonBox.value.trim()) reasonError.hidden = true;
    }

    function openReject(btn) {
        var form = document.getElementById('reject-form');
        form.action = btn.getAttribute('data-reject');
        setField(form, 'code', btn.getAttribute('data-code'));
        reasonBox.value = '';
        reasonError.hidden = true;
        updateCount();
        open('modal-reject');
    }

    function openDetail(id) {
        var tpl = document.getElementById('news-detail-' + id);
        var body = document.getElementById('detail-body');
        if (!tpl || !body) return;
        body.innerHTML = '';
        body.appendChild(tpl.content.cloneNode(true));
        open('modal-detail');
    }

    document.addEventListener('click', function (e) {
        var target = e.target;
        var btn;
        if ((btn = target.closest('[data-approve]'))) { e.preventDefault(); openApprove(btn); return; }
        if ((btn = target.closest('[data-reject]'))) { e.preventDefault(); openReject(btn); return; }
        if ((btn = target.closest('[data-detail]'))) { e.preventDefault(); openDetail(btn.getAttribute('data-detail')); return; }
        if ((btn = target.closest('[data-open]'))) { e.preventDefault(); open(btn.getAttribute('data-open')); return; }
        if (target.closest('[data-close]')) { e.preventDefault(); close(); return; }
        if ((btn = target.closest('[data-reason]'))) {
            reasonBox.value = btn.getAttribute('data-reason');
            updateCount();
            reasonBox.focus();
            return;
        }
        if ((btn = target.closest('[data-confirm]'))) {
            if (!window.confirm(btn.getAttribute('data-confirm'))) e.preventDefault();
            return;
        }
        // Click on the dark backdrop closes the dialog.
        if (openOverlay && target === openOverlay) close();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') close();
    });

    if (reasonBox) reasonBox.addEventListener('input', updateCount);

    var rejectForm = document.getElementById('reject-form');
    if (rejectForm) {
        rejectForm.addEventListener('submit', function (e) {
            if (!reasonBox.value.trim()) {
                e.preventDefault();
                reasonError.hidden = false;
                reasonBox.focus();
            }
        });
    }

    // Page size selector submits the filter form right away.
    document.querySelectorAll('[data-autosubmit]').forEach(function (select) {
        select.addEventListener('change', function () {
            var form = select.form || document.getElementById(select.getAttribute('form'));
            if (form) form.submit();
        });
    });

    // Prevent double submits on every POST form.
    document.querySelectorAll('form[method="post"]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (e.defaultPrevented) return;
            if (form.dataset.submitting) { e.preventDefault(); return; }
            form.dataset.submitting = '1';
        });
    });
})();
