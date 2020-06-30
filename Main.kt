package calculator
import java.lang.NumberFormatException
import java.util.*
import java.math.BigInteger

fun isPlainNumber(num: String):Boolean {
    try {
        num.trim().toBigInteger()
    } catch (err: NumberFormatException) {
        return false
    }

    return true
}

fun isNegateOperator(op: String): Boolean {
    val minuscount = op.count { it == '-' }
    return minuscount % 2 != 0
}

fun getValue(num:String, variables:MutableMap<String, BigInteger>): BigInteger? {
    try {
        return num.toBigInteger()
    } catch (e:NumberFormatException) {
        if (num in variables) {
            return variables[num]
        }
    }
    throw Error("Unknown variable")
}

fun operatorPriority(element: String?):Int {
    return when (element) {
        "*" -> 4
        "/" -> 4
        "+" -> 3
        "-" -> 3
        else -> -1
    }
}

fun calcPostfix(expr: MutableList<String>):BigInteger {
    //println(expr)
    var stack = ArrayDeque<BigInteger>()
    for (ele in expr) {
        if (isPlainNumber(ele))
            stack.push(ele.toBigInteger())
        else if (operatorPriority(ele) > 0) {
            val a = stack.pop()
            val b = stack.pop()
            val result = when (ele) {
                "*" -> a * b
                "/" -> b / a
                "+" -> a + b
                "-" -> b - a
                else -> throw Error("Invalid expression")
            }
            stack.push(result)
        }
    }
    return stack.pop()
}

fun toPostfix(line: String, variables:MutableMap<String, BigInteger>): MutableList<String> {
    val result = mutableListOf<String>()
    var stack = ArrayDeque<String>()
    for (ele in lineSplit(line.trim())) {
        //println(ele + stack)
        // 1.
        if (isPlainNumber(ele)) {
            result.add(ele)
            continue
        }
        if (isValidVariableName(ele)) {
            if (ele in variables) {
                //stack.push(variables[ele].toString())
                result.add(variables[ele].toString())
            } else {
                throw Error("Unknown variable")
            }
            continue
        }
        // 5.
        if (ele == "(") {
            stack.push(ele)
            continue
        }
        // 6.
        if (ele == ")") {
            var operator = ""
            while (!stack.isEmpty()) {
                operator = stack.pop()
                if (operator == "(")
                    break
                result.add(operator)
            }
            if (operator == "(")
                continue
            else
                throw Error("Invalid expression")
        }
        // 2.
        val opPriority = operatorPriority(ele)
        val stackOpPriority = operatorPriority(stack.peek())
        if (opPriority > 0 && (
                        stack.peek() == "(" || stack.isEmpty()
                        )) {
            stack.push(ele)
            continue
        }
        // 3.
        if (opPriority > stackOpPriority) {
            stack.push(ele)
            continue
        }
        // 4.
        if (opPriority > 0 && stackOpPriority > 0 &&
                opPriority <= stackOpPriority) {
            while (!stack.isEmpty()) {
                if (stack.peek() == "(" ||
                        (operatorPriority(stack.peek()) > 0 &&
                        operatorPriority(stack.peek()) < opPriority))
                    break
                result.add(stack.pop())
            }
            stack.push(ele)
            continue
        }
    }
    // 7.
    while (!stack.isEmpty()) {
        val operator = stack.pop()
        if (operator == "(" || operator == ")")
            throw Error("Invalid expression")
        result.add(operator)
    }
    return result
}

fun parseLine(line: String, variables:MutableMap<String, BigInteger>) {
    val elements = lineSplit(line.trim())
    var negater = 1
    var i = 0
    while (i < elements.size) {
        var ele = elements[i]
        if (isValidVariableName(ele)) {
            if (i+1 < elements.size && isPlainNumber(elements[i+1])) {
                throw Error("Invalid identifier")
            }
            if (i+1 < elements.size && elements[i+1] == "=") {
                if (!isValidVariableName(ele)) {
                    throw Error("Invalid identifier")
                }
                var value:BigInteger? = null
                if (isNegateOperator(elements[i+2])) {
                    value = getValue(elements[i+3], variables)
                    negater = -1
                    i += 3
                } else {
                    value = getValue(elements[i+2], variables)
                    negater = 1
                    i += 2
                }

                if (value != null) {
                    variables[ele] = value * negater.toBigInteger()
                }
                return
            }
        }
        i++
    }
}

fun operatorTrim(operator:String):String {
    val op = operator.trim()
    val minuscount = op.count { it -> it == '-'}
    val pluscount = op.count { it -> it == '+'}
    val mulcount = op.count { it -> it == '*'}
    val divcount = op.count { it -> it == '/'}
    if (mulcount > 1 || divcount > 1)
        throw Error("Invalid expression")
    if (minuscount > 0) {
        return if (minuscount % 2 != 0) "-" else "+"
    } else if (pluscount > 0) {
        return "+"
    }
    return op
}

fun lineSplit(line:String):List<String> {
    var elements:MutableList<String> = arrayListOf()
    var accu = ""
    var mode = 99
    for (i in line) {
        val newMode = when(i) {
            in '0'..'9' -> 0
            in 'a'..'Z' -> 1
            '+' -> 2
            '-' -> 2
            ' ' -> 3
            '=' -> 8
            '(' -> -4
            ')' -> -5
            else -> 1
        }
        if (mode == 99) {
            mode = newMode
            accu += i
            continue
        }
        if (newMode != mode || newMode < 0) {
            if (accu.trim() != "") {
                elements.add(operatorTrim(accu))
            }
            accu = i.toString()
            mode = newMode
        } else {
            accu += i
        }
    }
    if (accu.length > 0)
        elements.add(accu.trim())

    return elements.toList()
}

fun isValidVariableName(name:String):Boolean {
    return name.trim().all { it -> it.isLetter()}
}

fun isValidAssignment(line:String):Boolean {
    if ("=" !in line) return true
    val f = line.split("=")
    if (f.size != 2) return false
    if ((!isValidVariableName(f[1]) && !isPlainNumber(f[1]))) return false

    return true
}

fun help() {
    println("Calculates stuff")
}

fun main() {
    val scanner = Scanner(System.`in`)
    var variables = mutableMapOf<String, BigInteger>()
    loop@ while (true) {
        val line = scanner.nextLine().trim()

        if (line.isNullOrBlank() || line.isEmpty())
            continue

        if (!isValidAssignment(line)) {
            println("Invalid assignment")
            continue
        }

        if (line.startsWith("/")) {
            when (line) {
                "/exit" -> break@loop
                "/help" -> help()
                else -> println("Unknown command")
            }
        }

        if (!line.startsWith("/")) {
            try {
                if ("=" in line) {
                    parseLine(line, variables)
                } else {
                    val pf = toPostfix(line, variables)
                    println(calcPostfix(pf))
                }
            } catch (e: Error) {
                println(e.toString().split(":")[1].trim())
            }

        }
    }
    println("Bye!")
}
