/* M11 Promotion & Voucher Management – small UI helpers. Assignee: Nguyễn Đức Anh */
(function () {
    'use strict';

    // "Add new" dropdown
    document.querySelectorAll('[data-toggle-menu]').forEach(function (btn) {
        var menu = document.getElementById(btn.getAttribute('data-toggle-menu'));
        if (!menu) return;
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            menu.hidden = !menu.hidden;
            btn.setAttribute('aria-expanded', String(!menu.hidden));
        });
        document.addEventListener('click', function (e) {
            if (!menu.hidden && !menu.contains(e.target)) {
                menu.hidden = true;
                btn.setAttribute('aria-expanded', 'false');
            }
        });
    });

    // Rows-per-page select submits its form
    document.querySelectorAll('select[data-autosubmit]').forEach(function (sel) {
        sel.addEventListener('change', function () { sel.form.submit(); });
    });

    // Voucher code: upper-case, no spaces
    document.querySelectorAll('input[data-uppercase]').forEach(function (input) {
        input.addEventListener('input', function () {
            var pos = input.selectionStart;
            input.value = input.value.toUpperCase().replace(/\s+/g, '');
            try { input.setSelectionRange(pos, pos); } catch (ignored) { /* not supported */ }
        });
    });

    // Discount type: unit label + max-discount field only for percentage
    var typeSelect = document.querySelector('[data-discount-type]');
    if (typeSelect) {
        var unit = document.querySelector('[data-unit]');
        var percentOnly = document.querySelector('[data-percent-only]');
        var valueInput = document.getElementById('discountValue');
        var sync = function () {
            var percent = typeSelect.value === 'PERCENT';
            if (unit) unit.textContent = percent ? '%' : 'VND';
            if (percentOnly) percentOnly.hidden = !percent;
            if (valueInput) {
                if (percent) valueInput.max = '100'; else valueInput.removeAttribute('max');
            }
        };
        typeSelect.addEventListener('change', sync);
        sync();
    }

    // Copy voucher code
    document.querySelectorAll('[data-copy]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var text = btn.getAttribute('data-copy');
            if (navigator.clipboard) {
                navigator.clipboard.writeText(text).then(function () {
                    var old = btn.lastChild.textContent;
                    btn.lastChild.textContent = 'Copied';
                    setTimeout(function () { btn.lastChild.textContent = old; }, 1500);
                }, function () { /* ignore */ });
            }
        });
    });

    // Delete dialog: Escape closes it (goes back to the plain detail URL)
    var dialog = document.getElementById('delete-dialog');
    if (dialog && !dialog.hidden) {
        var close = dialog.querySelector('.mp-close');
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && close) window.location.href = close.href;
        });
        var first = dialog.querySelector('.mp-modal-foot a, .mp-modal-foot button');
        if (first) first.focus();
    }
})();
