'use strict';
// GYM-016: civil dates and sample plans, independent of time-of-day or DST offsets.
const GymCalendar = (() => {
  const pad = value => String(value).padStart(2, '0');
  const today = () => {
    const now = new Date(Date.now());
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  };
  const parse = date => new Date(`${date}T12:00:00Z`);
  function addDays(date, count) {
    const value = parse(date);
    value.setUTCDate(value.getUTCDate() + count);
    return `${value.getUTCFullYear()}-${pad(value.getUTCMonth() + 1)}-${pad(value.getUTCDate())}`;
  }
  function week(date) {
    const first = addDays(date, -((parse(date).getUTCDay() + 6) % 7));
    return Array.from({ length: 7 }, (_, index) => addDays(first, index));
  }
  function plan(date, schedule, overrides) {
    return Object.prototype.hasOwnProperty.call(overrides, date) ? overrides[date] : schedule[parse(date).getUTCDay()] ?? null;
  }
  function upcoming(date, schedule, overrides, limit = 3) {
    const entries = [];
    for (let day = 1; day <= 28 && entries.length < limit; day++) {
      const next = addDays(date, day);
      const sessionId = plan(next, schedule, overrides);
      if (sessionId !== null) entries.push({ date: next, sessionId });
    }
    return entries;
  }
  const label = (date, options) => parse(date).toLocaleDateString('en-GB', { ...options, timeZone: 'UTC' });
  return { today, addDays, week, plan, upcoming, label };
})();
if (typeof module !== 'undefined') module.exports = GymCalendar;
