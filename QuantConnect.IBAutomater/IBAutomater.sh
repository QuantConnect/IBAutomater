#!/bin/bash

export JAVA_TOOL_OPTIONS=$2

ps -AFH

pkill -9 Xvfb
pkill -9 java

sleep 5

rm /tmp/.X1-lock
ps -AFH

unset DISPLAY
Xvfb :1 -screen 0 1024x768x24 2>&1 >/dev/null &
export DISPLAY=:1

ibgatewayExecutable=$1
shift

$ibgatewayExecutable "$@"
