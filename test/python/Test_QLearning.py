import gym
from gym_TAIL import *
import numpy as np

"""
This file is a messy Python script I wrote while learning
about OpenAI gym and Q-Learning. This test proves that learning
can occur in TAIL by modifying a few lines of code from any existing
RL scripts written in Python.
"""


env = gym.make('TAIL-v1')
n_states = 20
iter_max = 10000

init_learning_rate = 1.0
min_learning_rate = 0.003
gamma = 1.0
t_max = 10000
eps = 0.02

env.seed(0)
np.random.seed(0)
debug('-----q learning -----')
qtable = np.zeros((n_states+1, n_states+1, env.action_space.n))


# -----------------------------------------------------------------------------------

def bellman(**kwargs):
    reward = kwargs.get('reward')
    learning_rate = kwargs.get('learning_rate')
    discount_rate = kwargs.get('discount_rate')
    present = kwargs.get('present')
    future = kwargs.get('future')
    return present + learning_rate * (reward + discount_rate * np.max(future) - present)


# -----------------------------------------------------------------------------------


def run_episode(env, policy=None, render=True):
    obs = env.reset()
    total_reward = 0
    step_idx = 0
    for _ in range(t_max):
        if render:
            env.render()
        if policy is None:
            action = env.action_space.sample()
        else:
            a, b = get_state(env, obs)
            action = policy[a][b]
        obs, reward, done, _ = env.step(action)
        total_reward += gamma ** step_idx * reward
        step_idx += 1
        if done:
            break
    return total_reward


def get_state(env, obs):
    low = env.observation_space.low
    high = env.observation_space.high
    dx = (high - low) / n_states
    a = int((obs[0] - low[0]) / dx[0])
    b = int((obs[1] - low[1]) / dx[1])
    # debug("""
    # In algorithm - get_state:
    #     low = %r
    #     high = %r
    #     dx = %r
    #     obs = %r
    #     n_states = %r
    #     a = %r
    #     b = %r
    # """ % (low, high, dx, obs, n_states, a, b))
    return a, b


def build_solution():
    for episode in range(iter_max):
        obs = env.reset()
        total_reward = 0
        # decrease learning rate at each step
        eta = max(min_learning_rate, init_learning_rate * (0.85 ** (episode // 100)))
        for _ in range(t_max):
            a, b = get_state(env, obs)
            if episode % 100 == 0:
                env.render()
            if np.random.uniform(0, 1) < eps:
                action = np.random.choice(env.action_space.n)
            else:
                logits = qtable[a][b]
                logits_exp = np.exp(logits)
                probs = logits_exp / np.sum(logits_exp)
                action = np.random.choice(env.action_space.n, p=probs)
            obs, reward, done, _ = env.step(action)
            # debug(done, override=True)
            total_reward += reward
            # update q table
            a_, b_ = get_state(env, obs)
            qtable[a][b][action] = bellman(
                present=qtable[a][b][action],
                learning_rate=eta,
                reward=reward,
                discount_rate=gamma,
                future=qtable[a_][b_]
            )
            # qtable[a][b][action] = qtable[a][b][action] + eta * (reward + gamma * np.max(qtable[a_][b_]) - qtable[a][b][action])
            if done:
                break
        if episode % 100 == 0:
            debug(qtable[a_][b_])
            debug('Iteration #%d -- Total reward = %d.' % (episode + 1, total_reward))


# try:
#     with open('solution', 'rb') as fp:
#         solution_policy = pickle.load(fp)
# except Exception as err:

build_solution()
# solution_policy = np.argmax(qtable, axis=2)
# solution_policy_scores = [run_episode(env, solution_policy, False) for _ in range(100)]
# debug("Average score of solution = ", np.mean(solution_policy_scores))



#
# # if (solution_policy is None):
# # with open('solution', 'wb') as fp:
# #     pickle.dump(np.argmax(qtable, axis=2), fp)
# # solution_policy = np.argmax(qtable, axis=2)
#
# run_episode(env, solution_policy, True)

# with open('solution', 'wb') as fp:
#     pickle.dump(solution_policy, fp)
