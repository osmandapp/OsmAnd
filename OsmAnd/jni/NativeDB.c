/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "NativeDB.h"
#include "sqlite3.h"

static jclass dbclass = 0;
static jclass  fclass = 0;
static jclass  aclass = 0;

static void * toref(jlong value)
{
    jvalue ret;
    ret.j = value;
    return (void *) ret.l;
}

static jlong fromref(void * value)
{
    jvalue ret;
    ret.l = value;
    return ret.j;
}

static void throwex(JNIEnv *env, jobject this)
{
    static jmethodID mth_throwex = 0;

    if (!mth_throwex)
        mth_throwex = (*env)->GetMethodID(env, dbclass, "throwex", "()V");

    (*env)->CallVoidMethod(env, this, mth_throwex);
}

static void throwexmsg(JNIEnv *env, const char *str)
{
    static jmethodID mth_throwexmsg = 0;

    if (!mth_throwexmsg) mth_throwexmsg = (*env)->GetStaticMethodID(
            env, dbclass, "throwex", "(Ljava/lang/String;)V");

    (*env)->CallStaticVoidMethod(env, dbclass, mth_throwexmsg,
                                (*env)->NewStringUTF(env, str));
}

static sqlite3 * gethandle(JNIEnv *env, jobject this)
{
    static jfieldID pointer = 0;
    if (!pointer) pointer = (*env)->GetFieldID(env, dbclass, "pointer", "J");

    return (sqlite3 *)toref((*env)->GetLongField(env, this, pointer));
}

static void sethandle(JNIEnv *env, jobject this, sqlite3 * ref)
{
    static jfieldID pointer = 0;
    if (!pointer) pointer = (*env)->GetFieldID(env, dbclass, "pointer", "J");

    (*env)->SetLongField(env, this, pointer, fromref(ref));
}

/* Returns number of 16-bit blocks in UTF-16 string, not including null. */
static jsize jstrlen(const jchar *str)
{
    const jchar *s;
    for (s = str; *s; s++);
    return (jsize)(s - str);
}


// User Defined Function SUPPORT ////////////////////////////////////

struct UDFData {
    JavaVM *vm;
    jobject func;
    struct UDFData *next;  // linked list of all UDFData instances
};

/* Returns the sqlite3_value for the given arg of the given function.
 * If 0 is returned, an exception has been thrown to report the reason. */
static sqlite3_value * tovalue(JNIEnv *env, jobject function, jint arg)
{
    jlong value_pntr = 0;
    jint numArgs = 0;
    static jfieldID func_value = 0,
                    func_args = 0;

    if (!func_value || !func_args) {
        func_value = (*env)->GetFieldID(env, fclass, "value", "J");
        func_args  = (*env)->GetFieldID(env, fclass, "args", "I");
    }

    // check we have any business being here
    if (arg  < 0) { throwexmsg(env, "negative arg out of range"); return 0; }
    if (!function) { throwexmsg(env, "inconstent function"); return 0; }

    value_pntr = (*env)->GetLongField(env, function, func_value);
    numArgs = (*env)->GetIntField(env, function, func_args);

    if (value_pntr == 0) { throwexmsg(env, "no current value"); return 0; }
    if (arg >= numArgs) { throwexmsg(env, "arg out of range"); return 0; }

    return ((sqlite3_value**)toref(value_pntr))[arg];
}

/* called if an exception occured processing xFunc */
static void xFunc_error(sqlite3_context *context, JNIEnv *env)
{
    const char *strmsg = 0;
    jstring msg = 0;
    jint msgsize = 0;

    jclass exclass = 0;
    static jmethodID exp_msg = 0;
    jthrowable ex = (*env)->ExceptionOccurred(env);

    (*env)->ExceptionClear(env);

    if (!exp_msg) {
        exclass = (*env)->FindClass(env, "java/lang/Throwable");
        exp_msg = (*env)->GetMethodID(
                env, exclass, "toString", "()Ljava/lang/String;");
    }

    msg = (jstring)(*env)->CallObjectMethod(env, ex, exp_msg);
    if (!msg) { sqlite3_result_error(context, "unknown error", 13); return; }

    msgsize = (*env)->GetStringUTFLength(env, msg);
    strmsg = (*env)->GetStringUTFChars(env, msg, 0);
    assert(strmsg); // out-of-memory

    sqlite3_result_error(context, strmsg, msgsize);

    (*env)->ReleaseStringUTFChars(env, msg, strmsg);
}

/* used to call xFunc, xStep and xFinal */
static xCall(
    sqlite3_context *context,
    int args,
    sqlite3_value** value,
    jobject func,
    jmethodID method)
{
    static jfieldID fld_context = 0,
                     fld_value = 0,
                     fld_args = 0;
    JNIEnv *env = 0;
    struct UDFData *udf = 0;

    udf = (struct UDFData*)sqlite3_user_data(context);
    assert(udf);
    (*udf->vm)->AttachCurrentThread(udf->vm, (void **)&env, 0);
    if (!func) func = udf->func;

    if (!fld_context || !fld_value || !fld_args) {
        fld_context = (*env)->GetFieldID(env, fclass, "context", "J");
        fld_value   = (*env)->GetFieldID(env, fclass, "value", "J");
        fld_args    = (*env)->GetFieldID(env, fclass, "args", "I");
    }

    (*env)->SetLongField(env, func, fld_context, fromref(context));
    (*env)->SetLongField(env, func, fld_value, value ? fromref(value) : 0);
    (*env)->SetIntField(env, func, fld_args, args);

    (*env)->CallVoidMethod(env, func, method);

    (*env)->SetLongField(env, func, fld_context, 0);
    (*env)->SetLongField(env, func, fld_value, 0);
    (*env)->SetIntField(env, func, fld_args, 0);

    // check if xFunc threw an Exception
    if ((*env)->ExceptionCheck(env)) xFunc_error(context, env);
}


void xFunc(sqlite3_context *context, int args, sqlite3_value** value)
{
    static jmethodID mth = 0;
    if (!mth) {
        JNIEnv *env;
        struct UDFData *udf = (struct UDFData*)sqlite3_user_data(context);
        (*udf->vm)->AttachCurrentThread(udf->vm, (void **)&env, 0);
        mth = (*env)->GetMethodID(env, fclass, "xFunc", "()V");
    }
    xCall(context, args, value, 0, mth);
}

void xStep(sqlite3_context *context, int args, sqlite3_value** value)
{
    JNIEnv *env;
    struct UDFData *udf;
    jobject *func = 0;
    static jmethodID mth = 0;
    static jmethodID clone = 0;

    if (!mth || !clone) {
        udf = (struct UDFData*)sqlite3_user_data(context);
        (*udf->vm)->AttachCurrentThread(udf->vm, (void **)&env, 0);

        mth = (*env)->GetMethodID(env, aclass, "xStep", "()V");
        clone = (*env)->GetMethodID(env, aclass, "clone",
            "()Ljava/lang/Object;");
    }

    // clone the Function.Aggregate instance and store a pointer
    // in SQLite's aggregate_context (clean up in xFinal)
    func = sqlite3_aggregate_context(context, sizeof(jobject));
    if (!*func) {
        udf = (struct UDFData*)sqlite3_user_data(context);
        (*udf->vm)->AttachCurrentThread(udf->vm, (void **)&env, 0);

        *func = (*env)->CallObjectMethod(env, udf->func, clone);
        *func = (*env)->NewGlobalRef(env, *func);
    }

    xCall(context, args, value, *func, mth);
}

void xFinal(sqlite3_context *context)
{
    JNIEnv *env = 0;
    struct UDFData *udf = 0;
    jobject *func = 0;
    static jmethodID mth = 0;

    udf = (struct UDFData*)sqlite3_user_data(context);
    (*udf->vm)->AttachCurrentThread(udf->vm, (void **)&env, 0);

    if (!mth) mth = (*env)->GetMethodID(env, aclass, "xFinal", "()V");

    func = sqlite3_aggregate_context(context, sizeof(jobject));
    assert(*func); // disaster

    xCall(context, 0, 0, *func, mth);

    // clean up Function.Aggregate instance
    (*env)->DeleteGlobalRef(env, *func);
}


// INITIALISATION ///////////////////////////////////////////////////

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env = 0;

    if (JNI_OK != (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2))
        return JNI_ERR;

    dbclass = (*env)->FindClass(env, "org/sqlite/NativeDB");
    if (!dbclass) return JNI_ERR;
    dbclass = (*env)->NewGlobalRef(env, dbclass);

    fclass = (*env)->FindClass(env, "org/sqlite/Function");
    if (!fclass) return JNI_ERR;
    fclass = (*env)->NewGlobalRef(env, fclass);

    aclass = (*env)->FindClass(env, "org/sqlite/Function$Aggregate");
    if (!aclass) return JNI_ERR;
    aclass = (*env)->NewGlobalRef(env, aclass);

    return JNI_VERSION_1_2;
}


// WRAPPERS for sqlite_* functions //////////////////////////////////

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_shared_1cache(
        JNIEnv *env, jobject this, jboolean enable)
{
    return sqlite3_enable_shared_cache(enable ? 1 : 0);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB__1open(
        JNIEnv *env, jobject this, jstring file)
{
    int ret;
    sqlite3 *db = gethandle(env, this);
    const char *str;

    if (db) {
        throwexmsg(env, "DB already open");
        sqlite3_close(db);
        return;
    }

    sqlite3_initialize();

    str = (*env)->GetStringUTFChars(env, file, 0); 
    if (sqlite3_open(str, &db)) {
        throwex(env, this);
        sqlite3_close(db);
        return;
    }
    (*env)->ReleaseStringUTFChars(env, file, str);

    sethandle(env, this, db);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB__1close(
        JNIEnv *env, jobject this)
{
    if (sqlite3_close(gethandle(env, this)) != SQLITE_OK)
        throwex(env, this);
    sethandle(env, this, 0);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_interrupt(JNIEnv *env, jobject this)
{
    sqlite3_interrupt(gethandle(env, this));
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_busy_1timeout(
    JNIEnv *env, jobject this, jint ms)
{
    sqlite3_busy_timeout(gethandle(env, this), ms);
}

JNIEXPORT jlong JNICALL Java_org_sqlite_NativeDB_prepare(
        JNIEnv *env, jobject this, jstring sql)
{
    sqlite3* db = gethandle(env, this);
    sqlite3_stmt* stmt;

    const char *strsql = (*env)->GetStringUTFChars(env, sql, 0);
    int status = sqlite3_prepare_v2(db, strsql, -1, &stmt, 0);
    (*env)->ReleaseStringUTFChars(env, sql, strsql);

    if (status != SQLITE_OK) {
        throwex(env, this);
        return fromref(0);
    }
    return fromref(stmt);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_errmsg(JNIEnv *env, jobject this)
{
    return (*env)->NewStringUTF(env, sqlite3_errmsg(gethandle(env, this)));
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_libversion(
        JNIEnv *env, jobject this)
{
    return (*env)->NewStringUTF(env, sqlite3_libversion());
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_changes(
        JNIEnv *env, jobject this)
{
    return sqlite3_changes(gethandle(env, this));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_finalize(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_finalize(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_step(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_step(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_reset(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_reset(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_clear_1bindings(
        JNIEnv *env, jobject this, jlong stmt)
{
    int i;
    int count = sqlite3_bind_parameter_count(toref(stmt));
    jint rc = SQLITE_OK;
    for(i=1; rc==SQLITE_OK && i <= count; i++) {
        rc = sqlite3_bind_null(toref(stmt), i);
    }
    return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1parameter_1count(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_bind_parameter_count(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_column_1count(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_column_count(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_column_1type(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_type(toref(stmt), col);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_column_1decltype(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const char *str = sqlite3_column_decltype(toref(stmt), col);
    return (*env)->NewStringUTF(env, str);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_column_1table_1name(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const void *str = sqlite3_column_table_name16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, jstrlen(str)) : NULL;
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_column_1name(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const void *str = sqlite3_column_name16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, jstrlen(str)) : NULL;
}

JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_column_1text(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return (*env)->NewStringUTF(
        env, (const char*)sqlite3_column_text(toref(stmt), col));
}

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_NativeDB_column_1blob(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    jsize length;
    jbyteArray jBlob;
    jbyte *a;
    const void *blob = sqlite3_column_blob(toref(stmt), col);
    if (!blob) return NULL;

    length = sqlite3_column_bytes(toref(stmt), col);
    jBlob = (*env)->NewByteArray(env, length);
    assert(jBlob); // out-of-memory

    a = (*env)->GetPrimitiveArrayCritical(env, jBlob, 0);
    memcpy(a, blob, length);
    (*env)->ReleasePrimitiveArrayCritical(env, jBlob, a, 0);

    return jBlob;
}

JNIEXPORT jdouble JNICALL Java_org_sqlite_NativeDB_column_1double(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_double(toref(stmt), col);
}

JNIEXPORT jlong JNICALL Java_org_sqlite_NativeDB_column_1long(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_int64(toref(stmt), col);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_column_1int(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_int(toref(stmt), col);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1null(
        JNIEnv *env, jobject this, jlong stmt, jint pos)
{
    return sqlite3_bind_null(toref(stmt), pos);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1int(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jint v)
{
    return sqlite3_bind_int(toref(stmt), pos, v);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1long(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jlong v)
{
    return sqlite3_bind_int64(toref(stmt), pos, v);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1double(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jdouble v)
{
    return sqlite3_bind_double(toref(stmt), pos, v);
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1text(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jstring v)
{
    const char *chars = (*env)->GetStringUTFChars(env, v, 0);
    int rc = sqlite3_bind_text(toref(stmt), pos, chars, -1, SQLITE_TRANSIENT);
    (*env)->ReleaseStringUTFChars(env, v, chars);
    return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_bind_1blob(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jbyteArray v)
{
    jint rc;
    void *a;
    jsize size = (*env)->GetArrayLength(env, v);
    assert(a = (*env)->GetPrimitiveArrayCritical(env, v, 0));
    rc = sqlite3_bind_blob(toref(stmt), pos, a, size, SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, v, a, JNI_ABORT);
    return rc;
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1null(
        JNIEnv *env, jobject this, jlong context)
{
    sqlite3_result_null(toref(context));
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1text(
        JNIEnv *env, jobject this, jlong context, jstring value)
{
    const jchar *str;
    jsize size;

    if (value == NULL) { sqlite3_result_null(toref(context)); return; }
    size = (*env)->GetStringLength(env, value) * 2;

    str = (*env)->GetStringCritical(env, value, 0);
    assert(str); // out-of-memory
    sqlite3_result_text16(toref(context), str, size, SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, value, str);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1blob(
        JNIEnv *env, jobject this, jlong context, jobject value)
{
    jbyte *bytes;
    jsize size;

    if (value == NULL) { sqlite3_result_null(toref(context)); return; }
    size = (*env)->GetArrayLength(env, value);

    // be careful with *Critical
    bytes = (*env)->GetPrimitiveArrayCritical(env, value, 0);
    assert(bytes); // out-of-memory
    sqlite3_result_blob(toref(context), bytes, size, SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, value, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1double(
        JNIEnv *env, jobject this, jlong context, jdouble value)
{
    sqlite3_result_double(toref(context), value);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1long(
        JNIEnv *env, jobject this, jlong context, jlong value)
{
    sqlite3_result_int64(toref(context), value);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_result_1int(
        JNIEnv *env, jobject this, jlong context, jint value)
{
    sqlite3_result_int(toref(context), value);
}




JNIEXPORT jstring JNICALL Java_org_sqlite_NativeDB_value_1text(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    jint length = 0;
    const void *str = 0;
    sqlite3_value *value = tovalue(env, f, arg);
    if (!value) return NULL;

    length = sqlite3_value_bytes16(value) / 2; // in jchars
    str = sqlite3_value_text16(value);
    return str ? (*env)->NewString(env, str, length) : NULL;
}

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_NativeDB_value_1blob(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    jsize length;
    jbyteArray jBlob;
    jbyte *a;
    const void *blob;
    sqlite3_value *value = tovalue(env, f, arg);
    if (!value) return NULL;

    blob = sqlite3_value_blob(value);
    if (!blob) return NULL;

    length = sqlite3_value_bytes(value);
    jBlob = (*env)->NewByteArray(env, length);
    assert(jBlob); // out-of-memory

    a = (*env)->GetPrimitiveArrayCritical(env, jBlob, 0);
    memcpy(a, blob, length);
    (*env)->ReleasePrimitiveArrayCritical(env, jBlob, a, 0);

    return jBlob;
}

JNIEXPORT jdouble JNICALL Java_org_sqlite_NativeDB_value_1double(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_double(value) : 0;
}

JNIEXPORT jlong JNICALL Java_org_sqlite_NativeDB_value_1long(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_int64(value) : 0;
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_value_1int(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_int(value) : 0;
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_value_1type(
        JNIEnv *env, jobject this, jobject func, jint arg)
{
    return sqlite3_value_type(tovalue(env, func, arg));
}


JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_create_1function(
        JNIEnv *env, jobject this, jstring name, jobject func)
{
    jint ret = 0;
    const char *strname = 0;
    int isAgg = 0;

    static jfieldID udfdatalist = 0;
    struct UDFData *udf = malloc(sizeof(struct UDFData));

    assert(udf); // out-of-memory

    if (!udfdatalist)
        udfdatalist = (*env)->GetFieldID(env, dbclass, "udfdatalist", "J");

    isAgg = (*env)->IsInstanceOf(env, func, aclass);
    udf->func = (*env)->NewGlobalRef(env, func);
    (*env)->GetJavaVM(env, &udf->vm);

    // add new function def to linked list
    udf->next = toref((*env)->GetLongField(env, this, udfdatalist));
    (*env)->SetLongField(env, this, udfdatalist, fromref(udf));

    strname = (*env)->GetStringUTFChars(env, name, 0);
    assert(strname); // out-of-memory

    ret = sqlite3_create_function(
            gethandle(env, this),
            strname,       // function name
            -1,            // number of args
            SQLITE_UTF16,  // preferred chars
            udf,
            isAgg ? 0 :&xFunc,
            isAgg ? &xStep : 0,
            isAgg ? &xFinal : 0
    );

    (*env)->ReleaseStringUTFChars(env, name, strname);

    return ret;
}

JNIEXPORT jint JNICALL Java_org_sqlite_NativeDB_destroy_1function(
        JNIEnv *env, jobject this, jstring name)
{
    const char* strname = (*env)->GetStringUTFChars(env, name, 0);
    sqlite3_create_function(
        gethandle(env, this), strname, -1, SQLITE_UTF16, 0, 0, 0, 0
    );
    (*env)->ReleaseStringUTFChars(env, name, strname);
}

JNIEXPORT void JNICALL Java_org_sqlite_NativeDB_free_1functions(
        JNIEnv *env, jobject this)
{
    // clean up all the malloc()ed UDFData instances using the
    // linked list stored in DB.udfdatalist
    jfieldID udfdatalist;
    struct UDFData *udf, *udfpass;

    udfdatalist = (*env)->GetFieldID(env, dbclass, "udfdatalist", "J");
    udf = toref((*env)->GetLongField(env, this, udfdatalist));
    (*env)->SetLongField(env, this, udfdatalist, 0);

    while (udf) {
        udfpass = udf->next;
        (*env)->DeleteGlobalRef(env, udf->func);
        free(udf);
        udf = udfpass;
    }
}


// COMPOUND FUNCTIONS ///////////////////////////////////////////////

JNIEXPORT jobjectArray JNICALL Java_org_sqlite_NativeDB_column_1metadata(
        JNIEnv *env, jobject this, jlong stmt)
{
    const char *zTableName, *zColumnName;
    int pNotNull, pPrimaryKey, pAutoinc, i, colCount;
    jobjectArray array;
    jbooleanArray colData;
    jboolean* colDataRaw;
    sqlite3 *db;
    sqlite3_stmt *dbstmt;

    db = gethandle(env, this);
    dbstmt = toref(stmt);

    colCount = sqlite3_column_count(dbstmt);
    array = (*env)->NewObjectArray(
        env, colCount, (*env)->FindClass(env, "[Z"), NULL) ;
    assert(array); // out-of-memory

    colDataRaw = (jboolean*)malloc(3 * sizeof(jboolean));
    assert(colDataRaw); // out-of-memory

    for (i = 0; i < colCount; i++) {
        // load passed column name and table name
        zColumnName = sqlite3_column_name(dbstmt, i);
        zTableName  = sqlite3_column_table_name(dbstmt, i);

        pNotNull = 0;
        pPrimaryKey = 0;
        pAutoinc = 0;

        // request metadata for column and load into output variables
        if (zTableName && zColumnName) {
            sqlite3_table_column_metadata(
                db, 0, zTableName, zColumnName,
                0, 0, &pNotNull, &pPrimaryKey, &pAutoinc
            );
        }

        // load relevant metadata into 2nd dimension of return results
        colDataRaw[0] = pNotNull;
        colDataRaw[1] = pPrimaryKey;
        colDataRaw[2] = pAutoinc;

        colData = (*env)->NewBooleanArray(env, 3);
        assert(colData); // out-of-memory

        (*env)->SetBooleanArrayRegion(env, colData, 0, 3, colDataRaw);
        (*env)->SetObjectArrayElement(env, array, i, colData);
    }

    free(colDataRaw);

    return array;
}

