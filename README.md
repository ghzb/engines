#### Q-Learning

Package names need to be changed based on folder structure.

## Known issue
- Sockets for PythonOptimization use a "busy wait". The current thread maxes out CPU usage and may cause other problems as a result. This article appears to have a solution. https://pymotw.com/2/select/
