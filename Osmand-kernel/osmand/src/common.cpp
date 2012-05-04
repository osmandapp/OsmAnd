#include <string>
#include <vector>
#include <SkPath.h>
#include <SkBitmap.h>
#include <jni.h>
#include <time.h>
#include <math.h>


#include "common.h"
#include "osmand_log.h"



TextDrawInfo::TextDrawInfo(std::string itext)
	: text(itext)
	, drawOnPath(false)
	, path(NULL)
	, pathRotate(0)
{
}

TextDrawInfo::~TextDrawInfo()
{
	if (path)
		delete path;
}

IconDrawInfo::IconDrawInfo()
	: bmp(NULL)
{
}

RenderingContext::RenderingContext()
{
}

RenderingContext::~RenderingContext()
{
	std::vector<TextDrawInfo*>::iterator itTextToDraw;
	for(itTextToDraw = textToDraw.begin(); itTextToDraw != textToDraw.end(); ++itTextToDraw)
		delete (*itTextToDraw);
}

bool RenderingContext::interrupted()
{
	return false;
}



float getDensityValue(RenderingContext* rc, float val)
{
	if (rc->highResMode && rc->density > 1)
		return val * rc->density * rc->mapTextSize;
	else
		return val * rc->mapTextSize;
}

ElapsedTimer::ElapsedTimer()
	: elapsedTime(0)
	, enableFlag(true)
	, run(false)
{
}

void ElapsedTimer::enable()
{
	enableFlag = true;
}

void ElapsedTimer::disable()
{
	pause();
	enableFlag = false;
}

void ElapsedTimer::start()
{
	if (!enableFlag)
		return;
	if (!run)
		clock_gettime(CLOCK_MONOTONIC, &startInit);
	run = true;
}

void ElapsedTimer::pause()
{
	if (!run)
		return;

	clock_gettime(CLOCK_MONOTONIC, &endInit);
	int sec = endInit.tv_sec - startInit.tv_sec;
	if (sec > 0)
		elapsedTime += 1e9 * sec;
	elapsedTime += endInit.tv_nsec - startInit.tv_nsec;
	run = false;
}

int ElapsedTimer::getElapsedTime()
{
	pause();
	return elapsedTime / 1e6;
}

SkBitmap* RenderingContext::getCachedBitmap(const std::string& bitmapResource) {
	return NULL;
}


HMAP::hash_map<std::string, SkBitmap*> cachedBitmaps;
SkBitmap* getCachedBitmap(RenderingContext* rc, const std::string& bitmapResource)
{

	if(bitmapResource.size() == 0)
		return NULL;

	// Try to find previously cached
	HMAP::hash_map<std::string, SkBitmap*>::iterator itPreviouslyCachedBitmap = cachedBitmaps.find(bitmapResource);
	if (itPreviouslyCachedBitmap != cachedBitmaps.end())
		return itPreviouslyCachedBitmap->second;
	
	rc->nativeOperations.pause();
	SkBitmap* iconBitmap = rc->getCachedBitmap(bitmapResource);
	cachedBitmaps[bitmapResource] = iconBitmap;
	rc->nativeOperations.start();

	return iconBitmap;
}

std::string RenderingContext::getTranslatedString(const std::string& src) {
	return src;
}


inline double getPowZoom(float zoom){
	if(zoom >= 0 && zoom - floor(zoom) < 0.05f){
		return 1 << ((int)zoom);
	} else {
		return pow(2, zoom);
	}
}


double checkLongitude(double longitude) {
	while (longitude < -180 || longitude > 180) {
		if (longitude < 0) {
			longitude += 360;
		} else {
			longitude -= 360;
		}
	}
	return longitude;
}

double checkLatitude(double latitude) {
	while (latitude < -90 || latitude > 90) {
		if (latitude < 0) {
			latitude += 180;
		} else {
			latitude -= 180;
		}
	}
	if (latitude < -85.0511) {
		return -85.0511;
	} else if (latitude > 85.0511) {
		return 85.0511;
	}
	return latitude;
}

inline double toRadians(double angdeg) {
        return angdeg / 180 * M_PI;
}

int get31TileNumberX(double longitude){
	longitude = checkLongitude(longitude);
	long l = 1l << 31;
	return (int)((longitude + 180)/360 * l);
}

int get31TileNumberY( double latitude){
	latitude = checkLatitude(latitude);
	double eval = log(tan(toRadians(latitude)) + 1/cos(toRadians(latitude)) );
		long l = 1l << 31;
		if(eval > M_PI){
			eval = M_PI;
		}
		return  (int) ((1 - eval / M_PI) / 2 * l);
}

double getLongitudeFromTile(float zoom, double x) {
		return x / getPowZoom(zoom) * 360.0 - 180.0;
}


double getLatitudeFromTile(float zoom, double y){
	int sign = y < 0 ? -1 : 1;
	double result = atan(sign*sinh(M_PI * (1 - 2 * y / getPowZoom(zoom)))) * 180. / M_PI;
	return result;
}

double get31LongitudeX(int tileX){
	return getLongitudeFromTile(21, tileX /1024.);
}

double get31LatitudeY(int tileY){
	return getLatitudeFromTile(21, tileY /1024.);

}


