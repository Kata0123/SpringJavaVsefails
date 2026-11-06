
function particles(card) {
  const c = card.querySelector('canvas');
  if (!c) return;

  const x = c.getContext('2d');
  if (!x) return;

  const sand = card.dataset.theme === 'sand';

  let W, H, ps, run = false;

  const init = () => {
    W = c.width = card.clientWidth;
    H = c.height = card.clientHeight;

    ps = Array.from({length: sand ? 110 : 70}, () => ({
      x: Math.random() * W,
      y: Math.random() * H,
      r: Math.random() * 2 + .5,
      s: Math.random() + .3,
      p: Math.random() * 6
    }));
  };

  const tick = () => {
    if (!run) return;

    x.clearRect(0, 0, W, H);

    for (const p of ps) {
      p.p += .03;

      if (sand) {
        p.x += p.s * 4;
        p.y += Math.sin(p.p) * .4;

        x.strokeStyle = `rgba(255,196,120,${.25 + p.s * .4})`;
        x.lineWidth = p.r * .7;

        x.beginPath();
        x.moveTo(p.x, p.y);
        x.lineTo(p.x - p.s * 14, p.y);
        x.stroke();

        if (p.x > W + 20) p.x = -20;
      } else {
        p.y -= p.s * .6;
        p.x += Math.sin(p.p) * .35;

        x.fillStyle = `rgba(255,80,90,${.25 + .35 * Math.sin(p.p) ** 2})`;
        x.shadowColor = '#ff2a40';
        x.shadowBlur = 10;

        x.beginPath();
        x.arc(p.x, p.y, p.r, 0, 7);
        x.fill();

        if (p.y < -5) {
          p.y = H + 5;
          p.x = Math.random() * W;
        }
      }
    }

    requestAnimationFrame(tick);
  };

  const on = () => {
    if (run) return;

    init();
    run = true;
    tick();
  };

  const off = () => {
    run = false;
  };

  card.addEventListener('pointerenter', on);
  card.addEventListener('focusin', on);
  card.addEventListener('pointerleave', off);
  card.addEventListener('focusout', off);
}

function renderFeatured(list) {
  const featured = $('#featured');
  if (!featured) return;

  featured.innerHTML = list.map(m => `
    <article
      class="feat"
      tabindex="0"
      role="button"
      data-id="${m.id}"
      data-theme="${esc(m.theme || '')}"
      style="--glow:${/^#[0-9a-f]{6}$/i.test(m.glow || '') ? m.glow : '#f4b942'}"
    >
      <div class="poster">${img(m)}</div>
      ${m.theme === 'web' ? webSVG() : '<canvas></canvas>'}
      <div class="info">
        <small>${esc(m.type)}, ${m.year}</small>
        <h3>${esc(m.title)}</h3>
        <span>★ ${m.rating ?? '—'}</span>
      </div>
    </article>
  `).join('');

  if (!matchMedia('(prefers-reduced-motion: reduce)').matches) {
    featured.querySelectorAll('.feat canvas').forEach(c => {
      particles(c.parentElement);
    });
  }
}

const shuffle = a => {
  a = [...a];

  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.random() * (i + 1) | 0;

    [a[i], a[j]] = [a[j], a[i]];
  }

  return a;
};

const renderPick = () => {
  const grid = $('#grid');
  if (!grid) return;

  grid.innerHTML = shuffle(ALL)
    .slice(0, 12)
    .map(cardHTML)
    .join('');
};

function openSelectedMovie(event) {
  const card = event.target.closest('[data-id]');

  if (!card) return;

  const id = Number(card.dataset.id);

  if (!Number.isSafeInteger(id) || id <= 0) return;

  openMovie(id);
}

function bindMovieCards() {
  const featured = $('#featured');
  const grid = $('#grid');

  if (featured) {
    featured.addEventListener('click', openSelectedMovie);

    featured.addEventListener('keydown', event => {
      if (event.key !== 'Enter' && event.key !== ' ') return;

      const card = event.target.closest('[data-id]');
      if (!card) return;

      event.preventDefault();
      openSelectedMovie(event);
    });
  }

  if (grid) {
    grid.addEventListener('click', openSelectedMovie);
  }
}

function bindHomepage() {
  const shuffleButton = $('#shuffle');
  const heroSearch = $('#heroSearch');

  if (shuffleButton) {
    shuffleButton.addEventListener('click', renderPick);
  }

  if (heroSearch) {
    heroSearch.addEventListener('submit', event => {
      event.preventDefault();

      const query = $('#q')?.value.trim() || '';

      location.href =
        'catalog.html?q=' + encodeURIComponent(query);
    });
  }

  bindMovieCards();
}

async function loadHomepage() {
  try {
    const data = await loadMovies();

    ALL = [
      ...(data.catalog || []),
      ...(data.featured || [])
    ];

    renderFeatured(data.featured || []);
    renderPick();
  } catch (error) {
    const featured = $('#featured');

    if (featured) {
      featured.textContent = error.message;
    }
  }
}

bindHomepage();
loadHomepage();
