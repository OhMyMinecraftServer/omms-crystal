package icu.takeneko.crystal.connector.dispacher

import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.CrystalTask
import jep.Interpreter
import jep.JepConfig
import jep.MainInterpreter
import jep.PyConfig
import jep.SharedInterpreter

abstract class PythonDispatcher : Thread("PythonDispatcher") {
    private val logger = LoggerUtil.createLogger("PythonDispatcher")
    protected lateinit var interpreter: Interpreter
    protected val taskDeque = ArrayDeque<CrystalTask<*>>()
    private val classloader = PythonDispatcher::class.java.classLoader

    override fun run() {
        logger.info("Bootstrapping Python Interpreter")
        SharedInterpreter.setConfig(
            JepConfig()
                .redirectStdout(System.out)
                .redirectStdErr(System.err)
                .setClassLoader(classloader)
                .setClassEnquirer(CrystalClassEnquirer())
        )
        MainInterpreter.setInitParams(
            PyConfig()
                //.setVerboseFlag(1)
        )
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