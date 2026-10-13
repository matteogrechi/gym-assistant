'use strict';
// GYM-001, GYM-003, GYM-004, GYM-008, GYM-010, GYM-016: fictional templates and history.
const GymData = (() => {
  const catalog = [
    { id: 'leg', name: 'Leg press', category: 'LOWER BODY', machine: 'Leg press 01', seat: 'Seat 4 · backrest 2', weight: 60, reps: 12 },
    { id: 'chest', name: 'Chest press', category: 'UPPER BODY', machine: 'Chest press 01', seat: 'Seat 3', weight: 20, reps: 10 },
    { id: 'lat', name: 'Lat pulldown', category: 'UPPER BODY', machine: 'Pulldown 01', seat: 'Thigh pad 2', weight: 30, reps: 12 }
  ];
  // GYM-028: local definitions have no fictional ROM or effort observations.
  const context = exercise => ({ exerciseId: exercise.id, machine: exercise.machine, setup: exercise.seat, unit: 'kg', convention: 'displayed-machine-load', rom: exercise.custom ? null : 'sample-full-rom', rir: exercise.custom ? null : 2 });
  // GYM-018: templates contain prescriptions only, never actual logs or timers.
  const program = {
    id: 'foundations', name: 'Full body foundations', revision: 1,
    sessions: ['A', 'B', 'C'].map((letter, index) => ({
      id: `session-${letter}`, label: letter, name: `Session ${letter}`, focus: ['Lower body first', 'Push first', 'Pull first'][index],
      exercises: [...catalog.slice(index), ...catalog.slice(0, index)].map(exercise => ({
        entryId: `${letter}-${exercise.id}`, exerciseId: exercise.id,
        setCount: 3, minReps: index === 1 ? 8 : 10, maxReps: index === 1 ? 10 : 12, recoverySeconds: 90
      }))
    })),
    schedule: { 1: 'session-A', 3: 'session-B', 5: 'session-C' }, dateAssignments: {}
  };
  // GYM-026: a second fictional program demonstrates browsing and activation.
  const routine = {
    id: 'two-day', name: 'Two-day routine', revision: 1,
    sessions: ['A', 'B'].map((letter, index) => ({
      id: `routine-${letter}`, label: letter, name: index === 0 ? 'Upper body' : 'Lower body', focus: '',
      exercises: (index === 0 ? catalog.slice(1) : [catalog[0], catalog[1]]).map(exercise => ({
        entryId: `routine-${letter}-${exercise.id}`, exerciseId: exercise.id, setCount: 2, minReps: 8, maxReps: 10, recoverySeconds: 90
      }))
    })), schedule: { 1: 'routine-A', 4: 'routine-B' }, dateAssignments: {}
  };
  const library = { plans: [program, routine], activePlanId: program.id };
  const dates = ['2026-09-14', '2026-09-21', '2026-09-28', '2026-10-05'];
  const history = dates.flatMap((date, index) => catalog.map(exercise => ({
    ...context(exercise), date, programId: program.id, templateId: 'session-A', occurrenceId: `sample-${date}`,
    sets: [
      { reps: 9 + index, weight: exercise.weight, done: true, warmup: false },
      { reps: 10, weight: exercise.weight - 10 + index * 5, done: true, warmup: false },
      { reps: 8, weight: exercise.weight, done: true, warmup: false }
    ]
  })));
  // GYM-020: additional Session B visits have attendance only, no invented set logs.
  const attendanceHistory = [
    ...dates.map(date => ({ programId: program.id, plannedDate: date, actualDate: date, templateId: 'session-A' })),
    { programId: program.id, plannedDate: '2026-09-16', actualDate: '2026-09-17', templateId: 'session-B' },
    { programId: program.id, plannedDate: '2026-09-23', actualDate: '2026-09-25', templateId: 'session-B' }
  ];
  return { catalog, library, history, attendanceHistory, context, get program() { return library.plans.find(item => item.id === library.activePlanId); }, get schedule() { return this.program.schedule; } };
})();
if (typeof module !== 'undefined') module.exports = GymData;
