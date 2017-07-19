###############################################################################
LOCAL_PATH := $(call my-dir)

###############################################################################
include $(CLEAR_VARS)

# Module name should match apk name to be installed.

LOCAL_MODULE := MotoLiveStream
LOCAL_MOTO_PLAYSTORE_APP := true
LOCAL_MODULE_TAGS := optional
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

LOCAL_CERTIFICATE := common

#LOCAL_SRC_FILES := $(LOCAL_PATH)/build/outputs/apk/MotoLiveStream-release.apk
LOCAL_GRADLE_TASKS := -PCLEAN_MOTOLIB -PRELEASE_VERSION assembleRelease
LOCAL_SRC_FILES := $(LOCAL_PATH)/build/outputs/apk/$(LOCAL_MODULE)-release.apk

include $(BUILD_GRADLE_PACKAGE)


