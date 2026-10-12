const { chromium, expect } = require('playwright/test');
const assert = require('node:assert/strict');
const { pathToFileURL } = require('node:url');
const path = require('node:path');
const fs = require('node:fs/promises');

(async () => {
  const previews = process.env.PREVIEW_DIR || path.join(require('node:os').tmpdir(), 'gym-assystant-previews');
  await fs.mkdir(previews, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.addInitScript(() => {
      const realNow = Date.now;
      window.clockOffset = 0;
      Date.now = () => realNow() + window.clockOffset;
    });
    await page.goto(pathToFileURL(path.resolve(__dirname, '../mockup/index.html')).href);
    // GYM-007, GYM-008, GYM-015: branding, three sessions, bottom navigation.
    await expect(page).toHaveTitle(/gym assystant/);
    await expect(page.locator('[data-session]')).toHaveCount(3);
    const navBox = await page.locator('.main-nav').boundingBox();
    assert(navBox.y > 700 && navBox.y + navBox.height <= 844);
    await page.screenshot({ path: path.join(previews, 'program.png') });
    await page.locator('[data-session="0"]').click();
    await expect(page.locator('#sessionName')).toHaveText('Session A');
    await page.locator('[data-exercise="0"]').click();
    await expect(page.locator('#prescription')).toContainText('3 working sets');
    // GYM-003, GYM-004, GYM-005, GYM-009: logging, validation, and hierarchy.
    const workReps = page.locator('[data-kind="sets"][data-set="0"][data-field="reps"]');
    const workWeight = page.locator('[data-kind="sets"][data-set="0"][data-field="weight"]');
    await workReps.fill('8');
    await workWeight.fill('42.5');
    await page.locator('[data-kind="sets"][data-toggle="0"]').click();
    await expect(page.locator('#setCount')).toHaveText('1 of 3 complete');
    for (const invalid of ['-2', '2.5', '']) {
      await workReps.fill(invalid);
      await workWeight.click();
      await expect(workReps).toHaveValue('8');
    }
    await workWeight.fill('-1');
    await workReps.click();
    await expect(workWeight).toHaveValue('42.5');
    // GYM-014: warm-up and working rows occupy the same list; counts stay distinct.
    await page.locator('#addWarmup').click();
    await expect(page.locator('#sets .set-row')).toHaveCount(4);
    await expect(page.locator('#sets .set-kind small').first()).toHaveText('WARM-UP');
    const warmReps = page.locator('[data-kind="warmups"][data-field="reps"]');
    await warmReps.fill('15');
    await page.locator('[data-kind="warmups"][data-field="weight"]').fill('20');
    await page.locator('[data-kind="warmups"][data-toggle="0"]').click();
    await expect(page.locator('#setCount')).toHaveText('1 of 3 complete');
    // GYM-012: execution pause/resume and recovery completion.
    await page.locator('#timerToggle').click();
    await page.evaluate(() => { window.clockOffset += 3500; });
    await expect(page.locator('#timerValue')).toHaveText('00:03');
    await page.locator('#timerToggle').click();
    await expect(page.locator('#timerStatus')).toHaveText('Paused');
    await page.evaluate(() => { window.clockOffset += 10000; });
    await expect(page.locator('#timerValue')).toHaveText('00:03');
    await page.locator('#recoveryMode').click();
    await page.locator('#recoverySeconds').fill('2');
    await page.locator('#timerToggle').click();
    await expect(page.locator('#recoverySeconds')).toBeDisabled();
    await page.evaluate(() => { window.clockOffset += 3000; });
    await expect(page.locator('#timerValue')).toHaveText('00:00');
    await expect(page.locator('#timerStatus')).toContainText('Recovery complete');
    await page.locator('#timerReset').click();
    await expect(page.locator('#timerValue')).toHaveText('00:02');
    await page.locator('#executionMode').click();
    await expect(page.locator('#timerValue')).toHaveText('00:03');
    await page.locator('#timerReset').click();
    await expect(page.locator('#timerValue')).toHaveText('00:00');
    await page.locator('#seatInput').fill('Seat 7');
    // GYM-002: secondary access, matching video panel, Escape dismisses.
    assert(await page.evaluate(() => document.getElementById('sets').compareDocumentPosition(document.getElementById('watchVideo')) & Node.DOCUMENT_POSITION_FOLLOWING));
    await page.locator('#watchVideo').click();
    await expect(page.locator('#videoTitle')).toHaveText('Leg press');
    await page.keyboard.press('Escape');
    await expect(page.locator('#videoDialog')).not.toBeVisible();
    // GYM-001, GYM-003, GYM-014: exercise/session state isolation and retention.
    await page.locator('#back').click();
    await expect(page.locator('#sessionCount')).toHaveText('1 / 9 working sets done');
    await page.locator('[data-exercise="1"]').click();
    await expect(page.locator('#seatInput')).toHaveValue('Seat 3');
    await expect(page.locator('.warmup-row')).toHaveCount(0);
    await page.locator('#back').click();
    await page.locator('[data-exercise="0"]').click();
    await expect(page.locator('#seatInput')).toHaveValue('Seat 7');
    await expect(warmReps).toHaveValue('15');
    await expect(workWeight).toHaveValue('42.5');
    await page.locator('#programNav').click();
    await page.locator('[data-session="1"]').click();
    await page.locator('[data-exercise="0"]').click();
    await expect(page.locator('#exerciseName')).toHaveText('Chest press');
    await expect(page.locator('#prescription')).toContainText('8–10');
    await expect(page.locator('#seatInput')).toHaveValue('Seat 3');
    // GYM-010: metric switch and historical values ignore current-session edits.
    await page.locator('#progressNav').click();
    await expect(page.locator('#metricValue')).toHaveText('12');
    await expect(page.locator('#workingSets')).toHaveText('12');
    await page.locator('[data-metric="weight"]').click();
    await expect(page.locator('#metricValue')).toHaveText('65');
    await page.locator('[data-metric="volume"]').click();
    await expect(page.locator('#metricValue')).toHaveText('1,850');
    await page.locator('#progressExercise').selectOption('1');
    await expect(page.locator('#metricValue')).toHaveText('650');
    await page.locator('[data-metric="reps"]').click();
    await expect(page.locator('#metricTitle')).toHaveText('Reps at 20 kg');
    await page.screenshot({ path: path.join(previews, 'progress.png') });
    // GYM-006, GYM-015: narrow widths, target sizes, and persistent bottom navigation.
    for (const width of [320, 390, 430]) {
      await page.setViewportSize({ width, height: 844 });
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      const badTargets = await page.evaluate(() => [...document.querySelectorAll('button,input,select')].filter(el => el.getClientRects().length && !el.disabled).map(el => ({ id: el.id, rect: el.getBoundingClientRect() })).filter(item => item.rect.width < 47.9 || item.rect.height < 47.9).map(item => item.id));
      assert.deepEqual(badTargets, [], `Small targets at ${width}px`);
    }
    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('#programNav').click();
    await page.locator('[data-session="0"]').click();
    await page.locator('[data-exercise="0"]').click();
    await expect(page.locator('#seatInput')).toHaveValue('Seat 7');
    await page.locator('#sets [data-remove="0"]').click();
    await expect(page.locator('.warmup-row')).toHaveCount(0);
    await page.locator('#addWarmup').click();
    await page.locator('[data-kind="warmups"][data-field="reps"]').fill('15');
    await page.locator('[data-kind="warmups"][data-field="weight"]').fill('20');
    await page.locator('[data-kind="warmups"][data-toggle="0"]').click();
    await page.evaluate(() => { document.querySelector('.screen-area').scrollTop = 0; });
    await page.screenshot({ path: path.join(previews, 'exercise.png') });
    const bottom = await page.locator('.main-nav').boundingBox();
    assert(bottom.y > 700);
    await page.locator('#timerReset').click();
    const scrolledBottom = await page.locator('.main-nav').boundingBox();
    assert.equal(scrolledBottom.y, bottom.y);
    const smallButtons = await page.evaluate(() => [...document.querySelectorAll('#exercise button,#exercise input')].filter(el => el.getClientRects().length && !el.disabled).filter(el => { const r = el.getBoundingClientRect(); return r.height < 47.9 || r.width < 47.9; }).map(el => el.id || el.className));
    assert.deepEqual(smallButtons, []);
    await page.setViewportSize({ width: 1280, height: 950 });
    await page.locator('#programNav').click();
    await page.screenshot({ path: path.join(previews, 'desktop.png') });
    await page.reload();
    await page.locator('[data-session="0"]').click();
    await page.locator('[data-exercise="0"]').click();
    await expect(page.locator('#seatInput')).toHaveValue('Seat 4 · backrest 2');
    await expect(page.locator('.warmup-row')).toHaveCount(0);
    await expect(page.locator('#timerValue')).toHaveText('00:00');
    assert.deepEqual(errors, []);
    console.log('PASS: program navigation, set logging/validation, unified warm-ups, timers, secondary video, progress metrics, state isolation, mobile widths, bottom navigation, 48px targets, reload reset.');
    console.log(`Previews: ${previews}`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
