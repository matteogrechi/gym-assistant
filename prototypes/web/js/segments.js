// GYM-015: shared text-only, single-choice segmented selector.
const GymSegments = (() => {
  function mount(container, { label, options, value, onChange }) {
    container.classList.add('segmented-selector');
    container.setAttribute('role', 'group');
    container.setAttribute('aria-label', label);

    const buttons = options.map(option => {
      const button = document.createElement('button');
      Object.entries(option.attributes || {}).forEach(([name, attribute]) => {
        button.setAttribute(name, attribute);
      });
      button.type = 'button';
      button.className = 'md-segment';
      button.textContent = option.label;
      button.dataset.segmentValue = option.value;
      return button;
    });

    function select(next) {
      const selected = buttons.find(button => button.dataset.segmentValue === next)
        || buttons[0];
      buttons.forEach(button => {
        button.setAttribute('aria-pressed', String(button === selected));
      });
    }

    function choose(button) {
      if (button.disabled) return;
      select(button.dataset.segmentValue);
      onChange?.(button.dataset.segmentValue);
    }

    buttons.forEach(button => {
      button.addEventListener('click', () => choose(button));
      button.addEventListener('keydown', event => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        if (button.disabled) return;
        const enabled = buttons.filter(option => !option.disabled);
        if (!enabled.length) return;
        const index = enabled.indexOf(button);
        let next;
        if (event.key === 'Home') next = enabled[0];
        else if (event.key === 'End') next = enabled[enabled.length - 1];
        else {
          const offset = event.key === 'ArrowRight' ? 1 : -1;
          next = enabled[(index + offset + enabled.length) % enabled.length];
        }
        next.focus();
        choose(next);
      });
    });

    container.replaceChildren(...buttons);
    select(value);
    return { select };
  }

  return { mount };
})();
