package com.example.sudoku

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Random

class MainActivity : AppCompatActivity() {

    private val SIZE = 9
    private val board = Array(SIZE) { IntArray(SIZE) }
    private var selectedCell: Pair<Int, Int>? = null
    private lateinit var gridLayout: GridLayout
    private var errorCount = 0
    private var firstSelectionMade = false

    // Variables para el cronómetro
    private var timeInSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerTextView: TextView
    private lateinit var errorCounterTextView: TextView // TextView para mostrar los errores
    private val cellLockStatus = Array(SIZE) { BooleanArray(SIZE) }
    private val lastValues = mutableListOf<Pair<Int, Int>>() // Para deshacer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.gridLayout)
        errorCounterTextView = findViewById(R.id.error_counter)
        timerTextView = findViewById(R.id.timer)

        initializeBoard()
        createBoard(gridLayout)

        // Configurar botones de números
        for (num in 1..9) {
            val buttonId = resources.getIdentifier("button_$num", "id", packageName)
            val button = findViewById<Button>(buttonId)
            button.setOnClickListener {
                selectedCell?.let { cell ->
                    onNumberSelected(num, cell.first, cell.second)
                }
            }
        }

        // Botón Deshacer
        val undoButton = findViewById<Button>(R.id.button_undo)
        undoButton.setOnClickListener { undoAction() }

        // Botón Borrar
        val deleteButton = findViewById<Button>(R.id.button_delete)
        deleteButton.setOnClickListener { deleteAction() }
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                timeInSeconds++
                val minutes = timeInSeconds / 60
                val seconds = timeInSeconds % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun initializeBoard() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                board[i][j] = 0
                cellLockStatus[i][j] = false
            }
        }

        val random = Random()
        val filledCells = mutableSetOf<Pair<Int, Int>>()

        while (filledCells.size < 20) {
            val row = random.nextInt(SIZE)
            val col = random.nextInt(SIZE)

            if (board[row][col] == 0) {
                val randomNum = random.nextInt(9) + 1
                if (isSafe(row, col, randomNum)) {
                    board[row][col] = randomNum
                    filledCells.add(Pair(row, col))
                    cellLockStatus[row][col] = true
                }
            }
        }

        createBoard(gridLayout)

        for (cell in filledCells) {
            val (row, col) = cell
            val button = gridLayout.getChildAt(row * SIZE + col) as Button
            button.isEnabled = false
            button.setTextColor(Color.BLUE)
        }
    }

    private fun createBoard(gridLayout: GridLayout) {
        gridLayout.removeAllViews()
        val buttonSize = (resources.displayMetrics.widthPixels / SIZE) - 8

        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val button = Button(this)
                button.text = if (board[i][j] == 0) "" else board[i][j].toString()
                val layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(i), GridLayout.spec(j)
                )
                layoutParams.width = buttonSize
                layoutParams.height = buttonSize

                button.background = ContextCompat.getDrawable(this, R.drawable.button_border)
                button.layoutParams = layoutParams

                button.setOnClickListener {
                    selectCell(i, j, button)
                }

                gridLayout.addView(button)
            }
        }
    }

    private fun selectCell(row: Int, col: Int, button: Button) {
        if (!firstSelectionMade) {
            firstSelectionMade = true
            startTimer() // Iniciar el cronómetro en la primera selección
        }

        resetButtonStyles()
        selectedCell = Pair(row, col)
        button.setBackgroundColor(Color.parseColor("#B0E0E6"))

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val cellButton: Button = gridLayout.getChildAt(r * SIZE + c) as Button
                if ((r == row || c == col) && !(r == row && c == col)) {
                    cellButton.background = ContextCompat.getDrawable(this, R.drawable.cell_translucent_background)
                }
            }
        }

        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3

        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                val cellButton: Button = gridLayout.getChildAt(r * SIZE + c) as Button
                if (r != row || c != col) {
                    cellButton.background = ContextCompat.getDrawable(this, R.drawable.cell_translucent_background)
                }
            }
        }
    }

    private fun resetButtonStyles() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val button = gridLayout.getChildAt(i * SIZE + j) as Button
                button.background = ContextCompat.getDrawable(this, R.drawable.button_border)
            }
        }
    }

    private fun onNumberSelected(num: Int, row: Int, col: Int) {
        if (cellLockStatus[row][col]) return

        lastValues.add(Pair(row, col)) // Guardar el último valor antes de cambiar
        board[row][col] = num
        val button = gridLayout.getChildAt(row * SIZE + col) as Button
        button.text = num.toString()

        val existsInRow = (0 until SIZE).any { board[row][it] == num && it != col }
        val existsInCol = (0 until SIZE).any { board[it][col] == num && it != row }
        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        val existsInQuadrant = (startRow until startRow + 3).any { r ->
            (startCol until startCol + 3).any { c ->
                board[r][c] == num && !(r == row && c == col)
            }
        }

        if (existsInRow || existsInCol || existsInQuadrant) {
            errorCount++
            errorCounterTextView.text = "Errores: $errorCount/3"
            button.setTextColor(Color.RED)

            if (errorCount >= 3) {
                showGameOverDialog() // Mostrar mensaje de Game Over
            }
        } else {
            button.setTextColor(Color.BLUE)
        }
    }

    private fun undoAction() {
        if (lastValues.isNotEmpty()) {
            val (row, col) = lastValues.removeAt(lastValues.size - 1)
            board[row][col] = 0
            val button = gridLayout.getChildAt(row * SIZE + col) as Button
            button.text = ""
            button.setTextColor(Color.BLACK)
            errorCount = (errorCount - 1).coerceAtLeast(0) // Decrementar errores si fue un error
            errorCounterTextView.text = "Errores: $errorCount/3"
        }
    }

    private fun deleteAction() {
        selectedCell?.let { cell ->
            val (row, col) = cell
            lastValues.add(Pair(row, col)) // Guardar el último valor antes de cambiar
            board[row][col] = 0
            val button = gridLayout.getChildAt(row * SIZE + col) as Button
            button.text = ""
            button.setTextColor(Color.BLACK)
        }
    }

    private fun isSafe(row: Int, col: Int, num: Int): Boolean {
        for (x in 0 until SIZE) {
            if (board[row][x] == num || board[x][col] == num) {
                return false
            }
        }

        val startRow = row - row % 3
        val startCol = col - col % 3
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                if (board[r + startRow][c + startCol] == num) {
                    return false
                }
            }
        }
        return true
    }

    private fun showGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("GAME OVER")
        builder.setMessage("Has alcanzado el límite de errores.")
        builder.setPositiveButton("TRY AGAIN") { _, _ -> restartGame() }
        builder.setNegativeButton("CANCEL", null)
        builder.show()
    }

    private fun restartGame() {
        errorCount = 0
        errorCounterTextView.text = "Errores: $errorCount/3"
        timeInSeconds = 0
        timerTextView.text = "00:00"
        firstSelectionMade = false
        lastValues.clear()
        initializeBoard()
        createBoard(gridLayout)
    }
}
