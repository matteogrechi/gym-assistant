'use strict';
/* global GymCalendar */
// GYM-020, GYM-026: original-program planned cohorts and observed attendance are distinct from performance.
const GymAttendance = (() => {
  const calendar = typeof module !== 'undefined' ? require('./calendar.js') : GymCalendar;
  const key = (date, templateId) => `${date}:${templateId}`;
  function planned(program, today, includeToday = false) {
    const records = [];
    for (let offset = -28; offset < (includeToday ? 1 : 0); offset++) {
      const date = calendar.addDays(today, offset);
      const templateId = calendar.plan(date, program.schedule, program.dateAssignments);
      const template = program.sessions.find(item => item.id === templateId);
      if (template) records.push({ id: key(date, templateId), plannedDate: date, templateId, programId: program.id, template: structuredClone(template), programName: program.name, planRevision: program.revision });
    }
    return records;
  }
  const validSet = set => set.done && Number.isInteger(set.reps) && set.reps > 0 && Number.isFinite(set.weight) && set.weight >= 0;
  function observe(instance, actualDate) {
    // Preview activity cannot create a real attendance observation.
    if (instance.date > actualDate || instance.attendedDate || !instance.exercises.some(item => item.sets.some(validSet))) return false;
    instance.attendedDate = actualDate;
    return true;
  }
  function observations(history, instances) {
    const byId = new Map(history.map(item => [key(item.plannedDate, item.templateId), { ...item }]));
    instances.forEach(item => {
      if (item.attendedDate) {
        const id = key(item.date, item.templateId);
        // A repeat visit to the same occurrence never replaces its first observation.
        if (!byId.has(id)) byId.set(id, { plannedDate: item.date, templateId: item.templateId, programId: item.programId, actualDate: item.attendedDate });
      }
    });
    return byId;
  }
  function pending(records, observed, today) {
    return records.filter(item => item.plannedDate >= calendar.addDays(today, -28) && item.plannedDate < today && !(observed.get(item.id)?.actualDate <= today)).sort((a, b) => a.plannedDate.localeCompare(b.plannedDate));
  }
  function summarize(records, observed, today) {
    const cohort = records.filter(item => item.plannedDate >= calendar.addDays(today, -28) && item.plannedDate < today);
    let onTime = 0, late = 0, early = 0, delay = 0;
    cohort.forEach(item => {
      const actual = observed.get(item.id)?.actualDate;
      if (!actual || actual > today) return;
      const days = Math.round((Date.parse(`${actual}T12:00:00Z`) - Date.parse(`${item.plannedDate}T12:00:00Z`)) / 86400000);
      if (days > 0) { late++; delay += days; }
      else if (days < 0) early++;
      else onTime++;
    });
    const logged = onTime + late + early;
    return { planned: cohort.length, logged, onTime, late, early, unlogged: cohort.length - logged, percent: cohort.length ? logged / cohort.length * 100 : null, meanDelay: late ? delay / late : null };
  }
  return { key, planned, validSet, observe, observations, pending, summarize };
})();
if (typeof module !== 'undefined') module.exports = GymAttendance;
