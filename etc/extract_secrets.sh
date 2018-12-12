#!/bin/bash

set -e

openssl aes-256-cbc -K $encrypted_858cdb5d04b3_key -iv $encrypted_858cdb5d04b3_iv -in etc/codesigning.asc.enc -out etc/codesigning.asc -d
gpg --fast-import etc/codesigning.asc

