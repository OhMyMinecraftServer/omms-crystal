package icu.takeneko.omms.crystal.util.reflect

import java.lang.reflect.Method

inline fun <reified A : Annotation> Class<*>.methodsWithAnnotation(): List<Method> =
    declaredMethods.filter { it.isAnnotationPresent(A::class.java) }
