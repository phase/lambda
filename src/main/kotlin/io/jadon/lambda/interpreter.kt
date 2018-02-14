package io.jadon.lambda

interface Expression

data class Application(val function: Expression, val argument: Expression) : Expression

data class Abstraction(val argument: String, val value: Expression) : Expression

data class Variable(val argument: String): Expression {
    override fun toString(): String = argument
}

class Environment {
    val variables = mutableMapOf<Variable, Expression>()
    override fun toString(): String = variables.toString()
}