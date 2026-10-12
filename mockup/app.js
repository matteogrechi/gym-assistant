'use strict';
/* global GymData, GymMetrics, GymTimers, GymCalendar */
const byId = id => document.getElementById(id);
let exerciseIndex = 0;
let metric = 'reps';
let todayDate = GymCalendar.today();
let selectedDate = todayDate;
let displayedWeek = GymCalendar.week(todayDate)[0];
let sessionDate = null;
const dateOverrides = {};
const sessionInstances = new Map();
const createSession = index => {
  const instance = structuredClone(GymData.program.sessions[index]);
  instance.exercises.forEach(entry => { entry.timers = GymTimers.create(); entry.warmups = []; });
  instance.state = 'ready';
  return instance;
};
// GYM-003, GYM-004, GYM-012, GYM-014, GYM-016: isolate each dated session occurrence.
function getSession(index, date) {
  const key = `${date}:${index}`;
  if (!sessionInstances.has(key)) sessionInstances.set(key, createSession(index));
  return sessionInstances.get(key);
}
let currentSession = null;
const session = () => currentSession;
const exercise = () => session().exercises[exerciseIndex];
const completed = item => item.sets.filter(set => set.done).length;
const format = value => new Intl.NumberFormat('en-GB', { maximumFractionDigits: 1 }).format(value);
const plannedIndex = date => GymCalendar.plan(date, GymData.schedule, dateOverrides);
const fullDate = date => GymCalendar.label(date, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
// GYM-006, GYM-008, GYM-010, GYM-015: view switching and keyboard focus.
function showView(id, focusId) {
  document.querySelectorAll('[data-view]').forEach(view => { view.hidden = view.id !== id; });
  ['programNav', 'progressNav'].forEach(navId => byId(navId).removeAttribute('aria-current'));
  byId(id === 'progress' ? 'progressNav' : 'programNav').setAttribute('aria-current', 'page');
  if (focusId) byId(focusId).focus();
  document.querySelector('.screen-area').scrollTop = 0;
}
// GYM-008, GYM-016: today banner, weekly calendar, and upcoming agenda.
function renderProgram() {
  todayDate = GymCalendar.today();
  renderCalendar();
}
function renderCalendar() {
  const todayIndex = plannedIndex(todayDate);
  const todaySession = todayIndex === null ? null : GymData.program.sessions[todayIndex];
  byId('todayCard').innerHTML = `<p class="eyebrow">TODAY · ${fullDate(todayDate)}</p><h3>${todaySession?.name ?? 'No session planned'}</h3><p>${todaySession ? todaySession.focus : 'Your next planned session is listed below.'}</p>${todaySession ? '<button id="openTodaySession" class="calendar-start">View today’s session <span aria-hidden="true">↗</span></button>' : ''}`;
  const dates = GymCalendar.week(displayedWeek);
  byId('weekLabel').textContent = `${GymCalendar.label(dates[0], { day: 'numeric', month: 'short' })} – ${GymCalendar.label(dates[6], { day: 'numeric', month: 'short', year: 'numeric' })}`;
  byId('calendarDays').innerHTML = dates.map(date => {
    const index = plannedIndex(date);
    const name = index === null ? 'No session planned' : GymData.program.sessions[index].name;
    return `<button class="calendar-day ${date === todayDate ? 'is-today' : ''}" data-date="${date}" aria-label="${fullDate(date)}${date === todayDate ? ', today' : ''}: ${name}" aria-pressed="${date === selectedDate}"><span>${date === todayDate ? 'Today' : GymCalendar.label(date, { weekday: 'short' })}</span><strong>${GymCalendar.label(date, { day: 'numeric' })}</strong><small>${index === null ? '—' : String.fromCharCode(65 + index)}</small></button>`;
  }).join('');
  byId('selectedDateLabel').textContent = `${selectedDate === todayDate ? 'TODAY · ' : ''}${fullDate(selectedDate)}`;
  const selectedIndex = plannedIndex(selectedDate);
  byId('plannedSession').value = selectedIndex === null ? 'none' : String(selectedIndex);
  byId('openPlannedSession').hidden = selectedIndex === null;
  byId('openPlannedSession').textContent = selectedDate > todayDate ? 'Preview session' : 'Open session';
  const upcoming = GymCalendar.upcoming(todayDate, GymData.schedule, dateOverrides);
  byId('upcomingSessions').innerHTML = upcoming.length ? upcoming.map(item => `<button class="exercise-card upcoming-card" data-planned-date="${item.date}"><span class="agenda-date"><strong>${GymCalendar.label(item.date, { day: 'numeric' })}</strong><small>${GymCalendar.label(item.date, { month: 'short' })}</small></span><span><strong>${GymData.program.sessions[item.sessionIndex].name}</strong><small>${GymCalendar.label(item.date, { weekday: 'long', day: 'numeric', month: 'short' })}</small></span><span class="chevron" aria-hidden="true">↗</span></button>`).join('') : '<p class="empty-calendar">No sessions planned in the next 28 days.</p>';
}
function openSession(index, date) {
  byId('planningDialog').close();
  sessionDate = date;
  currentSession = getSession(index, date);
  renderSession();
  showView('session', 'backProgram');
}
// GYM-001, GYM-005, GYM-008
function renderSession() {
  byId('sessionName').textContent = session().name;
  byId('sessionFocus').textContent = `${session().focus} · ${fullDate(sessionDate)}${sessionDate > todayDate ? ' · Planned preview' : ''}`;
  const done = session().exercises.reduce((sum, item) => sum + completed(item), 0);
  byId('sessionCount').textContent = `${done} / 9 working sets done`;
  const action = session().state === 'stopped' ? 'Resume session' : session().state === 'active' ? 'Continue session' : 'Start session';
  byId('startSession').innerHTML = `${action} <span aria-hidden="true">↗</span>`;
  byId('stopSession').hidden = session().state !== 'active';
  byId('sessionStatus').textContent = session().state === 'stopped' ? 'Stopped · your entries are kept' : session().state === 'active' ? (done === 9 ? 'All working sets logged · session active' : sessionDate > todayDate ? 'Preview in progress' : 'In progress') : 'Ready to start';
  byId('exerciseList').innerHTML = session().exercises.map((item, index) => `<button class="exercise-card" data-exercise="${index}"><span class="exercise-number">0${index + 1}</span><span><strong>${item.name}</strong><small>3 × ${item.target} reps · ${completed(item)}/3 done</small></span><span class="chevron" aria-hidden="true">↗</span></button>`).join('');
}
function selectExercise(index) {
  exerciseIndex = index;
  renderExercise();
  showView('exercise', 'back');
}
// GYM-004, GYM-005, GYM-009, GYM-014: reps first; warm-ups and working sets share a labeled list.
function setRow(set, index, kind) {
  const label = kind === 'warmups' ? 'Warm-up' : 'Working set';
  return `<div class="set-row ${kind === 'warmups' ? 'warmup-row' : ''} ${set.done ? 'complete' : ''}"><span class="set-kind"><small>${kind === 'warmups' ? 'WARM-UP' : 'WORKING'}</small><b>${index + 1}</b></span><input aria-label="${label} ${index + 1} repetitions" type="number" min="0" step="1" required value="${set.reps}" data-kind="${kind}" data-set="${index}" data-field="reps"><input aria-label="${label} ${index + 1} weight in kilograms" type="number" min="0" step="any" required value="${set.weight}" data-kind="${kind}" data-set="${index}" data-field="weight"><button aria-label="${label} ${index + 1} complete" aria-pressed="${set.done}" data-kind="${kind}" data-toggle="${index}">✓</button></div>${kind === 'warmups' ? `<button class="remove-warmup" data-remove="${index}" aria-label="Remove warm-up ${index + 1}">Remove warm-up ${index + 1}</button>` : ''}`;
}
function renderSets() {
  byId('setCount').textContent = `${completed(exercise())} of 3 complete`;
  byId('sets').innerHTML = exercise().warmups.map((set, index) => setRow(set, index, 'warmups')).join('') + exercise().sets.map((set, index) => setRow(set, index, 'sets')).join('');
}
// GYM-001, GYM-003, GYM-008, GYM-012
function renderExercise() {
  byId('exerciseName').textContent = exercise().name;
  byId('exerciseCategory').textContent = `${session().name.toUpperCase()} · ${exercise().category}`;
  byId('prescription').textContent = `Program target: 3 working sets × ${exercise().target} reps`;
  byId('back').textContent = `← ${session().name}`;
  byId('stopExerciseSession').hidden = session().state !== 'active';
  byId('seatInput').value = exercise().seat;
  renderSets();
  renderTimer();
}
// GYM-004, GYM-014: reject invalid numbers; retain last valid value.
function updateSet(input) {
  const set = exercise()[input.dataset.kind][Number(input.dataset.set)];
  if (!input.checkValidity() || !Number.isFinite(Number(input.value))) {
    input.reportValidity();
    input.value = set[input.dataset.field];
    return;
  }
  set[input.dataset.field] = Number(input.value);
}
// GYM-005, GYM-014
function toggleSet(index, kind) {
  const set = exercise()[kind][index];
  set.done = !set.done;
  renderSets();
  document.querySelector(`[data-kind="${kind}"][data-toggle="${index}"]`).focus();
}
// GYM-002
function openVideo() {
  byId('videoTitle').textContent = exercise().name;
  byId('videoDialog').showModal();
}
// GYM-010: fictional immutable historical observations, never today's editable samples.
function renderProgress() {
  const selected = GymData.catalog[Number(byId('progressExercise').value)];
  const context = GymData.context(selected);
  const series = GymMetrics.series(GymData.history, context, metric, selected.weight);
  const labels = {
    reps: { title: `Reps at ${selected.weight} kg`, unit: 'reps', subtitle: 'Best working set at this weight', explanation: 'More reps at the same weight can show progress. Compare similar effort, setup, and movement.' },
    weight: { title: 'Weight at 10 reps', unit: 'kg', subtitle: 'Heaviest working set of exactly 10 reps', explanation: 'Track weight for the same repetition task. This is a performed set, not a tested maximum.' },
    volume: { title: 'Volume load', unit: 'kg·reps', subtitle: 'Weight × reps, summed over working sets', explanation: 'This describes workload. More volume does not by itself mean more strength or muscle growth.' }
  }[metric];
  const last = series.at(-1);
  byId('metricTitle').textContent = labels.title;
  byId('metricValue').textContent = last?.value == null ? '—' : format(last.value);
  byId('metricUnit').textContent = labels.unit;
  byId('metricSubtitle').textContent = `${labels.subtitle} · latest session`;
  const max = Math.max(1, ...series.map(point => point.value ?? 0));
  const dateLabel = date => new Date(`${date}T12:00:00Z`).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' });
  byId('chart').innerHTML = series.map(point => `<div class="chart-column"><span class="chart-number">${point.value === null ? '—' : format(point.value)}</span><div class="bar-track"><div class="chart-bar" style="height:${point.value === null ? 0 : point.value / max * 100}%"></div></div><span class="chart-date">${dateLabel(point.date)}</span></div>`).join('');
  byId('chart').setAttribute('aria-label', `${labels.title}. ${series.map(point => `${dateLabel(point.date)}: ${point.value === null ? 'No comparable set' : `${format(point.value)} ${labels.unit}`}`).join('; ')}. Baseline zero.`);
  byId('workingSets').textContent = GymMetrics.countSets(GymData.history, context);
  byId('comparisonContext').textContent = `${selected.machine} · ${selected.seat}. Sample history uses the same movement and 2 reps in reserve (estimated reps left). Warm-ups excluded.`;
  byId('metricExplanation').textContent = labels.explanation;
  document.querySelectorAll('[data-metric]').forEach(button => button.setAttribute('aria-pressed', button.dataset.metric === metric));
}
// GYM-012: render from clock time; timer does not depend on interval callback frequency.
function renderTimer() {
  const state = exercise().timers;
  const mode = state.mode;
  const seconds = GymTimers.read(state, mode, Date.now());
  const running = state[mode].startedAt !== null;
  byId('timerValue').textContent = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  byId('timerValue').setAttribute('aria-label', mode === 'execution' ? 'Execution elapsed time' : 'Recovery time remaining');
  const status = mode === 'recovery' && seconds === 0 ? 'Recovery complete · continue when ready' : running ? 'Running' : state[mode].elapsed > 0 ? 'Paused' : 'Ready';
  if (byId('timerStatus').textContent !== status) byId('timerStatus').textContent = status;
  byId('timerToggle').disabled = session().state === 'stopped';
  byId('timerToggle').textContent = running ? 'Pause' : state[mode].elapsed > 0 && !(mode === 'recovery' && seconds === 0) ? 'Resume' : 'Start';
  byId('recoverySettings').hidden = mode !== 'recovery';
  byId('recoverySeconds').disabled = state.recovery.startedAt !== null;
  if (document.activeElement !== byId('recoverySeconds')) byId('recoverySeconds').value = state.duration;
  ['execution', 'recovery'].forEach(item => byId(`${item}Mode`).setAttribute('aria-pressed', mode === item));
  const other = mode === 'execution' ? 'recovery' : 'execution';
  byId('timerContext').textContent = session().state === 'stopped' ? 'Session stopped · resume from the session screen.' : state[other].startedAt !== null ? `Your ${other} timer is also running.` : 'For this exercise · manual controls';
}
byId('exerciseList').addEventListener('click', event => {
  const card = event.target.closest('[data-exercise]');
  if (card) selectExercise(Number(card.dataset.exercise));
});
byId('backProgram').addEventListener('click', () => {
  selectedDate = sessionDate;
  displayedWeek = GymCalendar.week(sessionDate)[0];
  renderProgram();
  showView('program');
  byId('calendarDays').querySelector(`[data-date="${sessionDate}"]`).focus();
});
byId('back').addEventListener('click', () => {
  renderSession();
  showView('session');
  byId('exerciseList').querySelector(`[data-exercise="${exerciseIndex}"]`).focus();
});
byId('programNav').addEventListener('click', () => { renderProgram(); showView('program', 'programNav'); });
// GYM-016: date selection and assignment change only that day's plan.
byId('calendarDays').addEventListener('click', event => {
  const day = event.target.closest('[data-date]');
  if (!day) return;
  selectedDate = day.dataset.date;
  renderCalendar();
  byId('planningDialog').showModal();
  byId('plannedSession').focus();
});
byId('plannedSession').addEventListener('change', event => {
  dateOverrides[selectedDate] = event.target.value === 'none' ? null : Number(event.target.value);
  renderCalendar();
});
byId('openPlannedSession').addEventListener('click', () => { const index = plannedIndex(selectedDate); if (index !== null) openSession(index, selectedDate); });
byId('todayCard').addEventListener('click', event => {
  if (!event.target.closest('#openTodaySession')) return;
  const index = plannedIndex(todayDate);
  if (index !== null) openSession(index, todayDate);
});
byId('upcomingSessions').addEventListener('click', event => {
  const card = event.target.closest('[data-planned-date]');
  if (!card) return;
  selectedDate = card.dataset.plannedDate;
  displayedWeek = GymCalendar.week(selectedDate)[0];
  renderCalendar();
  byId('planningDialog').showModal();
  byId('plannedSession').focus();
});
['previousWeek', 'nextWeek'].forEach(id => byId(id).addEventListener('click', () => {
  displayedWeek = GymCalendar.addDays(displayedWeek, id === 'previousWeek' ? -7 : 7);
  selectedDate = displayedWeek;
  renderCalendar();
}));
byId('calendarToday').addEventListener('click', () => {
  todayDate = GymCalendar.today(); selectedDate = todayDate; displayedWeek = GymCalendar.week(todayDate)[0]; renderCalendar();
});
// GYM-017: start/continue/resume selects unfinished work and preserves recorded data.
byId('startSession').addEventListener('click', () => {
  session().state = 'active';
  const next = session().exercises.findIndex(item => item.sets.some(set => !set.done));
  selectExercise(next < 0 ? 0 : next);
});
// GYM-017, GYM-012: stop pauses every timer in this session occurrence.
function stopSession() {
  if (session().state !== 'active') return;
  const now = Date.now();
  session().exercises.forEach(item => {
    GymTimers.pause(item.timers, 'execution', now);
    GymTimers.pause(item.timers, 'recovery', now);
  });
  session().state = 'stopped';
  renderSession();
  showView('session', 'startSession');
}
byId('stopSession').addEventListener('click', stopSession);
byId('stopExerciseSession').addEventListener('click', stopSession);
// GYM-006, GYM-016: native planning dialog supports Escape and explicit dismissal.
byId('closePlanning').addEventListener('click', () => byId('planningDialog').close());
byId('planningDialog').addEventListener('click', event => {
  const bounds = event.currentTarget.getBoundingClientRect();
  if (event.target === event.currentTarget && (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) event.currentTarget.close();
});
// GYM-010
byId('progressNav').addEventListener('click', () => { renderProgress(); showView('progress', 'progressNav'); });
byId('progressExercise').innerHTML = GymData.catalog.map((item, index) => `<option value="${index}">${item.name}</option>`).join('');
byId('progressExercise').addEventListener('change', renderProgress);
document.querySelectorAll('[data-metric]').forEach(button => button.addEventListener('click', () => { metric = button.dataset.metric; renderProgress(); }));
// GYM-003
byId('seatInput').addEventListener('input', event => { exercise().seat = event.target.value; });
// GYM-004, GYM-005, GYM-014
['sets'].forEach(id => {
  byId(id).addEventListener('change', event => { if (event.target.matches('input')) updateSet(event.target); });
  byId(id).addEventListener('click', event => {
    const button = event.target.closest('[data-toggle]');
    if (button) toggleSet(Number(button.dataset.toggle), button.dataset.kind);
    const remove = event.target.closest('[data-remove]');
    if (remove) { exercise().warmups.splice(Number(remove.dataset.remove), 1); renderSets(); byId('addWarmup').focus(); }
  });
});
byId('addWarmup').addEventListener('click', () => {
  exercise().warmups.push({ reps: 0, weight: 0, done: false });
  renderSets();
  byId('sets').querySelector(`[data-kind="warmups"][data-set="${exercise().warmups.length - 1}"][data-field="reps"]`).focus();
});
// GYM-012
['execution', 'recovery'].forEach(mode => byId(`${mode}Mode`).addEventListener('click', () => { exercise().timers.mode = mode; renderTimer(); }));
byId('timerToggle').addEventListener('click', () => {
  if (session().state === 'stopped') return;
  const state = exercise().timers;
  if (state[state.mode].startedAt === null) GymTimers.start(state, state.mode, Date.now());
  else GymTimers.pause(state, state.mode, Date.now());
  renderTimer();
});
byId('timerReset').addEventListener('click', () => { GymTimers.reset(exercise().timers, exercise().timers.mode); renderTimer(); });
byId('recoverySeconds').addEventListener('change', event => {
  if (!GymTimers.setDuration(exercise().timers, Number(event.target.value))) { event.target.reportValidity(); event.target.value = exercise().timers.duration; }
  renderTimer();
});
setInterval(() => {
  sessionInstances.forEach(item => item.exercises.forEach(entry => GymTimers.read(entry.timers, 'recovery', Date.now())));
  if (!byId('exercise').hidden) renderTimer();
}, 200);
// GYM-002, GYM-006
byId('watchVideo').addEventListener('click', openVideo);
byId('closeVideo').addEventListener('click', () => byId('videoDialog').close());
byId('videoDialog').addEventListener('click', event => {
  const bounds = event.currentTarget.getBoundingClientRect();
  if (event.target === event.currentTarget && (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) event.currentTarget.close();
});
renderProgram();
