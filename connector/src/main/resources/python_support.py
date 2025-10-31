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
from mcdreforged.executor.console_handler import ConsoleHandler
from mcdreforged.mcdr_server_args import MCDReforgedServerArgs
from mcdreforged.mcdr_server import MCDReforgedServer
from mcdreforged.logging.logger import MCDReforgedLogger

old_stdout = None
bridge = MCDReforgedBridge.INSTANCE
logger = LoggerUtil.INSTANCE.createLogger("Python", True)


class FakeConsoleHandler(ConsoleHandler):
    def start(self):
        pass

    def stop(self):
        pass


def __handle_logs(self: Logger, record: LogRecord):
    if (not self.disabled) and self.filter(record):
        bridge.log(record.name, record.levelno, record.getMessage(), record.stack_info)


def __internal_init__(self, args: MCDReforgedServerArgs, old):
    old(self, args)
    self.console_handler = FakeConsoleHandler(self)


def patch_logger():
    MCDReforgedLogger.handle = __handle_logs


def patch_signal():
    setattr(MCDReforgedServer, "_MCDReforgedServer__register_signal_handler", lambda self: None)


def patch_init():
    old = MCDReforgedServer.__init__
    MCDReforgedServer.__init__ = lambda self, args: __internal_init__(self, args, old)


def run_patches():
    logger.info("Setting up patches for MCDReforged.")
    patch_logger()
    patch_init()
    patch_signal()


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
})
