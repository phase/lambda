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
        is Term -> {
            if (names.containsKey(expression.argument)) {
                Pair(Term(names[expression.argument]!!), names)
            } else {
                Pair(expression, names)
            }
        }
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
        is Term -> {
            Pair(if (env.variables.keys.map { it.name }.contains(expression.argument))
                fillInFreeVariables(rename(env.variables.filter { it.key.name == expression.argument }.entries.first().value).first, env).first
            else expression, env)
        }
    }
}

fun betaReduction(e: Expression): Expression {
    return when (e) {
        is Application -> {
            val f = betaReduction(e.function)
            val a = betaReduction(e.argument)
            if (f is Abstraction) {
                betaReduction(replace(f.value, f.argument, a))
            } else {
                e
            }
        }
        else -> e
    }
}

fun replace(origin: Expression, name: String, replacement: Expression): Expression {
    return when (origin) {
        is Term -> {
            if (origin.argument == name) {
                replacement
            } else {
                origin
            }
        }
        is Abstraction -> {
            val value = betaReduction(replace(origin.value, name, replacement))
            Abstraction(origin.argument, value)
        }
        is Application -> {
            val function = betaReduction(replace(origin.function, name, replacement))
            val argument = betaReduction(replace(origin.argument, name, replacement))
            Application(function, argument)
        }
    }
}
