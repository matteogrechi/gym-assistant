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
  function render(container, model, { unit, selectedDate, format, dateLabel, view = { zoom: 1, scrollLeft: 0 }, onInspect = () => {} }) {
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
    const guide = document.createElement('div'); guide.className = 'chart-inspection-guide'; guide.setAttribute('aria-hidden', 'true'); plot.append(guide);
    const dates = document.createElement('div'); dates.className = 'chart-date-axis'; dates.setAttribute('aria-hidden', 'true');
    for (const point of model.points) {
      const button = document.createElement('button');
      button.type = 'button'; button.className = `md-icon-button chart-point${point.value === null ? ' is-gap' : ''}`;
      button.dataset.observationDate = point.date;
      button.style.left = `${point.x * 100}%`; button.style.top = point.y === null ? 'calc(100% - 24px)' : `${point.y * 100}%`;
      button.setAttribute('aria-label', `${dateLabel(point.date, true)}: ${point.value === null ? 'No comparable set' : `${format(point.value)} ${unit}`}`);
      button.setAttribute('aria-pressed', String(point.date === selectedDate));
      button.setAttribute('aria-describedby', 'chartHelp');
      const mark = document.createElement('span'); mark.className = 'chart-point-mark'; mark.setAttribute('aria-hidden', 'true');
      if (point.value === null) mark.textContent = '—';
      button.append(mark); plot.append(button);
      const date = document.createElement('span'); date.style.left = `${point.x * 100}%`; date.textContent = dateLabel(point.date); dates.append(date);
    }
    content.append(plot, dates); timeline.append(content); container.append(axis, timeline);
    // GYM-010: horizontal exploration is transient UI state, independent of metrics.
    const controls = document.createElement('div'); controls.className = 'chart-exploration'; controls.setAttribute('role', 'group'); controls.setAttribute('aria-label', 'Explore session dates');
    const makeControl = (id, label, path) => {
      const button = document.createElement('button'); button.type = 'button'; button.id = id;
      button.className = 'md-icon-button md-icon-outlined'; button.setAttribute('aria-label', label);
      const icon = document.createElementNS(svg.namespaceURI, 'svg'); icon.setAttribute('class', 'button-icon'); icon.setAttribute('viewBox', '0 0 24 24'); icon.setAttribute('aria-hidden', 'true'); icon.setAttribute('focusable', 'false');
      const shape = document.createElementNS(svg.namespaceURI, 'path'); shape.setAttribute('d', path); icon.append(shape); button.append(icon);
      controls.append(button); return button;
    };
    const zoomOut = makeControl('chartZoomOut', 'Zoom out', 'M5 12h14');
    const status = document.createElement('span'); status.id = 'chartZoomStatus'; status.setAttribute('role', 'status'); status.setAttribute('aria-live', 'polite'); status.setAttribute('aria-atomic', 'true'); controls.append(status);
    const zoomIn = makeControl('chartZoomIn', 'Zoom in', 'M5 12h14M12 5v14');
    const reset = makeControl('chartResetView', 'Reset view', 'M3 10a9 9 0 1 1 2 8M3 4v6h6');
    container.append(controls);
    const zooms = [1, 1.5, 2, 3, 4];
    if (model.points.length === 1) view.zoom = 1;
    view.zoom = zooms.includes(view.zoom) ? view.zoom : 1;
    const listeners = new AbortController();
    const listen = (element, type, handler) => element.addEventListener(type, handler, { signal: listeners.signal });
    let baseWidth = 0;
    let frame = 0;
    let gesture = null;
    let suppressClick = false;
    let disposed = false;
    function setStatus() {
      const index = zooms.indexOf(view.zoom);
      zoomOut.disabled = model.points.length < 2 || index === 0;
      zoomIn.disabled = model.points.length < 2 || index === zooms.length - 1;
      reset.disabled = model.points.length < 2;
      status.textContent = `Zoom ${view.zoom * 100}%`;
    }
    function select(date) {
      selectedDate = date;
      const point = model.points.find(item => item.date === date);
      guide.hidden = !point;
      if (point) guide.style.left = `${point.x * 100}%`;
    }
    function reveal() {
      const button = [...plot.querySelectorAll('[data-observation-date]')].find(item => item.dataset.observationDate === selectedDate);
      if (!button || !timeline.clientWidth) return;
      const left = button.offsetLeft;
      // The plot's 24px inset exactly contains half of each point's target.
      if (left < timeline.scrollLeft) timeline.scrollLeft = left;
      else if (left + 48 > timeline.scrollLeft + timeline.clientWidth) timeline.scrollLeft = left + 48 - timeline.clientWidth;
      view.scrollLeft = timeline.scrollLeft;
    }
    function resize() {
      if (disposed || !timeline.clientWidth) return;
      baseWidth = Math.max(model.minimumWidth, timeline.clientWidth - 48);
      content.style.width = `${baseWidth * view.zoom + 48}px`;
      timeline.scrollLeft = view.scrollLeft;
      view.scrollLeft = timeline.scrollLeft;
      // Reveal once per exploration view; rerenders retain deliberate panning.
      if (!view.selectionRevealed) {
        reveal();
        view.selectionRevealed = true;
      }
    }
    function zoom(value, resetView = false) {
      cancelGesture();
      if (!timeline.clientWidth) return;
      const center = (timeline.scrollLeft + timeline.clientWidth / 2 - 24) / (baseWidth * view.zoom);
      view.zoom = value;
      content.style.width = `${baseWidth * view.zoom + 48}px`;
      timeline.scrollLeft = resetView ? 0 : center * baseWidth * view.zoom + 24 - timeline.clientWidth / 2;
      view.scrollLeft = timeline.scrollLeft;
      setStatus();
      if (resetView) reveal();
    }
    function cancelGesture() {
      const previous = gesture;
      gesture = null;
      plot.classList.remove('is-scrubbing'); dates.classList.remove('is-panning');
      if (previous?.element.hasPointerCapture(previous.id)) previous.element.releasePointerCapture(previous.id);
      if (previous?.target.hasPointerCapture(previous.id)) previous.target.releasePointerCapture(previous.id);
    }
    function inspect(clientX) {
      const bounds = plot.getBoundingClientRect();
      const x = Math.max(0, Math.min(1, (clientX - bounds.left) / bounds.width));
      const nearest = model.points.reduce((best, point) => Math.abs(point.x - x) < Math.abs(best.x - x) ? point : best);
      onInspect(nearest.date);
    }
    function start(event, pan) {
      if (!event.isPrimary || event.button !== 0 || (pan && event.pointerType !== 'mouse')) return;
      cancelGesture(); suppressClick = false;
      gesture = { id: event.pointerId, element: event.currentTarget, target: event.target, x: event.clientX, y: event.clientY, scroll: timeline.scrollLeft, dragging: false, pan };
    }
    function move(event) {
      if (!gesture || gesture.id !== event.pointerId) return;
      const dx = event.clientX - gesture.x;
      const dy = event.clientY - gesture.y;
      if (!gesture.dragging) {
        if (Math.abs(dy) > 6 && Math.abs(dy) > Math.abs(dx)) { cancelGesture(); return; }
        if (Math.abs(dx) < 6 || Math.abs(dx) <= Math.abs(dy)) return;
        gesture.dragging = true; suppressClick = true;
        gesture.element.setPointerCapture(event.pointerId);
        gesture.element.classList.add(gesture.pan ? 'is-panning' : 'is-scrubbing');
      }
      event.preventDefault();
      if (gesture.pan) {
        timeline.scrollLeft = gesture.scroll - dx;
        view.scrollLeft = timeline.scrollLeft;
      } else inspect(event.clientX);
    }
    function end(event) {
      if (!gesture || gesture.id !== event.pointerId) return;
      if (!gesture.pan && event.type === 'pointerup') inspect(event.clientX);
      if (event.type !== 'pointerup') suppressClick = false;
      cancelGesture();
    }
    for (const element of [plot, dates]) {
      listen(element, 'pointerdown', event => start(event, element === dates));
      listen(element, 'pointermove', move);
      listen(element, 'pointerup', end);
      listen(element, 'pointercancel', end);
      listen(element, 'lostpointercapture', event => { if (event.target === element) end(event); });
      listen(element, 'pointerleave', event => { if (!element.hasPointerCapture(event.pointerId)) end(event); });
      listen(element, 'click', event => { if (suppressClick) { event.preventDefault(); event.stopPropagation(); suppressClick = false; } });
    }
    listen(timeline, 'scroll', () => { if (timeline.clientWidth) view.scrollLeft = timeline.scrollLeft; });
    listen(zoomIn, 'click', () => zoom(zooms[Math.min(zooms.length - 1, zooms.indexOf(view.zoom) + 1)]));
    listen(zoomOut, 'click', () => zoom(zooms[Math.max(0, zooms.indexOf(view.zoom) - 1)]));
    listen(reset, 'click', () => zoom(1, true));
    const observer = new ResizeObserver(resize); observer.observe(timeline);
    resize();
    frame = requestAnimationFrame(resize);
    select(selectedDate); setStatus();
    return {
      select, reveal, resize, cancelGesture,
      dispose() {
        if (timeline.clientWidth) view.scrollLeft = timeline.scrollLeft;
        disposed = true; cancelGesture(); cancelAnimationFrame(frame); observer.disconnect(); listeners.abort();
      }
    };
  }
  return { layout, render };
})();
if (typeof module !== 'undefined') module.exports = GymCharts;
