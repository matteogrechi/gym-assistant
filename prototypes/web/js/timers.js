'use strict';
// GYM-012: independent clock-based stopwatch/countdown; timestamps are injectable for verification.
const GymTimers = (() => {
  const create = (duration = 90) => ({ mode: 'execution', duration, execution: { elapsed: 0, startedAt: null }, recovery: { elapsed: 0, startedAt: null } });
  const elapsed = (timer, now) => timer.elapsed + (timer.startedAt === null ? 0 : Math.max(0, now - timer.startedAt));
  function start(state, mode, now) {
    const timer = state[mode];
    if (timer.startedAt !== null) return;
    if (mode === 'recovery' && elapsed(timer, now) >= state.duration * 1000) timer.elapsed = 0;
    timer.startedAt = now;
  }
  function pause(state, mode, now) {
    const timer = state[mode];
    timer.elapsed = elapsed(timer, now);
    timer.startedAt = null;
    if (mode === 'recovery') timer.elapsed = Math.min(timer.elapsed, state.duration * 1000);
  }
  function reset(state, mode) { state[mode].elapsed = 0; state[mode].startedAt = null; }
  function read(state, mode, now) {
    const ms = elapsed(state[mode], now);
    if (mode === 'recovery' && ms >= state.duration * 1000) { pause(state, mode, now); return 0; }
    return mode === 'execution' ? Math.floor(ms / 1000) : Math.ceil((state.duration * 1000 - ms) / 1000);
  }
  return { create, start, pause, reset, read };
})();
if (typeof module !== 'undefined') module.exports = GymTimers;
