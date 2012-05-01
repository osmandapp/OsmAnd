#ifndef _OSMAND_COMMON_H
#define _OSMAND_COMMON_H

#include <jni.h>
#include <string>
#include <vector>

#include <hash_map>
#include <hash_set>
#include <SkPath.h>
#include <SkBitmap.h>


#ifdef _MSC_VER
typedef __int8  int8;
typedef __int16 int16;
typedef __int32 int32;
typedef __int64 int64;

typedef unsigned __int8  uint8;
typedef unsigned __int16 uint16;
typedef unsigned __int32 uint32;
typedef unsigned __int64 uint64;
#else
typedef int8_t  int8;
typedef int16_t int16;
typedef int32_t int32;
typedef int64_t int64;

typedef uint8_t  uint8;
typedef uint16_t uint16;
typedef uint32_t uint32;
typedef uint64_t uint64;
#endif

#ifndef ANDROID

#define HMAP __gnu_cxx
namespace __gnu_cxx {
  template<>
  struct hash<std::string>
  {
    hash<char*> h;
    size_t operator()(const std::string &s) const
    {
      return h(s.c_str());
    };
  };
  template<>
    struct hash<long long int>
    {
      size_t
      operator()(long long int __x) const
    { return __x; }
  };

  template<>
    struct hash<unsigned long long int>
    {
      size_t
      operator()(unsigned long long int __x) const
      { return __x; }
    };
}


#else

#define HMAP

#endif

using namespace std;


#ifdef PROFILE_NATIVE_OPERATIONS
	#define PROFILE_NATIVE_OPERATION(rc, op) rc->nativeOperations.pause(); op; rc->nativeOperations.start()
#else
	#define PROFILE_NATIVE_OPERATION(rc, op) op;
#endif

struct RenderingContext;


// JNI Helpers
void throwNewException(JNIEnv* env, const char* msg);
jclass findClass(JNIEnv* env, const char* className, bool mustHave = true);
std::string getString(JNIEnv* env, jstring st);
std::string getStringMethod(JNIEnv* env, jobject o, jmethodID fid, int i);
std::string getStringMethod(JNIEnv* env, jobject o, jmethodID fid);
std::string getStringField(JNIEnv* env, jobject o, jfieldID fid);
jobject newGlobalRef(JNIEnv* env, jobject o);
jfieldID getFid(JNIEnv* env, jclass cls, const char* fieldName, const char* sig);

void pullFromJavaRenderingContext(JNIEnv* env, jobject jrc, RenderingContext* rc);
void pushToJavaRenderingContext(JNIEnv* env, jobject jrc, RenderingContext* rc);

class ElapsedTimer
{
private:
	long elapsedTime;
	bool enableFlag;
	timespec startInit;
	timespec endInit;
	bool run;

public:
	ElapsedTimer();

	void enable();
	void disable();

	void start();
	void pause();

	int getElapsedTime();
};

struct TextDrawInfo {
	TextDrawInfo(std::string);
	~TextDrawInfo();

	std::string text;

	SkRect bounds;
	float centerX;
	float centerY;

	float textSize;
	float minDistance;
	int textColor;
	int textShadow;
	uint textWrap;
	bool bold;
	std::string shieldRes;
	int textOrder;

	bool drawOnPath;
	SkPath* path;
	float pathRotate;
	float vOffset;
	float hOffset;
};

struct IconDrawInfo
{
	IconDrawInfo();

	SkBitmap* bmp;
	float x;
	float y;
};

struct RenderingContext
{
	RenderingContext();
	~RenderingContext();
	bool interrupted();

	jobject javaRenderingContext;

	jobject androidContext;
	JNIEnv* env;
	bool useEnglishNames;

	std::vector<TextDrawInfo*> textToDraw;
	std::vector<IconDrawInfo> iconsToDraw;
	bool highResMode;
	float mapTextSize;
	float density;

	float leftX;
	float topY;
	int width;
	int height;

	int zoom;
	float rotate;
	float tileDivisor;

	// debug purpose
	int pointCount;
	int pointInsideCount;
	int visible;
	int allObjects;
	int lastRenderedKey;
	class ElapsedTimer textRendering;
	class ElapsedTimer nativeOperations;

	// use to calculate points
	float calcX;
	float calcY;

	float cosRotateTileSize;
	float sinRotateTileSize;

	int shadowRenderingMode;

	// not expect any shadow
	int shadowLevelMin;
	int shadowLevelMax;
};

float getDensityValue(RenderingContext* rc, float val);

SkBitmap* getCachedBitmap(RenderingContext* rc, const std::string& bitmapResource);

#endif /*_OSMAND_COMMON_H*/
