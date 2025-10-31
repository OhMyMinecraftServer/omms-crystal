package icu.takeneko.crystal.connector.dispacher

class PyTask<R>(private val callback: () -> R) : () -> Unit {
    var finished: Boolean = false
        private set

    var result: Result<R>? = null

    fun markFinished(result: Result<R>) {
        finished = true
        this.result = result
    }

    override fun invoke() {
        try {
           markFinished(Result.Success(callback()))
        }catch (t: Throwable){
            markFinished(Result.Failure(t))
        }
    }
}