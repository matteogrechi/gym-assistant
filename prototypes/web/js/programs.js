'use strict';
/* global GymPlan */
// GYM-026: distinct saved programs, explicit activation and validated per-plan commits.
const GymPrograms = (() => {
  const plan = typeof module !== 'undefined' ? require('./plan.js') : GymPlan;
  function create(catalog, id) {
    return { id, name: 'New plan', revision: 1, sessions: [{
      id: `${id}-session-A`, label: 'A', name: 'Session A', focus: '',
      exercises: [{ entryId: `${id}-A-1`, exerciseId: catalog[0].id, setCount: 3, minReps: 10, maxReps: 12, recoverySeconds: 90 }]
    }], schedule: {}, dateAssignments: {} };
  }
  function commit(library, draft, catalog) {
    const errors = plan.validate(draft, catalog);
    if (typeof draft.id !== 'string' || !draft.id) errors.push('A plan needs a stable identity.');
    const other = library.plans.filter(item => item.id !== draft.id);
    const sessionIds = new Set(other.flatMap(item => item.sessions.map(session => session.id)));
    const entryIds = new Set(other.flatMap(item => item.sessions.flatMap(session => session.exercises.map(exercise => exercise.entryId))));
    const draftEntries = draft.sessions.flatMap(session => session.exercises.map(exercise => exercise.entryId));
    if (draft.sessions.some(session => sessionIds.has(session.id))) errors.push('Session identities must be unique across plans.');
    if (new Set(draftEntries).size !== draftEntries.length || draftEntries.some(id => entryIds.has(id))) errors.push('Exercise entry identities must be unique across plans.');
    if (errors.length) throw new Error(errors.join(' '));
    const saved = library.plans.find(item => item.id === draft.id);
    if (saved) plan.apply(saved, draft, catalog);
    else library.plans.push(plan.clone(draft));
    return library.plans.find(item => item.id === draft.id);
  }
  function activate(library, id, hasUnsavedChanges = false) {
    if (!library.plans.some(item => item.id === id)) throw new Error('Save this plan before using it.');
    if (hasUnsavedChanges) throw new Error('Save this plan’s changes before using it.');
    library.activePlanId = id;
  }
  return { create, commit, activate };
})();
if (typeof module !== 'undefined') module.exports = GymPrograms;
