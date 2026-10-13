'use strict';
// GYM-010: display existing observations; geometry never creates new metrics.
const GymCharts = (() => {
  function layout(series, integer = false) {
    if (!series.length) return { points: [], segments: [], ticks: [], maximum: 1, minimumWidth: 0 };
    const maximumValue = Math.max(1, ...series.map(point => Number.isFinite(point.value) && point.value >= 0 ? point.value : 0));
    const roughStep = maximumValue / 4;
    const magnitude = 10 ** Math.floor(Math.log10(roughStep));
    const step = Math.max(integer ? 1 : 0, [1, 2, 5, 10].find(value => value * magnitude >= roughStep) * magnitude);
    const roundedMaximum = Math.ceil(maximumValue / step) * step;
    const maximum = Number.isFinite(roundedMaximum) ? roundedMaximum : maximumValue;
    const ticks = Array.from({ length: Math.floor(maximum / step) + 1 }, (_, index) => index * step).reverse();
    const dates = series.map(point => Date.parse(`${point.date}T00:00:00Z`));
    const span = dates.at(-1) - dates[0];
    const spacing = dates.slice(1).map((date, index) => date - dates[index]).filter(value => value > 0);
    const minimumWidth = spacing.length ? 48 * span / Math.min(...spacing) : 48;
    const points = series.map((point, index) => {
      const value = Number.isFinite(point.value) && point.value >= 0 ? point.value : null;
      return { ...point, value, x: span ? (dates[index] - dates[0]) / span : .5, y: value === null ? null : 1 - value / maximum };
    });
    const segments = [];
    let current = [];
    for (const point of points) {
      if (point.y === null) { if (current.length) segments.push(current); current = []; }
      else current.push(point);
    }
    if (current.length) segments.push(current);
    return { points, segments, ticks, maximum, minimumWidth };
  }
  function render(container, model, { unit, selectedDate, format, dateLabel }) {
    container.replaceChildren();
    if (!model.points.length) {
      const empty = document.createElement('p');
      empty.className = 'chart-empty'; empty.textContent = 'No comparable sessions for this exercise.';
      container.append(empty);
      return;
    }
    const axis = document.createElement('div');
    axis.className = 'chart-scale'; axis.setAttribute('aria-hidden', 'true');
    for (const tick of model.ticks) {
      const label = document.createElement('span');
      label.style.top = `${(1 - tick / model.maximum) * 100}%`; label.textContent = format(tick);
      axis.append(label);
    }
    const timeline = document.createElement('div'); timeline.className = 'chart-timeline';
    const content = document.createElement('div'); content.className = 'chart-timeline-content'; content.style.minWidth = `${model.minimumWidth + 48}px`;
    const plot = document.createElement('div'); plot.className = 'chart-plot';
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 1000 180'); svg.setAttribute('preserveAspectRatio', 'none'); svg.setAttribute('aria-hidden', 'true');
    for (const tick of model.ticks) {
      const line = document.createElementNS(svg.namespaceURI, 'line');
      const y = (1 - tick / model.maximum) * 180;
      line.setAttribute('x1', '0'); line.setAttribute('x2', '1000'); line.setAttribute('y1', y); line.setAttribute('y2', y);
      line.setAttribute('class', tick === 0 ? 'chart-baseline' : 'chart-gridline'); svg.append(line);
    }
    for (const segment of model.segments.filter(points => points.length > 1)) {
      const line = document.createElementNS(svg.namespaceURI, 'polyline');
      line.setAttribute('points', segment.map(point => `${point.x * 1000},${point.y * 180}`).join(' '));
      line.setAttribute('class', 'chart-trend'); svg.append(line);
    }
    plot.append(svg);
    const dates = document.createElement('div'); dates.className = 'chart-date-axis'; dates.setAttribute('aria-hidden', 'true');
    for (const point of model.points) {
      const button = document.createElement('button');
      button.type = 'button'; button.className = `md-icon-button chart-point${point.value === null ? ' is-gap' : ''}`;
      button.dataset.observationDate = point.date;
      button.style.left = `${point.x * 100}%`; button.style.top = point.y === null ? 'calc(100% + 12px)' : `${point.y * 100}%`;
      button.setAttribute('aria-label', `${dateLabel(point.date, true)}: ${point.value === null ? 'No comparable set' : `${format(point.value)} ${unit}`}`);
      button.setAttribute('aria-pressed', String(point.date === selectedDate));
      button.setAttribute('aria-describedby', 'chartHelp');
      const mark = document.createElement('span'); mark.className = 'chart-point-mark'; mark.setAttribute('aria-hidden', 'true');
      if (point.value === null) mark.textContent = '—';
      button.append(mark); plot.append(button);
      const date = document.createElement('span'); date.style.left = `${point.x * 100}%`; date.textContent = dateLabel(point.date); dates.append(date);
    }
    content.append(plot, dates); timeline.append(content); container.append(axis, timeline);
  }
  return { layout, render };
})();
if (typeof module !== 'undefined') module.exports = GymCharts;
