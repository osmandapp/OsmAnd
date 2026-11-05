package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.router.TurnType;

public class TurnPathHelper {

	//Index of processed turn
	public static final int FIRST_TURN = 1;
	public static final int SECOND_TURN = 2;
	public static final int THIRD_TURN = 3;
	private static final boolean SHOW_STEPS = true;
	
	private static class TurnVariables {
		
		float radEndOfArrow = 44;
		float radInnerCircle = 10;
		float radOuterCircle = radInnerCircle + 8;
	
		float radBottom = radOuterCircle + 10;
		float radStepInter = radOuterCircle + 6;
		float radArrowTriangle1 = radOuterCircle + 7;
	
		float widthStepIn = 8;
		float widthStepInter = 6;
		float widthArrow = 22;
		float radArrowTriangle2;
		private final double dfL;
		private final double dfAr2;
		private final double dfStepInter;
		private final double dfAr;
		private final double dfOut;
		private final double dfStepOut;
		private final double dfIn;
		private final double minDelta;
		private double rot;
		private float cx;
		private float cy;
		private final float scaleTriangle;

		private TurnVariables(boolean leftSide, float turnAngle, int out, int wa, int ha, float scaleTriangle) {
			this.scaleTriangle = scaleTriangle;
			widthArrow = widthArrow * scaleTriangle;
			radArrowTriangle2 = radArrowTriangle1 + 1 * scaleTriangle * scaleTriangle;
			
			dfL = (leftSide ? 1 : -1) * Math.asin(widthStepIn / (2.0 * radBottom));
			dfAr2 = (leftSide ? 1 : -1) * Math.asin(widthArrow / (2.0 * radArrowTriangle2));
			dfStepInter = (leftSide ? 1 : -1) * Math.asin(widthStepInter / radStepInter);
			dfAr = Math.asin(radBottom * Math.sin(dfL) / radArrowTriangle1);
			dfOut = Math.asin(radBottom * Math.sin(dfL) / radOuterCircle);
			dfStepOut = Math.asin(radStepInter * Math.sin(dfStepInter) / radOuterCircle);
			dfIn = Math.asin(radBottom * Math.sin(dfL) / radInnerCircle);
			minDelta = Math.abs(dfIn * 2 / Math.PI * 180) + 2;

			// System.out.println("Angle " + dfL + " " + dfOut + " " + dfIn + " " + minDelta + " ");
			rot = alignRotation(turnAngle, leftSide, minDelta, out) / 180 * Math.PI;

			cx = wa / 2;
			cy = ha / 2;
			// align center
			float potentialArrowEndX = (float) (Math.sin(rot) * radEndOfArrow);
			float potentialArrowEndY = (float) (Math.cos(rot) * radEndOfArrow);
			if (potentialArrowEndX > cx) {
				cx = potentialArrowEndX;
			} else if (potentialArrowEndX < -cx) {
				cx = 2 * cx + potentialArrowEndX;
			}
			if (potentialArrowEndY > cy) {
				cy = 2 * cy - potentialArrowEndY;
			} else if (potentialArrowEndY < -cy) {
				cy = -potentialArrowEndY;
			}
		}
		
		private float getProjX(double angle, double radius) {
			return getX(angle, radius) + cx;
		}
		
		private float getProjY(double angle, double radius) {
			return getY(angle, radius) + cy;
		}
		
		public float getTriangle2X() {
			return getProjX(rot + dfAr, radArrowTriangle1);
		}
		
		public float getTriangle1X() {
			return getProjX(rot - dfAr, radArrowTriangle1);
		}
		
		public float getTriangle2Y() {
			return getProjY(rot + dfAr, radArrowTriangle1);
		}
		
		public float getTriangle1Y() {
			return getProjY(rot - dfAr, radArrowTriangle1);
		}

		public void drawTriangle(Path pathForTurn) {
			// up from arc
			arcLineTo(pathForTurn, rot - dfAr, cx, cy, radArrowTriangle1);
			// left triangle
			// arcLineTo(pathForTurn, rot - dfAr2, cx, cy, radAr2); // 1.
			// arcQuadTo(pathForTurn, rot - dfAr2, radAr2, rot, radArrow, 0.9f, cx, cy); // 2.
			arcQuadTo(pathForTurn, rot - dfAr, radArrowTriangle1, rot - dfAr2, radArrowTriangle2, rot, radEndOfArrow,
					4.5f * scaleTriangle, cx, cy); // 3.
			// arcLineTo(pathForTurn, rot, cx, cy, radArrow); // 1.
			arcQuadTo(pathForTurn, rot - dfAr2, radArrowTriangle2, rot, radEndOfArrow, rot + dfAr2, radArrowTriangle2,
					4.5f, cx, cy);
			// right triangle
			// arcLineTo(pathForTurn, rot + dfAr2, cx, cy, radAr2); // 1.
			arcQuadTo(pathForTurn, rot, radEndOfArrow, rot + dfAr2, radArrowTriangle2, rot + dfAr, radArrowTriangle1,
					4.5f * scaleTriangle, cx, cy);
			arcLineTo(pathForTurn, rot + dfAr, cx, cy, radArrowTriangle1);

		}
		
	}

	// 72x72
	public static void calcTurnPath(Path pathForTurn, Path outlay, TurnType turnType,
									Matrix transform, PointF center, boolean mini,
									boolean shortArrow, boolean noOverlap, boolean smallArrow) {
		if(turnType == null){
			return;
		}
		pathForTurn.reset();
		if(outlay != null) {
			outlay.reset();
		}
		int ha = 72;
		int wa = 72;
		int lowMargin = 6;
		float scaleTriangle = smallArrow ? 1.f : 1.5f;
		int turnTypeId = turnType.getValue();

		if (TurnType.C == turnTypeId) {
			TurnVariables tv = new TurnVariables(false, 0, 0, wa, ha, scaleTriangle);
			pathForTurn.moveTo(wa / 2 + tv.widthStepIn / 2, ha - lowMargin);
			tv.drawTriangle(pathForTurn);
			pathForTurn.lineTo(wa / 2 - tv.widthStepIn / 2, ha - lowMargin);
		} else if (TurnType.OFFR == turnTypeId){
			TurnVariables tv = new TurnVariables(false, 0, 0, wa, ha, scaleTriangle);
			float rightX = wa / 2 + tv.widthStepIn / 2;
			float leftX = wa / 2 - tv.widthStepIn / 2;
			int step = 7;
			
			pathForTurn.moveTo(rightX, ha - lowMargin);
			pathForTurn.rLineTo(0, -step);
			pathForTurn.rLineTo(-tv.widthStepIn , 0);
			pathForTurn.rLineTo(0 , step);
			pathForTurn.rLineTo(tv.widthStepIn, 0);
			
			pathForTurn.moveTo(rightX, ha - 2 * lowMargin - step);
			pathForTurn.rLineTo(0, -step);
			pathForTurn.rLineTo(-tv.widthStepIn , 0);
			pathForTurn.rLineTo(0 , step);
			pathForTurn.rLineTo(tv.widthStepIn, 0);
			
			pathForTurn.moveTo(rightX, ha - 3 * lowMargin - 2 * step);
			pathForTurn.rLineTo(0, -step);
			pathForTurn.rLineTo(-tv.widthStepIn , 0);
			pathForTurn.rLineTo(0 , step);
			pathForTurn.rLineTo(tv.widthStepIn, 0);
			
			pathForTurn.moveTo(rightX, ha - 4 * lowMargin - 3 * step);
			tv.drawTriangle(pathForTurn);
			pathForTurn.lineTo(leftX, ha - 4 * lowMargin - 3 * step);
		} else if (TurnType.TR == turnTypeId|| TurnType.TL == turnTypeId) {
			int b = TurnType.TR == turnTypeId? 1 : -1;
			TurnVariables tv = new TurnVariables(b != 1, b == 1 ? 90 : -90, 0, wa, (shortArrow ? ha : ha / 2), scaleTriangle);
			// calculated
			float rDiv =  (shortArrow ? 4 : noOverlap ? 1 : 2);
			float r = (tv.cy - tv.widthStepIn / 2) / rDiv;
			float centerCurveX = wa / 2 + b * (noOverlap ? 4 : r + tv.widthStepIn / 2);
			float centerCurveY = ha / 2 + (shortArrow ? r + tv.widthStepIn / 2 : !noOverlap ? -r : 0);
			float h = ha - centerCurveY - lowMargin;
			float centerLineX = centerCurveX - b * (r + tv.widthStepIn / 2);
			RectF innerOval = new RectF(centerCurveX - r, centerCurveY - r, centerCurveX + r, centerCurveY + r);
			RectF outerOval = new RectF(innerOval);
			outerOval.inset(-tv.widthStepIn, -tv.widthStepIn);
			
			pathForTurn.moveTo(centerLineX + b * tv.widthStepIn / 2, ha - lowMargin);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.arcTo(innerOval, b == 1 ? -180 : 0, b*  90);
			tv.drawTriangle(pathForTurn);
			pathForTurn.arcTo(outerOval, -90, - b *90);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSLR == turnTypeId || TurnType.TSLL == turnTypeId) {
			int b = TurnType.TSLR == turnTypeId ? 1 : -1;
			float angle = shortArrow ? 65 : 45;
			TurnVariables tv = new TurnVariables(b != 1, b == 1 ? angle : -angle, 0, wa, ha, scaleTriangle);
			tv.cx -= b * (shortArrow ? 0 : 7);
			tv.cy += shortArrow ? 12 : 0;
			float centerBottomX = wa / 2 - (noOverlap ? b * 6 : 0);
			float centerCurveY = shortArrow ? ha - 6 : ha / 2 + 8;
			float centerCurveX = centerBottomX + b * (wa / 2 - (shortArrow && noOverlap ? 6 : 0));
			// calculated
			float rx1 =  Math.abs(centerCurveX - centerBottomX) - tv.widthStepIn / 2;
			float rx2 =  Math.abs(centerCurveX - centerBottomX) + tv.widthStepIn / 2;
			double t1 = Math.acos(Math.abs(tv.getTriangle1X() - centerCurveX) / rx1) ;
			float rb1 = (float) (Math.abs(tv.getTriangle1Y() - centerCurveY) / Math.sin(t1));
			float ellipseAngle1 = (float) (t1 / Math.PI * 180);
			double t2 = Math.acos(Math.abs(tv.getTriangle2X() - centerCurveX) / rx2) ;
			float rb2 = (float) (Math.abs(tv.getTriangle2Y() - centerCurveY) / Math.sin(t2));
			float ellipseAngle2 = (float) (t2 / Math.PI * 180);
			
			RectF innerOval = new RectF(centerCurveX - rx1, centerCurveY - rb1, centerCurveX + rx1, centerCurveY + rb1);
			RectF outerOval = new RectF(centerCurveX - rx2, centerCurveY - rb2, centerCurveX + rx2, centerCurveY + rb2);
			
			pathForTurn.moveTo(centerBottomX + b * tv.widthStepIn / 2, ha - lowMargin);
			pathForTurn.arcTo(innerOval, -90 - b * 90, b * (ellipseAngle1));
			tv.drawTriangle(pathForTurn);
			pathForTurn.arcTo(outerOval, -90 - b * (90 - (ellipseAngle2)), -b * (ellipseAngle2));
			pathForTurn.lineTo(centerBottomX - b * tv.widthStepIn / 2, ha - lowMargin);
		} else if (TurnType.TSHR == turnTypeId || TurnType.TSHL == turnTypeId) {
			int b = TurnType.TSHR == turnTypeId ? 1 : -1;
			float centerCircleY = shortArrow ? ha / 2 : ha / 4;
			float centerCircleX = wa / 2 - (noOverlap ? b * (wa / 5) : 0);
			TurnVariables tv = new TurnVariables(b != 1, b == 1 ? 135 : -135, 0, wa, ha, scaleTriangle);
			// calculated
			float angle = 45;
			float r = tv.widthStepIn / 2;
			tv.cx = centerCircleX;
			tv.cy = centerCircleY;
			if (shortArrow) {
				tv.cx -= b * 2;
				tv.cy -= 2;
			}
			RectF innerOval = new RectF(centerCircleX - r, centerCircleY - r, centerCircleX + r, centerCircleY + r);
			pathForTurn.moveTo(centerCircleX + b * tv.widthStepIn / 2, ha - lowMargin);
			pathForTurn.lineTo(centerCircleX + b * tv.widthStepIn / 2, centerCircleY + 2 * r);
//			pathForTurn.arcTo(innerOval, -90 - b * 90, b * 45);
			tv.drawTriangle(pathForTurn);
//			pathForTurn.lineTo(centerCircleX - b * tv.widthStepIn / 2, (float) (centerCircleY - 2 *r));
			pathForTurn.arcTo(innerOval, -90  + b * angle,  - b * (90 + angle));
			pathForTurn.lineTo(centerCircleX - b * tv.widthStepIn / 2, ha - lowMargin);
		} else if(TurnType.TU == turnTypeId || TurnType.TRU == turnTypeId) {
			int b = TurnType.TU == turnTypeId ? -1 : 1;
			float radius = shortArrow ? 10 : 16;
			float centerRadiusY = ha / 2 + (shortArrow ? 10 : -10);
			float extraMarginBottom = shortArrow ? 0 : 5;
			TurnVariables tv = new TurnVariables(b != 1, 180, 0, wa, ha, scaleTriangle);
			// calculated
			float centerRadiusX = wa / 2 + (shortArrow ? b * radius : 0);
			tv.cx = centerRadiusX + b * radius;
			tv.cy = shortArrow ? ha - centerRadiusY : centerRadiusY - extraMarginBottom;
			lowMargin += extraMarginBottom;
			tv.rot = 0;
			
			float r = radius - tv.widthStepIn / 2;
			float r2 = radius + tv.widthStepIn / 2;
			RectF innerOval = new RectF(centerRadiusX - r, centerRadiusY - r, centerRadiusX + r, centerRadiusY + r);
			RectF outerOval = new RectF(centerRadiusX - r2, centerRadiusY - r2, centerRadiusX + r2, centerRadiusY + r2);
			
			pathForTurn.moveTo(centerRadiusX - b * (radius - tv.widthStepIn / 2), ha - lowMargin);
			pathForTurn.lineTo(centerRadiusX - b * (radius - tv.widthStepIn / 2), centerRadiusY);
			pathForTurn.arcTo(innerOval, -90 - b * 90, b * 180);
			tv.drawTriangle(pathForTurn);
			pathForTurn.arcTo(outerOval, -90 + b * 90, -b * 180);
			pathForTurn.lineTo(centerRadiusX - b * (radius + tv.widthStepIn / 2), ha - lowMargin);
		} else if (TurnType.KL == turnTypeId || TurnType.KR == turnTypeId) {
			int b = TurnType.KR == turnTypeId ? 1 : -1;
			float shiftX = shortArrow ? 12 : 8;
			float firstH = 18;
			float secondH = 20;
			TurnVariables tv = new TurnVariables(false, 0, 0, wa, ha, scaleTriangle);
			// calculated
			tv.cx += b * shiftX * (noOverlap ? 1 : 2);
			float dx = b * shiftX * (noOverlap ? 1 : 2);
			float mdx = -b * shiftX * (noOverlap ? 1 : 0);
			pathForTurn.moveTo(wa / 2 + tv.widthStepIn / 2 + mdx, ha - lowMargin);
			pathForTurn.lineTo(wa / 2 + tv.widthStepIn / 2 + mdx, ha - lowMargin - firstH);
			// pathForTurn.lineTo(wa / 2 + tv.widthStepIn / 2 + dx, ha - lowMargin - firstH - secondH);
			pathForTurn.cubicTo(
					wa / 2 + tv.widthStepIn / 2 + mdx, ha - lowMargin - firstH - secondH / 2 + b * 3,
					wa / 2 + tv.widthStepIn / 2 + dx, ha - lowMargin - firstH - secondH / 2 + b * 3,
					wa / 2 + tv.widthStepIn / 2 + dx, ha - lowMargin - firstH - secondH);
			tv.drawTriangle(pathForTurn);
			pathForTurn.lineTo(wa / 2 - tv.widthStepIn / 2 + dx, ha - lowMargin - firstH - secondH);
			pathForTurn.cubicTo(
					wa / 2 - tv.widthStepIn / 2 + dx, ha - lowMargin - firstH - secondH / 2 - b * 2,
					wa / 2 - tv.widthStepIn / 2 + mdx, ha - lowMargin - firstH - secondH / 2 - b * 2,
					wa / 2 - tv.widthStepIn / 2 + mdx, ha - lowMargin - firstH );
//			pathForTurn.lineTo(wa / 2 - tv.widthStepIn / 2 + mdx, ha - lowMargin - firstH);
			pathForTurn.lineTo(wa / 2 - tv.widthStepIn / 2 + mdx, ha - lowMargin);
		} else if(turnType != null && turnType.isRoundAbout() ) {
			int out = turnType.getExitOut();
			boolean leftSide = turnType.isLeftSide();
			boolean showSteps = SHOW_STEPS && !mini;
			TurnVariables tv = new TurnVariables(leftSide, turnType.getTurnAngle(), out, wa, ha, 1);
			if(center != null) {
				center.set(tv.cx, tv.cy);
			}
			RectF qrOut = new RectF(tv.cx - tv.radOuterCircle, tv.cy - tv.radOuterCircle,
					tv.cx + tv.radOuterCircle, tv.cy + tv.radOuterCircle);
			RectF qrIn = new RectF(tv.cx - tv.radInnerCircle, tv.cy - tv.radInnerCircle, tv.cx + tv.radInnerCircle, tv.cy + tv.radInnerCircle);
			if(outlay != null && !mini) {
				outlay.addArc(qrOut, 0, 360);
				outlay.addArc(qrIn, 0, -360);
//				outlay.addOval(qrOut, Direction.CCW);
//				outlay.addOval(qrIn, Direction.CW);
			}
			
			// move to bottom ring
			pathForTurn.moveTo(tv.getProjX(tv.dfOut, tv.radOuterCircle), tv.getProjY(tv.dfOut, tv.radOuterCircle));
			if (out <= 1) {
				showSteps = false;
			}
			if (showSteps && outlay != null) {
				double totalStepInter = (out - 1) * tv.dfStepOut;
				double st = (tv.rot - 2 * tv.dfOut - totalStepInter) / out;
				if ((tv.rot > 0) != (st > 0)) {
					showSteps = false;
				}
				if (Math.abs(st) < Math.PI / 60) {
					showSteps = false;
				}
				// double st = (rot - 2 * dfOut ) / (2 * out - 1);
				// dfStepOut = st;
				if (showSteps) {
					outlay.moveTo(tv.getProjX(tv.dfOut, tv.radOuterCircle), tv.getProjY(tv.dfOut, tv.radOuterCircle));
					for (int i = 0; i < out - 1; i++) {
						outlay.arcTo(qrOut, startArcAngle(tv.dfOut + i * (st + tv.dfStepOut)), sweepArcAngle(st));
						arcLineTo(outlay,
								tv.dfOut + (i + 1) * (st + tv.dfStepOut) - tv.dfStepOut / 2 - tv.dfStepInter / 2,
								tv.cx, tv.cy, tv.radStepInter);
						arcLineTo(outlay, tv.dfOut + (i + 1) * (st + tv.dfStepOut) - tv.dfStepOut / 2 + tv.dfStepInter / 2,
								tv.cx, tv.cy, tv.radStepInter);
						arcLineTo(outlay, tv.dfOut + (i + 1) * (st + tv.dfStepOut), tv.cx, tv.cy, tv.radOuterCircle);
						// pathForTurn.arcTo(qr1, startArcAngle(dfOut), sweepArcAngle(rot - dfOut - dfOut));
					}
					outlay.arcTo(qrOut, startArcAngle(tv.rot - tv.dfOut - st), sweepArcAngle(st));
					// swipe back
					arcLineTo(outlay, tv.rot - tv.dfIn, tv.cx, tv.cy, tv.radInnerCircle);
					outlay.arcTo(qrIn, startArcAngle(tv.rot - tv.dfIn), -sweepArcAngle(tv.rot - tv.dfIn - tv.dfIn));
				}
			}
//			if(!showSteps) {
//				// arc
//				pathForTurn.arcTo(qrOut, startArcAngle(dfOut), sweepArcAngle(rot - dfOut - dfOut));
//			}
			pathForTurn.arcTo(qrOut, startArcAngle(tv.dfOut), sweepArcAngle(tv.rot - tv.dfOut - tv.dfOut));
			
			tv.drawTriangle(pathForTurn);
			// down to arc
			arcLineTo(pathForTurn, tv.rot + tv.dfIn, tv.cx, tv.cy, tv.radInnerCircle);
			// arc
			pathForTurn.arcTo(qrIn, startArcAngle(tv.rot + tv.dfIn), sweepArcAngle(-tv.rot - tv.dfIn - tv.dfIn));
			// down
			arcLineTo(pathForTurn, -tv.dfL, tv.cx, tv.cy, tv.radBottom);
			// left
			arcLineTo(pathForTurn, tv.dfL, tv.cx, tv.cy, tv.radBottom);
		}
		pathForTurn.close();
		if(transform != null){
			pathForTurn.transform(transform);
		}
	}
	
	private static float alignRotation(float t, boolean leftSide, double minDelta, int out) {
		// t between ]-180, 180]
		while(t > 180) {
			t -= 360;
		}
		while(t <= -180) {
			t += 360;
		}
		// rot left - ] 0, 360], right ] -360,0]
		float rot = leftSide ? (t + 180) : (t - 180) ;
		if(rot == 0) {
			rot = leftSide ? 360 : -360;
		}
		float delta = (float) minDelta;
		if(rot > 360 - delta && rot <= 360) {
			rot = 360 - delta;
			if(out < 2) {
				rot = delta;
			}
		} else if (rot < -360 + delta && rot >= -360) {
			rot = -360 + delta;
			if(out < 2) {
				rot = -delta;
			}
		} else if (rot >= 0 && rot < delta) {
			rot = delta;
			if(out > 2) {
				rot = 360 - delta;
			}
		} else if (rot <= 0 && rot > -delta) {
			rot = -delta;
			if(out > 2) {
				rot = -360 + delta;
			}
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
			float dl, float cx, float cy) {
		float X0 = getProjX(angle0, cx, cy, radius0);
		float Y0 = getProjY(angle0, cx, cy, radius0);
		float X = getProjX(angle, cx, cy, radius);
		float Y = getProjY(angle, cx, cy, radius);
		float X2 = getProjX(angle2, cx, cy, radius2);
		float Y2 = getProjY(angle2, cx, cy, radius2);
		float l2 = (float) Math.sqrt((X-X2)*(X-X2) + (Y-Y2)*(Y-Y2));
		float l0 = (float) Math.sqrt((X-X0)*(X-X0) + (Y-Y0)*(Y-Y0));
		float proc2 = dl / l2;
		float proc = dl / l0;
		pathForTurn.lineTo(X0 * proc + X * (1 - proc), Y0 * proc + Y * (1 - proc));
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
		Paint paintRouteDirectionOutlay;
		Path p = new Path();
		Path dp = new Path();
		Path pOutlay = new Path();
		Path dpOutlay = new Path();
		private final boolean mini;

		public RouteDrawable(@NonNull Context ctx, boolean mini) {
			this.mini = mini;
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_distant));
			paintRouteDirection.setAntiAlias(true);
			paintRouteDirectionOutlay = new Paint();
			paintRouteDirectionOutlay.setStyle(Style.STROKE);
			paintRouteDirectionOutlay.setColor(Color.BLACK);
			paintRouteDirectionOutlay.setAntiAlias(true);
			calcTurnPath(dp, dpOutlay, TurnType.straight(), null, null, mini,
					false, true, false);
		}

		@Override
		protected void onBoundsChange(Rect bounds) {
			Matrix m = new Matrix();
			m.setScale(bounds.width() / 72f, bounds.height() / 72f);
			p.transform(m, dp);
			pOutlay.transform(m, dpOutlay);
		}
		
		public void setRouteType(TurnType t){
			calcTurnPath(p, pOutlay, t, null, null, mini,
					false, true, false);
			onBoundsChange(getBounds());
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			canvas.drawPath(dpOutlay, paintRouteDirectionOutlay);
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

		int turnType;
		boolean shortArrow;
		boolean noOverlap;
		boolean leftSide;

		TurnResource() {}

		TurnResource(int turnType, boolean shortArrow, boolean noOverlap, boolean leftSide) {
			this.turnType = turnType == 0 ? 1 : turnType;
			this.shortArrow = shortArrow;
			this.noOverlap = noOverlap;
			this.leftSide = leftSide;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			TurnResource other = (TurnResource) obj;
			return other.turnType == turnType && other.shortArrow == shortArrow
					&& other.noOverlap == noOverlap && other.leftSide == leftSide;
		}

		@Override
		public int hashCode() {
			return (turnType + (noOverlap ? 100 : 1) + (shortArrow ? 1000 : 1)) * (leftSide ? -1 : 1);
		}
	}

	public static Path getPathFromTurnType(int firstTurn, int secondTurn, int thirdTurn, int turnIndex,
										   float size, boolean leftSide, boolean smallArrow) {
		int firstTurnType = TurnType.valueOf(firstTurn, leftSide).getValue();
		int secondTurnType = TurnType.valueOf(secondTurn, leftSide).getValue();
		int thirdTurnType = TurnType.valueOf(thirdTurn, leftSide).getValue();

		TurnResource turnResource = null;

		if (turnIndex == FIRST_TURN) {
			if (secondTurnType == 0) {
				turnResource = new TurnResource(firstTurnType, false, false, leftSide);
			} else if (secondTurnType == TurnType.C || thirdTurnType == TurnType.C
					|| ((firstTurnType == TurnType.TL || firstTurnType == TurnType.TR)
					&& (secondTurnType == TurnType.TSLL || secondTurnType == TurnType.TSLR))) {
				turnResource = new TurnResource(firstTurnType, true, false, leftSide);
			} else {
				if (firstTurnType == TurnType.TU || firstTurnType == TurnType.TRU) {
					turnResource = new TurnResource(firstTurnType, true, false, leftSide);
				} else {
					turnResource = new TurnResource(firstTurnType, false, false, leftSide);
				}
			}
		} else if (turnIndex == SECOND_TURN) {
			if (TurnType.isLeftTurn(firstTurnType) && TurnType.isLeftTurn(secondTurnType)) {
				if (TurnType.isSlightTurn(firstTurnType)) {
					turnResource = new TurnResource(secondTurnType, true, false, leftSide);
				} else if (TurnType.isSlightTurn(secondTurnType)) {
					turnResource = new TurnResource(secondTurnType, false, false, leftSide);
				} else {
					turnResource = null;
				}
			} else if (TurnType.isRightTurn(firstTurnType) && TurnType.isRightTurn(secondTurnType)) {
				if (TurnType.isSlightTurn(firstTurnType)) {
					turnResource = new TurnResource(secondTurnType, true, false, leftSide);
				} else if (TurnType.isSlightTurn(secondTurnType)) {
					turnResource = new TurnResource(secondTurnType, false, false, leftSide);
				} else {
					turnResource = null;
				}
			} else if (firstTurnType != TurnType.C && TurnType.isSlightTurn(firstTurnType)
					&& !TurnType.isSlightTurn(secondTurnType) && !TurnType.isSlightTurn(thirdTurnType)) {
				if (TurnType.isLeftTurn(firstTurnType) && TurnType.isRightTurn(secondTurnType)
						&& (thirdTurnType == 0 || TurnType.isLeftTurn(thirdTurnType))) {
					turnResource = new TurnResource(secondTurnType, true, false, leftSide);
				} else if (TurnType.isRightTurn(firstTurnType) && TurnType.isLeftTurn(secondTurnType)
						&& (thirdTurnType == 0 || TurnType.isRightTurn(thirdTurnType))) {
					turnResource = new TurnResource(secondTurnType, true, false, leftSide);
				}
			} else if (firstTurnType == TurnType.C || thirdTurnType == TurnType.C) {
				// get the small one
				turnResource = new TurnResource(secondTurnType, true, false, leftSide);
			} else {
				turnResource = new TurnResource(secondTurnType, false, false, leftSide);
			}
		} else if (turnIndex == THIRD_TURN) {
			if (((TurnType.isLeftTurn(firstTurnType) && !TurnType.isSlightTurn(firstTurnType))
					|| TurnType.isLeftTurn(secondTurnType)) && TurnType.isLeftTurn(thirdTurnType)) {
				turnResource = null;
			} else if (((TurnType.isRightTurn(firstTurnType) && !TurnType.isSlightTurn(firstTurnType))
					|| TurnType.isRightTurn(secondTurnType)) && TurnType.isRightTurn(thirdTurnType)) {
				turnResource = null;
			} else {
				turnResource = new TurnResource(thirdTurnType, true, false, leftSide);
			}
		}
		if (turnResource == null) {
			return null;
		}
		return getPathFromTurnResource(turnResource, size, smallArrow);
	}

	private static Path getPathFromTurnResource(TurnResource turnResource, float size, boolean smallArrow) {
		float coef = size / 72.f;
		Path p = new Path();
		Matrix matrix = new Matrix();
		matrix.postScale(coef, coef);
		calcTurnPath(p, null, TurnType.valueOf(turnResource.turnType, turnResource.leftSide),
				matrix, null, false, turnResource.shortArrow, turnResource.noOverlap, smallArrow);
		return p;
	}


}
