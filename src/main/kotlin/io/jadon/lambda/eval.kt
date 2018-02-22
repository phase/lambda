package io.jadon.lambda

import java.util.concurrent.ThreadLocalRandom

fun rename(expression: Expression, names: MutableMap<String, String> = mutableMapOf()): Pair<Expression, MutableMap<String, String>> {
    return when (expression) {
        is Abstraction -> {
            val old = expression.argument
            // add a random number to the end
            val new = old + ThreadLocalRandom.current().nextInt(1024)
            names[old] = new
            val result = rename(expression.value, names)
            Pair(Abstraction(new, result.first), result.second)
        }
        is Application -> {
            val function = rename(expression.function, names)
            val argument = rename(expression.argument, function.second)
            Pair(Application(function.first, argument.first), argument.second)
        }
        is Variable -> {
            if (names.containsKey(expression.argument)) {
                Pair(Variable(names[expression.argument]!!), names)
            } else {
                Pair(expression, names)
            }
        }
        else -> Pair(expression, names)
    }
}

fun fillInFreeVariables(expression: Expression, env: Environment): Pair<Expression, Environment> {
    return when (expression) {
        is Abstraction -> {
            val result = fillInFreeVariables(expression.value, env)
            Pair(Abstraction(expression.argument, result.first), result.second)
        }
        is Application -> {
            val function = fillInFreeVariables(expression.function, env).first
            val argument = fillInFreeVariables(expression.argument, env).first
            val application = Application(function, argument)
            Pair(application, env)
        }
        is Variable -> {
            Pair(if (env.variables.containsKey(expression))
                fillInFreeVariables(rename(env.variables[expression]!!).first, env).first
            else expression, env)
        }
        else -> Pair(expression, env)
    }
}
