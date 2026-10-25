// manager-rooms.js – handles modal open/close, form submit via fetch, deactivate action

document.addEventListener('DOMContentLoaded', () => {
    const addBtn = document.getElementById('add-room-btn');
    const modal = document.getElementById('room-modal');
    const closeBtn = modal.querySelector('.close-btn');
    const form = document.getElementById('room-form');
    const errorDiv = document.getElementById('form-error');
    const successDiv = document.getElementById('form-success');

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
        // Reset messages
        errorDiv.textContent = '';
        successDiv.textContent = '';
        // If room provided, fill form for edit
        if (room) {
            form.roomId.value = room.roomId;
            form.roomCode.value = room.roomCode;
            form.roomName.value = room.roomName;
            form.roomType.value = room.roomType;
            form.totalRows.value = room.totalRows || '';
            form.seatsPerRow.value = room.seatsPerRow || '';
            form.capacity.value = room.capacity || '';
            form.projector.value = room.projector || '';
            form.soundSystem.value = room.soundSystem || '';
            form.screenSize.value = room.screenSize || '';
            form.status.value = room.status;
            form.description.value = room.description || '';
            updateCapacityCalculation(false);
        } else {
            form.reset();
            form.roomId.value = '';
            updateCapacityCalculation(false);
        }
        modal.classList.add('open');
    };

    const closeModal = () => {
        modal.classList.remove('open');
    };

    addBtn.addEventListener('click', () => openModal());
    closeBtn.addEventListener('click', closeModal);
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
                    alert('Failed to load room data');
                }
            } catch (err) { console.error(err); }
        });
    });

    // Deactivate buttons
    document.querySelectorAll('.deactivate-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            if (!confirm('Are you sure you want to set this screening room to Maintenance status?')) return;
            try {
                const resp = await fetch(`/api/manager/rooms/deactivate/${id}`, { method: 'PATCH' });
                const data = await resp.json();
                if (data.success) {
                    location.reload();
                } else {
                    alert(data.message);
                }
            } catch (err) { console.error(err); }
        });
    });

    // Form submit – create or update
    form.addEventListener('submit', async e => {
        e.preventDefault();
        errorDiv.textContent = '';
        successDiv.textContent = '';

        const totalRows = parseInt(form.totalRows.value, 10);
        const seatsPerRow = parseInt(form.seatsPerRow.value, 10);
        const capacity = parseInt(form.capacity.value, 10);

        if (!totalRows || totalRows <= 0) {
            errorDiv.textContent = 'Please enter total rows (vertical) greater than 0.';
            form.totalRows.focus();
            return;
        }
        if (!seatsPerRow || seatsPerRow <= 0) {
            errorDiv.textContent = 'Please enter seats per row (horizontal) greater than 0.';
            form.seatsPerRow.focus();
            return;
        }
        if (!capacity || capacity <= 0) {
            errorDiv.textContent = 'Please enter total room capacity greater than 0.';
            form.capacity.focus();
            return;
        }
        if (totalRows * seatsPerRow !== capacity) {
            const expected = totalRows * seatsPerRow;
            errorDiv.textContent = `Total room capacity (${capacity}) must equal total rows multiplied by seats per row (${totalRows} rows × ${seatsPerRow} seats = ${expected} seats).`;
            form.capacity.focus();
            return;
        }

        const formData = new FormData(form);
        const json = Object.fromEntries(formData.entries());
        const method = json.roomId ? 'PUT' : 'POST';
        const url = json.roomId ? `/api/manager/rooms/${json.roomId}` : '/api/manager/rooms';
        try {
            const resp = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(json)
            });
            let data;
            try {
                data = await resp.json();
            } catch (jsonErr) {
                data = { success: false, message: 'Server error (' + resp.status + ')' };
            }
            if (resp.ok && data.success) {
                successDiv.textContent = data.message || 'Room saved successfully!';
                setTimeout(() => location.reload(), 1200);
            } else {
                errorDiv.textContent = data.message || 'An error occurred while saving the room';
            }
        } catch (err) {
            console.error(err);
            errorDiv.textContent = 'Error connecting to server';
        }
    });
});
