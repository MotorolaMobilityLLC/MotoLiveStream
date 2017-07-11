#!/bin/bash
#-------------------------------------------------------------------------------
#  build.bash - phoenix application build script
#-------------------------------------------------------------------------------

# to make bash scripts behave like makefiles, exit on any error
set -e

script_name=$(basename ${0})

function usage {
    echo ""
    echo "Usage: ${script_name} -d -p -a"
    echo "      d: debug"
    echo "      a: ant build"
    echo "      f: build flavor, eg. myresConfigs"
    echo ""
    exit 1
}


while getopts dpaf o
do
    case "$o" in
        d)  set -x ;;
        a)  ant_build=true ;;
	f)  build_flavor="$OPTARG" ;;
        [?]) usage ;;
    esac
done

script_dir=$(cd $(dirname ${0}); pwd)
top_dir=${script_dir}

rm -rf ${script_dir}/build
cd ${script_dir}
git clone ssh://gerrit.mot.com/home/repo/dev/apps/build.git -b main-n-7.1 --single-branch
if [ -d ${script_dir}/build-common ]; then
    rm -rf ${script_dir}/build-common
fi
mv ${script_dir}/build ${script_dir}/build-common
source ${script_dir}/build-common/build-common.conf

#copy pre-commit hook to .git/hooks
cp -f ${script_dir}/build-common/pre-commit ${script_dir}/.git/hooks/pre-commit

if [ ! -d ${android_sdk_dir} ]; then
    android_sdk_branch=${android_sdk_starter_pkg_prefix}
    if [ "${operating_system}" == "Darwin" ]; then
        android_sdk_branch=${android_sdk_branch}-macosx
        git clone ssh://gerrit.mot.com/home/repo/dev/AndroidSDK.git -b ${android_sdk_branch} --single-branch
        mv AndroidSDK ${android_sdk_dir}
    elif [ "${operating_system}" == "Linux" ]; then
        android_sdk_branch=${android_sdk_branch}-linux
        if [ -d /apps/platform/${android_sdk_branch} ]; then
            android_sdk_dir="/apps/platform/${android_sdk_branch}"
        else
            git clone ssh://gerrit.mot.com/home/repo/dev/AndroidSDK.git -b ${android_sdk_branch} --single-branch
            mv AndroidSDK ${android_sdk_dir}
        fi
    fi
fi

# Get NDK
if [ ! -d ${android_ndk_dir} ]; then
    wget http://dl.google.com/android/ndk/${android_ndk_starter_pkg} -O ${top_dir}/${android_ndk_starter_pkg}
    chmod +x ${top_dir}/${android_ndk_starter_pkg}
    cd ${top_dir}
    ./${android_ndk_starter_pkg}
    rm ${top_dir}/${android_ndk_starter_pkg}
fi

rm -f ${top_dir}/v8*.log
cd ${script_dir}

export ANDROID_HOME=${android_sdk_dir}
# Version is <major>.<minor>.<build_number>
# AndroidVersionCode is an INT32 so MAX 4,294,967,295
# Defining the schema using 9 digits <P><2><2><5>
# where <P> is one digit which can have the value 0 to 3. We leave it to 0 for now.

VERSION_MAJOR=1
VERSION_MINOR=0
VERSION_CODE=`printf "%d%02d%05d" $VERSION_MAJOR $VERSION_MINOR $BUILD_NUMBER`
VERSION_NAME=`printf "%d.%d.%d" $VERSION_MAJOR $VERSION_MINOR $BUILD_NUMBER`

if [ "${ant_build}" == "true" ];then
    cd ${script_dir}
    ant clean release -Dversion.code=$VERSION_CODE -Dversion.name=$VERSION_NAME
else
    cd ${script_dir}
    if [ -z $build_flavor ]; then
	./gradlew clean build
    else
	if [ -z $GRADLE_PRODUCT_AAPT_CONFIG ]; then
	    echo "No value of GRADLE_PRODUCT_AAPT_CONFIG, please check"
	else
	    ./gradlew --refresh-dependencies clean assemble${build_flavor}
	fi
    fi
fi

#unaligned_files=$(find . -name "*.apk" | grep unaligned || exit 0)
#rm -f $unaligned_files
