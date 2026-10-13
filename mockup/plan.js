'use strict';
// GYM-018: editable prescriptions and independent dated execution snapshots.
const GymPlan = (() => {
  const clone = value => structuredClone(value);
  function validate(plan, catalog) {
    const errors = [];
    const text = (value, max) => typeof value === 'string' && value.trim().length > 0 && value.trim().length <= max;
    if (!text(plan.name, 80)) errors.push('Give the plan a name of 1–80 characters.');
    if (!Array.isArray(plan.sessions) || plan.sessions.length < 1 || plan.sessions.length > 7) errors.push('Keep between 1 and 7 session templates.');
    const sessions = plan.sessions ?? [];
    const ids = new Set(sessions.map(item => item.id));
    if (ids.size !== sessions.length) errors.push('Session identities must be unique.');
    const catalogIds = new Set(catalog.map(item => item.id));
    sessions.forEach((session, index) => {
      if (!text(session.name, 60)) errors.push(`Session ${index + 1} needs a name of 1–60 characters.`);
      if (!session.exercises?.length) errors.push(`${session.name || 'This session'} needs at least one exercise.`);
      session.exercises?.forEach((item, exerciseIndex) => {
        const prefix = `${session.name || 'Session'}, exercise ${exerciseIndex + 1}`;
        if (!catalogIds.has(item.exerciseId)) errors.push(`${prefix}: choose a catalog exercise.`);
        if (!Number.isInteger(item.setCount) || item.setCount < 1 || item.setCount > 10) errors.push(`${prefix}: working sets must be 1–10.`);
        if (!Number.isInteger(item.minReps) || !Number.isInteger(item.maxReps) || item.minReps < 1 || item.maxReps > 50 || item.minReps > item.maxReps) errors.push(`${prefix}: reps must form a whole-number range from 1 to 50.`);
      });
    });
    Object.values(plan.schedule ?? {}).forEach(id => { if (id !== null && !ids.has(id)) errors.push('A weekday refers to a removed session.'); });
    Object.entries(plan.dateAssignments ?? {}).forEach(([date, id]) => {
      const parsed = new Date(`${date}T12:00:00Z`);
      if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !Number.isFinite(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date) errors.push('Choose a valid date assignment.');
      if (id !== null && !ids.has(id)) errors.push('A date refers to a removed session.');
    });
    return errors;
  }
  function apply(program, draft, catalog) {
    const errors = validate(draft, catalog);
    if (errors.length) throw new Error(errors.join(' '));
    const revision = program.revision + 1;
    Object.assign(program, clone(draft), { revision });
  }
  function removeSession(draft, id) {
    if (draft.sessions.length <= 1) return false;
    draft.sessions = draft.sessions.filter(item => item.id !== id);
    for (const key of Object.keys(draft.schedule)) if (draft.schedule[key] === id) draft.schedule[key] = null;
    for (const date of Object.keys(draft.dateAssignments)) if (draft.dateAssignments[date] === id) draft.dateAssignments[date] = null;
    return true;
  }
  const locked = instance => instance && (instance.state !== 'ready' || instance.hasEntries);
  function openOccurrence(program, catalog, date, templateId, existing) {
    if (locked(existing)) return existing;
    const template = program.sessions.find(item => item.id === templateId);
    if (!template) throw new Error('This session template is no longer in the plan.');
    return {
      date, templateId, programName: program.name, planRevision: program.revision,
      name: template.name, focus: template.focus, label: template.label,
      state: 'ready', hasEntries: false,
      exercises: template.exercises.map(item => {
        const machine = catalog.find(entry => entry.id === item.exerciseId);
        return {
          ...clone(machine), entryId: item.entryId,
          target: item.minReps === item.maxReps ? String(item.minReps) : `${item.minReps}–${item.maxReps}`,
          sets: Array.from({ length: item.setCount }, () => ({ reps: item.maxReps, weight: machine.weight, done: false }))
        };
      })
    };
  }
  return { clone, validate, apply, removeSession, locked, openOccurrence };
})();
if (typeof module !== 'undefined') module.exports = GymPlan;
