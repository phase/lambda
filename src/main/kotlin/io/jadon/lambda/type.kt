package io.jadon.lambda

fun stripPolyTypes(type: Type, polyTypeNames: MutableList<String> = mutableListOf()): Pair<Type, MutableList<String>> {
    return when (type) {
        is PolyType -> {
            polyTypeNames.add(type.typeVariable)
            stripPolyTypes(type.enclosingType, polyTypeNames)
        }
        else -> Pair(type, polyTypeNames)
    }
}

val chars = "abcdefghijklmnopqrstuvwxyz".toCharArray()
var charIndex = 0
var charWraps = 0

fun infer(exp: Expression, env: Environment, typeMap: MutableMap<String, Type> = mutableMapOf()): Type {
    return when (exp) {
        is Term -> {
            var t = if (env.variables.map { it.key.name }.contains(exp.argument)) {
                val type: Type? = env.variables.filterKeys { it.name == exp.argument }.entries.firstOrNull()?.key?.type
                type ?: Untyped
            } else if (typeMap.containsKey(exp.argument)) {
                val type = typeMap.getOrElse(exp.argument) { Untyped }
                type
            } else {
                Untyped
            }
            while (t is PolyType) t = t.enclosingType
            t
        }
        is Abstraction -> {
            // generate random type name
            fun t(): String {
                val letter = chars[charIndex].toString()
                charIndex++
                if (charIndex > chars.size) charWraps++
                charIndex %= chars.size
                return letter + if (charWraps > 0) charWraps else ""
            }

            fun findUsage(needle: String, expression: Expression, isApplication: Boolean = false): Int {
                return when (expression) {
                    is Abstraction -> {
                        findUsage(needle, expression.value, false)
                    }
                    is Application -> {
                        val function = expression.function
                        var inFunction = findUsage(needle, function, true)
                        val inArgument = findUsage(needle, expression.argument, false)

                        if (function !is Term && inFunction > 0) {
                            inFunction += 1
                        }
                        inFunction + inArgument
                    }
                    is Term -> {
                        if (expression.argument == needle) {
                            if (isApplication) 1 else 0
                        } else {
                            0
                        }
                    }
                }
            }

            val usagesAsFunction = findUsage(exp.argument, exp.value)

            var f = if (usagesAsFunction > 0) {
                val names = (0..usagesAsFunction).map { t() }.reversed()
                var parentType: Type = NamedType(names.first())
                (1..usagesAsFunction).map { parentType = FunctionType(NamedType(names[it]), parentType) }
                parentType
            } else {
                NamedType(t())
            }

            typeMap[exp.argument] = f

            val bodyType = infer(exp.value, env, typeMap)

            // update the type to see if we got any new information
            // (specifically for inferences made in the application branch)
            val maybeNewType = typeMap[exp.argument]
            if (maybeNewType != null) {
                f = maybeNewType
            }

            val strippedBodyType = stripPolyTypes(bodyType)
            val bodyRealType = strippedBodyType.first

            // push polytypes to the front
            var wrapperPolyType: Type = FunctionType(f, bodyRealType)
            val polyTypeNamesInBody = strippedBodyType.second.reversed().distinct()
            polyTypeNamesInBody.forEach { wrapperPolyType = PolyType(it, wrapperPolyType) }

            if (usagesAsFunction > 0 && f is FunctionType) {
                // /a./b.(a -> b) -> bodyType
                var parentPolyType = wrapperPolyType
                var child: Type = f
                while (child is FunctionType) {
                    val namedChildType = child.a as NamedType
                    if (!polyTypeNamesInBody.contains(namedChildType.name)) {
                        parentPolyType = PolyType(namedChildType.name, parentPolyType)
                    }
                    child = child.b
                }
                if (child is NamedType) {
                    parentPolyType = PolyType(child.name, parentPolyType)
                }
                parentPolyType
            } else if (f is NamedType) {
                // /a.a -> bodyType
                PolyType(f.name, wrapperPolyType)
            } else Untyped // shouldn't ever occur
        }
        is Application -> {
            var functionType = infer(exp.function, env, typeMap)

            val argumentType = infer(exp.argument, env, typeMap)
            val arg = exp.argument

            if (functionType is FunctionType) {
                if (functionType.a is NamedType && arg is Term) {
                    typeMap[arg.argument] = functionType.a
                } else if (functionType.a is FunctionType) {
                    functionType = FunctionType(FunctionType((functionType.a as FunctionType).a, argumentType), functionType.b)
                }
            }

            fun returnType(type: Type): Type {
                return when (type) {
                    is PolyType -> returnType(type.enclosingType)
                    is FunctionType -> type.b
                    is NamedType -> type
                    else -> Untyped
                }
            }

            returnType(functionType)
        }
    }
}
