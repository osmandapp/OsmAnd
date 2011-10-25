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

#include "common.cpp"
#include "renderRules.cpp"

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
			if (box.intersect(node->bounds)) {
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


void drawWrappedText(SkCanvas* cv, TextDrawInfo* text, float textSize, SkPaint& paintText) {
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
				drawTextOnCanvas(cv, text->text.substr(start, pos),
						text->centerX, text->centerY + line * (textSize + 2), paintText, text->textShadow);
				start = pos;
			} else {
				drawTextOnCanvas(cv, text->text.substr(start, lastSpace),
						text->centerX, text->centerY + line * (textSize + 2), paintText, text->textShadow);
				start = lastSpace + 1;
				limit += (start - pos) - 1;
			}
			line++;

		}
	} else {
		drawTextOnCanvas(cv, text->text, text->centerX, text->centerY, paintText, text->textShadow);
	}
}

bool calculatePathToRotate(RenderingContext* rc, TextDrawInfo* p) {
	// TODO rotate bounds for shields?
	if (!p->drawOnPath || p->path == NULL) {
		return true;
	}
	int len = p->path->countPoints();
	SkPoint points[len];
	p->path->getPoints(points, len);

	bool inverse = false;
	float roadLength = 0;
	bool prevInside = false;
	float visibleRoadLength = 0;
	float textw = p->bounds.width();
	int i;
	int startVisible = 1;
	for (i = 0; i < len; i++) {
		bool inside = points[i].fX >= 0 && points[i].fX <= rc->width &&
				points[i].fY >= 0 && points[i].fY <= rc->height;
		if (i > 0) {
			float d = std::sqrt(
					(points[i].fX - points[i - 1].fX) * (points[i].fX - points[i - 1].fX)
							+ (points[i].fY - points[i - 1].fY) * (points[i].fY - points[i - 1].fY));
			roadLength += d;
			if(inside) {
				visibleRoadLength += d;
				if(!prevInside) {
					startVisible = i - 1;
				}
			} else if(!prevInside) {
				if(visibleRoadLength >= 1.5 * textw) {
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
	if(textw < visibleRoadLength) {
		startInd = startVisible - 1;
		endInd = i;
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
		float rot = std::atan2(py, px) * 180 / M_PI;
		if (rot < 0) rot += 360;
		if (rot > 90 && rot < 270) {
			rot += 180;
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
	p->vOffset = p->textSize / 2 - 1;
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

void drawTestBox(SkCanvas* cv, TextDrawInfo* text, SkPaint* paintIcon, SkPaint* paintText)
{
	cv->save();
	cv->translate(text->bounds.centerX(),text->bounds.centerY());
	cv->rotate(text->pathRotate);
	SkRect r = SkRect::MakeLTRB(-text->bounds.width()/2, -text->bounds.height()/2,
			text->bounds.width()/2, text->bounds.height()/2);
	cv->drawRect(r, *paintIcon);
	if (paintText != NULL) {
		cv->drawText(text->text.data(), text->text.length(), r.centerX(), r.centerY(),
				*paintText);
	}
	cv->restore();
}

bool intersect(TextDrawInfo* t, TextDrawInfo* s)
{
	// TODO rotation
	return t->bounds.intersect(s->bounds);
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

	if(!text->drawOnPath) {
		text->bounds.offset(text->centerX, text->centerY);
		// shift to match alignment
		text->bounds.offset(-text->bounds.width()/2, 0);
	} else {
		text->bounds.offset(text->centerX - text->bounds.width()/2, text->centerY - text->bounds.height()/2);
	}


	SkRect boundsSearch = text->bounds;
	float v = -getDensityValue(rc, std::max(5.0f, text->minDistance));
	boundsSearch.inset(v, v);

	// for text purposes
//	drawTestBox(cv, text, paintIcon, paintText);
	boundIntersections.query_in_box(boundsSearch, search);
	for (uint i = 0; i < search.size(); i++) {
		TextDrawInfo* t = search.at(i);
		if (intersect(text, t)) {
			return true;
		} else if (boundsSearch.intersect(t->bounds) && t->text == text->text) {
			return true;
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
				if (text->drawOnPath && text->path != NULL) {
					if (text->textShadow > 0) {
						paintText.setColor(WHITE_COLOR);
						paintText.setStyle(SkPaint::kStroke_Style);
						paintText.setStrokeWidth(2 + text->textShadow);
						cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->path, text->hOffset,
								text->vOffset, paintText);
						// reset
						paintText.setStyle(SkPaint::kFill_Style);
						paintText.setStrokeWidth(2);
						paintText.setColor(text->textColor);
					}
					cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->path, text->hOffset,
							text->vOffset, paintText);
				} else {
					if (text->shieldRes.length() > 0) {
						SkBitmap* ico = getCachedBitmap(rc, text->shieldRes);
						if (ico != NULL) {
							cv->drawBitmap(*ico, text->centerX - ico->width() / 2 - 0.5f,
									text->centerY - ico->height() / 2 - getDensityValue(rc, 4.5f), &paintIcon);
						}
					}

					drawWrappedText(cv, text, textSize, paintText);
				}
			}
		}
	}
}


