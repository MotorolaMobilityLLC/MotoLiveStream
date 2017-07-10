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

LOCAL_REQUIRED_MODULES := \
    com.motorola.livestream.xml

#LOCAL_SRC_FILES := $(LOCAL_PATH)/build/outputs/apk/MotoLiveStream-release.apk
LOCAL_GRADLE_TASKS := -PCLEAN_MOTOLIB -PRELEASE_VERSION assembleRelease
LOCAL_SRC_FILES := $(LOCAL_PATH)/build/outputs/apk/$(LOCAL_MODULE)-release.apk

include $(BUILD_GRADLE_PACKAGE)

# ========================================================
# Install the permissions file into system/etc/permissions
# to ensure Market Upgrades can only occur to devices which
# have preloaded this application
# ========================================================
include $(CLEAR_VARS)
LOCAL_MODULE := com.motorola.livestream.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

