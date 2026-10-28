(() => {
  const form = document.getElementById('revenueFilter');
  if (!form) return;
  const from = document.getElementById('revenueFrom');
  const to = document.getElementById('revenueTo');
  const help = document.getElementById('revenueRangeHelp');
  function anniversary(value, years) {
    const date = new Date(value + 'T00:00:00Z');
    const month = date.getUTCMonth();
    date.setUTCFullYear(date.getUTCFullYear() + years);
    if (date.getUTCMonth() !== month) date.setUTCDate(0);
    return date;
  }
  function validate() {
    from.setCustomValidity('');
    to.setCustomValidity('');
    if (!from.value || !to.value) {
      help.textContent = 'Select both dates. The maximum range is 10 years.';
      return false;
    }
    if (from.value > to.value) {
      to.setCustomValidity('To date must be on or after From date.');
    } else if (new Date(to.value + 'T00:00:00Z') > anniversary(from.value, 10)) {
      to.setCustomValidity('The maximum date range is 10 years.');
    }
    if (!from.validity.valid || !to.validity.valid) {
      help.textContent = 'Check the dates. From date must not be after To date, and the range must not exceed 10 years.';
      return false;
    }
    const end = new Date(to.value + 'T00:00:00Z');
    const days = (end - new Date(from.value + 'T00:00:00Z')) / 86400000 + 1;
    const unit = days <= 31 ? 'day' : days <= 90 ? 'week'
      : end < anniversary(from.value, 2) ? 'month'
      : end < anniversary(from.value, 5) ? 'quarter' : 'year';
    help.textContent = 'One ' + unit + ' per bar. Click Search to apply the selected dates.';
    return true;
  }
  for (const input of [from, to]) input.addEventListener('input', validate);
  form.addEventListener('submit', event => {
    if (!validate()) {
      event.preventDefault();
      form.reportValidity();
    }
  });
  validate();
})();
