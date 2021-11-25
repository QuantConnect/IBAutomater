#!/bin/bash

ps -AFH

pkill -9 Xvfb
pkill -9 java
pkill -9 socat

sleep 5

ps -AFH

socat TCP4-LISTEN:`hostname -I | awk '{print $1}' | awk '{split($0,p,"."); print 4p[4]}'`,bind=`hostname -I | awk '{print $1}'`,fork,forever TCP:127.0.0.1:4001 &

Xvfb :1 -screen 0 1024x768x24 2>&1 >/dev/null &
export DISPLAY=:1
$1/ibgateway
