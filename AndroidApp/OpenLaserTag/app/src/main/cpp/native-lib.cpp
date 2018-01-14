#include <jni.h>
#include <string>

extern "C"
jstring
Java_de_c_1ebberg_openlasertag_OpenLaserTagMain_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
