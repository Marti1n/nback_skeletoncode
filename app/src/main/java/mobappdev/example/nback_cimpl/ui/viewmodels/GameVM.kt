package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.Locale

/**
 * GameViewModel – Single-n-back
 */

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application
) : GameViewModel, ViewModel(), TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore


    override val nBack: Int = 2
    private val eventInterval: Long = 2000L

    private var job: Job? = null
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private val letterMap = ('A'..'I').toList()

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()

        events = nBackHelper.generateNBackString(10, 9, 30, nBack)
            .map { raw ->
                val mod = raw % 9
                if (mod >= 0) mod else mod + 9
            }
            .toTypedArray()

        _gameState.value = GameState(
            gameType = _gameState.value.gameType,
            eventValue = -1,
            index = -1,
            totalEvents = events.size,
            correct = 0,
            canRegisterForThisEvent = false,
            feedback = null,
            running = true
        )
        _score.value = 0

        job = viewModelScope.launch {
            if (_gameState.value.gameType == GameType.Audio && !ttsReady) {
                repeat(5) {
                    if (ttsReady) return@repeat
                    delay(200)
                }
            }

            when (_gameState.value.gameType) {
                GameType.Visual -> runVisualGame(events)
                GameType.Audio -> runAudioGame()
            }

            val finalCorrect = _gameState.value.correct
            if (finalCorrect > _highscore.value) {
                userPreferencesRepository.saveHighScore(finalCorrect)
            }
            _gameState.value = _gameState.value.copy(running = false)
        }
    }

    override fun checkMatch() {
        val s = _gameState.value
        if (!s.canRegisterForThisEvent || s.index !in events.indices) return

        val hit = s.index >= nBack && events[s.index] == events[s.index - nBack]
        val newCorrect = if (hit) s.correct + 1 else s.correct

        _gameState.value = s.copy(
            correct = newCorrect,
            canRegisterForThisEvent = false,
            feedback = if (hit) Feedback.SUCCESS else Feedback.ERROR
        )
        _score.value = newCorrect
    }

    // --- VISUAL ---
    private suspend fun runVisualGame(events: Array<Int>) {
        for (i in events.indices) {
            _gameState.value = _gameState.value.copy(
                index = i,
                eventValue = events[i],
                canRegisterForThisEvent = true,
                feedback = null
            )

            delay(eventInterval)

            _gameState.value = _gameState.value.copy(
                canRegisterForThisEvent = false,
                feedback = null
            )
        }
    }

    // --- AUDIO ---
    private suspend fun runAudioGame() {
        for (i in events.indices) {
            val value = events[i]

            _gameState.value = _gameState.value.copy(
                index = i,
                eventValue = value,
                canRegisterForThisEvent = true,
                feedback = null
            )

            if (ttsReady && value in 0..letterMap.lastIndex) {
                val letter = letterMap[value].toString()
                tts?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, "nback_audio_$i")
            }

            delay(eventInterval)

            _gameState.value = _gameState.value.copy(
                canRegisterForThisEvent = false,
                feedback = null
            )
        }
    }

    // --- TextToSpeech ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            @Suppress("DEPRECATION")
            tts?.language = Locale.US // or Locale("sv", "SE")
            ttsReady = true
        } else {
            ttsReady = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        tts?.stop()
        tts?.shutdown()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(
                    application.userPreferencesRespository,
                    application
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { _highscore.value = it }
        }
        tts = TextToSpeech(application, this)
    }
}

enum class GameType { Audio, Visual }
enum class Feedback { SUCCESS, ERROR }

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1,
    val index: Int = -1,
    val totalEvents: Int = 0,
    val correct: Int = 0,
    val canRegisterForThisEvent: Boolean = false,
    val feedback: Feedback? = null,
    val running: Boolean = false
)

class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState> =
        MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int> =
        MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int> =
        MutableStateFlow(42).asStateFlow()
    override val nBack: Int = 2

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkMatch() {}
}
