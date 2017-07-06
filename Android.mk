###############################################################################
LOCAL_PATH := $(call my-dir)

###############################################################################
include $(CLEAR_VARS)

# Module name should match apk name to be installed.

LOCAL_MODULE := MotoLiveStream
LOCAL_MOTO_PLAYSTORE_APP := true

LOCAL_SRC_FILES := app/build/outputs/apk/app-release.apk
LOCAL_MODULE_TAGS := optional
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

LOCAL_CERTIFICATE := common

include $(BUILD_GRADLE_PACKAGE)
