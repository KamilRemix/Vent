package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.PeriodInsightDetector
import com.example.data.ai.PsychologicalAiAnalyzer
import com.example.data.model.DiaryEntry
import com.example.data.preferences.PinLockPreferences
import com.example.data.repository.DiaryRepository
import com.example.notifications.LetterUnlockScheduler
import com.example.ui.auth.AuthWelcomeScreen
import com.example.ui.auth.BotanicalPinLockScreen
import com.example.ui.auth.PinSetupDialog
import com.example.ui.auth.launchOneTapOrGoogleSignIn
import com.example.ui.auth.signOutFromGoogle
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DiaryViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.AppLanguage
import com.example.util.AppLocaleHolder
import com.example.util.BiometricAuthHelper
import com.example.util.DateAndPhotoUtils
import com.example.util.LanguagePreferences
import com.example.util.LocalAppLanguage
import com.example.util.configureAppCheck
import com.example.util.tr
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class MainDestination(
    val labelEn: String,
    val labelRu: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    JOURNAL("Journal", "Дневник", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_journal"),
    CALENDAR("Calendar", "Календарь", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    LETTERS("Capsule", "Капсула", Icons.Filled.HourglassTop, Icons.Outlined.HourglassEmpty, "nav_letters"),
    SEARCH("Search", "Поиск", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    INSIGHTS("Insights", "Аналитика", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_insights");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) labelRu else labelEn
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        // Initialize language preference from phone locale (or user choice) before creating notification channel
        LanguagePreferences(this).getResolvedLanguage()
        LetterUnlockScheduler.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BotanicalDiaryApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotanicalDiaryApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val pinPrefs = remember(context) { PinLockPreferences(context) }
    val langPrefs = remember(context) { LanguagePreferences(context) }

    var languageMode by remember { mutableStateOf(langPrefs.getLanguageMode()) }
    val resolvedLanguage = remember(languageMode) {
        val lang = languageMode.resolveLanguage()
        AppLocaleHolder.currentLanguage = lang
        lang
    }

    var currentUserId by remember {
        mutableStateOf(
            Firebase.auth.currentUser?.uid
                ?: if (pinPrefs.isAuthSkipped()) PinLockPreferences.LOCAL_GUEST_UID else null
        )
    }
    var currentUserEmail by remember { mutableStateOf(Firebase.auth.currentUser?.email) }
    var isPinEnabled by remember { mutableStateOf(pinPrefs.isPinEnabled()) }
    var isPinLocked by remember { mutableStateOf(pinPrefs.isPinEnabled()) }
    var isBiometricEnabled by remember { mutableStateOf(pinPrefs.isBiometricEnabled()) }
    val isBiometricAvailable = remember(context) {
        BiometricAuthHelper.isBiometricAvailable(context)
    }

    // Runtime permission state for POST_NOTIFICATIONS on Android 13+ (API 33+)
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsGranted = isGranted
    }

    // Prompt for POST_NOTIFICATIONS permission on Android 13+ when entering the app
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides resolvedLanguage) {
        // 1. Authentication Gate (Google Sign-In via CredentialManager or Skippable Guest Mode)
        val activeUid = currentUserId
        if (activeUid == null) {
            AuthWelcomeScreen(
                onAuthSuccess = {
                    pinPrefs.setAuthSkipped(false)
                    currentUserId = Firebase.auth.currentUser?.uid
                    currentUserEmail = Firebase.auth.currentUser?.email
                },
                onSkipClick = {
                    pinPrefs.setAuthSkipped(true)
                    currentUserId = PinLockPreferences.LOCAL_GUEST_UID
                }
            )
            return@CompositionLocalProvider
        }

        // 2. Local PIN & Biometric (Fingerprint) Lock Gate
        if (isPinEnabled && isPinLocked) {
            BotanicalPinLockScreen(
                isBiometricEnabled = isBiometricEnabled,
                onVerifyPin = { code -> pinPrefs.verifyPin(code) },
                onUnlocked = { isPinLocked = false }
            )
            return@CompositionLocalProvider
        }

        val repository = remember(context) { DiaryRepository(context) }
        val aiAnalyzer = remember(context) { PsychologicalAiAnalyzer(context) }
        val viewModel: DiaryViewModel = viewModel(
            key = "diary_vm_$activeUid",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiaryViewModel(
                        repository = repository,
                        currentUserId = activeUid,
                        aiAnalyzer = aiAnalyzer
                    ) as T
                }
            }
        )

        val entriesUiState by viewModel.entriesState.collectAsStateWithLifecycle()
        val futureLetters by viewModel.futureLettersState.collectAsStateWithLifecycle()
        val filteredEntries by viewModel.filteredSearchEntries.collectAsStateWithLifecycle()
        val selectedDateIso by viewModel.selectedDateIso.collectAsStateWithLifecycle()
        val calendarYear by viewModel.calendarYear.collectAsStateWithLifecycle()
        val calendarMonthZeroBased by viewModel.calendarMonthZeroBased.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val selectedMoodFilter by viewModel.selectedMoodFilter.collectAsStateWithLifecycle()
        val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
        val statusMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
        val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
        val selectedAiCategory by viewModel.selectedAiCategory.collectAsStateWithLifecycle()
        val selectedAiPeriodKey by viewModel.selectedAiPeriodKey.collectAsStateWithLifecycle()
        val savedAiReports by viewModel.savedAiReports.collectAsStateWithLifecycle()
        val aiAnalysisUiState by viewModel.aiAnalysisUiState.collectAsStateWithLifecycle()

        val entries = remember(entriesUiState) {
            (entriesUiState as? UiState.Success)?.data.orEmpty()
        }
        val entriesForSelectedDate = remember(entries, selectedDateIso) {
            entries.filter { it.dateIso == selectedDateIso }
        }
        val onThisDayEntries = remember(entries, selectedDateIso) {
            val targetMonthDay = selectedDateIso.takeLast(5)
            val targetYear = selectedDateIso.take(4)
            entries.filter { it.monthDay == targetMonthDay && !it.dateIso.startsWith(targetYear) }
        }
        val allTags = remember(entries) {
            entries.flatMap { it.tags }.distinct().sorted()
        }
        val todayIso = remember { DateAndPhotoUtils.todayIso() }
        val availableAiPeriods = remember(entries, todayIso) {
            PeriodInsightDetector.buildAvailablePeriods(entries, todayIso)
        }
        val recommendedHomeAiPeriod = remember(entries, savedAiReports, todayIso) {
            PeriodInsightDetector.recommendedHomeBannerPeriod(entries, savedAiReports, todayIso)
        }

        // Ensure any sealed future letters synced from Firestore have a WorkManager notification scheduled
        LaunchedEffect(futureLetters) {
            futureLetters.forEach { letter ->
                if (!letter.isUnlocked(todayIso)) {
                    LetterUnlockScheduler.scheduleUnlockNotification(
                        context = context,
                        letterId = letter.id,
                        unlockDateIso = letter.dateIso,
                        writtenDateIso = letter.writtenDateIso(),
                        unlockHourOfDay = letter.unlockHour(),
                        unlockMinute = letter.unlockMinute(),
                        replaceExisting = false
                    )
                }
            }
        }

        // Navigation & Sub-screen State
        var currentTab by remember { mutableStateOf(MainDestination.JOURNAL) }
        var viewingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
        var isEditorOpen by remember { mutableStateOf(false) }
        var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
        var editorTargetDateIso by remember { mutableStateOf(selectedDateIso) }
        var isFutureLetterComposerOpen by remember { mutableStateOf(false) }
        var readingFutureLetter by remember { mutableStateOf<DiaryEntry?>(null) }
        var showPinSetupDialog by remember { mutableStateOf(false) }
        var isSettingsOpen by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(statusMessage) {
            statusMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusBanner()
            }
        }

        // 3. PIN Setup Modal
        if (showPinSetupDialog) {
            PinSetupDialog(
                isPinCurrentlyEnabled = isPinEnabled,
                onDismiss = { showPinSetupDialog = false },
                onSavePin = { newPin ->
                    pinPrefs.setPin(newPin)
                    isPinEnabled = true
                    isPinLocked = false
                    showPinSetupDialog = false
                },
                onDisablePin = {
                    pinPrefs.clearPin()
                    isPinEnabled = false
                    isPinLocked = false
                    showPinSetupDialog = false
                }
            )
        }

        // 4. Unlocked Future Letter Reader Modal
        readingFutureLetter?.let { letter ->
            FutureLetterReaderDialog(
                letter = letter,
                onDismiss = { readingFutureLetter = null },
                onDelete = { id ->
                    viewModel.deleteDiaryEntry(context = context, entryId = id)
                    readingFutureLetter = null
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Main Shell, Reader, or Settings
            when {
                isSettingsOpen -> {
                    SettingsScreen(
                        languageMode = languageMode,
                        onLanguageModeChange = { newMode ->
                            langPrefs.setLanguageMode(newMode)
                            languageMode = newMode
                            LetterUnlockScheduler.createNotificationChannel(context)
                        },
                        isPinEnabled = isPinEnabled,
                        isBiometricAvailable = isBiometricAvailable,
                        isBiometricEnabled = isBiometricEnabled,
                        onBiometricEnabledChange = { enabled ->
                            pinPrefs.setBiometricEnabled(enabled)
                            isBiometricEnabled = enabled
                        },
                        userEmail = currentUserEmail,
                        isSignedInWithGoogle = activeUid != PinLockPreferences.LOCAL_GUEST_UID,
                        onSignInWithGoogle = {
                            launchOneTapOrGoogleSignIn(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    pinPrefs.setAuthSkipped(false)
                                    val newUid = Firebase.auth.currentUser?.uid
                                    currentUserId = newUid
                                    currentUserEmail = Firebase.auth.currentUser?.email
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (resolvedLanguage == AppLanguage.RU) "Вход в Google выполнен" else "Signed in to Google"
                                        )
                                    }
                                },
                                onAuthError = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                },
                                scope = scope
                            )
                        },
                        notificationsGranted = notificationsGranted,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onSetupOrChangePin = { showPinSetupDialog = true },
                        onDisablePin = {
                            pinPrefs.clearPin()
                            isPinEnabled = false
                            isPinLocked = false
                        },
                        onLockNow = {
                            if (isPinEnabled) {
                                isSettingsOpen = false
                                isPinLocked = true
                            }
                        },
                        onClearAllEntries = { viewModel.clearAllEntries() },
                        onSignOut = {
                            isSettingsOpen = false
                            pinPrefs.setAuthSkipped(false)
                            signOutFromGoogle(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    currentUserId = null
                                    currentUserEmail = null
                                },
                                scope = scope
                            )
                        },
                        onBack = { isSettingsOpen = false }
                    )
                }

                viewingEntry != null -> {
                    val entryToRead = viewingEntry!!
                    val liveEntry = entries.firstOrNull { it.id == entryToRead.id } ?: entryToRead
                    DiaryEntryReaderScreen(
                        entry = liveEntry,
                        onEditClick = {
                            editingEntry = liveEntry
                            editorTargetDateIso = liveEntry.dateIso
                            isEditorOpen = true
                        },
                        onDeleteClick = {
                            viewModel.deleteDiaryEntry(context = context, entryId = liveEntry.id)
                            viewingEntry = null
                        },
                        onBack = { viewingEntry = null }
                    )
                }

                else -> {

        // 9. Main Adaptive Navigation Shell (TopAppBar with Settings Gear + No colliding FAB)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Vent",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = currentTab.label(resolvedLanguage),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { isSettingsOpen = true },
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = tr("Settings", "Настройки"),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentTab == dest
                                val tabLabel = dest.label(resolvedLanguage)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = dest },
                                    alwaysShowLabel = false,
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = tabLabel
                                        )
                                    },
                                    label = { Text(tabLabel, maxLines = 1) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail {
                            Spacer(modifier = Modifier.height(12.dp))
                            MainDestination.entries.forEach { dest ->
                                val selected = currentTab == dest
                                val tabLabel = dest.label(resolvedLanguage)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = tabLabel
                                        )
                                    },
                                    label = { Text(tabLabel) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            MainDestination.JOURNAL -> {
                                JournalHomeTab(
                                    entries = entries,
                                    selectedDateIso = selectedDateIso,
                                    entriesForSelectedDate = entriesForSelectedDate,
                                    onThisDayEntries = onThisDayEntries,
                                    recommendedAiPeriod = recommendedHomeAiPeriod,
                                    hasSavedAiReport = recommendedHomeAiPeriod?.let { savedAiReports.containsKey(it.periodKey) } == true,
                                    onOpenAiAnalysis = { periodKey ->
                                        viewModel.selectAiPeriodKey(periodKey, entries)
                                        currentTab = MainDestination.INSIGHTS
                                        val targetPeriod = availableAiPeriods.firstOrNull { it.periodKey == periodKey }
                                        if (targetPeriod != null && !savedAiReports.containsKey(periodKey)) {
                                            viewModel.generateAiPsychologicalReport(context, targetPeriod, entries)
                                        }
                                    },
                                    onCreateNewForDate = { dateIso ->
                                        editingEntry = null
                                        editorTargetDateIso = dateIso
                                        isEditorOpen = true
                                    },
                                    onViewEntry = { entry ->
                                        viewingEntry = entry
                                    },
                                    onOpenCalendar = {
                                        currentTab = MainDestination.CALENDAR
                                    }
                                )
                            }

                            MainDestination.CALENDAR -> {
                                CalendarTab(
                                    entries = entries,
                                    selectedDateIso = selectedDateIso,
                                    calendarYear = calendarYear,
                                    calendarMonthZeroBased = calendarMonthZeroBased,
                                    onSelectDate = { dateIso -> viewModel.selectDate(dateIso) },
                                    onPreviousMonth = { viewModel.navigateCalendarMonth(-1) },
                                    onNextMonth = { viewModel.navigateCalendarMonth(1) },
                                    onCreateEntryForDate = { dateIso ->
                                        editingEntry = null
                                        editorTargetDateIso = dateIso
                                        isEditorOpen = true
                                    },
                                    onViewEntry = { entry ->
                                        viewingEntry = entry
                                    }
                                )
                            }

                            MainDestination.LETTERS -> {
                                FutureLettersTab(
                                    futureLetters = futureLetters,
                                    todayIso = todayIso,
                                    onComposeLetter = { isFutureLetterComposerOpen = true },
                                    onOpenUnlockedLetter = { letter -> readingFutureLetter = letter },
                                    onDeleteLetter = { id ->
                                        viewModel.deleteDiaryEntry(context = context, entryId = id)
                                    }
                                )
                            }

                            MainDestination.SEARCH -> {
                                SearchAndTagsTab(
                                    filteredEntries = filteredEntries,
                                    allTags = allTags,
                                    searchQuery = searchQuery,
                                    selectedMoodFilter = selectedMoodFilter,
                                    selectedTagFilter = selectedTagFilter,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onMoodFilterChange = { viewModel.toggleMoodFilter(it) },
                                    onTagFilterChange = { viewModel.toggleTagFilter(it) },
                                    onClearFilters = {
                                        viewModel.updateSearchQuery("")
                                        if (selectedMoodFilter != null) viewModel.toggleMoodFilter(selectedMoodFilter)
                                        if (selectedTagFilter != null) viewModel.toggleTagFilter(selectedTagFilter)
                                    },
                                    onViewEntry = { entry ->
                                        viewingEntry = entry
                                    }
                                )
                            }

                            MainDestination.INSIGHTS -> {
                                AnalyticsTab(
                                    entries = entries,
                                    availableAiPeriods = availableAiPeriods,
                                    selectedAiCategory = selectedAiCategory,
                                    selectedAiPeriodKey = selectedAiPeriodKey,
                                    savedAiReports = savedAiReports,
                                    aiUiState = aiAnalysisUiState,
                                    onSelectAiCategory = { cat -> viewModel.selectAiCategory(cat, entries) },
                                    onSelectAiPeriodKey = { key -> viewModel.selectAiPeriodKey(key, entries) },
                                    onGenerateOrRefreshAiReport = { period ->
                                        viewModel.generateAiPsychologicalReport(context, period, entries)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
                }
            }

            // Smooth vertical slide-up animation for New / Edit Entry Screen
            AnimatedVisibility(
                visible = isEditorOpen,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 320)
                ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 280)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiaryEntryEditorScreen(
                        initialEntry = editingEntry,
                        targetDateIso = editorTargetDateIso,
                        isUploadingPhoto = isUploadingPhoto,
                        onUploadPhoto = { uri, onSuccess ->
                            viewModel.uploadPhotoAsWebp(context, uri, onSuccess)
                        },
                        onSave = { existingId, dateIso, mood, moodIntensity, title, content, reflectionPrompt, reflectionAnswer, tags, photoUrls ->
                            viewModel.saveDiaryEntry(
                                existingEntryId = existingId,
                                dateIso = dateIso,
                                mood = mood,
                                moodIntensity = moodIntensity,
                                title = title,
                                content = content,
                                reflectionPrompt = reflectionPrompt,
                                reflectionAnswer = reflectionAnswer,
                                tags = tags,
                                photoUrls = photoUrls
                            )
                            isEditorOpen = false
                            editingEntry = null
                            viewingEntry = null
                        },
                        onDelete = editingEntry?.let {
                            { id ->
                                viewModel.deleteDiaryEntry(context = context, entryId = id)
                                isEditorOpen = false
                                editingEntry = null
                                viewingEntry = null
                            }
                        },
                        onCancel = {
                            isEditorOpen = false
                            editingEntry = null
                        }
                    )
                }
            }

            // Smooth vertical slide-up animation for Future Letter Composer
            AnimatedVisibility(
                visible = isFutureLetterComposerOpen,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 320)
                ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 280)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FutureLetterComposerScreen(
                        todayIso = todayIso,
                        isUploadingPhoto = isUploadingPhoto,
                        onUploadPhoto = { uri, onSuccess ->
                            viewModel.uploadPhotoAsWebp(context, uri, onSuccess)
                        },
                        onSaveLetter = { unlockDateIso, unlockHour, unlockMinute, title, content, mood, photoUrls ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.saveFutureLetter(
                                context = context,
                                unlockDateIso = unlockDateIso,
                                unlockHour = unlockHour,
                                unlockMinute = unlockMinute,
                                mood = mood,
                                title = title,
                                content = content,
                                hopeOrGoal = null,
                                photoUrls = photoUrls
                            )
                            isFutureLetterComposerOpen = false
                        },
                        onCancel = { isFutureLetterComposerOpen = false }
                    )
                }
            }
        }
    }
}

