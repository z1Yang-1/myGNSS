#include <jni.h>
#include "rtklib.h"

// 定义全局变量
// 定义全局变量
rtcm_t *RTCM;
//rtcm_t *RTCM1;
//rtcm_t *RTCM2;
__attribute__((unused)) int sat1 = 0;
int nn=0;
int n1=0;
int n2=0;
int n3=0;
int n4=0;
int count1=0;
int count2=0;
int count3=0;



JNIEXPORT jint JNICALL
Java_com_example_myapplication_ui_Ntrip_NtripFragment_inputrtcm3(
        JNIEnv *env,jobject thiz, jobject rtcm,
        jbyte data, jobject obss, jobject obstime, jdoubleArray stapos) {
    // 将 jbyte 转换为 uint8_t
    uint8_t data_uint8 = (uint8_t)data;
    jdouble time = 0;
    jdouble sec = 0;
    jint satValue = 0;
    int result = input_rtcm3(RTCM, data_uint8);
    if (result != 0) {
        // 创建一个 Java double 数组
        jdoubleArray posArray = (*env)->NewDoubleArray(env, 3);
        if (posArray == NULL) {
            return (jint) NULL; // 内存分配失败
        }
        // 将 C 数组复制到 Java 数组
        (*env)->SetDoubleArrayRegion(env, posArray, 0, 3, RTCM->sta.pos);
        // 将 posArray 的内容复制到 stapos
        if (stapos != NULL) {
            (*env)->SetDoubleArrayRegion(env, stapos, 0, 3, RTCM->sta.pos);
        }
//        stapos = posArray;
                jclass obsClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env,obss,0));
                jclass timeClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env,obstime,0));
                for (int i = 0; i < RTCM->obs.n; i++) {
                    jfieldID pField = (*env)->GetFieldID(env, obsClass, "P", "[D");
                    jfieldID lField = (*env)->GetFieldID(env, obsClass, "L", "[D");
                    jfieldID dField = (*env)->GetFieldID(env, obsClass, "D", "[F");
                    jfieldID snrField = (*env)->GetFieldID(env, obsClass, "SNR", "[I");
                    jfieldID satField = (*env)->GetFieldID(env, obsClass, "sat", "[I");
                    jfieldID codeField = (*env)->GetFieldID(env, obsClass, "code", "[I");
                    jfieldID timeField = (*env)->GetFieldID(env, timeClass, "time", "D");
                    jfieldID secField = (*env)->GetFieldID(env, timeClass, "sec", "D");
                    // 创建新的数组，并将C语言中的值复制过来
                    jdoubleArray lArray = (*env)->NewDoubleArray(env, NFREQ + NEXOBS);
                    jdoubleArray pArray = (*env)->NewDoubleArray(env, NFREQ + NEXOBS);
                    jfloatArray dArray = (*env)->NewFloatArray(env, NFREQ + NEXOBS);
                    jintArray snrArray = (*env)->NewIntArray(env, NFREQ + NEXOBS);
                    jintArray satArray = (*env)->NewIntArray(env, 1);
                    jintArray codeArray = (*env)->NewIntArray(env, NFREQ + NEXOBS);

                    satValue = (jint) RTCM->obs.data[i].sat;
                    time = RTCM->obs.data[i].time.time;
                    sec = RTCM->obs.data[i].time.sec;


                    (*env)->SetIntArrayRegion(env, satArray, 0, 1, &satValue);

                    (*env)->SetDoubleArrayRegion(env, pArray, 0, NFREQ + NEXOBS,
                                                 RTCM->obs.data[i].P);

                    (*env)->SetDoubleArrayRegion(env, lArray, 0, NFREQ + NEXOBS,
                                                 RTCM->obs.data[i].L);

                    (*env)->SetFloatArrayRegion(env, dArray, 0, NFREQ + NEXOBS,
                                                 RTCM->obs.data[i].D);
                    jint snrValues[NFREQ + NEXOBS]; // 创建一个 jint 数组来存储处理后的值
                    for (int j = 0; j < NFREQ + NEXOBS; j++) {
                        snrValues[j] = RTCM->obs.data[i].SNR[j] / 1000; // 假设 SNR 是一个数组
                    }
                    (*env)->SetIntArrayRegion(env, snrArray, 0, NFREQ + NEXOBS,
                                              (const jint *) snrValues);

                    jint codeValues[NFREQ + NEXOBS];
                    for (int j = 0; j < NFREQ + NEXOBS; j++) {
                        codeValues[j] = (jint) RTCM->obs.data[i].code[j];
                    }
                    (*env)->SetIntArrayRegion(env, codeArray, 0, NFREQ + NEXOBS, codeValues);

                    jobject obsd_t_java = (*env)->GetObjectArrayElement(env, obss, i + n1 + n2);
                    jobject gtime_t_java = (*env)->GetObjectArrayElement(env, obstime, i + n1 + n2);
                    // 将新的数组设置到Java对象的字段
                    (*env)->SetObjectField(env, obsd_t_java, pField, pArray);
                    (*env)->SetObjectField(env, obsd_t_java, lField, lArray);
                    (*env)->SetObjectField(env, obsd_t_java, dField, dArray);
                    (*env)->SetObjectField(env, obsd_t_java, snrField, snrArray);
                    (*env)->SetObjectField(env, obsd_t_java, satField, satArray);
                    (*env)->SetObjectField(env, obsd_t_java, codeField, codeArray);
                    (*env)->SetDoubleField(env, gtime_t_java, timeField, time);
                    (*env)->SetDoubleField(env, gtime_t_java, secField, sec);

                    //释放本地引用
                    (*env)->DeleteLocalRef(env, pArray);
                    (*env)->DeleteLocalRef(env, lArray);
                    (*env)->DeleteLocalRef(env, dArray);
                    (*env)->DeleteLocalRef(env, snrArray);
                    (*env)->DeleteLocalRef(env, satArray);
                    (*env)->DeleteLocalRef(env, codeArray);
                    (*env)->DeleteLocalRef(env, obsd_t_java);
                }
        return result;
    }
    return result;
    }



JNIEXPORT void JNICALL
Java_com_example_myapplication_ui_Ntrip_NtripFragment_initrtcm(JNIEnv *env, jobject thiz) {
RTCM = malloc(sizeof(rtcm_t));
if (RTCM == NULL) {
fprintf(stderr, "内存分配失败\n");
exit(EXIT_FAILURE);
}
init_rtcm(RTCM);
}