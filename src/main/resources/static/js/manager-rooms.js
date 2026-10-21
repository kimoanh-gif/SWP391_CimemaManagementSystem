// manager-rooms.js - handles modal open/close, form submit via fetch, deactivate action

document.addEventListener('DOMContentLoaded', () => {
    const addBtn = document.getElementById('add-room-btn');
    const modal = document.getElementById('room-modal');
    const closeBtn = modal ? modal.querySelector('.close-btn') : null;
    const form = document.getElementById('room-form');
    const errorDiv = document.getElementById('form-error');
    const successDiv = document.getElementById('form-success');

    const openModal = (room = null) => {
        if (!modal || !form) return;
        // Reset messages
        if (errorDiv) { errorDiv.textContent = ''; errorDiv.style.display = 'none'; }
        if (successDiv) { successDiv.textContent = ''; successDiv.style.display = 'none'; }
        // If room provided, fill form for edit
        if (room) {
            form.roomId.value = room.roomId;
            form.roomCode.value = room.roomCode;
            form.roomName.value = room.roomName;
            form.roomType.value = room.roomType;
            form.capacity.value = room.capacity;
            form.totalRows.value = room.totalRows || '';
            form.seatsPerRow.value = room.seatsPerRow || '';
            form.projector.value = room.projector || '';
            form.soundSystem.value = room.soundSystem || '';
            form.screenSize.value = room.screenSize || '';
            form.status.value = room.status;
            form.description.value = room.description || '';
        } else {
            form.reset();
            form.roomId.value = '';
        }
        modal.classList.add('open');
    };

    const closeModal = () => {
        if (modal) modal.classList.remove('open');
    };

    if (addBtn) addBtn.addEventListener('click', () => openModal());
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (modal) {
        modal.addEventListener('click', e => {
            if (e.target === modal) closeModal();
        });
    }

    // Edit buttons - delegated event
    document.querySelectorAll('.edit-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            try {
                const resp = await fetch(`/api/manager/rooms/${id}`);
                const data = await resp.json();
                if (data.success) {
                    openModal(data.data);
                } else {
                    alert('Failed to retrieve room details: ' + (data.message || 'Unknown error'));
                }
            } catch (err) { console.error(err); }
        });
    });

    // Deactivate buttons
    document.querySelectorAll('.deactivate-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            if (!confirm('Are you sure you want to put this screening room into maintenance?')) return;
            try {
                const resp = await fetch(`/api/manager/rooms/deactivate/${id}`, { method: 'PATCH' });
                const data = await resp.json();
                if (data.success) {
                    location.reload();
                } else {
                    alert(data.message || 'Operation failed');
                }
            } catch (err) { console.error(err); }
        });
    });

    // Form submit - create or update
    if (form) {
        form.addEventListener('submit', async e => {
            e.preventDefault();
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
                const data = await resp.json();
                if (data.success) {
                    if (successDiv) {
                        successDiv.textContent = data.message || 'Saved successfully!';
                        successDiv.style.display = 'block';
                    }
                    setTimeout(() => location.reload(), 1000);
                } else {
                    if (errorDiv) {
                        errorDiv.textContent = data.message || 'An error occurred while saving the room.';
                        errorDiv.style.display = 'block';
                    }
                }
            } catch (err) {
                console.error(err);
                if (errorDiv) {
                    errorDiv.textContent = 'Network communication error. Please try again.';
                    errorDiv.style.display = 'block';
                }
            }
        });
    }
});
