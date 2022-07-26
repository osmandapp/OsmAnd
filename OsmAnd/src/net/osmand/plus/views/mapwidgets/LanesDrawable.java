package net.osmand.plus.views.mapwidgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;

import java.util.ArrayList;
import java.util.List;

public class LanesDrawable extends Drawable {

	public int[] lanes;
	public boolean imminent;
	public boolean isTurnByTurn;
	public boolean isNightMode;
	private final Context ctx;
	private final Paint paintBlack;
	private final Paint paintRouteDirection;
	private final Paint paintSecondTurn;
	private final float size;

	private float delta;
	private final boolean leftSide;
	private final float imgMinDelta;
	private final float imgMargin;
	private final float laneHalfSize;

	private int height;
	private int width;

	public LanesDrawable(@NonNull Context ctx, float strokeWidth) {
		this(ctx, strokeWidth, ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_size),
				ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_min_delta),
				ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_margin),
				ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_size));
	}

	public LanesDrawable(@NonNull Context ctx, float strokeWidth, float size, float imgMinDeltaPx, float imgMarginPx, float laneSizePx) {
		this.ctx = ctx;
		OsmandSettings settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
		leftSide = settings.DRIVING_REGION.get().leftHandDriving;
		imgMinDelta = imgMinDeltaPx;
		imgMargin = imgMarginPx;
		laneHalfSize = laneSizePx / 2f;
		this.size = size;

		paintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintBlack.setStyle(Paint.Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setStrokeWidth(strokeWidth);

		paintRouteDirection = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintRouteDirection.setStyle(Paint.Style.FILL);
		paintRouteDirection.setColor(ContextCompat.getColor(ctx, R.color.nav_arrow));

		paintSecondTurn = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintSecondTurn.setStyle(Paint.Style.FILL);
		paintSecondTurn.setColor(ContextCompat.getColor(ctx, R.color.nav_arrow_distant));
	}

	public void updateBounds() {
		float w = 0;
		float h = 0;
		float delta = imgMinDelta;
		if (lanes != null) {
			List<RectF> boundsList = new ArrayList<>(lanes.length);
			for (int lane : lanes) {
				int turnType = TurnType.getPrimaryTurn(lane);
				int secondTurnType = TurnType.getSecondaryTurn(lane);
				int thirdTurnType = TurnType.getTertiaryTurn(lane);

				RectF imgBounds = new RectF();
				if (thirdTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
							TurnPathHelper.THIRD_TURN, size, leftSide, true);
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
					Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
							TurnPathHelper.SECOND_TURN, size, leftSide, true);
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
				Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
						TurnPathHelper.FIRST_TURN, size, leftSide, true);
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
			canvas.save();
			// canvas.translate((int) (16 * scaleCoefficient), 0);
			for (int i = 0; i < lanes.length; i++) {
				if ((lanes[i] & 1) == 1) {
					if (isTurnByTurn) {
						paintRouteDirection.setColor(ColorUtilities.getActiveColor(ctx, isNightMode));
					} else {
						paintRouteDirection.setColor(imminent ? ContextCompat.getColor(ctx, R.color.nav_arrow_imminent) :
								ContextCompat.getColor(ctx, R.color.nav_arrow));
					}
				} else {
					paintRouteDirection.setColor(ContextCompat.getColor(ctx, R.color.nav_arrow_distant));
				}
				int turnType = TurnType.getPrimaryTurn(lanes[i]);
				int secondTurnType = TurnType.getSecondaryTurn(lanes[i]);
				int thirdTurnType = TurnType.getTertiaryTurn(lanes[i]);

				RectF imgBounds = new RectF();
				Path thirdTurnPath = null;
				Path secondTurnPath = null;
				Path firstTurnPath = null;

				if (thirdTurnType > 0) {
					Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
							TurnPathHelper.THIRD_TURN, size, leftSide, true);
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
					Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
							TurnPathHelper.SECOND_TURN, size, leftSide, true);
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
				Path p = TurnPathHelper.getPathFromTurnType(turnType, secondTurnType, thirdTurnType,
						TurnPathHelper.FIRST_TURN, size, leftSide, true);
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

	@SuppressLint("WrongConstant")
	@Override
	public int getOpacity() {
		return 0;
	}

}
