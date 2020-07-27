package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;

import java.util.ArrayList;
import java.util.List;

public class LanesDrawable extends Drawable {

	public int[] lanes = null;
	boolean imminent = false;
	public boolean isTurnByTurn = false;
	public boolean isNightMode = false;
	private Context ctx;
	private Paint paintBlack;
	private Paint paintRouteDirection;
	private Paint paintSecondTurn;
	private float scaleCoefficient;
	private int height;
	private int width;
	private float delta;
	private float laneHalfSize;
	private static final float miniCoeff = 2f;
	private final boolean leftSide;
	private int imgMinDelta;
	private int imgMargin;

	public LanesDrawable(MapActivity ctx, float scaleCoefficent) {
		this.ctx = ctx;
		OsmandSettings settings = ctx.getMyApplication().getSettings();
		leftSide = settings.DRIVING_REGION.get().leftHandDriving;
		imgMinDelta = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_min_delta);
		imgMargin = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_margin);
		laneHalfSize = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_size) / 2;

		this.scaleCoefficient = scaleCoefficent;

		paintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintBlack.setStyle(Paint.Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setStrokeWidth(scaleCoefficent);

		paintRouteDirection = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintRouteDirection.setStyle(Paint.Style.FILL);
		paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow));

		paintSecondTurn = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintSecondTurn.setStyle(Paint.Style.FILL);
		paintSecondTurn.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
	}

	public void updateBounds() {
		float w = 0;
		float h = 0;
		float delta = imgMinDelta;
		float coef = scaleCoefficient / miniCoeff;
		if (lanes != null) {
			List<RectF> boundsList = new ArrayList<>(lanes.length);
			for (int i = 0; i < lanes.length; i++) {
				int turnType = TurnType.getPrimaryTurn(lanes[i]);
				int secondTurnType = TurnType.getSecondaryTurn(lanes[i]);
				int thirdTurnType = TurnType.getTertiaryTurn(lanes[i]);

				RectF imgBounds = new RectF();
				if (thirdTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.THIRD_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
						}
					}
				}
				if (secondTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.SECOND_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
						}
					}
				}
				Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
						secondTurnType, thirdTurnType, TurnPathHelper.FIRST_TURN, coef, leftSide, true);
				if (p != null) {
					RectF b = new RectF();
					p.computeBounds(b, true);
					if (!b.isEmpty()) {
						if (imgBounds.isEmpty()) {
							imgBounds.set(b);
						} else {
							imgBounds.union(b);
						}
					}
				}
				if (imgBounds.right > 0) {
					boundsList.add(imgBounds);

					float imageHeight = imgBounds.bottom;
					if (imageHeight > h)
						h = imageHeight;
				}
			}
			if (boundsList.size() > 1) {
				for (int i = 1; i < boundsList.size(); i++) {
					RectF b1 = boundsList.get(i - 1);
					RectF b2 = boundsList.get(i);
					float d = b1.right + imgMargin * 2 - b2.left;
					if (delta < d)
						delta = d;
				}
				RectF b1 = boundsList.get(0);
				RectF b2 = boundsList.get(boundsList.size() - 1);
				w = -b1.left + (boundsList.size() - 1) * delta + b2.right;
			} else if (boundsList.size() > 0) {
				RectF b1 = boundsList.get(0);
				w = b1.width();
			}
			if (w > 0) {
				w += 4;
			}
			if (h > 0) {
				h += 4;
			}
		}
		this.width = (int) w;
		this.height = (int) h;
		this.delta = delta;
	}

	@Override
	public int getIntrinsicHeight() {
		return height;
	}

	@Override
	public int getIntrinsicWidth() {
		return width;
	}


	@Override
	public void draw(@NonNull Canvas canvas) {
		// setup default color
		//canvas.drawColor(0, PorterDuff.Mode.CLEAR);

		//to change color immediately when needed
		if (lanes != null && lanes.length > 0) {
			float coef = scaleCoefficient / miniCoeff;
			canvas.save();
			// canvas.translate((int) (16 * scaleCoefficient), 0);
			for (int i = 0; i < lanes.length; i++) {
				if ((lanes[i] & 1) == 1) {
					if (isTurnByTurn) {
						paintRouteDirection.setColor(isNightMode ? ctx.getResources().getColor(R.color.active_color_primary_dark) :
								ctx.getResources().getColor(R.color.active_color_primary_light));
					} else {
						paintRouteDirection.setColor(imminent ? ctx.getResources().getColor(R.color.nav_arrow_imminent) :
								ctx.getResources().getColor(R.color.nav_arrow));
					}
				} else {
					paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
				}
				int turnType = TurnType.getPrimaryTurn(lanes[i]);
				int secondTurnType = TurnType.getSecondaryTurn(lanes[i]);
				int thirdTurnType = TurnType.getTertiaryTurn(lanes[i]);

				RectF imgBounds = new RectF();
				Path thirdTurnPath = null;
				Path secondTurnPath = null;
				Path firstTurnPath = null;

				if (thirdTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.THIRD_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
							thirdTurnPath = p;
						}
					}
				}
				if (secondTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.SECOND_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
							secondTurnPath = p;
						}
					}
				}
				Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
						secondTurnType, thirdTurnType, TurnPathHelper.FIRST_TURN, coef, leftSide, true);
				if (p != null) {
					RectF b = new RectF();
					p.computeBounds(b, true);
					if (!b.isEmpty()) {
						if (imgBounds.isEmpty()) {
							imgBounds.set(b);
						} else {
							imgBounds.union(b);
						}
						firstTurnPath = p;
					}
				}

				if (firstTurnPath != null || secondTurnPath != null || thirdTurnPath != null) {
					if (i == 0) {
						imgBounds.set(imgBounds.left - 2, imgBounds.top, imgBounds.right + 2, imgBounds.bottom);
						canvas.translate(-imgBounds.left, 0);
					} else {
						canvas.translate(-laneHalfSize, 0);
					}

					// 1st pass
					if (thirdTurnPath != null) {
						//canvas.drawPath(thirdTurnPath, paintSecondTurn);
						canvas.drawPath(thirdTurnPath, paintBlack);
					}
					if (secondTurnPath != null) {
						//canvas.drawPath(secondTurnPath, paintSecondTurn);
						canvas.drawPath(secondTurnPath, paintBlack);
					}
					if (firstTurnPath != null) {
						//canvas.drawPath(firstTurnPath, paintRouteDirection);
						canvas.drawPath(firstTurnPath, paintBlack);
					}

					// 2nd pass
					if (thirdTurnPath != null) {
						canvas.drawPath(thirdTurnPath, paintSecondTurn);
					}
					if (secondTurnPath != null) {
						canvas.drawPath(secondTurnPath, paintSecondTurn);
					}
					if (firstTurnPath != null) {
						canvas.drawPath(firstTurnPath, paintRouteDirection);
					}

					canvas.translate(laneHalfSize + delta, 0);
				}
			}
			canvas.restore();
		}
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}

	@Override
	public int getOpacity() {
		return 0;
	}

}
