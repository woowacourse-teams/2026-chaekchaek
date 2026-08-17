import assert from 'node:assert/strict';
import { resolveActivePair, resolvePair } from './book-physics.mjs';

const dragged = { x: 0, y: 0, w: 100, h: 100, vx: 500, vy: 0, bouncesLeft: 1, dragging: true };
const other = { x: 50, y: 0, w: 100, h: 100, vx: 0, vy: 0, bouncesLeft: 1, dragging: false };

assert.equal(resolvePair(dragged, other), true);
assert.ok(other.x > 50, 'the dragged book should push an overlapping book away');
assert.ok(other.vx > 0, 'the dragged book should transfer its velocity');

const first = { x: 0, y: 0, w: 100, h: 100, vx: 500, vy: 0, bouncesLeft: 1, dragging: false };
const second = { x: 50, y: 0, w: 100, h: 100, vx: 0, vy: 0, bouncesLeft: 1, dragging: false };
resolvePair(first, second);
assert.equal(first.bouncesLeft, 0);
assert.equal(second.bouncesLeft, 0);

first.x = 0;
second.x = 50;
first.vx = 500;
second.vx = 0;
resolvePair(first, second);
assert.equal(first.vx, second.vx, 'later collisions should have no elastic rebound');

const sleeping = {
  x: 0,
  y: 0,
  w: 100,
  h: 100,
  vx: 0,
  vy: 0,
  bouncesLeft: 1,
  active: false,
  dragging: false,
};
const neighbor = {
  x: 50,
  y: 0,
  w: 100,
  h: 100,
  vx: 0,
  vy: 0,
  bouncesLeft: 1,
  active: false,
  dragging: false,
};
assert.equal(resolveActivePair(sleeping, neighbor), false);
assert.equal(neighbor.x, 50, 'two inactive overlapping books should keep their initial positions');

sleeping.active = true;
assert.equal(resolveActivePair(sleeping, neighbor), true);
assert.equal(neighbor.active, true, 'contact with an active book should wake its neighbor');
