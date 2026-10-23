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
