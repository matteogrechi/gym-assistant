'use strict';
/* global GymData, GymMetrics, GymTimers, GymCalendar, GymPlan, GymAttendance, GymSession, GymSettings, GymPrograms, GymDialogs, GymSorting, GymCharts */
const byId = id => document.getElementById(id);
// GYM-015: uniform decorative SVGs; the surrounding button provides its action name.
const buttonIcon = name => {
  const paths = {"back": "M19 12H5m7-7-7 7 7 7", "left": "m15 18-6-6 6-6", "right": "m9 6 6 6-6 6", "up": "M12 19V5m-7 7 7-7 7 7", "down": "M12 5v14m-7-7 7 7 7-7", "check": "m5 12 4 4L19 6", "add": "M12 5v14M5 12h14", "trash": "M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7", "play": "m8 5 11 7-11 7Z", "expand": "m6 9 6 6 6-6"};
  return `<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="${paths[name]}"/></svg>`;
};
// GYM-015, GYM-018, GYM-019: shared 48px drag grip, also operable by keyboard.
const sortHandle = name => `<button class="md-icon-button md-icon-outlined drag-handle" type="button" data-sort-handle aria-label="Reorder ${escapeHtml(name)}" aria-describedby="sortingHelp"><svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M9 5h.01M15 5h.01M9 12h.01M15 12h.01M9 19h.01M15 19h.01" stroke-width="3"/></svg></button>`;
let exerciseIndex = 0;
let metric = 'reps';
let progressDate = null;
let progressPoints = [];
let progressExercise = null;
let progressView = { zoom: 1, scrollLeft: 0 };
let progressExploration = null;
let todayDate = GymCalendar.today();
let selectedDate = todayDate;
let displayedWeek = GymCalendar.week(todayDate)[0];
let sessionDate = null;
const sessionInstances = new Map();
// GYM-020, GYM-026: past obligations retain original program identity; plan saves cannot rewrite attendance.
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
// GYM-018, GYM-026: opening new previews copies the owning plan; recorded executions retain snapshots.
function getSession(templateId, date, programId = GymData.library.activePlanId) {
  const key = `${date}:${templateId}`;
  const previous = sessionInstances.get(key);
  const past = date < todayDate && pastPlans.find(item => item.plannedDate === date && item.templateId === templateId);
  const definition = past ? { id: past.programId, name: past.programName, revision: past.planRevision, sessions: [past.template] } : GymData.library.plans.find(item => item.id === programId);
  const instance = GymPlan.openOccurrence(definition, GymData.catalog, date, templateId, previous);
  if (instance !== previous) instance.exercises.forEach(entry => { entry.timers = GymTimers.create(entry.recoverySeconds); entry.warmups = []; });
  sessionInstances.set(key, instance);
  return instance;
}
let currentSession = null;
const session = () => currentSession;
const exercise = () => session().exercises[exerciseIndex];
const completed = item => item.sets.filter(set => set.done).length;
const format = value => new Intl.NumberFormat('en-GB', { maximumFractionDigits: 1 }).format(value);
const formatObservation = value => new Intl.NumberFormat('en-GB', { maximumFractionDigits: 20 }).format(value);
const escapeHtml = value => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
const recordedForDate = date => [...sessionInstances.values()].find(item => item.date === date && GymPlan.locked(item));
const plannedForDate = date => {
  const recorded = recordedForDate(date);
  const past = date < todayDate && pastPlans.find(item => item.plannedDate === date);
  const sessionId = recorded?.templateId ?? past?.templateId ?? GymCalendar.plan(date, GymData.program.schedule, GymData.program.dateAssignments);
  return { sessionId, programId: recorded?.programId ?? past?.programId ?? GymData.library.activePlanId, template: recorded ?? past?.template ?? GymData.program.sessions.find(item => item.id === sessionId) ?? null };
};
const executionAssignments = () => {
  const assignments = { ...GymData.program.dateAssignments };
  sessionInstances.forEach(item => { if (GymPlan.locked(item)) assignments[item.date] = item.templateId; });
  return assignments;
};
const fullDate = date => GymCalendar.label(date, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
// GYM-006, GYM-008, GYM-010, GYM-015: view switching and keyboard focus.
function showView(id, focusId) {
  progressExploration?.cancelGesture();
  document.querySelectorAll('[data-view]').forEach(view => { view.hidden = view.id !== id; });
  if (id === 'progress') progressExploration?.resize();
  ['programNav', 'planNav', 'progressNav'].forEach(navId => byId(navId).removeAttribute('aria-current'));
  if (id !== 'settings') byId(id === 'progress' ? 'progressNav' : id === 'plans' || id === 'plan' || id === 'planSession' ? 'planNav' : 'programNav').setAttribute('aria-current', 'page');
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
  byId('sessionCard').innerHTML = `<p id="selectedDateLabel" class="eyebrow" aria-live="polite">${dateLabel}</p><h3>${escapeHtml(planned?.name ?? 'No session planned')}</h3><p>${escapeHtml(planned ? [selected.programId !== GymData.library.activePlanId ? planned.programName ?? pastPlans.find(item => item.plannedDate === selectedDate)?.programName : '', planned.focus || 'Training session'].filter(Boolean).join(' · ') : 'Assign a session in the Plan section.')}</p>${planned ? `<button id="openSelectedSession" class="md-button md-filled calendar-start">${selectedDate > todayDate ? 'Preview session' : 'View session'}</button>` : ''}`;
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
  byId('pendingSessions').innerHTML = pending.slice(0, 3).map(item => `<button class="exercise-card" data-pending-date="${item.plannedDate}"><span class="agenda-date"><strong>${GymCalendar.label(item.plannedDate, { day: 'numeric' })}</strong><small>${GymCalendar.label(item.plannedDate, { month: 'short' })}</small></span><span><strong>${escapeHtml(item.template.name)}</strong><small>${escapeHtml([item.programId !== GymData.library.activePlanId ? item.programName : '', GymCalendar.label(item.plannedDate, { weekday: 'long' })].filter(Boolean).join(' · '))}</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button>`).join('') || '<p class="empty-calendar">No pending sessions in the last 28 days.</p>';
}
function openSession(templateId, date, programId) {
  sessionDate = date;
  currentSession = getSession(templateId, date, programId);
  renderSession();
  showView('session', 'backProgram');
}
// GYM-001, GYM-005, GYM-008, GYM-019
function renderSession() {
  byId('sessionName').textContent = session().name;
  byId('sessionFocus').textContent = [session().programId !== GymData.library.activePlanId ? session().programName : '', session().focus, fullDate(sessionDate), sessionDate > todayDate ? 'Planned preview' : ''].filter(Boolean).join(' · ');
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
  byId('exerciseList').innerHTML = session().exercises.map((item, index) => `<div class="workout-exercise-row" data-sort-item="${escapeHtml(item.entryId)}"><button class="exercise-card" data-exercise="${index}"><span class="exercise-number">${String(index + 1).padStart(2, '0')}</span><span><strong>${escapeHtml(item.name)}</strong><small>${item.sets.length} × ${escapeHtml(item.target)} reps${completed(item) ? ` · ${completed(item)}/${item.sets.length} done` : ''}</small></span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button><div class="workout-order-controls">${sortHandle(item.name)}</div></div>`).join('');
  if (state === 'finished') {
    byId('exerciseList').querySelectorAll('[data-sort-handle]').forEach(button => { button.disabled = true; });
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
// GYM-025: wrapping values replace inactive text fields without changing data.
function renderReadonlyValue(field, readonly) {
  let preview = field.nextElementSibling;
  if (!preview?.classList.contains('readonly-value')) {
    if (!readonly) return;
    preview = document.createElement('p');
    preview.className = `readonly-value${field.classList.contains('plan-input') ? ' readonly-plan-value' : ''}`;
    preview.dataset.readonlyFor = field.id;
    field.after(preview);
  }
  field.hidden = readonly;
  preview.hidden = !readonly;
  preview.textContent = field.tagName === 'SELECT' ? field.selectedOptions[0]?.textContent ?? '' : field.value;
}
// GYM-001, GYM-003, GYM-008, GYM-009, GYM-012, GYM-017, GYM-025
function renderExercise() {
  byId('exercise').classList.toggle('finished-review', session().state === 'finished');
  byId('exerciseName').textContent = exercise().name;
  byId('prescription').textContent = `Target: ${exercise().sets.length} sets × ${exercise().target} reps`;
  byId('back').innerHTML = buttonIcon('back');
  byId('back').setAttribute('aria-label', `Back to ${session().name}`);
  byId('exerciseSessionActions').hidden = session().state === 'ready';
  byId('exerciseSessionStatus').textContent = GymSession.labels[session().state];
  byId('exerciseSessionStatus').hidden = !['paused', 'finished'].includes(session().state);
  byId('pauseExerciseSession').hidden = session().state !== 'inProgress';
  byId('finishExerciseSession').hidden = !['inProgress', 'paused'].includes(session().state);
  byId('finishExerciseSession').className = `md-button ${session().state === 'paused' ? 'md-outlined' : 'md-filled'}`;
  byId('seatInput').value = exercise().seat;
  byId('seatInput').disabled = session().state === 'finished';
  renderReadonlyValue(byId('seatInput'), session().state === 'finished');
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
  GymDialogs.open(byId('videoDialog'));
}
// GYM-010: fictional immutable historical observations, never today's editable samples.
function renderProgress() {
  progressExploration?.dispose();
  progressExploration = null;
  const exerciseId = byId('progressExercise').value;
  if (exerciseId !== progressExercise) {
    progressExercise = exerciseId;
    progressDate = null;
    progressView = { zoom: 1, scrollLeft: 0 };
  }
  renderAttendance();
  const selected = GymData.catalog[Number(byId('progressExercise').value)];
  const context = GymData.context(selected);
  const series = GymMetrics.series(GymData.history, context, metric, selected.weight);
  const labels = {
    reps: { title: `Reps at ${selected.weight} kg`, unit: 'reps', subtitle: 'Best working set at this weight', explanation: 'More reps at the same weight can show progress. Compare similar effort, setup, and movement.' },
    weight: { title: 'Weight at 10 reps', unit: 'kg', subtitle: 'Heaviest working set of exactly 10 reps', explanation: 'Track weight for the same repetition task. This is a performed set, not a tested maximum.' },
    volume: { title: 'Volume load', unit: 'kg·reps', subtitle: 'Weight × reps, summed over working sets', explanation: 'This describes workload. More volume does not by itself mean more strength or muscle growth.' }
  }[metric];
  const model = GymCharts.layout(series, metric === 'reps');
  progressPoints = model.points;
  if (!series.some(point => point.date === progressDate)) progressDate = series.at(-1)?.date ?? null;
  byId('metricTitle').textContent = labels.title;
  byId('metricUnit').textContent = labels.unit;
  byId('metricSubtitle').textContent = labels.subtitle;
  const dateLabel = (date, full = false) => GymCalendar.label(date, { day: 'numeric', month: 'short', ...(full ? { year: 'numeric' } : {}) });
  progressExploration = GymCharts.render(byId('chart'), model, {unit:labels.unit, selectedDate:progressDate, format:formatObservation, dateLabel, view:progressView, onInspect:selectProgressObservation});
  byId('chartHelp').textContent = `${model.points.some(point=>point.value===null) ? '— No comparable set · ' : ''}Drag plot to inspect; swipe dates to pan.`;
  byId('chartHelp').hidden = !model.points.length;
  selectProgressObservation(progressDate);
  byId('workingSets').textContent = GymMetrics.countSets(GymData.history, context);
  byId('comparisonContext').textContent = `${selected.machine} · ${selected.seat}. Sample history uses the same movement and 2 reps in reserve (estimated reps left). Warm-ups excluded.`;
  byId('metricExplanation').textContent = labels.explanation;
  document.querySelectorAll('[data-metric]').forEach(button => button.setAttribute('aria-pressed', button.dataset.metric === metric));
}
// GYM-010: inspection changes the readout/guide; rerenders retain independent pan.
function selectProgressObservation(date, reveal = false) {
  const point = progressPoints.find(item => item.date === date);
  progressDate = point?.date ?? null;
  byId('metricValue').textContent = point?.value == null ? '—' : formatObservation(point.value);
  byId('metricDate').textContent = point ? GymCalendar.label(point.date, {day:'numeric',month:'short',year:'numeric'}) : 'No observations';
  if (point) byId('metricDate').dateTime = point.date;
  else byId('metricDate').removeAttribute('datetime');
  byId('chart').querySelectorAll('[data-observation-date]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.observationDate===progressDate)));
  progressExploration?.select(progressDate);
  if (reveal) progressExploration?.reveal();
}
byId('chart').addEventListener('click', event => {
  const point = event.target.closest('[data-observation-date]');
  if (point) selectProgressObservation(point.dataset.observationDate, true);
});
byId('chart').addEventListener('keydown', event => {
  const point = event.target.closest('[data-observation-date]');
  if (!point || !['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;
  event.preventDefault();
  const points = [...byId('chart').querySelectorAll('[data-observation-date]')];
  const index = event.key === 'Home' ? 0 : event.key === 'End' ? points.length - 1 : points.indexOf(point)+(event.key==='ArrowLeft'?-1:1);
  const next = points[index];
  if (next) { next.focus({preventScroll:true}); selectProgressObservation(next.dataset.observationDate, true); }
});
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
  ['execution', 'recovery'].forEach(item => byId(`${item}Mode`).setAttribute('aria-pressed', mode === item));
  const other = mode === 'execution' ? 'recovery' : 'execution';
  byId('timerContext').textContent = session().state === 'paused' ? 'Resume the session to run timers.' : state[other].startedAt !== null ? `${other === 'execution' ? 'Execution' : 'Recovery'} is also running.` : '';
}
byId('exerciseList').addEventListener('click', event => {
  // GYM-001: select an exercise within the current dated workout.
  const card = event.target.closest('[data-exercise]');
  if (card) selectExercise(Number(card.dataset.exercise));
});
// GYM-019: preserve selected identity and all occurrence data on drop.
GymSorting.mount(byId('exerciseList'), (entryId, target) => {
  const selected = exercise();
  const next = GymPlan.reorderExercise(session(), entryId, target);
  if (next < 0) return;
  exerciseIndex = Math.max(0, session().exercises.indexOf(selected));
  renderSession();
  byId('executionOrderStatus').textContent = `${session().exercises[next].name} moved to position ${next + 1}.`;
  byId('exerciseList').children[next].querySelector('[data-sort-handle]').focus({ preventScroll: true });
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
  if (item.template) openSession(item.sessionId, selectedDate, item.programId);
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
// GYM-018, GYM-026: the selected editor uses its retained per-plan draft, separate from Train.
let planDraft = null;
let editingPlanId = GymData.library.activePlanId;
const planDrafts = new Map();
let draftDate = selectedDate;
let editingTemplateId = null;
let planDirty = false;
// GYM-026: browsing a saved plan never unlocks changes implicitly.
let planEditing = false;
let pendingPlanId = null;
let planMessage = '';
const ensureDraft = () => { if (!planDraft) planDraft = GymPlan.clone(GymData.library.plans.find(item => item.id === editingPlanId)); };
// GYM-026: cache each draft independently; editor navigation never commits it.
function retainDraft() {
  if (planDraft) planDrafts.set(editingPlanId, { draft: planDraft, dirty: planDirty, message: planMessage });
}
function openPlan(id) {
  retainDraft(); editingPlanId = id;
  const cached = planDrafts.get(id);
  planDraft = cached?.draft ?? GymPlan.clone(GymData.library.plans.find(item => item.id === id));
  planDirty = cached?.dirty ?? false; planMessage = cached?.message ?? '';
  planEditing = !GymData.library.plans.some(item => item.id === id);
  editingTemplateId = null; draftDate = selectedDate; renderPlan(); showView('plan', 'backToPlans');
}
function renderPlans() {
  retainDraft();
  const savedIds = new Set(GymData.library.plans.map(item => item.id));
  const plans = [...GymData.library.plans, ...[...planDrafts.values()].filter(item => !savedIds.has(item.draft.id)).map(item => item.draft)];
  byId('savedPlans').innerHTML = plans.map(item => {
    const active = item.id === GymData.library.activePlanId;
    const dirty = planDrafts.get(item.id)?.dirty;
    return `<div class="plan-library-row"><button class="exercise-card" data-edit-plan="${escapeHtml(item.id)}"><span><strong>${escapeHtml(item.name)}</strong><small>${item.sessions.length} ${item.sessions.length === 1 ? 'session' : 'sessions'}${dirty ? ' · Unsaved changes' : ''}</small>${active ? '<span class="active-plan-tag">Active</span>' : ''}</span><span class="chevron" aria-hidden="true">${buttonIcon('right')}</span></button></div>`;
  }).join('');
}
const templateDraft = () => planDraft.sessions.find(item => item.id === editingTemplateId);
const assignmentOptions = first => `<option value="${first}">${first === 'weekly' ? 'Use weekly pattern' : 'No session'}</option>${first === 'weekly' ? '<option value="none">No session</option>' : ''}${planDraft.sessions.map(item => `<option value="${item.id}">${escapeHtml(item.name)} (${item.label})</option>`).join('')}`;
// GYM-025: Save indicates a pending change instead of offering a no-op.
function syncSaveActions() {
  document.querySelectorAll('.save-plan').forEach(button => { button.disabled = !planDirty || !planEditing; button.hidden = !planEditing; });
  ['editPlan', 'editTemplate'].forEach(id => { byId(id).hidden = planEditing; });
  // GYM-026: activation is separate from library browsing and draft editing.
  const saved = GymData.library.plans.some(item => item.id === editingPlanId);
  const active = editingPlanId === GymData.library.activePlanId;
  byId('planActivation').hidden = planEditing || !saved;
  byId('planActivationStatus').textContent = active ? 'Active plan' : planDirty ? 'Save changes before making this plan active.' : '';
  byId('makePlanActive').hidden = active;
  byId('makePlanActive').disabled = planDirty;
  byId('makePlanActive').setAttribute('aria-label', `Make ${planDraft.name} the active plan`);
}
// GYM-026: leave preview/navigation usable while hiding mutation controls.
function applyPlanMode() {
  for (const form of [byId('planForm'), byId('templateForm')]) {
    form.classList.toggle('plan-readonly', !planEditing);
    form.querySelectorAll('input,select').forEach(field => { field.disabled = !planEditing; });
    form.querySelectorAll('input:not([type]),input[type="text"],select:not(#exerciseToAdd)').forEach(field => renderReadonlyValue(field, !planEditing));
    form.querySelectorAll('[data-sort-handle],[data-remove-entry],[data-delete-template],#addPlanSession,#addPlanExercise').forEach(action => {
      action.hidden = !planEditing;
      action.disabled = !planEditing || (action.matches('[data-delete-template]') && planDraft.sessions.length === 1) || (action.id === 'addPlanSession' && planDraft.sessions.length >= 7);
    });
  }
  byId('planHeading').textContent = planEditing ? 'Edit plan' : 'Plan';
  byId('templateHeading').textContent = planEditing ? 'Edit session' : 'Session';
  syncSaveActions();
}
['editPlan', 'editTemplate'].forEach(id => byId(id).addEventListener('click', () => {
  planEditing = true;
  renderPlan();
  if (editingTemplateId) renderTemplate();
  (id === 'editPlan' ? byId('planName') : byId('templateName')).focus();
}));
function markPlanDirty() {
  planDirty = true;
  planMessage = 'Unsaved changes';
  retainDraft();
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
  applyPlanMode();
}
// GYM-021, GYM-025: summaries retain targets and wrap complete prescription phrases.
function prescriptionSummary(item, index) {
  const name = GymData.catalog.find(machine => machine.id === item.exerciseId)?.name ?? 'Choose exercise';
  const reps = item.minReps === item.maxReps ? item.minReps : `${item.minReps}–${item.maxReps}`;
  return `${index + 1}. ${escapeHtml(name)} <span class="prescription-group">· ${item.setCount} × ${reps}</span> <span class="prescription-group">· ${item.recoverySeconds ?? 90}s recovery</span>`;
}
function renderTemplate(expanded = null) {
  syncSaveActions();
  const template = templateDraft();
  byId('templateHeading').textContent = 'Edit session';
  byId('templateName').value = template.name;
  byId('templateFocus').value = template.focus;
  byId('plannedExercises').innerHTML = template.exercises.length ? template.exercises.map((item, index) => `<div class="planned-exercise-row" data-sort-item="${escapeHtml(item.entryId)}">${sortHandle(GymData.catalog.find(machine => machine.id === item.exerciseId)?.name ?? 'exercise')}<details class="prescription-card" ${(expanded ? expanded.has(item.entryId) : index === 0) ? 'open' : ''}><summary><strong class="prescription-summary">${prescriptionSummary(item, index)}</strong><span class="disclosure-icon" aria-hidden="true">${buttonIcon('expand')}</span></summary><label class="select-label" for="planned-machine-${item.entryId}">Exercise</label><select id="planned-machine-${item.entryId}" data-entry="${item.entryId}" data-plan-field="exerciseId">${GymData.catalog.map(machine => `<option value="${machine.id}" ${machine.id === item.exerciseId ? 'selected' : ''}>${machine.name}</option>`).join('')}</select><div class="prescription-fields"><label>Working sets<input type="number" min="1" max="10" step="1" required value="${item.setCount}" data-entry="${item.entryId}" data-plan-field="setCount" aria-label="Exercise ${index + 1} planned working sets"></label><label>Min reps<input type="number" min="1" max="50" step="1" required value="${item.minReps}" data-entry="${item.entryId}" data-plan-field="minReps" aria-label="Exercise ${index + 1} minimum target reps"></label><label>Max reps<input type="number" min="1" max="50" step="1" required value="${item.maxReps}" data-entry="${item.entryId}" data-plan-field="maxReps" aria-label="Exercise ${index + 1} maximum target reps"></label></div><label class="select-label" for="planned-recovery-${item.entryId}">Recovery (s)</label><input id="planned-recovery-${item.entryId}" class="plan-input" type="number" min="1" max="3600" step="1" required value="${item.recoverySeconds ?? 90}" data-entry="${item.entryId}" data-plan-field="recoverySeconds" aria-label="Exercise ${index + 1} planned recovery seconds"><div class="prescription-actions"><button class="md-icon-button" type="button" data-remove-entry="${item.entryId}" aria-label="Remove exercise ${index + 1}">${buttonIcon('trash')}</button></div></details></div>`).join('') : '<p class="field-help">Add an exercise to this session.</p>';
  byId('exerciseToAdd').innerHTML = GymData.catalog.map(item => `<option value="${item.id}">${item.name}</option>`).join('');
  byId('templateFeedback').textContent = planMessage;
  applyPlanMode();
}
function savePlan(event) {
  event.preventDefault();
  if (!planEditing) return;
  const errors = GymPlan.validate(planDraft, GymData.catalog);
  if (errors.length) {
    planMessage = errors.join(' ');
    byId('planFeedback').textContent = planMessage;
    byId('templateFeedback').textContent = planMessage;
    return;
  }
  let saved;
  try { saved = planDirty ? GymPrograms.commit(GymData.library, planDraft, GymData.catalog) : GymData.library.plans.find(item => item.id === editingPlanId); }
  catch (error) { byId('planFeedback').textContent = error.message; byId('templateFeedback').textContent = error.message; return; }
  planDraft = GymPlan.clone(saved);
  planDirty = false;
  planEditing = false;
  planMessage = 'Plan saved';
  retainDraft();
  renderPlan();
  renderProgram();
  showView('plan', 'planNav');
}
byId('planNav').addEventListener('click', () => { renderPlans(); showView('plans', 'planNav'); });
byId('backToPlans').addEventListener('click', () => { renderPlans(); showView('plans', 'planNav'); });
byId('savedPlans').addEventListener('click', event => {
  const edit = event.target.closest('[data-edit-plan]');
  if (edit) openPlan(edit.dataset.editPlan);
});
// GYM-026: only a saved, clean, inactive plan in read-only detail can request activation.
byId('makePlanActive').addEventListener('click', () => {
  retainDraft();
  const target = GymData.library.plans.find(item => item.id === editingPlanId);
  if (!target || planEditing || planDirty || target.id === GymData.library.activePlanId) return;
  pendingPlanId = target.id;
  byId('activatePlanTitle').textContent = `Make ${target.name} active?`;
  byId('currentActivePlan').textContent = `Current plan: ${GymData.program.name}`;
  GymDialogs.open(byId('activatePlanDialog'));
});
// GYM-026: only this explicit confirmation changes the active plan.
byId('cancelPlanActivation').addEventListener('click', () => byId('activatePlanDialog').close());
byId('activatePlanDialog').addEventListener('close', () => { pendingPlanId = null; });
byId('confirmPlanActivation').addEventListener('click', () => {
  const id = pendingPlanId;
  try {
    GymPrograms.activate(GymData.library, id, planDrafts.get(id)?.dirty);
    byId('activatePlanDialog').close();
    renderProgram(); renderPlan();
    byId('backToPlans').focus();
  } catch (error) {
    byId('activatePlanDialog').close();
    planMessage = error.message; byId('planFeedback').textContent = planMessage;
  }
});
byId('addPlan').addEventListener('click', () => {
  retainDraft();
  const draft = GymPrograms.create(GymData.catalog, `plan-${crypto.randomUUID()}`);
  planDrafts.set(draft.id, { draft, dirty: true, message: 'Unsaved new plan' });
  openPlan(draft.id); byId('planName').focus();
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
  planDraft.sessions.push({ id, label, name: `Session ${label}`, focus: '', exercises: [{ entryId: crypto.randomUUID(), exerciseId: GymData.catalog[0].id, setCount: 3, minReps: 10, maxReps: 12, recoverySeconds: 90 }] });
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

});
// GYM-018: sorting changes the draft only, retaining disclosure state.
GymSorting.mount(byId('plannedExercises'), (entryId, target) => {
  if (!planEditing) return;
  const list = templateDraft().exercises;
  const index = list.findIndex(item => item.entryId === entryId);
  if (index < 0 || index === target || target < 0 || target >= list.length) return;
  const expanded = new Set([...byId('plannedExercises').querySelectorAll('[data-sort-item]')].filter(row => row.querySelector('details').open).map(row => row.dataset.sortItem));
  const [item] = list.splice(index, 1);
  list.splice(target, 0, item);
  markPlanDirty(); renderTemplate(expanded);
  byId('templateOrderStatus').textContent = `${GymData.catalog.find(machine => machine.id === item.exerciseId).name} moved to position ${target + 1}.`;
  byId('plannedExercises').children[target].querySelector('[data-sort-handle]').focus({ preventScroll: true });
});
byId('addPlanExercise').addEventListener('click', () => {
  templateDraft().exercises.push({ entryId: crypto.randomUUID(), exerciseId: byId('exerciseToAdd').value, setCount: 3, minReps: 10, maxReps: 12, recoverySeconds: 90 });
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

setInterval(() => {
  sessionInstances.forEach(item => item.exercises.forEach(entry => GymTimers.read(entry.timers, 'recovery', Date.now())));
  if (!byId('exercise').hidden) renderTimer();
}, 200);
// GYM-002, GYM-006
byId('watchVideo').addEventListener('click', openVideo);
byId('closeVideo').addEventListener('click', () => byId('videoDialog').close());
renderProgram();

// GYM-022–024: all exports use current state, not the selected chart or a saved demo fixture.
GymSettings.mount({
  show: showView,
  input: () => {
    todayDate = GymCalendar.today();
    capturePastPlans();
    retainDraft();
    return { settings: GymSettings.state, program: GymData.program, plans: GymData.library.plans, activePlanId: GymData.library.activePlanId, drafts: [...planDrafts.values()].filter(item => item.dirty).map(item => item.draft),
      catalog: GymData.catalog, sessions: sessionInstances, pastPlans,
      history: GymData.history, attendanceHistory: GymData.attendanceHistory,
      context: GymData.context, today: todayDate, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone };
  }
});
