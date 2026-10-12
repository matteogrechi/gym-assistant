'use strict';
// GYM-001, GYM-003, GYM-004, GYM-005: isolated sample data, held only in memory.
const sampleExercises = [
  { name: 'Leg press', category: 'LOWER BODY', seat: 'Seat 4 · backrest 2', weight: 60, reps: 12 },
  { name: 'Chest press', category: 'UPPER BODY', seat: 'Seat 3', weight: 20, reps: 10 },
  { name: 'Lat pulldown', category: 'UPPER BODY', seat: 'Thigh pad 2', weight: 30, reps: 12 }
].map(exercise => ({ ...exercise, sets: Array.from({ length: 3 }, () => ({ weight: exercise.weight, reps: exercise.reps, done: false })) }));
let selectedIndex = 0;
const byId = id => document.getElementById(id);
const completed = exercise => exercise.sets.filter(set => set.done).length;
// GYM-001, GYM-005
function renderSession() {
  byId('exerciseList').innerHTML = sampleExercises.map((exercise, index) => `<button class="exercise-card" data-req="GYM-001 GYM-005" data-exercise="${index}"><span class="exercise-number">0${index + 1}</span><span><strong>${exercise.name}</strong><small>3 sets · ${completed(exercise)}/3 complete</small></span><span class="chevron" aria-hidden="true">↗</span></button>`).join('');
}
// GYM-001
function selectExercise(index) {
  selectedIndex = index;
  renderExercise();
  byId('session').hidden = true;
  byId('exercise').hidden = false;
  byId('back').focus();
}
// GYM-003, GYM-004, GYM-005
function renderExercise() {
  const exercise = sampleExercises[selectedIndex];
  byId('exerciseName').textContent = exercise.name;
  byId('exerciseCategory').textContent = exercise.category;
  byId('seatInput').value = exercise.seat;
  byId('setCount').textContent = `${completed(exercise)} of 3 complete`;
  byId('sets').innerHTML = exercise.sets.map((set, index) => `<div class="set-row ${set.done ? 'complete' : ''}"><span>${index + 1}</span><input aria-label="Set ${index + 1} weight in kilograms" type="number" min="0" step="any" required value="${set.weight}" data-set="${index}" data-field="weight"><input aria-label="Set ${index + 1} repetitions" type="number" min="0" step="1" required value="${set.reps}" data-set="${index}" data-field="reps"><button aria-label="Set ${index + 1} complete" aria-pressed="${set.done}" data-toggle="${index}">✓</button></div>`).join('');
}
// GYM-004: retain the last valid value if input is invalid.
function updateSet(input) {
  const set = sampleExercises[selectedIndex].sets[Number(input.dataset.set)];
  if (!input.checkValidity()) {
    input.reportValidity();
    input.value = set[input.dataset.field];
    return;
  }
  set[input.dataset.field] = Number(input.value);
}
// GYM-005
function toggleSet(index) {
  const set = sampleExercises[selectedIndex].sets[index];
  set.done = !set.done;
  renderExercise();
  byId('sets').querySelector(`[data-toggle="${index}"]`).focus();
}
// GYM-002: intentional placeholder, not actual video playback.
function openVideo() {
  byId('videoTitle').textContent = sampleExercises[selectedIndex].name;
  byId('videoDialog').showModal();
}
byId('exerciseList').addEventListener('click', event => {
  const card = event.target.closest('[data-exercise]');
  if (card) selectExercise(Number(card.dataset.exercise));
});
// GYM-001, GYM-006: navigation restores keyboard focus.
byId('back').addEventListener('click', () => {
  renderSession();
  byId('exercise').hidden = true;
  byId('session').hidden = false;
  byId('exerciseList').querySelector(`[data-exercise="${selectedIndex}"]`).focus();
});
// GYM-003
byId('seatInput').addEventListener('input', event => { sampleExercises[selectedIndex].seat = event.target.value; });
// GYM-004
byId('sets').addEventListener('change', event => { if (event.target.matches('input')) updateSet(event.target); });
// GYM-005
byId('sets').addEventListener('click', event => {
  const button = event.target.closest('[data-toggle]');
  if (button) toggleSet(Number(button.dataset.toggle));
});
// GYM-002, GYM-006: native dialog supports Escape and focus restoration.
byId('watchVideo').addEventListener('click', openVideo);
byId('closeVideo').addEventListener('click', () => byId('videoDialog').close());
byId('videoDialog').addEventListener('click', event => {
  const bounds = event.currentTarget.getBoundingClientRect();
  if (event.target === event.currentTarget && (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) event.currentTarget.close();
});
renderSession();
