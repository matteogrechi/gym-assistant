'use strict';
// GYM-015, GYM-025, GYM-026, GYM-028: shared planning library shells and navigation rows (revision 33).
const GymPlanningUI = (() => {
  function createButton(className) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    return button;
  }

  function mount(section, { actionId, actionLabel, tabsId, listId, labelledBy, icon }) {
    const heading = document.createElement('div');
    heading.className = 'page-heading';
    heading.setAttribute('data-req', 'GYM-015 GYM-025');
    const title = document.createElement('h2');
    title.textContent = 'Plan';

    const action = createButton('md-button md-tonal');
    action.id = actionId;
    // Only the trusted existing SVG factory supplies markup; labels stay text.
    action.insertAdjacentHTML('beforeend', icon('add'));
    action.append(document.createTextNode(actionLabel));
    heading.append(title, action);

    const tabs = document.createElement('div');
    tabs.id = tabsId;
    tabs.setAttribute('data-req', 'GYM-015 GYM-028');

    const list = document.createElement('div');
    list.id = listId;
    list.setAttribute('role', 'tabpanel');
    list.setAttribute('aria-labelledby', labelledBy);

    const bar = document.createElement('div');
    bar.className = 'library-bar';
    bar.append(heading, tabs);
    section.replaceChildren(bar, list);
  }

  function renderList(container, entries, { attribute, icon }) {
    const rows = entries.map(entry => {
      const row = document.createElement('div');
      row.className = 'plan-library-row';
      const button = createButton('exercise-card');
      button.setAttribute(attribute, entry.id);

      const content = document.createElement('span');
      const title = document.createElement('strong');
      title.textContent = entry.title;
      const metadata = document.createElement('small');
      metadata.textContent = entry.description;
      content.append(title, metadata);
      if (entry.badge) {
        const badge = document.createElement('span');
        badge.className = 'active-plan-tag';
        badge.textContent = entry.badge;
        content.append(badge);
      }

      const chevron = document.createElement('span');
      chevron.className = 'chevron';
      chevron.setAttribute('aria-hidden', 'true');
      chevron.insertAdjacentHTML('beforeend', icon('right'));
      button.append(content, chevron);
      row.append(button);
      return row;
    });
    container.replaceChildren(...rows);
  }

  return { mount, renderList };
})();
