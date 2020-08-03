import random
import signal
import sys
import os

"""
This file is used to test IPC IO streams.
Java has a hard time writing the output to the console without hanging.
The loop has high CPU usage to prevent thread from going into waiting status.
"""

i = 0
while True:
    k = ""
    for j in range(100000):
        rand = random.random()
        if rand > 0.5:
            k += str(rand)
        else:
            k = ""
    print("hello " + str(i), flush=True)
    i += 1
