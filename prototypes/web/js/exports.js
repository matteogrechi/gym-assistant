'use strict';
/* global GymMetrics, GymAttendance, GymCalendar */
// GYM-023, GYM-024, GYM-026, GYM-028: version-2 library snapshots and the existing statistics, without changing live data.
const GymExports = (() => {
  const metrics = typeof module !== 'undefined' ? require('./metrics.js') : GymMetrics;
  const attendance = typeof module !== 'undefined' ? require('./attendance.js') : GymAttendance;
  const calendar = typeof module !== 'undefined' ? require('./calendar.js') : GymCalendar;
  const columns = ['format_version', 'record_type', 'source', 'program_id', 'exercise_id', 'machine', 'setup', 'load_convention', 'rom', 'rir', 'occurrence_id', 'template_id', 'planned_date', 'actual_date', 'period_start', 'period_end', 'as_of_date', 'metric', 'reference_weight_kg', 'value', 'unit', 'missing_reason'];
  const dictionary = {
    encoding: 'UTF-8; JSON RFC 8259; CSV quoted comma-separated cells and CRLF records (RFC 4180).',
    dates: 'Civil dates YYYY-MM-DD; exportedAt and finishedAt are UTC ISO timestamps; timeZone is the device IANA zone.',
    provenance: 'All data is demo data. samplePerformanceHistory/sampleAttendanceHistory are fictional; executionSessions are this browser visit.',
    savedPlans: 'All saved programs, each with stable id and reusable templates/prescriptions with weekday keys 0=Sunday through 6=Saturday and date overrides; null assignment means no session.',
    activePlanId: 'One saved program ID supplies untouched calendar dates; protected workouts keep their original program.',
    unsavedPlanDrafts: 'Separate drafts by plan id, including unsaved new programs; never applied by export. Empty array when absent.',
    exerciseCatalog: 'Stable exercise IDs, names, machine/movement variants and default seat/setup. Additive version-2 custom=true identifies locally created definitions (absent for existing samples); category=CUSTOM and weight/reps=0 are unrecorded defaults, never observed performance. Duplicate names keep distinct IDs; plan exerciseId and execution exercise id refer to these definitions.',
    pastPlannedSessions: 'Original planned dates and frozen template/program-revision snapshots; preserve these even after saved-plan edits or deletion.',
    attendance: 'sampleAttendanceHistory contains fictional planned/actual visits; executionSessions.attendedDate records this visit’s demo attendance. Only a completed working set with positive whole-number reps and finite nonnegative kg establishes attendance, once per occurrence. Warm-ups, future previews and Start alone do not.',
    executionSessions: 'programId identifies the original program; globally unique template IDs keep occurrenceId=date:templateId stable. date is planned date; attendedDate is first valid working-set attendance day; finishedAt ends a session, not proof all sets are done.',
    exercises: 'Array order is execution order; entryId is stable identity; seat is an independent copy of the catalog default setup; target is original rep range; prescribedExercises holds original working-set counts/ranges. Custom sets begin with reps=0, weight=0 and done=false; occurrence notes never rewrite catalog defaults.',
    recoverySeconds: 'Planned recovery per exercise: whole seconds from 1 to 3600, default 90 when absent in older files. Original prescribedExercises and execution exercises retain the occurrence target; timers.duration preserves its countdown seconds through plan changes.',
    sets: 'sets=working sets; warmups=warm-up sets. reps is a whole number; weight is kg; done is completion. Warm-ups do not count in performance or attendance.',
    context: 'Historical exerciseId/machine/setup/unit/convention/rom/rir define comparability; sample rom=sample-full-rom and rir=2 remain fictional sample context. Custom ROM/effort are unknown (null), with no invented sample history. No actual effort collection is implemented.',
    lifecycle: 'ready, inProgress, paused, finished; finished can be partial. Never restart timers just because a session is inProgress.',
    timers: 'duration in seconds; execution/recovery elapsed in milliseconds; startedAt is null in exported snapshots. wasRunningAtExport records prior status. Restore paused; do not count time away.',
    settings: 'language/theme requested choices. saveFolderPreview is a demo label, not a granted storage permission or a real save destination.',
    missing: 'JSON null / CSV empty values mean unavailable, not zero. CSV missing_reason explains gaps.',
    csvColumns: columns.join(','),
    csvMetrics: 'reps_at_reference_weight (reps), weight_at_10_reps (kg), volume_load (kg-reps), completed_working_sets (sets); attendance_status (on_time/late/early/unlogged); planned/logged/on_time/late/early/unlogged_sessions (sessions), logged_percent (percent), mean_delay_days (days).',
    csvPrograms: 'program_id retains original plan ownership for per-record data; attendance_summary spans the historical cohort across plans and leaves program_id blank. No retroactive obligations are invented when activating another plan.',
    csvPeriods: 'Performance uses sample actual dates; current edits excluded. Attendance uses the past 28 complete planned days and observations through as_of_date.',
    versions: 'Version 2 exploratory contract; v1 savedPlan becomes savedPlans=[savedPlan], its id becomes activePlanId, and non-null unsavedPlanDraft becomes a one-element unsavedPlanDrafts array. Preserve originals and original plan ownership. Future incompatible changes require a new version and published migration; keep originals. No app or account required to read these files.'
  };
  function snapshot(input, now = Date.now()) {
    const sessions = structuredClone([...input.sessions.values()]);
    sessions.forEach(item => {
      item.occurrenceId = attendance.key(item.date, item.templateId);
      if (item.finishedAt !== undefined) item.finishedAt = new Date(item.finishedAt).toISOString();
      item.exercises.forEach(exercise => {
        const timers = exercise.timers;
        for (const mode of ['execution', 'recovery']) {
          const timer = timers[mode];
          timer.wasRunningAtExport = timer.startedAt !== null;
          timer.elapsed += timer.startedAt === null ? 0 : Math.max(0, now - timer.startedAt);
          if (mode === 'recovery') timer.elapsed = Math.min(timer.elapsed, timers.duration * 1000);
          timer.startedAt = null;
        }
      });
    });
    return {
      format: 'gym-assystant-status', formatVersion: 2,
      exportedAt: new Date(now).toISOString(), timeZone: input.timeZone,
      dataKind: 'prototype_demo', dataDictionary: dictionary,
      settings: structuredClone(input.settings), savedPlans: structuredClone(input.plans ?? [input.program]),
      activePlanId: input.activePlanId ?? input.program.id,
      unsavedPlanDrafts: structuredClone(input.drafts ?? (input.draft ? [input.draft] : [])),
      exerciseCatalog: structuredClone(input.catalog), executionSessions: sessions,
      pastPlannedSessions: structuredClone(input.pastPlans),
      samplePerformanceHistory: structuredClone(input.history), sampleAttendanceHistory: structuredClone(input.attendanceHistory)
    };
  }
  function json(input, now) {
    return JSON.stringify(snapshot(input, now), (key, value) => {
      if (typeof value === 'number' && !Number.isFinite(value)) throw new Error('Cannot export a non-finite number.');
      return value;
    }, 2) + '\n';
  }
  const csv = rows => [columns, ...rows.map(row => columns.map(key => row[key] ?? ''))].map(row => row.map(value => '"' + String(value).replaceAll('"', '""') + '"').join(',')).join('\r\n') + '\r\n';
  function statistics(input) {
    const rows = [];
    const add = row => rows.push({ format_version: 2, ...row });
    input.catalog.forEach(exercise => {
      const context = input.context(exercise);
      const common = { record_type: 'performance', source: 'fictional_sample', exercise_id: exercise.id, machine: context.machine, setup: context.setup, load_convention: context.convention, rom: context.rom, rir: context.rir };
      for (const [metric, name, unit] of [['reps', 'reps_at_reference_weight', 'reps'], ['weight', 'weight_at_10_reps', 'kg'], ['volume', 'volume_load', 'kg-reps']]) {
        const records = metrics.comparable(input.history, context);
        metrics.series(input.history, context, metric, exercise.weight).forEach((point, index) => {
          const value = point.value;
          add({ ...common, occurrence_id: records[index].occurrenceId, template_id: records[index].templateId,
            program_id: records[index].programId, actual_date: point.date, metric: name, reference_weight_kg: metric === 'reps' ? exercise.weight : '',
            value: value === null || !Number.isFinite(value) ? '' : value, unit,
            missing_reason: value === null ? 'no_comparable_set' : !Number.isFinite(value) ? 'numeric_overflow' : '' });
        });
      }
      const records = metrics.comparable(input.history, context);
      add({ ...common, program_id: new Set(records.map(item => item.programId)).size === 1 ? records[0]?.programId : '', period_start: records[0]?.date, period_end: records.at(-1)?.date, metric: 'completed_working_sets', value: metrics.countSets(input.history, context), unit: 'sets' });
    });
    const observed = attendance.observations(input.attendanceHistory, input.sessions);
    const period = { period_start: calendar.addDays(input.today, -28), period_end: calendar.addDays(input.today, -1), as_of_date: input.today };
    input.pastPlans.filter(item => item.plannedDate >= period.period_start && item.plannedDate <= period.period_end).forEach(item => {
      const observation = observed.get(item.id);
      const actual = observation?.actualDate;
      const known = actual && actual <= input.today;
      const sample = input.attendanceHistory.some(record => attendance.key(record.plannedDate, record.templateId) === item.id);
      add({ record_type: 'attendance', source: known ? sample ? 'fictional_sample' : 'this_visit_demo' : 'planned_demo',
        program_id: item.programId, occurrence_id: item.id, template_id: item.templateId, planned_date: item.plannedDate, actual_date: actual ?? '', ...period,
        metric: 'attendance_status', value: !known ? 'unlogged' : actual === item.plannedDate ? 'on_time' : actual > item.plannedDate ? 'late' : 'early', unit: 'status', missing_reason: !known ? 'attendance_unknown_as_of_date' : '' });
    });
    const summary = attendance.summarize(input.pastPlans, observed, input.today);
    for (const [key, metric, unit] of [['planned', 'planned_sessions', 'sessions'], ['logged', 'logged_sessions', 'sessions'], ['onTime', 'on_time_sessions', 'sessions'], ['late', 'late_sessions', 'sessions'], ['early', 'early_sessions', 'sessions'], ['unlogged', 'unlogged_sessions', 'sessions'], ['percent', 'logged_percent', 'percent'], ['meanDelay', 'mean_delay_days', 'days']]) {
      add({ record_type: 'attendance_summary', source: 'sample_and_this_visit_demo', ...period, metric, value: summary[key], unit, missing_reason: summary[key] === null ? key === 'percent' ? 'no_planned_sessions' : 'no_late_sessions' : '' });
    }
    return csv(rows);
  }
  function guide() {
    return 'gym assystant — recover your data without the app\nFormat version 2; UTF-8 text files. All saved plans and separate drafts are included. All prototype files are DEMO DATA.\n\nStatus JSON is the complete backup. CSV contains derived statistics only.\nOpen JSON with any text editor; open CSV in a spreadsheet using UTF-8 and comma separation. Keep the original JSON.\n\nWith standard Python (no app libraries):\nimport json\nfrom pathlib import Path\nbackup = json.loads(Path("status.json").read_text(encoding="utf-8"))\nfor session in backup["executionSessions"]:\n    for exercise in session["exercises"]:\n        print(session["date"], session.get("attendedDate"), exercise["name"], exercise["seat"])\n        for kind in ("warmups", "sets"):\n            for row in exercise[kind]:\n                print(kind, row["reps"], row["weight"], "kg", row["done"])\n\nDATA DICTIONARY\n' + Object.entries(dictionary).map(([key, value]) => `${key}: ${value}`).join('\n\n') + '\n\nThe prototype has no import/restore or automatic folder save. Downloads go to the browser destination, not the preview folder.\n';
  }
  return { snapshot, json, statistics, csv, columns, guide };
})();
if (typeof module !== 'undefined') module.exports = GymExports;
