document.addEventListener('DOMContentLoaded', () => {
  const panel = document.getElementById('account-security');
  if (!panel) return;
  const form = document.getElementById('security-form');
  const send = document.getElementById('security-send');
  const save = document.getElementById('security-save');
  const message = document.getElementById('security-message');
  const countdown = document.getElementById('security-cooldown');
  const code = document.getElementById('security-otp');
  const password = document.getElementById('security-password');
  const confirm = document.getElementById('security-confirm');
  let timer, until = 0, busy = false;
  const show = (text, kind = '') => { message.textContent = text; message.dataset.kind = kind; };
  const tick = () => {
    const remaining = Math.max(0, Math.ceil((until - Date.now()) / 1000));
    send.disabled = busy || remaining > 0;
    countdown.textContent = remaining ? `You can resend in ${remaining}s.` : '';
    if (!remaining) clearInterval(timer);
  };
  const cooldown = seconds => { clearInterval(timer); until = Date.now() + Math.max(0, Number(seconds) || 0) * 1000; tick(); if (until > Date.now()) timer = setInterval(tick, 1000); };
  async function post(url, body = new URLSearchParams()) {
    const token = document.getElementById('security-csrf-token').value || decodeURIComponent(document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)?.[1] || '');
    const header = document.getElementById('security-csrf-header').value || 'X-XSRF-TOKEN';
    const response = await fetch(url, {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded', ...(token ? {[header]:token} : {})}, body});
    if (response.redirected || response.status === 401) throw new Error('Your session has expired. Please sign in again.');
    const result = await response.json();
    return {response,result};
  }
  send.addEventListener('click', async () => {
    busy = true; tick(); show('Sending verification code…');
    try {
      const {response,result} = await post('/account/password/request');
      cooldown(result.retryAfterSeconds);
      show(result.message, result.success ? 'success' : 'error');
      if (response.ok && result.success) {
        form.hidden = false; send.textContent = 'Resend verification code';
        document.getElementById('security-expiry').textContent = `Code expires in ${Math.ceil(result.expiresInSeconds / 60)} minutes. Only the latest code can be used.`;
        code.value = ''; code.focus();
      }
    } catch (error) { show(error.message || 'Verification is unavailable. Please try again.', 'error'); }
    finally { busy = false; tick(); }
  });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!/^[0-9]{6}$/.test(code.value.trim())) { show('Enter the 6-digit verification code.', 'error'); code.focus(); return; }
    const min = Math.max(8, Number(panel.dataset.minLength) || 8);
    if (password.value.length < min || !/^(?=.*[A-Za-z])(?=.*\d).{8,128}$/.test(password.value)) { show(`Use ${min}–128 characters with at least one letter and one number.`, 'error'); password.focus(); return; }
    if (password.value !== confirm.value) { show('Password confirmation does not match.', 'error'); confirm.focus(); return; }
    save.disabled = true;
    try {
      const {result} = await post('/account/password/update', new URLSearchParams({otp:code.value.trim(),password:password.value,confirmPassword:confirm.value}));
      show(result.message, result.success ? 'success' : 'error');
      if (result.success) { form.reset(); resetEyes(); form.hidden = true; send.textContent = 'Send verification code'; }
    } catch (error) { show(error.message || 'The password could not be updated.', 'error'); }
    finally { save.disabled = false; }
  });
  const eyes = panel.querySelectorAll('[data-toggle-password]');
  function resetEyes() { eyes.forEach(button => { document.getElementById(button.dataset.togglePassword).type = 'password'; button.setAttribute('aria-pressed','false'); button.setAttribute('aria-label',button.dataset.togglePassword === 'security-password' ? 'Show new password' : 'Show confirm password'); }); }
  eyes.forEach(button => button.addEventListener('click', () => { const input = document.getElementById(button.dataset.togglePassword); const visible = input.type === 'password'; input.type = visible ? 'text' : 'password'; button.setAttribute('aria-pressed',String(visible)); button.setAttribute('aria-label', (visible ? 'Hide ' : 'Show ') + (input === password ? 'new password' : 'confirm password')); }));
  const dialog = document.getElementById('password-modal');
  document.getElementById('open-password-modal')?.addEventListener('click', () => dialog.showModal());
  dialog?.querySelector('[data-close-security]')?.addEventListener('click', () => dialog.close());
  dialog?.addEventListener('close', () => { password.value = ''; confirm.value = ''; code.value = ''; resetEyes(); });
});
