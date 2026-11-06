
function webSVG() {
  const cx = 300;
  const n = 8;
  const rings = 6;
  const ang = i => (95 + i * 10.5) * Math.PI / 180;
  const pt = (a, r) => [
    (cx + Math.cos(a) * r).toFixed(1),
    (Math.sin(a) * r).toFixed(1)
  ];

  let p = '';

  for (let i = 0; i < n; i++) {
    p += `<path pathLength="1" style="--i:${i}" d="M${cx} 0L${pt(ang(i), 460).join(' ')}"/>`;
  }

  for (let k = 1; k <= rings; k++) {
    const r = k * k * 9 + 30;

    for (let i = 0; i < n - 1; i++) {
      const [x1, y1] = pt(ang(i), r);
      const [x2, y2] = pt(ang(i + 1), r);
      const [mx, my] = pt((ang(i) + ang(i + 1)) / 2, r * .86);

      p += `<path pathLength="1" style="--i:${n + k}" d="M${x1} ${y1}Q${mx} ${my} ${x2} ${y2}"/>`;
    }
  }

  return `<svg class="web" viewBox="0 0 300 400" preserveAspectRatio="xMaxYMin slice" aria-hidden="true">${p}</svg>`;
}

const THEMES = {
  default: {
    fx: 'mist',
    glow: '#626aa8',
    title: '#eef0ff',
    art: 'radial-gradient(ellipse at 50% 0%, #242849 0%, #141526 42%, #0a0b10 85%)'
  },
  upside: {
    fx: 'spores',
    glow: '#e0243a',
    title: '#ff5a68',
    art: 'radial-gradient(circle at 50% 0,#4a0a18,#07080d 75%)'
  },
  web: {
    fx: 'web',
    glow: '#e23636',
    art: 'linear-gradient(155deg,#1c3f9e,#6d0f1c)'
  },
  sand: {
    fx: 'sand',
    glow: '#f08a2a',
    art: 'linear-gradient(180deg,#3a2210,#c2622a 70%,#f0a85a)'
  },
  ember: {
    fx: 'embers',
    glow: '#ff6a2a',
    art: 'linear-gradient(180deg,#1a0705,#6b1d0a)'
  },
  space: {
    fx: 'stars',
    glow: '#6aa8ff',
    art: 'radial-gradient(circle at 30% 0,#14264d,#05060d 70%)'
  },
  rain: {
    fx: 'rain',
    glow: '#4a7bd0',
    art: 'linear-gradient(180deg,#0a1020,#1b2a44)'
  },
  code: {
    fx: 'code',
    glow: '#27e07a',
    title: '#5dffa0',
    art: 'linear-gradient(180deg,#02100a,#052b18)'
  },
  snow: {
    fx: 'snow',
    glow: '#bfe3ff',
    art: 'linear-gradient(180deg,#1a2a3a,#4a6178)'
  },
  gold: {
    fx: 'sparks',
    glow: '#f4b942',
    art: 'linear-gradient(180deg,#2a1d07,#6b4a10)'
  }
};

const themeOf = m => {
  const key = String(m?.theme || '').trim().toLowerCase();
  return Object.hasOwn(THEMES, key) ? key : 'default';
};

const FX = {
  mist: {
    n: 38,
    vx: [-0.18, 0.18],
    vy: [-0.35, -0.08],
    r: [0.5, 1.8],
    rgb: '148,158,225',
    alpha: 0.24,
    wob: 0.2,
    glow: 1
  },
  spores: {
    n: 60,
    vx: [-0.1, 0.1],
    vy: [-0.8, -0.2],
    r: [0.6, 2.6],
    rgb: '255,80,90',
    glow: 1,
    wob: 0.35
  },
  embers: {
    n: 50,
    vx: [-0.3, 0.3],
    vy: [-1.6, -0.5],
    r: [0.8, 2.2],
    rgb: '255,140,50',
    glow: 1,
    wob: 0.5
  },
  sparks: {
    n: 40,
    vx: [-0.4, 0.4],
    vy: [-1.2, -0.3],
    r: [0.6, 1.8],
    rgb: '255,215,100',
    glow: 1,
    wob: 0.3
  },
  snow: {
    n: 70,
    vx: [-0.2, 0.2],
    vy: [0.3, 1],
    r: [0.8, 2.4],
    rgb: '230,240,255',
    wob: 0.4
  },
  stars: {
    n: 80,
    vx: [0, 0],
    vy: [0, 0],
    r: [0.4, 1.6],
    rgb: '200,220,255',
    tw: 1
  },
  sand: {
    n: 60,
    vx: [2, 5],
    vy: [-0.2, 0.2],
    r: [0.5, 1.4],
    rgb: '255,196,120',
    line: 3
  },
  rain: {
    n: 60,
    vx: [-0.5, -0.3],
    vy: [9, 14],
    r: [0.5, 1],
    rgb: '150,180,255',
    line: 1.6
  },
  code: {
    n: 45,
    vx: [0, 0],
    vy: [1.5, 4],
    r: [12, 16],
    rgb: '39,224,122',
    glyph: 1
  }
};

function runFx(name) {
  const o = FX[name];

  if (!o) return;

  document.querySelectorAll('canvas.fx').forEach(canvas => {
    canvas.remove();
  });

  const c = document.createElement('canvas');
  const x = c.getContext('2d');

  if (!x) return;

  c.className = 'fx';
  c.style.pointerEvents = 'none';

  document.body.appendChild(c);

  const rnd = ([a, b]) => a + Math.random() * (b - a);

  let W;
  let H;
  let last = 0;
  let on = !document.hidden;

  const size = () => {
    W = c.width = innerWidth;
    H = c.height = innerHeight;
  };

  size();

  addEventListener('resize', size);

  const ps = Array.from({length: o.n}, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    vx: rnd(o.vx),
    vy: rnd(o.vy),
    r: rnd(o.r),
    ph: Math.random() * 6
  }));

  const tick = t => {
    if (!c.isConnected) return;

    if (on && t - last > 33) {
      last = t;

      x.clearRect(0, 0, W, H);

      for (const p of ps) {
        p.ph += 0.03;
        p.x += p.vx + (o.wob ? Math.sin(p.ph) * o.wob : 0);
        p.y += p.vy;

        if (p.x > W + 20) p.x = -20;
        if (p.x < -20) p.x = W + 20;
        if (p.y > H + 20) p.y = -20;
        if (p.y < -20) p.y = H + 20;

        const baseAlpha = o.tw
          ? 0.3 + 0.7 * Math.abs(Math.sin(p.ph))
          : 0.45 + 0.45 * Math.sin(p.ph) ** 2;

        const alpha = baseAlpha * (o.alpha ?? 1);

        x.fillStyle = x.strokeStyle =
          `rgba(${o.rgb},${alpha})`;

        if (o.glyph) {
          x.font = `${p.r}px monospace`;

          x.fillText(
            String.fromCharCode(
              0x30A0 + (Math.random() * 60 | 0)
            ),
            p.x,
            p.y
          );
        } else if (o.line) {
          x.lineWidth = p.r;

          x.beginPath();
          x.moveTo(p.x, p.y);
          x.lineTo(
            p.x - p.vx * o.line,
            p.y - p.vy * o.line
          );
          x.stroke();
        } else {
          x.shadowColor = o.glow
            ? `rgb(${o.rgb})`
            : 'transparent';

          x.shadowBlur = o.glow ? 10 : 0;

          x.beginPath();
          x.arc(p.x, p.y, p.r, 0, Math.PI * 2);
          x.fill();
        }
      }
    }

    requestAnimationFrame(tick);
  };

  document.addEventListener('visibilitychange', () => {
    on = !document.hidden;
  });

  requestAnimationFrame(tick);
}

function startTheme(m, host) {
  const key = themeOf(m);
  const t = THEMES[key];

  host.style.setProperty('--glow', t.glow);

  const background = host.querySelector('.mv-bg');

  if (background) {
    background.style.setProperty('--art', t.art);
  }

  const title = host.querySelector('h1');

  if (title) {
    title.style.color = t.title || '';
    title.style.textShadow = `0 0 28px ${t.glow}`;
  }

  if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }

  if (t.fx === 'web') {
    host.insertAdjacentHTML('afterbegin', webSVG());

    const w = host.querySelector('.web');

    if (w) {
      setTimeout(() => w.classList.add('on'), 60);
    }
  } else {
    runFx(t.fx);
  }
}
