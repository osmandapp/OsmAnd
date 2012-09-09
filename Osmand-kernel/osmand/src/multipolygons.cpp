#ifndef _MULTIPOLYGONS_CPP
#define _MULTIPOLYGONS_CPP

#include "multipolygons.h"
#include "osmand_log.h"

// returns true if coastlines were added!
bool processCoastlines(std::vector<MapDataObject*>&  coastLines, int leftX, int rightX, int bottomY, int topY, int zoom,
		bool showIfThereIncompleted, bool addDebugIncompleted, std::vector<MapDataObject*>& res) {
	std::vector<coordinates> completedRings;
	std::vector<coordinates > uncompletedRings;
	std::vector<MapDataObject*>::iterator val = coastLines.begin();
	long long dbId = 0;
	for (; val != coastLines.end(); val++) {
		MapDataObject* o = *val;
		int len = o->points.size();
		if (len < 2) {
			continue;
		}
		dbId = o->id >> 1;
		coordinates* cs = new coordinates();
		int px = o->points.at(0).first;
		int py = o->points.at(0).second;
		int x = px;
		int y = py;
		bool pinside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
		if (pinside) {
			cs->push_back(int_pair(x, y));
		}
		for (int i = 1; i < len; i++) {
			x = o->points.at(i).first;
			y = o->points.at(i).second;
			bool inside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
			bool lineEnded = calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY, *cs);
			if (lineEnded) {
				combineMultipolygonLine(completedRings, uncompletedRings, *cs);
				// create new line if it goes outside
				cs = new coordinates();
			}
			px = x;
			py = y;
			pinside = inside;
		}
		combineMultipolygonLine(completedRings, uncompletedRings, *cs);
	}
	if (completedRings.size() == 0 && uncompletedRings.size() == 0) {
		return false;
	}
	if (uncompletedRings.size() > 0) {
		unifyIncompletedRings(uncompletedRings, completedRings, leftX, rightX, bottomY, topY, dbId, zoom);
	}
	if (addDebugIncompleted) {
		// draw uncompleted for debug purpose
		for (int i = 0; i < uncompletedRings.size(); i++) {
			MapDataObject* o = new MapDataObject();
			o->points = uncompletedRings[i];
			o->types.push_back(tag_value("natural", "coastline_broken"));
			res.push_back(o);
		}
		// draw completed for debug purpose
		for (int i = 0; i < completedRings.size(); i++) {
			MapDataObject* o = new MapDataObject();
			o->points = completedRings[i];
			o->types.push_back(tag_value("natural", "coastline_line"));
			res.push_back(o);
		}

	}
	if (!showIfThereIncompleted && uncompletedRings.size() > 0) {
		return false;
	}
	bool clockwiseFound = false;
	for (int i = 0; i < completedRings.size(); i++) {
		bool clockwise = isClockwiseWay(completedRings[i]);
		clockwiseFound = clockwiseFound || clockwise;
		MapDataObject* o = new MapDataObject();
		o->points = completedRings[i];
		if (clockwise) {
			o->types.push_back(tag_value("natural", "coastline"));
		} else {
			o->types.push_back(tag_value("natural", "land"));
		}
		o->id = dbId;
		o->area = true;
		res.push_back(o);
	}

	if (!clockwiseFound && uncompletedRings.size() == 0) {
		// add complete water tile
		MapDataObject* o = new MapDataObject();
		o->points.push_back(int_pair(leftX, topY));
		o->points.push_back(int_pair(rightX, topY));
		o->points.push_back(int_pair(rightX, bottomY));
		o->points.push_back(int_pair(leftX, bottomY));
		o->points.push_back(int_pair(leftX, topY));
		o->id = dbId;
		o->types.push_back(tag_value("natural", "coastline"));
		osmand_log_print(LOG_ERROR,  "!!! Isolated islands !!!");
		res.push_back(o);

	}
	return true;
}

// Copied from MapAlgorithms
int ray_intersect_x(int prevX, int prevY, int x, int y, int middleY) {
	// prev node above line
	// x,y node below line
	if (prevY > y) {
		int tx = x;
		int ty = y;
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
	long long middleY = 0;
	for (size_t i = 0; i < c.size(); i++) {
		middleY += c.at(i).second;
	}
	middleY /= (long long) c.size();

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
		}
		prevX = x;
		prevY = y;
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



void combineMultipolygonLine(std::vector<coordinates>& completedRings, std::vector<coordinates>& incompletedRings,
			coordinates& coordinates) {
	if (coordinates.size() > 0) {
		if (coordinates.at(0) == coordinates.at(coordinates.size() - 1)) {
			completedRings.push_back(coordinates);
		} else {
			bool add = true;
			for (size_t k = 0; k < incompletedRings.size();) {
				bool remove = false;
				std::vector<int_pair> i = incompletedRings.at(k);
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
				} else {
					k++;
				}
				if (coordinates.at(0) == coordinates.at(coordinates.size() - 1)) {
					completedRings.push_back(coordinates);
					add = false;
					break;
				}
			}
			if (add) {
				incompletedRings.push_back(coordinates);
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

void unifyIncompletedRings(std::vector<std::vector<int_pair> >& toProccess, std::vector<std::vector<int_pair> >& completedRings,
		int leftX, int rightX, int bottomY, int topY, long dbId, int zoom) {
	std::set<int> nonvisitedRings;
	std::vector<coordinates > incompletedRings(toProccess);
	toProccess.clear();
	std::vector<coordinates >::iterator ir = incompletedRings.begin();
	int j = 0;
	for (j = 0; ir != incompletedRings.end(); ir++,j++) {
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
			osmand_log_print(LOG_ERROR, "Error processing multipolygon");
			toProccess.push_back(*ir);
		} else {
			nonvisitedRings.insert(j);
		}
	}
	ir = incompletedRings.begin();
	for (j = 0; ir != incompletedRings.end(); ir++, j++) {
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

#endif
