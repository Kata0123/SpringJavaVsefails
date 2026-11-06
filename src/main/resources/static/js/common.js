
const $ = s => document.querySelector(s);

let ALL = [];
let state = {f:'Все',q:''};
let CURRENT_USER = null;
let CSRF_TOKEN = '';
let WATCHLIST = [];
let RATINGS = {};
let RATING_SERVICE_AVAILABLE = true;

const RATING_UNAVAILABLE_MESSAGE =
  'Сервис оценок и списка просмотра временно недоступен.';

const art = t => {
  let h=0;
  for(const c of t) h=(h*31+c.charCodeAt(0))%360;
  return `linear-gradient(160deg,hsl(${h} 55% 30%),hsl(${(h+45)%360} 60% 11%))`;
};

const esc = s => String(s ?? '').replace(/[&<>"']/g,c=>({
  '&':'&amp;',
  '<':'&lt;',
  '>':'&gt;',
  '"':'&quot;',
  "'":'&#39;'
}[c]));

const safePoster = value => {
  if(!value) return '';
  const url=String(value);
  if(/^https?:\/\/[^\s]+$/i.test(url) || /^\/(?!\/)/.test(url)) return url;
  return '';
};

const img = m => {
  const url=safePoster(m.poster);
  return url
    ? `<img src="${esc(url)}" alt="Постер: ${esc(m.title)}" loading="lazy">`
    : '';
};

const openMovie = id => {
  location.href=`movie.html?id=${encodeURIComponent(id)}`;
};

const USER_ICON='<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>';

function isRatingServiceAvailable() {
  return RATING_SERVICE_AVAILABLE;
}

function applyRatingAvailability() {
  const unavailable =
    !!CURRENT_USER && !RATING_SERVICE_AVAILABLE;

  let banner=document.getElementById('rating-service-notice');

  if(unavailable && !banner) {
    banner=document.createElement('div');
    banner.id='rating-service-notice';
    banner.setAttribute('role','status');
    banner.setAttribute('aria-live','polite');
    banner.textContent=
      'Оценки и «Смотреть позже» временно недоступны. Каталог работает. Подключение восстановится автоматически.';

    banner.style.cssText=[
      'padding:14px 22px',
      'background:#292117',
      'color:#ffd18a',
      'border-bottom:1px solid #735426',
      'font-family:Manrope,sans-serif',
      'font-size:13px',
      'line-height:1.6',
      'text-align:center'
    ].join(';');

    const header=$('#top');

    if(header) {
      header.insertAdjacentElement('afterend',banner);
    } else {
      document.body.prepend(banner);
    }
  }

  if(banner) {
    banner.hidden=!unavailable;
  }

  document.querySelectorAll('#wl, .stars button').forEach(button=>{
    if(unavailable) {
      if(!button.disabled) {
        button.dataset.ratingServiceDisabled='true';
      }

      button.disabled=true;
      button.setAttribute(
        'title',
        RATING_UNAVAILABLE_MESSAGE
      );
    } else if(button.dataset.ratingServiceDisabled==='true') {
      button.disabled=false;
      delete button.dataset.ratingServiceDisabled;
      button.removeAttribute('title');
    }
  });
}

function markRatingServiceUnavailable() {
  if(!RATING_SERVICE_AVAILABLE) return;

  RATING_SERVICE_AVAILABLE=false;

  applyRatingAvailability();

  document.dispatchEvent(new Event('kadr:session'));
}

async function api(path,options={}) {
  const method=(options.method || 'GET').toUpperCase();
  const headers={...(options.headers || {})};

  if(!['GET','HEAD'].includes(method)) {
    headers['X-CSRF-TOKEN']=CSRF_TOKEN;

    if(options.body && typeof options.body==='string') {
      headers['Content-Type']='application/json';
    }
  }

  const response=await fetch(path,{
    ...options,
    method,
    headers,
    credentials:'same-origin'
  });

  if(!response.ok) {
    const ratingPath=
      /^\/api\/me\/(ratings|watchlist)(\/|$)/.test(path);

    if(ratingPath && response.status>=500) {
      markRatingServiceUnavailable();
      throw new Error(RATING_UNAVAILABLE_MESSAGE);
    }

    const error=await response.json().catch(()=>({}));

    if(error.error) {
      throw new Error(error.error);
    }

    if(response.status===401) {
      if(path.includes('/auth/login')) {
        throw new Error('Неверное имя пользователя или пароль.');
      }

      throw new Error('Для этого действия необходимо войти в аккаунт.');
    }

    if(response.status===403) {
      throw new Error(
        'Доступ запрещён. Недостаточно прав или неверный CSRF-токен.'
      );
    }

    if(response.status===409) {
      throw new Error('Пользователь с таким именем уже существует.');
    }

    throw new Error(`Ошибка сервера: ${response.status}`);
  }

  if(response.status===204) return null;

  return await response.json();
}

function getUser() {
  return CURRENT_USER;
}

function isAdmin() {
  return CURRENT_USER?.role==='ADMIN';
}

function getWL() {
  return [...WATCHLIST];
}

function inWL(id) {
  return WATCHLIST.includes(Number(id));
}

function getRT() {
  return {...RATINGS};
}

async function refreshSession(quiet=false) {
  const previous=JSON.stringify({
    user:CURRENT_USER,
    watchlist:WATCHLIST,
    ratings:RATINGS,
    available:RATING_SERVICE_AVAILABLE
  });

  const data=await api('/api/session');

  CSRF_TOKEN=data.csrfToken;
  CURRENT_USER=data.user || null;
  WATCHLIST=data.watchlist || [];
  RATINGS=data.ratings || {};
  RATING_SERVICE_AVAILABLE=data.ratingServiceAvailable!==false;

  const current=JSON.stringify({
    user:CURRENT_USER,
    watchlist:WATCHLIST,
    ratings:RATINGS,
    available:RATING_SERVICE_AVAILABLE
  });

  applyRatingAvailability();

  if(!quiet || previous!==current) {
    renderAuth();
    document.dispatchEvent(new Event('kadr:session'));
  }

  return data;
}

const APP_READY=refreshSession();

function renderAuth() {
  const box=$('#auth');
  const u=getUser();

  if(!box) return;

  if(!u) {
    box.innerHTML='<button class="btn ghost" type="button" id="openLogin">Войти</button>';
    return;
  }

  box.innerHTML=`<button class="profile" type="button" id="profileBtn" aria-haspopup="true" aria-expanded="false">
      <span class="ava">${USER_ICON}</span>
      <span class="uname">${esc(u.name)}</span>
    </button>
    <div class="menu" id="menu" hidden>
      <a href="profile.html">Профиль</a>
      <a href="catalog.html?view=watchlist">Watchlist</a>
      <a href="ratings.html">Оценки</a>
      ${isAdmin()?'<a href="admin.html">Админ-панель</a>':''}
      <hr>
      <button type="button" id="logout">Выйти</button>
    </div>`;
}

function toggleMenu(open) {
  const menu=$('#menu');
  const button=$('#profileBtn');

  if(!menu || !button) return;

  menu.hidden=!open;
  button.setAttribute('aria-expanded',String(open));
}

const authBox=$('#auth');
const modal=$('#loginModal');
const loginForm=$('#loginForm');

let registrationMode=false;

const switchMode=document.createElement('button');
switchMode.type='button';
switchMode.className='chip';
switchMode.textContent='Нет аккаунта? Зарегистрироваться';
loginForm.appendChild(switchMode);

const formError=document.createElement('p');
formError.className='lead';
formError.hidden=true;
loginForm.appendChild(formError);

function setAuthMode(registration) {
  registrationMode=registration;

  loginForm.querySelector('h3').textContent=
    registration?'Регистрация':'Вход';

  loginForm.querySelector('.btn').textContent=
    registration?'Зарегистрироваться':'Войти';

  switchMode.textContent=registration
    ?'Уже есть аккаунт? Войти'
    :'Нет аккаунта? Зарегистрироваться';

  loginForm.elements.pass.minLength=registration?8:1;
  formError.hidden=true;
}

switchMode.addEventListener('click',()=>{
  setAuthMode(!registrationMode);
});

authBox.addEventListener('click',async e=>{
  if(e.target.closest('#openLogin')) {
    setAuthMode(false);
    modal.showModal();
    return;
  }

  if(e.target.closest('#profileBtn')) {
    toggleMenu($('#menu').hidden);
    return;
  }

  if(e.target.closest('#logout')) {
    try {
      await api('/api/auth/logout',{method:'POST'});
      CSRF_TOKEN='';
      await refreshSession();

      if(document.body.dataset.page==='admin') {
        location.href='index.html';
      }
    } catch(error) {
      alert(error.message);
    }
  }
});

document.addEventListener('click',e=>{
  if(!e.target.closest('#auth')) toggleMenu(false);
});

document.addEventListener('keydown',e=>{
  if(e.key==='Escape') toggleMenu(false);
});

$('#loginClose').addEventListener('click',()=>{
  modal.close();
});

modal.addEventListener('click',e=>{
  if(e.target===modal) modal.close();
});

loginForm.addEventListener('submit',async e=>{
  e.preventDefault();

  const name=loginForm.elements.name.value.trim();
  const password=loginForm.elements.pass.value;
  const button=loginForm.querySelector('.btn');

  button.disabled=true;
  formError.hidden=true;

  try {
    await APP_READY;

    await api(
      registrationMode?'/api/auth/register':'/api/auth/login',
      {
        method:'POST',
        body:JSON.stringify({name,password})
      }
    );

    loginForm.reset();
    modal.close();

    await refreshSession();
  } catch(error) {
    formError.textContent=error.message;
    formError.hidden=false;
  } finally {
    button.disabled=false;
  }
});

const cardHTML=m=>`<button class="card" type="button" data-id="${m.id}" style="--art:${art(m.title)}">
  ${img(m)}
  <span class="rate">${m.rating ?? '—'}</span>
  <b>${esc(m.title)}</b>
</button>`;

async function toggleWL(id) {
  await APP_READY;

  if(!getUser()) {
    modal.showModal();
    return null;
  }

  if(!isRatingServiceAvailable()) {
    throw new Error(RATING_UNAVAILABLE_MESSAGE);
  }

  id=Number(id);
  const exists=inWL(id);

  await api('/api/me/watchlist/'+id,{
    method:exists?'DELETE':'POST'
  });

  if(exists) {
    WATCHLIST=WATCHLIST.filter(x=>x!==id);
  } else {
    WATCHLIST.unshift(id);
  }

  document.dispatchEvent(new Event('kadr:watchlist'));

  return !exists;
}

async function setRT(id,value) {
  await APP_READY;

  if(!getUser()) {
    modal.showModal();
    return null;
  }

  if(!isRatingServiceAvailable()) {
    throw new Error(RATING_UNAVAILABLE_MESSAGE);
  }

  id=Number(id);
  const previous=Number(RATINGS[id] || 0);
  const rating=previous===value?0:value;

  if(rating===0) {
    await api('/api/me/ratings/'+id,{method:'DELETE'});
    delete RATINGS[id];
  } else {
    await api('/api/me/ratings/'+id,{
      method:'PUT',
      body:JSON.stringify({rating})
    });
    RATINGS[id]=rating;
  }

  document.dispatchEvent(new Event('kadr:ratings'));

  return rating;
}

document.addEventListener('click',async e=>{
  const watchButton=e.target.closest('#wl');

  if(watchButton) {
    watchButton.disabled=true;

    try {
      const added=await toggleWL(watchButton.dataset.id);

      if(added!==null) {
        watchButton.textContent=added?'В списке':'Смотреть позже';
      }
    } catch(error) {
      alert(error.message);
    } finally {
      watchButton.disabled=false;
      applyRatingAvailability();
    }

    return;
  }

  const ratingButton=e.target.closest('.stars button');
  if(!ratingButton) return;

  const box=ratingButton.parentElement;
  const value=Number(ratingButton.dataset.v);

  try {
    const rating=await setRT(box.dataset.id,value);

    if(rating===null) return;

    box.querySelectorAll('button').forEach(button=>{
      button.classList.toggle(
        'on',
        Number(button.dataset.v)<=rating
      );
    });
  } catch(error) {
    alert(error.message);
  }
});

const ratingControlsObserver=new MutationObserver(()=>{
  applyRatingAvailability();
});

ratingControlsObserver.observe(document.body,{
  childList:true,
  subtree:true
});

let sessionCheckRunning=false;

setInterval(async()=>{
  if(document.hidden || !CURRENT_USER || sessionCheckRunning) {
    return;
  }

  sessionCheckRunning=true;

  try {
    await refreshSession(true);
  } catch(error) {
    console.warn('Session check failed:',error.message);
  } finally {
    sessionCheckRunning=false;
  }
},12000);

const solid=()=>{
  $('#top').classList.toggle(
    'solid',
    scrollY>40 || !!document.body.dataset.page
  );
};

addEventListener('scroll',solid,{passive:true});
solid();

const tmdbFooter=document.querySelector('.foot-brand');

if(tmdbFooter && !document.querySelector('.tmdb-attribution')) {
  const credit=document.createElement('div');

  credit.className='tmdb-attribution';
  credit.style.cssText='display:flex;align-items:center;gap:12px;margin-top:20px;color:#9aa1b5;font-size:11px;line-height:1.5;';

  tmdbFooter.appendChild(credit);
}
