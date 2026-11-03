document.addEventListener('DOMContentLoaded', () => {
  const menus = document.querySelectorAll('.workspace-account');
  document.addEventListener('click', event => menus.forEach(menu => {
    if (!menu.contains(event.target)) menu.open = false;
  }));
  document.addEventListener('keydown', event => {
    if (event.key !== 'Escape') return;
    menus.forEach(menu => {
      if (menu.open) { menu.open = false; menu.querySelector('summary').focus(); }
    });
  });
  menus.forEach(menu => menu.addEventListener('toggle', () => {
    if (menu.open) menus.forEach(other => { if (other !== menu) other.open = false; });
  }));
});
