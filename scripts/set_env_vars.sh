#!/usr/bin/env bash
# Copyright (c) Meta Platforms, Inc. and affiliates.
# 
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

# Get path to where this file is. https://stackoverflow.com/a/246128
DIR="$( cd "$( dirname "${BASH_SOURCE[0]:-${(%):-%x}}"   )" >/dev/null 2>&1 && pwd   )"
REPO_ROOT=$(dirname $(dirname $DIR))

# Set Github env vars for authenticating to github packages
GITHUB_DOTENV=$DIR/github.env
if [ -f "$GITHUB_DOTENV"  ]; then
    source $GITHUB_DOTENV
    export $(cut -d= -f1 $GITHUB_DOTENV)
else
    echo "Warning: github.env not found so Github env vars were not automatically set"
fi
