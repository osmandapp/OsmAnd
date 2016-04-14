package net.osmand.plus.views;

import android.graphics.*;
import net.osmand.plus.R;
import net.osmand.router.TurnType;
import android.content.res.Resources;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import java.util.Map;

public class TurnPathHelper {

	//Index of processed turn
	public static final int FIRST_TURN = 1;
	public static final int SECOND_TURN = 2;
	public static final int THIRD_TURN = 3;

	// 72x72
	public static void calcTurnPath(Path pathForTurn, TurnType turnType, Matrix transform) {
		if(turnType == null){
			return;
		}
		pathForTurn.reset();
		int ha = 72;
		int wa = 72;

		int th = 12; // thickness
		pathForTurn.moveTo(wa / 2, ha - 1);
		float sarrowL = 22; // side of arrow ?
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float spartArrowL = (float) ((sarrowL - th / Math.sqrt(2)) / 2);
		float hpartArrowL = (float) (harrowL - th) / 2;

		if (TurnType.C == turnType.getValue()) {
			int h = (int) (ha - hpartArrowL - 16);
			pathForTurn.rMoveTo(th, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(-harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-harrowL / 2, harrowL / 2);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TR == turnType.getValue()|| TurnType.TL == turnType.getValue()) {
			int b = TurnType.TR == turnType.getValue()? 1 : -1;
			float quadShiftX = 18;
			float quadShiftY = 18;
			int wl = 10; // width
			int h = (int) (ha - quadShiftY - harrowL + hpartArrowL - 5);
			int sl = wl + th / 2;
			
			pathForTurn.rMoveTo(-b * sl, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -quadShiftY, b * quadShiftX, -quadShiftY);
			pathForTurn.rLineTo(b * wl, 0);
			
			pathForTurn.rLineTo(0, hpartArrowL);
			pathForTurn.rLineTo(b * harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-b * harrowL / 2, -harrowL / 2);
			pathForTurn.rLineTo(0, hpartArrowL);
			
			pathForTurn.rLineTo(-b * wl, 0);
			pathForTurn.rQuadTo(-b * (quadShiftX + th), 0, -b * (quadShiftX + th), quadShiftY + th);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.KL == turnType.getValue() || TurnType.KR == turnType.getValue()) {
			int b = TurnType.KR == turnType.getValue()? 1 : -1;
			float quadShiftX = 14;
			float quadShiftY = 14;
			th = 10;
			spartArrowL = (float) ((sarrowL - th / Math.sqrt(2)) / 2);
			hpartArrowL = (float) (harrowL - th) / 2;
			int h = 12;
			int lh = 15;
			int sl = th / 2;
			
			pathForTurn.rMoveTo(-b * (sl + 10), 0);
			pathForTurn.rLineTo(0, -lh);
			// 1st arc
			pathForTurn.rQuadTo(0, -quadShiftY, b * quadShiftX, -quadShiftY);
			// 2nd arc
			pathForTurn.rQuadTo(b * quadShiftX, 0, b * quadShiftX, -quadShiftY);
			// center
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rLineTo(b*hpartArrowL, 0);
			pathForTurn.rLineTo(-b*harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-b*harrowL / 2, harrowL / 2);
			pathForTurn.rLineTo(b*hpartArrowL, 0);
			pathForTurn.rLineTo(0, h );
			// 2nd arc
			pathForTurn.rQuadTo(0, quadShiftY - th, -b * (quadShiftX - th), quadShiftY- th);
			//1st arc
			pathForTurn.rQuadTo(-b * (quadShiftX + th), 0, -b * (quadShiftX + th ), quadShiftY + th);
			pathForTurn.rLineTo(0, lh );

		} else if (TurnType.TSLR == turnType.getValue() || TurnType.TSLL == turnType.getValue()) {
			int b = TurnType.TSLR == turnType.getValue() ? 1 : -1;
			int h = 24;
			int quadShiftY = 22;
			float quadShiftX = (float) (quadShiftY / (1 + Math.sqrt(2)));
			float nQuadShiftX = (sarrowL - 2 * spartArrowL) - quadShiftX - th;
			float nQuadShifty = quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * 4, 0);
			pathForTurn.rLineTo(0, -h /* + partArrowL */);
			pathForTurn.rQuadTo(0, -quadShiftY + quadShiftX /*- partArrowL*/, b * quadShiftX, -quadShiftY /*- partArrowL*/);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(0, -sarrowL); // center
			pathForTurn.rLineTo(-b * sarrowL, 0);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rQuadTo(b * nQuadShiftX, -nQuadShiftX, b * nQuadShiftX, nQuadShifty);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSHR == turnType.getValue() || TurnType.TSHL == turnType.getValue()) {
			int b = TurnType.TSHR == turnType.getValue() ? 1 : -1;
			int h = 28;
			float quadShiftX = 22;
			int sh = 10;
			float quadShiftY = -(float) (quadShiftX / (1 + Math.sqrt(2)));
			float nQuadShiftX = -(sarrowL - 2 * spartArrowL) - quadShiftX - th;
			float nQuadShiftY = -quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * sh, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftX - quadShiftY), b * quadShiftX, quadShiftY);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(b * sarrowL, 0); // center
			pathForTurn.rLineTo(0, -sarrowL);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rCubicTo(b * nQuadShiftX / 2, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if(TurnType.TU == turnType.getValue() || TurnType.TRU == turnType.getValue()) {
			int h = 40;
			// right left
			int b = TurnType.TU == turnType.getValue() ? 1 : -1;
			float quadShiftX = 10; // 13
			float quadShiftY = 10; // 13
			int sm = 10;

			pathForTurn.rMoveTo(b * 28, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftY+th), -b * (quadShiftX+th), -(quadShiftY+th));
			pathForTurn.rQuadTo(-b * (quadShiftX+th), 0, -b * (quadShiftX+th), (quadShiftY+th));
			pathForTurn.rLineTo(0, sm);

			pathForTurn.rLineTo(-b * hpartArrowL, 0);
			pathForTurn.rLineTo(b * harrowL/2, harrowL/2); // center
			pathForTurn.rLineTo(b * harrowL/2, -harrowL/2);
			pathForTurn.rLineTo(-b  *hpartArrowL, 0);

			pathForTurn.rLineTo(0, -sm);
			pathForTurn.rQuadTo(0, -quadShiftX, b *quadShiftX, -quadShiftY);
			pathForTurn.rQuadTo(b * quadShiftX, 0, b * quadShiftX, quadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.OFFR == turnType.getValue()){
			int h = (int) (ha - hpartArrowL - 16);
			pathForTurn.rMoveTo(th, 0); //12 0
			//first square
			pathForTurn.rLineTo(0, -h / 4); //0 -7
			pathForTurn.rLineTo(-th, 0); //-12 0
			pathForTurn.rLineTo(0, h / 4); //0 7
			pathForTurn.rLineTo(th, 0); //12 0
			pathForTurn.rMoveTo(0, -h / 2); //12 0
			//second square
			pathForTurn.rLineTo(0, -h / 4); //0 -7
			pathForTurn.rLineTo(-th, 0); //-12 0
			pathForTurn.rLineTo(0, h / 4); //0 7
			pathForTurn.rLineTo(th, 0); //12 0
			pathForTurn.rMoveTo(0, -h / 2 + 1); //31 0
			//arrow
			pathForTurn.rLineTo(hpartArrowL, 0); //9 0
			pathForTurn.rLineTo(-harrowL / 2, -harrowL / 2); // center -15 -15
			pathForTurn.rLineTo(-harrowL / 2, harrowL / 2); // -15 15
			pathForTurn.rLineTo(hpartArrowL + th, 0); //9 0
		} else if (turnType != null && turnType.isRoundAbout()) {
			float t = turnType.getTurnAngle();
			if (t >= 170 && t < 220) {
				t = 220;
			} else if (t > 160 && t < 170) {
				t = 160;
			}
			boolean leftSide = turnType.isLeftSide();
			float sweepAngle = (t - 360) - 180;
			if (sweepAngle < -360) {
				sweepAngle += 360;
			}
			if(leftSide && sweepAngle < 0) {
				sweepAngle += 360;
			}
			
			float r1 = ha / 3f - 1;
			float r2 = r1 - 9;
			float angleToRot = leftSide ? -0.3f : 0.3f;
			int cx = wa / 2 ;
			int cy = ha / 2 - 2;
			if (leftSide) {
				pathForTurn.moveTo(cx - 8, ha - 1);
				pathForTurn.lineTo(cx - 8, cy + r1);
			} else {
				pathForTurn.moveTo(cx, ha - 1);
				pathForTurn.lineTo(cx, cy + r1);
			}
			RectF r = new RectF(cx - r1, cy - r1, cx + r1, cy + r1);
			
			int out = turnType.getExitOut();
			if (out < 1) {
				out = 1;
			}
			float prev = 90;
			float init = 90;
			
			float step = sweepAngle / out;
			for (int i = 1; i <= out; i++) {
				float to = step * i;
				if (i == out) {
					pathForTurn.arcTo(r, prev, to - prev + init);
				} else {
					float tsRad = (float) ((to - step / 8 + 180) * Math.PI / 180f);
					float tsRad2 = (float) ((to + step / 8 + 180) * Math.PI / 180f);
					pathForTurn.arcTo(r, prev, to - step / 6 - prev + init );
					pathForTurn.lineTo(cx + (r1 + 10) * (float) Math.sin(tsRad), cy - (r1 + 10) * (float) Math.cos(tsRad));
					pathForTurn.lineTo(cx + (r1 + 10) * (float) Math.sin(tsRad2), cy - (r1 + 10) * (float) Math.cos(tsRad2));
					// not necessary for next arcTo
					//pathForTurn.lineTo(cx + (r1 + 0) * (float) Math.sin(tsRad2), cy - (r1 + 0) * (float) Math.cos(tsRad2));
					prev = to + step / 6 + init;
				}
			}
		
			float angleRad = (float) ((180 + sweepAngle) * Math.PI / 180f);
			
			pathForTurn.lineTo(cx + (r1 + 4) * (float) Math.sin(angleRad), cy - (r1 + 4) * (float) Math.cos(angleRad));
			pathForTurn.lineTo(cx + (r1 + 6) * (float) Math.sin(angleRad + angleToRot/2), cy - (r1 + 6) * (float) Math.cos(angleRad + angleToRot/2));
			pathForTurn.lineTo(cx + (r1 + 14) * (float) Math.sin(angleRad - angleToRot/2), cy - (r1 + 12) * (float) Math.cos(angleRad - angleToRot/2));
			pathForTurn.lineTo(cx + (r1 + 6) * (float) Math.sin(angleRad - 3*angleToRot/2), cy - (r1 + 6) * (float) Math.cos(angleRad - 3*angleToRot/2));
			pathForTurn.lineTo(cx + (r1 + 4) * (float) Math.sin(angleRad - angleToRot), cy - (r1 + 4) * (float) Math.cos(angleRad - angleToRot));
			pathForTurn.lineTo(cx + r2 * (float) Math.sin(angleRad - angleToRot), cy - r2 * (float) Math.cos(angleRad - angleToRot));
			
			r.set(cx - r2, cy - r2, cx + r2, cy + r2);
			pathForTurn.arcTo(r, 360 + sweepAngle + 90, -sweepAngle);
			if (leftSide) {
				pathForTurn.lineTo(cx, cy + r2);
				pathForTurn.lineTo(cx, ha - 1);
			} else {
				pathForTurn.lineTo(cx - 8, cy + r2);
				pathForTurn.lineTo(cx - 8, ha - 1);
			}
			pathForTurn.close();
		}
		pathForTurn.close();
		if(transform != null){
			pathForTurn.transform(transform);
		}
	}
	
	public static class RouteDrawable extends Drawable {
		Paint paintRouteDirection;
		Path p = new Path();
		Path dp = new Path();
		
		public RouteDrawable(Resources resources){
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(resources.getColor(R.color.nav_arrow_distant));
			paintRouteDirection.setAntiAlias(true);
			TurnPathHelper.calcTurnPath(dp, TurnType.straight(), null);
		}

		@Override
		protected void onBoundsChange(Rect bounds) {
			Matrix m = new Matrix();
			m.setScale(bounds.width() / 72f, bounds.height() / 72f);
			p.transform(m, dp);
		}
		
		public void setRouteType(TurnType t){
			TurnPathHelper.calcTurnPath(p, t, null);
			onBoundsChange(getBounds());
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawPath(dp, paintRouteDirection);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);
			
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}
		
	}


	public static class TurnResource {
		boolean flip;
		int resourceId;

		public TurnResource(){}

		public TurnResource(int resourceId, boolean value) {
			this.resourceId = resourceId;
			this.flip = value;
		}

		@Override
		public boolean equals(Object o) {
			return super.equals(o);
		}

		@Override
		public int hashCode() {
			return resourceId * (flip ? -1 : 1);
		}
	}

	private static TurnResource getTallArrow(int tt){

		TurnResource result = new TurnResource();

		switch (tt){
			case TurnType.C:
				result.resourceId = R.drawable.map_turn_forward_small;
				break;
			case TurnType.TR:
			case TurnType.TL:
				result.resourceId = R.drawable.map_turn_right2_small;
				break;
			case TurnType.KR:
			case TurnType.KL:
				result.resourceId = R.drawable.map_turn_keep_right_small;
				break;
			case TurnType.TSLR:
			case TurnType.TSLL:
				result.resourceId = R.drawable.map_turn_slight_right_small;
				break;
			case TurnType.TSHR:
			case TurnType.TSHL:
				result.resourceId = R.drawable.map_turn_sharp_right_small;
				break;
			case TurnType.TRU:
			case TurnType.TU:
				result.resourceId = R.drawable.map_turn_uturn_right_small;
				break;
			default:
				result.resourceId = R.drawable.map_turn_forward_small;
				break;
		}

		if(tt == TurnType.TL || tt == TurnType.KL || tt == TurnType.TSLL
				|| tt == TurnType.TSHL || tt == TurnType.TU){
			result.flip = true;
		}

		return result;

	}

	private static TurnResource getShortArrow(int tt){

		TurnResource result = new TurnResource();

		switch (tt) {
			case TurnType.C:
				result.resourceId = R.drawable.map_turn_forward_small;
				break;
			case TurnType.TR:
			case TurnType.TL:
				result.resourceId = R.drawable.map_turn_forward_right_turn_small;
				break;
			case TurnType.KR:
			case TurnType.KL:
				result.resourceId = R.drawable.map_turn_forward_keep_right_small;
				break;
			case TurnType.TSLR:
			case TurnType.TSLL:
				result.resourceId = R.drawable.map_turn_forward_slight_right_turn_small;
				break;
			case TurnType.TSHR:
			case TurnType.TSHL:
				result.resourceId = R.drawable.map_turn_forward_turn_sharp_small;
				break;
			case TurnType.TRU:
			case TurnType.TU:
				result.resourceId = R.drawable.map_turn_forward_uturn_right_small;
				break;
			default:
				result.resourceId = R.drawable.map_turn_forward_small;
				break;
		}

		if(tt == TurnType.TL || tt == TurnType.KL || tt == TurnType.TSLL
				|| tt == TurnType.TSHL || tt == TurnType.TU){
			result.flip = true;
		}

		return result;

	}

	public static Bitmap getBitmapFromTurnType(Resources res, Map<TurnResource, Bitmap> cache, int firstTurn,
			int secondTurn, int thirdTurn, int turnIndex, float coef, boolean leftSide) {

		int firstTurnType = TurnType.valueOf(firstTurn, leftSide).getValue();
		int secondTurnType = TurnType.valueOf(secondTurn, leftSide).getValue();
		int thirdTurnType = TurnType.valueOf(thirdTurn, leftSide).getValue();

		TurnResource turnResource = null;

		if (turnIndex == FIRST_TURN) {
			if (secondTurnType == 0) {
				turnResource = getTallArrow(firstTurnType);
			} else if (secondTurnType == TurnType.C || thirdTurnType == TurnType.C) {
				turnResource = getShortArrow(firstTurnType);
			} else {
				if (firstTurnType == TurnType.TU || firstTurnType == TurnType.TRU) {
					turnResource = getShortArrow(firstTurnType);
				} else {
					turnResource = getTallArrow(firstTurnType);
				}
			}
		} else if (turnIndex == SECOND_TURN) {
			if (TurnType.isLeftTurn(firstTurnType) && TurnType.isLeftTurn(secondTurnType)) {
				turnResource = null;
			} else if (TurnType.isRightTurn(firstTurnType) && TurnType.isRightTurn(secondTurnType)) {
				turnResource = null;
			} else if (firstTurnType == TurnType.C || thirdTurnType == TurnType.C) {
				// get the small one
				turnResource = getShortArrow(secondTurnType);
			} else {
				turnResource = getTallArrow(secondTurnType);
			}
		} else if (turnIndex == THIRD_TURN) {
			if ((TurnType.isLeftTurn(firstTurnType) || TurnType.isLeftTurn(secondTurnType)) && TurnType.isLeftTurn(thirdTurnType)) {
				turnResource = null;
			} else if ((TurnType.isRightTurn(firstTurnType) || TurnType.isRightTurn(secondTurnType)) && TurnType.isRightTurn(thirdTurnType)) {
				turnResource = null;
			} else {
				turnResource = getShortArrow(thirdTurnType);
			}
		}
		if (turnResource == null) {
			return null;
		}

		Bitmap b = cache.get(turnResource);
		if (b == null) {
			b = turnResource.flip ? getFlippedBitmap(res, turnResource.resourceId) : BitmapFactory.decodeResource(res,
					turnResource.resourceId);
			cache.put(turnResource, b);
		}

		// Maybe redundant scaling
		/*
		 * float bRatio = (float)b.getWidth() / (float)b.getHeight(); float s = 72f * coef; int wq = Math.round(s /
		 * bRatio); int hq = Math.round(s); b = Bitmap.createScaledBitmap(b, wq, hq, false);
		 */

		return b;
	}

	public static Bitmap getFlippedBitmap(Resources res, int resId){

		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		//Below line is necessary to fill in opt.outWidth, opt.outHeight
		Bitmap b = BitmapFactory.decodeResource(res, resId, opt);

		b = Bitmap.createBitmap(opt.outWidth, opt.outHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(b);

		Matrix flipHorizontalMatrix = new Matrix();
		flipHorizontalMatrix.setScale(-1, 1);
		flipHorizontalMatrix.postTranslate(b.getWidth(), 0);

		Bitmap bb = BitmapFactory.decodeResource(res, resId);
		canvas.drawBitmap(bb, flipHorizontalMatrix, null);

		return b;
	}


}
