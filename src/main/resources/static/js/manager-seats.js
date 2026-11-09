// Screen 58: Seat Management JS with Multi-Seat Drag, Sweep & Box Selection

const selectedSeatElements = new Set();
let isMouseDown = false;
let dragMoved = false;
let startX = 0;
let startY = 0;
let marquee = null;
let currentMode = true; // true = selecting, false = unselecting

document.addEventListener('DOMContentLoaded', () => {
    initSeatInteractions();
    initRowSelection();
    initKeyboardShortcuts();

    const seatModal = document.getElementById('seatModal');
    window.addEventListener('click', (e) => {
        if (e.target === seatModal) {
            closeSeatModal();
        }
    });
});

/**
 * Initialize all seat interactions: Click to toggle, Drag/Sweep across rows/cols, Marquee box.
 */
function initSeatInteractions() {
    const container = document.querySelector('.seat-plan-container');
    const chart = document.querySelector('.cinema-seating-chart');
    if (!container || !chart) return;

    // Prevent native text selection or ghost drag during selection
    container.style.userSelect = 'none';
    container.style.webkitUserSelect = 'none';
    chart.style.userSelect = 'none';
    chart.style.webkitUserSelect = 'none';

    // 1. MouseDown on container / seats
    container.addEventListener('mousedown', (e) => {
        // Only primary mouse button, ignore clicks on buttons, links, inputs, modals
        if (e.button !== 0) return;
        if (e.target.closest('button') || e.target.closest('a') || e.target.closest('.modal-content') || e.target.closest('.selection-action-bar')) {
            return;
        }
        if (e.target.closest('.row-indicator')) {
            return;
        }

        e.preventDefault(); // Stop native drag/text selection

        isMouseDown = true;
        dragMoved = false;
        startX = e.pageX;
        startY = e.pageY;

        const clickedSeat = e.target.closest('.seat-item');
        if (clickedSeat) {
            // Determine whether we are selecting or unselecting based on clicked seat
            currentMode = !selectedSeatElements.has(clickedSeat);
            if (currentMode) {
                selectSeatElement(clickedSeat);
            } else {
                unselectSeatElement(clickedSeat);
            }
            updateSelectionBar();
        } else {
            // Clicked empty background: if no Ctrl/Shift, start fresh
            if (!e.ctrlKey && !e.metaKey && !e.shiftKey) {
                clearSeatSelection();
            }
            currentMode = true;
        }
    });

    // 2. MouseEnter on seats while mouse is held down (Sweep / Brush selection across rows/columns)
    const allSeats = chart.querySelectorAll('.seat-item');
    allSeats.forEach(seat => {
        seat.addEventListener('mouseenter', () => {
            if (isMouseDown) {
                dragMoved = true;
                if (currentMode) {
                    selectSeatElement(seat);
                } else {
                    unselectSeatElement(seat);
                }
                updateSelectionBar();
            }
        });

        // Double click to configure directly
        seat.addEventListener('dblclick', (e) => {
            e.preventDefault();
            e.stopPropagation();
            clearSeatSelection();
            selectSeatElement(seat);
            openSingleSeatModal(seat);
        });
    });

    // 3. MouseMove on window for Box Marquee selection
    window.addEventListener('mousemove', (e) => {
        if (!isMouseDown) return;

        const dx = e.pageX - startX;
        const dy = e.pageY - startY;
        const dist = Math.hypot(dx, dy);

        if (dist > 8) {
            dragMoved = true;

            if (!marquee) {
                marquee = document.createElement('div');
                marquee.className = 'selection-marquee';
                document.body.appendChild(marquee);
            }

            const left = Math.min(startX, e.pageX);
            const top = Math.min(startY, e.pageY);
            const width = Math.abs(dx);
            const height = Math.abs(dy);

            marquee.style.left = left + 'px';
            marquee.style.top = top + 'px';
            marquee.style.width = width + 'px';
            marquee.style.height = height + 'px';

            const marqueeRect = {
                left: left,
                top: top,
                right: left + width,
                bottom: top + height
            };

            allSeats.forEach(seat => {
                const rect = seat.getBoundingClientRect();
                const seatRect = {
                    left: rect.left + window.scrollX,
                    top: rect.top + window.scrollY,
                    right: rect.right + window.scrollX,
                    bottom: rect.bottom + window.scrollY
                };

                const intersects = !(
                    seatRect.right < marqueeRect.left ||
                    seatRect.left > marqueeRect.right ||
                    seatRect.bottom < marqueeRect.top ||
                    seatRect.top > marqueeRect.bottom
                );

                if (intersects) {
                    selectSeatElement(seat);
                }
            });

            updateSelectionBar();
        }
    });

    // 4. MouseUp on window to finish drag
    window.addEventListener('mouseup', () => {
        if (!isMouseDown) return;
        isMouseDown = false;

        if (marquee) {
            marquee.remove();
            marquee = null;
        }
    });
}

/**
 * Click row letters (A, B, C...) to toggle selection of entire row.
 */
function initRowSelection() {
    const rowIndicators = document.querySelectorAll('.cinema-seat-row .row-indicator');
    rowIndicators.forEach(indicator => {
        indicator.title = 'Click to select / deselect entire row';
        indicator.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();

            const rowElem = indicator.closest('.cinema-seat-row');
            if (!rowElem) return;

            const seatsInRow = Array.from(rowElem.querySelectorAll('.seat-item'));
            if (seatsInRow.length === 0) return;

            const allSelected = seatsInRow.every(s => selectedSeatElements.has(s));
            seatsInRow.forEach(s => {
                if (allSelected) {
                    unselectSeatElement(s);
                } else {
                    selectSeatElement(s);
                }
            });

            updateSelectionBar();
        });
    });
}

function initKeyboardShortcuts() {
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const modal = document.getElementById('seatModal');
            if (modal && modal.classList.contains('show')) {
                closeSeatModal();
            } else {
                clearSeatSelection();
            }
        } else if (e.key === 'Enter') {
            const modal = document.getElementById('seatModal');
            if (selectedSeatElements.size > 0 && (!modal || !modal.classList.contains('show'))) {
                openMultiSeatModal();
            }
        }
    });
}

function selectSeatElement(seat) {
    if (!seat) return;
    selectedSeatElements.add(seat);
    seat.classList.add('is-selected');
}

function unselectSeatElement(seat) {
    if (!seat) return;
    selectedSeatElements.delete(seat);
    seat.classList.remove('is-selected');
}

function toggleSeatSelection(seat) {
    if (selectedSeatElements.has(seat)) {
        unselectSeatElement(seat);
    } else {
        selectSeatElement(seat);
    }
    updateSelectionBar();
}

function clearSeatSelection() {
    selectedSeatElements.forEach(s => s.classList.remove('is-selected'));
    selectedSeatElements.clear();
    updateSelectionBar();
}

function updateSelectionBar() {
    const bar = document.getElementById('selectionActionBar');
    const countEl = document.getElementById('selectedSeatsCount');
    const listEl = document.getElementById('selectedSeatsList');
    if (!bar) return;

    const count = selectedSeatElements.size;
    if (count > 0) {
        bar.style.display = 'flex';
        if (countEl) countEl.textContent = count;

        if (listEl) {
            const sortedLabels = Array.from(selectedSeatElements).map(el => {
                const row = el.getAttribute('data-row') || '';
                const num = parseInt(el.getAttribute('data-num') || '0', 10);
                return { label: row + num, row, num };
            }).sort((a, b) => {
                if (a.row !== b.row) return a.row.localeCompare(b.row);
                return a.num - b.num;
            }).map(item => item.label);

            if (sortedLabels.length <= 12) {
                listEl.textContent = sortedLabels.join(', ');
            } else {
                listEl.textContent = sortedLabels.slice(0, 10).join(', ') + ' ... +' + (sortedLabels.length - 10) + ' more';
            }
        }

        const actionBtn = bar.querySelector('.selection-actions .btn-primary');
        if (actionBtn) {
            if (count === 1) {
                const singleSeat = Array.from(selectedSeatElements)[0];
                const lbl = (singleSeat.getAttribute('data-row') || '') + (singleSeat.getAttribute('data-num') || '');
                actionBtn.innerHTML = `<span class="material-symbols-outlined" style="font-size:16px;">tune</span> Configure Seat (${lbl})`;
            } else {
                actionBtn.innerHTML = `<span class="material-symbols-outlined" style="font-size:16px;">tune</span> Configure ${count} Selected Seats`;
            }
        }
    } else {
        bar.style.display = 'none';
    }
}

/**
 * Open single seat modal
 */
function openSingleSeatModal(element) {
    const seatId = element.getAttribute('data-id');
    const roomId = element.getAttribute('data-roomid');
    const row = element.getAttribute('data-row');
    const num = element.getAttribute('data-num');
    const type = element.getAttribute('data-type');
    const typeId = element.getAttribute('data-typeid');
    const status = element.getAttribute('data-status');

    document.getElementById('modalSeatId').value = seatId;
    const modalSeatIds = document.getElementById('modalSeatIds');
    if (modalSeatIds) modalSeatIds.value = seatId;

    document.getElementById('modalRoomId').value = roomId;
    document.getElementById('modalSeatPos').textContent = row + num + ' (Row ' + row + ', Seat ' + num + ')';

    const modalTitle = document.getElementById('seatModalTitle');
    if (modalTitle) modalTitle.textContent = 'Seat configuration — ' + row + num;

    const errorBox = document.getElementById('seatModalError');
    if (errorBox) {
        errorBox.style.display = 'none';
        errorBox.textContent = '';
    }

    matchSeatTypeOption(typeId, type);

    const statusSelect = document.getElementById('modalSeatStatus');
    if (statusSelect) {
        if (status === 'Maintenance' || status === 'Lock' || status === 'Locked') {
            statusSelect.value = 'Maintenance';
        } else {
            statusSelect.value = 'Active';
        }
    }

    const modal = document.getElementById('seatModal');
    if (modal) modal.classList.add('show');
}

/**
 * Open modal to configure all currently selected seats at once.
 */
function openMultiSeatModal() {
    if (selectedSeatElements.size === 0) return;

    if (selectedSeatElements.size === 1) {
        openSingleSeatModal(Array.from(selectedSeatElements)[0]);
        return;
    }

    const seats = Array.from(selectedSeatElements);
    const seatIds = seats.map(s => s.getAttribute('data-id')).filter(Boolean);
    const roomId = seats[0].getAttribute('data-roomid');

    const sortedSeats = seats.map(el => {
        const row = el.getAttribute('data-row') || '';
        const num = parseInt(el.getAttribute('data-num') || '0', 10);
        return { label: row + num, row, num, type: el.getAttribute('data-type'), typeId: el.getAttribute('data-typeid'), status: el.getAttribute('data-status') };
    }).sort((a, b) => {
        if (a.row !== b.row) return a.row.localeCompare(b.row);
        return a.num - b.num;
    });

    const labels = sortedSeats.map(s => s.label);

    document.getElementById('modalSeatId').value = seatIds[0];
    const modalSeatIds = document.getElementById('modalSeatIds');
    if (modalSeatIds) modalSeatIds.value = seatIds.join(',');

    document.getElementById('modalRoomId').value = roomId;

    const modalTitle = document.getElementById('seatModalTitle');
    if (modalTitle) modalTitle.textContent = `Batch configure ${seatIds.length} selected seats`;

    let displayLabel = `${seatIds.length} seats selected: `;
    if (labels.length <= 8) {
        displayLabel += labels.join(', ');
    } else {
        displayLabel += labels.slice(0, 6).join(', ') + ' ... +' + (labels.length - 6) + ' more';
    }
    document.getElementById('modalSeatPos').textContent = displayLabel;

    const errorBox = document.getElementById('seatModalError');
    if (errorBox) {
        errorBox.style.display = 'none';
        errorBox.textContent = '';
    }

    matchSeatTypeOption(sortedSeats[0].typeId, sortedSeats[0].type);

    const statusSelect = document.getElementById('modalSeatStatus');
    if (statusSelect) {
        const sStatus = sortedSeats[0].status;
        if (sStatus === 'Maintenance' || sStatus === 'Lock' || sStatus === 'Locked') {
            statusSelect.value = 'Maintenance';
        } else {
            statusSelect.value = 'Active';
        }
    }

    const modal = document.getElementById('seatModal');
    if (modal) modal.classList.add('show');
}

function matchSeatTypeOption(typeId, type) {
    const typeSelect = document.getElementById('modalSeatTypeName');
    if (!typeSelect) return;

    let matched = false;
    if (typeId) {
        for (let i = 0; i < typeSelect.options.length; i++) {
            if (typeSelect.options[i].getAttribute('data-id') === typeId) {
                typeSelect.selectedIndex = i;
                matched = true;
                break;
            }
        }
    }
    if (!matched && type) {
        const cleanType = type.trim().toLowerCase();
        for (let i = 0; i < typeSelect.options.length; i++) {
            const optVal = typeSelect.options[i].value.trim().toLowerCase();
            const optCode = (typeSelect.options[i].getAttribute('data-code') || '').trim().toLowerCase();
            if (optVal === cleanType || optCode === cleanType || optVal.includes(cleanType) || cleanType.includes(optVal)) {
                typeSelect.selectedIndex = i;
                matched = true;
                break;
            }
        }
    }
}

function closeSeatModal() {
    const modal = document.getElementById('seatModal');
    if (modal) modal.classList.remove('show');
}

function validateSeatUpdateForm(event) {
    const seatId = parseInt(document.getElementById('modalSeatId')?.value || '0', 10);
    const seatIds = (document.getElementById('modalSeatIds')?.value || '').trim();
    const roomId = parseInt(document.getElementById('modalRoomId')?.value || '0', 10);
    const seatTypeName = (document.getElementById('modalSeatTypeName')?.value || '').trim();
    const status = (document.getElementById('modalSeatStatus')?.value || '').trim();
    const errorBox = document.getElementById('seatModalError');

    const allowedStatuses = ['Active', 'Maintenance', 'Lock', 'Locked'];
    let errorMsg = '';

    if ((!seatId || seatId <= 0) && !seatIds) {
        errorMsg = 'Invalid seat or screening room identifier.';
    } else if (!roomId || roomId <= 0) {
        errorMsg = 'Invalid screening room identifier.';
    } else if (!seatTypeName) {
        errorMsg = 'Please select a valid seat type.';
    } else if (!allowedStatuses.includes(status)) {
        errorMsg = 'Please select a valid seat status (Active or Lock).';
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
