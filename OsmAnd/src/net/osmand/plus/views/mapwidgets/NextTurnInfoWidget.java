package net.osmand.plus.views.mapwidgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;



public class NextTurnInfoWidget extends TextInfoWidget {

	protected boolean horisontalMini;
	
	protected int deviatedPath = 0;
	protected int nextTurnDistance = 0;
	
	private TurnDrawable turnDrawable;
	private OsmandApplication app;
	

	public NextTurnInfoWidget(Activity activity, OsmandApplication app, boolean horisontalMini) {
		super(activity);
		this.app = app;
		this.horisontalMini = horisontalMini;
		turnDrawable = new TurnDrawable(activity, horisontalMini);
		if(horisontalMini) {
			setImageDrawable(turnDrawable, false);
			setTopImageDrawable(null, null);
		} else {
			setImageDrawable(null, true);
			setTopImageDrawable(turnDrawable, "");
		}
	}
	
	public TurnType getTurnType() {
		return turnDrawable.turnType;
	}
	
	public void setTurnType(TurnType turnType) {
		boolean vis = updateVisibility(turnType != null);
		if (turnDrawable.setTurnType(turnType) || vis) {
			turnDrawable.setTextPaint(topTextView.getPaint());
			if(horisontalMini) {
				setImageDrawable(turnDrawable, false);
			} else {
				setTopImageDrawable(turnDrawable, "");
//				setTopImageDrawable(turnDrawable, turnType == null || turnType.getExitOut() == 0 ? "" : 
//					turnType.getExitOut() + "");
			}
		}
	}
	
	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		if(turnDrawable.turnImminent != turnImminent || turnDrawable.deviatedFromRoute != deviatedFromRoute) {
			turnDrawable.setTurnImminent(turnImminent, deviatedFromRoute);
		}
	}
	
	public void setDeviatePath(int deviatePath) {
		if (RouteInfoWidgetsFactory.distChanged(deviatePath, this.deviatedPath)) {
			this.deviatedPath = deviatePath;
			updateDistance();
		}
	}

	public void setTurnDistance(int nextTurnDistance) {
		if (RouteInfoWidgetsFactory.distChanged(nextTurnDistance, this.nextTurnDistance)) {
			this.nextTurnDistance = nextTurnDistance;
			updateDistance();
		}		
	}

	private void updateDistance() {
		int deviatePath = turnDrawable.deviatedFromRoute ? deviatedPath : nextTurnDistance;
		String ds = OsmAndFormatter.getFormattedDistance(deviatePath, app);

		if (ds != null) {
			TurnType turnType = getTurnType();
			RoutingHelper routingHelper = app.getRoutingHelper();
			if ((turnType != null) && (routingHelper != null)) {
				setContentDescription(ds + " " + routingHelper.getRoute().toString(turnType, app, false));
			} else {
				setContentDescription(ds);
			}
		}

		int ls = ds.lastIndexOf(' ');
		if (ls == -1) {
			setTextNoUpdateVisibility(ds, null);
		} else {
			setTextNoUpdateVisibility(ds.substring(0, ls), ds.substring(ls + 1));
		}
	}
	
	@Override
	public boolean updateInfo(DrawSettings drawSettings) {
		return false;
	}
	
	public static class TurnDrawable extends Drawable {
		protected Paint paintBlack;
		protected Paint paintRouteDirection;
		protected Path pathForTurn = new Path();
		protected Path pathForTurnOutlay = new Path();
		protected TurnType turnType = null;
		protected int turnImminent;
		protected boolean deviatedFromRoute;
		private Context ctx;
		private boolean mini;
		private PointF centerText;
		private TextPaint textPaint;
		private int clr;
		
		public TurnDrawable(Context ctx, boolean mini) {
			this.ctx = ctx;
			this.mini = mini;
			centerText = new PointF();
			paintBlack = new Paint();
			paintBlack.setStyle(Style.STROKE);
			paintBlack.setColor(Color.BLACK);
			paintBlack.setAntiAlias(true);
			paintBlack.setStrokeWidth(2.5f);
			

			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL);
			paintRouteDirection.setAntiAlias(true);
			setColor(R.color.nav_arrow);
		}
		
		@ColorRes
		public void setColor(int clr) {
			if(clr != this.clr) {
				this.clr = clr;
				paintRouteDirection.setColor(ctx.getResources().getColor(clr));
			}
		}
		
		@Override
		protected void onBoundsChange(Rect bounds) {
			Matrix m = new Matrix();
			float scaleX = bounds.width() / 72f;
			float scaleY = bounds.height() / 72f;
			m.setScale(scaleX, scaleY);
			pathForTurn.transform(m, pathForTurn);
			centerText.x = scaleX * centerText.x;
			centerText.y = scaleY * centerText.y;
			pathForTurnOutlay.transform(m, pathForTurnOutlay);
		}
		
		public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
			//if user deviates from route that we should draw grey arrow
			this.turnImminent = turnImminent;
			this.deviatedFromRoute = deviatedFromRoute;
			if (deviatedFromRoute){
				paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
			} else if (turnImminent > 0) {
				paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow));
			} else if (turnImminent == 0) {
				paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_imminent));
			} else {
				paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
			}
			invalidateSelf();	
			
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			/// small indent
			// canvas.translate(0, 3 * scaleCoefficient);
			canvas.drawPath(pathForTurnOutlay, paintBlack);
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			if(textPaint != null ) {
				if (turnType != null && !mini && turnType.getExitOut() > 0) {
					canvas.drawText(turnType.getExitOut() + "", centerText.x, 
							centerText.y - textPaint.ascent() / 2, textPaint);
				}
			}
		}
		
		public void setTextPaint(TextPaint textPaint) {
			this.textPaint = textPaint;
			this.textPaint.setTextAlign(Align.CENTER);
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		public boolean setTurnType(TurnType turnType) {
			if(turnType != this.turnType) {
				this.turnType = turnType;
				TurnPathHelper.calcTurnPath(pathForTurn, pathForTurnOutlay, turnType, null,
						centerText, mini, false, true, false);
				onBoundsChange(getBounds());
				return true;
			}
			return false;
		}
		
	}
}