package io.jadon.lambda

fun main(args: Array<String>) {
    println("Polymorphic Lambda Calculus Interpreter")
    val env = Environment()
    while (true) {
        print(">> ")
        val line = readLine() ?: ""
        if (line == "q") break
        else if (line.startsWith("import ")) {
            val fileName = line.split("import ")[1] + ".txt"
            val importedModule = Module.getModule(fileName)
            when (importedModule) {
                is Either.Left -> {
                    val module = importedModule.value
                    env.variables.putAll(module.environment.variables)
                }
                is Either.Right -> {
                    println("Error: ${importedModule.value}")
                }
            }
            continue
        }
        val tokens = tokenize(line)
        tokens.forEach {
            println("${it.type} = ${it.value}")
        }
        val result = parse(tokens, env)
        when (result) {
            is Either.Left<Triple<Expression, Environment, ResultType>> -> {
                val r = result.value
                println("${r.third}: ${r.first}")
                println(r.second)
                if (r.third == ResultType.EXPRESSION) {
                    val expression = r.first
                    val renamed = rename(expression).first
                    println("Renamed: $renamed")
                    val filled = fillInFreeVariables(renamed, r.second)
                    println("Filled: ${filled.first}")
                    val reduced = betaReduction(filled.first)
                    println("Reduced: $reduced")

                    // TODO not this
                    charIndex = 0
                    charWraps = 0
                    val type = infer(reduced, env)
                    println("Type: $type")
                }
            }
            is Either.Right<Error> -> {
                println(result.value)
            }
        }
    }
}
