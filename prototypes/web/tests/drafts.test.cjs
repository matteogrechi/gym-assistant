const test = require('node:test');
const assert = require('node:assert/strict');
const programs = require('../js/programs.js');

test('discard isolates a never-saved new draft from saved plans, other drafts and active identity', () => {
  const library = { plans: [{ id: 'active' }, { id: 'saved' }], activePlanId: 'active' };
  const wanted = { draft: { id: 'wanted', name: 'Keep me' }, dirty: true };
  const savedEdits = { draft: { id: 'saved', name: 'Uncommitted edits' }, dirty: true };
  const drafts = new Map([['wanted', wanted], ['unwanted', { draft: { id: 'unwanted' } }], ['saved', savedEdits]]);
  const before = structuredClone(library);
  assert.equal(programs.discardDraft(library, drafts, 'saved'), false);
  assert.equal(programs.discardDraft(library, drafts, 'active'), false);
  assert.equal(programs.discardDraft(library, drafts, 'missing'), false);
  assert.equal(programs.discardDraft(library, drafts, 'unwanted'), true);
  assert.equal(programs.discardDraft(library, drafts, 'unwanted'), false);
  assert.deepEqual(library, before);
  assert.equal(drafts.get('wanted'), wanted);
  assert.equal(drafts.get('saved'), savedEdits);
  assert.equal(drafts.has('unwanted'), false);
});
