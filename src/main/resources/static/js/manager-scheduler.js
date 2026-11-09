/**
 * CINEORA — Room Showtime Scheduler JS
 * Implements exact UI mockup interactions:
 * - Realtime Auto Calculation (Film End = Start + 10m trailer + duration, Cleaning = 15m)
 * - Proportional Timeline track with Saved (green), Draft (amber), and Cleaning Buffer (hatched)
 * - Full-featured Schedule Slots Table with Draft vs Saved distinction
 * - Realtime inline conflict detection
 * - Atomic Batch Save via /api/manager/showtimes/batch
 */

document.addEventListener('DOMContentLoaded', () => {
    // Application State
    const state = {
        roomId: window.SERVER_DATA ? window.SERVER_DATA.selectedRoomId : 0,
        roomName: 'Room',
        date: window.SERVER_DATA ? window.SERVER_DATA.selectedDate : '',
        minDate: window.SERVER_DATA ? window.SERVER_DATA.minDate : '',
        bufferMinutes: window.SERVER_DATA ? window.SERVER_DATA.cleaningBufferMinutes : 15,
        trailerMinutes: 0,
        existingShowtimes: window.SERVER_DATA && window.SERVER_DATA.existingShowtimes ? window.SERVER_DATA.existingShowtimes : [],
        draftShowtimes: [],
        editingDraftId: null,
        suggestedNextTime: null
    };

    // DOM Elements - Selection Card
    const roomSelect = document.getElementById('roomSelect');
    const dateSelect = document.getElementById('dateSelect');
    const infoRoomType = document.getElementById('infoRoomType');
    const infoRoomCapacity = document.getElementById('infoRoomCapacity');
    const infoRoomStatus = document.getElementById('infoRoomStatus');
    const timelineRoomBadge = document.getElementById('timelineRoomBadge');

    // DOM Elements - Timeline
    const timelineRuler = document.getElementById('timelineRuler');
    const timelineTrackArea = document.getElementById('timelineTrackArea');
    const legendSavedCount = document.getElementById('legendSavedCount');
    const legendDraftCount = document.getElementById('legendDraftCount');

    // DOM Elements - Table
    const scheduleTableBody = document.getElementById('scheduleTableBody');

    // DOM Elements - Add Slot Form
    const drawerMovieSelect = document.getElementById('drawerMovieSelect');
    const drawerStartTime = document.getElementById('drawerStartTime');
    const btnAddSlot = document.getElementById('btnAddSlot');
    const btnClearForm = document.getElementById('btnClearForm');

    // DOM Elements - Auto Calculation Box
    const calcDuration = document.getElementById('calcDuration');
    const calcFilmEnd = document.getElementById('calcFilmEnd');
    const calcRoomAvailable = document.getElementById('calcRoomAvailable');
    const calcConflictAlert = document.getElementById('calcConflictAlert');
    const calcConflictTitle = document.getElementById('calcConflictTitle');
    const calcConflictDesc = document.getElementById('calcConflictDesc');
    const btnApplySuggestion = document.getElementById('btnApplySuggestion');

    // DOM Elements - Sticky Save Bar
    const schedulerStickyBar = document.getElementById('schedulerStickyBar');
    const draftCountNumber = document.getElementById('draftCountNumber');
    const btnDiscardDrafts = document.getElementById('btnDiscardDrafts');
    const btnSaveSchedule = document.getElementById('btnSaveSchedule');

    // DOM Elements - Alerts
    const alertBox = document.getElementById('schedulerAlertBox');
    const alertMsg = document.getElementById('schedulerAlertMsg');
    const successBox = document.getElementById('schedulerSuccessBox');
    const successMsg = document.getElementById('schedulerSuccessMsg');

    // DOM Elements - Edit Modal
    const editModal = document.getElementById('editShowtimeModal');
    const editForm = document.getElementById('editShowtimeForm');
    const editShowtimeId = document.getElementById('editShowtimeId');
    const editMovieSelect = document.getElementById('editMovieSelect');
    const editStartTime = document.getElementById('editStartTime');

    // Timeline Boundaries: 08:00 to 02:00 (+1 day) = 18 hours = 1080 mins
    const TIMELINE_START_HOUR = 8;
    const TIMELINE_TOTAL_HOURS = 18;
    const TIMELINE_TOTAL_MINS = TIMELINE_TOTAL_HOURS * 60;

    const pad = (n) => String(n).padStart(2, '0');

    /**
     * Initialize Application
     */
    function init() {
        renderRuler();
        updateRoomBanner();
        renderAll();
        bindEvents();
        updateAutoCalculation();
    }

    function showAlert(msg) {
        if (alertBox && alertMsg) {
            alertMsg.innerHTML = msg;
            alertBox.style.display = 'flex';
            if (successBox) successBox.style.display = 'none';
            alertBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    function showSuccess(msg) {
        if (successBox && successMsg) {
            successMsg.innerHTML = msg;
            successBox.style.display = 'flex';
            if (alertBox) alertBox.style.display = 'none';
            successBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    function hideAlerts() {
        if (alertBox) alertBox.style.display = 'none';
        if (successBox) successBox.style.display = 'none';
    }

    /**
     * Update room information and timeline room badge from dropdown selection
     */
    function updateRoomBanner() {
        if (!roomSelect) return;
        const opt = roomSelect.options[roomSelect.selectedIndex];
        if (!opt) return;

        state.roomName = opt.getAttribute('data-name') || opt.text;
        const type = opt.getAttribute('data-type') || 'Standard Laser';
        const cap = opt.getAttribute('data-capacity') || '120';
        const status = opt.getAttribute('data-status') || 'Ready';

        if (infoRoomType) infoRoomType.textContent = type;
        if (infoRoomCapacity) infoRoomCapacity.textContent = cap;
        if (infoRoomStatus) infoRoomStatus.textContent = status;
        if (timelineRoomBadge) timelineRoomBadge.textContent = state.roomName;
    }

    /**
     * Render Hour Ruler for Timeline: 08:00 to 02:00 next day
     */
    function renderRuler() {
        if (!timelineRuler) return;
        timelineRuler.innerHTML = '';
        for (let i = 0; i <= TIMELINE_TOTAL_HOURS; i++) {
            const h = (TIMELINE_START_HOUR + i) % 24;
            const mark = document.createElement('div');
            mark.className = 'ruler-mark';
            mark.textContent = `${pad(h)}:00`;
            timelineRuler.appendChild(mark);
        }
    }

    /**
     * Get unified combined and sorted list of showtimes
     */
    function getCombinedShowtimes() {
        const combined = [
            ...state.existingShowtimes.map(st => ({
                isDraft: false,
                id: st.showtimeId,
                movieTitle: st.movieTitle,
                movieDuration: st.movieDuration || 120,
                trailerDuration: st.trailerDuration != null ? st.trailerDuration : 0,
                startTime: st.startTime,
                endTime: st.endTime,
                baseTicketPrice: st.baseTicketPrice,
                status: st.status
            })),
            ...state.draftShowtimes.map(d => ({
                isDraft: true,
                id: d.id,
                movieTitle: d.movieTitle,
                movieDuration: d.movieDuration,
                trailerDuration: 0,
                startTime: d.startTime,
                endTime: d.endTime,
                baseTicketPrice: d.baseTicketPrice,
                status: 'Draft'
            }))
        ];

        return combined.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    }

    /**
     * Master render function
     */
    function renderAll() {
        renderTimeline();
        renderTable();
        updateStickyBar();
    }

    /**
     * Render the Timeline Track Area
     */
    function renderTimeline() {
        if (!timelineTrackArea) return;
        timelineTrackArea.innerHTML = '';

        const baseTime = new Date(`${state.date}T${pad(TIMELINE_START_HOUR)}:00:00`).getTime();
        const totalDurationMs = TIMELINE_TOTAL_MINS * 60 * 1000;
        const items = getCombinedShowtimes();

        let savedCount = 0;
        let draftCount = 0;

        items.forEach(item => {
            if (item.isDraft) draftCount++;
            else savedCount++;

            const startMs = new Date(item.startTime).getTime();
            const endMs = new Date(item.endTime).getTime();
            const bufferEndMs = endMs + state.bufferMinutes * 60 * 1000;

            const leftPct = Math.max(0, ((startMs - baseTime) / totalDurationMs) * 100);
            const widthPct = Math.min(100 - leftPct, ((endMs - startMs) / totalDurationMs) * 100);
            const bufferWidthPct = Math.min(100 - (leftPct + widthPct), ((state.bufferMinutes * 60 * 1000) / totalDurationMs) * 100);

            if (widthPct > 0 && leftPct < 100) {
                // Movie Slot Block
                const block = document.createElement('div');
                block.className = `timeline-slot-block ${item.isDraft ? 'is-draft' : 'is-saved'}`;
                block.style.left = `${leftPct}%`;
                block.style.width = `${widthPct}%`;

                const startStr = formatTime(item.startTime);
                const endStr = formatTime(item.endTime);

                block.innerHTML = `
                    <span class="material-symbols-outlined block-icon">play_circle</span>
                    <div class="block-text">
                        <span class="block-title" title="${item.movieTitle}">${item.movieTitle}</span>
                        <span class="block-time">${startStr} – ${endStr}</span>
                    </div>
                `;

                if (item.isDraft) {
                    block.title = `Draft: ${item.movieTitle} (${startStr} – ${endStr}). Click to edit.`;
                    block.addEventListener('click', () => editDraft(item.id));
                } else {
                    block.title = `Saved: ${item.movieTitle} (${startStr} – ${endStr}). Click to edit.`;
                    block.addEventListener('click', () => openEditSavedModal(item.id));
                }

                timelineTrackArea.appendChild(block);

                // Cleaning Buffer Block (Hatched Pattern)
                if (bufferWidthPct > 0) {
                    const buf = document.createElement('div');
                    buf.className = 'timeline-cleaning-block';
                    buf.style.left = `${leftPct + widthPct}%`;
                    buf.style.width = `${bufferWidthPct}%`;
                    if (bufferWidthPct >= 4) {
                        buf.textContent = `Cleaning 15m`;
                    } else if (bufferWidthPct >= 2.2) {
                        buf.textContent = `15m`;
                    } else {
                        buf.textContent = ``;
                    }
                    buf.title = `Cleaning buffer (15m) until ${formatTime(new Date(bufferEndMs))}`;
                    timelineTrackArea.appendChild(buf);
                }
            }
        });

        if (legendSavedCount) legendSavedCount.textContent = savedCount;
        if (legendDraftCount) legendDraftCount.textContent = draftCount;
    }

    /**
     * Render the Schedule Slots Table
     */
    function renderTable() {
        if (!scheduleTableBody) return;
        scheduleTableBody.innerHTML = '';

        const items = getCombinedShowtimes();

        if (items.length === 0) {
            scheduleTableBody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align:center;padding:32px;color:#9CA3AF;">
                        No showtimes scheduled for ${state.roomName} on this date. Use the form on the right to add your first movie slot.
                    </td>
                </tr>
            `;
            return;
        }

        items.forEach((item, index) => {
            const tr = document.createElement('tr');
            if (item.isDraft) tr.className = 'row-draft';

            const startStr = formatTime(item.startTime);
            const durationStr = `${item.movieDuration}m`;
            const endStr = formatTime(item.endTime);
            const availStr = formatTime(new Date(new Date(item.endTime).getTime() + state.bufferMinutes * 60 * 1000));
            const priceFormatted = new Intl.NumberFormat('vi-VN').format(item.baseTicketPrice) + ' ₫';

            const statusHtml = item.isDraft
                ? `<span class="badge-status-draft"><span class="material-symbols-outlined" style="font-size:13px">schedule</span> Draft</span>`
                : `<span class="badge-status-saved"><span class="material-symbols-outlined" style="font-size:13px">check</span> Saved</span>`;

            let actionsHtml = '';
            if (item.isDraft) {
                actionsHtml = `
                    <div class="table-actions">
                        <button type="button" class="btn-table-action" onclick="window.schedulerEditDraft('${item.id}')" title="Edit draft slot">
                            <span class="material-symbols-outlined" style="font-size:16px;">edit</span>
                        </button>
                        <button type="button" class="btn-table-action text-del" onclick="window.schedulerRemoveDraft('${item.id}')" title="Remove draft slot">
                            <span class="material-symbols-outlined" style="font-size:16px;">delete</span>
                        </button>
                    </div>
                `;
            } else {
                actionsHtml = `
                    <div class="table-actions">
                        <button type="button" class="btn-table-action" onclick="window.schedulerEditExisting(${item.id})" title="Edit showtime">
                            <span class="material-symbols-outlined" style="font-size:16px;">edit</span>
                        </button>
                        <button type="button" class="btn-table-action text-del" onclick="window.schedulerStopExisting(${item.id})" title="Stop showtime (BR-54)">
                            <span class="material-symbols-outlined" style="font-size:16px;">delete</span>
                        </button>
                    </div>
                `;
            }

            tr.innerHTML = `
                <td style="text-align:center;"><strong>${index + 1}</strong></td>
                <td class="col-movie">
                    <div class="movie-title-cell">
                        <span class="material-symbols-outlined movie-icon">movie</span>
                        <span title="${item.movieTitle}">${item.movieTitle}</span>
                    </div>
                </td>
                <td><strong>${startStr}</strong></td>
                <td>${durationStr}</td>
                <td><strong>${endStr}</strong></td>
                <td><strong style="color:#111827;">${availStr}</strong></td>
                <td><strong>${priceFormatted}</strong></td>
                <td style="text-align:center;">${statusHtml}</td>
                <td style="text-align:center;">${actionsHtml}</td>
            `;

            scheduleTableBody.appendChild(tr);
        });
    }

    /**
     * Update Sticky Bottom Save Bar
     */
    function updateStickyBar() {
        if (!schedulerStickyBar) return;
        const count = state.draftShowtimes.length;
        if (count > 0) {
            schedulerStickyBar.style.display = 'flex';
            if (draftCountNumber) draftCountNumber.textContent = count;
            const labelEl = document.getElementById('stickyBarLabel');
            if (labelEl) {
                labelEl.innerHTML = `<strong>${count}</strong> draft slot${count > 1 ? 's' : ''} ready to save`;
            }
        } else {
            schedulerStickyBar.style.display = 'none';
        }
    }

    /**
     * Realtime Auto Calculation in Add Slot Form
     * Film End Time = Start Time + Trailer (10m) + Movie Duration
     * Room Available At = Film End Time + 15m Cleaning Buffer
     */
    function updateAutoCalculation() {
        if (!drawerMovieSelect || !drawerStartTime) return;
        const opt = drawerMovieSelect.options[drawerMovieSelect.selectedIndex];
        const hasMovie = opt && opt.value && opt.value.trim() !== '';
        const duration = hasMovie ? parseInt(opt.getAttribute('data-duration') || '120', 10) : null;
        const timeVal = drawerStartTime.value;

        if (!hasMovie) {
            if (calcDuration) calcDuration.textContent = '-- min';
            if (calcFilmEnd) calcFilmEnd.textContent = '--:--';
            if (calcRoomAvailable) calcRoomAvailable.textContent = '--:--';
            if (calcConflictAlert) calcConflictAlert.style.display = 'none';
            if (btnApplySuggestion) btnApplySuggestion.style.display = 'none';
            return;
        }

        if (calcDuration) calcDuration.textContent = `${duration} min`;

        if (!timeVal) {
            if (calcFilmEnd) calcFilmEnd.textContent = '--:--';
            if (calcRoomAvailable) calcRoomAvailable.textContent = '--:--';
            if (calcConflictAlert) calcConflictAlert.style.display = 'none';
            if (btnApplySuggestion) btnApplySuggestion.style.display = 'none';
            return;
        }

        const startDt = new Date(`${state.date}T${timeVal}:00`);
        if (isNaN(startDt.getTime())) return;

        // Mandatory Formula: Film End = Start + duration (no trailer)
        const totalMovieSpanMs = duration * 60 * 1000;
        const filmEndDt = new Date(startDt.getTime() + totalMovieSpanMs);
        const roomAvailDt = new Date(filmEndDt.getTime() + state.bufferMinutes * 60 * 1000);

        if (calcFilmEnd) calcFilmEnd.textContent = formatTime(filmEndDt);
        if (calcRoomAvailable) calcRoomAvailable.textContent = formatTime(roomAvailDt);

        // Realtime conflict check
        checkConflictRealtime(startDt, filmEndDt);
    }

    /**
     * Check conflict in realtime against existing DB showtimes + drafts
     */
    function checkConflictRealtime(startDt, filmEndDt) {
        if (!calcConflictAlert) return false;
        const conflict = findConflictWithAll(startDt, filmEndDt, state.editingDraftId);

        if (conflict) {
            const suggested = calculateNextAvailableTime();
            state.suggestedNextTime = suggested;

            if (calcConflictTitle) calcConflictTitle.textContent = 'Schedule Conflict!';
            if (calcConflictDesc) calcConflictDesc.textContent = conflict.message;

            if (btnApplySuggestion && suggested && suggested !== drawerStartTime.value) {
                btnApplySuggestion.textContent = `Use ${suggested}`;
                btnApplySuggestion.style.display = 'inline-block';
            } else if (btnApplySuggestion) {
                btnApplySuggestion.style.display = 'none';
            }

            calcConflictAlert.style.display = 'block';
            return true;
        } else {
            calcConflictAlert.style.display = 'none';
            if (btnApplySuggestion) btnApplySuggestion.style.display = 'none';
            state.suggestedNextTime = null;
            return false;
        }
    }

    /**
     * Symmetrical conflict detector:
     * Room occupied during [StartTime, FilmEndTime + 15m cleaning)
     */
    function findConflictWithAll(startDt, filmEndDt, excludeDraftId) {
        const startMs = startDt.getTime();
        const endMs = filmEndDt.getTime();
        const bufMs = state.bufferMinutes * 60 * 1000;

        // Check Existing DB showtimes
        for (const st of state.existingShowtimes) {
            const stStartMs = new Date(st.startTime).getTime();
            const stEndMs = new Date(st.endTime).getTime();

            // Symmetrical conflict: newStart < existingEnd + 15m AND existingStart < newEnd + 15m
            if (startMs < stEndMs + bufMs && stStartMs < endMs + bufMs) {
                const conflictEndStr = formatTime(new Date(stEndMs + bufMs));
                return {
                    hasConflict: true,
                    message: `${state.roomName} is occupied until ${conflictEndStr} by '${st.movieTitle}' (including 15m cleaning).`
                };
            }
        }

        // Check other Draft showtimes
        for (const d of state.draftShowtimes) {
            if (excludeDraftId && d.id === excludeDraftId) continue;

            const dStartMs = new Date(d.startTime).getTime();
            const dEndMs = new Date(d.endTime).getTime();

            if (startMs < dEndMs + bufMs && dStartMs < endMs + bufMs) {
                const conflictEndStr = formatTime(new Date(dEndMs + bufMs));
                return {
                    hasConflict: true,
                    message: `Conflict with draft '${d.movieTitle}' (${formatTime(d.startTime)} – ${formatTime(d.endTime)} + 15m cleaning until ${conflictEndStr}).`
                };
            }
        }

        return null;
    }

    /**
     * Auto-calculate next non-conflicting time (HH:mm)
     */
    function calculateNextAvailableTime() {
        const opt = drawerMovieSelect.options[drawerMovieSelect.selectedIndex];
        const duration = (opt && opt.value) ? parseInt(opt.getAttribute('data-duration') || '120', 10) : 120;
        const requiredSpanMs = (duration + state.bufferMinutes) * 60 * 1000;

        const intervals = [
            ...state.existingShowtimes.map(st => ({
                startMs: new Date(st.startTime).getTime(),
                endBufMs: new Date(st.endTime).getTime() + state.bufferMinutes * 60 * 1000
            })),
            ...state.draftShowtimes.filter(d => d.id !== state.editingDraftId).map(d => ({
                startMs: new Date(d.startTime).getTime(),
                endBufMs: new Date(d.endTime).getTime() + state.bufferMinutes * 60 * 1000
            }))
        ].sort((a, b) => a.startMs - b.startMs);

        let candidateStartMs = new Date(`${state.date}T09:00:00`).getTime();

        for (const it of intervals) {
            if (candidateStartMs + requiredSpanMs <= it.startMs) {
                break;
            }
            if (candidateStartMs < it.endBufMs) {
                candidateStartMs = it.endBufMs;
            }
        }

        const candidateDate = new Date(candidateStartMs);
        return `${pad(candidateDate.getHours())}:${pad(candidateDate.getMinutes())}`;
    }

    /**
     * Auto resolve base price according to Ticket Pricing rules:
     * - Weekend = 110,000 VND
     * - Weekday: morning (< 12h) = 65,000; standard (12h-17h) = 85,000; golden hour (> 17h) = 95,000
     * - IMAX surcharge = +60,000 VND
     */
    function resolveSlotBasePrice(dt, roomType) {
        if (!dt || isNaN(dt.getTime())) return 85000;
        const day = dt.getDay(); // 0 is Sunday, 6 is Saturday
        const isWeekend = (day === 0 || day === 6);
        const timeInMinutes = dt.getHours() * 60 + dt.getMinutes();

        let base = 85000;
        if (isWeekend) {
            base = 110000;
        } else {
            if (timeInMinutes < 12 * 60) {
                base = 65000;
            } else if (timeInMinutes < 17 * 60) {
                base = 85000;
            } else {
                base = 95000;
            }
        }

        const rt = (roomType || state.roomType || '').toUpperCase();
        if (rt.includes('IMAX')) {
            base += 60000;
        }
        return base;
    }

    /**
     * Add Slot to Draft Schedule
     */
    function handleAddSlot() {
        const movieId = drawerMovieSelect.value;
        const timeVal = drawerStartTime.value;

        if (!movieId) {
            showAlert('Please select a movie for this slot.');
            drawerMovieSelect.focus();
            return;
        }
        if (!timeVal) {
            showAlert('Please enter start time.');
            drawerStartTime.focus();
            return;
        }

        const opt = drawerMovieSelect.options[drawerMovieSelect.selectedIndex];
        const duration = parseInt(opt.getAttribute('data-duration') || '120', 10);
        const title = opt.getAttribute('data-title') || opt.text;

        const startDt = new Date(`${state.date}T${timeVal}:00`);
        const priceVal = resolveSlotBasePrice(startDt, state.roomType);
        const totalSpanMs = duration * 60 * 1000;
        const filmEndDt = new Date(startDt.getTime() + totalSpanMs);
        const roomAvailDt = new Date(filmEndDt.getTime() + state.bufferMinutes * 60 * 1000);

        // Validate Conflict
        const conflict = findConflictWithAll(startDt, filmEndDt, state.editingDraftId);
        if (conflict) {
            checkConflictRealtime(startDt, filmEndDt);
            showAlert('Cannot add slot: ' + conflict.message);
            return;
        }

        const startTimeIso = `${state.date}T${timeVal}:00`;
        const endTimeIso = formatIso(filmEndDt);

        if (state.editingDraftId) {
            // Edit mode
            const idx = state.draftShowtimes.findIndex(d => d.id === state.editingDraftId);
            if (idx !== -1) {
                state.draftShowtimes[idx] = {
                    ...state.draftShowtimes[idx],
                    movieId: parseInt(movieId, 10),
                    movieTitle: title,
                    movieDuration: duration,
                    trailerDuration: 0,
                    startTime: startTimeIso,
                    endTime: endTimeIso,
                    baseTicketPrice: parseFloat(priceVal)
                };
            }
            state.editingDraftId = null;
            btnAddSlot.innerHTML = `<span class="material-symbols-outlined" style="font-size:18px;">add</span> Add to Schedule`;
        } else {
            // New draft
            const newDraft = {
                id: 'draft_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
                movieId: parseInt(movieId, 10),
                movieTitle: title,
                movieDuration: duration,
                trailerDuration: 0,
                startTime: startTimeIso,
                endTime: endTimeIso,
                baseTicketPrice: parseFloat(priceVal),
                status: 'Draft'
            };
            state.draftShowtimes.push(newDraft);
        }

        hideAlerts();
        renderAll();

        // Auto-advance Start Time to Room Available At for convenience
        drawerStartTime.value = formatTime(roomAvailDt);
        updateAutoCalculation();
    }

    /**
     * Clear Form Inputs (does not affect drafts)
     */
    function handleClearForm() {
        state.editingDraftId = null;
        btnAddSlot.innerHTML = `<span class="material-symbols-outlined" style="font-size:18px;">add</span> Add to Schedule`;
        drawerMovieSelect.selectedIndex = 0;
        drawerStartTime.value = '17:00';
        if (calcConflictAlert) calcConflictAlert.style.display = 'none';
        updateAutoCalculation();
    }

    /**
     * Edit a Draft Slot
     */
    function editDraft(draftId) {
        const d = state.draftShowtimes.find(item => item.id === draftId);
        if (!d) return;

        state.editingDraftId = draftId;
        btnAddSlot.innerHTML = `<span class="material-symbols-outlined" style="font-size:18px;">check</span> Update Draft Slot`;
        drawerMovieSelect.value = d.movieId;

        const startDt = new Date(d.startTime);
        drawerStartTime.value = `${pad(startDt.getHours())}:${pad(startDt.getMinutes())}`;

        updateAutoCalculation();
        drawerStartTime.focus();
    }

    /**
     * Remove a Draft Slot
     */
    function removeDraft(draftId) {
        state.draftShowtimes = state.draftShowtimes.filter(d => d.id !== draftId);
        if (state.editingDraftId === draftId) {
            handleClearForm();
        }
        renderAll();
    }

    /**
     * Batch Save Schedule to Backend
     */
    async function handleBatchSave() {
        if (state.draftShowtimes.length === 0) return;

        if (!btnSaveSchedule) return;
        btnSaveSchedule.disabled = true;
        const originalText = btnSaveSchedule.innerHTML;
        btnSaveSchedule.innerHTML = `<span class="material-symbols-outlined" style="font-size:18px;animation:spin 1s linear infinite;">sync</span> Saving...`;

        hideAlerts();

        const payload = {
            roomId: state.roomId,
            showtimes: state.draftShowtimes.map(d => ({
                movieId: d.movieId,
                roomId: state.roomId,
                startTime: d.startTime,
                baseTicketPrice: d.baseTicketPrice,
                status: 'Scheduled'
            }))
        };

        try {
            const resp = await fetch('/api/manager/showtimes/batch', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (resp.redirected && resp.url.includes('/login')) {
                showAlert('Your session has expired. Please refresh the page or sign in again.');
                return;
            }

            let data;
            const contentType = resp.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                data = await resp.json();
            } else {
                if (!resp.ok) {
                    showAlert(`Server error (HTTP ${resp.status}). Please check your login session.`);
                    return;
                }
                const text = await resp.text();
                if (text.includes('Sign In') || text.includes('login')) {
                    showAlert('Your session has expired. Please refresh the page or sign in again.');
                    return;
                }
                showAlert('Unexpected response from server.');
                return;
            }

            if (resp.ok && data.success) {
                showSuccess(data.message || `Successfully created ${state.draftShowtimes.length} showtimes!`);
                state.draftShowtimes = [];
                await reloadRoomSchedule();
            } else {
                const errMsg = data.message || (data.errors ? Object.values(data.errors).join(', ') : 'Failed to save schedule.');
                showAlert(errMsg);
            }
        } catch (err) {
            showAlert('Network error: ' + err.message);
        } finally {
            btnSaveSchedule.disabled = false;
            btnSaveSchedule.innerHTML = originalText;
            updateStickyBar();
        }
    }

    /**
     * Reload room schedule from REST API
     */
    async function reloadRoomSchedule() {
        try {
            const resp = await fetch(`/api/manager/showtimes/room-schedule?roomId=${state.roomId}&date=${state.date}`);
            const contentType = resp.headers.get('content-type') || '';
            if (resp.ok && contentType.includes('application/json')) {
                const data = await resp.json();
                if (data.success) {
                    state.existingShowtimes = data.data || [];
                    renderAll();
                }
            }
        } catch (e) {
            console.error('Failed to reload schedule:', e);
        }
    }

    /**
     * Handle Room or Date Selector Change
     */
    function handleRoomOrDateChange() {
        if (state.draftShowtimes.length > 0) {
            if (!confirm('You have unsaved draft showtimes for this room. Changing room or date will discard your drafts. Continue?')) {
                roomSelect.value = state.roomId;
                dateSelect.value = state.date;
                return;
            }
            state.draftShowtimes = [];
        }

        state.roomId = parseInt(roomSelect.value, 10);
        state.date = dateSelect.value;
        updateRoomBanner();
        hideAlerts();
        reloadRoomSchedule();
    }

    /**
     * Formatting Helpers
     */
    function formatTime(dtInput) {
        if (!dtInput) return '--:--';
        const d = new Date(dtInput);
        if (isNaN(d.getTime())) return '--:--';
        return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }

    function formatIso(d) {
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
    }

    /**
     * Edit Modal for Existing Saved Showtime
     */
    function openEditSavedModal(id) {
        const item = state.existingShowtimes.find(st => st.showtimeId === id);
        if (!item) return;

        if (editShowtimeId) editShowtimeId.value = item.showtimeId;
        if (editMovieSelect) editMovieSelect.value = item.movieId;
        if (editStartTime) {
            const dt = new Date(item.startTime);
            editStartTime.value = `${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
        }

        const titleEl = document.getElementById('editModalTitle');
        if (titleEl) titleEl.textContent = `Edit Saved Showtime #${item.showtimeId}`;

        if (editModal) editModal.classList.add('open');
    }

    window.closeEditModal = () => {
        if (editModal) editModal.classList.remove('open');
    };

    /**
     * Bind DOM Events
     */
    function bindEvents() {
        // Selection card changes
        if (roomSelect) roomSelect.addEventListener('change', handleRoomOrDateChange);
        if (dateSelect) dateSelect.addEventListener('change', handleRoomOrDateChange);

        // Add Slot Form
        if (drawerMovieSelect) drawerMovieSelect.addEventListener('change', updateAutoCalculation);
        if (drawerStartTime) drawerStartTime.addEventListener('input', updateAutoCalculation);
        if (btnAddSlot) btnAddSlot.addEventListener('click', handleAddSlot);
        if (btnClearForm) btnClearForm.addEventListener('click', handleClearForm);

        if (btnApplySuggestion) {
            btnApplySuggestion.addEventListener('click', () => {
                if (state.suggestedNextTime) {
                    drawerStartTime.value = state.suggestedNextTime;
                    updateAutoCalculation();
                }
            });
        }

        // Sticky Bar
        if (btnSaveSchedule) btnSaveSchedule.addEventListener('click', handleBatchSave);
        if (btnDiscardDrafts) {
            btnDiscardDrafts.addEventListener('click', () => {
                if (confirm('Are you sure you want to discard all unsaved draft showtimes?')) {
                    state.draftShowtimes = [];
                    handleClearForm();
                    renderAll();
                }
            });
        }

        // Edit form submit
        if (editForm) {
            editForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const id = editShowtimeId.value;
                const movieId = editMovieSelect.value;
                const timeVal = editStartTime.value;
                const startDt = new Date(`${state.date}T${timeVal}:00`);
                const autoPrice = resolveSlotBasePrice(startDt, state.roomType);

                const payload = {
                    showtimeId: parseInt(id, 10),
                    movieId: parseInt(movieId, 10),
                    roomId: state.roomId,
                    startTime: `${state.date}T${timeVal}:00`,
                    baseTicketPrice: autoPrice,
                    status: 'Scheduled'
                };

                try {
                    const resp = await fetch(`/api/manager/showtimes/${id}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const contentType = resp.headers.get('content-type') || '';
                    if (resp.ok && contentType.includes('application/json')) {
                        const res = await resp.json();
                        if (res.success) {
                            showSuccess('Showtime updated successfully.');
                            closeEditModal();
                            reloadRoomSchedule();
                        } else {
                            showAlert(res.message || 'Failed to update showtime.');
                        }
                    } else {
                        showAlert(`Failed to update showtime (HTTP ${resp.status}).`);
                    }
                } catch (err) {
                    showAlert('Error: ' + err.message);
                }
            });
        }
    }

    // Expose functions to window for onclick handlers
    window.schedulerEditDraft = editDraft;
    window.schedulerRemoveDraft = removeDraft;
    window.schedulerEditExisting = openEditSavedModal;
    window.schedulerStopExisting = (id) => {
        if (confirm('Are you sure you want to stop this showtime (BR-54)?')) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = `/manager/showtimes/${id}/stop?date=${state.date}`;
            document.body.appendChild(form);
            form.submit();
        }
    };

    // Run
    init();
});
