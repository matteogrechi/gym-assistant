'use strict';
/* global GymData, GymMetrics, GymTimers, GymCalendar, GymPlan, GymAttendance, GymSession, GymSettings */
const byId = id => document.getElementById(id);
// GYM-015: uniform decorative SVGs; the surrounding button provides its action name.
const buttonIcon = name => {
  const paths = {"back": "M19 12H5m7-7-7 7 7 7", "left": "m15 18-6-6 6-6", "right": "m9 6 6 6-6 6", "up": "M12 19V5m-7 7 7-7 7 7", "down": "M12 5v14m-7-7 7 7 7-7", "check": "m5 12 4 4L19 6", "add": "M12 5v14M5 12h14", "trash": "M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7", "play": "m8 5 11 7-11 7Z"};
  return `<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="${paths[name]}"/></svg>`;
};
let exerciseIndex = 0;
let metric = 'reps';
let todayDate = GymCalendar.today();
let selectedDate = todayDate;
let displayedWeek = GymCalendar.week(todayDate)[0];
let sessionDate = null;
const sessionInstances = new Map();
// GYM-020: past obligations are snapshots; plan saves cannot rewrite attendance.
const pastPlans = GymAttendance.planned(GymData.program, todayDate, true);
let capturedThrough = todayDate;
const observedAttendance = () => GymAttendance.observations(GymData.attendanceHistory, sessionInstances);
function capturePastPlans() {
  const current = GymAttendance.planned(GymData.program, todayDate, true);
  current.filter(item => item.plannedDate > capturedThrough).forEach(item => pastPlans.push(item));
  // Today remains editable until it has a recorded execution; once past, freeze it.
  if (!recordedForDate(todayDate)) {
    const index = pastPlans.findIndex(item => item.plannedDate === todayDate);
    if (index >= 0) pastPlans.splice(index, 1);
    current.filter(item => item.plannedDate === todayDate).forEach(item => pastPlans.push(item));
  }
  capturedThrough = todayDate;
}
// GYM-018: opening new previews copies the plan; recorded executions retain snapshots.
function getSession(templateId, date) {
  const key = `${date}:${templateId}`;
  const previous = sessionInstances.get(key);
  const past = date < todayDate && pastPlans.find(item => item.plannedDate === date && item.templateId === templateId);
  const definition = past ? { name: past.programName, revision: past.planRevision, sessions: [past.template] } : GymData.program;
  const instance = GymPlan.openOccurrence(definition, GymData.catalog, date, templateId, previous);
  if (instance !== previous) instance.exercises.forEach(entry => { entry.timers = GymTimers.create(); entry.warmups = []; });
  sessionInstances.set(key, instance);
  return instance;
}
let currentSession = null;
const session = () => currentSession;
const exercise = () => session().exercises[exerciseIndex];
const completed = item => item.sets.filter(set => set.done).length;
const format = value => new Intl.NumberFormat('en-GB', { maximumFractionDigits: 1 }).format(value);
const escapeHtml = value => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
const recordedForDate = date => [...sessionInstances.values()].find(item => item.date === date && GymPlan.locked(item));
const plannedForDate = date => {
  const recorded = recordedForDate(date);
  const past = date < todayDate && pastPlans.find(item => item.plannedDate === date);
  const sessionId = recorded?.templateId ?? past?.templateId ?? GymCalendar.plan(date, GymData.program.schedule, GymData.program.dateAssignments);
  return { sessionId, template: recorded ?? past?.template ?? GymData.program.sessions.find(item => item.id === sessionId) ?? null };
};
const executionAssignments = () => {
  const assignments = { ...GymData.program.dateAssignments };
  sessionInstances.forEach(item => { if (GymPlan.locked(item)) assignments[item.date] = item.templateId; });
  return assignments;
};
const fullDate = date => GymCalendar.label(date, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
// GYM-006, GYM-008, GYM-010, GYM-015: view switching and keyboard focus.
function showView(id, focusId) {
  document.querySelectorAll('[data-view]').forEach(view => { view.hidden = view.id !== id; });
  ['programNav', 'planNav', 'progressNav'].forEach(navId => byId(navId).removeAttribute('aria-current'));
  if (id !== 'settings') byId(id === 'progress' ? 'progressNav' : id === 'plan' || id === 'planSession' ? 'planNav' : 'programNav').setAttribute('aria-current', 'page');
  if (focusId) byId(focusId).focus();
  document.querySelector('.screen-area').scrollTop = 0;
}
// GYM-008, GYM-016: selected-date banner, fitted week, and upcoming agenda.
function renderProgram() {
  todayDate = GymCalendar.today();
  capturePastPlans();
  byId('trainProgramName').textContent = GymData.program.name;
  renderCalendar();
}
function renderCalendar() {
  const selected = plannedForDate(selectedDate);
  const planned = selected.template;
  const dateLabel = `${selectedDate === todayDate ? 'TODAY · ' : ''}${fullDate(selectedDate)}`;
  byId('sessionCard').innerHTML = `<p id="selectedDateLabel" class="eyebrow" aria-live="polite">${dateLabel}</p><h3>${escapeHtml(planned?.name ?? 'No session planned')}</h3><p>${escapeHtml(planned ? planned.focus || 'Training session' : 'Assign a session in the Plan section.')}</p>${planned ? `<button id="openSelectedSession" class="md-button md-filled calendar-start">${selectedDate > todayDate ? 'Preview session' : 'View session'}</button>` : ''}`;
  const dates = GymCalendar.week(displayedWeek);
  byId('weekLabel').textContent = `${GymCalendar.label(dates[0], { day: 'numeric', month: 'short' })} – ${GymCalendar.label(dates[6], { day: 'numeric', month: 'short', year: 'numeric' })}`;
  byId('calendarDays').innerHTML = dates.map(date => {
    const item = plannedForDate(date).template;
    return `<button class="calendar-day ${date === todayDate ? 'is-today' : ''}" data-date="${date}" aria-label="${fullDate(date)}${date === todayDate ? ', today' : ''}: ${escapeHtml(item?.name ?? 'No session planned')}" aria-pressed="${date === selectedDate}"><span>${date === todayDate ? 'Today' : GymCalendar.label(date, { weekday: 'short' })}</span><strong>${GymCalendar.label(date, { day: 'numeric' })}</strong><small>${item?.label ?? '—'}</small></button>`;
  }).join('');
  const upcoming = GymCalendar.upcoming(todayDate, GymData.program.schedule, executionAssignments());
  byId('upcomingSessions').innerHTML = upcoming.length ? upcoming.map(item => `<button class="exercise-card upcoming-card" data-planned-date="${item.date}"><span class="agenda-date"><strong>${GymCalendar.label(item.date, { day: 'numeric' })}</strong><small>${GymCalendar.label(item.date, { month: 'short' })}</small></span><span><strong>${escapeHtml(plannedForDate(item.date).template.name)}</strong><small>${GymCalendar.label(item.date, { weekday: 'long' })}</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button>`).join('') : '<p class="empty-calendar">No sessions planned in the next 28 days.</p>';
  // GYM-020: overdue workouts remain available, oldest first; dates do not move.
  const finishedIds = new Set([...sessionInstances.values()].filter(item => item.state === 'finished').map(item => GymAttendance.key(item.date, item.templateId)));
  const pending = GymAttendance.pending(pastPlans, observedAttendance(), todayDate).filter(item => !finishedIds.has(item.id));
  byId('pendingCount').textContent = `${pending.length} not yet logged`;
  byId('pendingSessions').innerHTML = pending.slice(0, 3).map(item => `<button class="exercise-card" data-pending-date="${item.plannedDate}"><span class="agenda-date"><strong>${GymCalendar.label(item.plannedDate, { day: 'numeric' })}</strong><small>${GymCalendar.label(item.plannedDate, { month: 'short' })}</small></span><span><strong>${escapeHtml(item.template.name)}</strong><small>${GymCalendar.label(item.plannedDate, { weekday: 'long' })}</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button>`).join('') || '<p class="empty-calendar">No pending sessions in the last 28 days.</p>';
}
function openSession(templateId, date) {
  sessionDate = date;
  currentSession = getSession(templateId, date);
  renderSession();
  showView('session', 'backProgram');
}
// GYM-001, GYM-005, GYM-008, GYM-019
function renderSession() {
  byId('sessionName').textContent = session().name;
  byId('sessionFocus').textContent = [session().focus, fullDate(sessionDate), sessionDate > todayDate ? 'Planned preview' : ''].filter(Boolean).join(' · ');
  const done = session().exercises.reduce((sum, item) => sum + completed(item), 0);
  const total = session().exercises.reduce((sum, item) => sum + item.sets.length, 0);
  byId('sessionCount').textContent = `${done} / ${total} working sets done`;
  const state = session().state;
  const action = state === 'paused' ? 'Resume' : 'Start';
  byId('startSession').textContent = action;
  byId('startSession').setAttribute('aria-label', `${action} session`);
  byId('startSession').hidden = state !== 'ready' && state !== 'paused';
  byId('pauseSession').hidden = state !== 'inProgress';
  byId('finishSession').hidden = state !== 'inProgress' && state !== 'paused';
  byId('finishSession').className = `md-button ${state === 'paused' ? 'md-outlined' : 'md-filled'}`;
  byId('sessionStatus').textContent = GymSession.labels[state];
  const actual = session().attendedDate ?? observedAttendance().get(GymAttendance.key(sessionDate, session().templateId))?.actualDate;
  byId('attendanceStatus').textContent = actual && actual !== sessionDate ? `Logged ${fullDate(actual)}` : '';
  byId('exerciseList').innerHTML = session().exercises.map((item, index) => `<div class="workout-exercise-row"><button class="exercise-card" data-exercise="${index}"><span class="exercise-number">${String(index + 1).padStart(2, '0')}</span><span><strong>${escapeHtml(item.name)}</strong><small>${item.sets.length} × ${escapeHtml(item.target)} reps${completed(item) ? ` · ${completed(item)}/${item.sets.length} done` : ''}</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button><div class="workout-order-controls"><button class="md-icon-button md-icon-outlined" data-move-exercise="${escapeHtml(item.entryId)}" data-direction="-1" aria-label="Move ${escapeHtml(item.name)} earlier" ${index === 0 ? 'disabled' : ''}>${buttonIcon('up')}</button><button class="md-icon-button md-icon-outlined" data-move-exercise="${escapeHtml(item.entryId)}" data-direction="1" aria-label="Move ${escapeHtml(item.name)} later" ${index === session().exercises.length - 1 ? 'disabled' : ''}>${buttonIcon('down')}</button></div></div>`).join('');
  if (state === 'finished') {
    byId('exerciseList').querySelectorAll('[data-move-exercise]').forEach(button => { button.disabled = true; });
    byId('exerciseList').querySelectorAll('.workout-order-controls').forEach(controls => { controls.hidden = true; });
  }
  byId('executionOrderStatus').textContent = '';
}
function selectExercise(index) {
  exerciseIndex = index;
  renderExercise();
  showView('exercise', 'back');
}
// GYM-004, GYM-005, GYM-009, GYM-014: reps first; warm-ups and working sets share a labeled list.
function setRow(set, index, kind) {
  const label = kind === 'warmups' ? 'Warm-up' : 'Working set';
  const remove = kind === 'warmups' && session().state !== 'finished' ? `<button class="md-icon-button warmup-remove" data-remove="${index}" aria-label="Remove warm-up ${index + 1}">${buttonIcon('trash')}</button>` : '';
  // GYM-025: terminal review exposes a status, not a disabled interactive-looking toggle.
  const completion = session().state === 'finished'
    ? `<span class="set-status ${set.done ? 'is-complete' : ''}" role="img" aria-label="${label} ${index + 1} ${set.done ? 'complete' : 'not logged'}">${set.done ? buttonIcon('check') : '<span aria-hidden="true">—</span>'}</span>`
    : `<button class="md-icon-button md-toggle" aria-label="${label} ${index + 1} complete" aria-pressed="${set.done}" data-kind="${kind}" data-toggle="${index}">${buttonIcon('check')}</button>`;
  return `<div class="set-row ${kind === 'warmups' ? 'warmup-row' : ''} ${set.done ? 'complete' : ''}"><span class="set-kind"><small>${kind === 'warmups' ? 'WARM-UP' : 'WORKING'}</small><b>${index + 1}</b>${remove}</span><input aria-label="${label} ${index + 1} repetitions" type="number" min="0" step="1" required value="${set.reps}" data-kind="${kind}" data-set="${index}" data-field="reps"><input aria-label="${label} ${index + 1} weight in kilograms" type="number" min="0" step="any" required value="${set.weight}" data-kind="${kind}" data-set="${index}" data-field="weight">${completion}</div>`;
}
function renderSets() {
  byId('setCount').textContent = `${completed(exercise())} of ${exercise().sets.length} complete`;
  byId('sets').innerHTML = exercise().warmups.map((set, index) => setRow(set, index, 'warmups')).join('') + exercise().sets.map((set, index) => setRow(set, index, 'sets')).join('');
  if (session().state === 'finished') byId('sets').querySelectorAll('input,button').forEach(control => { control.disabled = true; });
}
// GYM-001, GYM-003, GYM-008, GYM-012
function renderExercise() {
  byId('exercise').classList.toggle('finished-review', session().state === 'finished');
  byId('exerciseName').textContent = exercise().name;
  byId('prescription').textContent = `Target: ${exercise().sets.length} sets × ${exercise().target} reps`;
  byId('back').innerHTML = buttonIcon('back');
  byId('back').setAttribute('aria-label', `Back to ${session().name}`);
  byId('exerciseSessionStatus').textContent = GymSession.labels[session().state];
  byId('exerciseSessionStatus').hidden = !['paused', 'finished'].includes(session().state);
  byId('pauseExerciseSession').hidden = session().state !== 'inProgress';
  byId('finishExerciseSession').hidden = !['inProgress', 'paused'].includes(session().state);
  byId('finishExerciseSession').className = `md-button ${session().state === 'paused' ? 'md-outlined' : 'md-filled'}`;
  byId('seatInput').value = exercise().seat;
  byId('seatInput').disabled = session().state === 'finished';
  byId('addWarmup').disabled = session().state === 'finished';
  byId('addWarmup').hidden = session().state === 'finished';
  renderSets();
  renderTimer();
}
// GYM-004, GYM-014: reject invalid numbers; retain last valid value.
function updateSet(input) {
  if (session().state === 'finished') return;
  const set = exercise()[input.dataset.kind][Number(input.dataset.set)];
  if (!input.checkValidity() || !Number.isFinite(Number(input.value))) {
    input.reportValidity();
    input.value = set[input.dataset.field];
    return;
  }
  set[input.dataset.field] = Number(input.value);
  session().hasEntries = true;
  if (input.dataset.kind === 'sets' && GymAttendance.validSet(set)) GymAttendance.observe(session(), GymCalendar.today());
}
// GYM-005, GYM-014
function toggleSet(index, kind) {
  if (session().state === 'finished') return;
  const set = exercise()[kind][index];
  set.done = !set.done;
  session().hasEntries = true;
  if (kind === 'sets' && GymAttendance.validSet(set)) GymAttendance.observe(session(), GymCalendar.today());
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
  renderAttendance();
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
  byId('metricSubtitle').textContent = labels.subtitle;
  const max = Math.max(1, ...series.map(point => point.value ?? 0));
  const dateLabel = date => new Date(`${date}T12:00:00Z`).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' });
  byId('chart').innerHTML = series.map(point => `<div class="chart-column"><span class="chart-number">${point.value === null ? '—' : format(point.value)}</span><div class="bar-track"><div class="chart-bar" style="height:${point.value === null ? 0 : point.value / max * 100}%"></div></div><span class="chart-date">${dateLabel(point.date)}</span></div>`).join('');
  byId('chart').setAttribute('aria-label', `${labels.title}. ${series.map(point => `${dateLabel(point.date)}: ${point.value === null ? 'No comparable set' : `${format(point.value)} ${labels.unit}`}`).join('; ')}. Baseline zero.`);
  byId('workingSets').textContent = GymMetrics.countSets(GymData.history, context);
  byId('comparisonContext').textContent = `${selected.machine} · ${selected.seat}. Sample history uses the same movement and 2 reps in reserve (estimated reps left). Warm-ups excluded.`;
  byId('metricExplanation').textContent = labels.explanation;
  document.querySelectorAll('[data-metric]').forEach(button => button.setAttribute('aria-pressed', button.dataset.metric === metric));
}
// GYM-020: attendance describes recorded visits, never a strength penalty.
function renderAttendance() {
  const today = GymCalendar.today();
  todayDate = today;
  capturePastPlans();
  const stats = GymAttendance.summarize(pastPlans, observedAttendance(), today);
  byId('attendanceWindow').textContent = `${GymCalendar.label(GymCalendar.addDays(today, -28), { day: 'numeric', month: 'short' })}–${GymCalendar.label(GymCalendar.addDays(today, -1), { day: 'numeric', month: 'short' })}`;
  byId('attendanceAsOf').textContent = `Planned period above; attendance updated through ${fullDate(today)}.`;
  byId('attendanceTotal').textContent = `${stats.logged} / ${stats.planned}`;
  byId('attendancePercent').textContent = stats.percent === null ? 'N/A' : `${Math.round(stats.percent)}%`;
  byId('attendanceOnTime').textContent = stats.onTime;
  byId('attendanceLate').textContent = stats.late;
  byId('attendanceEarly').textContent = stats.early;
  byId('attendanceUnlogged').textContent = stats.unlogged;
  byId('attendanceDelay').textContent = stats.meanDelay === null ? 'N/A' : `${format(stats.meanDelay)} days`;
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
  byId('timerStatus').className = mode === 'recovery' && seconds === 0 ? 'field-help' : 'sr-only';
  byId('timerToggle').disabled = ['paused', 'finished'].includes(session().state);
  byId('timerReset').disabled = session().state === 'finished';
  byId('timerToggle').hidden = session().state === 'finished';
  byId('timerReset').hidden = session().state === 'finished';
  byId('timerToggle').textContent = running ? 'Pause' : state[mode].elapsed > 0 && !(mode === 'recovery' && seconds === 0) ? 'Resume' : 'Start';
  byId('recoverySettings').hidden = mode !== 'recovery';
  byId('recoverySeconds').disabled = state.recovery.startedAt !== null || session().state === 'finished';
  if (document.activeElement !== byId('recoverySeconds')) byId('recoverySeconds').value = state.duration;
  ['execution', 'recovery'].forEach(item => byId(`${item}Mode`).setAttribute('aria-pressed', mode === item));
  const other = mode === 'execution' ? 'recovery' : 'execution';
  byId('timerContext').textContent = session().state === 'paused' ? 'Resume the session to run timers.' : state[other].startedAt !== null ? `${other === 'execution' ? 'Execution' : 'Recovery'} is also running.` : '';
}
byId('exerciseList').addEventListener('click', event => {
  // GYM-019: only execution order changes; keep the selected object's identity.
  const move = event.target.closest('[data-move-exercise]');
  if (move) {
    if (session().state === 'finished') return;
    const selected = exercise();
    const entryId = move.dataset.moveExercise;
    const direction = Number(move.dataset.direction);
    const next = GymPlan.moveExercise(session(), entryId, direction);
    if (next < 0) return;
    exerciseIndex = Math.max(0, session().exercises.indexOf(selected));
    renderSession();
    byId('executionOrderStatus').textContent = `${session().exercises[next].name} moved to position ${next + 1}.`;
    const row = byId('exerciseList').children[next];
    const button = row.querySelector(`[data-direction="${direction}"]`);
    (button.disabled ? row.querySelector('[data-exercise]') : button).focus();
    return;
  }
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
  byId('calendarDays').querySelector(`[data-date="${selectedDate}"]`).focus();
});
byId('sessionCard').addEventListener('click', event => {
  if (!event.target.closest('#openSelectedSession')) return;
  const item = plannedForDate(selectedDate);
  if (item.template) openSession(item.sessionId, selectedDate);
});
byId('upcomingSessions').addEventListener('click', event => {
  const card = event.target.closest('[data-planned-date]');
  if (!card) return;
  selectedDate = card.dataset.plannedDate;
  displayedWeek = GymCalendar.week(selectedDate)[0];
  renderCalendar();
  if (byId('openSelectedSession')) byId('openSelectedSession').focus();
  document.querySelector('.screen-area').scrollTop = 0;
});
byId('pendingSessions').addEventListener('click', event => {
  const card = event.target.closest('[data-pending-date]');
  if (!card) return;
  selectedDate = card.dataset.pendingDate;
  displayedWeek = GymCalendar.week(selectedDate)[0];
  renderCalendar();
  byId('openSelectedSession').focus();
  document.querySelector('.screen-area').scrollTop = 0;
});
['previousWeek', 'nextWeek'].forEach(id => byId(id).addEventListener('click', () => {
  displayedWeek = GymCalendar.addDays(displayedWeek, id === 'previousWeek' ? -7 : 7);
  selectedDate = displayedWeek;
  renderCalendar();
}));
byId('calendarToday').addEventListener('click', () => {
  todayDate = GymCalendar.today(); selectedDate = todayDate; displayedWeek = GymCalendar.week(todayDate)[0]; renderCalendar();
});
// GYM-017: Start/Resume select unfinished work; exercise selection is navigation.
byId('startSession').addEventListener('click', () => {
  const action = session().state === 'paused' ? 'resume' : 'start';
  if (!GymSession.transition(session(), action, Date.now())) return;
  const next = session().exercises.findIndex(item => item.sets.some(set => !set.done));
  selectExercise(next < 0 ? 0 : next);
});
// GYM-017, GYM-012: Pause/Finish pause all occurrence timers, preserving entries.
function pauseSession() {
  if (!GymSession.transition(session(), 'pause', Date.now())) return;
  renderSession();
  showView('session', 'startSession');
}
byId('pauseSession').addEventListener('click', pauseSession);
byId('pauseExerciseSession').addEventListener('click', pauseSession);
function finishSession() {
  if (!GymSession.transition(session(), 'finish', Date.now())) return;
  renderSession();
  showView('session', 'backProgram');
}
byId('finishSession').addEventListener('click', finishSession);
byId('finishExerciseSession').addEventListener('click', finishSession);
// GYM-018: a single editable draft owns templates and scheduling, separate from Train.
let planDraft = null;
let draftDate = selectedDate;
let editingTemplateId = null;
let planDirty = false;
let planMessage = '';
const ensureDraft = () => { if (!planDraft) planDraft = GymPlan.clone(GymData.program); };
const templateDraft = () => planDraft.sessions.find(item => item.id === editingTemplateId);
const assignmentOptions = first => `<option value="${first}">${first === 'weekly' ? 'Use weekly pattern' : 'No session'}</option>${first === 'weekly' ? '<option value="none">No session</option>' : ''}${planDraft.sessions.map(item => `<option value="${item.id}">${escapeHtml(item.name)} (${item.label})</option>`).join('')}`;
// GYM-025: Save indicates a pending change instead of offering a no-op.
function syncSaveActions() {
  document.querySelectorAll('.save-plan').forEach(button => { button.disabled = !planDirty; });
}
function markPlanDirty() {
  planDirty = true;
  planMessage = 'Unsaved changes';
  syncSaveActions();
  byId('planFeedback').textContent = planMessage;
  byId('templateFeedback').textContent = planMessage;
}
function renderDateAssignment() {
  byId('planDateSession').innerHTML = assignmentOptions('weekly');
  const override = Object.prototype.hasOwnProperty.call(planDraft.dateAssignments, draftDate);
  byId('planDateSession').value = override ? planDraft.dateAssignments[draftDate] ?? 'none' : 'weekly';
}
function renderPlan() {
  ensureDraft();
  syncSaveActions();
  const today = GymCalendar.today();
  draftDate = draftDate < today ? today : draftDate;
  byId('planDate').min = today;
  byId('planName').value = planDraft.name;
  byId('planSessions').innerHTML = planDraft.sessions.map(item => `<div class="plan-session-row"><button type="button" class="exercise-card" data-edit-template="${item.id}"><span class="exercise-number">${item.label}</span><span><strong>${escapeHtml(item.name)}</strong><small>${item.exercises.length} exercises</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button><button class="md-icon-button delete-template" type="button" data-delete-template="${item.id}" aria-label="Remove ${escapeHtml(item.name)}" ${planDraft.sessions.length === 1 ? 'disabled' : ''}>${buttonIcon('trash')}</button></div>`).join('');
  byId('addPlanSession').disabled = planDraft.sessions.length >= 7;
  const days = [[1, 'Monday'], [2, 'Tuesday'], [3, 'Wednesday'], [4, 'Thursday'], [5, 'Friday'], [6, 'Saturday'], [0, 'Sunday']];
  byId('weeklySchedule').innerHTML = days.map(([day, name]) => `<div class="schedule-row"><label for="weekday-${day}">${name}</label><select id="weekday-${day}" data-weekday="${day}">${assignmentOptions('none')}</select></div>`).join('');
  days.forEach(([day]) => { byId(`weekday-${day}`).value = planDraft.schedule[day] ?? 'none'; });
  byId('planDate').value = draftDate;
  renderDateAssignment();
  byId('planFeedback').textContent = planMessage;
}
// GYM-021: summaries retain targets while occasional editing stays on demand.
function prescriptionSummary(item, index) {
  const name = GymData.catalog.find(machine => machine.id === item.exerciseId)?.name ?? 'Choose exercise';
  const reps = item.minReps === item.maxReps ? item.minReps : `${item.minReps}–${item.maxReps}`;
  return `${index + 1}. ${escapeHtml(name)} · ${item.setCount} × ${reps}`;
}
function renderTemplate() {
  syncSaveActions();
  const template = templateDraft();
  byId('templateHeading').textContent = 'Edit session';
  byId('templateName').value = template.name;
  byId('templateFocus').value = template.focus;
  byId('plannedExercises').innerHTML = template.exercises.length ? template.exercises.map((item, index) => `<details class="prescription-card" ${index === 0 ? 'open' : ''}><summary><strong class="prescription-summary">${prescriptionSummary(item, index)}</strong></summary><label class="select-label" for="planned-machine-${item.entryId}">Exercise</label><select id="planned-machine-${item.entryId}" data-entry="${item.entryId}" data-plan-field="exerciseId">${GymData.catalog.map(machine => `<option value="${machine.id}" ${machine.id === item.exerciseId ? 'selected' : ''}>${machine.name}</option>`).join('')}</select><div class="prescription-fields"><label>Working sets<input type="number" min="1" max="10" step="1" required value="${item.setCount}" data-entry="${item.entryId}" data-plan-field="setCount" aria-label="Exercise ${index + 1} planned working sets"></label><label>Min reps<input type="number" min="1" max="50" step="1" required value="${item.minReps}" data-entry="${item.entryId}" data-plan-field="minReps" aria-label="Exercise ${index + 1} minimum target reps"></label><label>Max reps<input type="number" min="1" max="50" step="1" required value="${item.maxReps}" data-entry="${item.entryId}" data-plan-field="maxReps" aria-label="Exercise ${index + 1} maximum target reps"></label></div><div class="prescription-actions"><button class="md-icon-button md-icon-outlined" type="button" data-move-entry="${item.entryId}" data-direction="-1" aria-label="Move exercise ${index + 1} up" ${index === 0 ? 'disabled' : ''}>${buttonIcon('up')}</button><button class="md-icon-button md-icon-outlined" type="button" data-move-entry="${item.entryId}" data-direction="1" aria-label="Move exercise ${index + 1} down" ${index === template.exercises.length - 1 ? 'disabled' : ''}>${buttonIcon('down')}</button><button class="md-icon-button" type="button" data-remove-entry="${item.entryId}" aria-label="Remove exercise ${index + 1}">${buttonIcon('trash')}</button></div></details>`).join('') : '<p class="field-help">Add an exercise to this session.</p>';
  byId('exerciseToAdd').innerHTML = GymData.catalog.map(item => `<option value="${item.id}">${item.name}</option>`).join('');
  byId('templateFeedback').textContent = planMessage;
}
function savePlan(event) {
  event.preventDefault();
  const errors = GymPlan.validate(planDraft, GymData.catalog);
  if (errors.length) {
    planMessage = errors.join(' ');
    byId('planFeedback').textContent = planMessage;
    byId('templateFeedback').textContent = planMessage;
    return;
  }
  if (planDirty) GymPlan.apply(GymData.program, planDraft, GymData.catalog);
  planDraft = GymPlan.clone(GymData.program);
  planDirty = false;
  planMessage = 'Plan saved';
  renderPlan();
  renderProgram();
  showView('plan', 'planNav');
}
byId('planNav').addEventListener('click', () => {
  ensureDraft(); draftDate = selectedDate; renderPlan(); showView('plan', 'planNav');
});
byId('planName').addEventListener('input', event => { planDraft.name = event.target.value; markPlanDirty(); });
byId('weeklySchedule').addEventListener('change', event => {
  if (!event.target.matches('[data-weekday]')) return;
  planDraft.schedule[event.target.dataset.weekday] = event.target.value === 'none' ? null : event.target.value;
  markPlanDirty();
});
byId('planDate').addEventListener('change', event => {
  // GYM-020: a draft must not remove past obligations from attendance metrics.
  if (!event.target.checkValidity()) { event.target.reportValidity(); event.target.value = draftDate; return; }
  draftDate = event.target.value;
  renderDateAssignment();
});
byId('planDateSession').addEventListener('change', event => {
  if (!byId('planDate').checkValidity()) { byId('planDate').reportValidity(); return; }
  if (event.target.value === 'weekly') delete planDraft.dateAssignments[draftDate];
  else planDraft.dateAssignments[draftDate] = event.target.value === 'none' ? null : event.target.value;
  markPlanDirty();
});
byId('planSessions').addEventListener('click', event => {
  const edit = event.target.closest('[data-edit-template]');
  if (edit) { editingTemplateId = edit.dataset.editTemplate; renderTemplate(); showView('planSession', 'backToPlan'); }
  const remove = event.target.closest('[data-delete-template]');
  if (remove && GymPlan.removeSession(planDraft, remove.dataset.deleteTemplate)) { markPlanDirty(); renderPlan(); }
});
byId('addPlanSession').addEventListener('click', () => {
  if (planDraft.sessions.length >= 7) return;
  const label = [...'ABCDEFG'].find(value => !planDraft.sessions.some(item => item.label === value));
  const id = `session-${crypto.randomUUID()}`;
  planDraft.sessions.push({ id, label, name: `Session ${label}`, focus: '', exercises: [{ entryId: crypto.randomUUID(), exerciseId: GymData.catalog[0].id, setCount: 3, minReps: 10, maxReps: 12 }] });
  editingTemplateId = id;
  markPlanDirty(); renderTemplate(); showView('planSession', 'backToPlan');
});
byId('backToPlan').addEventListener('click', () => { renderPlan(); showView('plan', 'planNav'); });
byId('templateName').addEventListener('input', event => { templateDraft().name = event.target.value; markPlanDirty(); });
byId('templateFocus').addEventListener('input', event => { templateDraft().focus = event.target.value; markPlanDirty(); });
byId('plannedExercises').addEventListener('input', event => {
  const field = event.target.dataset.planField;
  if (!field) return;
  const item = templateDraft().exercises.find(entry => entry.entryId === event.target.dataset.entry);
  item[field] = field === 'exerciseId' ? event.target.value : Number(event.target.value);
  event.target.closest('.prescription-card').querySelector('.prescription-summary').innerHTML = prescriptionSummary(item, templateDraft().exercises.indexOf(item));
  markPlanDirty();
});
byId('plannedExercises').addEventListener('click', event => {
  const remove = event.target.closest('[data-remove-entry]');
  if (remove) { templateDraft().exercises = templateDraft().exercises.filter(item => item.entryId !== remove.dataset.removeEntry); markPlanDirty(); renderTemplate(); return; }
  const move = event.target.closest('[data-move-entry]');
  if (!move) return;
  const list = templateDraft().exercises;
  const index = list.findIndex(item => item.entryId === move.dataset.moveEntry);
  const next = index + Number(move.dataset.direction);
  if (next >= 0 && next < list.length) { [list[index], list[next]] = [list[next], list[index]]; markPlanDirty(); renderTemplate(); }
});
byId('addPlanExercise').addEventListener('click', () => {
  templateDraft().exercises.push({ entryId: crypto.randomUUID(), exerciseId: byId('exerciseToAdd').value, setCount: 3, minReps: 10, maxReps: 12 });
  markPlanDirty(); renderTemplate();
});
byId('planForm').addEventListener('submit', savePlan);
byId('templateForm').addEventListener('submit', savePlan);
// GYM-021: reveal a collapsed invalid field before the browser moves focus to it.
['planForm', 'templateForm'].forEach(id => byId(id).addEventListener('invalid', event => {
  const details = event.target.closest('details');
  if (details) details.open = true;
}, true));

// GYM-010
byId('progressNav').addEventListener('click', () => { renderProgress(); showView('progress', 'progressNav'); });
byId('progressExercise').innerHTML = GymData.catalog.map((item, index) => `<option value="${index}">${item.name}</option>`).join('');
byId('progressExercise').addEventListener('change', renderProgress);
document.querySelectorAll('[data-metric]').forEach(button => button.addEventListener('click', () => { metric = button.dataset.metric; renderProgress(); }));
// GYM-003
byId('seatInput').addEventListener('input', event => { if (session().state !== 'finished') { exercise().seat = event.target.value; session().hasEntries = true; } });
// GYM-004, GYM-005, GYM-014
['sets'].forEach(id => {
  byId(id).addEventListener('change', event => { if (event.target.matches('input')) updateSet(event.target); });
  byId(id).addEventListener('click', event => {
    if (session().state === 'finished') return;
    const button = event.target.closest('[data-toggle]');
    if (button) toggleSet(Number(button.dataset.toggle), button.dataset.kind);
    const remove = event.target.closest('[data-remove]');
    if (remove) { exercise().warmups.splice(Number(remove.dataset.remove), 1); session().hasEntries = true; renderSets(); byId('addWarmup').focus(); }
  });
});
byId('addWarmup').addEventListener('click', () => {
  if (session().state === 'finished') return;
  exercise().warmups.push({ reps: 0, weight: 0, done: false });
  session().hasEntries = true;
  renderSets();
  byId('sets').querySelector(`[data-kind="warmups"][data-set="${exercise().warmups.length - 1}"][data-field="reps"]`).focus();
});
// GYM-012
['execution', 'recovery'].forEach(mode => byId(`${mode}Mode`).addEventListener('click', () => { exercise().timers.mode = mode; renderTimer(); }));
byId('timerToggle').addEventListener('click', () => {
  if (['paused', 'finished'].includes(session().state)) return;
  session().hasEntries = true;
  const state = exercise().timers;
  if (state[state.mode].startedAt === null) GymTimers.start(state, state.mode, Date.now());
  else GymTimers.pause(state, state.mode, Date.now());
  renderTimer();
});
byId('timerReset').addEventListener('click', () => { if (session().state === 'finished') return; session().hasEntries = true; GymTimers.reset(exercise().timers, exercise().timers.mode); renderTimer(); });
byId('recoverySeconds').addEventListener('change', event => {
  if (session().state === 'finished') return;
  if (!GymTimers.setDuration(exercise().timers, Number(event.target.value))) { event.target.reportValidity(); event.target.value = exercise().timers.duration; }
  else session().hasEntries = true;
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

// GYM-022–024: all exports use current state, not the selected chart or a saved demo fixture.
GymSettings.mount({
  show: showView,
  input: () => {
    todayDate = GymCalendar.today();
    capturePastPlans();
    return { settings: GymSettings.state, program: GymData.program, draft: planDirty ? planDraft : null,
      catalog: GymData.catalog, sessions: sessionInstances, pastPlans,
      history: GymData.history, attendanceHistory: GymData.attendanceHistory,
      context: GymData.context, today: todayDate, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone };
  }
});
