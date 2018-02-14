package io.jadon.lambda

fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var buffer = ""

    fun pushBuffer() {
        when (true) {
            buffer.isEmpty() -> {
            }
            buffer.matches("[a-zA-Z0-9]+".toRegex()) ->
                tokens.add(Token(buffer, Token.TokenType.IDENTIFIER))
            buffer == ":=" ->
                tokens.add(Token(buffer, Token.TokenType.ASSIGNMENT))
        }
        buffer = ""
    }

    input.forEachIndexed { i: Int, it: Char ->
        if (it.isWhitespace()) {
            //end of token
            pushBuffer()
        } else if (it.isLetterOrDigit() || it == ':' || it == '=') {
            buffer += it
        } else if (it == '.') {
            pushBuffer()
            tokens.add(Token(".", Token.TokenType.PERIOD))
        } else if (it == '\\') {
            pushBuffer()
            tokens.add(Token("\\", Token.TokenType.LAMBDA))
        } else if (it == '(') {
            pushBuffer()
            tokens.add(Token("(", Token.TokenType.OPEN_PAREN))
        } else if (it == ')') {
            pushBuffer()
            tokens.add(Token(")", Token.TokenType.CLOSE_PAREN))
        }

        if (it == '\r' || it == '\n' || it.isISOControl() || i == input.length - 1) {
            pushBuffer()
        }
    }

    return tokens
}

data class Token(val value: String, val type: TokenType) {
    override fun toString(): String {
        return when (type) {
            TokenType.IDENTIFIER -> value
            TokenType.ASSIGNMENT -> ":="
            TokenType.LAMBDA -> "\\"
            TokenType.PERIOD -> "."
            TokenType.OPEN_PAREN -> "("
            TokenType.CLOSE_PAREN -> ")"
        }
    }

    enum class TokenType {
        IDENTIFIER,
        ASSIGNMENT,
        LAMBDA,
        PERIOD,
        OPEN_PAREN,
        CLOSE_PAREN
    }
}

sealed class Either<out A, out B> {
    class Left<out A>(val value: A) : Either<A, Nothing>()
    class Right<out B>(val value: B) : Either<Nothing, B>()
}

typealias Error = String

//temp
private var parseDepth = 0

fun parse(tokens: List<Token>, env: Environment, lastExpression: Expression? = null): Either<Pair<Expression, Environment>, Error> {
    println("Parse Depth #${parseDepth++}: $tokens")
    if (tokens.size >= 2 && tokens[0].type == Token.TokenType.IDENTIFIER && tokens[1].type == Token.TokenType.ASSIGNMENT) {
        val varName = tokens[0].value
        val rest = tokens.subList(2, tokens.size)
        val result = parse(rest, env)
        return when (result) {
            is Either.Left<Pair<Expression, Environment>> -> {
                result.value.second.variables[Variable(varName)] = result.value.first
                result
            }
            is Either.Right<Error> -> result
        }
    } else if (tokens.isNotEmpty() && tokens[0].type == Token.TokenType.LAMBDA) {
        if (tokens.size >= 3 && tokens[1].type == Token.TokenType.IDENTIFIER && tokens[2].type == Token.TokenType.PERIOD) {
            val ident = tokens[1]
            val rest = tokens.subList(3, tokens.size)
            val result = parse(rest, env)
            return when (result) {
                is Either.Left<Pair<Expression, Environment>> -> {
                    val r = result.value
                    val abstraction = Abstraction(ident.value, r.first)
                    val env = r.second
                    Either.Left<Pair<Expression, Environment>>(Pair(abstraction, env))
                }
                is Either.Right<Error> -> result
            }
        } else {
            return Either.Right("Expected identifier after lambda.")
        }
    } else if (tokens.isNotEmpty() && tokens[0].type == Token.TokenType.IDENTIFIER) {
        if (lastExpression != null) {
            val application = Application(lastExpression, Variable(tokens[0].value))
            return if (tokens.size > 1) {
                val rest = tokens.subList(1, tokens.size)
                parse(rest, env, application)
            } else {
                Either.Left(Pair(application, env))
            }
        } else if (tokens.size >= 2 && tokens[1].type == Token.TokenType.IDENTIFIER) {
            val function = tokens[0].value
            val argument = Variable(tokens[1].value)
            val application = Application(Variable(function), argument)
            return if (tokens.size > 2) {
                val rest = tokens.subList(2, tokens.size)
                parse(rest, env, application)
            } else {
                Either.Left(Pair(application, env))
            }
        } else if (tokens.size >= 2 && tokens[1].type == Token.TokenType.OPEN_PAREN) {
            val function = tokens[0].value
            var indexOfCloseParen = -1
            tokens.forEachIndexed { index, token ->
                if (token.type == Token.TokenType.CLOSE_PAREN) {
                    indexOfCloseParen = index
                }
            }
            if (indexOfCloseParen < 0) {
                return Either.Right("Open parenthesis found without a closing one.")
            }
            val rest = tokens.subList(2, indexOfCloseParen)
            val result = parse(rest, env)
            return when (result) {
                is Either.Left<Pair<Expression, Environment>> -> {
                    val r = result.value
                    val argument = r.first
                    val env = r.second
                    Either.Left(Pair(Application(Variable(function), argument), env))
                }
                is Either.Right<Error> -> result
            }
        } else {
            // lone identifier
            return Either.Left(Pair(Variable(tokens[0].value), env))
        }
    }
    return Either.Right("Unknown")
}