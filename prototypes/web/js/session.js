'use strict';
/* global GymTimers */
// GYM-017: explicit legal lifecycle transitions, separate from set completion/attendance.
const GymSession = (() => {
  const timers = typeof module !== 'undefined' ? require('./timers.js') : GymTimers;
  const labels = { ready: 'Ready', inProgress: 'In progress', paused: 'Paused', finished: 'Finished' };
  const transitions = {
    ready: { start: 'inProgress' },
    inProgress: { pause: 'paused', finish: 'finished' },
    paused: { resume: 'inProgress', finish: 'finished' },
    finished: {}
  };
  function transition(instance, action, now) {
    const next = transitions[instance.state]?.[action];
    if (!next) return false;
    if (action === 'pause' || action === 'finish') instance.exercises.forEach(item => {
      timers.pause(item.timers, 'execution', now);
      timers.pause(item.timers, 'recovery', now);
    });
    instance.state = next;
    if (action === 'finish') instance.finishedAt = now;
    return true;
  }
  return { labels, transition };
})();
if (typeof module !== 'undefined') module.exports = GymSession;
