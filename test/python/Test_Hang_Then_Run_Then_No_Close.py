import gym
import gym_TAIL
import random
import time

"""
This file is used to verify that TAIL blocks the current thread until Python calls step or reset.
This file hangs differently than "Test_Infinite"
The file also acts as a script that does not call close.
"""

env = gym.make('TAIL-v1')
time.sleep(5)
rand = random.random()
for i in range(20):
    if rand > 0.5:
        env.reset()
    else:
        env.step()
