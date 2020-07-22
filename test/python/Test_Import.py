import gym
import gym_TAIL

env = gym.make('MountainCar-v0')
for i in range(100):
    print(env.reset())