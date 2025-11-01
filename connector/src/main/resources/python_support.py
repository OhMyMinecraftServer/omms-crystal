def py311_workaround():
    import os.path
    import sys

    if os.name == 'nt' and \
            sys.version_info.major == 3 and \
            sys.version_info.minor >= 11:
        # Python 3.11 has a bug with DLLs directory missing from sys.path
        # so add it if it's not there
        base = sys.base_exec_prefix
        dllspath = os.path.join(base, sys.platlibdir)
        if os.path.exists(dllspath) and dllspath not in sys.path:
            i = sys.path.index(base) if base in sys.path else len(sys.path)
            sys.path.insert(i, dllspath)


py311_workaround()

from logging import LogRecord, Logger

from icu.takeneko.crystal.connector.mcdreforged import MCDReforgedBridge
from icu.takeneko.omms.crystal.util import LoggerUtil
from mcdreforged.constants import core_constant
from mcdreforged.plugin.type.builtin_plugin import BuiltinPlugin
from mcdreforged.executor.console_handler import ConsoleHandler
from mcdreforged.mcdr_server_args import MCDReforgedServerArgs
from mcdreforged.mcdr_server import MCDReforgedServer
from mcdreforged.logging.logger import MCDReforgedLogger
from mcdreforged.mcdr_state import ServerState, MCDReforgedState
from mcdreforged.plugin.plugin_manager import PluginManager

old_stdout = None
bridge = MCDReforgedBridge.INSTANCE
logger = LoggerUtil.INSTANCE.createLogger("Python", True)
__mcdr_instance: MCDReforgedServer = None


class FakeConsoleHandler(ConsoleHandler):
    def start(self):
        pass

    def stop(self):
        pass

class DelegatedPopen:
    def __getattr__(self, item):
        if item == "pid":
            return bridge.getServerPid()
        return None


class CrystalPlugin(BuiltinPlugin):
    def __init__(self, plugin_manager: 'PluginManager'):
        super().__init__(plugin_manager, {
            'id': 'crystal',
            'version': bridge.getCrystalVersion(),
            'name': 'Crystal {}'.format(bridge.getCrystalVersion())
        })

    def load(self):
        self.mcdr_server.logger.info(
            self.mcdr_server.translate('mcdreforged.python_plugin.info', core_constant.NAME, self.get_meta_name()))

    def _create_repr_fields(self) -> dict:
        return {'version': bridge.getCrystalVersion()}


def __handle_logs(self: Logger, record: LogRecord):
    global bridge
    if (not self.disabled) and self.filter(record):
        bridge.log(record.name, record.levelno, record.getMessage(), record.stack_info)


def __internal_init__(self: MCDReforgedServer, args: MCDReforgedServerArgs, old):
    global __mcdr_instance
    old(self, args)
    self.console_handler = FakeConsoleHandler(self)
    __mcdr_instance = self


def invoke_mcdr_internals(self, func, *args, **kwargs):
    getattr(self, f"_MCDReforgedServer{func}")(*args, **kwargs)

def __internal_start_server(self:MCDReforgedServer) -> bool:
    global bridge
    bridge.setMCDRRunning(True)
    with getattr(self, "_MCDReforgedServer__starting_server_lock"):
        invoke_mcdr_internals(self, "__on_server_start_pre")
        v = bridge.dispatchStartServer()
        if v:
            self.process = DelegatedPopen()
            invoke_mcdr_internals(self, "__on_server_start_post")
        return v


def __internal_tick(self, old):
    old()
    bridge.pythonBridgePollEvents()


def __internal_stop(self: MCDReforgedServer, forced: bool) -> bool:
    global bridge
    with self.__stop_lock:
        value = bridge.dispatchStopServer(forced)
        if value:
            self.set_server_state(ServerState.STOPPING)
        return value


def __internal_register_builtin_plugin(self: PluginManager, old):
    old(self)
    getattr(self, "_PluginManager__add_builtin_plugin")(CrystalPlugin(self))

def __internal_on_mcdr_start(self: MCDReforgedServer, old):
    old(self)


def patch_kill_server():
    setattr(MCDReforgedServer, "_MCDReforgedServer__kill_server", lambda self: __internal_stop(self, True))


def patch_logger():
    MCDReforgedLogger.handle = __handle_logs


def patch_signal():
    setattr(MCDReforgedServer, "_MCDReforgedServer__register_signal_handler", lambda self: None)


def patch_recv():
    setattr(MCDReforgedServer, "_MCDReforgedServer__receive", lambda self: bridge.pollServerStdout())


def patch_init():
    old = MCDReforgedServer.__init__
    MCDReforgedServer.__init__ = lambda self, args: __internal_init__(self, args, old)


def patch_start_server():
    MCDReforgedServer.start_server = __internal_start_server


def patch_stop():
    MCDReforgedServer.stop = __internal_stop


def patch_tick():
    old = getattr(MCDReforgedServer, "_MCDReforgedServer__tick")
    setattr(MCDReforgedServer, "_MCDReforgedServer__tick", lambda self: __internal_tick(self, old))


def patch_builtin_plugin():
    old = PluginManager.register_builtin_plugins
    PluginManager.register_builtin_plugins = lambda self: __internal_register_builtin_plugin(self, old)

def patch_run_mcdr():
    old = getattr(MCDReforgedServer, "_MCDReforgedServer__on_mcdr_start")
    setattr(MCDReforgedServer, "_MCDReforgedServer__on_mcdr_start", lambda self: __internal_on_mcdr_start(self, old))

def run_patches():
    logger.info("Setting up patches for MCDReforged.")
    patch_logger()
    patch_init()
    patch_signal()
    patch_tick()
    patch_start_server()
    patch_kill_server()
    patch_stop()
    patch_recv()
    patch_builtin_plugin()
    patch_run_mcdr()


def launch_mcdr(config_path: str, permission_path: str):
    logger.info("Launching MCDReforged.")
    from mcdreforged.cli.cmd_run import run_mcdr
    run_mcdr(
        MCDReforgedServerArgs(
            auto_init=False,
            no_server_start=False,
            config_file_path=config_path,
            permission_file_path=permission_path
        )
    )


def stop_mcdr():
    if __mcdr_instance:
        __mcdr_instance.interrupt()


def prepare_environment(config_file_path: str, permission_file_path: str):
    from mcdreforged.cli.cmd_init import initialize_environment
    initialize_environment(
        config_file_path=config_file_path,
        permission_file_path=permission_file_path,
        quiet=False
    )


globals().update({
    'py311_workaround': py311_workaround,
    'run_patches': run_patches,
    'launch_mcdr': launch_mcdr,
    'prepare_environment': prepare_environment,
    "stop_mcdr": stop_mcdr
})
