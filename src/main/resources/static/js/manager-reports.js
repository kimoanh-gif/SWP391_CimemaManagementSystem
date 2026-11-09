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
    const btnExportCsv = document.getElementById('btnExportCsv');

    // If user changes dates, reset preset to custom
    [startDateInput, endDateInput].forEach(input => {
        input.addEventListener('change', function () {
            if (presetInput) presetInput.value = 'custom';
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
});
