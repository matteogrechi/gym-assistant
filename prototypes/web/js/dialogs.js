'use strict';
// GYM-006, GYM-015, GYM-025: retain native modal semantics while confining presentation to the phone.
const GymDialogs = (() => {
  const phone = document.querySelector('.phone');
  function position(dialog) {
    const bounds = phone.getBoundingClientRect();
    const style = getComputedStyle(phone);
    const radius = Math.max(0, parseFloat(style.borderTopLeftRadius) - phone.clientLeft);
    const innerTop = bounds.top + phone.clientTop;
    const viewport = window.visualViewport;
    const viewportTop = Number.isFinite(viewport?.offsetTop) ? viewport.offsetTop : 0;
    const fallbackHeight = Number.isFinite(window.innerHeight) ? window.innerHeight : 0;
    const viewportHeight = Math.max(0, Number.isFinite(viewport?.height) ? viewport.height : fallbackHeight);
    const visibleTop = Math.min(phone.clientHeight, Math.max(0, viewportTop - innerTop));
    const visibleBottom = Math.min(phone.clientHeight, Math.max(visibleTop, viewportTop + viewportHeight - innerTop));
    dialog.style.setProperty('--modal-left', `${bounds.left + phone.clientLeft}px`);
    dialog.style.setProperty('--modal-top', `${innerTop}px`);
    dialog.style.setProperty('--modal-width', `${phone.clientWidth}px`);
    dialog.style.setProperty('--modal-height', `${phone.clientHeight}px`);
    dialog.style.setProperty('--modal-visible-top', `${visibleTop}px`);
    dialog.style.setProperty('--modal-visible-height', `${visibleBottom - visibleTop}px`);
    dialog.style.setProperty('--modal-radius', `${radius}px`);
  }
  const sync = () => phone.querySelectorAll('dialog[open]').forEach(position);
  new ResizeObserver(sync).observe(phone);
  window.addEventListener('resize', sync);
  window.addEventListener('scroll', sync, true);
  window.visualViewport?.addEventListener('resize', sync);
  window.visualViewport?.addEventListener('scroll', sync);
  phone.querySelectorAll('dialog').forEach(dialog => {
    // Keep Tab within the app dialog, including the single-button video placeholder.
    dialog.addEventListener('keydown', event => {
      if (event.key !== 'Tab') return;
      const controls = [...dialog.querySelectorAll('button,input,select,textarea,a[href],[tabindex]:not([tabindex="-1"])')].filter(element => !element.disabled && element.getClientRects().length);
      const first = controls[0], last = controls.at(-1);
      if (!first) { event.preventDefault(); dialog.focus(); return; }
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    });
    dialog.addEventListener('click', event => {
      if (event.target === dialog) dialog.close();
    });
  });
  function open(dialog) {
    position(dialog);
    dialog.showModal();
  }
  return { open };
})();
