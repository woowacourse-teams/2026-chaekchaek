const RESTITUTION = 0.72;
const MAX_SPEED = 700;
const THROW_SPEED = 0.5;

export const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

function limitSpeed(book) {
  const speed = Math.hypot(book.vx, book.vy);
  if (speed <= MAX_SPEED) return;
  book.vx *= MAX_SPEED / speed;
  book.vy *= MAX_SPEED / speed;
}

function hitBox(book) {
  // ponytail: rotated SAT is unnecessary for 20 covers; upgrade if corner-accurate collisions matter.
  const insetX = book.w * 0.175;
  const insetY = book.h * 0.05;
  return {
    left: book.x + insetX,
    right: book.x + book.w - insetX,
    top: book.y + insetY,
    bottom: book.y + book.h - insetY,
  };
}

export function resolvePair(a, b) {
  const boxA = hitBox(a);
  const boxB = hitBox(b);
  const overlapX = Math.min(boxA.right, boxB.right) - Math.max(boxA.left, boxB.left);
  const overlapY = Math.min(boxA.bottom, boxB.bottom) - Math.max(boxA.top, boxB.top);
  if (overlapX <= 0 || overlapY <= 0) return false;

  let nx = 0;
  let ny = 0;
  let overlap = overlapY;
  if (overlapX < overlapY) {
    nx = a.x + a.w / 2 < b.x + b.w / 2 ? 1 : -1;
    overlap = overlapX;
  } else {
    ny = a.y + a.h / 2 < b.y + b.h / 2 ? 1 : -1;
  }

  const massA = a.dragging ? 0 : 1;
  const massB = b.dragging ? 0 : 1;
  const totalMass = massA + massB;
  if (!totalMass) return false;

  a.x -= (nx * overlap * massA) / totalMass;
  a.y -= (ny * overlap * massA) / totalMass;
  b.x += (nx * overlap * massB) / totalMass;
  b.y += (ny * overlap * massB) / totalMass;

  const relativeSpeed = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
  if (relativeSpeed < 0) {
    const canBounce = !a.dragging && !b.dragging && a.bouncesLeft > 0 && b.bouncesLeft > 0;
    const impulse = (-(1 + (canBounce ? RESTITUTION : 0)) * relativeSpeed) / totalMass;
    a.vx -= impulse * nx * massA;
    a.vy -= impulse * ny * massA;
    b.vx += impulse * nx * massB;
    b.vy += impulse * ny * massB;
    limitSpeed(a);
    limitSpeed(b);
    if (canBounce) {
      a.bouncesLeft--;
      b.bouncesLeft--;
    }
  }
  return true;
}

export function resolveActivePair(a, b) {
  if (!a.active && !b.active) return false;
  if (!resolvePair(a, b)) return false;
  a.active = true;
  b.active = true;
  return true;
}

export function init() {
  const stage = document.querySelector('[data-pencil-name="다크 흩어진 책 웹 홈"]');

  if (!stage) return;

  const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const books = [...stage.children].map((el, index) => {
    const style = getComputedStyle(el);
    const matrix = new DOMMatrixReadOnly(style.transform);
    const book = {
      el,
      x: parseFloat(style.left),
      y: parseFloat(style.top),
      w: parseFloat(style.width),
      h: parseFloat(style.height),
      angle: Math.atan2(matrix.b, matrix.a),
      vx: 0,
      vy: 0,
      bouncesLeft: 1,
      active: false,
      dragging: false,
      history: [],
    };
    el.style.left = '0';
    el.style.top = '0';
    el.style.transformOrigin = 'top left';
    el.tabIndex = 0;
    el.setAttribute('role', 'button');
    el.setAttribute('aria-label', `책 ${index + 1}: 드래그해서 던지기`);
    return book;
  });

  let started = false;
  let topZ = books.length;

  function point(event) {
    const rect = stage.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  }

  function render(book) {
    book.el.style.transform = `translate3d(${book.x}px, ${book.y}px, 0) rotate(${book.angle}rad)`;
  }

  function keepInside(book) {
    const maxX = stage.clientWidth - book.w;
    const maxY = stage.clientHeight - book.h;
    if (book.dragging) {
      book.x = clamp(book.x, 0, maxX);
      book.y = clamp(book.y, 0, maxY);
      return;
    }
    if (book.x < 0 || book.x > maxX) {
      book.x = clamp(book.x, 0, maxX);
      book.vx *= book.bouncesLeft > 0 ? -RESTITUTION : 0;
      if (book.bouncesLeft > 0) book.bouncesLeft--;
    }
    if (book.y < 0 || book.y > maxY) {
      book.y = clamp(book.y, 0, maxY);
      book.vy *= book.bouncesLeft > 0 ? -RESTITUTION : 0;
      if (book.bouncesLeft > 0) book.bouncesLeft--;
    }
  }

  books.forEach((book) => {
    let pointerId;
    let offsetX = 0;
    let offsetY = 0;

    book.el.addEventListener('pointerdown', (event) => {
      if (event.button !== 0) return;
      event.preventDefault();
      const p = point(event);
      pointerId = event.pointerId;
      offsetX = p.x - book.x;
      offsetY = p.y - book.y;
      book.dragging = true;
      book.active = true;
      book.vx = 0;
      book.vy = 0;
      books.forEach((item) => {
        item.bouncesLeft = 1;
      });
      book.history = [{ x: book.x, y: book.y, time: performance.now() }];
      book.el.style.zIndex = String(++topZ);
      book.el.setPointerCapture(pointerId);
      started = true;
    });

    book.el.addEventListener('pointermove', (event) => {
      if (!book.dragging || event.pointerId !== pointerId) return;
      const p = point(event);
      const now = performance.now();
      const previous = book.history.at(-1);
      book.x = clamp(p.x - offsetX, 0, stage.clientWidth - book.w);
      book.y = clamp(p.y - offsetY, 0, stage.clientHeight - book.h);
      const elapsed = Math.max(now - previous.time, 1) / 1000;
      book.vx = (book.x - previous.x) / elapsed;
      book.vy = (book.y - previous.y) / elapsed;
      limitSpeed(book);
      book.history.push({ x: book.x, y: book.y, time: now });
      book.history = book.history.filter((sample) => now - sample.time <= 100);
      render(book);
    });

    function release(event) {
      if (!book.dragging || event.pointerId !== pointerId) return;
      const now = performance.now();
      const last = book.history.at(-1);
      if (!last || now - last.time > 80 || reduceMotion) {
        book.vx = 0;
        book.vy = 0;
      } else {
        const first = book.history[0];
        const elapsed = Math.max(last.time - first.time, 1) / 1000;
        book.vx = ((last.x - first.x) / elapsed) * THROW_SPEED;
        book.vy = ((last.y - first.y) / elapsed) * THROW_SPEED;
        limitSpeed(book);
      }
      book.dragging = false;
      book.history = [];
      pointerId = undefined;
    }

    book.el.addEventListener('pointerup', release);
    book.el.addEventListener('pointercancel', release);
    book.el.addEventListener('keydown', (event) => {
      const velocity = 350;
      const directions = {
        ArrowLeft: [-velocity, 0],
        ArrowRight: [velocity, 0],
        ArrowUp: [0, -velocity],
        ArrowDown: [0, velocity],
      };
      if (!directions[event.key]) return;
      event.preventDefault();
      [book.vx, book.vy] = directions[event.key];
      book.active = true;
      books.forEach((item) => {
        item.bouncesLeft = 1;
      });
      book.el.style.zIndex = String(++topZ);
      started = true;
    });
    render(book);
  });

  let previousTime = performance.now();
  function tick(now) {
    const dt = Math.min((now - previousTime) / 1000, 1 / 30);
    previousTime = now;
    if (started) {
      const damping = Math.pow(0.992, dt * 60);
      books.forEach((book) => {
        if (book.dragging) return;
        book.x += book.vx * dt;
        book.y += book.vy * dt;
        book.vx *= damping;
        book.vy *= damping;
        if (Math.abs(book.vx) < 1) book.vx = 0;
        if (Math.abs(book.vy) < 1) book.vy = 0;
        keepInside(book);
      });

      // ponytail: O(n²) is simpler and cheap for 20 books; add a spatial hash at hundreds.
      for (let pass = 0; pass < 2; pass++) {
        for (let i = 0; i < books.length; i++) {
          for (let j = i + 1; j < books.length; j++) resolveActivePair(books[i], books[j]);
        }
      }
      books.forEach((book) => {
        keepInside(book);
        render(book);
      });
    }
    requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}
