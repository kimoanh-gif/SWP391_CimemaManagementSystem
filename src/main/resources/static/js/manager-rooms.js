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
                if (noticeText) noticeText.textContent = `Tự động tính: ${r} hàng × ${s} ghế = ${expected} ghế (khớp với tổng sức chứa)`;
                if (applyBtn) applyBtn.style.display = 'none';
            } else if (c === expected) {
                capacityInput.classList.remove('is-invalid');
                noticeBox.className = 'calc-status-box is-match';
                if (noticeIcon) noticeIcon.textContent = 'check_circle';
                if (noticeText) noticeText.textContent = `Chính xác: ${r} hàng × ${s} ghế = ${expected} ghế (khớp hoàn toàn với tổng sức chứa)`;
                if (applyBtn) applyBtn.style.display = 'none';
            } else {
                capacityInput.classList.add('is-invalid');
                noticeBox.className = 'calc-status-box is-mismatch';
                if (noticeIcon) noticeIcon.textContent = 'warning';
                if (noticeText) noticeText.textContent = `Chưa khớp: ${r} hàng × ${s} ghế = ${expected} ghế (khác với sức chứa đang nhập: ${c || 0} ghế)`;
                if (applyBtn) {
                    applyBtn.textContent = `Áp dụng ${expected} ghế`;
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
            if (noticeText) noticeText.textContent = 'Vui lòng nhập cả số hàng và số ghế mỗi hàng để đối chiếu sức chứa.';
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
            form.projector.value = room.projector || '';
            form.soundSystem.value = room.soundSystem || '';
            form.screenSize.value = room.screenSize || '';
            form.status.value = room.status || 'Ready';
            form.description.value = room.description || '';
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
                alert('Không thể tải thông tin phòng chiếu.');
            }
        });
    });

    // Deactivate buttons
    document.querySelectorAll('.deactivate-room-btn').forEach(btn => {
        btn.addEventListener('click', async e => {
            const id = btn.dataset.id;
            if (!confirm('Bạn có chắc chắn muốn đưa phòng chiếu này vào trạng thái Bảo trì?')) return;
            try {
                const resp = await fetch(`/api/manager/rooms/deactivate/${id}`, { method: 'PATCH' });
                const data = await resp.json();
                if (data.success) {
                    location.reload();
                } else {
                    alert(data.message || 'Lỗi khi bảo trì phòng');
                }
            } catch (err) {
                console.error(err);
                alert('Lỗi kết nối máy chủ.');
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

        // Validation rule requested: capacity > 0
        if (!c || c <= 0) {
            showFormError('Vui lòng nhập tổng sức chứa hợp lệ (lớn hơn 0).');
            capacityInput.focus();
            return;
        }

        // Validation rule requested: totalRows * seatsPerRow must equal capacity
        if (r > 0 || s > 0) {
            if (r <= 0 || s <= 0) {
                showFormError('Vui lòng nhập đầy đủ cả Số hàng và Số ghế mỗi hàng để đối chiếu sức chứa.');
                if (r <= 0) totalRowsInput.focus(); else seatsPerRowInput.focus();
                return;
            }
            const expected = r * s;
            if (c !== expected) {
                showFormError(`Tổng sức chứa (${c} ghế) phải bằng số hàng (${r}) × số ghế mỗi hàng (${s} = ${expected} ghế). Vui lòng điều chỉnh lại.`);
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
            saveBtn.textContent = 'Đang lưu...';
        }

        try {
            const resp = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(json)
            });
            const data = await resp.json();
            if (data.success) {
                showFormSuccess(data.message || 'Lưu phòng chiếu thành công!');
                setTimeout(() => location.reload(), 900);
            } else {
                showFormError(data.message || 'Không thể lưu phòng chiếu. Vui lòng kiểm tra lại.');
            }
        } catch (err) {
            console.error(err);
            showFormError('Lỗi kết nối máy chủ. Vui lòng thử lại.');
        } finally {
            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Save room';
            }
        }
    });
});
