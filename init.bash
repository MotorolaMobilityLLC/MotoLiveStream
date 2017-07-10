#!/bin/bash
#-------------------------------------------------------------------------------
#  build.bash - phoenix application build script
#-------------------------------------------------------------------------------

# to make bash scripts behave like makefiles, exit on any error
set -e

script_dir=$(cd $(dirname ${0}); pwd)
cd ${script_dir}
rm -rf ${script_dir}/build
git clone ssh://gerrit.mot.com/home/repo/dev/apps/build.git -b main-l-5.1 --single-branch
#copy pre-commit hook to .git/hooks
cp -f ${script_dir}/build/pre-commit ${script_dir}/.git/hooks/pre-commit
rm -rf ${script_dir}/build
