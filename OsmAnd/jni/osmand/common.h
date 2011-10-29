#ifndef _OSMAND_COMMON_H
#define _OSMAND_COMMON_H

#include <jni.h>
#include <string>
#include <vector>
#include <hash_map>
#include <SkPath.h>
#include <SkBitmap.h>

// Constants
#define DEBUG_NAT_OPERATIONS
#ifdef DEBUG_NAT_OPERATIONS
	#define NAT_COUNT(rc, op) rc->nativeOperations.pause(); op; rc->nativeOperations.start()
#else
	#define NAT_COUNT(rc, op) op;
#endif

const std::string EMPTY_STRING;
const int WHITE_COLOR = -1;
const int BLACK_COLOR = 0xff000000;

JNIEnv* globalEnv();

JNIEnv* setGlobalEnv(JNIEnv* e);

// JNI Helpers
//extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);

std::string getString(jstring st);
std::string getStringMethod(jobject o, jmethodID fid, int i);
std::string getStringMethod(jobject o, jmethodID fid);
std::string getStringField(jobject o, jfieldID fid);
jclass globalRef(jobject o);
jfieldID getFid(jclass cls,const char* fieldName, const char* sig );


class watcher {

private:
	long elapsedTime;
	bool enableFlag;
	//	timeval startInit;
	//	timeval endInit;
	timespec startInit;
	timespec endInit;
	bool run;

public:
	watcher();

	void enable();

	void disable();

	void start();

	void pause();

	int getElapsedTime();
};

// Rendering context methods
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

extern jfieldID RenderingContext_interrupted;
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
	int lastRenderedKey;
	watcher textRendering;
	watcher nativeOperations;


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
		return globalEnv()->GetBooleanField(originalRC, RenderingContext_interrupted);
	}
	~RenderingContext() {
		for (uint i = 0; i < textToDraw.size(); i++) {
			delete textToDraw.at(i);
		}
	}
};
void copyRenderingContext(jobject orc, RenderingContext* rc);
void mergeRenderingContext(jobject orc, RenderingContext* rc);

float getDensityValue(RenderingContext* rc, float val);

SkBitmap* getCachedBitmap(RenderingContext* rc, std::string js);
SkBitmap* getNativeBitmap(jobject bmpObj);


#endif /*_OSMAND_COMMON_H*/
