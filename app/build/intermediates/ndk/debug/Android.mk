LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := plasma
LOCAL_SRC_FILES := \
	/Users/izzy/AndroidstudioProjects/Penelope/app/src/main/jni/Android.mk \
	/Users/izzy/AndroidstudioProjects/Penelope/app/src/main/jni/Application.mk \
	/Users/izzy/AndroidstudioProjects/Penelope/app/src/main/jni/plasma.c \

LOCAL_C_INCLUDES += /Users/izzy/AndroidstudioProjects/Penelope/app/src/main/jni
LOCAL_C_INCLUDES += /Users/izzy/AndroidstudioProjects/Penelope/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
