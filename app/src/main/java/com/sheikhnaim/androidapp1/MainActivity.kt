package com.sheikhnaim.androidapp1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.content.Context
import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * WHACK-A-MOLE! game
 * Tap the mole as many times as you can in 20 seconds
 */
class MainActivity : AppCompatActivity() {

    // ============================================
    // UI stuff - these are all the things on screen
    // ============================================

    private lateinit var timerText: TextView          // shows the 20 second countdown
    private lateinit var scoreText: TextView          // shows how many times you whacked
    private lateinit var topScoresText: TextView      // shows top 5 scores
    private lateinit var moleButton: ImageButton      // the mole you tap
    private lateinit var resetButton: Button          // resets the game
    private lateinit var resetScoresButton: Button    // clears all scores
    private lateinit var whackText: TextView          // "WHACK!" text on the mole

    // ============================================
    // Game stuff - keeps track of what's happening
    // ============================================

    private var whackCount = 0           // how many times you whacked
    private var gameRunning = false      // is the game playing?
    private var countDownTimer: CountDownTimer? = null  // the 20 second timer
    private val topScores = mutableListOf<Int>()        // stores top 5 scores

    // ============================================
    // Sound stuff - plays when you hit the mole
    // ============================================

    private var whackSound: MediaPlayer? = null      // sound when you whack
    private var gameOverSound: MediaPlayer? = null   // sound when time runs out

    // ============================================
    // Constants - these never change
    // ============================================

    companion object {
        private const val GAME_TIME = 20_000L        // 20 seconds
        private const val TIMER_INTERVAL = 1_000L    // update every second
        private const val PREFS_NAME = "WhackAMole2026"      // saved data name
        private const val SCORES_KEY = "TopScores"           // scores key
    }

    // ============================================
    // onCreate - this runs when the app starts up
    // ============================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Makes the app look nice with system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find everything on the screen
        timerText = findViewById(R.id.timerText)
        scoreText = findViewById(R.id.scoreText)
        topScoresText = findViewById(R.id.topScoresText)
        moleButton = findViewById(R.id.moleButton)
        resetButton = findViewById(R.id.resetButton)
        resetScoresButton = findViewById(R.id.resetScoresButton)
        whackText = findViewById(R.id.whackText)

        // Load the sounds
        whackSound = MediaPlayer.create(this, R.raw.hit_sound)
        gameOverSound = MediaPlayer.create(this, R.raw.hide_sound)

        // Load saved scores from previous games
        loadTopScores()

        // Tell the buttons what to do when clicked
        moleButton.setOnClickListener {
            whackMole()
        }

        resetButton.setOnClickListener {
            resetGame()
        }

        resetScoresButton.setOnClickListener {
            resetScores()
        }

        // Show empty score list to start
        updateTopScoresDisplay()
    }

    // ============================================
    // Game functions
    // ============================================

    // Starts the 20 second timer
    private fun startGame() {
        gameRunning = true
        moleButton.isEnabled = true

        countDownTimer = object : CountDownTimer(GAME_TIME, TIMER_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished + 999) / 1000
                timerText.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                timerText.text = "0"
                gameRunning = false
                moleButton.isEnabled = false

                playGameOverSound()
                addScore(whackCount)

                Toast.makeText(
                    this@MainActivity,
                    "Game Over! Score: $whackCount",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    // Called every time you tap the mole
    private fun whackMole() {
        // Don't count taps after game ends
        if (!gameRunning && whackCount > 0) {
            return
        }

        // First tap starts the game
        if (!gameRunning) {
            startGame()
        }

        // Count this whack
        whackCount++
        scoreText.text = whackCount.toString()

        // Play sound
        playWhackSound()

        // Move the mole to a random spot
        moveMole()
    }

    // ============================================
    // Move the mole - same as professor's code
    // ============================================

    private fun moveMole() {
        moleButton.post {
            val parent = moleButton.parent as ViewGroup
            val left = min(moleButton.left, whackText.left)
            val top = min(moleButton.top, whackText.top)
            val right = max(moleButton.right, whackText.right)
            val bottom = max(moleButton.bottom, whackText.bottom)

            val minimumX = -left.toFloat()
            val maximumX = (parent.width - right).toFloat()
            val minimumY = -top.toFloat()
            val maximumY = (parent.height - bottom).toFloat()

            val randomX = if (maximumX >= minimumX) {
                Random.nextFloat() * (maximumX - minimumX) + minimumX
            } else {
                0f
            }
            val randomY = if (maximumY >= minimumY) {
                Random.nextFloat() * (maximumY - minimumY) + minimumY
            } else {
                0f
            }

            moleButton.translationX = randomX
            moleButton.translationY = randomY
            whackText.translationX = randomX
            whackText.translationY = randomY
        }
    }

    // ============================================
    // Sound effects
    // ============================================

    private fun playWhackSound() {
        whackSound?.let { player ->
            if (player.isPlaying) {
                player.seekTo(0)
            }
            player.start()
        }
    }

    private fun playGameOverSound() {
        gameOverSound?.let { player ->
            if (player.isPlaying) {
                player.seekTo(0)
            }
            player.start()
        }
    }

    // ============================================
    // Score stuff
    // ============================================

    // Adds score to leaderboard
    private fun addScore(score: Int) {
        topScores.add(score)
        topScores.sortDescending()
        while (topScores.size > 5) {
            topScores.removeAt(topScores.lastIndex)
        }
        saveTopScores()
        updateTopScoresDisplay()
    }

    // Saves scores to phone
    private fun saveTopScores() {
        val scoresString = topScores.joinToString(",")
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(SCORES_KEY, scoresString).apply()
    }

    // Loads scores from phone
    private fun loadTopScores() {
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scoresString = preferences.getString(SCORES_KEY, "")
        if (scoresString.isNullOrEmpty()) { return }
        val savedScores = scoresString.split(",").mapNotNull { it.toIntOrNull() }
        topScores.clear()
        topScores.addAll(savedScores)
        topScores.sortDescending()
        while (topScores.size > 5) {
            topScores.removeAt(topScores.lastIndex)
        }
    }

    // Shows top 5 on screen
    private fun updateTopScoresDisplay() {
        if (topScores.isEmpty()) {
            topScoresText.text = "1. ---\n2. ---\n3. ---\n4. ---\n5. ---"
            return
        }
        val displayText = StringBuilder()
        for (i in 0 until 5) {
            displayText.append("${i + 1}. ")
            displayText.append(if (i < topScores.size) topScores[i] else "---")
            displayText.append("\n")
        }
        topScoresText.text = displayText.toString().trimEnd()
    }

    // ============================================
    // Reset stuff
    // ============================================

    // Resets current game
    private fun resetGame() {
        countDownTimer?.cancel()
        whackCount = 0
        gameRunning = false
        timerText.text = "20"
        scoreText.text = "0"
        moleButton.translationX = 0f
        moleButton.translationY = 0f
        whackText.translationX = 0f
        whackText.translationY = 0f
        moleButton.isEnabled = true
        Toast.makeText(this, "Game Reset!", Toast.LENGTH_SHORT).show()
    }

    // Clears all scores
    private fun resetScores() {
        topScores.clear()
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().remove(SCORES_KEY).apply()
        updateTopScoresDisplay()
        Toast.makeText(this, "Scores Cleared!", Toast.LENGTH_SHORT).show()
    }

    // ============================================
    // Cleanup - runs when app closes
    // ============================================

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        whackSound?.release()
        whackSound = null
        gameOverSound?.release()
        gameOverSound = null
    }
}