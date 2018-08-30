package io.jadon.lambda

import java.util.concurrent.ThreadLocalRandom

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
            fun t(): String {
                val letter = chars[charIndex].toString()
                charIndex++
                if (charIndex > chars.size) charWraps++
                charIndex %= chars.size
                return letter + if (charWraps > 0) charWraps else ""
            }
            // /a./b.a -> b
            val a = t()
            val b = t()
            val f = FunctionType(NamedType(a), NamedType(b))
            val p = PolyType(a, PolyType(b, f))
            typeMap[exp.argument] = p

            val bodyType = infer(exp.value, env, typeMap)

            val strippedBodyType = stripPolyTypes(bodyType)
            val bodyRealType = strippedBodyType.first

            val f2 = FunctionType(f, bodyRealType)
            var wrapperPolyType: Type = f2
            strippedBodyType.second.reversed().forEach { wrapperPolyType = PolyType(it, wrapperPolyType) }

            PolyType(a, PolyType(b, wrapperPolyType))
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
