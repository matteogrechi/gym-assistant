'use strict';
// GYM-001, GYM-003, GYM-004, GYM-008, GYM-010: fictional templates and history.
const GymData = (() => {
  const catalog = [
    { id: 'leg', name: 'Leg press', category: 'LOWER BODY', machine: 'Leg press 01', seat: 'Seat 4 · backrest 2', weight: 60, reps: 12 },
    { id: 'chest', name: 'Chest press', category: 'UPPER BODY', machine: 'Chest press 01', seat: 'Seat 3', weight: 20, reps: 10 },
    { id: 'lat', name: 'Lat pulldown', category: 'UPPER BODY', machine: 'Pulldown 01', seat: 'Thigh pad 2', weight: 30, reps: 12 }
  ];
  const context = exercise => ({ exerciseId: exercise.id, machine: exercise.machine, setup: exercise.seat, unit: 'kg', convention: 'displayed-machine-load', rom: 'sample-full-rom', rir: 2 });
  const program = {
    id: 'foundations', name: 'Full body foundations',
    sessions: ['A', 'B', 'C'].map((letter, index) => ({
      id: `session-${letter}`, name: `Session ${letter}`, focus: ['Lower body first', 'Push first', 'Pull first'][index],
      exercises: [...catalog.slice(index), ...catalog.slice(0, index)].map(exercise => ({
        ...exercise, target: index === 1 ? '8–10' : '10–12',
        sets: Array.from({ length: 3 }, () => ({ reps: index === 1 ? 10 : exercise.reps, weight: exercise.weight, done: false }))
      }))
    }))
  };
  const dates = ['2026-09-14', '2026-09-21', '2026-09-28', '2026-10-05'];
  const history = dates.flatMap((date, index) => catalog.map(exercise => ({
    ...context(exercise), date, programId: program.id, templateId: 'session-A', occurrenceId: `sample-${date}`,
    sets: [
      { reps: 9 + index, weight: exercise.weight, done: true, warmup: false },
      { reps: 10, weight: exercise.weight - 10 + index * 5, done: true, warmup: false },
      { reps: 8, weight: exercise.weight, done: true, warmup: false }
    ]
  })));
  return { catalog, program, history, context };
})();
if (typeof module !== 'undefined') module.exports = GymData;
