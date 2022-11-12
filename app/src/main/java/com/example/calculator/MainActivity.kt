package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var input: StringBuilder = StringBuilder()
    private var canAddOperation = true
    private var canAddDecimal = true
    private var isFirstDecimalInNum = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun clearAllAction(view: View) {
        input.clear()
        restart()
        update()
        binding.tvResult.text = ""
        binding.btnDecimal.isClickable = false
    }

    private fun restart() {
        canAddDecimal = true
        isFirstDecimalInNum = true
        binding.btnDecimal.isClickable = true
    }

    fun operationAction(view: View) {
        if (view is MaterialButton && input.isNotBlank()) {
            if (canAddOperation) {
                input.append(view.text)
                update()
                canAddOperation = false
                restart()
                canAddDecimal = false
            }
        }
    }

    fun numberAction(view: View) {
        if (view is MaterialButton) {
            val text = view.text.toString()

            if ((text.toIntOrNull()) is Int) {
                input.append(text)
                update()
                canAddOperation = true
                binding.btnDecimal.isClickable = true
                canAddDecimal = true
            } else if (text == ".") {
                if (canAddDecimal && isFirstDecimalInNum) {
                    input.append(text)
                    update()
                    canAddDecimal = false
                    canAddOperation = false
                    isFirstDecimalInNum = false
                }
            }
        }
    }

    private fun update() {
        binding.tvInput.text = input
    }

    fun onClearOneAction(view: View) {
        if (view is MaterialButton) {
            if (input.isBlank()) return
            else {
                input = StringBuilder(input.subSequence(0, input.lastIndex))
                if (!input.last().isDigit()) canAddOperation = false
            }
            update()
        }
    }

    fun resultAction(view: View) {
        if (!input.last().isDigit()) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tvResult.text = calculate()
    }

    private fun calculate(): String {
        var partsResult = expr(LexemeBuffer(lexAnalyze(binding.tvInput.text.toString())!!))
        return partsResult.toString()
    }

    enum class LexemeType {
        LEFT_BRACKET, RIGHT_BRACKET, OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, NUMBER, EOF
    }

    class Lexeme {
        var type: LexemeType
        var value: String

        constructor(type: LexemeType, value: String) {
            this.type = type
            this.value = value
        }

        constructor(type: LexemeType, value: Char) {
            this.type = type
            this.value = value.toString()
        }
    }

    class LexemeBuffer(var lexemes: List<Lexeme>) {
        var pos = 0
            private set

        operator fun next(): Lexeme {
            return lexemes[pos++]
        }

        fun back() {
            pos--
        }
    }

    private fun lexAnalyze(expText: String): List<Lexeme>? {
        val lexemes: ArrayList<Lexeme> = ArrayList()
        var pos = 0
        while (pos < expText.length) {
            var c = expText[pos]
            when (c) {
                '(' -> {
                    lexemes.add(Lexeme(LexemeType.LEFT_BRACKET, c))
                    pos++
                    continue
                }
                ')' -> {
                    lexemes.add(Lexeme(LexemeType.RIGHT_BRACKET, c))
                    pos++
                    continue
                }
                '+' -> {
                    lexemes.add(Lexeme(LexemeType.OP_PLUS, c))
                    pos++
                    continue
                }
                '-' -> {
                    lexemes.add(Lexeme(LexemeType.OP_MINUS, c))
                    pos++
                    continue
                }
                '*' -> {
                    lexemes.add(Lexeme(LexemeType.OP_MUL, c))
                    pos++
                    continue
                }
                '/' -> {
                    lexemes.add(Lexeme(LexemeType.OP_DIV, c))
                    pos++
                    continue
                }
                else -> if (c in '0'..'9' || c == '.') {
                    val sb = java.lang.StringBuilder()
                    do {
                        sb.append(c)
                        pos++
                        if (pos >= expText.length) {
                            break
                        }
                        c = expText[pos]
                    } while (c in '0'..'9' || c == '.')
                    lexemes.add(Lexeme(LexemeType.NUMBER, sb.toString()))
                } else {
                    if (c != ' ') {
                        throw RuntimeException("Unexpected character: $c")
                    }
                    pos++
                }
            }
        }
        lexemes.add(Lexeme(LexemeType.EOF, ""))
        return lexemes
    }

    private fun expr(lexemes: LexemeBuffer): Float {
        val lexeme = lexemes.next()
        return if (lexeme.type == LexemeType.EOF) {
            0f
        } else {
            lexemes.back()
            plusminus(lexemes)
        }
    }

    private fun plusminus(lexemes: LexemeBuffer): Float {
        var value = multdiv(lexemes)
        while (true) {
            val lexeme = lexemes.next()
            when (lexeme.type) {
                LexemeType.OP_PLUS -> value += multdiv(lexemes)
                LexemeType.OP_MINUS -> value -= multdiv(lexemes)
                LexemeType.EOF, LexemeType.RIGHT_BRACKET -> {
                    lexemes.back()
                    return value
                }
                else -> throw RuntimeException(
                    "Unexpected token: " + lexeme.value
                            + " at position: " + lexemes.pos
                )
            }
        }
    }

    private fun multdiv(lexemes: LexemeBuffer): Float {
        var value = factor(lexemes)
        while (true) {
            val lexeme = lexemes.next()
            when (lexeme.type) {
                LexemeType.OP_MUL -> value *= factor(lexemes)
                LexemeType.OP_DIV -> value /= factor(lexemes)
                LexemeType.EOF, LexemeType.RIGHT_BRACKET, LexemeType.OP_PLUS, LexemeType.OP_MINUS -> {
                    lexemes.back()
                    return value
                }
                else -> throw RuntimeException(
                    ("Unexpected token: " + lexeme.value
                            + " at position: " + lexemes.pos)
                )
            }
        }
    }

    private fun factor(lexemes: LexemeBuffer): Float {
        var lexeme = lexemes.next()
        when (lexeme.type) {
            LexemeType.NUMBER -> return lexeme.value.toFloat()
            LexemeType.LEFT_BRACKET -> {
                val value = plusminus(lexemes)
                lexeme = lexemes.next()
                if (lexeme.type != LexemeType.RIGHT_BRACKET) {
                    throw RuntimeException(
                        ("Unexpected token: " + lexeme.value
                                + " at position: " + lexemes.pos)
                    )
                }
                return value
            }
            else -> throw RuntimeException(
                ("Unexpected token: " + lexeme.value
                        + " at position: " + lexemes.pos)
            )
        }
    }
}