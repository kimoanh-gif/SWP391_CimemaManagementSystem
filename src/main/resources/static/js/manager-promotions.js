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
                var policy = document.querySelector('[data-voucher-policy]');
                valueInput.max = percent ? (policy ? policy.dataset.maxPercent : '100') : '1000000';
            }
        };
        typeSelect.addEventListener('change', sync);
        sync();
    }

    var policyBox = document.querySelector('[data-voucher-policy]');
    var voucherForm = document.querySelector('form.mp-form-card');
    if (policyBox && voucherForm && typeSelect) {
        var rules = policyBox.dataset;
        var fields = [];
        function limit(id, min, max, required, hint) {
            var input = document.getElementById(id);
            if (!input) return;
            input.min = String(min);
            if (max !== null) input.max = String(max);
            input.required = required;
            if (id !== 'discountValue') input.step = '1';
            fields.push(input);
            var note = document.createElement('small');
            note.className = 'mp-hint-text';
            note.textContent = hint;
            input.closest('.mp-field, .mp-limit').appendChild(note);
        }
        limit('discountValue', 1, null, true, 'Admin maximum percentage: ' + rules.maxPercent + '%. Fixed discounts: up to 1,000,000 VND.');
        limit('minBookingAmount', rules.minOrder, null, Number(rules.minOrder) > 0, 'Admin minimum order: ' + rules.minOrder + ' VND or higher.');
        limit('totalUsageLimit', 1, rules.total, true, 'Admin maximum: ' + rules.total + ' uses per voucher.');
        limit('perAccountLimit', 1, rules.perAccount, true, 'Admin maximum: ' + rules.perAccount + ' uses per customer per voucher.');
        var cap = document.getElementById('maxDiscountAmount');
        function updateCap() {
            var capped = typeSelect.value === 'PERCENT' && Number(rules.maxCap) > 0;
            cap.disabled = typeSelect.value !== 'PERCENT';
            cap.required = capped;
            cap.min = capped ? '1' : '0';
            cap.step = '1';
            if (capped) cap.max = rules.maxCap;
            else cap.removeAttribute('max');
        }
        typeSelect.addEventListener('change', updateCap);
        updateCap();
        fields.push(cap);
        var start = document.getElementById('validFrom');
        var end = document.getElementById('validTo');
        var note = document.createElement('small');
        note.textContent = 'Admin maximum validity: ' + rules.days + ' days from start to end.';
        end.parentElement.appendChild(note);
        fields.push(start, end);
        voucherForm.addEventListener('submit', function (event) {
            end.setCustomValidity('');
            var from = Date.parse(start.value + 'Z'), to = Date.parse(end.value + 'Z');
            if (Number.isFinite(from) && Number.isFinite(to) && (to < from || to - from > Number(rules.days) * 86400000))
                end.setCustomValidity('End must follow start and be within ' + rules.days + ' days (Admin policy).');
            for (var input of fields) {
                if (!input.reportValidity()) { event.preventDefault(); break; }
            }
        });
        end.addEventListener('input', function () { end.setCustomValidity(''); });
        start.addEventListener('input', function () { end.setCustomValidity(''); });
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
