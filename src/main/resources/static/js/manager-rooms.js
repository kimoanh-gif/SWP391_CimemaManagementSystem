// manager-rooms.js – handles modal open/close, real-time seat calculation, validation and form submit

document.addEventListener('DOMContentLoaded', () => {
    const addBtn = document.getElementById('add-room-btn');
    const modal = document.getElementById('room-modal');
    const closeBtns = modal.querySelectorAll('.close-btn');
    const form = document.getElementById('room-form');
    const errorDiv = document.getElementById('form-error');
    const successDiv = document.getElementById('form-success');
    const modalTitle = document.getElementById('modal-title');
    const saveBtn = document.getElementById('btn-save-room');

    const totalRowsInput = form.totalRows;
    const seatsPerRowInput = form.seatsPerRow;
    const capacityInput = form.capacity;
    const noticeBox = document.getElementById('calc-sync-notice');
    const noticeIcon = document.getElementById('calc-sync-icon');
    const noticeText = document.getElementById('calc-sync-text');
    const applyBtn = document.getElementById('btn-apply-calc');

    const showFormError = (msg) => {
        if (!errorDiv) return;
        errorDiv.textContent = msg;
        errorDiv.style.display = 'block';
        if (successDiv) successDiv.style.display = 'none';
    };

    const showFormSuccess = (msg) => {
        if (!successDiv) return;
        successDiv.textContent = msg;
        successDiv.style.display = 'block';
        if (errorDiv) errorDiv.style.display = 'none';
    };

    const clearMessages = () => {
        if (errorDiv) { errorDiv.textContent = ''; errorDiv.style.display = 'none'; }
        if (successDiv) { successDiv.textContent = ''; successDiv.style.display = 'none'; }
    };

    // Real-time calculation and validation between Rows x SeatsPerRow and Capacity
    const updateSeatCalculation = (autoFillIfEmpty = false) => {
        const r = totalRowsInput && totalRowsInput.value ? parseInt(totalRowsInput.value, 10) : 0;
        const s = seatsPerRowInput && seatsPerRowInput.value ? parseInt(seatsPerRowInput.value, 10) : 0;
        const c = capacityInput && capacityInput.value ? parseInt(capacityInput.value, 10) : 0;

        if (!noticeBox) return;

        if (r > 0 && s > 0) {
            const expected = r * s;
            noticeBox.style.display = 'flex';

            if ((!c || c === 0) && autoFillIfEmpty) {
                capacityInput.value = expected;
                capacityInput.classList.remove('is-invalid');
                noticeBox.className = 'calc-status-box is-match';
                if (noticeIcon) noticeIcon.textContent = 'check_circle';
                if (noticeText) noticeText.textContent = `Auto-calculated: ${r} rows × ${s} seats = ${expected} seats (matches total capacity)`;
                if (applyBtn) applyBtn.style.display = 'none';
            } else if (c === expected) {
                capacityInput.classList.remove('is-invalid');
                noticeBox.className = 'calc-status-box is-match';
                if (noticeIcon) noticeIcon.textContent = 'check_circle';
                if (noticeText) noticeText.textContent = `Exact: ${r} rows × ${s} seats = ${expected} seats (matches total capacity)`;
                if (applyBtn) applyBtn.style.display = 'none';
            } else {
                capacityInput.classList.add('is-invalid');
                noticeBox.className = 'calc-status-box is-mismatch';
                if (noticeIcon) noticeIcon.textContent = 'warning';
                if (noticeText) noticeText.textContent = `Mismatch: ${r} rows × ${s} seats = ${expected} seats (current input: ${c || 0} seats)`;
                if (applyBtn) {
                    applyBtn.textContent = `Apply ${expected} seats`;
                    applyBtn.style.display = 'inline-block';
                    applyBtn.onclick = () => {
                        capacityInput.value = expected;
                        updateSeatCalculation(false);
                    };
                }
            }
        } else if ((r > 0 && s <= 0) || (r <= 0 && s > 0)) {
            noticeBox.style.display = 'flex';
            noticeBox.className = 'calc-status-box';
            if (noticeIcon) noticeIcon.textContent = 'info';
            if (noticeText) noticeText.textContent = 'Please enter both rows and seats per row to verify capacity.';
            if (applyBtn) applyBtn.style.display = 'none';
            if (capacityInput) capacityInput.classList.remove('is-invalid');
        } else {
            noticeBox.style.display = 'none';
            if (capacityInput) capacityInput.classList.remove('is-invalid');
        }
    };

    if (totalRowsInput) totalRowsInput.addEventListener('input', () => updateSeatCalculation(true));
    if (seatsPerRowInput) seatsPerRowInput.addEventListener('input', () => updateSeatCalculation(true));
    if (capacityInput) capacityInput.addEventListener('input', () => updateSeatCalculation(false));

    const hintText = document.getElementById('capacityCalcText');

    const updateCapacityCalculation = (autoCompute = false) => {
        if (!hintText) return;
        const rows = parseInt(form.totalRows.value, 10);
        const cols = parseInt(form.seatsPerRow.value, 10);
        const cap = parseInt(form.capacity.value, 10);

        if (rows > 0 && cols > 0) {
            const expected = rows * cols;
            if (autoCompute || !form.capacity.value) {
                form.capacity.value = expected;
                hintText.innerHTML = `<span style="color:#16a34a;font-weight:600;">✓ ${rows} vertical rows × ${cols} seats per row = ${expected} total seats</span>`;
            } else if (cap === expected) {
                hintText.innerHTML = `<span style="color:#16a34a;font-weight:600;">✓ Valid: ${rows} × ${cols} = ${expected} total seats</span>`;
            } else {
                hintText.innerHTML = `<span style="color:#dc2626;font-weight:600;">⚠ Mismatch: ${rows} vertical rows × ${cols} seats = ${expected} seats (current: ${cap || 0})</span>`;
            }
        } else {
            hintText.innerHTML = `Rule: <strong>Total room capacity = Vertical rows × Horizontal seats</strong>`;
        }
    };

    if (form.totalRows) form.totalRows.addEventListener('input', () => updateCapacityCalculation(true));
    if (form.seatsPerRow) form.seatsPerRow.addEventListener('input', () => updateCapacityCalculation(true));
    if (form.capacity) form.capacity.addEventListener('input', () => updateCapacityCalculation(false));

    const openModal = (room = null) => {
        clearMessages();
        if (capacityInput) capacityInput.classList.remove('is-invalid');

        if (room) {
            if (modalTitle) modalTitle.textContent = `Screening room configuration — ${room.roomName || room.roomCode}`;
            form.roomId.value = room.roomId;
            form.roomCode.value = room.roomCode || '';
            form.roomName.value = room.roomName || '';
            form.roomType.value = room.roomType || 'Standard Laser';
            form.capacity.value = room.capacity || '';
            form.totalRows.value = room.totalRows || '';
            form.seatsPerRow.value = room.seatsPerRow || '';
            form.capacity.value = room.capacity || '';
            form.projector.value = room.projector || '';
            form.soundSystem.value = room.soundSystem || '';
            form.screenSize.value = room.screenSize || '';
            form.status.value = room.status || 'Ready';
            form.description.value = room.description || '';
            updateCapacityCalculation(false);
        } else {
            if (modalTitle) modalTitle.textContent = 'Screening room configuration';
            form.reset();
            form.roomId.value = '';
            form.status.value = 'Ready';
            form.roomType.value = 'Standard Laser';
        }

        updateSeatCalculation(false);
        modal.classList.add('open');
    };

    const closeModal = () => {
        modal.classList.remove('open');
        clearMessages();
    };

    if (addBtn) addBtn.addEventListener('click', () => openModal());
    closeBtns.forEach(btn => btn.addEventListener('click', closeModal));
    modal.addEventListener('click', e => {
        if (e.target === modal) closeModal();
    });

    // Edit buttons – delegated event
    document.querySelectorAll('.edit-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            try {
                const resp = await fetch(`/api/manager/rooms/${id}`);
                const data = await resp.json();
                if (data.success) {
                    openModal(data.data);
                } else {
                    alert('Failed to load room data: ' + (data.message || ''));
                }
            } catch (err) {
                console.error(err);
                alert('Cannot load screening room details.');
            }
        });
    });

    // Deactivate buttons
    document.querySelectorAll('.deactivate-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            if (!confirm('Are you sure you want to set this screening room to Maintenance?')) return;
            try {
                const resp = await fetch(`/api/manager/rooms/deactivate/${id}`, { method: 'PATCH' });
                const data = await resp.json();
                if (data.success) {
                    location.reload();
                } else {
                    alert(data.message || 'Error setting room to maintenance');
                }
            } catch (err) {
                console.error(err);
                alert('Server connection error.');
            }
        });
    });

    // Form submit – create or update with strict validation
    form.addEventListener('submit', async e => {
        e.preventDefault();
        clearMessages();

        const r = totalRowsInput && totalRowsInput.value ? parseInt(totalRowsInput.value, 10) : 0;
        const s = seatsPerRowInput && seatsPerRowInput.value ? parseInt(seatsPerRowInput.value, 10) : 0;
        const c = capacityInput && capacityInput.value ? parseInt(capacityInput.value, 10) : 0;

        // Validation rule requested: code, name, rows, seatsPerRow and capacity > 0
        const codeVal = (form.roomCode?.value || '').trim(); const nameVal = (form.roomName?.value || '').trim(); if (!codeVal || codeVal.length > 20 || !/^[A-Za-z0-9_-]+$/.test(codeVal)) { showFormError('Invalid room code (max 20 characters, alphanumeric with '-' and '_').'); form.roomCode?.focus(); return; } if (!nameVal || nameVal.length > 100) { showFormError('Room name cannot be empty and max 100 characters.'); form.roomName?.focus(); return; } if (r <= 0 || s <= 0) { showFormError('Please enter both rows and seats per row greater than 0.'); if (r <= 0) totalRowsInput.focus(); else seatsPerRowInput.focus(); return; }
        if (!c || c <= 0) {
            showFormError('Please enter a valid total capacity (greater than 0).');
            capacityInput.focus();
            return;
        }

        // Validation rule requested: totalRows * seatsPerRow must equal capacity
        if (r > 0 || s > 0) {
            if (r <= 0 || s <= 0) {
                showFormError('Please enter both rows and seats per row to verify capacity.');
                if (r <= 0) totalRowsInput.focus(); else seatsPerRowInput.focus();
                return;
            }
            const expected = r * s;
            if (c !== expected) {
                showFormError(`Total capacity (${c} seats) must equal rows (${r}) × seats per row (${s} = ${expected} seats). Please adjust accordingly.`);
                capacityInput.classList.add('is-invalid');
                capacityInput.focus();
                return;
            }
        }

        const formData = new FormData(form);
        const json = Object.fromEntries(formData.entries());

        // Convert numeric fields
        if (json.capacity) json.capacity = parseInt(json.capacity, 10);
        if (json.totalRows) json.totalRows = parseInt(json.totalRows, 10); else json.totalRows = null;
        if (json.seatsPerRow) json.seatsPerRow = parseInt(json.seatsPerRow, 10); else json.seatsPerRow = null;

        const method = json.roomId ? 'PUT' : 'POST';
        const url = json.roomId ? `/api/manager/rooms/${json.roomId}` : '/api/manager/rooms';

        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.textContent = 'Saving...';
        }

        try {
            const resp = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(json)
            });
            const data = await resp.json();
            if (data.success) {
                showFormSuccess(data.message || 'Screening room saved successfully!');
                setTimeout(() => location.reload(), 900);
            } else {
                showFormError(data.message || 'Cannot save screening room. Please check again.');
            }
        } catch (err) {
            console.error(err);
            showFormError('Server connection error. Please try again.');
        } finally {
            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Save room';
            }
        }
    });
});
