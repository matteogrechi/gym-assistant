'use strict';
// GYM-018, GYM-019: pointer/touch sorting with keyboard parity; commit only on drop.
const GymSorting = (() => {
  function mount(container, onMove) {
    const scroller = container.closest('.screen-area');
    const rows = () => [...container.querySelectorAll('[data-sort-item]')];
    let drag = null, frame = null;
    function clearMarks() {
      rows().forEach(row => { row.classList.remove('is-dragging'); delete row.dataset.drop; });
    }
    function end(commit) {
      const previous = drag;
      drag = null;
      cancelAnimationFrame(frame);
      clearMarks();
      if (!previous) return;
      if (previous.handle.hasPointerCapture(previous.pointerId)) previous.handle.releasePointerCapture(previous.pointerId);
      if (commit && previous.active) onMove(previous.id, previous.target);
    }
    function position() {
      if (!drag?.active) return;
      if (!drag.row.isConnected) { end(false); return; }
      const others = rows().filter(row => row !== drag.row);
      let target = others.findIndex(row => { const bounds = row.getBoundingClientRect(); return drag.y < bounds.top + bounds.height / 2; });
      if (target < 0) target = others.length;
      others.forEach(row => { delete row.dataset.drop; });
      if (others[target]) others[target].dataset.drop = 'before';
      else if (others.length) others.at(-1).dataset.drop = 'after';
      drag.target = target;
    }
    function scroll() {
      if (!drag?.active) return;
      const bounds = scroller.getBoundingClientRect();
      const speed = drag.y < bounds.top + 48 ? -8 : drag.y > bounds.bottom - 48 ? 8 : 0;
      if (speed) { scroller.scrollTop += speed; position(); }
      frame = requestAnimationFrame(scroll);
    }
    container.addEventListener('pointerdown', event => {
      const handle = event.target.closest('[data-sort-handle]');
      if (!handle || handle.disabled || event.button !== 0 || !event.isPrimary || drag) return;
      const row = handle.closest('[data-sort-item]');
      event.preventDefault();
      handle.focus({ preventScroll: true });
      handle.setPointerCapture(event.pointerId);
      drag = { handle, row, id: row.dataset.sortItem, pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, y: event.clientY, target: rows().indexOf(row), active: false };
    });
    container.addEventListener('pointermove', event => {
      if (!drag || event.pointerId !== drag.pointerId) return;
      drag.y = event.clientY;
      if (!drag.active && Math.hypot(event.clientX - drag.startX, event.clientY - drag.startY) >= 6) {
        drag.active = true;
        drag.row.classList.add('is-dragging');
        frame = requestAnimationFrame(scroll);
      }
      position();
    });
    container.addEventListener('pointerup', event => { if (drag?.pointerId === event.pointerId) end(true); });
    container.addEventListener('pointercancel', event => { if (drag?.pointerId === event.pointerId) end(false); });
    container.addEventListener('lostpointercapture', event => { if (drag?.pointerId === event.pointerId) end(false); });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && drag) { event.preventDefault(); end(false); }
    });
    container.addEventListener('keydown', event => {
      const handle = event.target.closest('[data-sort-handle]');
      if (!handle || handle.disabled || !['ArrowUp', 'ArrowDown'].includes(event.key) || drag) return;
      event.preventDefault();
      const row = handle.closest('[data-sort-item]');
      const next = rows().indexOf(row) + (event.key === 'ArrowUp' ? -1 : 1);
      if (next >= 0 && next < rows().length) onMove(row.dataset.sortItem, next);
    });
  }
  return { mount };
})();
