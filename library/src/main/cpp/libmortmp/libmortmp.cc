/*The MIT License (MIT)
Copyright (c) 2013-2015 SRS(ossrs)
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
/*
gcc srs_publish.c ../../objs/lib/srs_librtmp.a -g -O0 -lstdc++ -o srs_publish
*/
#include <jni.h>
#include <stdio.h>
#include <string.h>
using namespace std;

#include <android/log.h>
extern "C" {
#include <srs_librtmp.h>
}

#define LIBMORTMP_LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, "libmortmp", __VA_ARGS__))
#define LIBMORTMP_LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO , "libmortmp", __VA_ARGS__))
#define LIBMORTMP_LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN , "libmortmp", __VA_ARGS__))
#define LIBMORTMP_LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "libmortmp", __VA_ARGS__))

#define LIBMORTMP_ARRAY_ELEMS(a)  (sizeof(a) / sizeof(a[0]))

static JavaVM *jvm;
static JNIEnv *jenv;

srs_rtmp_t rtmp = NULL;

JNIEXPORT jint libmortmp_mo_rtmp_write_package(JNIEnv *env, jobject obj, jchar type , jint timestamp, jbyteArray data, jint size) {
    LIBMORTMP_LOGD("libmortmp_mo_rtmp_write_package\n");

    char *flv_data = (char*)env->GetByteArrayElements(data,JNI_FALSE);
    LIBMORTMP_LOGD("flv_data = %s", flv_data);
    char* tmp_data = NULL;
    int tmp_size = strlen(flv_data);
    LIBMORTMP_LOGD("tmp_size = %d", tmp_size);
    tmp_data = (char*)malloc(size);   // allocate data buffer to receive audio/video data from java side
    memcpy(tmp_data, flv_data,size);
    LIBMORTMP_LOGD("tmp_data = %s", tmp_data);

    if(rtmp != NULL){
        LIBMORTMP_LOGD("g_rtmp != NULL");
    } else {
        LIBMORTMP_LOGD("g_rtmp == NULL");
    }
    LIBMORTMP_LOGD("type=%s, time=%d, size=%d",
                   srs_human_flv_tag_type2string(type), timestamp, size);

    if (srs_rtmp_write_packet(rtmp, type, timestamp, tmp_data, size) != 0) {
        LIBMORTMP_LOGD("srs_rtmp_write_packet fail, srs_rtmp_destroy");
        srs_rtmp_destroy(rtmp);
        return 1;
    }

    env->ReleaseByteArrayElements(data, (jbyte*) flv_data, 0);
    LIBMORTMP_LOGD("srs_rtmp_write_packet success");
    LIBMORTMP_LOGD("sent packet: type=%s, time=%d, size=%d",
                    srs_human_flv_tag_type2string(type), timestamp, size);

    return 0;
}

void do_rtmp_destroy(srs_rtmp_t rtmp) {
    LIBMORTMP_LOGD("rtmp_destroy");
    srs_rtmp_destroy(rtmp);
}

JNIEXPORT jint libmortmp_mo_rtmp_create(JNIEnv *env, jobject obj, jstring url) {
    LIBMORTMP_LOGD("libmortmp_mo_rtmp_creater\n");
    char* tmp_url = (char*)env->GetStringUTFChars(url,JNI_FALSE);
    LIBMORTMP_LOGD("rtmp url: %s", tmp_url);

    rtmp = srs_rtmp_create(tmp_url);
    int sign = 0;

    if (srs_rtmp_handshake(rtmp) != 0) {
        LIBMORTMP_LOGD("simple handshake failed.");
        do_rtmp_destroy(rtmp);
    }
    sign++;
    LIBMORTMP_LOGD("simple handshake success");
    if (srs_rtmp_connect_app(rtmp) != 0) {
        LIBMORTMP_LOGD("connect vhost/app failed.");
        do_rtmp_destroy(rtmp);
    }
    sign++;
    LIBMORTMP_LOGD("connect vhost/app success");
    if (srs_rtmp_publish_stream(rtmp) != 0) {
        LIBMORTMP_LOGD("publish stream failed.");
        do_rtmp_destroy(rtmp);
    }
    sign++;
    LIBMORTMP_LOGD("publish stream success");

    return sign;
}

static JNINativeMethod libmortmp_methods[] = {
        {"moRtmpCreate",     "(Ljava/lang/String;)I", (void *) libmortmp_mo_rtmp_create},
        {"moRtmpWritePackage",     "(CI[BI)I", (void *) libmortmp_mo_rtmp_write_package},
};


jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;

    if (jvm->GetEnv((void **) &jenv, JNI_VERSION_1_6) != JNI_OK) {
        LIBMORTMP_LOGE("Env not got");
        return JNI_ERR;
    }

    jclass clz = jenv->FindClass("com/mo/rtmp/RtmpPublisher");
    if (clz == NULL) {
        LIBMORTMP_LOGE("Class \"com/mo/rtmp/RtmpPublisher\" not found");
        return JNI_ERR;
    }

    if (jenv->RegisterNatives(clz, libmortmp_methods, LIBMORTMP_ARRAY_ELEMS(libmortmp_methods))) {
        LIBMORTMP_LOGE("methods not registered");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
