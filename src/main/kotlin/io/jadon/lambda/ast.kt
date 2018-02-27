package io.jadon.lambda

import java.io.File

class Module(val name: String, val environment: Environment = Environment()) {

    override fun toString(): String = "Module $name: $environment"

    companion object {
        fun getModule(fileName: String): Either<Module, Error> {
            val env = Environment()
            val file = File(fileName)
            file.readLines().forEach {
                if (it.isNotEmpty()) {
                    val tokens = tokenize(it)
                    val result = parse(tokens, env)
                    when (result) {
                        is Either.Left -> {
                            val r = result.value
                            println("${r.third}: ${r.first}")
                            println(r.second)
                            if (r.third == ResultType.EXPRESSION) {
                                val expression = r.first
                                val renamed = rename(expression).first
                                val filled = fillInFreeVariables(renamed, r.second)
                                val reduced = betaReduction(filled.first)
                                println("${r.first} => $reduced")
                            }
                        }
                        is Either.Right -> {
                            return result
                        }
                    }
                }
            }
            return Either.Left(Module(file.nameWithoutExtension, env))
        }
    }
}

interface Expression

data class Application(val function: Expression, val argument: Expression) : Expression {
    override fun toString(): String = "($function $argument)"
}

data class Abstraction(val argument: String, val value: Expression) : Expression {
    override fun toString(): String = "\\$argument.$value"
}

data class IdentifierExpression(val argument: String) : Expression {
    override fun toString(): String = argument
}

data class Variable(val name: String, val type: Type) {
    override fun toString(): String = "$name : $type"
}

class Environment {
    val variables = mutableMapOf<Variable, Expression>()
    override fun toString(): String = variables.toString()
}

// types

interface Type

data class PrimitiveType(val name: String) : Type {
    override fun toString(): String = name
}

data class FunctionType(val a: Type, val b: Type) : Type {
    override fun toString(): String = "$a -> $b"
}

data class PolyType(val typeVariable: String, val enclosingType: Type) : Type {
    override fun toString(): String = "/$typeVariable.$enclosingType"
}
