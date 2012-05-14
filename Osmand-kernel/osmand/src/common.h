#ifndef _OSMAND_COMMON_H
#define _OSMAND_COMMON_H

#include <string>
#include <vector>

#include <hash_map>
#include <hash_set>
#include <SkPath.h>
#include <SkBitmap.h>
#include "osmand_log.h"


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

  	  template<>
      struct hash<void*>
      {
        size_t
        operator()(void* __x) const
      { return (size_t) __x; }
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

inline double toRadians(double angdeg) {
	return angdeg / 180 * M_PI;
}

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

static const int TILE_SIZE = 256;
struct RenderingContext
{
private :
	// parameters
	bool useEnglishNames;
	float density;

	float leftX;
	float topY;
	int width;
	int height;
	int defaultColor;

	int zoom;
	float rotate;
	// int shadowRenderingMode = 0; // no shadow (minumum CPU)
	// int shadowRenderingMode = 1; // classic shadow (the implementaton in master)
	// int shadowRenderingMode = 2; // blur shadow (most CPU, but still reasonable)
	// int shadowRenderingMode = 3; solid border (CPU use like classic version or even smaller)
	int shadowRenderingMode;
	string defaultIconsDir;

public:
	// debug purpose
	int pointCount;
	int pointInsideCount;
	int visible;
	int allObjects;
	int lastRenderedKey;
	class ElapsedTimer textRendering;
	class ElapsedTimer nativeOperations;

// because they used in 3rd party functions
public :

	// calculated
	float tileDivisor;
	float cosRotateTileSize;
	float sinRotateTileSize;

	std::vector<TextDrawInfo*> textToDraw;
	std::vector<IconDrawInfo> iconsToDraw;
	// use to calculate points
	float calcX;
	float calcY;

	// not expect any shadow
	int shadowLevelMin;
	int shadowLevelMax;

public:
	RenderingContext() : shadowLevelMax(0), shadowLevelMin(256), density(true), useEnglishNames(false), pointCount(0),
		pointInsideCount(0), visible(0), allObjects(0){
		setRotate(0);
		setZoom(15);
		setDefaultColor(0xfff1eee8);
	}
	virtual ~RenderingContext();

	virtual bool interrupted();
	virtual SkBitmap* getCachedBitmap(const std::string& bitmapResource);
	virtual std::string getTranslatedString(const std::string& src);

	void setDefaultIconsDir(string path) {
		defaultIconsDir = path;
	}

	void setZoom(int z) {
		this->zoom = z;
		this->tileDivisor = (1 << (31 - z));
	}

	void setDefaultColor(int z) {
		this->defaultColor = z;
	}

	void setRotate(float rot) {
		this->rotate = rot;
		this->cosRotateTileSize = cos(toRadians(rot)) * TILE_SIZE;
		this->sinRotateTileSize = sin(toRadians(rot)) * TILE_SIZE;
	}

	void setLocation(double leftX, double topY) {
		this->leftX = leftX;
		this->topY = topY;
	}

	void setDimension(int width, int height) {
		this->width = width;
		this->height = height;
	}

	inline int getShadowRenderingMode(){
		return shadowRenderingMode;
	}

	inline int getWidth(){
		return width;
	}

	inline int getDefaultColor(){
		return defaultColor;
	}

	inline int getHeight(){
		return height;
	}

	inline int getZoom() {
		return zoom;
	}

	inline float getLeft() {
		return leftX;
	}

	inline float getTop() {
		return topY;
	}

	void setShadowRenderingMode(int mode){
		this->shadowRenderingMode = mode;
	}

	void setDensityScale(float val) {
		density = val;
	}

	float getDensityValue(float val) {
		return val * density;
	}

	void setUseEnglishNames(bool b){
		this->useEnglishNames = b;
	}

	bool isUsingEnglishNames(){
		return this->useEnglishNames;
	}

};

SkBitmap* getCachedBitmap(RenderingContext* rc, const std::string& bitmapResource);
void purgeCachedBitmaps();

int get31TileNumberX(double longitude);
int get31TileNumberY( double latitude);

double getLongitudeFromTile(float zoom, double x) ;
double getLatitudeFromTile(float zoom, double y);

double get31LongitudeX(int tileX);
double get31LatitudeY(int tileY);
double getTileNumberX(float zoom, double longitude);
double getTileNumberY(float zoom, double latitude);

double getPowZoom(float zoom);

#endif /*_OSMAND_COMMON_H*/
