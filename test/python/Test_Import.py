import gym
import gym_TAIL

"""
This file is used to verify that TAIL can run a Python file with 3rd party libraries.
"""

env = gym.make('MountainCar-v0')
for i in range(100):
    print(env.reset())