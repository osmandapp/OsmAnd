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
	private static final boolean USE_NEW_RNDB = true;
	private static final boolean SHOW_STEPS = true;

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
		} else if(turnType != null && turnType.isRoundAbout() && USE_NEW_RNDB) {
			int out = turnType.getExitOut();
			boolean leftSide = turnType.isLeftSide();
			float radArrow = 35;
			float radIn = 11;
			float radOut = radIn + 8;
		
			float radBottom = radOut + 8;
			float radStepInter = radOut + 7;
			float radAr = radOut + 3;
			float radAr2 = radOut + 2;
		
			float widthStepIn = 8;
			float widthStepInter = 6;
			float widthArrow = 20;
			float cx = wa / 2 ;
			float cy = ha / 2 ;
		
			
			double dfL = (leftSide ? 1 : -1) * Math.asin(widthStepIn / (2.0 * radBottom)); 
			double dfAr2 = (leftSide ? 1 : -1) * Math.asin(widthArrow / (2.0 * radAr2));
			double dfStepInter = (leftSide ? 1 : -1) * Math.asin(widthStepInter / radStepInter);
			double dfAr = Math.asin(radBottom * Math.sin(dfL) / radAr);
			double dfOut = Math.asin(radBottom * Math.sin(dfL) / radOut);
			double dfStepOut = Math.asin(radStepInter * Math.sin(dfStepInter) / radOut);
			double dfIn = Math.asin(radBottom * Math.sin(dfL) / radIn);
			double minDelta = Math.abs(dfIn * 2 / Math.PI * 180 ) + 2;
			boolean showSteps = SHOW_STEPS;
//			System.out.println("Angle " + dfL + " " + dfOut + " " + dfIn + " " + minDelta + " ");
			double rot = alignRotation(turnType.getTurnAngle(), leftSide, minDelta) / 180 * Math.PI;
			
			
			RectF qrOut = new RectF(cx - radOut, cy - radOut, cx + radOut, cy + radOut);
			RectF qrIn = new RectF(cx - radIn, cy - radIn, cx + radIn, cy + radIn);
			
			// move to bottom ring
			pathForTurn.moveTo(getProjX(dfOut, cx, cy, radOut), getProjY(dfOut, cx, cy, radOut));
			if (out <= 1) {
				showSteps = false;
			}
			if (showSteps) {
				double totalStepInter = (out - 1) * dfStepOut;
				double st = (rot - 2 * dfOut - totalStepInter) / out;
				if ((rot > 0) != (st > 0)) {
					showSteps = false;
				}
				if (Math.abs(st) < Math.PI / 60) {
					showSteps = false;
				}
				// double st = (rot - 2 * dfOut ) / (2 * out - 1);
				// dfStepOut = st;
				if (showSteps) {
					for (int i = 0; i < out - 1; i++) {
						pathForTurn.arcTo(qrOut, startArcAngle(dfOut + i * (st + dfStepOut)), sweepArcAngle(st));
						arcLineTo(pathForTurn,
								dfOut + (i + 1) * (st + dfStepOut) - dfStepOut / 2 - dfStepInter / 2, cx, cy, radStepInter);
						arcLineTo(pathForTurn, dfOut + (i + 1) * (st + dfStepOut) - dfStepOut / 2 + dfStepInter / 2, cx, cy, radStepInter);
						arcLineTo(pathForTurn, dfOut + (i + 1) * (st + dfStepOut), cx, cy, radOut);
						// pathForTurn.arcTo(qr1, startArcAngle(dfOut), sweepArcAngle(rot - dfOut - dfOut));
					}
					pathForTurn.arcTo(qrOut, startArcAngle(rot - dfOut - st), sweepArcAngle(st));
				}
			} 
			if(!showSteps) {
				// arc
				pathForTurn.arcTo(qrOut, startArcAngle(dfOut), sweepArcAngle(rot - dfOut - dfOut));
			}
			
			// up from arc
			arcLineTo(pathForTurn, rot - dfAr, cx, cy, radAr);
			// left triangle 
//			arcLineTo(pathForTurn, rot - dfAr2, cx, cy, radAr2); // 1.
//			arcQuadTo(pathForTurn, rot - dfAr2, radAr2, rot, radArrow, 0.9f, cx, cy); // 2.
			arcQuadTo(pathForTurn, rot - dfAr, radAr, rot - dfAr2, radAr2, rot, radArrow, 0.7f, 0.3f, cx, cy); // 3.
			
//			arcLineTo(pathForTurn, rot, cx, cy, radArrow); // 1.
			arcQuadTo(pathForTurn, rot - dfAr2, radAr2, rot, radArrow, rot + dfAr2, radAr2, 0.1f, 0.1f, cx, cy);
			// right triangle 
//			arcLineTo(pathForTurn, rot + dfAr2, cx, cy, radAr2); // 1.
			arcQuadTo(pathForTurn, rot, radArrow, rot + dfAr2, radAr2, rot + dfAr, radAr, 0.3f, 0.7f, cx, cy);
			
			arcLineTo(pathForTurn, rot + dfAr, cx, cy, radAr);
			// down to arc
			arcLineTo(pathForTurn, rot + dfIn, cx, cy, radIn);
			// arc
			pathForTurn.arcTo(qrIn, startArcAngle(rot + dfIn), sweepArcAngle(-rot - dfIn - dfIn));
			// down
			arcLineTo(pathForTurn, -dfL, cx, cy, radBottom);
			// left
			arcLineTo(pathForTurn, dfL, cx, cy, radBottom);
			
		} else if (turnType != null && turnType.isRoundAbout()) {
			float t = turnType.getTurnAngle();
			boolean leftSide = turnType.isLeftSide();
			double minTurn = 25;
			if (t >= 170 && t < 215) {
				t = 215;
			} else if (t > 155 && t < 170) {
				t = 155;
			}
			
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
	
	private static float alignRotation(float t, boolean leftSide, double minDelta) {
		// t between -180, 180
		while(t > 180) {
			t -= 360;
		}
		while(t < -180) {
			t += 360;
		}
		float rot = leftSide ? (t + 180) : (t - 180) ;
		float delta = (float) minDelta;
		if(rot > 360 - delta && rot < 360) {
			rot = 360 - delta;
		} else if (rot > 0 && rot < delta) {
			rot = delta;
		} else if (rot < -360 + delta && rot > -360) {
			rot = -360 + delta;
		} else if (rot < 0 && rot > -delta) {
			rot = -delta;
		}
		return rot;
	}
	
	private static void arcLineTo(Path pathForTurn, double angle, float cx, float cy, float radius) {
		pathForTurn.lineTo(getProjX(angle, cx, cy, radius), getProjY(angle, cx, cy, radius));		
	}
	
	private static void arcQuadTo(Path pathForTurn, double angle, float radius, double angle2, float radius2,
			float proc, float cx, float cy) {
		float X = getProjX(angle, cx, cy, radius);
		float Y = getProjY(angle, cx, cy, radius);
		float X2 = getProjX(angle2, cx, cy, radius2);
		float Y2 = getProjY(angle2, cx, cy, radius2);
		pathForTurn.quadTo(X, Y, X2 * proc + X * (1 - proc), Y2 * proc + Y * (1 - proc));
	}
	
	private static void arcQuadTo(Path pathForTurn, double angle0, float radius0, double angle, float radius, double angle2, float radius2,
			float proc0, float proc2, float cx, float cy) {
		float X0 = getProjX(angle0, cx, cy, radius0);
		float Y0 = getProjY(angle0, cx, cy, radius0);
		float X = getProjX(angle, cx, cy, radius);
		float Y = getProjY(angle, cx, cy, radius);
		float X2 = getProjX(angle2, cx, cy, radius2);
		float Y2 = getProjY(angle2, cx, cy, radius2);
		pathForTurn.lineTo(X0 * proc0 + X * (1 - proc0), Y0 * proc0 + Y * (1 - proc0));
		pathForTurn.quadTo(X, Y, X2 * proc2 + X * (1 - proc2), Y2 * proc2 + Y * (1 - proc2));
	}
	

	// angle - bottom is zero, right is -90, left is 90 
	private static float getX(double angle, double radius) {
		return (float) (Math.cos(angle + Math.PI / 2) * radius);
	}
	
	private static float getY(double angle, double radius) {
		return (float) (Math.sin(angle + Math.PI / 2) * radius);
	}
	
	private static float getProjX(double angle, float cx, float cy, double radius) {
		return getX(angle, radius) + cx;
	}
	
	private static float getProjY(double angle, float cx, float cy, double radius) {
		return getY(angle, radius) + cy;
	}


	private static float startArcAngle(double i) {
		return (float) (i * 180 / Math.PI + 90);
	}
	
	private static float sweepArcAngle(double d) {
		return (float) (d * 180 / Math.PI);
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

	private static TurnResource getTallArrow(int tt, boolean nooverlap){

		TurnResource result = new TurnResource();

		switch (tt){
			case TurnType.C:
				result.resourceId = R.drawable.map_turn_forward_small;
				break;
			case TurnType.TR:
			case TurnType.TL:
				result.resourceId = nooverlap ? R.drawable.map_turn_right_small : R.drawable.map_turn_right2_small;
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
				turnResource = getTallArrow(firstTurnType, true);
			} else if (secondTurnType == TurnType.C || thirdTurnType == TurnType.C) {
				turnResource = getShortArrow(firstTurnType);
			} else {
				if (firstTurnType == TurnType.TU || firstTurnType == TurnType.TRU) {
					turnResource = getShortArrow(firstTurnType);
				} else {
					turnResource = getTallArrow(firstTurnType, false);
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
				turnResource = getTallArrow(secondTurnType, false);
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
