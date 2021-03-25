#!/bin/bash
# workaround for building the fix from https://github.com/probot/smee-client/pull/162
# to be removed after the PR ^ is merged and a new smee-client released!!

git clone -b drop-host-header https://github.com/kahowell/smee-client
cd smee-client
npm install
npm run build
