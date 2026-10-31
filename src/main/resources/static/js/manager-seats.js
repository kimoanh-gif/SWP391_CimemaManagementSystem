// Screen 58: Seat Management JS

document.addEventListener('DOMContentLoaded', () => {
    const seatModal = document.getElementById('seatModal');

    window.addEventListener('click', (e) => {
        if (e.target === seatModal) {
            closeSeatModal();
        }
    });
});

function openSeatModal(element) {
    const seatId = element.getAttribute('data-id');
    const roomId = element.getAttribute('data-roomid');
    const row = element.getAttribute('data-row');
    const num = element.getAttribute('data-num');
    const type = element.getAttribute('data-type');
    const status = element.getAttribute('data-status');

    document.getElementById('modalSeatId').value = seatId;
    document.getElementById('modalRoomId').value = roomId;
    document.getElementById('modalSeatPos').textContent = row + num + ' (Row ' + row + ', Seat ' + num + ')';

    const errorBox = document.getElementById('seatModalError');
    if (errorBox) {
        errorBox.style.display = 'none';
        errorBox.textContent = '';
    }

    const typeSelect = document.getElementById('modalSeatTypeName');
    if (typeSelect) {
        typeSelect.value = type;
    }

    const statusSelect = document.getElementById('modalSeatStatus');
    if (statusSelect) {
        statusSelect.value = status;
    }

    const modal = document.getElementById('seatModal');
    if (modal) modal.classList.add('show');
}

function closeSeatModal() {
    const modal = document.getElementById('seatModal');
    if (modal) modal.classList.remove('show');
}

function validateSeatUpdateForm(event) {
    const seatId = parseInt(document.getElementById('modalSeatId')?.value || '0', 10);
    const roomId = parseInt(document.getElementById('modalRoomId')?.value || '0', 10);
    const seatTypeName = (document.getElementById('modalSeatTypeName')?.value || '').trim();
    const status = (document.getElementById('modalSeatStatus')?.value || '').trim();
    const errorBox = document.getElementById('seatModalError');

    const allowedStatuses = ['Active', 'Maintenance', 'Unavailable'];
    let errorMsg = '';

    if (!seatId || seatId <= 0 || !roomId || roomId <= 0) {
        errorMsg = 'Invalid seat or screening room identifier.';
    } else if (!seatTypeName) {
        errorMsg = 'Please select a valid seat type.';
    } else if (!allowedStatuses.includes(status)) {
        errorMsg = 'Please select a valid seat status (Active, Maintenance, Unavailable).';
    }

    if (errorMsg) {
        if (event) event.preventDefault();
        if (errorBox) {
            errorBox.textContent = errorMsg;
            errorBox.style.display = 'block';
        }
        return false;
    }
    return true;
}
