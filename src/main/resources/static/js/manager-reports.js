/**
 * manager-reports.js
 * Consolidated JavaScript for Unified Reports & Analytics Module (M12, M13, M14 merged)
 */
document.addEventListener('DOMContentLoaded', function () {
    const filterForm = document.getElementById('reportFilterForm');
    const tabInput = document.getElementById('tabInput');
    const presetInput = document.getElementById('presetInput');
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const presetButtons = document.querySelectorAll('.preset-btn');
    const btnExportCsv = document.getElementById('btnExportCsv');

    // Preset button click handling
    presetButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const preset = this.getAttribute('data-preset');
            if (presetInput) presetInput.value = preset;

            presetButtons.forEach(b => {
                b.classList.remove('bg-cinema-red', 'text-white', 'border-cinema-red', 'shadow-sm');
                b.classList.add('bg-surface-container', 'text-secondary', 'border-[#EFE0D5]');
            });
            this.classList.remove('bg-surface-container', 'text-secondary', 'border-[#EFE0D5]');
            this.classList.add('bg-cinema-red', 'text-white', 'border-cinema-red', 'shadow-sm');

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

            if (preset !== 'custom' && startDateInput && endDateInput) {
                startDateInput.value = formatDateIso(start);
                endDateInput.value = formatDateIso(end);
            }

            if (filterForm) {
                filterForm.submit();
            }
        });
    });

    // If user manually changes dates, reset preset to custom
    [startDateInput, endDateInput].forEach(input => {
        if (input) {
            input.addEventListener('change', function () {
                if (presetInput) presetInput.value = 'custom';
                presetButtons.forEach(b => {
                    b.classList.remove('bg-cinema-red', 'text-white', 'border-cinema-red', 'shadow-sm');
                    b.classList.add('bg-surface-container', 'text-secondary', 'border-[#EFE0D5]');
                });
            });
        }
    });

    // Form submission validation
    if (filterForm) {
        filterForm.addEventListener('submit', function (e) {
            if (startDateInput && endDateInput && startDateInput.value && endDateInput.value) {
                if (startDateInput.value > endDateInput.value) {
                    e.preventDefault();
                    alert('Start date cannot be greater than end date.');
                    return false;
                }
            }
        });
    }

    // Export CSV handling for the active tab and current filters
    if (btnExportCsv) {
        btnExportCsv.addEventListener('click', function () {
            const preset = (presetInput && presetInput.value) ? presetInput.value : 'thismonth';
            const startDate = (startDateInput && startDateInput.value) ? startDateInput.value : '';
            const endDate = (endDateInput && endDateInput.value) ? endDateInput.value : '';
            const activeTab = (tabInput && tabInput.value) ? tabInput.value.toLowerCase() : 'revenue';

            let reportType = 'DAILY';
            if (activeTab === 'movies') {
                reportType = 'MOVIE';
            } else if (activeTab === 'transactions') {
                reportType = 'TRANSACTIONS';
            }

            if (startDate && endDate && startDate > endDate) {
                alert('Start date cannot be greater than end date.');
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
