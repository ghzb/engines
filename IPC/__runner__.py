# Do not use this file because it is used as a template to run python scripts. Without this file, users cannot debug fatal crashes.
def set_proc_name(newname):
    from ctypes import cdll, byref, create_string_buffer
    libc = cdll.LoadLibrary('libc.so.6')
    buff = create_string_buffer(len(newname)+1)
    buff.value = newname
    libc.prctl(15, byref(buff), 0, 0, 0)

def get_proc_name():
    from ctypes import cdll, byref, create_string_buffer
    libc = cdll.LoadLibrary('libc.so.6')
    buff = create_string_buffer(128)
    # 16 == PR_GET_NAME from <linux/prctl.h>
    libc.prctl(16, byref(buff), 0, 0, 0)
    return buff.value
set_proc_name(b"python_TAIL_RUNNER")
import traceback
from gym_TAIL import SocketRegister


"""
This file is auto generated by running PythonOptimizationEngine.
"""
try:
    #INJECT
except Exception as err:
    errors = traceback.TracebackException.from_exception(err).format()
    tb = ''.join(errors)
    SocketRegister.get(0).send(message=tb, channel="$ISSUE")
    SocketRegister.get(0).disconnect()
    quit()
