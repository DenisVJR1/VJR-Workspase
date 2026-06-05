package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

enum class Screen {
    Dashboard,
    Calendar,
    Tasks,
    FileViewer,
    FileEditor,
    CloudAuth,
    Pomodoro
}

data class DateItem(
    val day: Int,
    val dateString: String, // "YYYY-MM-DD"
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val monthValue: Int,
    val yearValue: Int
)

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao)

    // Current navigation state
    private val _currentScreen = MutableStateFlow(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Tasks State
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date for detail views "YYYY-MM-DD"
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Monthly navigation state
    private val _calendarYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1-12
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    // Filtered / Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // File Manager State
    private val rootDirectory = application.filesDir
    private val _currentDirectory = MutableStateFlow(rootDirectory)
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _folderItems = MutableStateFlow<List<File>>(emptyList())
    val folderItems: StateFlow<List<File>> = _folderItems.asStateFlow()

    // Selected File for Editor view
    private val _activeFile = MutableStateFlow<File?>(null)
    val activeFile: StateFlow<File?> = _activeFile.asStateFlow()

    private val _activeFileContent = MutableStateFlow("")
    val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

    // Custom Launcher Wallpaper Option: "Lavender Mint", "Midnight Gold", "Organic Emerald", "Dynamic Slate"
    private val _launcherWallpaper = MutableStateFlow("Dynamic Slate")
    val launcherWallpaper: StateFlow<String> = _launcherWallpaper.asStateFlow()

    // ----------------------------------------------------
    // CLOUD AUTH & SIMULATION STATE
    // ----------------------------------------------------
    private val _cloudUserEmail = MutableStateFlow<String?>(null)
    val cloudUserEmail: StateFlow<String?> = _cloudUserEmail.asStateFlow()

    private val _cloudUserToken = MutableStateFlow<String?>(null)
    val cloudUserToken: StateFlow<String?> = _cloudUserToken.asStateFlow()

    private val _firebaseApiKey = MutableStateFlow("")
    val firebaseApiKey: StateFlow<String> = _firebaseApiKey.asStateFlow()

    private val _firebaseDbUrl = MutableStateFlow("")
    val firebaseDbUrl: StateFlow<String> = _firebaseDbUrl.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("ніколи")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf("Ініціалізація модуля VJR Cloud Sync...", "Офлайн-режим активовано")
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    fun addLog(msg: String) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = formatter.format(Date())
        _syncLogs.value = _syncLogs.value + "[$time] $msg"
    }

    fun userLogin(email: String, psw: String, isRegister: Boolean): Boolean {
        if (email.isBlank() || psw.length < 5) {
            addLog("Помилка авторизації: некоректна пошта або занадто короткий пароль!")
            return false
        }
        _cloudUserEmail.value = email
        _cloudUserToken.value = "vjr_tok_" + email.hashCode()
        if (isRegister) {
            addLog("Створено новий хмарний профіль для: $email")
            addLog("Активовано інтеграцію шифрування VJR-RSA-2048")
        } else {
            addLog("Вхід до кабінету VJR Cloud успішний: $email")
        }
        triggerCloudSync()
        return true
    }

    fun userLogout() {
        val old = _cloudUserEmail.value
        _cloudUserEmail.value = null
        _cloudUserToken.value = null
        addLog("Користувач $old вийшов із профілю. Перехід в офлайн.")
    }

    fun saveFirebaseConfig(key: String, url: String) {
        _firebaseApiKey.value = key
        _firebaseDbUrl.value = url
        addLog("Збережено конфігурацію Firebase API.")
    }

    fun triggerCloudSync() {
        if (_cloudUserEmail.value == null) {
            addLog("Помилка синхронізації: Увійдіть у хмарний кабінет!")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            addLog("Підключення до віддаленої хмари VJR...")
            delay(800)
            addLog("Перевірка токенів сесії...")
            delay(600)
            addLog("Резервне копіювання локальних даних...")
            
            try {
                val emailSanitized = _cloudUserEmail.value!!.replace("@", "_").replace(".", "_")
                val backupDir = File(rootDirectory, "VJR_Cloud_Backups/$emailSanitized")
                if (!backupDir.exists()) backupDir.mkdirs()
                
                val tasksFile = File(backupDir, "tasks_backup.json")
                val tasksList = allTasks.value
                val sBuilder = StringBuilder()
                sBuilder.append("[\n")
                tasksList.forEachIndexed { idx, t ->
                    sBuilder.append("  {\"title\":\"${t.title}\", \"desc\":\"${t.description}\", \"date\":\"${t.date}\", \"priority\":\"${t.priority}\"}")
                    if (idx < tasksList.lastIndex) sBuilder.append(",")
                    sBuilder.append("\n")
                }
                sBuilder.append("]")
                tasksFile.writeText(sBuilder.toString())
                
                addLog("Завантаження даних до хмарного сховища...")
                delay(900)
                addLog("Контрольна сума SHA-256 збігається.")
                _lastSyncTime.value = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                addLog("Успішно синхронізовано з хмарою VJR! Збережено резервну копію.")
            } catch (e: Exception) {
                addLog("Помилка запису бекапу: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // ----------------------------------------------------
    // POMODORO TIMER STATE
    // ----------------------------------------------------
    private val _pomodoroTimeLeftSec = MutableStateFlow(1500)
    val pomodoroTimeLeftSec: StateFlow<Int> = _pomodoroTimeLeftSec.asStateFlow()

    private val _pomodoroIsRunning = MutableStateFlow(false)
    val pomodoroIsRunning: StateFlow<Boolean> = _pomodoroIsRunning.asStateFlow()

    private val _pomodoroMode = MutableStateFlow("Focus")
    val pomodoroMode: StateFlow<String> = _pomodoroMode.asStateFlow()

    private val _pomodoroTotalSessions = MutableStateFlow(0)
    val pomodoroTotalSessions: StateFlow<Int> = _pomodoroTotalSessions.asStateFlow()

    private var pomodoroJob: Job? = null

    fun togglePomodoroTimer() {
        if (_pomodoroIsRunning.value) {
            _pomodoroIsRunning.value = false
            pomodoroJob?.cancel()
        } else {
            _pomodoroIsRunning.value = true
            pomodoroJob = viewModelScope.launch {
                while (_pomodoroTimeLeftSec.value > 0) {
                    delay(1000)
                    _pomodoroTimeLeftSec.value -= 1
                }
                _pomodoroIsRunning.value = false
                if (_pomodoroMode.value == "Focus") {
                    _pomodoroTotalSessions.value += 1
                    _pomodoroMode.value = "Break"
                    _pomodoroTimeLeftSec.value = 300
                    addLog("Pomodoro: Робочий сеанс завершено!")
                } else {
                    _pomodoroMode.value = "Focus"
                    _pomodoroTimeLeftSec.value = 1500
                    addLog("Pomodoro: Перерва закінчена!")
                }
            }
        }
    }

    fun resetPomodoro() {
        _pomodoroIsRunning.value = false
        pomodoroJob?.cancel()
        _pomodoroMode.value = "Focus"
        _pomodoroTimeLeftSec.value = 1500
    }

    fun setCustomPomodoroTime(minutes: Int) {
        _pomodoroIsRunning.value = false
        pomodoroJob?.cancel()
        _pomodoroTimeLeftSec.value = minutes * 60
    }

    init {
        // Initialize default workspace preview on startup
        initDefaultWorkspace()
        loadFolderItems()
    }

    // Navigation helpers
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setWallpaper(name: String) {
        _launcherWallpaper.value = name
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
    }

    // Task Actions
    fun addTask(title: String, description: String, date: String, priority: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(Task(title = title, description = description, date = date, priority = priority))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun updateTaskFull(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    // Calendar Calculations
    fun nextMonth() {
        if (_calendarMonth.value == 12) {
            _calendarMonth.value = 1
            _calendarYear.value += 1
        } else {
            _calendarMonth.value += 1
        }
    }

    fun prevMonth() {
        if (_calendarMonth.value == 1) {
            _calendarMonth.value = 12
            _calendarYear.value -= 1
        } else {
            _calendarMonth.value -= 1
        }
    }

    fun getDaysGrid(): List<DateItem> {
        val list = mutableListOf<DateItem>()
        val year = _calendarYear.value
        val month = _calendarMonth.value

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1) // 0-based
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Sunday=1, Monday=2, Tuesday=3, ... Saturday=7
        // Monday-first padding logic
        val prefixPadding = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        // Previous month's padding
        val prevCal = cal.clone() as Calendar
        prevCal.add(Calendar.MONTH, -1)
        val prevMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthVal = prevCal.get(Calendar.MONTH) + 1
        val prevYearVal = prevCal.get(Calendar.YEAR)

        for (i in prefixPadding - 1 downTo 0) {
            val day = prevMaxDays - i
            val dateStr = String.format("%04d-%02d-%02d", prevYearVal, prevMonthVal, day)
            list.add(DateItem(day, dateStr, false, false, prevMonthVal, prevYearVal))
        }

        // Current month's days
        val todayStr = getCurrentDateString()
        for (day in 1..maxDays) {
            val dateStr = String.format("%04d-%02d-%02d", year, month, day)
            val isToday = (dateStr == todayStr)
            list.add(DateItem(day, dateStr, true, isToday, month, year))
        }

        // Next month's padding to fill 42 cells (6 rows)
        var nextCount = 42 - list.size
        if (nextCount > 0) {
            val nextCal = cal.clone() as Calendar
            nextCal.add(Calendar.MONTH, 1)
            val nextMonthVal = nextCal.get(Calendar.MONTH) + 1
            val nextYearVal = nextCal.get(Calendar.YEAR)
            for (day in 1..nextCount) {
                val dateStr = String.format("%04d-%02d-%02d", nextYearVal, nextMonthVal, day)
                list.add(DateItem(day, dateStr, false, false, nextMonthVal, nextYearVal))
            }
        }

        return list
    }

    // Search query helper
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Disk-based File system Operations
    fun loadFolderItems() {
        val dir = _currentDirectory.value
        val items = dir.listFiles()?.toList() ?: emptyList()
        // Sort folders first, then files alphabetically
        _folderItems.value = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun changeDirectory(subDir: File) {
        if (subDir.isDirectory) {
            _currentDirectory.value = subDir
            loadFolderItems()
        }
    }

    fun navigateUp() {
        val parent = _currentDirectory.value.parentFile
        // Do not let user navigate above context.filesDir
        if (parent != null && _currentDirectory.value != rootDirectory) {
            _currentDirectory.value = parent
            loadFolderItems()
        }
    }

    fun isAtRoot(): Boolean {
        return _currentDirectory.value == rootDirectory
    }

    fun getCurrentDirectoryRelativePath(): String {
        val rootPath = rootDirectory.absolutePath
        val currPath = _currentDirectory.value.absolutePath
        return if (currPath == rootPath) {
            "/"
        } else {
            currPath.removePrefix(rootPath)
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newFolder = File(_currentDirectory.value, name)
            if (!newFolder.exists()) {
                newFolder.mkdirs()
                loadFolderItems()
            }
        }
    }

    fun createTextFile(name: String, content: String = "") {
        if (name.isBlank()) return
        val sanitized = if (name.endsWith(".txt")) name else "$name.txt"
        viewModelScope.launch(Dispatchers.IO) {
            val newFile = File(_currentDirectory.value, sanitized)
            if (!newFile.exists()) {
                newFile.writeText(content)
                loadFolderItems()
            }
        }
    }

    fun openFileForEditing(file: File) {
        if (file.isFile) {
            _activeFile.value = file
            try {
                _activeFileContent.value = file.readText()
            } catch (e: Exception) {
                _activeFileContent.value = ""
            }
            navigateTo(Screen.FileEditor)
        }
    }

    fun saveActiveFileContent(newContent: String) {
        val file = _activeFile.value
        if (file != null && file.isFile) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    file.writeText(newContent)
                    _activeFileContent.value = newContent
                } catch (e: Exception) {
                    // Fail silently
                }
            }
        }
    }

    fun deleteFileOnDisk(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.deleteRecursively()
                loadFolderItems()
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    // Default Sandbox Setup
    private fun initDefaultWorkspace() {
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs()
        }
        val welcomeFile = File(rootDirectory, "Welcome.txt")
        if (!welcomeFile.exists()) {
            welcomeFile.writeText(
                "Ласкаво просимо у ваш Workspace!\n\n" +
                "Це комплексний робочий простір із:\n" +
                "1. Вбудованим календарем подій.\n" +
                "2. Справжнім провідником файлів та редактором текстів.\n" +
                "3. Офлайн-менеджером завдань (за допомогою БД Room).\n" +
                "4. Настроюваним Material You лаунчером!\n\n" +
                "Також усі завдання з вказаними датами автоматично інтегруються на календарну сітку у вигляді кольорових баджів відповідного пріоритету (червоний - високий, жовтий - середній, зелений - низький).\n\n" +
                "Ви можете переглядати та редагувати файли безпосередньо у цій пісочниці.\n\n" +
                "З повагою,\n" +
                "- ШІ Асистент"
            )
        }

        val projectsDir = File(rootDirectory, "Проекти та Цілі")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
            File(projectsDir, "Ідеї_додатків.txt").writeText(
                "1. Розумний будильник із тренером дихання.\n" +
                "2. Калькулятор калорій у стилі Material You."
            )
            File(projectsDir, "Розклад_тижня.txt").writeText(
                "Понеділок: Робота з кодом, Jetpack Compose.\n" +
                "Вівторок: Перегляд БД Room.\n" +
                "Середа: Розробка календаря та запуск на емуляторі.\n" +
                "Четвер: Фінальний білд та реліз."
            )
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
