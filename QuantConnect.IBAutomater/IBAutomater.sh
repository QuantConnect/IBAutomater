#!/bin/bash

pkill Xvfb
pkill ibgateway

Xvfb :1 -screen 0 1024x768x24 2>&1 >/dev/null &
export DISPLAY=:1
$1/ibgateway
