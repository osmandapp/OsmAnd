#ifndef _OSMAND_COMMON
#define _OSMAND_COMMON

#include <jni.h>
#include <string>
#include <vector>
#include <hash_map>
#include <SkPath.h>
#include <SkBitmap.h>


JNIEnv* env;
const std::string EMPTY_STRING;
const int WHITE_COLOR = -1;
const int BLACK_COLOR = 0xff000000;

jclass RenderingContextClass;
jfieldID RenderingContext_interrupted;

jclass RenderingIconsClass;
jmethodID RenderingIcons_getIcon;

jclass globalRef(jobject o)
{
	return  (jclass) env->NewGlobalRef( o);
}

class TextDrawInfo {
public :
	std::string text;

	TextDrawInfo(std::string itext)  {
		text = itext;
		drawOnPath = false;
		path = NULL;
		pathRotate = 0;
	}
	SkRect bounds;
	float centerX;
	float centerY;

	float textSize ;
	float minDistance ;
	int textColor;
	int textShadow ;
	uint textWrap ;
	bool bold ;
	std::string shieldRes;
	int textOrder;

	bool drawOnPath;
	SkPath* path;
	float pathRotate;
	float vOffset ;
	float hOffset ;

	~TextDrawInfo() {
		if (path != NULL) {
			delete path;
		}
	}
};

struct IconDrawInfo {
	SkBitmap* bmp;
	float x;
	float y;
};

jfieldID getFid(jclass cls,const char* fieldName, const char* sig )
{
	return env->GetFieldID( cls, fieldName, sig);
}

class timer {
	int elapsedTime;
	timeval startInit;
	timeval endInit;
	bool run;
public:
	timer() {
		elapsedTime = 0;
	}
	void start() {
		if (!run) {
			gettimeofday(&startInit, NULL);
		}
		run = true;
	}
	void pause() {
		if (run) {
			gettimeofday(&endInit, NULL);
			elapsedTime += (endInit.tv_sec * 1000 + endInit.tv_usec / 1000)
							- (startInit.tv_sec * 1000 + startInit.tv_usec / 1000);
		}
		run = false;
	}
	int getElapsedTime() {
		pause();
		return elapsedTime;
	}
};


struct RenderingContext {
	jobject originalRC;
	jobject androidContext;

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
	timer textRendering;
	timer nativeOperations;

	// use to calculate points
	float calcX;
	float calcY;

	float cosRotateTileSize;
	float sinRotateTileSize;

	int shadowRenderingMode;

	// not expect any shadow
	int shadowLevelMin;
	int shadowLevelMax;

	bool interrupted() {
		return env->GetBooleanField(originalRC, RenderingContext_interrupted);
	}
	~RenderingContext() {
		for (uint i = 0; i < textToDraw.size(); i++) {
			delete textToDraw.at(i);
		}
	}
};




std::string getStringField(jobject o, jfieldID fid)
{
	jstring st = (jstring) env->GetObjectField(o, fid);
	if(st == NULL)
	{
		return std::string();
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

std::string getStringMethod(jobject o, jmethodID fid)
{
	jstring st = (jstring) env->CallObjectMethod(o, fid);
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

std::string getStringMethod(jobject o, jmethodID fid, int i)
{
	jstring st = (jstring) env->CallObjectMethod(o, fid, i);
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

float getDensityValue(RenderingContext* rc, float val) {
	if (rc -> highResMode && rc -> density > 1) {
		return val * rc -> density * rc -> mapTextSize;
	} else {
		return val * rc -> mapTextSize;
	}
}

SkBitmap* getNativeBitmap(jobject bmpObj){
	if(bmpObj == NULL){
		return NULL;
	}
	jclass bmpClass = env->GetObjectClass(bmpObj);
	SkBitmap* bmp = (SkBitmap*)env->CallIntMethod(bmpObj, env->GetMethodID(bmpClass, "ni", "()I"));
	env->DeleteLocalRef(bmpClass);
	return bmp;
}

std::hash_map<std::string, SkBitmap*> cachedBitmaps;
SkBitmap* getCachedBitmap(RenderingContext* rc, std::string js)
{
	if (cachedBitmaps.find(js) != cachedBitmaps.end()) {
		return cachedBitmaps[js];
	}
	rc->nativeOperations.pause();
	jstring jstr = env->NewStringUTF(js.c_str());
	jobject bmp = env->CallStaticObjectMethod(RenderingIconsClass, RenderingIcons_getIcon, rc->androidContext, jstr);
	SkBitmap* res = getNativeBitmap(bmp);
	rc->nativeOperations.start();

	env->DeleteLocalRef(bmp);
	env->DeleteLocalRef(jstr);
	if(res != NULL){
		res = new SkBitmap(*res);
	}
	cachedBitmaps[js] = res;

	return res;
}

void loadJniCommon(jobject rc) {

	RenderingContextClass = globalRef(env->GetObjectClass(rc));
	RenderingContext_interrupted = getFid(RenderingContextClass, "interrupted", "Z");

	RenderingIconsClass = globalRef(env->FindClass("net/osmand/plus/render/RenderingIcons"));
	RenderingIcons_getIcon = env->GetStaticMethodID(RenderingIconsClass, "getIcon",
			"(Landroid/content/Context;Ljava/lang/String;)Landroid/graphics/Bitmap;");

}

void unloadJniCommon() {
	env->DeleteGlobalRef(RenderingContextClass);
	env->DeleteGlobalRef(RenderingIconsClass);
}



#endif
