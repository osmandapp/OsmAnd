#include "common.h"

#include <SkPath.h>
#include <SkBitmap.h>
#include <SkImageDecoder.h>

#include "osmand_log.h"

#if defined(_WIN32)
#	include <windows.h>
#	include <mmsystem.h>
#elif defined(__APPLE__)
#	include <mach/mach_time.h>|
#else
#	include <time.h>
#endif

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

ElapsedTimer::ElapsedTimer()
	: elapsedTime(0)
	, enableFlag(true)
	, run(false)
{
#if defined(__APPLE__)
	mach_timebase_info(&machTimeInfo);
#endif
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
	{
#if defined(_WIN32)
		startInit = timeGetTime();
#elif defined(__APPLE__)
		startInit = mach_absolute_time();
#else
		clock_gettime(CLOCK_MONOTONIC, &startInit);
#endif
	}
	run = true;
}

void ElapsedTimer::pause()
{
	if (!run)
		return;
#if defined(_WIN32)
	endInit = timeGetTime();
	elapsedTime += (endInit - startInit) * 1e6;
#elif defined(__APPLE__)
	endInit = mach_absolute_time();
	uint64_t duration = endInit - startInit;
	duration *= machTimeInfo.numer;
	duration /= machTimeInfo.denom;
	elapsedTime += duration;
#else
	clock_gettime(CLOCK_MONOTONIC, &endInit);
	int sec = endInit.tv_sec - startInit.tv_sec;
	if (sec > 0)
		elapsedTime += 1e9 * sec;
	elapsedTime += endInit.tv_nsec - startInit.tv_nsec;
#endif
	run = false;
}

int ElapsedTimer::getElapsedTime()
{
	pause();
	return elapsedTime / 1e6;
}

SkBitmap* RenderingContext::getCachedBitmap(const std::string& bitmapResource) {
	if (defaultIconsDir.size() > 0) {
		string fl = string(defaultIconsDir + "h_" + bitmapResource + ".png");
		FILE* f = fopen(fl.c_str(), "r");
		if (f == NULL) {
			fl = string(defaultIconsDir + "g_" + bitmapResource + ".png");
			f = fopen(fl.c_str(), "r");
		}
		if (f != NULL) {
			fclose(f);
			osmand_log_print(LOG_INFO, "Open file %s", fl.c_str());
			SkBitmap* bmp = new SkBitmap();
			if (!SkImageDecoder::DecodeFile(fl.c_str(), bmp)) {
				return NULL;
			}
			return bmp;
		}
	}
	return NULL;
}


UNORDERED(map)<std::string, SkBitmap*> cachedBitmaps;
SkBitmap* getCachedBitmap(RenderingContext* rc, const std::string& bitmapResource)
{

	if(bitmapResource.size() == 0)
		return NULL;

	// Try to find previously cached
	UNORDERED(map)<std::string, SkBitmap*>::iterator itPreviouslyCachedBitmap = cachedBitmaps.find(bitmapResource);
	if (itPreviouslyCachedBitmap != cachedBitmaps.end())
		return itPreviouslyCachedBitmap->second;
	
	rc->nativeOperations.pause();
	SkBitmap* iconBitmap = rc->getCachedBitmap(bitmapResource);
	cachedBitmaps[bitmapResource] = iconBitmap;
	rc->nativeOperations.start();

	return iconBitmap;
}

void purgeCachedBitmaps() {
	UNORDERED(map)<std::string, SkBitmap*>::iterator it = cachedBitmaps.begin();
	for (; it != cachedBitmaps.end(); it++) {
		delete it->second;
	}
}

std::string RenderingContext::getTranslatedString(const std::string& src) {
	return src;
}

std::string RenderingContext::getReshapedString(const std::string& src) {
	return src;
}


double getPowZoom(float zoom){
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


int get31TileNumberX(double longitude){
	longitude = checkLongitude(longitude);
	long long int l =  1;
	l <<= 31;
	return (int)((longitude + 180)/360 * l);
}

int get31TileNumberY(double latitude) {
	latitude = checkLatitude(latitude);
	double eval = log(tan(toRadians(latitude)) + 1 / cos(toRadians(latitude)));
	long long int l =  1;
	l <<= 31;
	if (eval > M_PI) {
		eval = M_PI;
	}
	return (int) ((1 - eval / M_PI) / 2 * l);
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


double getTileNumberX(float zoom, double longitude) {
	if (longitude == 180.) {
		return getPowZoom(zoom) - 1;
	}
	longitude = checkLongitude(longitude);
	return (longitude + 180.) / 360. * getPowZoom(zoom);
}


double getTileNumberY(float zoom, double latitude) {
	latitude = checkLatitude(latitude);
	double eval = log(tan(toRadians(latitude)) + 1 / cos(toRadians(latitude)));
	if (isinf(eval) || isnan(eval)) {
		latitude = latitude < 0 ? -89.9 : 89.9;
		eval = log(tan(toRadians(latitude)) + 1 / cos(toRadians(latitude)));
	}
	double result = (1 - eval / M_PI) / 2 * getPowZoom(zoom);
	return result;
}


