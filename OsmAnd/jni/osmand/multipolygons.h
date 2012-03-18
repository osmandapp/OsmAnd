
#include <android/log.h>
#include <stdio.h>
#include <map>
#include <set>
#include <hash_map>


#include "renderRules.h"
#include "common.h"
#include "mapObjects.h"

struct tagValueType {
	int type;
	std::string tag;
	std::string value;

	tagValueType(tag_value t, int type) : type(type) {
		tag = t.first;
		value = t.second;
	}

};
bool operator==(const tagValueType& __x, const tagValueType& __y) {
	return __x.type == __y.type;
}
bool operator<(const tagValueType& __x, const tagValueType& __y) {
	return __x.type < __y.type;
}
/// !!! Fuly copied from MapRenderRepositories.java, should be carefully synchroinized
bool isClockwiseWay(std::vector<int_pair>& c) ;
bool calculateLineCoordinates(bool inside, int x, int y, bool pinside, int px, int py, int leftX, int rightX,
		int bottomY, int topY, std::vector<int_pair>& coordinates);

void processMultipolygonLine(std::vector<std::vector<int_pair> >& completedRings, std::vector<std::vector<int_pair> >& incompletedRings,
			std::vector<std::string> &completedRingsNames, std::vector<std::string> &incompletedRingsNames, std::vector<int_pair> & coordinates, std::string name);

void unifyIncompletedRings(std::vector<std::vector<int_pair> >& incompletedRings, std::vector<std::vector<int_pair> >& completedRings, std::vector<std::string> &completedRingNames,
			std::vector<std::string> &incompletedRingNames, int leftX, int rightX, int bottomY, int topY, long dbId, int zoom);

MultiPolygonObject* processMultiPolygon(int leftX, int rightX, int bottomY, int topY,
		std::vector<std::vector<int_pair > >& completedRings, std::vector<std::vector<int_pair> >& incompletedRings,
		std::vector<std::string>& completedRingNames, std::vector<std::string>& incompletedRingNames,
		const tagValueType&  type, std::vector<MapDataObject* > & directList, std::vector<MapDataObject*>& inverselist,
		int zoom) {
	MultiPolygonObject* pl = new MultiPolygonObject();
	// delete direction last bit (to not show point)
	pl->tag = type.tag;
	pl->value = type.value;
	pl->layer = getNegativeWayLayer(type.type);
	long long dbId = 0;
	for (int km = 0; km < 2; km++) {
		std::vector<MapDataObject* >::iterator o = (km == 0 ? directList.begin() : inverselist.begin());
		std::vector<MapDataObject* >::iterator oEnd = (km == 0 ? directList.end() : inverselist.end());
		for (; o != oEnd; o++) {
			int len = (*o)->points.size();
			if (len < 2) {
				continue;
			}
			dbId = (*o)->id >> 1;
			std::vector<int_pair> coordinates;
			int_pair p = (*o)->points.at(km == 0 ? 0 : len - 1);
			int px = p.first;
			int py = p.second;
			int x = p.first;
			int y = p.second;
			bool pinside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
			if (pinside) {
				coordinates.push_back(int_pair(x, y));
			}
			for (int i = 1; i < len; i++) {
				int_pair cp = (*o)->points.at(km == 0 ? i : len - i - 1);
				x = cp.first;
				y = cp.second;
				bool inside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
				bool lineEnded = calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY,
						coordinates);
				if (lineEnded) {
					processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames,
							coordinates, (*o)->name);
					// create new line if it goes outside
					coordinates.clear();
				}
				px = x;
				py = y;
				pinside = inside;
			}
			processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames,
					coordinates, (*o)->name);
		}
	}
	if (completedRings.size() == 0 && incompletedRings.size() == 0) {
		return NULL;
	}
	if (incompletedRings.size() > 0) {
		unifyIncompletedRings(incompletedRings, completedRings, completedRingNames, incompletedRingNames, leftX, rightX,
				bottomY, topY, dbId, zoom);
	} else {
		// due to self intersection small objects (for low zooms check only coastline)
		if (zoom >= 13 || ("natural" == type.tag && "coastline" == type.value)) {
			bool clockwiseFound = false;
			std::vector<std::vector<int_pair> > ::iterator c = completedRings.begin();
			for (; c != completedRings.end(); c++) {
				if (isClockwiseWay(*c)) {
					clockwiseFound = true;
					break;
				}
			}
			if (!clockwiseFound) {
				// add whole bound
				std::vector<int_pair> whole;
				whole.push_back(int_pair(leftX, topY));
				whole.push_back(int_pair(rightX, topY));
				whole.push_back(int_pair(leftX, bottomY));
				whole.push_back(int_pair(rightX, bottomY));
				completedRings.push_back(whole);
				__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "!!! Isolated island !!!");
			}

		}
	}

	pl->names = completedRingNames;
	pl->points = completedRings;
	return pl;
}

static std::vector<MapDataObject*> EMPTY_LIST;
void proccessMultiPolygons(std::map<tagValueType, std::vector<MapDataObject*> >& multyPolygons, int leftX,
		int rightX, int bottomY, int topY, int zoom, std::vector<BaseMapDataObject*>& listPolygons) {
	std::vector<std::vector<int_pair> > completedRings;
	std::vector<std::vector<int_pair> > incompletedRings;
	std::vector<std::string> completedRingNames;
	std::vector<std::string> incompletedRingNames;
	std::map<tagValueType, std::vector<MapDataObject*> >::iterator val = multyPolygons.begin();
	for (; val != multyPolygons.end(); val++) {
		std::vector<MapDataObject*>* directList;
		std::vector<MapDataObject*>* inverselist;
		if (((val->first.type >> 15) & 1) == 1) {
			tagValueType directType = val->first;
			directType.type = val->first.type & ((1 << 15) - 1);
			if (multyPolygons.find(directType) == multyPolygons.end()) {
				inverselist = &val->second;
				directList = &EMPTY_LIST;
			} else {
				// continue on inner boundaries
				continue;
			}
		} else {
			tagValueType inverseType = val->first;
			inverseType.type = val->first.type | (1 << 15);
			directList = &val->second;
			inverselist = &multyPolygons[inverseType];
		}
		completedRings.clear();
		incompletedRings.clear();
		completedRingNames.clear();
		incompletedRingNames.clear();

		__android_log_print(ANDROID_LOG_INFO, LOG_TAG,  "Process multipolygon %s %s direct list %d rev %d", val->first.tag.c_str(), val->first.value.c_str(), directList->size(), inverselist->size());
		MultiPolygonObject* pl = processMultiPolygon(leftX, rightX, bottomY, topY, completedRings, incompletedRings,
				completedRingNames, incompletedRingNames, val->first, *directList, *inverselist, zoom);
		if (pl != NULL) {
			listPolygons.push_back(pl);
		} else {
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Multipolygon skipped");
		}
	}
}

// Copied from MapAlgorithms
int ray_intersect_x(int prevX, int prevY, int x, int y, int middleY) {
	// prev node above line
	// x,y node below line
	if (prevY > y) {
		int tx = prevX;
		int ty = prevY;
		x = prevX;
		y = prevY;
		prevX = tx;
		prevY = ty;
	}
	if (y == middleY || prevY == middleY) {
		middleY -= 1;
	}
	if (prevY > middleY || y < middleY) {
		return INT_MIN;
	} else {
		if (y == prevY) {
			// the node on the boundary !!!
			return x;
		}
		// that tested on all cases (left/right)
		double rx = x + ((double) middleY - y) * ((double) x - prevX) / (((double) y - prevY));
		return (int) rx;
	}
}

// Copied from MapAlgorithms
bool isClockwiseWay(std::vector<int_pair>& c) {
	if (c.size() == 0) {
		return true;
	}

	// calculate middle Y
	long middleY = 0;
	for (size_t i = 0; i < c.size(); i++) {
		middleY += c.at(i).second;
	}
	middleY /= (long) c.size();

	double clockwiseSum = 0;

	bool firstDirectionUp = false;
	int previousX = INT_MIN;
	int firstX = INT_MIN;

	int prevX = c.at(0).first;
	int prevY = c.at(0).second;

	for (size_t i = 1; i < c.size(); i++) {
		int x = c.at(i).first;
		int y = c.at(i).second;
		int rX = ray_intersect_x(prevX, prevY, x, y, (int) middleY);
		if (rX != INT_MIN) {
			bool skipSameSide = (y <= middleY) == (prevY <= middleY);
			if (skipSameSide) {
				continue;
			}
			bool directionUp = prevY >= middleY;
			if (firstX == INT_MIN) {
				firstDirectionUp = directionUp;
				firstX = rX;
			} else {
				bool clockwise = (!directionUp) == (previousX < rX);
				if (clockwise) {
					clockwiseSum += abs(previousX - rX);
				} else {
					clockwiseSum -= abs(previousX - rX);
				}
			}
			previousX = rX;
			prevX = x;
			prevY = y;
		}
	}

	if (firstX != INT_MIN) {
		bool clockwise = (!firstDirectionUp) == (previousX < firstX);
		if (clockwise) {
			clockwiseSum += abs(previousX - firstX);
		} else {
			clockwiseSum -= abs(previousX - firstX);
		}
	}

	return clockwiseSum >= 0;
}



void processMultipolygonLine(std::vector<std::vector<int_pair> >& completedRings, std::vector<std::vector<int_pair> >& incompletedRings,
			std::vector<std::string> &completedRingsNames,
		std::vector<std::string> &incompletedRingsNames, std::vector<int_pair> & coordinates, std::string name) {
	if (coordinates.size() > 0) {
		if (coordinates.at(0) == coordinates.at(coordinates.size() - 1)) {
			completedRings.push_back(coordinates);
			completedRingsNames.push_back(name);
		} else {
			bool add = true;
			for (size_t k = 0; k < incompletedRings.size();) {
				bool remove = false;
				std::vector<int_pair> i = incompletedRings.at(k);
				std::string oldName = incompletedRingsNames.at(k);
				if (coordinates.at(0) == i.at(i.size() - 1)) {
					std::vector<int_pair>::iterator tit =  coordinates.begin();
					i.insert(i.end(), ++tit, coordinates.end());
					remove = true;
					coordinates = i;
				} else if (coordinates.at(coordinates.size() - 1) == i.at(0)) {
					std::vector<int_pair>::iterator tit =  i.begin();
					coordinates.insert(coordinates.end(), ++tit, i.end());
					remove = true;
				}
				if (remove) {
					std::vector<std::vector<int_pair> >::iterator ti = incompletedRings.begin();
					ti += k;
					incompletedRings.erase(ti);
					std::vector<std::string> :: iterator tis = incompletedRingsNames.begin();
					tis += k;
					incompletedRingsNames.erase(tis);
				} else {
					k++;
				}
				if (coordinates.at(0) == coordinates.at(coordinates.size() - 1)) {
					completedRings.push_back(coordinates);
					if (oldName.length() > 0) {
						completedRingsNames.push_back(oldName);
					} else {
						completedRingsNames.push_back(name);
					}
					add = false;
					break;
				}
			}
			if (add) {
				incompletedRings.push_back(coordinates);
				incompletedRingsNames.push_back(name);
			}
		}
	}
}

int safelyAddDelta(int number, int delta) {
	int res = number + delta;
	if (delta > 0 && res < number) {
		return INT_MAX;
	} else if (delta < 0 && res > number) {
		return INT_MIN;
	}
	return res;
}

void unifyIncompletedRings(std::vector<std::vector<int_pair> >& incompletedRings, std::vector<std::vector<int_pair> >& completedRings,
		std::vector<std::string> &completedRingNames, std::vector<std::string> & incompletedRingsNames,
		int leftX, int rightX, int bottomY, int topY, long dbId, int zoom) {
	std::set<int> nonvisitedRings;
	std::vector<std::vector<int_pair> >::iterator ir = incompletedRings.begin();
	std::vector<std::string>::iterator irs = incompletedRingsNames.begin();
	int j = 0;
	for (j = 0; ir != incompletedRings.end(); ir++, irs++, j++) {
		int x = ir->at(0).first;
		int y = ir->at(0).second;
		int sx = ir->at(ir->size() - 1).first;
		int sy = ir->at(ir->size() - 1).second;
		bool st = y == topY || x == rightX || y == bottomY || x == leftX;
		bool end = sy == topY || sx == rightX || sy == bottomY || sx == leftX;
		// something goes wrong
		// These exceptions are used to check logic about processing multipolygons
		// However this situation could happen because of broken multipolygons (so it should data causes app error)
		// that's why these exceptions could be replaced with return; statement.
		if (!end || !st) {
			// TODO message
//				float dx = (float) MapUtils.get31LongitudeX(x);
//				float dsx = (float) MapUtils.get31LongitudeX(sx);
//				float dy = (float) MapUtils.get31LatitudeY(y);
//				float dsy = (float) MapUtils.get31LatitudeY(sy);
//				String str;
//				if (!end) {
//					str = " Start point (to close) not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
//					System.err
//							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
//				}
//				if (!st) {
//					str = " End not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
//					System.err
//							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
//				}
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error processing multipolygon");
		} else {
			nonvisitedRings.insert(j);
		}
	}
	ir = incompletedRings.begin();
	irs = incompletedRingsNames.begin();
	for (j = 0; ir != incompletedRings.end(); ir++, irs++, j++) {
		if (nonvisitedRings.find(j) == nonvisitedRings.end()) {
			continue;
		}
		int x = ir->at(ir->size() - 1).first;
		int y = ir->at(ir->size() - 1).second;
		// 31 - (zoom + 8)
		const int EVAL_DELTA = 6 << (23 - zoom);
		const int UNDEFINED_MIN_DIFF = -1 - EVAL_DELTA;
		while (true) {
			int st = 0; // st already checked to be one of the four
			if (y == topY) {
				st = 0;
			} else if (x == rightX) {
				st = 1;
			} else if (y == bottomY) {
				st = 2;
			} else if (x == leftX) {
				st = 3;
			}
			int nextRingIndex = -1;
			// BEGIN go clockwise around rectangle
			for (int h = st; h < st + 4; h++) {

				// BEGIN find closest nonvisited start (including current)
				int mindiff = UNDEFINED_MIN_DIFF;
				std::vector<std::vector<int_pair> >::iterator cni = incompletedRings.begin();
				int cnik = 0;
				for (;cni != incompletedRings.end(); cni++, cnik ++) {
					if (nonvisitedRings.find(cnik) == nonvisitedRings.end()) {
						continue;
					}
					int csx = cni->at(0).first;
					int csy = cni->at(0).second;
					if (h % 4 == 0) {
						// top
						if (csy == topY && csx >= safelyAddDelta(x, -EVAL_DELTA)) {
							if (mindiff == UNDEFINED_MIN_DIFF || (csx - x) <= mindiff) {
								mindiff = (csx - x);
								nextRingIndex = cnik;
							}
						}
					} else if (h % 4 == 1) {
						// right
						if (csx == rightX && csy >= safelyAddDelta(y, -EVAL_DELTA)) {
							if (mindiff == UNDEFINED_MIN_DIFF || (csy - y) <= mindiff) {
								mindiff = (csy - y);
								nextRingIndex = cnik;
							}
						}
					} else if (h % 4 == 2) {
						// bottom
						if (csy == bottomY && csx <= safelyAddDelta(x, EVAL_DELTA)) {
							if (mindiff == UNDEFINED_MIN_DIFF || (x - csx) <= mindiff) {
								mindiff = (x - csx);
								nextRingIndex = cnik;
							}
						}
					} else if (h % 4 == 3) {
						// left
						if (csx == leftX && csy <= safelyAddDelta(y, EVAL_DELTA)) {
							if (mindiff == UNDEFINED_MIN_DIFF || (y - csy) <= mindiff) {
								mindiff = (y - csy);
								nextRingIndex = cnik;
							}
						}
					}
				} // END find closest start (including current)

				// we found start point
				if (mindiff != UNDEFINED_MIN_DIFF) {
					break;
				} else {
					if (h % 4 == 0) {
						// top
						y = topY;
						x = rightX;
					} else if (h % 4 == 1) {
						// right
						y = bottomY;
						x = rightX;
					} else if (h % 4 == 2) {
						// bottom
						y = bottomY;
						x = leftX;
					} else if (h % 4 == 3) {
						y = topY;
						x = leftX;
					}
					ir->push_back(int_pair(x, y));
				}

			} // END go clockwise around rectangle
			if (nextRingIndex == -1) {
				// it is impossible (current start should always be found)
			} else if (nextRingIndex == j) {
				ir->push_back(ir->at(0));
				nonvisitedRings.erase(j);
				break;
			} else {
				std::vector<int_pair> p = incompletedRings.at(nextRingIndex);
				ir->insert(ir->end(), p.begin(), p.end());
				nonvisitedRings.erase(nextRingIndex);
				// get last point and start again going clockwise
				x = ir->at(ir->size() - 1).first;
				y = ir->at(ir->size() - 1).second;
			}
		}

		completedRings.push_back(*ir);
		completedRingNames.push_back(*irs);
	}

}



	/**
 * @return -1 if there is no instersection or x<<32 | y
 */
bool calculateIntersection(int x, int y, int px, int py, int leftX, int rightX, int bottomY, int topY, int_pair& b) {
	// firstly try to search if the line goes in
	if (py < topY && y >= topY) {
		int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
		if (leftX <= tx && tx <= rightX) {
			b.first = tx;
			b.second = topY;
			return true;
		}
	}
	if (py > bottomY && y <= bottomY) {
		int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
		if (leftX <= tx && tx <= rightX) {
			b.first = tx;
			b.second = bottomY;
			return true;
		}
	}
	if (px < leftX && x >= leftX) {
		int ty = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
		if (ty >= topY && ty <= bottomY) {
			b.first = leftX;
			b.second = ty;
			return true;
		}

	}
	if (px > rightX && x <= rightX) {
		int ty = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
		if (ty >= topY && ty <= bottomY) {
			b.first = rightX;
			b.second = ty;
			return true;
		}

	}

	// try to search if point goes out
	if (py > topY && y <= topY) {
		int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
		if (leftX <= tx && tx <= rightX) {
			b.first = tx;
			b.second = topY;
			return true;
		}
	}
	if (py < bottomY && y >= bottomY) {
		int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
		if (leftX <= tx && tx <= rightX) {
			b.first = tx;
			b.second = bottomY;
			return true;
		}
	}
	if (px > leftX && x <= leftX) {
		int ty = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
		if (ty >= topY && ty <= bottomY) {
			b.first = leftX;
			b.second = ty;
			return true;
		}

	}
	if (px < rightX && x >= rightX) {
		int ty = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
		if (ty >= topY && ty <= bottomY) {
			b.first = rightX;
			b.second = ty;
			return true;
		}

	}

	if (px == rightX || px == leftX || py == topY || py == bottomY) {
		b.first = px;
		b.second = py;
//		return true;
		// Is it right? to not return anything?
	}
	return false;
}

bool calculateLineCoordinates(bool inside, int x, int y, bool pinside, int px, int py, int leftX, int rightX,
		int bottomY, int topY, std::vector<int_pair>& coordinates) {
	bool lineEnded = false;
	int_pair b(x, y);
	if (pinside) {
		if (!inside) {
			bool is = calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY, b);
			if (!is) {
				b.first = px;
				b.second = py;
			}
			coordinates.push_back(b);
			lineEnded = true;
		} else {
			coordinates.push_back(b);
		}
	} else {
		bool is = calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY, b);
		if (inside) {
			// assert is != -1;
			coordinates.push_back(b);
			int_pair n(x, y);
			coordinates.push_back(n);
		} else if (is) {
			coordinates.push_back(b);
			calculateIntersection(x, y, b.first, b.second, leftX, rightX, bottomY, topY, b);
			coordinates.push_back(b);
			lineEnded = true;
		}
	}

	return lineEnded;
}
