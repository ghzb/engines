import gym
import gym_TAIL
import numpy

"""
This file is used to verify that TAIL can run a Python file with 3rd party libraries.
"""

env = gym.make('TAIL-v1')
env.close()
