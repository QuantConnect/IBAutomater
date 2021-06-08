#!/bin/bash

ps -AFH

pkill -9 Xvfb
pkill -9 java

sleep 5

ps -AFH

Xvfb :1 -screen 0 1024x768x24 2>&1 >/dev/null &
export DISPLAY=:1
$1/ibgateway
