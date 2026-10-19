// Screen 57: Seat Type Management JS

document.addEventListener('DOMContentLoaded', () => {
    const seatTypeModal = document.getElementById('seatTypeModal');
    const multiplierModal = document.getElementById('multiplierModal');
    const btnCreateSeatType = document.getElementById('btnCreateSeatType');
    const seatTypeForm = document.getElementById('seatTypeForm');

    // Open Create Modal
    if (btnCreateSeatType) {
        btnCreateSeatType.addEventListener('click', () => {
            document.getElementById('modalTitle').textContent = 'Thêm Loại Ghế Mới';
            document.getElementById('formSeatTypeId').value = '';
            document.getElementById('typeCode').value = '';
            document.getElementById('typeCode').readOnly = false;
            document.getElementById('typeName').value = '';
            document.getElementById('priceMultiplier').value = '1.00';
            document.getElementById('colorCode').value = '#3B82F6';
            document.getElementById('status').value = 'Active';
            document.getElementById('description').value = '';
            seatTypeForm.action = '/manager/seat-types/save';
            seatTypeModal.classList.add('show');
        });
    }

    // Open Edit Seat Type Modal
    document.querySelectorAll('.btn-edit-seattype').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            const code = btn.getAttribute('data-code');
            const name = btn.getAttribute('data-name');
            const multiplier = btn.getAttribute('data-multiplier');
            const color = btn.getAttribute('data-color') || '#3B82F6';
            const desc = btn.getAttribute('data-description') || '';
            const status = btn.getAttribute('data-status') || 'Active';

            document.getElementById('modalTitle').textContent = 'Chỉnh Sửa Loại Ghế #' + id;
            document.getElementById('formSeatTypeId').value = id;
            document.getElementById('typeCode').value = code;
            document.getElementById('typeCode').readOnly = true;
            document.getElementById('typeName').value = name;
            document.getElementById('priceMultiplier').value = multiplier;
            document.getElementById('colorCode').value = color;
            document.getElementById('status').value = status;
            document.getElementById('description').value = desc;

            seatTypeForm.action = '/manager/seat-types/update/' + id;
            seatTypeModal.classList.add('show');
        });
    });

    // Open Quick Multiplier Modal
    document.querySelectorAll('.btn-edit-multiplier').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            const name = btn.getAttribute('data-name');
            const multiplier = btn.getAttribute('data-multiplier');

            document.getElementById('quickSeatTypeId').value = id;
            document.getElementById('quickSeatTypeName').textContent = name;
            document.getElementById('quickMultiplier').value = multiplier;

            multiplierModal.classList.add('show');
        });
    });

    // Close on clicking backdrop
    window.addEventListener('click', (e) => {
        if (e.target === seatTypeModal) {
            closeSeatTypeModal();
        }
        if (e.target === multiplierModal) {
            closeMultiplierModal();
        }
    });
});

function closeSeatTypeModal() {
    const modal = document.getElementById('seatTypeModal');
    if (modal) modal.classList.remove('show');
}

function closeMultiplierModal() {
    const modal = document.getElementById('multiplierModal');
    if (modal) modal.classList.remove('show');
}
