package org.audhd.aha.presentation.omnibar

/**
 * Lightweight recursive descent arithmetic expression evaluator for inline math in Omnibar.
 * Supports +, -, *, /, %, ^, parentheses, floats, and negative numbers without external dependencies.
 */
object SimpleMathEvaluator {

    fun evaluate(expression: String): Double? {
        val clean = expression.replace(" ", "")
        if (clean.isEmpty()) return null

        return try {
            val parser = Parser(clean)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private class Parser(private val input: String) {
        private var pos = -1
        private var ch = '\u0000'

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw RuntimeException("Unexpected: $ch")
            return x
        }

        // Expression = Term (+ or - Term)*
        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        // Term = Factor (* or / or % Factor)*
        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        // Factor = (+ or - Factor) | (Expression) | Number | Factor ^ Factor
        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw RuntimeException("Missing ')'")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: $ch")
            }

            if (eat('^')) {
                x = Math.pow(x, parseFactor())
            }

            return x
        }
    }
}
