'use strict';
// GYM-010: explicit comparable-history filters and workload/performance calculations.
const GymMetrics = (() => {
  const contextKeys = ['exerciseId', 'machine', 'setup', 'unit', 'convention', 'rom', 'rir'];
  const workingSets = record => record.sets.filter(set => set.done && !set.warmup && Number.isInteger(set.reps) && set.reps > 0 && Number.isFinite(set.weight) && set.weight >= 0);
  function comparable(history, context) {
    return history.filter(record => contextKeys.every(key => context[key] !== undefined && record[key] === context[key])).sort((a, b) => a.date.localeCompare(b.date));
  }
  function series(history, context, metric, referenceWeight) {
    return comparable(history, context).map(record => {
      const sets = workingSets(record);
      let values;
      if (metric === 'reps') values = sets.filter(set => set.weight === referenceWeight).map(set => set.reps);
      else if (metric === 'weight') values = sets.filter(set => set.reps === 10).map(set => set.weight);
      else if (metric === 'volume') values = sets.map(set => set.weight * set.reps);
      else throw new Error('Unknown metric');
      return { date: record.date, value: values.length ? (metric === 'volume' ? values.reduce((a, b) => a + b, 0) : Math.max(...values)) : null };
    });
  }
  // GYM-010: expose only recorded contexts and qualifying reference loads.
  const contexts = (history, base) => [...new Set(history.filter(record => contextKeys.every(key => key === 'setup' || (base[key] !== undefined && record[key] === base[key]))).map(record => record.setup))].map(setup => ({ ...base, setup }));
  const referenceWeights = (history, context) => [...new Set(comparable(history, context).flatMap(workingSets).map(set => set.weight))].sort((a, b) => a - b);
  const countSets = (history, context) => comparable(history, context).reduce((sum, record) => sum + workingSets(record).length, 0);
  return { series, countSets, comparable, contexts, referenceWeights };
})();
if (typeof module !== 'undefined') module.exports = GymMetrics;
