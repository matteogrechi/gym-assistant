const test = require('node:test');
const assert = require('node:assert/strict');
const catalogApi = require('../js/exercise-catalog.js');
const data = require('../js/data.js');
const plan = require('../js/plan.js');
const metrics = require('../js/metrics.js');
const exportsApi = require('../js/exports.js');
const timers = require('../js/timers.js');

const input = { name: '  Rematore à  ', machine: '  Cable 02  ', seat: '  Seat 5  ' };
const fixture = () => {
  const catalog = structuredClone(data.catalog);
  const definition = catalogApi.create(catalog, input, 'custom-test');
  const program = plan.clone(data.program);
  program.sessions[0].exercises = [{ entryId: 'custom-entry', exerciseId: definition.id, setCount: 3, minReps: 8, maxReps: 12, recoverySeconds: 60 }];
  return { catalog, definition, program };
};

test('GYM-028: creation trims user text, preserves the supplied stable ID and leaves existing data intact', () => {
  const catalog = structuredClone(data.catalog);
  const original = structuredClone(catalog);
  const fields = structuredClone(input);
  const history = structuredClone(data.history);
  const definition = catalogApi.create(catalog, fields, 'custom-fixed-uuid');
  assert.deepEqual(definition, {
    id: 'custom-fixed-uuid', name: 'Rematore à', machine: 'Cable 02', seat: 'Seat 5',
    weight: 0, reps: 0, custom: true, category: 'CUSTOM'
  });
  assert.strictEqual(catalog.at(-1), definition);
  assert.deepEqual(catalog.slice(0, -1), original);
  assert.deepEqual(fields, input);
  fields.name = 'Changed input';
  assert.equal(definition.name, 'Rematore à');
  assert.deepEqual(data.history, history);
});

test('GYM-028: trimmed field limits and optional empty setup are accepted', () => {
  const catalog = [];
  const maximum = catalogApi.create(catalog, { name: ` ${'n'.repeat(80)} `, machine: ` ${'m'.repeat(80)} `, seat: ` ${'s'.repeat(160)} ` }, 'custom-max');
  assert.equal(maximum.name.length, 80);
  assert.equal(maximum.machine.length, 80);
  assert.equal(maximum.seat.length, 160);
  for (const [index, fields] of [{ name: 'n', machine: 'm' }, { name: 'n', machine: 'm', seat: '   ' }].entries()) {
    assert.equal(catalogApi.create(catalog, fields, `custom-empty-${index}`).seat, '');
  }
});

test('GYM-028: invalid fields or IDs and collisions fail without any catalog mutation', () => {
  const catalog = structuredClone(data.catalog);
  const existing = [...catalog];
  const before = structuredClone(catalog);
  const invalid = [null, undefined, {}, ...['name', 'machine'].flatMap(field => [
    { ...input, [field]: '' }, { ...input, [field]: ' \n ' },
    { ...input, [field]: 'x'.repeat(81) }, { ...input, [field]: 12 }, { ...input, [field]: null }
  ]), { ...input, seat: 'x'.repeat(161) }, { ...input, seat: null }, { ...input, seat: 5 }];
  for (const fields of invalid) {
    assert.throws(() => catalogApi.create(catalog, fields, 'custom-invalid'));
    assert.deepEqual(catalog, before);
  }
  for (const id of ['', ' \n ', undefined, null, 42, catalog[0].id]) {
    assert.throws(() => catalogApi.create(catalog, input, id));
    assert.deepEqual(catalog, before);
  }
  existing.forEach((definition, index) => assert.strictEqual(catalog[index], definition));
  const created = catalogApi.create(catalog, input, 'custom-once');
  const after = structuredClone(catalog);
  assert.throws(() => catalogApi.create(catalog, { ...input, name: 'Another exercise' }, created.id), /unique/);
  assert.deepEqual(catalog, after);
});

test('GYM-028, GYM-010: duplicate names and matching setup never merge distinct exercise IDs', () => {
  const catalog = structuredClone(data.catalog);
  const fields = { name: catalog[0].name, machine: catalog[0].machine, seat: catalog[0].seat };
  const first = catalogApi.create(catalog, fields, 'custom-first');
  const second = catalogApi.create(catalog, fields, 'custom-second');
  assert.notEqual(first.id, second.id);
  assert.equal(first.name, second.name);
  assert.deepEqual(metrics.comparable(data.history, data.context(first)), []);
  const history = [first, second].map((exercise, index) => ({
    ...data.context(exercise), date: '2026-10-13',
    sets: [{ reps: index === 0 ? 10 : 5, weight: 20, done: true }]
  }));
  assert.deepEqual(metrics.comparable(history, data.context(first)), [history[0]]);
  assert.deepEqual(metrics.comparable(history, data.context(second)), [history[1]]);
  assert.deepEqual(metrics.series(history, data.context(first), 'volume', 20), [{ date: '2026-10-13', value: 200 }]);
  assert.deepEqual(metrics.series(history, data.context(second), 'volume', 20), [{ date: '2026-10-13', value: 100 }]);
  assert.equal(metrics.countSets(history, data.context(first)), 1);
});

test('GYM-028: custom comparison context remains unknown and sample contexts stay unchanged', () => {
  const { definition } = fixture();
  assert.deepEqual(data.context(definition), {
    exerciseId: definition.id, machine: definition.machine, setup: definition.seat,
    unit: 'kg', convention: 'displayed-machine-load', rom: null, rir: null
  });
  for (const sample of data.catalog) {
    assert.deepEqual(data.context(sample), {
      exerciseId: sample.id, machine: sample.machine, setup: sample.seat,
      unit: 'kg', convention: 'displayed-machine-load', rom: 'sample-full-rom', rir: 2
    });
  }
  assert.deepEqual(metrics.series(data.history, data.context(definition), 'reps', 0), []);
  assert.equal(metrics.countSets(data.history, data.context(definition)), 0);
});

test('GYM-028, GYM-018: custom occurrences keep prescriptions but begin with isolated zero unrecorded sets/setup', () => {
  const { catalog, definition, program } = fixture();
  assert.deepEqual(plan.validate(program, catalog), []);
  const first = plan.openOccurrence(program, catalog, '2026-10-13', 'session-A');
  const second = plan.openOccurrence(program, catalog, '2026-10-14', 'session-A');
  const exercise = first.exercises[0];
  assert.equal(exercise.id, definition.id);
  assert.equal(exercise.entryId, 'custom-entry');
  assert.equal(exercise.seat, definition.seat);
  assert.equal(exercise.target, '8–12');
  assert.equal(exercise.recoverySeconds, 60);
  assert.deepEqual(exercise.sets, Array.from({ length: 3 }, () => ({ reps: 0, weight: 0, done: false })));
  assert.equal(first.hasEntries, false);
  assert.equal(first.state, 'ready');
  exercise.seat = 'Actual seat 7';
  exercise.sets[0] = { reps: 7, weight: 15, done: true };
  assert.equal(definition.seat, 'Seat 5');
  assert.equal(second.exercises[0].seat, 'Seat 5');
  assert.deepEqual(exercise.sets[1], { reps: 0, weight: 0, done: false });
  assert.deepEqual(second.exercises[0].sets[0], { reps: 0, weight: 0, done: false });
  assert.equal(program.sessions[0].exercises[0].maxReps, 12);
  program.sessions[0].exercises[0].maxReps = 15;
  assert.equal(first.prescribedExercises[0].maxReps, 12);
  const sample = plan.openOccurrence(data.program, data.catalog, '2026-10-13', 'session-A');
  assert.deepEqual(sample.exercises[0].sets[0], { reps: 12, weight: 60, done: false });
});

test('GYM-028, GYM-018: protected custom occurrences survive plan changes and template removal', () => {
  for (const protection of [{ state: 'inProgress' }, { state: 'paused' }, { state: 'finished' }, { hasEntries: true }, { orderChanged: true }]) {
    const { catalog, definition, program } = fixture();
    const occurrence = plan.openOccurrence(program, catalog, '2026-10-13', 'session-A');
    Object.assign(occurrence, protection);
    occurrence.exercises[0].seat = 'Occurrence setup';
    const before = structuredClone(occurrence);
    const draft = plan.clone(program);
    plan.removeSession(draft, 'session-A');
    plan.apply(program, draft, catalog);
    assert.strictEqual(plan.openOccurrence(program, catalog, occurrence.date, 'session-A', occurrence), occurrence);
    assert.deepEqual(occurrence, before);
    assert.equal(definition.seat, 'Seat 5');
  }
});

test('GYM-028, GYM-024: version-2 JSON retains custom definitions and saved/draft/execution references without history', () => {
  const { catalog, definition, program } = fixture();
  const draft = plan.clone(program);
  draft.name = 'Unsaved draft';
  const session = plan.openOccurrence(program, catalog, '2026-10-13', 'session-A');
  session.exercises.forEach(exercise => { exercise.timers = timers.create(exercise.recoverySeconds); exercise.warmups = []; });
  session.exercises[0].seat = 'Actual setup';
  const source = {
    settings: {}, program, plans: [program], activePlanId: program.id, drafts: [draft], catalog,
    sessions: new Map([['2026-10-13:session-A', session]]), pastPlans: [],
    history: structuredClone(data.history), attendanceHistory: structuredClone(data.attendanceHistory), timeZone: 'Europe/Rome'
  };
  const before = structuredClone(source);
  const decoded = JSON.parse(exportsApi.json(source, 10000));
  assert.equal(decoded.formatVersion, 2);
  assert.deepEqual(decoded.exerciseCatalog.find(exercise => exercise.id === definition.id), definition);
  assert.equal(decoded.savedPlans[0].sessions[0].exercises[0].exerciseId, definition.id);
  assert.equal(decoded.unsavedPlanDrafts[0].sessions[0].exercises[0].exerciseId, definition.id);
  assert.equal(decoded.executionSessions[0].prescribedExercises[0].exerciseId, definition.id);
  assert.equal(decoded.executionSessions[0].exercises[0].id, definition.id);
  assert.equal(decoded.executionSessions[0].exercises[0].seat, 'Actual setup');
  assert.equal(decoded.exerciseCatalog.find(exercise => exercise.id === definition.id).seat, 'Seat 5');
  assert.deepEqual(decoded.executionSessions[0].exercises[0].sets, Array.from({ length: 3 }, () => ({ reps: 0, weight: 0, done: false })));
  assert.deepEqual(decoded.samplePerformanceHistory, data.history);
  assert.deepEqual(decoded.sampleAttendanceHistory, data.attendanceHistory);
  assert.equal(decoded.samplePerformanceHistory.some(record => record.exerciseId === definition.id), false);
  assert.match(decoded.dataDictionary.exerciseCatalog, /custom=true/);
  assert.match(decoded.dataDictionary.context, /unknown \(null\)/);
  assert.match(exportsApi.guide(), /Additive version-2 custom=true/);
  assert.deepEqual(source, before);
});
