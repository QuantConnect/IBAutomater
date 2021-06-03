#!/bin/bash

xwd -display :1 -root -silent | convert xwd:- png:$1
