#include <vector>
#include <set>
#include <algorithm>
#include <hash_map>
#include <time.h>
#include <jni.h>
#include "SkTypes.h"
#include "SkTypeface.h"
#include "SkCanvas.h"
#include "SkPaint.h"
#include "SkPath.h"

#include "common.h"
#include "renderRules.h"

const char REF_CHAR = ((char)0x0019);
const char DELIM_CHAR = ((char)0x0018);

template <typename T> class quad_tree {
private :
	struct node {
        typedef std::vector<T> cont_t;
        cont_t data;
		node* children[4];
		SkRect bounds;

		node(SkRect& b) : bounds(b) {
            std::memset(children,0,4*sizeof(node*));
		}

		~node() {
			for (int i = 0; i < 4; i++) {
				if (children[i] != NULL) {
					delete children[i];
				}
			}
		}
	};
	typedef typename node::cont_t cont_t;
	typedef typename cont_t::iterator node_data_iterator;
	double ratio;
	unsigned int max_depth;
	node root;
public:
	quad_tree(SkRect& r, int depth=8, double ratio = 0.55) : ratio(ratio), max_depth(depth), root(r) {
	}

    void insert(T data, SkRect& box)
    {
        unsigned int depth=0;
        do_insert_data(data, box, &root, depth);
    }

    void query_in_box(SkRect& box, std::vector<T>& result)
    {
        result.clear();
        query_node(box, result, &root);
    }

private:

    void query_node(SkRect& box, std::vector<T> & result, node* node) const {
		if (node) {
			if (SkRect::Intersects(box, node->bounds)) {
				node_data_iterator i = node->data.begin();
				node_data_iterator end = node->data.end();
				while (i != end) {
					result.push_back(*i);
					++i;
				}
				for (int k = 0; k < 4; ++k) {
					query_node(box, result, node->children[k]);
				}
			}
		}
	}


    void do_insert_data(T data, SkRect& box, node * n, unsigned int& depth)
    {
        if (++depth >= max_depth) {
			n->data.push_back(data);
		} else {
			SkRect& node_extent = n->bounds;
			SkRect ext[4];
			split_box(node_extent, ext);
			for (int i = 0; i < 4; ++i) {
				if (ext[i].contains(box)) {
					if (!n->children[i]) {
						n->children[i] = new node(ext[i]);
					}
					do_insert_data(data, box, n->children[i], depth);
					return;
				}
			}
			n->data.push_back(data);
		}
    }
    void split_box(SkRect& node_extent,SkRect * ext)
    {
        //coord2d c=node_extent.center();

    	float width=node_extent.width();
    	float height=node_extent.height();

        float lox=node_extent.fLeft;
        float loy=node_extent.fTop;
        float hix=node_extent.fRight;
        float hiy=node_extent.fBottom;

        ext[0]=SkRect::MakeLTRB(lox,loy,lox + width * ratio,loy + height * ratio);
        ext[1]=SkRect::MakeLTRB(hix - width * ratio,loy,hix,loy + height * ratio);
        ext[2]=SkRect::MakeLTRB(lox,hiy - height*ratio,lox + width * ratio,hiy);
        ext[3]=SkRect::MakeLTRB(hix - width * ratio,hiy - height*ratio,hix,hiy);
    }
};



void fillTextProperties(TextDrawInfo* info, RenderingRuleSearchRequest* render, float cx, float cy) {
	info->centerX = cx;
	info->centerY = cy + render->getIntPropertyValue(render->props()->R_TEXT_DY, 0);
	// used only for draw on path where centerY doesn't play role
	info->vOffset = render->getIntPropertyValue(render->props()->R_TEXT_DY, 0);
	info->textColor = render->getIntPropertyValue(render->props()->R_TEXT_COLOR);
	if (info->textColor == 0) {
		info->textColor = 0xff000000;
	}
	info->textSize = render->getIntPropertyValue(render->props()->R_TEXT_SIZE);
	info->textShadow = render->getIntPropertyValue(render->props()->R_TEXT_HALO_RADIUS, 0);
	info->textWrap = render->getIntPropertyValue(render->props()->R_TEXT_WRAP_WIDTH, 0);
	info->bold = render->getIntPropertyValue(render->props()->R_TEXT_BOLD, 0) > 0;
	info->minDistance = render->getIntPropertyValue(render->props()->R_TEXT_MIN_DISTANCE, 0);
	info->shieldRes = render->getStringPropertyValue(render->props()->R_TEXT_SHIELD);
	info->textOrder = render->getIntPropertyValue(render->props()->R_TEXT_ORDER, 100);
}

bool isLetterOrDigit(char c)
{
	return c != ' ';
}

void drawTextOnCanvas(SkCanvas* cv, std::string text, float centerX, float centerY, SkPaint& paintText,
		float textShadow) {
	if (textShadow > 0) {
		int c = paintText.getColor();
		paintText.setStyle(SkPaint::kStroke_Style);
		paintText.setColor(-1); // white
		paintText.setStrokeWidth(2 + textShadow);
		cv->drawText(text.c_str(), text.length(), centerX, centerY, paintText);
// reset
		paintText.setStrokeWidth(2);
		paintText.setStyle(SkPaint::kFill_Style);
		paintText.setColor(c);
	}
	cv->drawText(text.data(), text.length(), centerX, centerY, paintText);
}


void drawWrappedText(RenderingContext* rc, SkCanvas* cv, TextDrawInfo* text, float textSize, SkPaint& paintText) {
	if(text->textWrap == 0) {
		// set maximum for all text
		text->textWrap = 40;
	}

	if(text->text.length() > text->textWrap) {
		int start = 0;
		int end = text->text.length();
		int lastSpace = -1;
		int line = 0;
		int pos = 0;
		int limit = 0;
		while(pos < end) {
			lastSpace = -1;
			limit += text->textWrap;
			// in UTF-8 all non ASCII characters has 2 or more characters
			int symbolsRead = 0;
			int utf8pos = pos;
			while(symbolsRead < limit && pos < end) {
				if(utf8pos == pos) {
					if(text->text.at(pos) <= 128) {
						symbolsRead++;
						if(!isLetterOrDigit(text->text.at(pos))) {
							lastSpace = pos;
						}
						utf8pos ++;
					}
				} else {
					// here could be code to determine if UTF-8 is ended (currently only 2 chars)
					symbolsRead++;
					utf8pos = pos + 1;
				}
				pos++;
			}
			if(lastSpace == -1) {
				PROFILE_NATIVE_OPERATION(rc, drawTextOnCanvas(cv, text->text.substr(start, pos), text->centerX, text->centerY + line * (textSize + 2), paintText, text->textShadow));
				start = pos;
			} else {
				PROFILE_NATIVE_OPERATION(rc, drawTextOnCanvas(cv, text->text.substr(start, lastSpace), text->centerX, text->centerY + line * (textSize + 2), paintText, text->textShadow));
				start = lastSpace + 1;
				limit += (start - pos) - 1;
			}
			line++;

		}
	} else {
		PROFILE_NATIVE_OPERATION(rc, drawTextOnCanvas(cv, text->text, text->centerX, text->centerY, paintText, text->textShadow));
	}
}

bool calculatePathToRotate(RenderingContext* rc, TextDrawInfo* p) {
	if(p->path == NULL) {
		return true;
	}
	int len = p->path->countPoints();
	SkPoint points[len];
	p->path->getPoints(points, len);
	if (!p->drawOnPath) {
		// simply calculate rotation of path used for shields
		float px = 0;
		float py = 0;
		for (int i = 1; i < len; i++) {
			px += points[i].fX - points[i - 1].fX;
			py += points[i].fY - points[i - 1].fY;
		}
		if (px != 0 || py != 0) {
			p->pathRotate = std::atan2(py, px);
		}
		return true;
	}

	bool inverse = false;
	float roadLength = 0;
	bool prevInside = false;
	float visibleRoadLength = 0;
	float textw = p->bounds.width();
	int i;
	int startVisible = 0;
	std::vector<float> distances;
	distances.resize(roadLength, 0);

	float normalTextLen = 1.5 * textw;
	for (i = 0; i < len; i++) {
		bool inside = points[i].fX >= 0 && points[i].fX <= rc->width &&
				points[i].fY >= 0 && points[i].fY <= rc->height;
		if (i > 0) {
			float d = std::sqrt(
					(points[i].fX - points[i - 1].fX) * (points[i].fX - points[i - 1].fX)
							+ (points[i].fY - points[i - 1].fY) * (points[i].fY - points[i - 1].fY));
			distances.push_back(d);
			roadLength += d;
			if(inside) {
				visibleRoadLength += d;
				if(!prevInside) {
					startVisible = i - 1;
				}
			} else if(prevInside) {
				if(visibleRoadLength >= normalTextLen) {
					break;
				}
				visibleRoadLength = 0;
			}

		}
		prevInside = inside;
	}
	if (textw >= roadLength) {
		return false;
	}
	int startInd = 0;
	int endInd = len;

	if(textw < visibleRoadLength && i - startVisible > 1) {
		startInd = startVisible;
		endInd = i;
		// display long road name in center
		if (visibleRoadLength > 3 * textw) {
			bool ch ;
			do {
				ch = false;
				if(endInd - startInd > 2 && visibleRoadLength - distances[startInd] > normalTextLen){
					visibleRoadLength -= distances.at(startInd);
					startInd++;
					ch = true;
				}
				if(endInd - startInd > 2 && visibleRoadLength - distances[endInd - 2] > normalTextLen){
					visibleRoadLength -= distances.at(endInd - 2);
					endInd--;
					ch = true;
				}
			} while(ch);
		}
	}
	// shrink path to display more text
	if (startInd > 0 || endInd < len) {
		// find subpath
		SkPath* path = new SkPath;
		for (int i = startInd; i < endInd; i++) {
			if (i == startInd) {
				path->moveTo(points[i].fX, points[i].fY);
			} else {
				path->lineTo(points[i].fX, points[i].fY);
			}
		}
		if (p->path != NULL) {
			delete p->path;
		}
		p->path = path;
	}
	// calculate vector of the road (px, py) to proper rotate it
	float px = 0;
	float py = 0;
	for (i = startInd + 1; i < endInd; i++) {
		px += points[i].fX - points[i - 1].fX;
		py += points[i].fY - points[i - 1].fY;
	}
	float scale = 0.5f;
	float plen = std::sqrt(px * px + py * py);
	// vector ox,oy orthogonal to px,py to measure height
	float ox = -py;
	float oy = px;
	if(plen > 0) {
		float rot = std::atan2(py, px);
		if (rot < 0) rot += M_PI * 2;
		if (rot > M_PI_2 && rot < 3 * M_PI_2) {
			rot += M_PI;
			inverse = true;
			ox = -ox;
			oy = -oy;
		}
		p->pathRotate = rot;
		ox *= (p->bounds.height() / plen) / 2;
		oy *= (p->bounds.height() / plen) / 2;
	}

	p->centerX = points[startInd].fX + scale * px + ox;
	p->centerY = points[startInd].fY + scale * py + oy;
	p->vOffset += p->textSize / 2 - 1;
	p->hOffset = 0;

	if (inverse) {
		SkPath* path = new SkPath;
		for (int i = endInd - 1; i >= startInd; i--) {
			if (i == (int)(endInd - 1)) {
				path->moveTo(points[i].fX, points[i].fY);
			} else {
				path->lineTo(points[i].fX, points[i].fY);
			}
		}
		if (p->path != NULL) {
			delete p->path;
		}
		p->path = path;
	}
	return true;
}

void drawTestBox(SkCanvas* cv, SkRect* r, float rot, SkPaint* paintIcon, std::string text, SkPaint* paintText)
{
	cv->save();
	cv->translate(r->centerX(),r->centerY());
	cv->rotate(rot * 180 / M_PI);
	SkRect rs = SkRect::MakeLTRB(-r->width()/2, -r->height()/2,
			r->width()/2, r->height()/2);
	cv->drawRect(rs, *paintIcon);
	if (paintText != NULL) {
		cv->drawText(text.data(), text.length(), rs.centerX(), rs.centerY(),
				*paintText);
	}
	cv->restore();
}

inline float sqr(float a){
	return a*a;
}

bool intersects(SkRect tRect, float tRot, TextDrawInfo* s)
{
	float sRot = s->pathRotate;
	if (abs(tRot) < M_PI / 15 && abs(sRot) < M_PI / 15) {
		return SkRect::Intersects(tRect, s->bounds);
	}
	float dist = sqrt(sqr(tRect.centerX() - s->bounds.centerX()) + sqr(tRect.centerY() - s->bounds.centerY()));
	if(dist < 3) {
		return true;
	}
	SkRect sRect = s->bounds;

	// difference close to 90/270 degrees
	if(abs(cos(tRot-sRot)) < 0.3 ){
		// rotate one rectangle to 90 degrees
		tRot += M_PI_2;
		tRect = SkRect::MakeXYWH(tRect.centerX() -  tRect.height() / 2, tRect.centerY() -  tRect.width() / 2,
				tRect.height(), tRect.width());
	}

	// determine difference close to 180/0 degrees
	if(abs(sin(tRot-sRot)) < 0.3){
		// rotate t box
		// (calculate offset for t center suppose we rotate around s center)
		float diff = atan2(tRect.centerY() - sRect.centerY(), tRect.centerX() - sRect.centerX());
		diff -= sRot;
		float left = sRect.centerX() + dist* cos(diff) - tRect.width()/2;
		float top = sRect.centerY() - dist* sin(diff) - tRect.height()/2;
		SkRect nRect = SkRect::MakeXYWH(left, top, tRect.width(), tRect.height());
		return SkRect::Intersects(nRect, sRect);
	}

	// TODO other cases not covered
	return SkRect::Intersects(tRect, sRect);
}

bool intersects(TextDrawInfo* t, TextDrawInfo* s) {
	return intersects(t->bounds, t->pathRotate, s);
}
std::vector<TextDrawInfo*> search;
bool findTextIntersection(SkCanvas* cv, RenderingContext* rc, quad_tree<TextDrawInfo*>& boundIntersections, TextDrawInfo* text,
		SkPaint* paintText, SkPaint* paintIcon) {
	paintText->measureText(text->text.c_str(), text->text.length(), &text->bounds);
	// make wider
	text->bounds.inset(-getDensityValue(rc, 3), -getDensityValue(rc, 10));

	bool display = calculatePathToRotate(rc, text);
	if (!display) {
		return true;
	}

	if(text->path == NULL) {
		text->bounds.offset(text->centerX, text->centerY);
		// shift to match alignment
		text->bounds.offset(-text->bounds.width()/2, 0);
	} else {
		text->bounds.offset(text->centerX - text->bounds.width()/2, text->centerY - text->bounds.height()/2);
	}

	// for text purposes
//	drawTestBox(cv, &text->bounds, text->pathRotate, paintIcon, text->text, NULL/*paintText*/);
	boundIntersections.query_in_box(text->bounds, search);
	for (uint i = 0; i < search.size(); i++) {
		TextDrawInfo* t = search.at(i);
		if (intersects(text, t)) {
			return true;
		}
	}
	if(text->minDistance > 0) {
		SkRect boundsSearch = text->bounds;
		boundsSearch.inset(-getDensityValue(rc, std::max(5.0f, text->minDistance)), -getDensityValue(rc, 15));
		boundIntersections.query_in_box(boundsSearch, search);
//		drawTestBox(cv, &boundsSearch, text->pathRotate, paintIcon, text->text, paintText);
		for (uint i = 0; i < search.size(); i++) {
			TextDrawInfo* t = search.at(i);
			if (t->minDistance > 0 && t->text == text->text && intersects(boundsSearch, text->pathRotate,  t)) {
				return true;
			}
		}
	}

	boundIntersections.insert(text, text->bounds);

	return false;
}


bool textOrder(TextDrawInfo* text1, TextDrawInfo* text2) {
	return text1->textOrder < text2->textOrder;
}

SkTypeface* serif = SkTypeface::CreateFromName("Droid Serif", SkTypeface::kNormal);
void drawTextOverCanvas(RenderingContext* rc, SkCanvas* cv) {
	SkRect r = SkRect::MakeLTRB(0, 0, rc->width, rc->height);
	r.inset(-100, -100);
	quad_tree<TextDrawInfo*> boundsIntersect(r, 4, 0.6);

	SkPaint paintIcon;
	paintIcon.setStyle(SkPaint::kStroke_Style);
	paintIcon.setStrokeWidth(1);
	paintIcon.setColor(0xff000000);
	SkPaint paintText;
	paintText.setStyle(SkPaint::kFill_Style);
	paintText.setStrokeWidth(1);
	paintText.setColor(0xff000000);
	paintText.setTextAlign(SkPaint::kCenter_Align);
	paintText.setTypeface(serif);
	paintText.setAntiAlias(true);
	SkPaint::FontMetrics fm;

	// 1. Sort text using text order
	std::sort(rc->textToDraw.begin(), rc->textToDraw.end(), textOrder);
	uint size = rc->textToDraw.size();
	for (uint i = 0; i < size; i++) {
		TextDrawInfo* text = rc->textToDraw.at(i);
		if (text->text.length() > 0) {
			size_t d = text->text.find(DELIM_CHAR);
			// not used now functionality
			// possibly it will be used specifying english names after that character
			if (d > 0) {
				text->text = text->text.substr(0, d);
			}

			// sest text size before finding intersection (it is used there)
			float textSize = getDensityValue(rc, text->textSize);
			paintText.setTextSize(textSize);
			paintText.setFakeBoldText(text->bold);
			paintText.setColor(text->textColor);
			// align center y
			paintText.getFontMetrics(&fm);
			text->centerY += (-fm.fAscent);

			// calculate if there is intersection
			bool intersects = findTextIntersection(cv, rc, boundsIntersect, text, &paintText, &paintIcon);
			if (!intersects) {
				if(rc->interrupted()){
						return;
				}
				if (text->drawOnPath && text->path != NULL) {
					if (text->textShadow > 0) {
						paintText.setColor(0xFFFFFFFF);
						paintText.setStyle(SkPaint::kStroke_Style);
						paintText.setStrokeWidth(2 + text->textShadow);
						rc->nativeOperations.pause();
						cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->path, text->hOffset,
								text->vOffset, paintText);
						rc->nativeOperations.start();
						// reset
						paintText.setStyle(SkPaint::kFill_Style);
						paintText.setStrokeWidth(2);
						paintText.setColor(text->textColor);
					}
					rc->nativeOperations.pause();
					cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->path, text->hOffset,
							text->vOffset, paintText);
					rc->nativeOperations.start();
				} else {
					if (text->shieldRes.length() > 0) {
						SkBitmap* ico = getCachedBitmap(rc, text->shieldRes);
						if (ico != NULL) {
							if(rc->highResMode) {
								float left = text->centerX - getDensityValue(rc, ico->width() / 2) - 0.5f;
								float top =text->centerY - getDensityValue(rc, ico->height() / 2) - getDensityValue(rc, 4.5f);
								SkRect r = SkRect::MakeXYWH(left, top, getDensityValue(rc, ico->width()), getDensityValue(rc, ico->height()));
								PROFILE_NATIVE_OPERATION(rc, cv->drawBitmapRect(*ico, (SkIRect*) NULL, r, &paintIcon));
							} else {
								PROFILE_NATIVE_OPERATION(rc, cv->drawBitmap(*ico, text->centerX - ico->width() / 2 - 0.5f,
									text->centerY - ico->height() / 2 - getDensityValue(rc, 4.5f), &paintIcon));
							}
						}
					}
					drawWrappedText(rc, cv, text, textSize, paintText);
				}
			}
		}
	}
}


