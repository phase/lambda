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
                typeMap.getOrElse(exp.argument) { Untyped }
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

            fun findUsage(needle: String, expression: Expression, isApplication: Boolean = false): Boolean {
                return when (expression) {
                    is Abstraction -> {
                        findUsage(needle, expression.value, false)
                    }
                    is Application -> {
                        findUsage(needle, expression.function, true) || findUsage(needle, expression.argument, false)
                    }
                    is Term -> {
                        if (expression.argument == needle) {
                            isApplication
                        } else {
                            false
                        }
                    }
                }
            }

            val isBeingUsedAsApplication = findUsage(exp.argument, exp.value)

            val f = if (isBeingUsedAsApplication) {
                val a = t()
                val b = t()
                FunctionType(NamedType(a), NamedType(b))
            } else {
                val a = t()
                NamedType(a)
            }

            typeMap[exp.argument] = f

            val bodyType = infer(exp.value, env, typeMap)

            val strippedBodyType = stripPolyTypes(bodyType)
            val bodyRealType = strippedBodyType.first

            // push polytypes to the front
            val f2 = FunctionType(f, bodyRealType)
            var wrapperPolyType: Type = f2
            strippedBodyType.second.reversed().forEach { wrapperPolyType = PolyType(it, wrapperPolyType) }

            if (isBeingUsedAsApplication && f is FunctionType) {
                // /a.a -> bodyType
                val a = (f.a as NamedType)
                val b = (f.b as NamedType)
                PolyType(a.name, PolyType(b.name, wrapperPolyType))
            } else if (f is NamedType) {
                // /a./b.(a -> b) -> bodyType
                PolyType(f.name, wrapperPolyType)
            } else Untyped // shouldn't ever occur
        }
        is Application -> {
            val functionType = infer(exp.function, env, typeMap)

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
