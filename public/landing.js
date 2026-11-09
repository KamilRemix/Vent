/**
 * VENT — Landing Page Logic & Showcase
 * Pure SVG Botanical Art, Emil Kowalski tactile physics, Zero Emojis
 */

const BotanicalSVGs = {
  BLOOM: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <circle cx="50" cy="50" r="28" fill="#C87A5B"/>
      <circle cx="44" cy="44" r="12" fill="#FFF9EE" opacity="0.9"/>
      <line x1="50" y1="12" x2="50" y2="2" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
      <line x1="50" y1="88" x2="50" y2="98" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
      <line x1="83" y1="31" x2="92" y2="26" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
      <line x1="17" y1="69" x2="8" y2="74" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
      <line x1="83" y1="69" x2="92" y2="74" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
      <line x1="17" y1="31" x2="8" y2="26" stroke="#D4A359" stroke-width="6" stroke-linecap="round"/>
    </svg>
  `,

  CALM_LEAF: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M12 44 C32 22, 68 66, 88 44" stroke="#4A6B53" stroke-width="7" stroke-linecap="round"/>
      <path d="M20 66 C40 48, 60 84, 80 66" stroke="#7A9E84" stroke-width="6" stroke-linecap="round"/>
      <circle cx="50" cy="22" r="7" fill="#4A6B53"/>
    </svg>
  `,

  WARM_ROOT: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M50 82 C10 52, 14 18, 40 26 C47 28, 50 38, 50 38 C50 38, 53 28, 60 26 C86 18, 90 52, 50 82 Z" fill="#B8863B"/>
      <circle cx="50" cy="46" r="6" fill="#D4A359"/>
    </svg>
  `,

  MIST_FERN: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M26 58 C10 58, 10 38, 26 36 C28 18, 54 14, 64 28 C74 20, 90 32, 86 46 C96 52, 90 58, 78 58 Z" fill="#5B757A"/>
      <path d="M50 68 L54 78 Q50 84, 46 78 Z" fill="#88A4A9"/>
    </svg>
  `,

  AUTUMN_WIND: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M15 62 C35 82, 75 62, 82 38 C86 22, 62 18, 45 30 C36 40, 42 52, 54 50" stroke="#A95C47" stroke-width="7" stroke-linecap="round"/>
      <circle cx="72" cy="74" r="7" fill="#D0836C"/>
    </svg>
  `,

  WINTER_SEED: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M55 18 C20 28, 20 72, 55 82 C38 68, 38 32, 55 18 Z" fill="#6E6259"/>
      <circle cx="74" cy="42" r="6" fill="#9C8E83"/>
    </svg>
  `,

  WILDFIRE: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M28 76 C20 62, 22 46, 30 38 C35 48, 40 52, 44 42 C46 30, 48 20, 50 14 C52 20, 54 30, 56 42 C60 52, 65 48, 70 38 C78 46, 80 62, 72 76 C62 84, 38 84, 28 76 Z" fill="#C04B3E"/>
      <path d="M42 74 C40 62, 46 48, 50 42 C54 48, 60 62, 58 74 C52 78, 48 78, 42 74 Z" fill="#DC685A"/>
      <circle cx="64" cy="20" r="4.5" fill="#DC685A"/>
      <circle cx="34" cy="24" r="3.5" fill="#C04B3E"/>
    </svg>
  `,

  CHAOS_WIND: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M16 46 C22 18, 78 18, 84 46 C88 76, 36 84, 32 58 C30 42, 62 40, 64 54" stroke="#6D597A" stroke-width="7" stroke-linecap="round"/>
      <circle cx="24" cy="28" r="6" fill="#957DAD"/>
      <circle cx="76" cy="70" r="4.5" fill="#957DAD"/>
    </svg>
  `,

  ASH_MIST: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <path d="M14 34 C36 26, 64 40, 86 34" stroke="#94A3B8" stroke-width="5" stroke-linecap="round"/>
      <path d="M30 52 C30 36, 70 36, 70 52 C70 68, 30 68, 30 52 Z" fill="#64748B"/>
      <path d="M20 70 C42 64, 68 76, 86 70" stroke="#94A3B8" stroke-width="4.5" stroke-linecap="round" opacity="0.8"/>
    </svg>
  `
};

const ShowcaseMoods = [
  { key: 'BLOOM', titleEn: 'Joy', subEn: 'Energy & light', titleRu: 'Радость', subRu: 'Энергия и свет', quoteEn: 'Celebrate the warmth when it comes.', quoteRu: 'Празднуйте тепло, когда оно приходит.' },
  { key: 'CALM_LEAF', titleEn: 'Calm', subEn: 'Clarity & peace', titleRu: 'Спокойствие', subRu: 'Ясность и покой', quoteEn: 'Grounding roots into the present moment.', quoteRu: 'Заземление и ясность в настоящем моменте.' },
  { key: 'WARM_ROOT', titleEn: 'Warmth', subEn: 'Cozy grounding', titleRu: 'Тепло', subRu: 'Уют и тепло', quoteEn: 'Gentle safety after the storm.', quoteRu: 'Мягкая безопасность после любой бури.' },
  { key: 'MIST_FERN', titleEn: 'Sadness', subEn: 'Soft melancholia', titleRu: 'Грусть', subRu: 'Тихая грусть', quoteEn: 'Grief is just love with nowhere to go.', quoteRu: 'Грусть — это любовь, которой некуда деться.' },
  { key: 'WILDFIRE', titleEn: 'Anger', subEn: 'Fire & rage', titleRu: 'Гнев', subRu: 'Злость и ярость', quoteEn: 'A healthy fire defending your core values.', quoteRu: 'Здоровый огонь, защищающий ваши границы.' },
  { key: 'CHAOS_WIND', titleEn: 'Overwhelmed', subEn: 'Chaos in mind', titleRu: 'Перегруз', subRu: 'Хаос в голове', quoteEn: 'Unspool the tangled thoughts thread by thread.', quoteRu: 'Размотайте клубок мыслей нить за нитью.' },
  { key: 'ASH_MIST', titleEn: 'Emptiness', subEn: 'Numb silence', titleRu: 'Опустошение', subRu: 'Пустота и онемение', quoteEn: 'Even quiet soil rests before new spring.', quoteRu: 'Даже земля отдыхает перед новой весной.' }
];

const LandingTranslations = {
  en: {
    hero_tag: 'Private Decompression Sanctuary',
    hero_title_prefix: 'Where raw thoughts find',
    hero_title_highlight: 'quiet understanding',
    hero_sub: 'An intimate, judgment-free journal. Botanical emotional states, daily soundtrack analysis, and compassionate reflections grounded in clinical psychology.',
    btn_open_app: 'Open Web Version',
    btn_download_apk: 'Download Vent.apk',
    preview_url_label: 'vent-sanctuary.web.app • Chromebook, iPhone & Android',
    preview_sync_label: 'Cloud Sync Active',
    preview_headline: 'One Journal. Everywhere You Need It.',
    preview_sub: 'Write on your phone at home, open your Chromebook in school, or check from an iPhone. Sign in with Google to synchronize your thoughts seamlessly.',
    btn_preview_open: 'Open Vent Web Sanctuary',
    phil_title: 'Designed Around Clinical Psychology',
    phil_sub: 'Unlike sterile habit apps or toxic productivity tools, Vent is engineered to protect your mental energy and give raw feelings a safe harbor.',
    card1_title: 'Botanical Moods',
    card1_desc: 'No generic emojis. Emotional states shift like weather: Wildfire anger, Melancholy rain, Calm leaves, and Ash mist. Or simply vent freely without choosing any label.',
    card2_title: 'Soundtrack Psychology',
    card2_desc: 'Music acts as an emotional lightning rod. Gemini understands whether heavy beats serve as a cathartic adrenaline release or ambient lo-fi acts as a nervous system blanket.',
    card3_title: 'Circadian Rhythm',
    card3_desc: 'Writing at 3:00 AM? The AI recognizes nighttime prefrontal exhaustion and gently reminds you not to judge your whole life after midnight.',
    card4_title: 'Time Capsules',
    card4_desc: 'Write letters to your future self that stay sealed until a chosen date. Revisit your growth with genuine perspective.',
    card5_title: 'Seamless Google Sync',
    card5_desc: 'Connect your Google Account to synchronize across Android phone, school Chromebook, and desktop browser without losing a single line.',
    card6_title: '100% Private',
    card6_desc: 'Zero advertising, zero tracker SDKs, and offline-first encryption. Your vulnerable feelings belong only to you.',
    down_banner_title: 'Take Vent Everywhere',
    down_banner_desc: 'Download the official Android APK (v1.0.8, 26.6 MB). Private, offline-first, signed build.',
    nav_phil: 'Philosophy',
    nav_apk: 'Android APK'
  },
  ru: {
    hero_tag: 'Личное убежище для разгрузки',
    hero_title_prefix: 'Где искренние мысли находят',
    hero_title_highlight: 'покой и понимание',
    hero_sub: 'Интимный дневник без осуждения. Ботанические метафоры настроения, психология саундтрека дня и чуткие инсайты на основе клинической психологии.',
    btn_open_app: 'Открыть веб-версию',
    btn_download_apk: 'Скачать Vent.apk',
    preview_url_label: 'vent-sanctuary.web.app • Хромбук, iPhone и Android',
    preview_sync_label: 'Синхронизация Google',
    preview_headline: 'Один дневник. На всех ваших устройствах.',
    preview_sub: 'Пишите с телефона дома, открывайте на хромбуке в школе или заходите с компьютера. Входите через Google и ваши мысли всегда синхронизированы.',
    btn_preview_open: 'Открыть веб-версию Vent',
    phil_title: 'Создано на основе доказательной психологии',
    phil_sub: 'В отличие от сухих трекеров привычек и токсичной продуктивности, Vent создан для сохранения вашей ментальной энергии и искренней поддержки.',
    card1_title: 'Ботанические настроения',
    card1_desc: 'Никаких шаблонных эмодзи. Состояния сменяются как погода: огонь гнева, дождь меланхолии, спокойный лист или туман опустошения. Или пишите вовсе без ярлыков.',
    card2_title: 'Психология саундтрека',
    card2_desc: 'Музыка — эмоциональный громоотвод. ИИ понимает, когда тяжелый бит помогает выплеснуть адреналин, а эмбиент укутывает уставшую нервную систему.',
    card3_title: 'Циркадные биоритмы',
    card3_desc: 'Пишете в 3 часа ночи? ИИ распознает истощение префронтальной коры и мягко напомнит не делать глобальных выводов о жизни посреди ночи.',
    card4_title: 'Капсулы времени',
    card4_desc: 'Пишите письма себе в будущее, которые остаются строго запечатанными до выбранного дня. Оцените свой рост свежим взглядом.',
    card5_title: 'Синхронизация через Google',
    card5_desc: 'Подключите Google Аккаунт для мгновенной синхронизации между Android телефоном, хромбуком в школе и домашним компьютером.',
    card6_title: '100% Конфиденциально',
    card6_desc: 'Ноль рекламных трекеров, ноль сбора данных и автономное локальное шифрование. Ваши сокровенные мысли принадлежат только вам.',
    down_banner_title: 'Возьмите Vent с собой',
    down_banner_desc: 'Скачайте официальный Android APK (v1.0.8, 26.6 МБ). Защищённая автономная сборка с офлайн-доступом.',
    nav_phil: 'Философия',
    nav_apk: 'Android APK'
  }
};

let currentLang = localStorage.getItem('vent_lang') || 'ru';
let currentTheme = localStorage.getItem('vent_theme') || 'dark';
let activeShowcaseMood = 'CALM_LEAF';

function setLandingLanguage(lang) {
  currentLang = lang;
  localStorage.setItem('vent_lang', lang);

  const t = LandingTranslations[lang] || LandingTranslations.ru;

  // Update all data-t elements
  document.querySelectorAll('[data-t]').forEach(el => {
    const key = el.getAttribute('data-t');
    if (t[key]) {
      el.textContent = t[key];
    }
  });

  // Update lang button text
  const langBtn = document.getElementById('langToggleBtn');
  if (langBtn) {
    langBtn.textContent = lang.toUpperCase();
  }

  // Update showcase card
  updateShowcaseCard();
}

function updateShowcaseCard() {
  const isRu = currentLang === 'ru';
  const item = ShowcaseMoods.find(m => m.key === activeShowcaseMood) || ShowcaseMoods[1];
  
  const previewIcon = document.getElementById('heroPreviewIcon');
  if (previewIcon) {
    previewIcon.innerHTML = BotanicalSVGs[item.key](64);
  }

  const quoteEl = document.getElementById('heroPreviewQuote');
  if (quoteEl) {
    quoteEl.textContent = isRu ? item.quoteRu : item.quoteEn;
  }

  // Render strip
  const strip = document.getElementById('heroBotanicalStrip');
  if (strip) {
    strip.innerHTML = ShowcaseMoods.map(m => {
      const isSelected = m.key === activeShowcaseMood;
      return `
        <button class="preview-botanical-item ${isSelected ? 'active' : ''}" onclick="selectShowcaseMood('${m.key}')" style="${isSelected ? 'border-color: var(--accent-primary); background: var(--surface-hover); transform: translateY(-2px);' : ''}">
          <div>${BotanicalSVGs[m.key](26)}</div>
          <span class="preview-botanical-label">${isRu ? m.titleRu : m.titleEn}</span>
        </button>
      `;
    }).join('');
  }
}

window.selectShowcaseMood = function(key) {
  activeShowcaseMood = key;
  updateShowcaseCard();
};

function toggleLandingTheme() {
  currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
  localStorage.setItem('vent_theme', currentTheme);
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateThemeIcon();
}

function updateThemeIcon() {
  const btn = document.getElementById('themeToggleBtn');
  if (!btn) return;
  if (currentTheme === 'dark') {
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`;
    btn.setAttribute('title', 'Switch to Light Theme');
  } else {
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
    btn.setAttribute('title', 'Switch to Dark Theme');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  // Set theme
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateThemeIcon();

  // Wire Theme Toggle
  const themeBtn = document.getElementById('themeToggleBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', toggleLandingTheme);
  }

  // Wire Language Toggle
  const langBtn = document.getElementById('langToggleBtn');
  if (langBtn) {
    langBtn.addEventListener('click', () => {
      setLandingLanguage(currentLang === 'ru' ? 'en' : 'ru');
    });
  }

  // Inject botanical svg in showcase card 1
  const bloomSlot = document.getElementById('showcaseBloomSlot');
  if (bloomSlot) {
    bloomSlot.innerHTML = BotanicalSVGs.CALM_LEAF(22);
  }

  // Initial language & showcase render
  setLandingLanguage(currentLang);
});
