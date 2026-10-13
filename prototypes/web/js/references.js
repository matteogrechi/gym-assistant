'use strict';
// GYM-006: development-only references live outside the simulated Android app.
const GymReferences = (() => {
  const phone = document.querySelector('.phone');
  const id = document.getElementById('mockupReferenceId');
  const name = document.getElementById('mockupReferenceName');
  function update() {
    const active = phone.querySelector('dialog[open][data-mockup-ref]')
      ?? phone.querySelector('#overflowMenu:not([hidden])')
      ?? phone.querySelector('[data-view]:not([hidden])');
    id.textContent = active.dataset.mockupRef;
    name.textContent = active.dataset.mockupName;
  }
  // Observe presentation only: references never change logs, timers or settings.
  new MutationObserver(records => {
    if (records.some(record => record.target.matches('[data-mockup-ref]'))) update();
  }).observe(phone, { subtree: true, attributes: true, attributeFilter: ['hidden', 'open'] });
  update();
  return { update };
})();
