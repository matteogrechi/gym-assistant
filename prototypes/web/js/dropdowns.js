'use strict';
// GYM-006, GYM-015, GYM-025: style native pickers inside the simulated app window.
// Selection, validation, labels and keyboard interaction remain browser-owned.
(() => {
  if (!CSS.supports('appearance', 'base-select')) return;
  const phone = document.querySelector('.phone');
  function position(select) {
    if (select.disabled) return;
    const field = select.getBoundingClientRect();
    const bounds = phone.getBoundingClientRect();
    const left = Math.max(8, bounds.left + phone.clientLeft + 8);
    const right = Math.min(innerWidth - 8, bounds.left + phone.clientLeft + phone.clientWidth - 8);
    const top = Math.max(8, bounds.top + phone.clientTop + 8);
    const bottom = Math.min(innerHeight - 8, bounds.top + phone.clientTop + phone.clientHeight - 8);
    const below = Math.max(0, bottom - field.bottom - 4);
    const above = Math.max(0, field.top - top - 4);
    const desired = Math.min(320, select.options.length * 48 + 16);
    const opensBelow = below >= desired || below >= above;
    const width = Math.min(field.width, right - left);
    select.style.setProperty('--picker-left', `${Math.max(left, Math.min(field.left, right - width))}px`);
    select.style.setProperty('--picker-width', `${width}px`);
    select.style.setProperty('--picker-top', opensBelow ? `${Math.max(top, field.bottom + 4)}px` : 'auto');
    select.style.setProperty('--picker-bottom', opensBelow ? 'auto' : `${innerHeight - Math.min(bottom, field.top - 4)}px`);
    select.style.setProperty('--picker-height', `${Math.min(320, opensBelow ? below : above)}px`);
  }
  // Delegation also covers schedule/prescription fields created while editing.
  for (const event of ['pointerdown', 'keydown']) document.addEventListener(event, action => {
    const select = action.target.closest('select');
    if (select) position(select);
  }, true);
  const sync = () => document.querySelectorAll('select:open').forEach(position);
  new ResizeObserver(sync).observe(phone);
  window.addEventListener('resize', sync);
  window.addEventListener('scroll', sync, true);
  window.visualViewport?.addEventListener('resize', sync);
  window.visualViewport?.addEventListener('scroll', sync);
})();
