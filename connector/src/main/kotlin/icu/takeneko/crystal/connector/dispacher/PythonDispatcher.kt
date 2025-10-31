package icu.takeneko.crystal.connector.dispacher

import jep.Interpreter
import jep.JepConfig
import jep.SharedInterpreter

abstract class PythonDispatcher : Thread("PythonDispatcher") {
    protected lateinit var interpreter: Interpreter
    protected val taskDeque = ArrayDeque<PyTask<*>>()
    private val classloader = PythonDispatcher::class.java.classLoader

    init {
        SharedInterpreter.setConfig(
            JepConfig()
                .redirectStdout(System.out)
                .redirectStdErr(System.err)
        )
    }

    override fun run() {
        interpreter = SharedInterpreter()
        val initScript = classloader.getResourceAsStream("python_support.py")?.reader()?.readText()
            ?: throw RuntimeException("Python Bridge support file not found.")
        interpreter.exec(initScript)
        pyEntrypoint()
    }

    fun eval(string: String) {
        if (currentThread() != this) {
            throw IllegalStateException("eval() can be only called from PythonDispatcher threads!")
        }
        interpreter.eval(string)
    }

    abstract fun pyEntrypoint()

    fun runTasks() {
        synchronized(taskDeque) {
            for (task in taskDeque.toList()) {
                task()
            }
            taskDeque.clear()
        }
    }
}