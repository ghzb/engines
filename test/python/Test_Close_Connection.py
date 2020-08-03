import gym
import gym_TAIL
import random
import time

"""
This file is used in Java to verify that a Python script has the ability to close its connection with TAIL.
"""

env = gym.make('TAIL-v1')
time.sleep(5)
rand = random.random()

while True:
    if rand > 0.5:
        env.close()
        break
    else:
        env.step()