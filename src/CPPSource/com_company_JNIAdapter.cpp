#include "com_company_JNIAdapter.h"

#include "com_company_JNIAdapterImpl.h"
//For more comments please view .h file

/*
 * Casts java variable of type jlong containing pointer to JNIWindowSnap 
 * to c++ pointer
 */
JNIWindowsSnapAdapter* castPointer(jlong pointer)
{
	#if defined(_WIN64)
 		return reinterpret_cast<JNIWindowsSnapAdapter*>(pointer);
	#elif defined(_WIN32)
 		// 32-bit programs run on both 32-bit and 64-bit Windows
 		// so must sniff
 		return reinterpret_cast<JNIWindowsSnapAdapter*>(static_cast<void*>(pointer));
	#else
 		throw SnapError(L"This is not a WIN32 or WIN64 system!");
	#endif
}

/*
 * Casts process name from windows wchar_t* (max 260 symbols) to jstring
*/
jstring castProcessName(JNIEnv *env, wchar_t* name)
{
	return env->NewString(reinterpret_cast<unsigned short*>(name), 259);
}

/*
 * Does the same as above, but with char*
jstring castProcessName(JNIEnv *env, char* name)
{
	return env->NewStringUTF(name);
}
*/

/*
 * Class:     com_company_JNIAdapter
 * Method:    callConstructor
 * Signature: ()J
 *"Constructor"
 */
JNIEXPORT jlong JNICALL Java_com_company_JNIAdapter_callConstructor
	(JNIEnv * env, jobject object)
{
	return reinterpret_cast<jlong>(new JNIWindowsSnapAdapter());
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    callDestructor
 * Signature: (J)V
 *"Destructor"
 */
JNIEXPORT void JNICALL Java_com_company_JNIAdapter_callDestructor
	(JNIEnv * env, jobject object, jlong pointer)
{
	delete castPointer(pointer);
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getCurProcID
 * Signature: (J)J
 * Returns current process id
 */
JNIEXPORT jlong JNICALL Java_com_company_JNIAdapter_getCurProcID
	(JNIEnv *env, jobject object, jlong pointer)
{
	//Here unsigned casts to signed implicitly. That's not good but there is a guarantee
	//that after extension ulong -> ulong long last bits are 0
	return static_cast<unsigned long long>(castPointer(pointer)->getCurProcID());
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getCurProcName
 * Signature: (J)[C
 * Returns current process name
 * Returns wchar* containing current process name. Max length = 260
 */
JNIEXPORT jstring JNICALL Java_com_company_JNIAdapter_getCurProcName
	(JNIEnv *env, jobject object, jlong pointer)
{
	return castProcessName(env, castPointer(pointer)->getCurProcName());
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getCurProcThreadCnt
 * Signature: (J)I
 * Returns number of current process threads
 */
JNIEXPORT jint JNICALL Java_com_company_JNIAdapter_getCurProcThreadCnt
	(JNIEnv *env, jobject object, jlong pointer)
{
	return castPointer(pointer)->getCurProcThreadCnt();
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getProgramNameByActiveWindow
 * Signature: (J)[C
 * Returns process thaJava_com_company_JNIAdapter_getProgramNameByActiveWindowt owns current active window
 * Function can't get name of a priveleged (run as administrator) process.
 * It opens process inside to get it's name
 * Returns "" on failure
 */
JNIEXPORT jstring JNICALL Java_com_company_JNIAdapter_getProgramNameByActiveWindow
	(JNIEnv *env, jobject object, jlong pointer)
{
	wchar_t* name = castPointer(pointer)->getProgramNameByActiveWindow();
	jstring nameStr = castProcessName(env, castPointer(pointer)->getProgramNameByActiveWindow());
	delete[] name;
	return nameStr;
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getCpuLoadByProcess
 * Signature: (J)D
 * Returns current process CPU usage, in %
 */
JNIEXPORT jdouble JNICALL Java_com_company_JNIAdapter_getCpuLoadByProcess
	(JNIEnv *env, jobject object, jlong pointer)
{
	return castPointer(pointer)->getCpuLoadByProcess();
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getSizeTMax
 * Signature: (J)J
 * Returns amount of RAM used by current process, in MB
 * Returns SIZE_MAX on fail
 */
JNIEXPORT jlong JNICALL Java_com_company_JNIAdapter_getSizeTMax
  (JNIEnv *, jobject)
{
	return static_cast<jlong>(SIZE_MAX);
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    getRAMLoadByProcess
 * Signature: (J)J
 * Returns amount of RAM used by current process, in MB
 * Returns SIZE_MAX on fail
 */
JNIEXPORT jlong JNICALL Java_com_company_JNIAdapter_getRAMLoadByProcess
	(JNIEnv *env, jobject object, jlong pointer)
{
	return static_cast<jlong>(castPointer(pointer)->getRAMLoadByProcess());
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    toNextProcess
 * Signature: (J)Z
 * Gets next process entry from snap, opens next process, returns false on failure
 */
JNIEXPORT jboolean JNICALL Java_com_company_JNIAdapter_toNextProcess
	(JNIEnv *env, jobject object, jlong pointer)
{
	//jboolean is typedefed as unsigned char, so here bool extends to 8 bits
	return static_cast<jboolean>(castPointer(pointer)->toNextProcess());
}

/*
 * Class:     com_company_JNIAdapter
 * Method:    updateSnap
 * Signature: (J)Z
 * Update snap, returns false on failure
 */
JNIEXPORT jboolean JNICALL Java_com_company_JNIAdapter_updateSnap
	(JNIEnv *env, jobject object, jlong pointer)
{
	return static_cast<jboolean>(castPointer(pointer)->updateSnap());
}