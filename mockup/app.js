'use strict';
/* global GymData, GymMetrics, GymTimers */
const byId = id => document.getElementById(id);
let sessionIndex = 0;
let exerciseIndex = 0;
let metric = 'reps';
const session = () => GymData.program.sessions[sessionIndex];
const exercise = () => session().exercises[exerciseIndex];
const completed = item => item.sets.filter(set => set.done).length;
const format = value => new Intl.NumberFormat('en-GB', { maximumFractionDigits: 1 }).format(value);
// GYM-012, GYM-014: independent transient state for each exercise occurrence.
GymData.program.sessions.forEach(item => item.exercises.forEach(entry => {
  entry.timers = GymTimers.create();
  entry.warmups = [];
}));
// GYM-006, GYM-008, GYM-010, GYM-015: view switching and keyboard focus.
function showView(id, focusId) {
  document.querySelectorAll('[data-view]').forEach(view => { view.hidden = view.id !== id; });
  ['programNav', 'progressNav'].forEach(navId => byId(navId).removeAttribute('aria-current'));
  byId(id === 'progress' ? 'progressNav' : 'programNav').setAttribute('aria-current', 'page');
  if (focusId) byId(focusId).focus();
  document.querySelector('.screen-area').scrollTop = 0;
}
// GYM-008
function renderProgram() {
  byId('sessionList').innerHTML = GymData.program.sessions.map((item, index) => `<button class="exercise-card" data-session="${index}"><span class="exercise-number">${String.fromCharCode(65 + index)}</span><span><strong>${item.name}</strong><small>${item.focus} · ${item.exercises.length} exercises</small></span><span class="chevron" aria-hidden="true">↗</span></button>`).join('');
}
// GYM-001, GYM-005, GYM-008
function renderSession() {
  byId('sessionName').textContent = session().name;
  byId('sessionFocus').textContent = session().focus;
  byId('sessionCount').textContent = `${session().exercises.reduce((sum, item) => sum + completed(item), 0)} / 9 working sets done`;
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
  byId('timerToggle').textContent = running ? 'Pause' : state[mode].elapsed > 0 && !(mode === 'recovery' && seconds === 0) ? 'Resume' : 'Start';
  byId('recoverySettings').hidden = mode !== 'recovery';
  byId('recoverySeconds').disabled = state.recovery.startedAt !== null;
  if (document.activeElement !== byId('recoverySeconds')) byId('recoverySeconds').value = state.duration;
  ['execution', 'recovery'].forEach(item => byId(`${item}Mode`).setAttribute('aria-pressed', mode === item));
  const other = mode === 'execution' ? 'recovery' : 'execution';
  byId('timerContext').textContent = state[other].startedAt !== null ? `Your ${other} timer is also running.` : 'For this exercise · manual controls';
}
// GYM-008, GYM-001
byId('sessionList').addEventListener('click', event => {
  const card = event.target.closest('[data-session]');
  if (!card) return;
  sessionIndex = Number(card.dataset.session);
  renderSession();
  showView('session', 'backProgram');
});
byId('exerciseList').addEventListener('click', event => {
  const card = event.target.closest('[data-exercise]');
  if (card) selectExercise(Number(card.dataset.exercise));
});
byId('backProgram').addEventListener('click', () => {
  showView('program');
  byId('sessionList').querySelector(`[data-session="${sessionIndex}"]`).focus();
});
byId('back').addEventListener('click', () => {
  renderSession();
  showView('session');
  byId('exerciseList').querySelector(`[data-exercise="${exerciseIndex}"]`).focus();
});
byId('programNav').addEventListener('click', () => showView('program', 'programNav'));
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
  GymData.program.sessions.forEach(item => item.exercises.forEach(entry => GymTimers.read(entry.timers, 'recovery', Date.now())));
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
