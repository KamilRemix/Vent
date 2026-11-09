/**
 * VENT — Mindful Personal Diary & Time Capsule
 * 1-to-1 Architecture Parity with Kotlin Android App (MainActivity.kt)
 * Destinations: JOURNAL, CALENDAR, LETTERS, SEARCH, INSIGHTS
 * Modals: DiaryEntryEditorScreen, DiaryEntryReaderScreen, FutureLetterComposerScreen, SettingsScreen
 * Strictly zero emojis, 100% vector SVG artwork & clean lines
 */

// ==========================================================================
// 1. EXACT BOTANICAL VECTOR ARTWORK (From BotanicalIllustrations.kt)
// ==========================================================================
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
  `,

  NONE: (size = 32) => `
    <svg width="${size}" height="${size}" viewBox="0 0 100 100" fill="none">
      <circle cx="50" cy="50" r="34" stroke="#8E9BAE" stroke-width="5" stroke-dasharray="6 6"/>
      <circle cx="50" cy="50" r="14" fill="#8E9BAE" opacity="0.4"/>
    </svg>
  `
};

// ==========================================================================
// 2. BOTANICAL MOOD DEFINITIONS (From BotanicalMood.kt)
// ==========================================================================
const BotanicalMoodData = [
  { key: 'BLOOM', titleEn: 'Joy', subEn: 'Energy & light', titleRu: 'Радость', subRu: 'Энергия и свет', defaultIntensity: 5, color: '#C87A5B', soft: 'rgba(200, 122, 91, 0.15)' },
  { key: 'CALM_LEAF', titleEn: 'Calm', subEn: 'Clarity & peace', titleRu: 'Спокойствие', subRu: 'Ясность и покой', defaultIntensity: 4, color: '#4A6B53', soft: 'rgba(74, 107, 83, 0.15)' },
  { key: 'WARM_ROOT', titleEn: 'Warmth', subEn: 'Comfort & warmth', titleRu: 'Тепло', subRu: 'Уют и тепло', defaultIntensity: 4, color: '#B8863B', soft: 'rgba(184, 134, 59, 0.15)' },
  { key: 'MIST_FERN', titleEn: 'Sadness', subEn: 'Quiet reflection', titleRu: 'Грусть', subRu: 'Тихая грусть', defaultIntensity: 3, color: '#5B757A', soft: 'rgba(91, 117, 122, 0.15)' },
  { key: 'AUTUMN_WIND', titleEn: 'Anxiety', subEn: 'Racing thoughts', titleRu: 'Тревога', subRu: 'Поток мыслей', defaultIntensity: 2, color: '#A95C47', soft: 'rgba(169, 92, 71, 0.15)' },
  { key: 'WINTER_SEED', titleEn: 'Tiredness', subEn: 'Rest & stillness', titleRu: 'Усталость', subRu: 'Отдых и тишина', defaultIntensity: 1, color: '#6E6259', soft: 'rgba(110, 98, 89, 0.15)' },
  { key: 'WILDFIRE', titleEn: 'Anger', subEn: 'Frustration & fire', titleRu: 'Гнев', subRu: 'Злость и ярость', defaultIntensity: 5, color: '#C04B3E', soft: 'rgba(192, 75, 62, 0.18)' },
  { key: 'CHAOS_WIND', titleEn: 'Overwhelmed', subEn: 'Mental chaos', titleRu: 'Перегруз', subRu: 'Хаос в голове', defaultIntensity: 4, color: '#6D597A', soft: 'rgba(109, 89, 122, 0.15)' },
  { key: 'ASH_MIST', titleEn: 'Numbness', subEn: 'Empty & detached', titleRu: 'Опустошение', subRu: 'Пустота и онемение', defaultIntensity: 2, color: '#64748B', soft: 'rgba(100, 116, 139, 0.15)' },
  { key: 'NONE', titleEn: 'Raw Vent', subEn: 'No mood label', titleRu: 'Без эмоций', subRu: 'Чистый поток', defaultIntensity: 3, color: '#7D736A', soft: 'rgba(125, 115, 106, 0.15)' }
];

// ==========================================================================
// 3. REFLECTION PROMPTS (1-to-1 from ReflectionPrompts.kt)
// ==========================================================================
const ReflectionPrompts = {
  en: [
    "What gave me a sense of inner grounding and safety today?",
    "What small detail from today am I genuinely grateful for?",
    "Which emotion spoke the loudest in me today, and what was it trying to say?",
    "When did I feel most at ease with myself today?",
    "What can I gently let go of tonight so tomorrow feels lighter?",
    "How did I care for my mind and body today?",
    "What conversation or quiet moment from today do I want to remember?",
    "What do I need most right now — support, silence, or rest?"
  ],
  ru: [
    "Что сегодня дало мне ощущение внутренней опоры и спокойствия?",
    "За какую маленькую деталь сегодняшнего дня я искренне благодарен?",
    "Какая эмоция звучала сегодня громче всего и о чем она хотела сказать?",
    "В какой момент сегодня я чувствовал себя наиболее естественно и легко?",
    "Что я могу мягко отпустить этим вечером, чтобы завтра стало легче?",
    "Как я позаботился о своем теле и душевном состоянии сегодня?",
    "Какой разговор или тихий момент сегодняшнего дня хочется сохранить в памяти?",
    "Что мне сейчас нужнее всего — поддержка, тишина или отдых?"
  ],
  getPromptForDate: function(dateIso, lang = 'ru') {
    const list = lang === 'ru' ? this.ru : this.en;
    let hash = 0;
    for (let i = 0; i < dateIso.length; i++) {
      hash = (hash << 5) - hash + dateIso.charCodeAt(i);
      hash |= 0;
    }
    const idx = Math.abs(hash) % list.length;
    return list[idx];
  }
};

// ==========================================================================
// 4. DICTIONARY (EN / RU) — 1-to-1 with Kotlin strings.xml & AppLocalization.kt
// ==========================================================================
const I18N = {
  en: {
    app_name: 'Vent',
    btn_back_site: 'Site',
    sign_in_google: 'Continue with Google',
    nav_journal: 'Journal',
    nav_calendar: 'Calendar',
    nav_letters: 'Capsule',
    nav_search: 'Search',
    nav_analytics: 'Insights',
    mindful_journal_tag: 'Mindful Journal',
    all_dates_btn: 'All Dates',
    btn_ai_day: 'AI Day',
    btn_new_entry: 'Write Entry',
    calendar_mood_title: 'Mood Calendar',
    capsules_title: 'Time Capsules',
    btn_compose_letter: 'Write Letter',
    sealed_letters_title: 'Sealed Letters',
    unlocked_letters_title: 'Unlocked Letters',
    filter_mood_tags_title: 'Filter by Mood & Tags',
    reset_all_btn: 'Reset All',
    metric_entries_title: 'Entries',
    metric_entries_sub: 'Total notes',
    metric_days_title: 'Active Days',
    metric_days_sub: 'Journaled days',
    metric_photos_title: 'Photos',
    metric_photos_sub: 'Cloud WebP',
    ai_badge: 'Gemini Flash Psychological Hub',
    ai_music_title: 'Soundtrack Psychological Role:',
    emotional_balance_title: 'Emotional Balance'
  },
  ru: {
    app_name: 'Vent',
    btn_back_site: 'Сайт',
    sign_in_google: 'Войти через Google',
    nav_journal: 'Дневник',
    nav_calendar: 'Календарь',
    nav_letters: 'Капсула',
    nav_search: 'Поиск',
    nav_analytics: 'Аналитика',
    mindful_journal_tag: 'Дневник эмоций',
    all_dates_btn: 'Все даты',
    btn_ai_day: 'ИИ-день',
    btn_new_entry: 'Запись',
    calendar_mood_title: 'Календарь настроений',
    capsules_title: 'Капсулы времени',
    btn_compose_letter: 'Написать письмо',
    sealed_letters_title: 'Запечатанные письма',
    unlocked_letters_title: 'Открытые письма',
    filter_mood_tags_title: 'Фильтр по настроению и тегам',
    reset_all_btn: 'Сбросить',
    metric_entries_title: 'Записи',
    metric_entries_sub: 'Всего в дневнике',
    metric_days_title: 'Дней',
    metric_days_sub: 'С записями',
    metric_photos_title: 'Фото',
    metric_photos_sub: 'В облаке',
    ai_badge: 'Психологический хаб Gemini Flash',
    ai_music_title: 'Психологическая роль саундтрека:',
    emotional_balance_title: 'Эмоциональный баланс'
  }
};

// ==========================================================================
// 5. DATE & TIME UTILITIES (From DateAndPhotoUtils.kt)
// ==========================================================================
const DateUtils = {
  todayIso: function() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  formatFullDate: function(isoStr, lang = 'ru') {
    if (!isoStr) return '';
    try {
      const parts = isoStr.split('-');
      const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      return d.toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (_) {
      return isoStr;
    }
  },

  formatShortDate: function(isoStr, lang = 'ru') {
    if (!isoStr) return '';
    try {
      const parts = isoStr.split('-');
      const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      return d.toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'en-US', {
        day: 'numeric',
        month: 'short'
      });
    } catch (_) {
      return isoStr;
    }
  },

  formatMonthYearHeader: function(year, monthZeroBased, lang = 'ru') {
    const d = new Date(year, monthZeroBased, 1);
    const str = d.toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'en-US', {
      month: 'long',
      year: 'numeric'
    });
    return str.charAt(0).toUpperCase() + str.slice(1);
  },

  buildMonthGrid: function(year, monthZeroBased) {
    const firstDay = new Date(year, monthZeroBased, 1);
    let dayOfWeek = firstDay.getDay() - 1; // 0 = Mon, 6 = Sun
    if (dayOfWeek < 0) dayOfWeek = 6;

    const daysInMonth = new Date(year, monthZeroBased + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, monthZeroBased, 0).getDate();

    const cells = [];
    // Previous month filler
    for (let i = dayOfWeek - 1; i >= 0; i--) {
      const dayNum = daysInPrevMonth - i;
      const prevDate = new Date(year, monthZeroBased - 1, dayNum);
      cells.push({
        dayNumber: dayNum,
        isCurrentMonth: false,
        isoDate: DateUtils.toIso(prevDate)
      });
    }

    // Current month days
    for (let day = 1; day <= daysInMonth; day++) {
      const curDate = new Date(year, monthZeroBased, day);
      cells.push({
        dayNumber: day,
        isCurrentMonth: true,
        isoDate: DateUtils.toIso(curDate)
      });
    }

    // Next month filler to complete 42 or 35 cells
    let nextDay = 1;
    while (cells.length < 35 || (cells.length > 35 && cells.length < 42)) {
      const nextDate = new Date(year, monthZeroBased + 1, nextDay);
      cells.push({
        dayNumber: nextDay,
        isCurrentMonth: false,
        isoDate: DateUtils.toIso(nextDate)
      });
      nextDay++;
    }

    return cells;
  },

  toIso: function(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  formatCountdown: function(unlockIso, unlockHhMm, lang = 'ru') {
    try {
      const parts = unlockIso.split('-');
      const timeParts = (unlockHhMm || '09:00').split(':');
      const target = new Date(
        parseInt(parts[0]),
        parseInt(parts[1]) - 1,
        parseInt(parts[2]),
        parseInt(timeParts[0] || '9'),
        parseInt(timeParts[1] || '0'),
        0
      );

      const diff = target.getTime() - Date.now();
      if (diff <= 0) {
        return lang === 'ru' ? 'Открыто прямо сейчас' : 'Unlocked now';
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

      if (lang === 'ru') {
        if (days > 0) return `Откроется через ${days} дн. ${hours} ч.`;
        if (hours > 0) return `Откроется через ${hours} ч. ${mins} мин.`;
        return `Откроется через ${mins} мин.`;
      } else {
        if (days > 0) return `Unlocks in ${days}d ${hours}h`;
        if (hours > 0) return `Unlocks in ${hours}h ${mins}m`;
        return `Unlocks in ${mins}m`;
      }
    } catch (_) {
      return unlockIso;
    }
  }
};

// ==========================================================================
// 6. APPLICATION STATE & STORAGE (Strictly zero dummy entries)
// ==========================================================================
class AppState {
  constructor() {
    this.lang = localStorage.getItem('vent_lang') || 'ru';
    this.theme = localStorage.getItem('vent_theme') || 'dark';
    this.currentDestination = 'JOURNAL';
    this.selectedDateIso = DateUtils.todayIso();

    const now = new Date();
    this.calendarYear = now.getFullYear();
    this.calendarMonthZeroBased = now.getMonth();

    this.entries = this.loadLocalEntries();
    this.searchQuery = '';
    this.selectedMoodFilter = null;
    this.selectedTagFilter = null;
    this.selectedAiCategory = 'DAYS';

    this.editorMood = 'CALM_LEAF';
    this.editorEditingId = null;
    this.viewingEntry = null;

    this.currentUser = null;
    this.unsubscribeFirestore = null;
  }

  loadLocalEntries() {
    try {
      const data = localStorage.getItem('vent_entries_v4');
      if (data) {
        let parsed = JSON.parse(data);
        if (Array.isArray(parsed)) {
          // Purge any test/demo entries so user always starts with a pure private journal
          parsed = parsed.filter(e => 
            e.title !== 'Вечер после сложного дня' && 
            e.title !== 'Послание через год' &&
            !(e.content && e.content.includes('прогулка в парке вернула спокойствие'))
          );
          return parsed;
        }
      }
    } catch (_) {}
    return []; // Clean empty start
  }

  saveLocalEntries() {
    localStorage.setItem('vent_entries_v4', JSON.stringify(this.entries));
  }

  addOrUpdateEntry(entry) {
    const idx = this.entries.findIndex(e => e.id === entry.id);
    if (idx >= 0) {
      this.entries[idx] = entry;
    } else {
      this.entries.unshift(entry);
    }
    this.saveLocalEntries();

    if (this.currentUser && window.ventFirestore) {
      try {
        const firestoreData = {
          userId: this.currentUser.uid,
          dateIso: entry.dateIso,
          monthDay: entry.dateIso.slice(5),
          moodKey: entry.moodKey === 'NONE' ? 'CALM_LEAF' : entry.moodKey,
          moodIntensity: entry.moodIntensity || 4,
          title: entry.title || '',
          content: entry.content || '',
          reflectionPrompt: entry.reflectionPrompt || null,
          reflectionAnswer: entry.reflectionAnswer || null,
          tags: entry.tags || [],
          photosBase64: [],
          updatedAt: firebase.firestore.FieldValue.serverTimestamp()
        };
        if (!entry.createdAt) {
          firestoreData.createdAt = firebase.firestore.FieldValue.serverTimestamp();
        }

        window.ventFirestore
          .collection('users')
          .doc(this.currentUser.uid)
          .collection('entries')
          .doc(entry.id)
          .set(firestoreData, { merge: true });
      } catch (err) {
        console.warn('Firestore write failed, cached locally', err);
      }
    }
  }

  deleteEntry(id) {
    this.entries = this.entries.filter(e => e.id !== id);
    this.saveLocalEntries();

    if (this.currentUser && window.ventFirestore) {
      try {
        window.ventFirestore
          .collection('users')
          .doc(this.currentUser.uid)
          .collection('entries')
          .doc(id)
          .delete();
      } catch (err) {
        console.warn('Firestore delete failed', err);
      }
    }
  }

  clearAllEntries() {
    const ids = this.entries.map(e => e.id);
    this.entries = [];
    this.saveLocalEntries();

    if (this.currentUser && window.ventFirestore) {
      ids.forEach(id => {
        try {
          window.ventFirestore
            .collection('users')
            .doc(this.currentUser.uid)
            .collection('entries')
            .doc(id)
            .delete();
        } catch (_) {}
      });
    }
  }
}

const state = new AppState();

// ==========================================================================
// 7. FIREBASE CLOUD INITIALIZATION & REAL-TIME SYNC
// ==========================================================================
function initFirebaseSync() {
  if (typeof firebase === 'undefined') {
    updateSyncIndicator(false, false);
    return;
  }

  const firebaseConfig = {
    apiKey: atob("QUl6YVN5RFRJWXhSUFg0V1F0NlpmeEdoVmRVbnNra0VWcE02emZR"),
    authDomain: "gen-lang-client-0494620647.firebaseapp.com",
    projectId: "gen-lang-client-0494620647",
    storageBucket: "gen-lang-client-0494620647.firebasestorage.app",
    messagingSenderId: "852279005031",
    appId: "1:852279005031:web:de30a5a9bf51350af5a9c2"
  };

  try {
    if (!firebase.apps.length) {
      firebase.initializeApp(firebaseConfig);
    }
    window.ventAuth = firebase.auth();
    window.ventFirestore = firebase.firestore();

    window.ventAuth.onAuthStateChanged(user => {
      state.currentUser = user;
      updateAuthUI(user);

      if (user) {
        attachFirestoreListener(user.uid);
      } else {
        if (state.unsubscribeFirestore) {
          state.unsubscribeFirestore();
          state.unsubscribeFirestore = null;
        }
        updateSyncIndicator(false, false);
      }
    });

    // Check redirect authentication result
    window.ventAuth.getRedirectResult().then(result => {
      if (result && result.user) {
        state.currentUser = result.user;
        updateAuthUI(result.user);
      }
    }).catch(err => {
      console.warn('Redirect auth result info:', err);
    });
  } catch (e) {
    console.warn('Firebase init error (offline/local fallback):', e);
    updateSyncIndicator(false, false);
  }
}

function attachFirestoreListener(userId) {
  try {
    updateSyncIndicator(true, true);
    const colRef = window.ventFirestore
      .collection('users')
      .doc(userId)
      .collection('entries')
      .orderBy('createdAt', 'desc');

    state.unsubscribeFirestore = colRef.onSnapshot(snapshot => {
      const cloudEntries = [];
      snapshot.forEach(doc => {
        const d = doc.data();
        const musicTag = (d.tags || []).find(t => t.startsWith('music:'))?.replace('music:', '');
        const cleanTags = (d.tags || []).filter(t => !t.startsWith('music:') && !t.startsWith('mood:'));
        const isFutureLetter = doc.id.startsWith('capsule_') || (d.reflectionPrompt && d.reflectionPrompt.startsWith('WRITTEN_AT:'));

        cloudEntries.push({
          id: doc.id,
          dateIso: d.dateIso || DateUtils.todayIso(),
          moodKey: d.moodKey || 'CALM_LEAF',
          moodIntensity: d.moodIntensity || 4,
          title: d.title || '',
          content: d.content || '',
          reflectionPrompt: d.reflectionPrompt || null,
          reflectionAnswer: d.reflectionAnswer || null,
          music: musicTag || '',
          tags: cleanTags,
          isFutureLetter: isFutureLetter,
          timestamp: d.createdAt ? d.createdAt.toMillis() : Date.now()
        });
      });

      state.entries = cloudEntries;
      state.saveLocalEntries();
      renderCurrentDestinationView();
      updateSyncIndicator(true, false);
    }, err => {
      console.warn('Firestore snapshot error', err);
      updateSyncIndicator(true, false);
    });
  } catch (err) {
    console.warn('Failed to attach Firestore listener', err);
    updateSyncIndicator(false, false);
  }
}

function updateSyncIndicator(isConnected, isSyncing) {
  const syncDot = document.getElementById('syncDot');
  const syncText = document.getElementById('syncStatusText');
  if (!syncDot || !syncText) return;

  const isRu = state.lang === 'ru';
  if (isConnected) {
    syncDot.className = 'sync-pulse-dot';
    syncText.textContent = isSyncing 
      ? (isRu ? 'Синхронизация...' : 'Syncing...')
      : (isRu ? 'Синхронизировано' : 'Cloud Sync');
  } else {
    syncDot.className = 'sync-pulse-dot offline';
    syncText.textContent = isRu ? 'Локально (Гость)' : 'Guest (Local)';
  }
}

function updateAuthUI(user) {
  const container = document.getElementById('authContainer');
  const settingsAuth = document.getElementById('settingsAuthSection');
  const isRu = state.lang === 'ru';

  if (user) {
    const avatar = user.photoURL || 'icon.png';
    const name = user.displayName || user.email || 'User';
    const pillHtml = `
      <div class="auth-user-pill" onclick="openSettingsModal()" title="${isRu ? 'Настройки аккаунта' : 'Account settings'}">
        <img src="${avatar}" class="auth-avatar" alt="Avatar" onerror="this.src='icon.png'">
        <span class="auth-name">${escapeHtml(name)}</span>
      </div>
    `;
    if (container) container.innerHTML = pillHtml;
    if (settingsAuth) {
      settingsAuth.innerHTML = `
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px;">
          <div>
            <div style="font-weight: 700;">${escapeHtml(name)}</div>
            <div style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(user.email || '')}</div>
          </div>
          <button class="btn-secondary" onclick="signOutGoogle()" style="padding: 6px 14px; font-size: 12px;">${isRu ? 'Выйти' : 'Sign out'}</button>
        </div>
      `;
    }
  } else {
    const btnHtml = `
      <button class="btn-google-login" onclick="signInWithGoogle()">
        <svg width="14" height="14" viewBox="0 0 24 24"><path fill="#EA4335" d="M12 5c1.6 0 3 .6 4.1 1.6l3.1-3.1C17.3 1.7 14.8 1 12 1 7.5 1 3.7 3.6 1.9 7.3l3.7 2.9C6.5 7.4 9 5 12 5z"/><path fill="#4285F4" d="M23.5 12.3c0-.8-.1-1.7-.2-2.3H12v4.6h6.5c-.3 1.5-1.1 2.8-2.4 3.7l3.7 2.9c2.2-2 3.7-5 3.7-8.9z"/><path fill="#FBBC05" d="M5.6 14.8c-.2-.7-.4-1.5-.4-2.8s.2-2.1.4-2.8L1.9 6.3C.7 8.7 0 10.8 0 12s.7 3.3 1.9 5.7l3.7-2.9z"/><path fill="#34A853" d="M12 23c3.2 0 6-1.1 8-3l-3.7-2.9c-1.1.7-2.5 1.2-4.3 1.2-3 0-5.5-2-6.4-4.8L1.9 16.4C3.7 20.4 7.5 23 12 23z"/></svg>
        <span>${isRu ? 'Войти' : 'Sign in'}</span>
      </button>
    `;
    if (container) container.innerHTML = btnHtml;
    if (settingsAuth) {
      settingsAuth.innerHTML = `
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px;">
          <div style="font-size: 13px; color: var(--text-secondary);">${isRu ? 'Вход не выполнен. Записи сохраняются локально.' : 'Not signed in. Entries saved locally.'}</div>
          <button class="btn-primary" onclick="signInWithGoogle()" style="padding: 6px 14px; font-size: 12px;">${isRu ? 'Войти через Google' : 'Sign in with Google'}</button>
        </div>
      `;
    }
  }
}

window.signInWithGoogle = async function() {
  if (!window.ventAuth) {
    alert(state.lang === 'ru' ? 'Firebase не загружен или офлайн.' : 'Firebase is not initialized or offline.');
    return;
  }
  try {
    const provider = new firebase.auth.GoogleAuthProvider();
    provider.setCustomParameters({ prompt: 'select_account' });
    const result = await window.ventAuth.signInWithPopup(provider);
    if (result && result.user) {
      state.currentUser = result.user;
      updateAuthUI(result.user);
    }
  } catch (err) {
    console.warn('Google sign-in popup notice:', err);
    if (err.code === 'auth/popup-blocked' || err.code === 'auth/cancelled-popup-request') {
      try {
        const provider = new firebase.auth.GoogleAuthProvider();
        provider.setCustomParameters({ prompt: 'select_account' });
        await window.ventAuth.signInWithRedirect(provider);
        return;
      } catch (redirectErr) {
        console.error('Google sign-in redirect error:', redirectErr);
      }
    }
    if (err.code !== 'auth/popup-closed-by-user') {
      const msg = state.lang === 'ru' ? `Ошибка авторизации через Google: ${err.message}` : `Google authorization error: ${err.message}`;
      alert(msg);
    }
  }
};

window.signOutGoogle = function() {
  if (!window.ventAuth) return;
  const isRu = state.lang === 'ru';
  if (confirm(isRu ? 'Выйти из Google-аккаунта?' : 'Sign out of Google Account?')) {
    window.ventAuth.signOut();
  }
};

// ==========================================================================
// 8. NAVIGATION BETWEEN THE 5 DESTINATIONS (1-to-1 MainDestination)
// ==========================================================================
window.navigateTo = function(destKey) {
  state.currentDestination = destKey;

  // 1. Update NavigationRail & BottomNav items
  document.querySelectorAll('.nav-rail-item, .bottom-nav-item').forEach(el => {
    el.classList.toggle('active', el.getAttribute('data-destination') === destKey);
  });

  // 2. Update Active Screen View
  const destToViewMap = {
    JOURNAL: 'view-journal',
    CALENDAR: 'view-calendar',
    LETTERS: 'view-letters',
    SEARCH: 'view-search',
    INSIGHTS: 'view-insights'
  };

  document.querySelectorAll('.screen-view').forEach(view => {
    view.classList.toggle('active', view.id === destToViewMap[destKey]);
  });

  // 3. Update TopAppBar Title Badge
  const badgeText = document.getElementById('currentTabBadgeText');
  if (badgeText) {
    const isRu = state.lang === 'ru';
    const titles = {
      JOURNAL: isRu ? 'Дневник' : 'Journal',
      CALENDAR: isRu ? 'Календарь' : 'Calendar',
      LETTERS: isRu ? 'Капсула' : 'Capsule',
      SEARCH: isRu ? 'Поиск' : 'Search',
      INSIGHTS: isRu ? 'Аналитика' : 'Insights'
    };
    badgeText.textContent = titles[destKey] || destKey;
  }

  // 4. Render Destination
  renderCurrentDestinationView();
};

function renderCurrentDestinationView() {
  switch (state.currentDestination) {
    case 'JOURNAL':
      renderJournalTab();
      break;
    case 'CALENDAR':
      renderCalendarTab();
      break;
    case 'LETTERS':
      renderLettersTab();
      break;
    case 'SEARCH':
      renderSearchTab();
      break;
    case 'INSIGHTS':
      renderInsightsTab();
      break;
  }
}

// ==========================================================================
// 9. DESTINATION 1: JOURNAL (1-to-1 JournalHomeTab)
// ==========================================================================
function renderJournalTab() {
  const isRu = state.lang === 'ru';
  const todayIso = DateUtils.todayIso();

  // 1. Date Switcher Header
  const dateDisplay = document.getElementById('dayNavDateDisplay');
  if (dateDisplay) {
    dateDisplay.textContent = DateUtils.formatFullDate(state.selectedDateIso, state.lang);
  }

  const btnNext = document.getElementById('btnNextDay');
  if (btnNext) {
    btnNext.disabled = state.selectedDateIso >= todayIso;
  }

  // 2. Entries for Selected Date
  const dateEntries = state.entries.filter(e => !e.isFutureLetter && e.dateIso === state.selectedDateIso);
  const otherEntries = state.entries.filter(e => !e.isFutureLetter && e.dateIso !== state.selectedDateIso);

  // 3. Mood status in Hero
  const latestMoodKey = dateEntries[0]?.moodKey || state.entries.find(e => !e.isFutureLetter)?.moodKey || 'CALM_LEAF';
  const moodObj = BotanicalMoodData.find(m => m.key === latestMoodKey) || BotanicalMoodData[1];
  
  const heroMoodText = document.getElementById('heroMoodStatusText');
  if (heroMoodText) {
    heroMoodText.textContent = dateEntries.length > 0
      ? (isRu ? `Настроение: ${moodObj.titleRu} — ${moodObj.subRu}` : `Mood: ${moodObj.titleEn} — ${moodObj.subEn}`)
      : (isRu ? 'Тихое пространство для ваших мыслей' : 'A peaceful space for your thoughts');
  }

  const heroSlot = document.getElementById('heroBotanicalSlot');
  if (heroSlot && BotanicalSVGs[latestMoodKey]) {
    heroSlot.innerHTML = BotanicalSVGs[latestMoodKey](64);
  }

  // 4. Section titles
  const shortDate = DateUtils.formatShortDate(state.selectedDateIso, state.lang);
  const secTitle = document.getElementById('selectedDateSectionTitle');
  const secSub = document.getElementById('selectedDateSectionSub');
  const btnAiDay = document.getElementById('btnAnalyzeDayAi');

  if (secTitle) secTitle.textContent = isRu ? `Записи за ${shortDate}` : `Entries for ${shortDate}`;
  if (secSub) {
    secSub.textContent = dateEntries.length === 0
      ? (isRu ? 'За этот день пока нет записей' : 'No notes recorded for this day yet')
      : (isRu ? `Записей: ${dateEntries.length} — нажмите, чтобы прочитать` : `${dateEntries.length} entries — tap to read`);
  }
  if (btnAiDay) btnAiDay.style.display = dateEntries.length > 0 ? 'inline-flex' : 'none';

  // 5. Selected Date Cards or Empty State Prompt
  const list = document.getElementById('selectedDateEntriesList');
  if (list) {
    if (dateEntries.length === 0) {
      list.innerHTML = `
        <div class="empty-day-prompt-card">
          <div class="empty-day-icon">${BotanicalSVGs.CALM_LEAF(48)}</div>
          <h4 class="empty-day-title">${isRu ? 'За этот день пока нет записей' : 'No entries for this day'}</h4>
          <p class="empty-day-sub">${isRu ? 'Запишите свои мысли, переживания или саундтрек сегодняшнего дня.' : 'Record your thoughts, emotions, or daily soundtrack.'}</p>
          <button class="btn-primary" onclick="openEditorModal('${state.selectedDateIso}')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            <span>${isRu ? 'Написать мысль' : 'Write Entry'}</span>
          </button>
        </div>
      `;
    } else {
      list.innerHTML = dateEntries.map(e => renderDiaryCardHtml(e)).join('');
    }
  }

  // 6. Recent Timeline (other dates)
  const recentSection = document.getElementById('recentTimelineSection');
  const recentGrid = document.getElementById('recentTimelineGrid');
  if (recentSection && recentGrid) {
    if (otherEntries.length === 0) {
      recentSection.style.display = 'none';
    } else {
      recentSection.style.display = 'block';
      recentGrid.innerHTML = otherEntries.slice(0, 30).map(e => renderDiaryCardHtml(e)).join('');
    }
  }
}

window.shiftSelectedDay = function(delta) {
  try {
    const parts = state.selectedDateIso.split('-');
    const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    d.setDate(d.getDate() + delta);
    const newIso = DateUtils.toIso(d);
    if (newIso <= DateUtils.todayIso()) {
      state.selectedDateIso = newIso;
      renderJournalTab();
    }
  } catch (_) {}
};

window.openDayAiAnalysis = function() {
  navigateTo('INSIGHTS');
  setAiCategory('DAYS');
};

function renderDiaryCardHtml(entry) {
  const isRu = state.lang === 'ru';
  const moodObj = BotanicalMoodData.find(m => m.key === entry.moodKey) || BotanicalMoodData[1];
  const moodSvg = BotanicalSVGs[entry.moodKey] ? BotanicalSVGs[entry.moodKey](26) : BotanicalSVGs.NONE(26);
  const moodTitle = isRu ? moodObj.titleRu : moodObj.titleEn;
  const moodSub = isRu ? moodObj.subRu : moodObj.subEn;
  const dateFormatted = DateUtils.formatFullDate(entry.dateIso, state.lang);

  const musicHtml = entry.music ? `
    <div class="card-soundtrack-strip">
      <div class="vinyl-disc"></div>
      <span style="font-weight: 600;">${escapeHtml(entry.music)}</span>
    </div>
  ` : '';

  const tagsHtml = (entry.tags || []).map(t => `<span class="tag-pill">#${escapeHtml(t)}</span>`).join('');

  return `
    <div class="diary-entry-card" onclick="openReaderModal('${entry.id}')">
      <div class="card-top-row">
        <div class="card-mood-info">
          <div class="mood-badge-art" style="--badge-bg: ${moodObj.soft}; --badge-border: ${moodObj.color}40;">
            ${moodSvg}
          </div>
          <div>
            <div class="mood-badge-text-title">${moodTitle}</div>
            <div class="mood-badge-text-sub">${moodSub}</div>
          </div>
        </div>
        <div class="card-meta-time">${dateFormatted}</div>
      </div>

      ${entry.title ? `<div class="card-title-text">${escapeHtml(entry.title)}</div>` : ''}
      <div class="card-content-preview">${escapeHtml(entry.content)}</div>

      ${musicHtml}
      ${tagsHtml ? `<div class="card-tags-row">${tagsHtml}</div>` : ''}
    </div>
  `;
}

// ==========================================================================
// 10. DESTINATION 2: CALENDAR (1-to-1 CalendarTab)
// ==========================================================================
function renderCalendarTab() {
  const isRu = state.lang === 'ru';

  // 1. Month Header
  const title = document.getElementById('calendarMonthYearTitle');
  if (title) {
    title.textContent = DateUtils.formatMonthYearHeader(state.calendarYear, state.calendarMonthZeroBased, state.lang);
  }

  // 2. Weekday Row (Пн, Вт, Ср, Чт, Пт, Сб, Вс)
  const weekdaysRow = document.getElementById('calendarWeekdaysRow');
  if (weekdaysRow) {
    const days = isRu 
      ? ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
      : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    weekdaysRow.innerHTML = days.map(d => `<div>${d}</div>`).join('');
  }

  // 3. Month Grid Cells
  const grid = document.getElementById('calendarMonthGrid');
  if (grid) {
    const cells = DateUtils.buildMonthGrid(state.calendarYear, state.calendarMonthZeroBased);
    const todayIso = DateUtils.todayIso();

    grid.innerHTML = cells.map(c => {
      const dayEntries = state.entries.filter(e => !e.isFutureLetter && e.dateIso === c.isoDate);
      const isSelected = c.isoDate === state.selectedDateIso;
      const isToday = c.isoDate === todayIso;

      let dotHtml = '';
      if (dayEntries.length > 0) {
        const moodKey = dayEntries[0].moodKey || 'CALM_LEAF';
        const moodObj = BotanicalMoodData.find(m => m.key === moodKey) || BotanicalMoodData[1];
        dotHtml = `<div class="calendar-day-dot" style="background: ${moodObj.color};"></div>`;
      }

      return `
        <div class="calendar-day-cell ${c.isCurrentMonth ? '' : 'other-month'} ${isSelected ? 'selected' : ''} ${isToday ? 'today' : ''}" 
             onclick="selectCalendarDate('${c.isoDate}')">
          <span class="calendar-day-number">${c.dayNumber}</span>
          ${dotHtml}
        </div>
      `;
    }).join('');
  }

  // 4. Selected Day Entries in Calendar Tab
  const selTitle = document.getElementById('calendarSelectedDayTitle');
  const selSub = document.getElementById('calendarSelectedDaySub');
  const selList = document.getElementById('calendarSelectedDayEntriesList');

  const selectedEntries = state.entries.filter(e => !e.isFutureLetter && e.dateIso === state.selectedDateIso);
  const shortDate = DateUtils.formatShortDate(state.selectedDateIso, state.lang);

  if (selTitle) selTitle.textContent = isRu ? `Записи за ${shortDate}` : `Entries for ${shortDate}`;
  if (selSub) {
    selSub.textContent = selectedEntries.length === 0
      ? (isRu ? 'За этот день пока нет записей' : 'No notes recorded for this day yet')
      : (isRu ? `Записей: ${selectedEntries.length}` : `${selectedEntries.length} entries`);
  }
  if (selList) {
    if (selectedEntries.length === 0) {
      selList.innerHTML = `
        <div class="empty-day-prompt-card" style="padding: 24px;">
          <p class="empty-day-sub" style="margin-bottom: 0;">${isRu ? 'На этот день записей нет.' : 'No entries for this day.'}</p>
        </div>
      `;
    } else {
      selList.innerHTML = selectedEntries.map(e => renderDiaryCardHtml(e)).join('');
    }
  }
}

window.navigateCalendarMonth = function(delta) {
  state.calendarMonthZeroBased += delta;
  if (state.calendarMonthZeroBased < 0) {
    state.calendarMonthZeroBased = 11;
    state.calendarYear -= 1;
  } else if (state.calendarMonthZeroBased > 11) {
    state.calendarMonthZeroBased = 0;
    state.calendarYear += 1;
  }
  renderCalendarTab();
};

window.selectCalendarDate = function(isoDate) {
  state.selectedDateIso = isoDate;
  renderCalendarTab();
};

// ==========================================================================
// 11. DESTINATION 3: CAPSULE (1-to-1 FutureLettersTab)
// ==========================================================================
function renderLettersTab() {
  const isRu = state.lang === 'ru';
  const todayIso = DateUtils.todayIso();

  const letters = state.entries.filter(e => e.isFutureLetter);
  const sealed = letters.filter(e => e.dateIso > todayIso);
  const unlocked = letters.filter(e => e.dateIso <= todayIso);

  const sealedGrid = document.getElementById('sealedLettersGrid');
  const unlockedGrid = document.getElementById('unlockedLettersGrid');

  if (sealedGrid) {
    if (sealed.length === 0) {
      sealedGrid.innerHTML = `
        <div class="empty-day-prompt-card" style="grid-column: 1/-1; padding: 32px;">
          <p class="empty-day-sub">${isRu ? 'Нет запечатанных писем. Напишите послание себе в будущее!' : 'No sealed letters. Write a letter to your future self!'}</p>
        </div>
      `;
    } else {
      sealedGrid.innerHTML = sealed.map(s => {
        const timeHhMm = s.reflectionPrompt?.includes('UNLOCK_TIME:') ? s.reflectionPrompt.split('UNLOCK_TIME:')[1].slice(0, 5) : '09:00';
        const countdownStr = DateUtils.formatCountdown(s.dateIso, timeHhMm, state.lang);
        const unlockDate = DateUtils.formatFullDate(s.dateIso, state.lang);

        return `
          <div class="capsule-card" onclick="openSealedLetterModal('${s.id}')">
            <div class="capsule-seal-stamp">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <span>${isRu ? 'Письмо запечатано' : 'Sealed Time Capsule'}</span>
            </div>
            <div style="font-size: 13.5px; color: var(--text-secondary); margin-bottom: 6px;">
              ${isRu ? 'Дата открытия:' : 'Unlocks on:'} <strong style="color: var(--text-primary);">${unlockDate}</strong>
            </div>
            <div class="capsule-countdown-pill">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>${countdownStr}</span>
            </div>
          </div>
        `;
      }).join('');
    }
  }

  if (unlockedGrid) {
    if (unlocked.length === 0) {
      unlockedGrid.innerHTML = `
        <div class="empty-day-prompt-card" style="grid-column: 1/-1; padding: 24px;">
          <p class="empty-day-sub" style="margin-bottom: 0;">${isRu ? 'Пока нет открытых писем.' : 'No unlocked letters yet.'}</p>
        </div>
      `;
    } else {
      unlockedGrid.innerHTML = unlocked.map(u => renderDiaryCardHtml(u)).join('');
    }
  }
}

// Live Countdown Tick every 15 seconds
setInterval(() => {
  if (state.currentDestination === 'LETTERS') {
    renderLettersTab();
  }
}, 15000);

// ==========================================================================
// 12. DESTINATION 4: SEARCH (1-to-1 SearchAndTagsTab)
// ==========================================================================
function renderSearchTab() {
  const isRu = state.lang === 'ru';
  const query = state.searchQuery.toLowerCase().trim();

  // 1. Mood filter chips
  const moodRow = document.getElementById('searchMoodFiltersRow');
  if (moodRow) {
    moodRow.innerHTML = BotanicalMoodData.filter(m => m.key !== 'NONE').map(m => {
      const isSelected = state.selectedMoodFilter === m.key;
      const title = isRu ? m.titleRu : m.titleEn;
      const svg = BotanicalSVGs[m.key] ? BotanicalSVGs[m.key](16) : '';
      return `
        <div class="filter-chip ${isSelected ? 'active' : ''}" onclick="toggleMoodFilter('${m.key}')">
          <span>${svg}</span>
          <span>${title}</span>
        </div>
      `;
    }).join('');
  }

  // 2. Tag filter chips
  const tagRow = document.getElementById('searchTagFiltersRow');
  if (tagRow) {
    const allTags = Array.from(new Set(state.entries.flatMap(e => e.tags || []))).filter(Boolean);
    tagRow.innerHTML = allTags.map(t => {
      const isSelected = state.selectedTagFilter === t;
      return `
        <div class="filter-chip ${isSelected ? 'active' : ''}" onclick="toggleTagFilter('${escapeHtml(t)}')">
          <span>#${escapeHtml(t)}</span>
        </div>
      `;
    }).join('');
  }

  // 3. Reset Button visibility
  const resetBtn = document.getElementById('btnResetFilters');
  const hasFilters = query.length > 0 || state.selectedMoodFilter !== null || state.selectedTagFilter !== null;
  if (resetBtn) resetBtn.style.display = hasFilters ? 'inline-block' : 'none';

  // 4. Filter entries
  const filtered = state.entries.filter(e => {
    if (state.selectedMoodFilter && e.moodKey !== state.selectedMoodFilter) return false;
    if (state.selectedTagFilter && !(e.tags || []).includes(state.selectedTagFilter)) return false;
    if (query) {
      const matchTitle = (e.title || '').toLowerCase().includes(query);
      const matchContent = (e.content || '').toLowerCase().includes(query);
      const matchMusic = (e.music || '').toLowerCase().includes(query);
      const matchTags = (e.tags || []).some(t => t.toLowerCase().includes(query));
      if (!matchTitle && !matchContent && !matchMusic && !matchTags) return false;
    }
    return true;
  });

  const countEl = document.getElementById('searchResultsCountText');
  if (countEl) {
    countEl.textContent = isRu ? `Найдено записей: ${filtered.length}` : `Found ${filtered.length} entries`;
  }

  const resultsGrid = document.getElementById('searchResultsGrid');
  if (resultsGrid) {
    if (filtered.length === 0) {
      resultsGrid.innerHTML = `
        <div class="empty-day-prompt-card" style="padding: 40px 20px;">
          <h4 class="empty-day-title">${isRu ? 'Ничего не найдено' : 'No entries found'}</h4>
          <p class="empty-day-sub">${isRu ? 'Попробуйте изменить поисковый запрос или сбросить фильтры.' : 'Try adjusting your search keywords or clear active filters.'}</p>
        </div>
      `;
    } else {
      resultsGrid.innerHTML = filtered.map(e => renderDiaryCardHtml(e)).join('');
    }
  }
}

window.handleSearchInputChange = function(val) {
  state.searchQuery = val;
  const clearBtn = document.getElementById('btnClearSearch');
  if (clearBtn) clearBtn.style.display = val.length > 0 ? 'flex' : 'none';
  renderSearchTab();
};

window.clearSearchQuery = function() {
  const input = document.getElementById('searchInputField');
  if (input) input.value = '';
  handleSearchInputChange('');
};

window.toggleMoodFilter = function(moodKey) {
  state.selectedMoodFilter = state.selectedMoodFilter === moodKey ? null : moodKey;
  renderSearchTab();
};

window.toggleTagFilter = function(tag) {
  state.selectedTagFilter = state.selectedTagFilter === tag ? null : tag;
  renderSearchTab();
};

window.resetSearchFilters = function() {
  state.searchQuery = '';
  state.selectedMoodFilter = null;
  state.selectedTagFilter = null;
  const input = document.getElementById('searchInputField');
  if (input) input.value = '';
  const clearBtn = document.getElementById('btnClearSearch');
  if (clearBtn) clearBtn.style.display = 'none';
  renderSearchTab();
};

// ==========================================================================
// 13. DESTINATION 5: INSIGHTS (1-to-1 AnalyticsTab)
// ==========================================================================
function renderInsightsTab() {
  const isRu = state.lang === 'ru';
  const nonLetters = state.entries.filter(e => !e.isFutureLetter);

  // 1. Metrics Grid
  const totalEntriesEl = document.getElementById('metricTotalEntries');
  const activeDaysEl = document.getElementById('metricActiveDays');
  const totalPhotosEl = document.getElementById('metricTotalPhotos');

  if (totalEntriesEl) totalEntriesEl.textContent = nonLetters.length;
  if (activeDaysEl) {
    const uniqueDays = new Set(nonLetters.map(e => e.dateIso)).size;
    activeDaysEl.textContent = uniqueDays;
  }
  if (totalPhotosEl) totalPhotosEl.textContent = '0';

  // 2. AI Psychological Hub Content
  const headlineEl = document.getElementById('aiHeadlineDisplay');
  const warmLetterEl = document.getElementById('aiWarmLetterDisplay');
  const musicEl = document.getElementById('aiMusicAnalysisDisplay');

  if (nonLetters.length === 0) {
    if (headlineEl) headlineEl.textContent = isRu ? '«Твой безопасный уголок ждет первой мысли»' : '"Your safe space is ready."';
    if (warmLetterEl) warmLetterEl.textContent = isRu
      ? 'Здесь пока тихо. Как только вы сохраните свою первую запись в дневнике, я бережно разберу эмоциональный фон, поддержу и помогу заметить точки опоры.'
      : 'It is quiet here. Once you write your first entry, I will gently analyze your emotional balance and resilience.';
    if (musicEl) musicEl.textContent = isRu
      ? 'Укажите саундтрек в наушниках при написании записи, чтобы раскрыть его психологическую роль.'
      : 'Note your soundtrack when writing an entry to discover its emotional function.';
  } else {
    const musicNames = nonLetters.filter(e => e.music).map(e => e.music);
    if (headlineEl) headlineEl.textContent = isRu ? '«Ты справляешься лучше, чем тебе кажется в часы усталости»' : '"You are navigating this far better than you give yourself credit for."';
    if (warmLetterEl) warmLetterEl.textContent = isRu
      ? `Привет. Я внимательно перечитал твои записи. Сквозь строчки видно, сколько всего сейчас происходит и как много душевных сил уходит на то, чтобы удерживать равновесие. Но главное, что я вижу — это то, как ты не сдаешься. Когда накатывает раздражение или усталость, ты находишь зрелость выплеснуть всё в дневник, а не копить внутри. Пожалуйста, помни: ты имеешь полное право уставать. Я рядом и горжусь тем, как ты держишься.`
      : `Hey. I took my time reading through what you poured into your journal. Through your words, I can see the weight you carry. But what stands out most is your resilience. Take a deep breath tonight — you made it through.`;

    if (musicEl) {
      if (musicNames.length > 0) {
        musicEl.textContent = isRu
          ? `В твоих недавних записях саундтрек («${escapeHtml(musicNames[0])}») выполняет важную регулирующую функцию: разрядку адреналина при стрессе и заземление нервной системы.`
          : `In your recent entries, the music ("${escapeHtml(musicNames[0])}") acts as emotional release and nervous system grounding.`;
      } else {
        musicEl.textContent = isRu
          ? 'Музыкальные треки пока не указаны. Добавьте трек в наушниках при создании следующей записи!'
          : 'No soundtracks noted yet. Add what you are listening to when writing next time!';
      }
    }
  }

  // 3. Dominant Mood Card
  const moodCounts = {};
  nonLetters.forEach(e => {
    moodCounts[e.moodKey] = (moodCounts[e.moodKey] || 0) + 1;
  });

  let dominantKey = null;
  let maxCount = 0;
  for (const k in moodCounts) {
    if (moodCounts[k] > maxCount) {
      maxCount = moodCounts[k];
      dominantKey = k;
    }
  }

  const domSlot = document.getElementById('dominantMoodHighlightSlot');
  if (domSlot) {
    if (dominantKey && BotanicalMoodData.find(m => m.key === dominantKey)) {
      const domObj = BotanicalMoodData.find(m => m.key === dominantKey);
      const domSvg = BotanicalSVGs[dominantKey] ? BotanicalSVGs[dominantKey](48) : '';
      domSlot.innerHTML = `
        <div class="bento-card" style="background: ${domObj.soft}; border-color: ${domObj.color}60;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div>${domSvg}</div>
            <div>
              <div style="font-size: 11px; font-weight: 700; color: ${domObj.color}; text-transform: uppercase;">
                ${isRu ? 'Преобладающее настроение' : 'Most Frequent Mood'}
              </div>
              <div style="font-size: 18px; font-weight: 700; color: var(--text-primary); margin-top: 2px;">
                ${isRu ? domObj.titleRu : domObj.titleEn}
              </div>
              <div style="font-size: 13px; color: var(--text-secondary);">
                ${isRu ? domObj.subRu : domObj.subEn} • ${maxCount} ${isRu ? 'записей' : 'entries'}
              </div>
            </div>
          </div>
        </div>
      `;
    } else {
      domSlot.innerHTML = '';
    }
  }

  // 4. Emotional Balance Bar & Breakdown
  const barTrack = document.getElementById('moodDistributionBarTrack');
  const breakdownList = document.getElementById('moodBreakdownList');
  const totalCount = nonLetters.length;

  if (barTrack && breakdownList) {
    if (totalCount === 0) {
      barTrack.innerHTML = '<div style="width: 100%; height: 100%; background: var(--border-subtle);"></div>';
      breakdownList.innerHTML = `<p style="font-size: 13px; color: var(--text-secondary); text-align: center;">${isRu ? 'Данные появятся после первых записей' : 'Data will appear after your first entries'}</p>`;
    } else {
      let trackHtml = '';
      let listHtml = '';

      BotanicalMoodData.filter(m => m.key !== 'NONE').forEach(m => {
        const count = moodCounts[m.key] || 0;
        const pct = ((count / totalCount) * 100).toFixed(1);
        if (count > 0) {
          trackHtml += `<div class="mood-distribution-slice" style="width: ${pct}%; background: ${m.color};" title="${m.titleRu}: ${count}"></div>`;
        }
        const svg = BotanicalSVGs[m.key] ? BotanicalSVGs[m.key](24) : '';
        const title = isRu ? m.titleRu : m.titleEn;
        const sub = isRu ? m.subRu : m.subEn;

        listHtml += `
          <div class="mood-breakdown-row">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div>${svg}</div>
              <div>
                <div style="font-size: 13.5px; font-weight: 600;">${title}</div>
                <div style="font-size: 11px; color: var(--text-secondary);">${sub}</div>
              </div>
            </div>
            <div style="background: ${m.soft}; color: ${m.color}; font-weight: 700; font-size: 13px; padding: 3px 10px; border-radius: var(--radius-sm);">
              ${count}
            </div>
          </div>
        `;
      });

      barTrack.innerHTML = trackHtml;
      breakdownList.innerHTML = listHtml;
    }
  }
}

window.setAiCategory = function(cat) {
  state.selectedAiCategory = cat;
  document.querySelectorAll('[data-cat]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-cat') === cat);
  });
  renderInsightsTab();
};

// ==========================================================================
// 14. MODAL 1: DIARY ENTRY EDITOR (1-to-1 DiaryEntryEditorScreen)
// ==========================================================================
window.openEditorModal = function(targetDateIso = state.selectedDateIso, existingEntry = null) {
  state.editorTargetDate = targetDateIso || DateUtils.todayIso();
  state.editorEditingId = existingEntry ? existingEntry.id : null;
  state.editorMood = existingEntry ? existingEntry.moodKey : 'CALM_LEAF';

  const overlay = document.getElementById('editorModalOverlay');
  const title = document.getElementById('editorModalTitle');
  const dateSub = document.getElementById('editorModalDateSub');

  const isRu = state.lang === 'ru';
  if (title) title.textContent = existingEntry ? (isRu ? 'Редактирование записи' : 'Edit Entry') : (isRu ? 'Новая запись' : 'New Entry');
  if (dateSub) dateSub.textContent = DateUtils.formatFullDate(state.editorTargetDate, state.lang);

  // Prefill values
  const titleInput = document.getElementById('editorTitleInput');
  const contentInput = document.getElementById('editorContentInput');
  const musicInput = document.getElementById('editorMusicInput');
  const tagsInput = document.getElementById('editorTagsInput');
  const promptLabel = document.getElementById('editorReflectionPromptLabel');
  const promptText = document.getElementById('editorReflectionPromptText');
  const answerInput = document.getElementById('editorReflectionAnswerInput');

  if (titleInput) titleInput.value = existingEntry ? existingEntry.title || '' : '';
  if (contentInput) contentInput.value = existingEntry ? existingEntry.content || '' : '';
  if (musicInput) musicInput.value = existingEntry ? existingEntry.music || '' : '';
  if (tagsInput) tagsInput.value = existingEntry ? (existingEntry.tags || []).join(', ') : '';

  const dailyPrompt = ReflectionPrompts.getPromptForDate(state.editorTargetDate, state.lang);
  if (promptLabel) promptLabel.textContent = isRu ? 'Вопрос дня:' : 'Question of the Day:';
  if (promptText) promptText.textContent = dailyPrompt;
  if (answerInput) answerInput.value = existingEntry ? existingEntry.reflectionAnswer || '' : '';

  renderEditorMoodGrid();
  if (overlay) overlay.style.display = 'flex';
};

window.closeEditorModal = function() {
  const overlay = document.getElementById('editorModalOverlay');
  if (overlay) overlay.style.display = 'none';
  state.editorEditingId = null;
};

function renderEditorMoodGrid() {
  const grid = document.getElementById('editorMoodSelectorGrid');
  if (!grid) return;
  const isRu = state.lang === 'ru';

  grid.innerHTML = BotanicalMoodData.map(m => {
    const isSelected = state.editorMood === m.key;
    const svg = BotanicalSVGs[m.key] ? BotanicalSVGs[m.key](26) : '';
    const title = isRu ? m.titleRu : m.titleEn;
    const sub = isRu ? m.subRu : m.subEn;

    return `
      <div class="mood-select-card ${isSelected ? 'active' : ''}" 
           style="--mood-color: ${m.color}; --mood-color-soft: ${m.soft};" 
           onclick="state.editorMood = '${m.key}'; renderEditorMoodGrid();">
        <div>${svg}</div>
        <div class="mood-select-name">${title}</div>
        <div class="mood-select-desc">${sub}</div>
      </div>
    `;
  }).join('');
}

window.submitSaveEntry = function() {
  const content = (document.getElementById('editorContentInput')?.value || '').trim();
  if (!content) {
    alert(state.lang === 'ru' ? 'Напишите хотя бы пару слов перед сохранением.' : 'Please write something before saving.');
    return;
  }

  const title = (document.getElementById('editorTitleInput')?.value || '').trim();
  const music = (document.getElementById('editorMusicInput')?.value || '').trim();
  const rawTags = (document.getElementById('editorTagsInput')?.value || '').trim();
  const tags = rawTags ? rawTags.split(/[\s,]+/).map(t => t.replace(/^#/, '')).filter(Boolean) : [];
  if (music) tags.push(`music:${music}`);

  const prompt = document.getElementById('editorReflectionPromptText')?.textContent || '';
  const answer = (document.getElementById('editorReflectionAnswerInput')?.value || '').trim();

  const entry = {
    id: state.editorEditingId || `entry_${Date.now()}`,
    dateIso: state.editorTargetDate || DateUtils.todayIso(),
    moodKey: state.editorMood || 'CALM_LEAF',
    moodIntensity: 4,
    title: title,
    content: content,
    reflectionPrompt: prompt,
    reflectionAnswer: answer,
    music: music,
    tags: tags.filter(t => !t.startsWith('music:')),
    isFutureLetter: false,
    timestamp: Date.now()
  };

  state.addOrUpdateEntry(entry);
  closeEditorModal();
  renderCurrentDestinationView();
};

// ==========================================================================
// 15. MODAL 2: DIARY ENTRY READER (1-to-1 DiaryEntryReaderScreen)
// ==========================================================================
window.openReaderModal = function(entryId) {
  const entry = state.entries.find(e => e.id === entryId);
  if (!entry) return;
  state.viewingEntry = entry;

  const overlay = document.getElementById('readerModalOverlay');
  const container = document.getElementById('readerModalContent');
  const isRu = state.lang === 'ru';

  const moodObj = BotanicalMoodData.find(m => m.key === entry.moodKey) || BotanicalMoodData[1];
  const moodSvg = BotanicalSVGs[entry.moodKey] ? BotanicalSVGs[entry.moodKey](56) : BotanicalSVGs.NONE(56);
  const dateStr = DateUtils.formatFullDate(entry.dateIso, state.lang);

  const musicHtml = entry.music ? `
    <div class="card-soundtrack-strip" style="font-size: 13px; padding: 6px 14px; margin: 16px 0;">
      <div class="vinyl-disc"></div>
      <span style="font-weight: 700;">${escapeHtml(entry.music)}</span>
    </div>
  ` : '';

  const promptHtml = entry.reflectionPrompt && entry.reflectionAnswer ? `
    <div class="reflection-prompt-box" style="margin: 18px 0;">
      <div class="prompt-icon-box">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><circle cx="12" cy="12" r="5"/></svg>
      </div>
      <div>
        <strong style="color: var(--accent-amber);">${escapeHtml(entry.reflectionPrompt)}</strong>
        <p style="margin-top: 6px; font-size: 14px; color: var(--text-primary);">${escapeHtml(entry.reflectionAnswer)}</p>
      </div>
    </div>
  ` : '';

  const tagsHtml = (entry.tags || []).map(t => `<span class="tag-pill" style="font-size: 12px; padding: 4px 10px;">#${escapeHtml(t)}</span>`).join('');

  if (container) {
    container.innerHTML = `
      <div style="display: flex; align-items: center; gap: 18px; margin-bottom: 20px;">
        <div style="width: 64px; height: 64px; border-radius: var(--radius-md); background: ${moodObj.soft}; display: flex; align-items: center; justify-content: center;">
          ${moodSvg}
        </div>
        <div>
          <h2 style="font-family: var(--font-serif); font-size: 26px; font-weight: 700; color: var(--text-primary); margin-bottom: 2px;">
            ${isRu ? moodObj.titleRu : moodObj.titleEn}
          </h2>
          <div style="font-size: 13px; color: var(--text-secondary);">${dateStr}</div>
        </div>
      </div>

      ${entry.title ? `<h3 style="font-size: 20px; font-weight: 700; margin-bottom: 12px; color: var(--text-primary);">${escapeHtml(entry.title)}</h3>` : ''}

      <div style="font-size: 15.5px; line-height: 1.75; color: var(--text-primary); white-space: pre-wrap; margin: 16px 0 20px;">
        ${escapeHtml(entry.content)}
      </div>

      ${musicHtml}
      ${promptHtml}
      ${tagsHtml ? `<div class="card-tags-row" style="margin-top: 14px;">${tagsHtml}</div>` : ''}

      <div style="margin-top: 28px; border-top: 1px solid var(--border-subtle); padding-top: 18px; display: flex; gap: 10px; flex-wrap: wrap;">
        <button class="outlined-chip-btn" onclick="closeReaderModal(); navigateTo('INSIGHTS'); setAiCategory('DAYS');">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a5 5 0 0 1 5 5v1h1a3 3 0 0 1 3 3v2a3 3 0 0 1-3 3h-1v1a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5v-1H3a3 3 0 0 1-3-3v-2a3 3 0 0 1 3-3h1V7a5 5 0 0 1 5-5h3z"/></svg>
          <span>${isRu ? 'Разобрать день с ИИ' : 'Analyze Day with AI'}</span>
        </button>
      </div>
    `;
  }

  if (overlay) overlay.style.display = 'flex';
};

window.closeReaderModal = function() {
  const overlay = document.getElementById('readerModalOverlay');
  if (overlay) overlay.style.display = 'none';
  state.viewingEntry = null;
};

window.editCurrentViewingEntry = function() {
  const entry = state.viewingEntry;
  if (!entry) return;
  closeReaderModal();
  openEditorModal(entry.dateIso, entry);
};

window.deleteCurrentViewingEntry = function() {
  const entry = state.viewingEntry;
  if (!entry) return;
  const isRu = state.lang === 'ru';
  if (confirm(isRu ? 'Удалить эту запись из дневника?' : 'Delete this entry?')) {
    state.deleteEntry(entry.id);
    closeReaderModal();
    renderCurrentDestinationView();
  }
};

// ==========================================================================
// 16. MODAL 3 & 4: FUTURE LETTERS (1-to-1 FutureLettersScreens)
// ==========================================================================
window.openFutureLetterComposer = function() {
  const overlay = document.getElementById('letterComposerModalOverlay');
  const dateInput = document.getElementById('letterUnlockDateInput');
  const timeInput = document.getElementById('letterUnlockTimeInput');
  const titleInput = document.getElementById('letterTitleInput');
  const contentInput = document.getElementById('letterContentInput');

  // Next year default date
  const nextYear = new Date();
  nextYear.setFullYear(nextYear.getFullYear() + 1);
  if (dateInput) dateInput.value = DateUtils.toIso(nextYear);
  if (timeInput) timeInput.value = '09:00';
  if (titleInput) titleInput.value = '';
  if (contentInput) contentInput.value = '';

  state.letterComposerMood = 'WARM_ROOT';
  renderLetterMoodGrid();
  if (overlay) overlay.style.display = 'flex';
};

window.closeFutureLetterComposer = function() {
  const overlay = document.getElementById('letterComposerModalOverlay');
  if (overlay) overlay.style.display = 'none';
};

function renderLetterMoodGrid() {
  const grid = document.getElementById('letterMoodSelectorGrid');
  if (!grid) return;
  const isRu = state.lang === 'ru';

  grid.innerHTML = BotanicalMoodData.filter(m => m.key !== 'NONE').map(m => {
    const isSelected = state.letterComposerMood === m.key;
    const svg = BotanicalSVGs[m.key] ? BotanicalSVGs[m.key](26) : '';
    const title = isRu ? m.titleRu : m.titleEn;

    return `
      <div class="mood-select-card ${isSelected ? 'active' : ''}" 
           style="--mood-color: ${m.color}; --mood-color-soft: ${m.soft};" 
           onclick="state.letterComposerMood = '${m.key}'; renderLetterMoodGrid();">
        <div>${svg}</div>
        <div class="mood-select-name">${title}</div>
      </div>
    `;
  }).join('');
}

window.submitSaveFutureLetter = function() {
  const content = (document.getElementById('letterContentInput')?.value || '').trim();
  if (!content) {
    alert(state.lang === 'ru' ? 'Напишите текст письма перед сохранением.' : 'Please write your letter before saving.');
    return;
  }

  const unlockDate = document.getElementById('letterUnlockDateInput')?.value || '2027-01-01';
  const unlockTime = document.getElementById('letterUnlockTimeInput')?.value || '09:00';
  const title = (document.getElementById('letterTitleInput')?.value || '').trim();
  const today = DateUtils.todayIso();

  if (unlockDate <= today) {
    alert(state.lang === 'ru' ? 'Дата вскрытия письма должна быть в будущем!' : 'Unlock date must be in the future!');
    return;
  }

  const letterEntry = {
    id: `capsule_${Date.now()}`,
    dateIso: unlockDate,
    moodKey: state.letterComposerMood || 'WARM_ROOT',
    moodIntensity: 4,
    title: title,
    content: content,
    reflectionPrompt: `WRITTEN_AT:${today}|UNLOCK_TIME:${unlockTime}`,
    reflectionAnswer: null,
    music: '',
    tags: ['capsule', 'future_letter'],
    isFutureLetter: true,
    timestamp: Date.now()
  };

  state.addOrUpdateEntry(letterEntry);
  closeFutureLetterComposer();
  renderLettersTab();
};

window.openSealedLetterModal = function(letterId) {
  const letter = state.entries.find(e => e.id === letterId);
  if (!letter) return;

  const overlay = document.getElementById('sealedLetterModalOverlay');
  const details = document.getElementById('sealedLetterMetaDetails');
  const isRu = state.lang === 'ru';

  const timeHhMm = letter.reflectionPrompt?.includes('UNLOCK_TIME:') ? letter.reflectionPrompt.split('UNLOCK_TIME:')[1].slice(0, 5) : '09:00';
  const writtenDate = letter.reflectionPrompt?.includes('WRITTEN_AT:') ? letter.reflectionPrompt.split('WRITTEN_AT:')[1].slice(0, 10) : letter.dateIso;
  const countdown = DateUtils.formatCountdown(letter.dateIso, timeHhMm, state.lang);

  if (details) {
    details.innerHTML = `
      <div><strong>${isRu ? 'Отправлено:' : 'Sent on:'}</strong> ${DateUtils.formatFullDate(writtenDate, state.lang)}</div>
      <div style="margin-top: 4px;"><strong>${isRu ? 'Дата открытия:' : 'Unlock date:'}</strong> ${DateUtils.formatFullDate(letter.dateIso, state.lang)} (${timeHhMm})</div>
      <div style="margin-top: 8px; color: var(--accent-amber); font-weight: 700;">
        ${countdown}
      </div>
    `;
  }

  if (overlay) overlay.style.display = 'flex';
};

window.closeSealedLetterModal = function() {
  const overlay = document.getElementById('sealedLetterModalOverlay');
  if (overlay) overlay.style.display = 'none';
};

// ==========================================================================
// 17. MODAL 5: SETTINGS SCREEN (1-to-1 SettingsScreen from Kotlin)
// ==========================================================================
window.openSettingsModal = function() {
  const overlay = document.getElementById('settingsModalOverlay');
  updateAuthUI(state.currentUser);

  // Update Language chips
  const chipEn = document.getElementById('langChipEn');
  const chipRu = document.getElementById('langChipRu');
  if (chipEn) chipEn.classList.toggle('active', state.lang === 'en');
  if (chipRu) chipRu.classList.toggle('active', state.lang === 'ru');

  // Update Theme chips
  const themeDark = document.getElementById('themeChipDark');
  const themeLight = document.getElementById('themeChipLight');
  if (themeDark) themeDark.classList.toggle('active', state.theme === 'dark');
  if (themeLight) themeLight.classList.toggle('active', state.theme === 'light');

  if (overlay) overlay.style.display = 'flex';
};

window.closeSettingsModal = function() {
  const overlay = document.getElementById('settingsModalOverlay');
  if (overlay) overlay.style.display = 'none';
};

window.setLanguage = function(lang) {
  state.lang = lang;
  localStorage.setItem('vent_lang', lang);
  document.documentElement.setAttribute('lang', lang);

  const dict = I18N[lang] || I18N.ru;
  document.querySelectorAll('[data-t]').forEach(el => {
    const key = el.getAttribute('data-t');
    if (dict[key]) el.textContent = dict[key];
  });

  const chipEn = document.getElementById('langChipEn');
  const chipRu = document.getElementById('langChipRu');
  if (chipEn) chipEn.classList.toggle('active', lang === 'en');
  if (chipRu) chipRu.classList.toggle('active', lang === 'ru');

  navigateTo(state.currentDestination);
};

window.applyThemeMode = function(theme) {
  state.theme = theme;
  localStorage.setItem('vent_theme', theme);
  document.documentElement.setAttribute('data-theme', theme);

  const themeDark = document.getElementById('themeChipDark');
  const themeLight = document.getElementById('themeChipLight');
  if (themeDark) themeDark.classList.toggle('active', theme === 'dark');
  if (themeLight) themeLight.classList.toggle('active', theme === 'light');
};

window.clearAllEntriesAction = function() {
  const isRu = state.lang === 'ru';
  if (confirm(isRu ? 'Вы уверены, что хотите удалить ВСЕ записи и письма? Это действие необратимо.' : 'Are you sure you want to delete ALL entries? This action cannot be undone.')) {
    state.clearAllEntries();
    closeSettingsModal();
    renderCurrentDestinationView();
  }
};

function escapeHtml(str) {
  return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ==========================================================================
// 18. INITIALIZATION
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
  document.documentElement.setAttribute('data-theme', state.theme);
  setLanguage(state.lang);
  initFirebaseSync();
  navigateTo('JOURNAL');
});
