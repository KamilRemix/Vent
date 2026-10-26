package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.DiaryEntry
import com.example.data.preferences.PinLockPreferences
import com.example.data.repository.DiaryRepository
import com.example.ui.auth.AuthWelcomeScreen
import com.example.ui.auth.BotanicalPinLockScreen
import com.example.ui.auth.PinSetupDialog
import com.example.ui.auth.signOutFromGoogle
import com.example.ui.screens.AnalyticsAndProfileTab
import com.example.ui.screens.CalendarTab
import com.example.ui.screens.DiaryEntryEditorScreen
import com.example.ui.screens.JournalHomeTab
import com.example.ui.screens.SearchAndTagsTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DiaryViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyDiaryAppRoot()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class DiaryTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    JOURNAL(R.string.nav_journal, Icons.Filled.Book, Icons.Outlined.Book, "nav_tab_journal"),
    CALENDAR(R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_calendar"),
    SEARCH(R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
    ANALYTICS(R.string.nav_analytics, Icons.Filled.AutoGraph, Icons.Outlined.AutoGraph, "nav_tab_analytics")
}

@Composable
fun MyDiaryAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthWelcomeScreen(onAuthSuccess = {})
    } else {
        AuthenticatedDiaryShell(
            currentUserId = user.uid,
            userEmail = user.email
        )
    }
}

@Composable
fun AuthenticatedDiaryShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinPrefs = remember(context) { PinLockPreferences(context) }
    var isPinEnabled by remember { mutableStateOf(pinPrefs.isPinEnabled()) }
    var isPinUnlocked by rememberSaveable { mutableStateOf(!pinPrefs.isPinEnabled()) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    val viewModel: DiaryViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DiaryViewModel(DiaryRepository(db), currentUserId)
            }
        }
    )

    if (isPinEnabled && !isPinUnlocked) {
        BotanicalPinLockScreen(
            onVerifyPin = { pin -> pinPrefs.verifyPin(pin) },
            onUnlocked = { isPinUnlocked = true }
        )
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(DiaryTab.JOURNAL) }
    var isEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var editorInitialDateIso by rememberSaveable { mutableStateOf<String?>(null) }

    val entriesState by viewModel.entriesState.collectAsStateWithLifecycle()
    val selectedDateIso by viewModel.selectedDateIso.collectAsStateWithLifecycle()
    val calendarYear by viewModel.calendarYear.collectAsStateWithLifecycle()
    val calendarMonthZeroBased by viewModel.calendarMonthZeroBased.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val selectedMoodFilter by viewModel.selectedMoodFilter.collectAsStateWithLifecycle()
    val filteredSearchEntries by viewModel.filteredSearchEntries.collectAsStateWithLifecycle()
    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    if (currentTab != DiaryTab.JOURNAL && !isEditorOpen) {
        BackHandler { currentTab = DiaryTab.JOURNAL }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            isPinCurrentlyEnabled = isPinEnabled,
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { newPin ->
                pinPrefs.setPin(newPin)
                isPinEnabled = true
                isPinUnlocked = true
            },
            onDisablePin = {
                pinPrefs.clearPin()
                isPinEnabled = false
                isPinUnlocked = true
            }
        )
    }

    if (isEditorOpen) {
        DiaryEntryEditorScreen(
            initialDateIso = editorInitialDateIso ?: selectedDateIso,
            existingEntry = editingEntry,
            isSaving = isSaving,
            onDismiss = {
                isEditorOpen = false
                editingEntry = null
            },
            onSave = { existingId, dateIso, mood, intensity, title, content, prompt, answer, tags, photos ->
                viewModel.saveDiaryEntry(
                    existingEntryId = existingId,
                    dateIso = dateIso,
                    mood = mood,
                    moodIntensity = intensity,
                    title = title,
                    content = content,
                    reflectionPrompt = prompt,
                    reflectionAnswer = answer,
                    tags = tags,
                    photosBase64 = photos,
                    onSaved = {
                        isEditorOpen = false
                        editingEntry = null
                    }
                )
            },
            onDelete = { entryId ->
                viewModel.deleteDiaryEntry(
                    entryId = entryId,
                    onDeleted = {
                        isEditorOpen = false
                        editingEntry = null
                    }
                )
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            if (currentTab == DiaryTab.JOURNAL || currentTab == DiaryTab.CALENDAR) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingEntry = null
                        editorInitialDateIso = selectedDateIso
                        isEditorOpen = true
                    },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_entry_button)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_entry_fab")
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DiaryTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = { Text(stringResource(tab.titleRes)) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = statusBannerMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.clearStatusBanner() }) {
                            Text("ОК")
                        }
                    }
                }
            }

            when (val state = entriesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val entries = state.data
                    when (currentTab) {
                        DiaryTab.JOURNAL -> JournalHomeTab(
                            entries = entries,
                            selectedDateIso = selectedDateIso,
                            onSelectDate = { viewModel.selectDate(it) },
                            onOpenNewEntryForDate = { date ->
                                editingEntry = null
                                editorInitialDateIso = date
                                isEditorOpen = true
                            },
                            onEditEntry = { entry ->
                                editingEntry = entry
                                editorInitialDateIso = entry.dateIso
                                isEditorOpen = true
                            },
                            onTagClicked = { tag ->
                                viewModel.toggleTagFilter(tag)
                                currentTab = DiaryTab.SEARCH
                            },
                            onJumpToCalendar = { currentTab = DiaryTab.CALENDAR },
                            onDeleteEntry = { entryId -> viewModel.deleteDiaryEntry(entryId) }
                        )
                        DiaryTab.CALENDAR -> CalendarTab(
                            entries = entries,
                            calendarYear = calendarYear,
                            calendarMonthZeroBased = calendarMonthZeroBased,
                            selectedDateIso = selectedDateIso,
                            onSelectDate = { viewModel.selectDate(it) },
                            onNavigateMonth = { viewModel.navigateCalendarMonth(it) },
                            onNavigateYear = { viewModel.navigateCalendarYear(it) },
                            onJumpToToday = { viewModel.jumpToToday() },
                            onOpenNewEntryForDate = { date ->
                                editingEntry = null
                                editorInitialDateIso = date
                                isEditorOpen = true
                            },
                            onEditEntry = { entry ->
                                editingEntry = entry
                                editorInitialDateIso = entry.dateIso
                                isEditorOpen = true
                            },
                            onTagClicked = { tag ->
                                viewModel.toggleTagFilter(tag)
                                currentTab = DiaryTab.SEARCH
                            }
                        )
                        DiaryTab.SEARCH -> SearchAndTagsTab(
                            allEntries = entries,
                            filteredEntries = filteredSearchEntries,
                            searchQuery = searchQuery,
                            selectedTag = selectedTagFilter,
                            selectedMood = selectedMoodFilter,
                            onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                            onToggleTag = { viewModel.toggleTagFilter(it) },
                            onToggleMood = { viewModel.toggleMoodFilter(it) },
                            onEditEntry = { entry ->
                                editingEntry = entry
                                editorInitialDateIso = entry.dateIso
                                isEditorOpen = true
                            }
                        )
                        DiaryTab.ANALYTICS -> AnalyticsAndProfileTab(
                            analytics = analyticsData,
                            userEmail = userEmail,
                            isPinEnabled = isPinEnabled,
                            onOpenPinSetup = { showPinSetupDialog = true },
                            onLockNow = {
                                if (isPinEnabled) {
                                    isPinUnlocked = false
                                }
                            },
                            onClearAllEntries = { viewModel.clearAllEntries() },
                            onSignOut = {
                                val credentialManager = CredentialManager.create(context)
                                signOutFromGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
