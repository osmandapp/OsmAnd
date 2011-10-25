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
	info->textOrder = render->getIntPropertyValue(render->props()->R_TEXT_ORDER, 20);
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
			while(pos < limit && pos < end) {
				if(!isLetterOrDigit(text->text.at(pos))) {
					lastSpace = pos;
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

const static bool findAllTextIntersections = true;
bool findTextIntersection(RenderingContext* rc, std::vector<SkRect>& boundsNotPathIntersect,
		std::vector<SkRect>&  boundsPathIntersect, /*Comparator<RectF> c, */TextDrawInfo* text,
		SkPaint* paintText) {
	bool horizontalWayDisplay = (text->pathRotate > 45 && text->pathRotate < 135)
			|| (text->pathRotate > 225 && text->pathRotate < 315);
	//text->minDistance = 0;

	float textWidth = paintText->measureText(text->text.c_str(), text->text.length()) +
			(!horizontalWayDisplay ? 0 : text->minDistance);
	// Paint.ascent is negative, so negate it.
	SkPaint::FontMetrics fm;
	paintText->getFontMetrics(&fm);
	int ascent = (int) std::ceil(-fm.fAscent);
	int descent = (int) std::ceil(fm.fDescent);
	float textHeight = ascent + descent + (horizontalWayDisplay ? 0 : text->minDistance) + getDensityValue(rc, 5);

	SkRect bounds;
	if (text->drawOnPath == NULL || horizontalWayDisplay) {
		bounds.set(text->centerX - textWidth / 2, text->centerY - textHeight / 2, text->centerX + textWidth / 2,
				text->centerY + textHeight / 2);
	} else {
		bounds.set(text->centerX - textHeight / 2, text->centerY - textWidth / 2, text->centerX + textHeight / 2,
				text->centerY + textWidth / 2);
	}
	std::vector< SkRect >* boundsIntersect =
			text->drawOnPath == NULL || findAllTextIntersections ? &boundsNotPathIntersect : &boundsPathIntersect;
	if (boundsIntersect->size()==0) {
		boundsIntersect->push_back(bounds);
	} else {
		int diff = (int) (getDensityValue(rc, 3));
		int diff2 = (int) (getDensityValue(rc, 15));
		// implement binary search
		// TODO binary search
//		int index = Collections.binarySearch(boundsIntersect, bounds, c);
//		if (index < 0) {
//			index = -(index + 1);
//		}
		int index = 0;
		// find sublist that is appropriate
		uint e = index;
		while (e < boundsIntersect->size()) {
			if (boundsIntersect->at(e).fLeft < bounds.fRight) {
				e++;
			} else {
				break;
			}
		}
		int st = index - 1;
		while (st >= 0) {
			// that's not exact algorithm that replace comparison rect with each other
			// because of that comparison that is not obvious
			// (we store array sorted by left boundary, not by right) - that's euristic
			if (boundsIntersect->at(st).fRight > bounds.fLeft) {
				st--;
			} else {
				break;
			}
		}
		if (st < 0) {
			st = 0;
		}
		// test functionality
		//cv.drawRect(bounds, paint);
		//cv.drawText(text->text->substring(0, Math.min(5, text->text->length())), bounds.centerX(), bounds.centerY(), paint);

		for (uint j = st; j < e; j++) {
			SkRect b = boundsIntersect->at(j);
			float x = std::min(bounds.fRight, b.fRight) - std::max(b.fLeft, bounds.fLeft);
			float y = std::min(bounds.fBottom, b.fBottom) - std::max(b.fTop, bounds.fTop);
			if ((x > diff && y > diff2) || (x > diff2 && y > diff)) {
				return true;
			}
		}
		// TODO insert
		// boundsIntersect->(index, bounds);
	}
	return false;
}

SkTypeface* serif = SkTypeface::CreateFromName("Droid Serif", SkTypeface::kNormal);
void drawTextOverCanvas(RenderingContext* rc, SkCanvas* cv) {
		std::vector<SkRect> boundsNotPathIntersect;
		std::vector<SkRect> boundsPathIntersect;
		uint size = rc->textToDraw.size();

		// TODO comparator !!!
//	Comparator < SkRect > c = new Comparator<SkRect>()
//	{
//		int compare(SkRect object1, SkRect object2) {
//			return Float.compare(object1.left, object2.left);
//		}
//
//	};

	// 1. Sort text using text order
		// TODO sort !!!
//	Collections.sort(rc.textToDraw, new Comparator<TextDrawInfo>() {
//				@Override
//			public int compare(TextDrawInfo object1, TextDrawInfo object2) {
//					return object1.textOrder - object2.textOrder;
//				}
//			});
	SkPaint paintIcon;
	paintIcon.setStyle(SkPaint::kStroke_Style);
	SkPaint paintText;
	paintText.setStyle(SkPaint::kFill_Style);
	paintText.setStrokeWidth(1);
	paintText.setColor(0xff000000);
	paintText.setTextAlign(SkPaint::kCenter_Align);
	paintText.setTypeface(serif);
	paintText.setAntiAlias(true);
	SkPaint::FontMetrics fm;

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
			// bool intersects = findTextIntersection(rc, boundsNotPathIntersect, boundsPathIntersect, c, text);
			// TODO
			bool intersects = false;
			if (!intersects) {

				if (text->drawOnPath != NULL) {
					if (text->textShadow > 0) {
						paintText.setColor(WHITE_COLOR);
						paintText.setStyle(SkPaint::kStroke_Style);
						paintText.setStrokeWidth(2 + text->textShadow);
						cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->drawOnPath, 0,
								text->vOffset, paintText);
						// reset
						paintText.setStyle(SkPaint::kFill_Style);
						paintText.setStrokeWidth(2);
						paintText.setColor(text->textColor);
					}
					cv->drawTextOnPathHV(text->text.c_str(), text->text.length(), *text->drawOnPath, 0, text->vOffset,
							paintText);
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


