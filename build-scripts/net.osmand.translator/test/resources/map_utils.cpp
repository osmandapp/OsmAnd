#include <math.h>
#include <string>

const double PI = 3.141592653589793238462;

class MapUtils {   
 	
	private: 
		static const wchar_t intToBase64[64]= {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','_','@'};
		static const string BASE_SHORT_OSM_URL = "http://osm.org/go/";

	double toRadians(double a) {
		return  a / 180.0 * PI;
	}
	
	double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double R = 6371;
		double dLat = toRadians(lat2-lat1);
		double dLon = toRadians(lon2-lon1); 
		double a = sin(dLat/2) * sin(dLat/2) +
		       cos(toRadians(lat1)) * cos(toRadians(lat2)) * 
		       sin(dLon/2) * sin(dLon/2); 
		double c = 2 * atan2(sqrt(a), sqrt(1-a)); 
		return R * c * 1000;
	}
}