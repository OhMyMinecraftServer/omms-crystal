package icu.takeneko.omms.crystal.util

sealed interface Result<R> {
    val isSuccess: Boolean

    data class Success<R>(val result: R) : Result<R> {
        override val isSuccess: Boolean
            get() = true
    }

    data class Failure<R>(val exception: Throwable) : Result<R> {
        override val isSuccess: Boolean
            get() = false
    }
}