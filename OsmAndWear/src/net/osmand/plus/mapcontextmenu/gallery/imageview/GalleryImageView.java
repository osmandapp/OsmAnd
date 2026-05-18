package net.osmand.plus.mapcontextmenu.gallery.imageview;

import static android.view.GestureDetector.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.OverScroller;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class GalleryImageView extends AppCompatImageView {
	private static final float FINAL_SCALE_MIN_MULTIPLIER = .50f;
	private static final float FINAL_SCALE_MAX_MULTIPLIER = 1.5f;
	private static final float MAX_USER_SCALE = 3f;
	private static final float MIN_USER_SCALE = 1f;
	private static final float ZOOM_TIME = 400;

	private Context ctx;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	private OnDoubleTapListener doubleTapListener = null;
	private OnTouchListener userTouchListener = null;

	private float normalizedScale;
	private Matrix currentMatrix;
	private Matrix previousMatrix;
	private float minScale;
	private float maxScale;
	private float[] matrix;

	private ZoomParams delayedZoomParams;
	private State state;
	private ScaleType scaleType;
	private GalleryImageFling galleryImageFling;

	private int viewWidth, viewHeight;
	private int previousViewWidth, previousViewHeight;
	private float matchViewWidth, matchViewHeight;
	private float previousMatchViewWidth, previousMatchViewHeight;

	private boolean imageRenderedAtLeastOnce;
	private boolean onDrawReady;

	public GalleryImageView(@NonNull Context context) {
		this(context, null);
	}

	public GalleryImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GalleryImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initView(@NonNull Context ctx) {
		setClickable(true);
		this.ctx = ctx;

		scaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
		gestureDetector = new GestureDetector(ctx, new GestureListener());

		currentMatrix = new Matrix();
		previousMatrix = new Matrix();
		matrix = new float[9];
		normalizedScale = 1;

		scaleType = ScaleType.FIT_CENTER;
		minScale = MIN_USER_SCALE;
		maxScale = MAX_USER_SCALE;

		setImageMatrix(currentMatrix);
		setScaleType(ScaleType.MATRIX);
		setState(State.NONE);
		onDrawReady = false;

		super.setOnTouchListener(new GalleryImageOnTouchListener());
	}

	@Override
	public void setOnTouchListener(OnTouchListener listener) {
		userTouchListener = listener;
	}

	public void setOnDoubleTapListener(@Nullable OnDoubleTapListener listener) {
		doubleTapListener = listener;
	}

	@Override
	public void setImageResource(@DrawableRes int resId) {
		super.setImageResource(resId);
		savePreviousImageValues();
		fitImageToView();
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		savePreviousImageValues();
		fitImageToView();
	}

	@Override
	public void setImageDrawable(@Nullable Drawable drawable) {
		super.setImageDrawable(drawable);
		savePreviousImageValues();
		fitImageToView();
	}

	@Override
	public void setImageURI(@Nullable Uri uri) {
		super.setImageURI(uri);
		savePreviousImageValues();
		fitImageToView();
	}

	@Override
	public void setScaleType(ScaleType type) {
		if (type == ScaleType.FIT_START || type == ScaleType.FIT_END) {
			throw new UnsupportedOperationException("Unsupported ScaleType");
		}
		if (type == ScaleType.MATRIX) {
			super.setScaleType(ScaleType.MATRIX);
		} else {
			scaleType = type;
			if (onDrawReady) {
				setInitZoom();
			}
		}
	}

	@Override
	public ScaleType getScaleType() {
		return scaleType;
	}

	private boolean isZoomed() {
		return normalizedScale != 1;
	}

	private void savePreviousImageValues() {
		if (currentMatrix != null && viewHeight != 0 && viewWidth != 0) {
			currentMatrix.getValues(matrix);
			previousMatrix.setValues(matrix);
			previousMatchViewHeight = matchViewHeight;
			previousMatchViewWidth = matchViewWidth;
			previousViewHeight = viewHeight;
			previousViewWidth = viewWidth;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		onDrawReady = true;
		imageRenderedAtLeastOnce = true;
		if (delayedZoomParams != null) {
			setZoom(delayedZoomParams.scale, delayedZoomParams.focusX, delayedZoomParams.focusY, delayedZoomParams.scaleType);
			delayedZoomParams = null;
		}
		super.onDraw(canvas);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		savePreviousImageValues();
	}

	private float getCurrentZoom() {
		return normalizedScale;
	}

	public void resetZoom() {
		normalizedScale = 1;
		fitImageToView();
	}

	private void setZoom(float scale, float focusX, float focusY, @NonNull ScaleType scaleType) {
		if (!onDrawReady) {
			delayedZoomParams = new ZoomParams(scale, focusX, focusY, scaleType);
			return;
		}

		if (scaleType != this.scaleType) {
			setScaleType(scaleType);
		}
		resetZoom();
		scaleImage(scale, (float) viewWidth / 2, (float) viewHeight / 2, true);
		currentMatrix.getValues(matrix);
		matrix[Matrix.MTRANS_X] = -((focusX * getImageWidth()) - (viewWidth * 0.5f));
		matrix[Matrix.MTRANS_Y] = -((focusY * getImageHeight()) - (viewHeight * 0.5f));
		currentMatrix.setValues(matrix);
		fixTrans();
		setImageMatrix(currentMatrix);
	}

	private void setInitZoom() {
		PointF center = getScrollPosition();
		if (center != null) {
			setZoom(getCurrentZoom(), center.x, center.y, getScaleType());
		}
	}

	private PointF getScrollPosition() {
		Drawable drawable = getDrawable();
		if (drawable == null) {
			return null;
		}
		int drawableWidth = drawable.getIntrinsicWidth();
		int drawableHeight = drawable.getIntrinsicHeight();

		PointF point = transformCoordTouchToBitmap((float) viewWidth / 2, (float) viewHeight / 2, true);
		point.x /= drawableWidth;
		point.y /= drawableHeight;
		return point;
	}

	private void fixTrans() {
		currentMatrix.getValues(matrix);
		float transX = matrix[Matrix.MTRANS_X];
		float transY = matrix[Matrix.MTRANS_Y];

		float fixTransX = getFixTrans(transX, viewWidth, getImageWidth());
		float fixTransY = getFixTrans(transY, viewHeight, getImageHeight());

		if (fixTransX != 0 || fixTransY != 0) {
			currentMatrix.postTranslate(fixTransX, fixTransY);
		}
	}

	private void fixScaleTrans() {
		fixTrans();
		currentMatrix.getValues(matrix);
		if (getImageWidth() < viewWidth) {
			matrix[Matrix.MTRANS_X] = (viewWidth - getImageWidth()) / 2;
		}

		if (getImageHeight() < viewHeight) {
			matrix[Matrix.MTRANS_Y] = (viewHeight - getImageHeight()) / 2;
		}
		currentMatrix.setValues(matrix);
	}

	private float getFixTrans(float trans, float viewSize, float contentSize) {
		float minTrans;
		float maxTrans;

		if (contentSize <= viewSize) {
			minTrans = 0;
			maxTrans = viewSize - contentSize;
		} else {
			minTrans = viewSize - contentSize;
			maxTrans = 0;
		}

		if (trans < minTrans) {
			return -trans + minTrans;
		}
		if (trans > maxTrans) {
			return -trans + maxTrans;
		}
		return 0;
	}

	private float getFixDragTrans(float delta, float viewSize, float contentSize) {
		if (contentSize <= viewSize) {
			return 0;
		}
		return delta;
	}

	private float getImageWidth() {
		return matchViewWidth * normalizedScale;
	}

	private float getImageHeight() {
		return matchViewHeight * normalizedScale;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Drawable drawable = getDrawable();
		if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
			setMeasuredDimension(0, 0);
			return;
		}

		int drawableWidth = drawable.getIntrinsicWidth();
		int drawableHeight = drawable.getIntrinsicHeight();
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		viewWidth = setViewSize(widthMode, widthSize, drawableWidth);
		viewHeight = setViewSize(heightMode, heightSize, drawableHeight);

		setMeasuredDimension(viewWidth, viewHeight);
		fitImageToView();
	}

	private void fitImageToView() {
		Drawable drawable = getDrawable();
		if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
			return;
		}
		if (currentMatrix == null || previousMatrix == null) {
			return;
		}

		int drawableWidth = drawable.getIntrinsicWidth();
		int drawableHeight = drawable.getIntrinsicHeight();

		float scaleX = (float) viewWidth / drawableWidth;
		float scaleY = (float) viewHeight / drawableHeight;

		switch (scaleType) {
			case CENTER:
				scaleX = scaleY = 1;
				break;
			case CENTER_CROP:
				scaleX = scaleY = Math.max(scaleX, scaleY);
				break;
			case CENTER_INSIDE:
				scaleX = scaleY = Math.min(1, Math.min(scaleX, scaleY));
			case FIT_CENTER:
				scaleX = scaleY = Math.min(scaleX, scaleY);
				break;
			case FIT_XY:
				break;
			default:
				throw new UnsupportedOperationException("Unsupported ScaleType");
		}

		float redundantXSpace = viewWidth - (scaleX * drawableWidth);
		float redundantYSpace = viewHeight - (scaleY * drawableHeight);
		matchViewWidth = viewWidth - redundantXSpace;
		matchViewHeight = viewHeight - redundantYSpace;

		if (!isZoomed() && !imageRenderedAtLeastOnce) {
			currentMatrix.setScale(scaleX, scaleY);
			currentMatrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);
			normalizedScale = 1;
		} else {
			if (previousMatchViewWidth == 0 || previousMatchViewHeight == 0) {
				savePreviousImageValues();
			}
			previousMatrix.getValues(matrix);

			matrix[Matrix.MSCALE_X] = matchViewWidth / drawableWidth * normalizedScale;
			matrix[Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * normalizedScale;

			float transX = matrix[Matrix.MTRANS_X];
			float transY = matrix[Matrix.MTRANS_Y];

			float prevActualWidth = previousMatchViewWidth * normalizedScale;
			float actualWidth = getImageWidth();
			translateMatrixAfterRotate(Matrix.MTRANS_X, transX, prevActualWidth, actualWidth, previousViewWidth, viewWidth, drawableWidth);

			float prevActualHeight = previousMatchViewHeight * normalizedScale;
			float actualHeight = getImageHeight();
			translateMatrixAfterRotate(Matrix.MTRANS_Y, transY, prevActualHeight, actualHeight, previousViewHeight, viewHeight, drawableHeight);

			currentMatrix.setValues(matrix);
		}
		fixTrans();
		setImageMatrix(currentMatrix);
	}

	private int setViewSize(int mode, int size, int drawableSize) {
		return switch (mode) {
			case MeasureSpec.EXACTLY -> size;
			case MeasureSpec.AT_MOST -> Math.min(drawableSize, size);
			case MeasureSpec.UNSPECIFIED -> drawableSize;
			default -> size;
		};
	}

	private void translateMatrixAfterRotate(int axis, float trans, float previousImageSize, float imageSize, int previousViewSize, int viewSize, int drawableSize) {
		if (imageSize < viewSize) {
			matrix[axis] = (viewSize - (drawableSize * matrix[Matrix.MSCALE_X])) * 0.5f;
		} else if (trans > 0) {
			matrix[axis] = -((imageSize - viewSize) * 0.5f);
		} else {
			float percentage = (Math.abs(trans) + (0.5f * previousViewSize)) / previousImageSize;
			matrix[axis] = -((percentage * imageSize) - (viewSize * 0.5f));
		}
	}

	private void setState(@NonNull State state) {
		this.state = state;
	}

	@Override
	public boolean canScrollHorizontally(int direction) {
		currentMatrix.getValues(matrix);
		float x = matrix[Matrix.MTRANS_X];

		if (getImageWidth() < viewWidth) {
			return false;

		} else if (x >= -1 && direction < 0) {
			return false;

		} else if (Math.abs(x) + viewWidth + 1 >= getImageWidth() && direction > 0) {
			return false;
		}

		return true;
	}

	private void scaleImage(double deltaScale, float focusX, float focusY, boolean stretchImageToFinal) {
		float lowerScale, upperScale;
		if (stretchImageToFinal) {
			lowerScale = FINAL_SCALE_MIN_MULTIPLIER * minScale;
			upperScale = FINAL_SCALE_MAX_MULTIPLIER * maxScale;

		} else {
			lowerScale = minScale;
			upperScale = maxScale;
		}

		float origScale = normalizedScale;
		normalizedScale *= (float) deltaScale;
		if (normalizedScale > upperScale) {
			normalizedScale = upperScale;
			deltaScale = upperScale / origScale;
		} else if (normalizedScale < lowerScale) {
			normalizedScale = lowerScale;
			deltaScale = lowerScale / origScale;
		}

		currentMatrix.postScale((float) deltaScale, (float) deltaScale, focusX, focusY);
		fixScaleTrans();
	}

	private class GestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
			if (doubleTapListener != null) {
				return doubleTapListener.onSingleTapConfirmed(e);
			}
			return performClick();
		}

		@Override
		public void onLongPress(@NonNull MotionEvent e) {
			performLongClick();
		}

		@Override
		public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
			if (galleryImageFling != null) {
				galleryImageFling.cancelFling();
			}
			galleryImageFling = new GalleryImageFling((int) velocityX, (int) velocityY);
			compatPostOnAnimation(galleryImageFling);
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onDoubleTap(@NonNull MotionEvent e) {
			boolean consumed = false;
			if (doubleTapListener != null) {
				consumed = doubleTapListener.onDoubleTap(e);
			}
			if (state == State.NONE) {
				float targetZoom = (normalizedScale == minScale) ? maxScale : minScale;
				DoubleTapZoom doubleTap = new DoubleTapZoom(targetZoom, e.getX(), e.getY(), false);
				compatPostOnAnimation(doubleTap);
				consumed = true;
			}
			return consumed;
		}

		@Override
		public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
			if (doubleTapListener != null) {
				return doubleTapListener.onDoubleTapEvent(e);
			}
			return false;
		}
	}

	private class GalleryImageOnTouchListener implements OnTouchListener {
		private final PointF last = new PointF();

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			scaleDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			PointF curr = new PointF(event.getX(), event.getY());

			if (state == State.NONE || state == State.DRAG || state == State.FLING) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						last.set(curr);
						if (galleryImageFling != null)
							galleryImageFling.cancelFling();
						setState(State.DRAG);
						break;
					case MotionEvent.ACTION_MOVE:
						if (state == State.DRAG) {
							float deltaX = curr.x - last.x;
							float deltaY = curr.y - last.y;
							float fixTransX = getFixDragTrans(deltaX, viewWidth, getImageWidth());
							float fixTransY = getFixDragTrans(deltaY, viewHeight, getImageHeight());
							currentMatrix.postTranslate(fixTransX, fixTransY);
							fixTrans();
							last.set(curr.x, curr.y);
						}
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:
						setState(State.NONE);
						break;
				}
			}

			setImageMatrix(currentMatrix);
			if (userTouchListener != null) {
				userTouchListener.onTouch(v, event);
			}
			return true;
		}
	}

	private class ScaleListener extends SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
			setState(State.ZOOM);
			return true;
		}

		@Override
		public boolean onScale(@NonNull ScaleGestureDetector detector) {
			scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY(), true);
			return true;
		}

		@Override
		public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
			super.onScaleEnd(detector);
			setState(State.NONE);
			boolean animateToZoomBoundary = false;
			float targetZoom = normalizedScale;
			if (normalizedScale > maxScale) {
				targetZoom = maxScale;
				animateToZoomBoundary = true;

			} else if (normalizedScale < minScale) {
				targetZoom = minScale;
				animateToZoomBoundary = true;
			}

			if (animateToZoomBoundary) {
				DoubleTapZoom doubleTap = new DoubleTapZoom(targetZoom, (float) viewWidth / 2, (float) viewHeight / 2, true);
				compatPostOnAnimation(doubleTap);
			}
		}
	}

	private class DoubleTapZoom implements Runnable {
		private final long startTime;
		private final float startZoom;
		private final float targetZoom;
		private final float bitmapX;
		private final float bitmapY;
		private final boolean stretchImageToFinal;
		private final AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		private final PointF startTouch;
		private final PointF endTouch;

		DoubleTapZoom(float targetZoom, float focusX, float focusY, boolean stretchImageToFinal) {
			setState(State.ANIMATE_ZOOM);

			startTime = System.currentTimeMillis();
			this.startZoom = normalizedScale;
			this.targetZoom = targetZoom;
			this.stretchImageToFinal = stretchImageToFinal;

			PointF bitmapPoint = transformCoordTouchToBitmap(focusX, focusY, false);
			this.bitmapX = bitmapPoint.x;
			this.bitmapY = bitmapPoint.y;
			startTouch = transformCoordBitmapToTouch(bitmapX, bitmapY);
			endTouch = new PointF((float) viewWidth / 2, (float) viewHeight / 2);
		}

		@Override
		public void run() {
			float t = interpolate();
			double deltaScale = calculateDeltaScale(t);
			scaleImage(deltaScale, bitmapX, bitmapY, stretchImageToFinal);
			translateImageToCenterTouchPosition(t);
			fixScaleTrans();
			setImageMatrix(currentMatrix);

			if (t < 1f) {
				compatPostOnAnimation(this);
			} else {
				setState(State.NONE);
			}
		}

		private void translateImageToCenterTouchPosition(float t) {
			float targetX = startTouch.x + t * (endTouch.x - startTouch.x);
			float targetY = startTouch.y + t * (endTouch.y - startTouch.y);
			PointF curr = transformCoordBitmapToTouch(bitmapX, bitmapY);
			currentMatrix.postTranslate(targetX - curr.x, targetY - curr.y);
		}

		private float interpolate() {
			long currTime = System.currentTimeMillis();
			float elapsed = (currTime - startTime) / ZOOM_TIME;
			elapsed = Math.min(1f, elapsed);
			return interpolator.getInterpolation(elapsed);
		}

		private double calculateDeltaScale(float t) {
			double zoom = startZoom + t * (targetZoom - startZoom);
			return zoom / normalizedScale;
		}
	}

	private PointF transformCoordTouchToBitmap(float x, float y, boolean clipToBitmap) {
		currentMatrix.getValues(matrix);
		float originalWidth = getDrawable().getIntrinsicWidth();
		float originalHeight = getDrawable().getIntrinsicHeight();
		float transX = matrix[Matrix.MTRANS_X];
		float transY = matrix[Matrix.MTRANS_Y];
		float finalX = ((x - transX) * originalWidth) / getImageWidth();
		float finalY = ((y - transY) * originalHeight) / getImageHeight();

		if (clipToBitmap) {
			finalX = Math.min(Math.max(finalX, 0), originalWidth);
			finalY = Math.min(Math.max(finalY, 0), originalHeight);
		}

		return new PointF(finalX, finalY);
	}

	private PointF transformCoordBitmapToTouch(float bx, float by) {
		currentMatrix.getValues(matrix);
		float originalWidth = getDrawable().getIntrinsicWidth();
		float originalHeight = getDrawable().getIntrinsicHeight();
		float px = bx / originalWidth;
		float py = by / originalHeight;
		float finalX = matrix[Matrix.MTRANS_X] + getImageWidth() * px;
		float finalY = matrix[Matrix.MTRANS_Y] + getImageHeight() * py;
		return new PointF(finalX, finalY);
	}

	private class GalleryImageFling implements Runnable {
		private OverScroller scroller;
		private int currX;
		private int currY;

		GalleryImageFling(int velocityX, int velocityY) {
			setState(State.FLING);
			scroller = new OverScroller(ctx);
			currentMatrix.getValues(matrix);

			int startX = (int) matrix[Matrix.MTRANS_X];
			int startY = (int) matrix[Matrix.MTRANS_Y];
			int minX;
			int maxX;
			int minY;
			int maxY;

			if (getImageWidth() > viewWidth) {
				minX = viewWidth - (int) getImageWidth();
				maxX = 0;
			} else {
				minX = maxX = startX;
			}

			if (getImageHeight() > viewHeight) {
				minY = viewHeight - (int) getImageHeight();
				maxY = 0;
			} else {
				minY = maxY = startY;
			}

			scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
			currX = startX;
			currY = startY;
		}

		public void cancelFling() {
			if (scroller != null) {
				setState(State.NONE);
				scroller.forceFinished(true);
			}
		}

		@Override
		public void run() {
			if (scroller.isFinished()) {
				scroller = null;
				return;
			}

			if (scroller.computeScrollOffset()) {
				int newX = scroller.getCurrX();
				int newY = scroller.getCurrY();
				int transX = newX - currX;
				int transY = newY - currY;
				currX = newX;
				currY = newY;
				currentMatrix.postTranslate(transX, transY);
				fixTrans();
				setImageMatrix(currentMatrix);
				compatPostOnAnimation(this);
			}
		}
	}

	private void compatPostOnAnimation(Runnable runnable) {
		postOnAnimation(runnable);
	}

	private enum State {NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM}
}