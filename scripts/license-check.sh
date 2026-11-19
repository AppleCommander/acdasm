#!/bin/bash

find . -name "*.java" -not -name module-info.java -not -name package-info.java | xargs grep -L "Apache License, Version 2.0"
