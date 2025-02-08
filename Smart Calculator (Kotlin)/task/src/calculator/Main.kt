package calculator

import java.math.BigInteger
import java.util.Stack
import kotlin.math.pow
import kotlin.system.exitProcess

fun main() {

    val variablesMap = mutableMapOf<String, BigInteger>()

    while (true) {
        try {
            val line = readlnOrNull()
            if (!line.isNullOrBlank() && line.startsWith("/")) {
                command(line)
            } else {
                val expr0 = reduceOperator(line.toString().trim())
                val expr = prepareExpression(expr0).joinToString(" ")

                val myList = expr.split(" ")
                if (line != null) {
                    if (myList[0].isNotBlank()) {

                        if ((myList.size == 1) && !myList[0].contains("=")) {
                            oneParameter(expr, variablesMap)
                        } else if (myList.size == 2 && myList[0] == "-") {
                            println(expr)
                        } else if (expr.contains("=")) {
                            storeVariable(line, variablesMap)
                        } else {
                            val postfixExpression = infixToPostfix(expr)
                            val expression = postfixExpression.toList()
                            val res = calculatePostFixExpression(expression, map = variablesMap)
                            println(res)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}


fun calculatePostFixExpression(list: List<String>, map: MutableMap<String, BigInteger>): BigInteger {

    val st = Stack<String>()
    for (s in list) {
        if (!isOperator(s)) {
            st.push(s)
        } else if (isOperator(s) && st.size > 1) {
            val rop = st.pop()
            val lop = st.pop()

            val res = evalOperation(s, evalVariable(lop, map), evalVariable(rop, map))
            st.push(res.toString())
        }
    }
    return evalVariable(st.pop(), map)
}


private fun priorityOperator(operator: String): Int {
    return when (operator) {
        "+" -> 1
        "-" -> 1
        "*" -> 2
        "/" -> 2
        "^" -> 3
        else -> 0
    }
}

private fun isOperator(operator: String): Boolean {
    return (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "^")
}

fun prepareExpression(str: String): List<String> {
    val regex = "(?<=op)|(?=op)".replace("op", "[-+*/()^]")
    return str
        .trim()
        .split(regex.toRegex())
        .filter { it.trim().isNotEmpty() }
        .map { it.trim() }
        .toMutableList()
}

private fun infixToPostfix(line: String): Stack<String> {
    val st = Stack<String>()
    val linePostfix = Stack<String>()
    val myList = prepareExpression(line)

    for (element in myList) {
        when (element) {
            "(" -> st.push(element)
            ")" -> {
                do {
                    if (isOperator(st.peek())) {
                        linePostfix.push(st.pop())
                    }
                } while (st.isNotEmpty() && st.peek() != "(")
                if (st.isNotEmpty() && st.peek() == "(") {
                    st.pop()
                } else {
                    throw Exception("Invalid expression")
                }
            }

            "+", "-", "*", "/", "^" -> {
                if (st.empty() || st.peek() == "(") {
                    st.push(element)
                } else if (priorityOperator(element) > priorityOperator(st.peek())) {
                    st.push(element)
                } else if ((priorityOperator(element) <= priorityOperator(st.peek()))) {
                    do {
                        if (st.peek() != "(" && st.peek() != ")") {
                            linePostfix.push(st.pop())
                        } else {
                            st.pop()
                        }
                    } while (st.isNotEmpty() && (st.peek() != "(" && priorityOperator(element) > priorityOperator(st.peek())))
                    st.push(element)
                }
            }

            else -> {
                linePostfix.push(element)
            }
        }
    }

    while (st.isNotEmpty()) {
        if (st.peek() == "(") {
            st.pop()
            throw Exception("Invalid expression")
        } else {
            linePostfix.push(st.pop())
        }
    }

    /*for (element in linePostfix) {
        print("$element ")
    }
    println()
*/
    return linePostfix
}


fun storeVariable(numbers: String, map: MutableMap<String, BigInteger>) {
    val regexOpUnaryNum = Regex("""[-|+]?[0-9]+""")
    val regexOpUnaryStr = Regex("""[+|-]?[a-zA-Z]+""")

    val numberList = numbers.split("=")
    val lOperand = numberList[0].trim()
    val rOperand = numberList[1].trim()

    if (regexOpUnaryStr.matches(lOperand) && regexOpUnaryNum.matches(rOperand)) {
        map[lOperand] = rOperand.toBigInteger()
    } else if (regexOpUnaryStr.matches(lOperand) && regexOpUnaryStr.matches(rOperand)) {
        if (map[rOperand] != null) {
            val result = map[rOperand]
            map[lOperand] = result!!.toString().toBigInteger()
        } else {
            throw Exception("Unknown variable")
        }
    } else {
        throw Exception("Invalid expression")
    }

}


fun evalVariable(ro: Any, map: MutableMap<String, BigInteger>): BigInteger {
    val regex = Regex("""-?[0-9]+""")
    if (regex.matches(ro.toString())) {
        return ro.toString().toBigInteger()
    } else {
        if (map.isNotEmpty() && map.keys.contains(ro)) {
            return map[ro]?.toString()?.toBigInteger() ?:BigInteger.ZERO // map[ro]?.toBInt() ?: 0
        } else {
            return BigInteger.ZERO
        }
    }
}


private fun evalOperation(operator: String, lo: BigInteger, ro: BigInteger): BigInteger {
    return when (operator) {
        "+" -> lo.plus(ro)
        "-" -> lo.minus(ro)
        "*" -> lo.multiply(ro)
        "/" -> lo.divide(ro)
        //"^" -> lo.toDouble().pow(ro.toDouble()).toInt()
        else -> BigInteger.ZERO
    }
}


fun reduceOperator(st: String): String {
    val regex1 = Regex("""\+\+""")
    val regex2 = Regex("""\+-""")
    val regex3 = Regex("""-\+""")
    val regex4 = Regex("""--""")
    val regex5 = Regex("""(. [*]{2,} .)|(. [* ]{2,} .)""")
    val regex6 = Regex("""(. /{2,} .)|(. [/ ]{2,} .)""")

    if (regex5.matches(st) || regex6.matches(st)) {
        throw Exception("Invalid expression")
    }

    var s = st
    while (regex1.containsMatchIn(s) || regex2.containsMatchIn(s) || regex3.containsMatchIn(s)
        || regex4.containsMatchIn(s)
    ) {
        s = regex1.replace(s, "+")
        s = regex2.replace(s, "-")
        s = regex3.replace(s, "-")
        s = regex4.replace(s, "+")
    }
    return s

}

fun oneParameter(line: String, map: MutableMap<String, BigInteger>) {
    val regexOpUnaryNum = Regex("""[+|-]?[0-9]+""")
    val regexOpUnaryStr = Regex("""[+|-]?[a-zA-Z]+""")

    if (regexOpUnaryNum.matches(line)) {
        println(line)
    } else if (regexOpUnaryStr.matches(line)) {
        if (map[line] != null) {
            println(map[line])
        } else {
            throw Exception("Unknown variable")
        }
    } else {
        throw Exception("Invalid identifier")
    }
}

private fun command(a: String) {
    when (a) {
        "/exit" -> {
            println("Bye!")
            exitProcess(0)
        }

        "/help" -> {
            println(
                """
                The program calculates the sum, subtraction, multiplication, division of numbers, as well as the power of 2 numbers.
            """.trimIndent()
            )
        }

        else -> throw Exception("Unknown command")
    }
}
