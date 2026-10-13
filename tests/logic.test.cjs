const test = require('node:test');
const assert = require('node:assert/strict');
const data = require('../mockup/data.js');
const metrics = require('../mockup/metrics.js');
const timers = require('../mockup/timers.js');
const context = data.context(data.catalog[0]);

// GYM-010: expected outcomes derive from the spec's sample records and formulas.
test('GYM-010: sample progression separates matched reps, matched weight, and workload', () => {
  assert.deepEqual(metrics.series(data.history, context, 'reps', 60).map(p => p.value), [9, 10, 11, 12]);
  assert.deepEqual(metrics.series(data.history, context, 'weight', 60).map(p => p.value), [50, 60, 60, 65]);
  assert.deepEqual(metrics.series(data.history, context, 'volume', 60).map(p => p.value), [1520, 1630, 1740, 1850]);
  assert.equal(metrics.countSets(data.history, context), 12);
});

test('GYM-010: different machine/setup/unit/effort/technique never silently merges', () => {
  for (const key of ['machine', 'setup', 'unit', 'convention', 'rom', 'rir', 'exerciseId']) {
    const altered = { ...data.history[0], [key]: `different-${key}` };
    assert.equal(metrics.series([altered], context, 'volume', 60).length, 0);
  }
  assert.deepEqual(metrics.series(data.history, { ...context, rir: undefined }, 'reps', 60), []);
});

test('GYM-010, GYM-014: warmups, unfinished and invalid sets do not inflate statistics', () => {
  const record = { ...context, date: '2026-10-12', sets: [
    { reps: 10, weight: 60, done: true, warmup: false },
    { reps: 100, weight: 100, done: true, warmup: true },
    { reps: 20, weight: 80, done: false, warmup: false },
    { reps: -2, weight: 60, done: true, warmup: false },
    { reps: 2.5, weight: 60, done: true, warmup: false },
    { reps: 10, weight: Infinity, done: true, warmup: false }
  ] };
  assert.equal(metrics.series([record], context, 'volume', 60)[0].value, 600);
  assert.equal(metrics.series([record], context, 'reps', 60)[0].value, 10);
  assert.equal(metrics.series([record], context, 'weight', 60)[0].value, 60);
  assert.equal(metrics.countSets([record], context), 1);
});

test('GYM-010: missing comparators show null gaps and dates stay chronological', () => {
  const record = { ...context, date: '2026-10-12', sets: [{ reps: 8, weight: 55, done: true }] };
  assert.equal(metrics.series([record], context, 'reps', 60)[0].value, null);
  assert.equal(metrics.series([record], context, 'weight', 60)[0].value, null);
  assert.deepEqual(metrics.series([...data.history].reverse(), context, 'reps', 60).map(p => p.date), ['2026-09-14', '2026-09-21', '2026-09-28', '2026-10-05']);
});

// GYM-012: injected timestamps verify elapsed time without real sleeps.
test('GYM-012: execution counts elapsed time, pauses, resumes and resets', () => {
  const state = timers.create();
  timers.start(state, 'execution', 1000);
  assert.equal(timers.read(state, 'execution', 6500), 5);
  timers.pause(state, 'execution', 6500);
  assert.equal(timers.read(state, 'execution', 100000), 5);
  timers.start(state, 'execution', 100000);
  assert.equal(timers.read(state, 'execution', 102500), 8);
  timers.reset(state, 'execution');
  assert.equal(timers.read(state, 'execution', 200000), 0);
  assert.equal(state.execution.startedAt, null);
});

test('GYM-012: recovery catches up after background delay, stops at zero, and restarts', () => {
  const state = timers.create(90);
  timers.start(state, 'recovery', 1000);
  assert.equal(timers.read(state, 'recovery', 32000), 59);
  assert.equal(timers.read(state, 'recovery', 200000), 0);
  assert.equal(state.recovery.startedAt, null);
  timers.start(state, 'recovery', 201000);
  assert.equal(timers.read(state, 'recovery', 201000), 90);
});

test('GYM-012: modes and separate exercise timers remain independent', () => {
  const first = timers.create(30);
  const second = timers.create();
  timers.start(first, 'execution', 0);
  timers.start(first, 'recovery', 1000);
  timers.pause(first, 'execution', 5000);
  assert.equal(timers.read(first, 'execution', 10000), 5);
  assert.equal(timers.read(first, 'recovery', 10000), 21);
  assert.equal(timers.read(second, 'execution', 10000), 0);
  assert.equal(timers.read(second, 'recovery', 10000), 90);
});

test('GYM-012: recovery duration validates bounds and cannot change while running', () => {
  const state = timers.create();
  for (const bad of [0, -1, 1.5, 3601, NaN, Infinity]) assert.equal(timers.setDuration(state, bad), false);
  assert.equal(timers.setDuration(state, 1), true);
  timers.start(state, 'recovery', 0);
  assert.equal(timers.setDuration(state, 20), false);
  timers.pause(state, 'recovery', 500);
  assert.equal(timers.setDuration(state, 3600), true);
  assert.equal(timers.read(state, 'recovery', 2000), 3600);
});

// GYM-016: actual calendar outcomes rather than DOM presence.
const calendar = require('../mockup/calendar.js');
test('GYM-016: Monday-first weeks cross month, year, leap-day and DST boundaries', () => {
  assert.deepEqual(calendar.week('2026-10-12'), ['2026-10-12', '2026-10-13', '2026-10-14', '2026-10-15', '2026-10-16', '2026-10-17', '2026-10-18']);
  assert.equal(calendar.week('2027-01-01')[0], '2026-12-28');
  assert.equal(calendar.addDays('2028-02-28', 1), '2028-02-29');
  assert.equal(calendar.addDays('2026-03-28', 2), '2026-03-30');
  assert.equal(calendar.addDays('2026-10-24', 2), '2026-10-26');
});

test('GYM-016: sample sessions recur on weekdays; date edits affect only one day', () => {
  assert.equal(calendar.plan('2026-10-12', data.schedule, {}), 'session-A');
  assert.equal(calendar.plan('2026-10-13', data.schedule, {}), null);
  assert.equal(calendar.plan('2026-10-14', data.schedule, {}), 'session-B');
  assert.equal(calendar.plan('2026-10-16', data.schedule, {}), 'session-C');
  const overrides = { '2026-10-14': 'session-C', '2026-10-16': null };
  assert.equal(calendar.plan('2026-10-14', data.schedule, overrides), 'session-C');
  assert.equal(calendar.plan('2026-10-16', data.schedule, overrides), null);
  assert.equal(calendar.plan('2026-10-21', data.schedule, overrides), 'session-B');
});

test('GYM-016: upcoming agenda excludes today and respects overrides', () => {
  assert.deepEqual(calendar.upcoming('2026-10-12', data.schedule, {}), [
    { date: '2026-10-14', sessionId: 'session-B' }, { date: '2026-10-16', sessionId: 'session-C' }, { date: '2026-10-19', sessionId: 'session-A' }
  ]);
  assert.deepEqual(calendar.upcoming('2026-10-12', data.schedule, { '2026-10-13': 'session-C', '2026-10-14': null }), [
    { date: '2026-10-13', sessionId: 'session-C' }, { date: '2026-10-16', sessionId: 'session-C' }, { date: '2026-10-19', sessionId: 'session-A' }
  ]);
});

test('GYM-016: an empty planning horizon produces an empty agenda', () => {
  const overrides = Object.fromEntries(Array.from({ length: 28 }, (_, index) => [calendar.addDays('2026-10-12', index + 1), null]));
  assert.deepEqual(calendar.upcoming('2026-10-12', data.schedule, overrides), []);
});

const plan = require('../mockup/plan.js');
const lifecycle = require('../mockup/session.js');
test('GYM-017: state diagram admits only Start/Pause/Resume/Finish transitions', () => {
  const legal = { ready: { start: 'inProgress' }, inProgress: { pause: 'paused', finish: 'finished' }, paused: { resume: 'inProgress', finish: 'finished' }, finished: {} };
  for (const state of Object.keys(legal)) for (const action of ['start', 'pause', 'resume', 'finish']) {
    const instance = { state, exercises: [] };
    assert.equal(lifecycle.transition(instance, action, 1000), Boolean(legal[state][action]), `${state}: ${action}`);
    assert.equal(instance.state, legal[state][action] ?? state);
  }
  assert.equal(lifecycle.labels.inProgress, 'In progress');
});

test('GYM-017: Pause retains all logs and pauses both timers on every exercise; Resume is manual for timers', () => {
  const instance = plan.openOccurrence(data.program, data.catalog, '2026-10-12', 'session-A');
  instance.exercises.forEach(item => { item.timers = timers.create(); item.warmups = [{ reps: 15, weight: 20, done: true }]; });
  instance.exercises[0].seat = 'Seat 7';
  instance.exercises[0].sets[0].done = true;
  assert.equal(lifecycle.transition(instance, 'start', 1000), true);
  instance.exercises.forEach(item => { timers.start(item.timers, 'execution', 1000); timers.start(item.timers, 'recovery', 1000); });
  assert.equal(lifecycle.transition(instance, 'pause', 4000), true);
  instance.exercises.forEach(item => {
    assert.equal(item.timers.execution.startedAt, null);
    assert.equal(item.timers.recovery.startedAt, null);
    assert.equal(timers.read(item.timers, 'execution', 9000), 3);
    assert.equal(timers.read(item.timers, 'recovery', 9000), 87);
    assert.equal(item.warmups[0].reps, 15);
  });
  assert.equal(instance.exercises[0].seat, 'Seat 7');
  assert.equal(instance.exercises[0].sets[0].done, true);
  assert.equal(lifecycle.transition(instance, 'resume', 9000), true);
  assert.equal(instance.exercises[0].timers.execution.startedAt, null);
});

test('GYM-017: partial Finish is terminal, preserves records without marking sets done or adding attendance', () => {
  const instance = plan.openOccurrence(data.program, data.catalog, '2026-10-12', 'session-A');
  instance.exercises.forEach(item => { item.timers = timers.create(); });
  lifecycle.transition(instance, 'start', 1000);
  timers.start(instance.exercises[0].timers, 'execution', 1000);
  instance.exercises[0].sets[0].done = true;
  assert.equal(lifecycle.transition(instance, 'finish', 6000), true);
  assert.equal(instance.state, 'finished');
  assert.equal(instance.finishedAt, 6000);
  assert.equal(instance.attendedDate, undefined);
  assert.equal(instance.exercises.flatMap(item => item.sets).filter(set => set.done).length, 1);
  assert.equal(timers.read(instance.exercises[0].timers, 'execution', 9000), 5);
  assert.equal(plan.moveExercise(instance, 'A-leg', 1), -1);
  for (const action of ['start', 'pause', 'resume', 'finish']) assert.equal(lifecycle.transition(instance, action, 9000), false);
  assert.strictEqual(plan.openOccurrence(data.program, data.catalog, instance.date, 'session-A', instance), instance);
});

const attendance = require('../mockup/attendance.js');
test('GYM-020: cohort excludes today/future, separates late visits, and retains original plans', () => {
  const definition = plan.clone(data.program);
  const records = attendance.planned(definition, '2026-10-12');
  const observed = attendance.observations(data.attendanceHistory, []);
  assert.deepEqual(attendance.summarize(records, observed, '2026-10-12'), { planned: 12, logged: 6, onTime: 4, late: 2, early: 0, unlogged: 6, percent: 50, meanDelay: 1.5 });
  assert.equal(records[0].plannedDate, '2026-09-14');
  assert.equal(records.at(-1).plannedDate, '2026-10-09');
  assert.equal(attendance.pending(records, observed, '2026-10-12')[0].plannedDate, '2026-09-18');
  plan.removeSession(definition, 'session-A');
  definition.sessions[0].name = 'Renamed';
  assert.equal(records[0].template.name, 'Session A');
  assert.equal(records[0].template.exercises.length, 3);
  assert.equal(attendance.summarize(records, observed, '2026-10-12').planned, 12);
});

test('GYM-020: valid working log catches up once on actual date, without altering strength history', () => {
  const workout = plan.openOccurrence(data.program, data.catalog, '2026-09-18', 'session-C');
  workout.state = 'inProgress';
  workout.exercises[0].warmups = [{ reps: 15, weight: 20, done: true }];
  assert.equal(attendance.observe(workout, '2026-10-12'), false);
  workout.exercises[0].sets[0] = { reps: 0, weight: 30, done: true };
  assert.equal(attendance.observe(workout, '2026-10-12'), false);
  workout.exercises[0].sets[0].reps = 12;
  const before = plan.clone(data.history);
  assert.equal(attendance.observe(workout, '2026-10-12'), true);
  workout.state = 'paused';
  assert.equal(attendance.observe(workout, '2026-10-13'), false);
  assert.equal(workout.attendedDate, '2026-10-12');
  assert.equal(workout.date, '2026-09-18');
  const records = attendance.planned(data.program, '2026-10-12');
  const observed = attendance.observations(data.attendanceHistory, [workout, workout]);
  const stats = attendance.summarize(records, observed, '2026-10-12');
  assert.equal(stats.logged, 7);
  assert.equal(stats.late, 3);
  assert.equal(stats.unlogged, 5);
  assert.equal(stats.meanDelay, 9);
  assert.equal(attendance.pending(records, observed, '2026-10-12').length, 5);
  assert.deepEqual(data.history, before);
});

test('GYM-020: preview is not attendance; no plans/late visits mean N/A; early visits and civil delays work', () => {
  const future = plan.openOccurrence(data.program, data.catalog, '2026-10-19', 'session-A');
  future.exercises[0].sets[0].done = true;
  assert.equal(attendance.observe(future, '2026-10-12'), false);
  const empty = attendance.summarize([], new Map(), '2026-10-12');
  assert.equal(empty.percent, null);
  assert.equal(empty.meanDelay, null);
  const records = [{ id: 'test', plannedDate: '2026-10-24' }];
  const observed = new Map([['test', { actualDate: '2026-10-26' }]]);
  assert.equal(attendance.summarize(records, observed, '2026-10-27').meanDelay, 2);
  observed.set('test', { actualDate: '2026-10-23' });
  assert.equal(attendance.summarize(records, observed, '2026-10-27').early, 1);
  observed.set('test', { actualDate: '2026-10-28' });
  assert.equal(attendance.pending(records, observed, '2026-10-27').length, 1);
  assert.equal(attendance.summarize(records, observed, '2026-10-27').logged, 0);
});

test('GYM-020: today stays outside the denominator until the next civil day', () => {
  const records = attendance.planned(data.program, '2026-10-12', true);
  const observed = attendance.observations([...data.attendanceHistory, { plannedDate: '2026-10-12', actualDate: '2026-10-12', templateId: 'session-A' }], []);
  assert.equal(attendance.summarize(records, observed, '2026-10-12').logged, 6);
  assert.equal(attendance.summarize(records, observed, '2026-10-13').logged, 6);
  assert.equal(attendance.summarize(records, observed, '2026-10-13').planned, 12);
  assert.equal(attendance.pending(records, observed, '2026-10-13').length, 6);
});

test('GYM-019: permutation retains exercise identity, logs and running timers; plan and other date stay unchanged', () => {
  const definition = plan.clone(data.program);
  const workout = plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A');
  const other = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A');
  const leg = workout.exercises[0];
  leg.seat = 'Seat 7';
  leg.sets[0] = { reps: 8, weight: 42.5, done: true };
  leg.warmups = [{ reps: 15, weight: 20, done: true }];
  leg.timers = timers.create();
  timers.start(leg.timers, 'execution', 1000);
  const before = plan.clone(definition);
  assert.equal(plan.moveExercise(workout, 'A-leg', 1), 1);
  assert.strictEqual(workout.exercises[1], leg);
  assert.deepEqual(leg.sets[0], { reps: 8, weight: 42.5, done: true });
  assert.equal(leg.seat, 'Seat 7');
  assert.equal(leg.warmups[0].reps, 15);
  assert.equal(timers.read(leg.timers, 'execution', 6000), 5);
  assert.equal(leg.timers.execution.startedAt, 1000);
  assert.deepEqual(definition, before);
  assert.deepEqual(other.exercises.map(item => item.entryId), ['A-leg', 'A-chest', 'A-lat']);
});

test('GYM-019: reordered Ready preview survives reopening and plan saves; boundaries leave it untouched', () => {
  const definition = plan.clone(data.program);
  const preview = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A');
  for (const [id, direction] of [['A-leg', -1], ['A-lat', 1], ['missing', 1], ['A-leg', 2]]) {
    assert.equal(plan.moveExercise(preview, id, direction), -1);
    assert.equal(preview.orderChanged, false);
  }
  assert.equal(plan.moveExercise(preview, 'A-chest', -1), 0);
  const draft = plan.clone(definition);
  draft.sessions[0].name = 'New plan name';
  plan.apply(definition, draft, data.catalog);
  assert.strictEqual(plan.openOccurrence(definition, data.catalog, preview.date, 'session-A', preview), preview);
  assert.equal(preview.state, 'ready');
  assert.equal(preview.name, 'Session A');
  assert.equal(preview.exercises[0].entryId, 'A-chest');
});

test('GYM-018: actual reps and weights never mutate the prescribed plan', () => {
  const definition = plan.clone(data.program);
  const workout = plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A');
  workout.exercises[0].sets[0].reps = 7;
  workout.exercises[0].sets[0].weight = 42.5;
  assert.equal(definition.sessions[0].exercises[0].minReps, 10);
  assert.equal(definition.sessions[0].exercises[0].maxReps, 12);
  const otherDay = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A');
  assert.equal(otherDay.exercises[0].sets[0].weight, 60);
  assert.equal(otherDay.exercises[0].sets[0].reps, 12);
});

test('GYM-018: saving updates new and untouched previews but protects recorded snapshots', () => {
  const definition = plan.clone(data.program);
  const active = plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A');
  active.state = 'inProgress';
  active.exercises[0].sets[0].reps = 7;
  const untouched = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A');
  const draft = plan.clone(definition);
  draft.sessions[0].name = 'Strength A';
  Object.assign(draft.sessions[0].exercises[0], { setCount: 4, minReps: 6, maxReps: 8 });
  plan.apply(definition, draft, data.catalog);
  const stillActive = plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A', active);
  assert.strictEqual(stillActive, active);
  assert.equal(stillActive.name, 'Session A');
  assert.equal(stillActive.exercises[0].sets.length, 3);
  assert.equal(stillActive.exercises[0].sets[0].reps, 7);
  const updated = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A', untouched);
  assert.equal(updated.name, 'Strength A');
  assert.equal(updated.exercises[0].sets.length, 4);
  assert.equal(updated.exercises[0].target, '6–8');
  assert.equal(updated.planRevision, 2);
  assert.equal(active.planRevision, 1);
});

test('GYM-018: entries and Paused state keep the snapshot even after template removal', () => {
  const definition = plan.clone(data.program);
  const recorded = plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A');
  recorded.hasEntries = true;
  const paused = plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A');
  paused.state = 'paused';
  const draft = plan.clone(definition);
  draft.dateAssignments['2026-10-20'] = 'session-A';
  assert.equal(plan.removeSession(draft, 'session-A'), true);
  assert.equal(draft.schedule[1], null);
  assert.equal(draft.dateAssignments['2026-10-20'], null);
  plan.apply(definition, draft, data.catalog);
  assert.strictEqual(plan.openOccurrence(definition, data.catalog, '2026-10-12', 'session-A', recorded), recorded);
  assert.strictEqual(plan.openOccurrence(definition, data.catalog, '2026-10-19', 'session-A', paused), paused);
  assert.equal(definition.sessions.find(item => item.id === 'session-B').label, 'B');
  assert.equal(calendar.plan('2026-10-21', definition.schedule, {}), 'session-B');
});

test('GYM-018: invalid prescriptions or dangling assignments cannot replace the saved plan', () => {
  const definition = plan.clone(data.program);
  for (const patch of [{ setCount: 0 }, { setCount: 1.5 }, { minReps: 15, maxReps: 10 }, { maxReps: 51 }, { exerciseId: 'unknown' }]) {
    const draft = plan.clone(definition);
    Object.assign(draft.sessions[0].exercises[0], patch);
    assert.throws(() => plan.apply(definition, draft, data.catalog));
    assert.equal(definition.revision, 1);
  }
  const missing = plan.clone(definition);
  missing.sessions[0].exercises = [];
  assert(plan.validate(missing, data.catalog).length > 0);
  const dangling = plan.clone(definition);
  dangling.schedule[1] = 'removed';
  assert.throws(() => plan.apply(definition, dangling, data.catalog));
});
