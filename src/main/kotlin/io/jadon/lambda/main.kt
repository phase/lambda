package io.jadon.lambda

fun main(args: Array<String>) {
    println("Untyped Lambda Calculus Interpreter")
    val env = Environment()
    while (true) {
        print(">> ")
        val line = readLine() ?: ""
        if (line == "q") break
        val tokens = tokenize(line)
        tokens.forEach {
            println("${it.type}=${it.value}")
        }
        val result = parse(tokens, env)
        when (result) {
            is Either.Left<Triple<Expression, Environment, ResultType>> -> {
                val r = result.value
                println("${r.third}: ${r.first}")
                println(r.second)
            }
            is Either.Right<Error> -> {
                println(result.value)
            }
        }
    }
}