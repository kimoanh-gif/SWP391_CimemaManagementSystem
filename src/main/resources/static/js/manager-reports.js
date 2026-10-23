/**
 * manager-reports.js
 * JavaScript for Screen 47 & 49: View Reports & Statistics & Export
 */
document.addEventListener('DOMContentLoaded', function () {
    const filterForm = document.getElementById('reportFilterForm');
    const presetInput = document.getElementById('presetInput');
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const reportTypeSelect = document.getElementById('reportType');
    const presetButtons = document.querySelectorAll('.preset-btn');
    const btnExportCsv = document.getElementById('btnExportCsv');

    // Preset button click handling
    presetButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const preset = this.getAttribute('data-preset');
            presetInput.value = preset;

            presetButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            const today = new Date();
            let start = new Date();
            let end = new Date();

            if (preset === 'today') {
                start = today;
                end = today;
            } else if (preset === 'last7days') {
                start = new Date(today);
                start.setDate(today.getDate() - 6);
                end = today;
            } else if (preset === 'thismonth') {
                start = new Date(today.getFullYear(), today.getMonth(), 1);
                end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
            } else if (preset === 'thisquarter') {
                const currentMonth = today.getMonth(); // 0-11
                const quarterStartMonth = Math.floor(currentMonth / 3) * 3;
                start = new Date(today.getFullYear(), quarterStartMonth, 1);
                end = new Date(today.getFullYear(), quarterStartMonth + 3, 0);
            } else if (preset === 'all') {
                start = new Date(2020, 0, 1);
                end = new Date(today.getFullYear() + 1, 11, 31);
            }

            if (preset !== 'custom') {
                startDateInput.value = formatDateIso(start);
                endDateInput.value = formatDateIso(end);
            }

            filterForm.submit();
        });
    });

    // If user manually changes dates, reset preset to custom
    [startDateInput, endDateInput].forEach(input => {
        input.addEventListener('change', function () {
            presetInput.value = 'custom';
            presetButtons.forEach(b => b.classList.remove('active'));
        });
    });

    // Form submission validation
    filterForm.addEventListener('submit', function (e) {
        if (startDateInput.value && endDateInput.value) {
            if (startDateInput.value > endDateInput.value) {
                e.preventDefault();
                alert('The start date cannot be after the end date.');
                return;
            }
        }
    });

    // Export CSV handling
    if (btnExportCsv) {
        btnExportCsv.addEventListener('click', function () {
            const preset = presetInput.value || 'thismonth';
            const startDate = startDateInput.value || '';
            const endDate = endDateInput.value || '';
            const reportType = reportTypeSelect.value || 'DAILY';

            if (startDate && endDate && startDate > endDate) {
                alert('The start date cannot be after the end date.');
                return;
            }

            const exportUrl = `/api/manager/reports/export?preset=${encodeURIComponent(preset)}&startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&reportType=${encodeURIComponent(reportType)}`;

            // Trigger file download
            window.location.href = exportUrl;
        });
    }

    function formatDateIso(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
});
