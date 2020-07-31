#!/usr/bin/env bash
#trap 'echo "Be patient"' INT
command -v python
/usr/bin/env pip3 list | grep gym
$1 "$2"