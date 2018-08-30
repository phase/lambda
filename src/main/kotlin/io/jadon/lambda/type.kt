package io.jadon.lambda

import java.util.concurrent.ThreadLocalRandom

fun infer(exp: Expression, env: Environment, typeMap: MutableMap<String, Type> = mutableMapOf()): Type {
    return when (exp) {
        is Term -> {
            if (env.variables.map { it.key.name }.contains(exp.argument)) {
                val type: Type? = env.variables.filterKeys { it.name == exp.argument }.entries.firstOrNull()?.key?.type
                type ?: Untyped
            } else if (typeMap.containsKey(exp.argument)) {
                typeMap.getOrElse(exp.argument) { Untyped }
            } else {
                Untyped
            }
        }
        is Abstraction -> {
            fun t(): String = "t" + ThreadLocalRandom.current().nextInt(1024)
            // /a./b.a -> b
            val a = t()
            val b = t()
            val f = FunctionType(NamedType(a), NamedType(b))
            val p = PolyType(a, PolyType(b, f))
            typeMap[exp.argument] = p

            val bodyType  = infer(exp.value, env, typeMap)
            val f2 = FunctionType(NamedType(a), bodyType)
            PolyType(a, PolyType(b, f2))
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
