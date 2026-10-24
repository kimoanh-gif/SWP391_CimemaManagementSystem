// manager-rooms.js – handles modal open/close, form submit via fetch, deactivate action

document.addEventListener('DOMContentLoaded', () => {
    const addBtn = document.getElementById('add-room-btn');
    const modal = document.getElementById('room-modal');
    const closeBtn = modal.querySelector('.close-btn');
    const form = document.getElementById('room-form');
    const errorDiv = document.getElementById('form-error');
    const successDiv = document.getElementById('form-success');

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
            if (!confirm('Are you sure you want to put this room into maintenance?')) return;
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
                successDiv.textContent = data.message;
                setTimeout(() => location.reload(), 1200);
            } else {
                errorDiv.textContent = data.message || 'Something went wrong';
            }
        } catch (err) { console.error(err); errorDiv.textContent = 'Network error'; }
    });
});
