LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := libmortmp
LOCAL_SRC_FILES += \
    $(LOCAL_PATH)/srs_librtmp/source/srs_librtmp.cpp \
    $(LOCAL_PATH)/libmortmp.cc
LOCAL_C_INCLUDES := $(LOCAL_PATH)/srs_librtmp/include
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)