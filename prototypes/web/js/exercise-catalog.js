'use strict';
// GYM-028: local definition creation only; prescriptions and logs remain separate.
const GymExerciseCatalog = (() => {
  function create(catalog, input, id) {
    const text = (value, min, max) => typeof value === 'string' && value.trim().length >= min && value.trim().length <= max;
    if (!text(input?.name, 1, 80)) throw new Error('Give the exercise a name of 1–80 characters.');
    if (!text(input?.machine, 1, 80)) throw new Error('Give the machine or movement variant a name of 1–80 characters.');
    const seat = input.seat === undefined ? '' : input.seat;
    if (!text(seat, 0, 160)) throw new Error('Keep the default seat/setup within 160 characters.');
    if (typeof id !== 'string' || !id.trim()) throw new Error('An exercise needs a nonempty stable identity.');
    if (catalog.some(exercise => exercise.id === id)) throw new Error('Exercise identities must be unique.');
    const definition = {
      id, name: input.name.trim(), machine: input.machine.trim(), seat: seat.trim(),
      weight: 0, reps: 0, custom: true, category: 'CUSTOM'
    };
    catalog.push(definition);
    return definition;
  }
  return { create };
})();
if (typeof module !== 'undefined') module.exports = GymExerciseCatalog;
