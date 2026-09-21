package net.osmand.plus.views.mapwidgets.widgets;

import static android.view.View.INVISIBLE;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.SizeF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ViewChangeProvider.ViewChangeListener;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceApplier;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class SimpleWidget extends TextInfoWidget implements ISupportWidgetResizing, ISupportMultiRow {

	private final SimpleWidgetState widgetState;

	protected OutlinedTextContainer widgetName;
	private boolean isFullRow;
	@Nullable
	private WidgetSize renderedWidgetSize;

	@Nullable
	protected WidgetSize androidAutoRenderedWidgetSize;
	protected String cachedWidgetName;
	protected TextPaint widgetNameTextPaint = new TextPaint();
	protected Rect cachedWidgetNameTextBounds = new Rect();
	protected StaticLayout cachedWidgetNameTextLayout;

	public SimpleWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
	                    @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, widgetType, customId, panel);
		widgetState = new SimpleWidgetState(app, customId, widgetType, getDefaultWidgetSize());
	}

	public SimpleWidget(@NonNull OsmandApplication app, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(app, widgetType, customId, panel);
		widgetState = new SimpleWidgetState(app, customId, widgetType, getDefaultWidgetSize());
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);

		setupViews();
		updateWidgetView();
	}

	private void setupViews() {
		LinearLayout container = (LinearLayout) getView();
		container.removeAllViews();

		int layoutId = getContentLayoutId();
		renderedWidgetSize = widgetState.getWidgetSizePref().get();
		UiUtilities.getInflater(mapActivity, nightMode).inflate(layoutId, container);
		findViews();
		container.setOnLongClickListener(v -> {
			List<PopUpMenuItem> actions = getWidgetActions();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(v.getContext());
			WidgetsContextMenu.showMenu(v, mapActivity, widgetType, customId, actions, layoutMode, panel, nightMode, true);
			return true;
		});
		container.setOnClickListener(getOnClickListener());
	}

	@LayoutRes
	protected int getContentLayoutId() {
		return isVerticalWidget() ? getProperVerticalLayoutId(widgetState) : getProperSideLayoutId(widgetState);
	}

	@NonNull
	protected WidgetSize getDefaultWidgetSize() {
		return isVerticalWidget() ? WidgetSize.MEDIUM : WidgetSize.SMALL;
	}

	@Override
	public void updateValueAlign(boolean fullRow) {
		if (WidgetSize.SMALL != getWidgetSizePref().get()) {
			ViewGroup.LayoutParams textViewLayoutParams = textView.getLayoutParams();
			if (textViewLayoutParams instanceof FrameLayout.LayoutParams params) {
				textView.setGravity(fullRow ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
				int startMargin = dpToPx(app, (shouldShowIcon() || fullRow) ? 36 : 0);
				int endMargin = dpToPx(app, fullRow ? 36 : 0);
				if (params.getMarginStart() != startMargin || params.getMarginEnd() != endMargin) {
					params.setMarginStart(startMargin);
					params.setMarginEnd(endMargin);
					textView.setLayoutParams(params);
				}
			}
		}
	}

	private void findViews() {
		View view = getView();
		container = view.findViewById(R.id.container);
		emptyBanner = view.findViewById(R.id.empty_banner);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
		widgetName = view.findViewById(R.id.widget_name);
		bottomDivider = view.findViewById(R.id.bottom_divider);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.simple_widget_vertical_content_container;
	}

	@LayoutRes
	private int getProperSideLayoutId(@NonNull SimpleWidgetState simpleWidgetState) {
		return switch (simpleWidgetState.getWidgetSizePref().get()) {
			case SMALL -> R.layout.map_hud_widget;
			case LARGE -> R.layout.simple_map_widget_large;
			default -> R.layout.simple_map_widget_medium;
		};
	}

	@LayoutRes
	private int getProperVerticalLayoutId(@NonNull SimpleWidgetState simpleWidgetState) {
		return switch (simpleWidgetState.getWidgetSizePref().get()) {
			case SMALL ->
					isFullRow ? R.layout.simple_map_widget_small_full : R.layout.simple_map_widget_small;
			case LARGE -> R.layout.simple_map_widget_large;
			default -> R.layout.simple_map_widget_medium;
		};
	}

	public void updateWidgetView() {
		updateWidgetName();
		if (!isAndroidAuto()) {
			boolean showIcon = shouldShowIcon();
			AndroidUiHelper.updateVisibility(imageView, showIcon);
			if (isVerticalWidget()) {
				app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
			} else {
				updateValueAlign(false);
			}
		}
	}

	public boolean shouldShowIcon() {
		return isSmallSize() && !isVerticalWidget()
				|| widgetState.getShowIconPref().get();
	}

	@NonNull
	public CommonPreference<Boolean> shouldShowIconPref() {
		return widgetState.getShowIconPref();
	}

	@Override
	public boolean allowResize() {
		return true;
	}

	@NonNull
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetState.getWidgetSizePref();
	}

	public void recreateViewIfNeeded(@NonNull WidgetsPanel panel) {
		boolean oldWidgetOrientation = isVerticalWidget();
		setPanel(panel);
		if (!isAndroidAuto() && oldWidgetOrientation != isVerticalWidget()) {
			recreateView();
		}
	}

	@Override
	protected void recreateInternalForAndroidAuto() {
		super.recreateInternalForAndroidAuto();
		androidAutoRenderedWidgetSize = widgetState.getWidgetSizePref().get();
		updateWidgetName();
	}

	@Override
	protected void recreateViewInternal() {
		initView();
		ImageView oldImageView = imageView;
		OutlinedTextContainer oldTextView = textView;
		OutlinedTextContainer oldSmallTextView = smallTextView;
		TextView oldSmallTextViewShadow = smallTextViewShadow;
		View oldContainer = container;
		View oldEmptyBanner = emptyBanner;
		View oldBottomDivider = bottomDivider;

		setupViews();
		findViews();

		imageView.setImageDrawable(oldImageView.getDrawable());
		copyView(imageView, oldImageView);
		AndroidUiHelper.setVisibility(oldContainer.getVisibility(), getView());

		copyTextView(textView, oldTextView);
		copyTextView(smallTextView, oldSmallTextView);
		copyTextView(smallTextViewShadow, oldSmallTextViewShadow);
		copyView(emptyBanner, oldEmptyBanner);
		copyView(bottomDivider, oldBottomDivider);

		updateInfo(null);
		updateWidgetView();
	}

	@Nullable
	protected List<PopUpMenuItem> getWidgetActions() {
		return null;
	}

	@Override
	public final void updateInfo(@NonNull View view, @Nullable DrawSettings drawSettings) {
		boolean shouldHide = shouldHide();
		boolean emptyValueTextView = Algorithms.isEmpty(textView.getText());
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean visible = typeAllowed && !(shouldHide || emptyValueTextView);

		updateVisibility(visible);
		if (typeAllowed && (!shouldHide || emptyValueTextView)) {
			updateSimpleWidgetInfo(drawSettings);
		}
	}

	@Override
	protected void updateInfoForAndroidAuto(@Nullable DrawSettings drawSettings) {
		super.updateInfoForAndroidAuto(drawSettings);
		updateSimpleWidgetInfoForAndroidAuto(drawSettings);
	}

	protected boolean shouldHide() {
		return (!(panel == BOTTOM && visibilityHelper.shouldShowBottomWidgets())) && (isVerticalWidget() && visibilityHelper.shouldHideVerticalWidgets() ||
				panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets());
	}

	protected void updateSimpleWidgetInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {

	}

	protected void updateSimpleWidgetInfoForAndroidAuto(@Nullable OsmandMapLayer.DrawSettings drawSettings) {

	}

	@Override
	public boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (isVerticalWidget() && updatedVisibility) {
			app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
		}
		return updatedVisibility;
	}

	protected void updateWidgetName() {
		String newWidgetName = getWidgetName();

		if (newWidgetName != null && this.widgetName != null) {

			String additionalName = getAdditionalWidgetName();
			if (additionalName != null) {
				newWidgetName = getString(getAdditionalWidgetNameDivider(), newWidgetName, additionalName);
			}

			String oldWidgetName = String.valueOf(this.widgetName.getText());
			this.widgetName.setText(newWidgetName);

			if (!oldWidgetName.equals(newWidgetName)) {
				if (widgetName.getVisibility() == View.GONE) {
					widgetName.setVisibility(INVISIBLE);
				}
				checkForMaxWidgetName();
			}
		}
		cachedWidgetName = newWidgetName;
		markAndroidAutoLayoutNeeded();
	}

	private void checkForMaxWidgetName() {
		if (widgetName == null) {
			return;
		}

		widgetName.addViewChangeListener(new ViewChangeListener() {
			@Override
			public void onSizeChanged(@NonNull View view, int w, int h, int oldWidth, int oldHeight) {
				String text = widgetName.getText().toString();

				String firstFourSymbols = (text.length() > 4 ? text.substring(0, 4) : text).toUpperCase();

				if (text.length() > 4) {
					firstFourSymbols += "…";
				}

				int titleViewWidth = widgetName.getWidth();
				if (titleViewWidth == 0) {
					return;
				}

				TextPaint paint = widgetName.getPaint();
				float requiredWidth = paint.measureText(firstFourSymbols);
				float availableWidth = titleViewWidth - widgetName.getPaddingLeft() - widgetName.getPaddingRight();
				boolean hideTitle = availableWidth < requiredWidth;
				AndroidUiHelper.updateVisibility(widgetName, !hideTitle);
			}

			@Override
			public void onVisibilityChanged(@NonNull View view, int visibility) {

			}
		});
	}

	@Nullable
	protected String getWidgetName() {
		return widgetType != null ? getString(widgetType.titleId) : null;
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode,
	                                 @NonNull ApplicationMode appMode, @Nullable String customId) {
		if (widgetState != null) {
			widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
		}
	}

	@Nullable
	protected String getAdditionalWidgetName() {
		return null;
	}

	@StringRes
	protected int getAdditionalWidgetNameDivider() {
		return R.string.ltr_or_rtl_combine_via_comma;
	}

	private void copyTextView(@Nullable TextView newTextView, @Nullable TextView oldTextView) {
		if (newTextView != null && oldTextView != null) {
			newTextView.setTextColor(oldTextView.getCurrentTextColor());
			newTextView.setTypeface(oldTextView.getTypeface());
			newTextView.getPaint().setStrokeWidth(oldTextView.getPaint().getStrokeWidth());
			newTextView.getPaint().setStyle(oldTextView.getPaint().getStyle());
			newTextView.setText(oldTextView.getText());
			copyView(newTextView, oldTextView);
		}
	}

	private void copyTextView(@Nullable OutlinedTextContainer newTextView, @Nullable OutlinedTextContainer oldTextView) {
		if (newTextView != null && oldTextView != null) {
			newTextView.copyFromTextContainer(oldTextView);
			copyView(newTextView, oldTextView);
		}
	}

	private void copyView(@Nullable View newView, @Nullable View oldTView) {
		if (newView != null && oldTView != null) {
			newView.setFocusable(oldTView.isFocusable());
			newView.setVisibility(oldTView.getVisibility());
			newView.setContentDescription(oldTView.getContentDescription());
		}
	}

	protected View.OnClickListener getOnClickListener() {
		return null;
	}

	public void setImageDrawable(@NonNull ImageView imageView, @Nullable Drawable drawable, int visibility) {
		if (shouldShowIcon()) {
			if (drawable != null) {
				imageView.setImageDrawable(drawable);
				Object anim = imageView.getDrawable();
				if (anim instanceof AnimationDrawable) {
					((AnimationDrawable) anim).start();
				}
				imageView.setVisibility(View.VISIBLE);
			}
		} else {
			imageView.setVisibility(View.GONE);
		}
		imageView.invalidate();
	}

	public void updateIcon() {
		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}
	}

	@Override
	protected void onPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {
		if (renderedWidgetSize != getWidgetSizePref().get()) {
			recreateView();
			return;
		}
		AndroidUiHelper.updateVisibility(imageView, shouldShowIcon());
		if (!isVerticalWidget()) {
			updateValueAlign(false);
		}
		if (isVerticalWidget()) {
			applySimpleWidgetAppearance(appearance);
		} else if (WidgetSize.SMALL != getWidgetSizePref().get() && widgetName != null) {
			applySimpleWidgetAppearance(appearance);
		} else {
			super.onPanelAppearanceChanged(appearance);
		}
	}

	protected void applySimpleWidgetAppearance(@NonNull ResolvedPanelAppearance appearance) {
		PanelAppearanceApplier.applyPrimaryText(textView, appearance);
		PanelAppearanceApplier.applySecondaryText(smallTextView, appearance);
		PanelAppearanceApplier.applySecondaryText(widgetName, appearance);
		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}
		View view = getView();
		View widgetBg = view.findViewById(R.id.widget_bg);
		if (widgetBg != view) {
			view.setBackground(null);
		}
		PanelAppearanceApplier.applyBackground(widgetBg, appearance);
		PanelAppearanceApplier.applyDivider(bottomDivider, appearance);
	}

	@Override
	protected View getContentView() {
		return isVerticalWidget() ? getView() : container;
	}

	@Override
	public void updateFullRowState(int widgetsCount) {
		boolean fullRow = widgetsCount <= 1;
		if (isFullRow != fullRow) {
			isFullRow = fullRow;
			recreateView();
			updateInfo(null);
		}
	}

	private boolean isSmallSize() {
		return getWidgetSizePref().get() == WidgetSize.SMALL;
	}

	// region android auto

	@Override
	public void drawForAndroidAuto(@NonNull Canvas canvas, @NonNull DrawSettings drawSettings,
	                               float widgetWidthPx, float widgetHeightPx, boolean isRtl) {
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			drawSmallSize(canvas, drawSettings);
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			drawLargeSize(canvas, drawSettings);
		} else {
			drawMediumSize(canvas, drawSettings);
		}
	}

	@Override
	public void onAndroidAutoPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {
		super.onAndroidAutoPanelAppearanceChanged(appearance);
		if (androidAutoRenderedWidgetSize != getWidgetSizePref().get()) {
			recreateInternalForAndroidAuto();
		}
		configureAAPaints(appearance);
		shouldDrawAndroidAutoIcon = shouldShowIcon();
		markAndroidAutoLayoutNeeded();
	}

	@Override
	protected void configureAAPaints(ResolvedPanelAppearance appearance) {
		super.configureAAPaints(appearance);
		applyWidgetNameTextAppearance(widgetNameTextPaint, appearance);
	}


	// region text sizes
	@Override
	protected float getPrimaryTextSizeAA() {
		int resId;

		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			resId = R.dimen.map_widget_text_size;
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			resId = R.dimen.simple_widget_value_large_size;
		} else {
			resId = R.dimen.simple_widget_value_medium_size;
		}

		return app.getResources().getDimension(resId);
	}

	@Override
	protected float getSecondaryTextSizeAA() {
		int resId;
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			resId = R.dimen.map_widget_text_size_small;
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			resId = R.dimen.simple_widget_description_text_size;
		} else {
			resId = R.dimen.simple_widget_description_text_size;
		}
		return app.getResources().getDimension(resId);
	}

	protected float getWidgetNameTextSizeAA() {
		int resId;

		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			return 0f;
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			resId = R.dimen.simple_widget_description_text_size;
		} else {
			resId = R.dimen.simple_widget_description_text_size;
		}

		return app.getResources().getDimension(resId);
	}
	// endregion

	// region text appearances
	@Override
	protected void applyPrimaryTextAppearance(Paint paint, ResolvedPanelAppearance appearance) {
		super.applyPrimaryTextAppearance(paint, appearance);
		//noinspection StatementWithEmptyBody
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			// no additional tweaks for now
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			paint.setLetterSpacing(0.04f);
		} else {
			paint.setLetterSpacing(0.04f);
		}
	}

	@Override
	protected void applySecondaryTextAppearance(Paint paint, ResolvedPanelAppearance appearance) {
		super.applySecondaryTextAppearance(paint, appearance);
		//noinspection StatementWithEmptyBody
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			// no additional tweaks for now
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			paint.setLetterSpacing(0.04f);
		} else {
			paint.setLetterSpacing(0.04f);
		}
	}

	protected void applyWidgetNameTextAppearance(Paint paint, ResolvedPanelAppearance appearance) {
		applyTextAppearance(paint, appearance.getSecondaryTextColor(), appearance);
		paint.setTextSize(getWidgetNameTextSizeAA());
		//noinspection StatementWithEmptyBody
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			// no additional tweaks for now
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			paint.setLetterSpacing(0.06f);
		} else {
			paint.setLetterSpacing(0.06f);
		}
	}
	// endregion

	// region draw
	protected void drawSmallSize(@NonNull Canvas canvas, @NonNull DrawSettings drawSettings) {
		if (shouldDrawAndroidAutoIcon) {
			int iconId = getIconId(drawSettings.isNightMode());
			if (iconId != 0) {
				Drawable iconDrawable = iconsCache.getIcon(iconId, 0);
				if (iconDrawable != null) {
					iconDrawable.setBounds(cachedIconBounds);
					iconDrawable.draw(canvas);
				}
			}
		}

		drawTextLineInRect(canvas, cachedTextBounds, cachedTextLayout);
		drawTextLineInRect(canvas, cachedSmallTextBounds, cachedSmallTextLayout);
	}

	protected void drawMediumSize(@NonNull Canvas canvas, @NonNull DrawSettings drawSettings) {
		if (shouldDrawAndroidAutoIcon) {
			int iconId = getIconId(drawSettings.isNightMode());
			if (iconId != 0) {
				Drawable iconDrawable = iconsCache.getIcon(iconId, 0);
				if (iconDrawable != null) {
					iconDrawable.setBounds(cachedIconBounds);
					iconDrawable.draw(canvas);
				}
			}
		}


		drawTextLineInRect(canvas, cachedTextBounds, cachedTextLayout,
				Gravity.CENTER_VERTICAL | Gravity.START
		);
		drawTextLineInRect(canvas, cachedSmallTextBounds, cachedSmallTextLayout,
				Gravity.BOTTOM | Gravity.END
		);
		drawTextLineInRect(canvas, cachedWidgetNameTextBounds, cachedWidgetNameTextLayout,
				Gravity.BOTTOM | Gravity.START
		);
	}

	protected void drawLargeSize(@NonNull Canvas canvas, @NonNull DrawSettings drawSettings) {
		if (shouldDrawAndroidAutoIcon) {
			int iconId = getIconId(drawSettings.isNightMode());
			if (iconId != 0) {
				Drawable iconDrawable = iconsCache.getIcon(iconId, 0);
				if (iconDrawable != null) {
					iconDrawable.setBounds(cachedIconBounds);
					iconDrawable.draw(canvas);
				}
			}
		}

		drawTextLineInRect(canvas, cachedTextBounds, cachedTextLayout,
				Gravity.CENTER_VERTICAL | Gravity.START
		);
		drawTextLineInRect(canvas, cachedSmallTextBounds, cachedSmallTextLayout,
				Gravity.BOTTOM | Gravity.END
		);
		drawTextLineInRect(canvas, cachedWidgetNameTextBounds, cachedWidgetNameTextLayout,
				Gravity.BOTTOM | Gravity.START
		);
	}
	// endregion

	// region layout


	@Override
	protected void doLayoutAAWidget(Context context, int desiredWidthPx, boolean isRtl) {
		if (androidAutoRenderedWidgetSize == WidgetSize.SMALL) {
			layoutSmallWidgetAA(context, desiredWidthPx, isRtl);
		} else if (androidAutoRenderedWidgetSize == WidgetSize.LARGE) {
			layoutLargeWidgetAA(context, desiredWidthPx, isRtl);
		} else {
			layoutMediumWidgetAA(context, desiredWidthPx, isRtl);
		}
	}

	private void layoutSmallWidgetAA(Context context, float widgetWidthPx, boolean isRtl) {
		Resources resources = context.getResources();
		float minBottomLayoutHeight = resources.getDimension(R.dimen.map_widget_height);
		int iconSize = resources.getDimensionPixelSize(R.dimen.map_widget_icon);

		if (shouldDrawAndroidAutoIcon) {
			cachedIconBounds.set(0, 0, iconSize, iconSize);
		} else {
			cachedIconBounds.set(0, 0, 0, 0);
		}
		cachedTextLayout = updateCachedTextBounds(cachedTextBounds, textPaint, cachedText);
		cachedSmallTextLayout = updateCachedTextBounds(cachedSmallTextBounds, smallTextPaint, cachedSmallText);

		float textBlockHeight = Math.max(cachedTextBounds.height(), cachedSmallTextBounds.height());
		float contentHeight = Math.max(iconSize, Math.max(textBlockHeight, minBottomLayoutHeight));

		SizeF widgetSize = new SizeF(widgetWidthPx, contentHeight);

		int iconMargin = resources.getDimensionPixelSize(R.dimen.map_widget_icon_margin);
		int textMargin = AndroidUtils.dpToPx(context, 4);
		float centerVertical = contentHeight / 2f;

		float textBlockMarginStart = AndroidUtils.dpToPxF(context, 4);
		float smallTextMarginStart = AndroidUtils.dpToPxF(context, 4);
		float smallTextPaddingBottom = AndroidUtils.dpToPxF(context, 2);

		float iconTop = centerVertical - cachedIconBounds.height() / 2f;
		float textBottom = centerVertical + textBlockHeight / 2f;
		float textTop = textBottom - cachedTextBounds.height();
		float smallTextTop = textBottom - smallTextPaddingBottom - cachedSmallTextBounds.height();

		if (isRtl) {
			cachedIconBounds.offsetTo((int) widgetWidthPx, (int) iconTop);
			cachedTextBounds.offsetTo((int) widgetWidthPx, (int) textTop);
			cachedSmallTextBounds.offsetTo((int) widgetWidthPx, (int) smallTextTop);
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offset((int) (widgetWidthPx - iconMargin - iconSize), (int) iconTop);
				cachedTextBounds.offsetTo((int) (cachedIconBounds.left - iconMargin - textBlockMarginStart - cachedTextBounds.width()), cachedTextBounds.top);
			} else {
				cachedTextBounds.offsetTo((int) (widgetWidthPx - textBlockMarginStart - cachedTextBounds.width()), cachedTextBounds.top);
			}
			cachedSmallTextBounds.offsetTo((int) (cachedTextBounds.left - smallTextMarginStart - cachedSmallTextBounds.width()), (int) smallTextTop);
		} else {
			cachedIconBounds.offsetTo(0, (int) iconTop);
			cachedTextBounds.offsetTo(0, (int) textTop);
			cachedSmallTextBounds.offsetTo(0, (int) smallTextTop);
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offset(iconMargin, 0);
				cachedTextBounds.offsetTo(cachedIconBounds.right + iconMargin + textMargin, cachedTextBounds.top);
			} else {
				cachedTextBounds.offsetTo(textMargin, cachedTextBounds.top);
			}
			cachedSmallTextBounds.offsetTo((int) (cachedTextBounds.right + smallTextMarginStart), (int) smallTextTop);
		}

		measuredAAHeight = widgetSize.getHeight();
		measuredAAWidth = widgetSize.getWidth();
	}


	private void layoutMediumWidgetAA(Context context, float widgetWidthPx, boolean isRtl) {
		Resources resources = context.getResources();
		int iconSize = resources.getDimensionPixelSize(R.dimen.map_widget_icon);
		float widgetHeightPx = resources.getDimension(R.dimen.simple_widget_medium_height);

		SizeF widgetSize = new SizeF(widgetWidthPx, widgetHeightPx);

		int paddingHorizontal = AndroidUtils.dpToPx(context, 16);
		int paddingTop = AndroidUtils.dpToPx(context, 6);
		int paddingBottom = AndroidUtils.dpToPx(context, 3);

		Rect containerRect = new Rect(paddingHorizontal,
				paddingTop,
				(int) (widgetWidthPx - paddingHorizontal),
				(int) (widgetHeightPx - paddingBottom));

		Rect topRowRect = new Rect(containerRect.left,
				containerRect.top,
				containerRect.right,
				containerRect.top + AndroidUtils.dpToPx(context, 17));
		int topRowTextMargin = AndroidUtils.dpToPx(context, 3);

		float smallTextLineSpacingExtra = -1 * AndroidUtils.spToPx(context, 2);
		cachedSmallTextLayout = updateCachedTextBounds(cachedSmallTextBounds, smallTextPaint, cachedSmallText, smallTextLineSpacingExtra);

		if (shouldDrawAndroidAutoIcon) {
			cachedIconBounds.set(0, 0, iconSize, iconSize);
		} else {
			cachedIconBounds.set(0, 0, 0, 0);
		}

		if (isRtl) {
			cachedSmallTextBounds.offsetTo(topRowRect.left, topRowRect.bottom - cachedSmallTextBounds.height());

			cachedWidgetNameTextBounds.set(
					cachedSmallTextBounds.right + topRowTextMargin,
					topRowRect.top,
					topRowRect.left,
					topRowRect.bottom
			);
		} else {
			cachedSmallTextBounds.offsetTo(
					topRowRect.right - cachedSmallTextBounds.width(),
					topRowRect.bottom - cachedSmallTextBounds.height()
			);
			cachedWidgetNameTextBounds.set(
					topRowRect.left,
					topRowRect.top,
					cachedSmallTextBounds.left - topRowTextMargin,
					topRowRect.bottom
			);
		}

		Rect bottomRowRect = new Rect(paddingHorizontal,
				topRowRect.bottom,
				(int) (widgetWidthPx - paddingHorizontal),
				(int) (widgetHeightPx - paddingBottom));
		float bottomRowCenterVertical = bottomRowRect.top + bottomRowRect.height() / 2f;
		float iconTop = bottomRowCenterVertical - cachedIconBounds.height() / 2f;
		float bottomTextMarginStart = AndroidUtils.dpToPx(context, 12);
		if (isRtl) {
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offsetTo(bottomRowRect.right - cachedIconBounds.width(), (int) iconTop);
			}
			cachedTextBounds.set(
					bottomRowRect.left,
					bottomRowRect.top,
					(int) (cachedIconBounds.left - bottomTextMarginStart),
					bottomRowRect.bottom);
		} else {
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offsetTo(bottomRowRect.left, (int) iconTop);
			}
			cachedTextBounds.set(
					(int) (cachedIconBounds.right + bottomTextMarginStart),
					bottomRowRect.top,
					bottomRowRect.right,
					bottomRowRect.bottom
			);
		}


		float maxTextSize = getPrimaryTextSizeAA();
		float minTextSize = resources.getDimension(R.dimen.simple_widget_value_minimum_size);
		float textSizeStep = AndroidUtils.spToPx(context, 2);
		String textToDraw = cachedText.toUpperCase();
		float optimalTextSize = findOptimalSingleLineTextSize(textToDraw, textPaint, cachedTextBounds.width(), cachedTextBounds.height(), minTextSize, maxTextSize, textSizeStep);
		textPaint.setTextSize(optimalTextSize);
		cachedTextLayout = buildTextLineStaticLayout(textToDraw, textPaint, cachedTextBounds.width(),
				Gravity.CENTER_VERTICAL | Gravity.START, null, null);

		float widgetNameLineSpacingExtra = -1 * AndroidUtils.spToPx(context, 2);
		cachedWidgetNameTextLayout = buildTextLineStaticLayout(cachedWidgetName, widgetNameTextPaint, cachedWidgetNameTextBounds.width(),
				Gravity.BOTTOM | Gravity.START, widgetNameLineSpacingExtra, TextUtils.TruncateAt.END);

		measuredAAHeight = widgetSize.getHeight();
		measuredAAWidth = widgetSize.getWidth();
	}

	private void layoutLargeWidgetAA(Context context, float widgetWidthPx, boolean isRtl) {
		Resources resources = context.getResources();
		int iconSize = resources.getDimensionPixelSize(R.dimen.map_widget_icon);
		float widgetHeightPx = resources.getDimension(R.dimen.simple_widget_large_height);

		SizeF widgetSize = new SizeF(widgetWidthPx, widgetHeightPx);

		int paddingHorizontal = AndroidUtils.dpToPx(context, 16);
		int paddingTop = AndroidUtils.dpToPx(context, 6);
		int paddingBottom = AndroidUtils.dpToPx(context, 3);

		Rect containerRect = new Rect(paddingHorizontal,
				paddingTop,
				(int) (widgetWidthPx - paddingHorizontal),
				(int) (widgetHeightPx - paddingBottom));

		Rect topRowRect = new Rect(containerRect.left,
				containerRect.top,
				containerRect.right,
				containerRect.top + AndroidUtils.dpToPx(context, 17));
		int topRowTextMargin = AndroidUtils.dpToPx(context, 3);

		float smallTextLineSpacingExtra = -1 * AndroidUtils.spToPx(context, 2);
		cachedSmallTextLayout = updateCachedTextBounds(cachedSmallTextBounds, smallTextPaint, cachedSmallText, smallTextLineSpacingExtra);

		if (shouldDrawAndroidAutoIcon) {
			cachedIconBounds.set(0, 0, iconSize, iconSize);
		} else {
			cachedIconBounds.set(0, 0, 0, 0);
		}

		if (isRtl) {
			cachedSmallTextBounds.offsetTo(topRowRect.left, topRowRect.bottom - cachedSmallTextBounds.height());

			cachedWidgetNameTextBounds.set(
					cachedSmallTextBounds.right + topRowTextMargin,
					topRowRect.top,
					topRowRect.left,
					topRowRect.bottom
			);
		} else {
			cachedSmallTextBounds.offsetTo(
					topRowRect.right - cachedSmallTextBounds.width(),
					topRowRect.bottom - cachedSmallTextBounds.height()
			);
			cachedWidgetNameTextBounds.set(
					topRowRect.left,
					topRowRect.top,
					cachedSmallTextBounds.left - topRowTextMargin,
					topRowRect.bottom
			);
		}
		Rect bottomRowRect = new Rect(paddingHorizontal,
				topRowRect.bottom,
				(int) (widgetWidthPx - paddingHorizontal),
				(int) (widgetHeightPx - paddingBottom));
		float bottomRowCenterVertical = bottomRowRect.top + bottomRowRect.height() / 2f;
		float iconTop = bottomRowCenterVertical - cachedIconBounds.height() / 2f;
		float bottomTextMarginStart = AndroidUtils.dpToPx(context, 12);
		if (isRtl) {
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offsetTo(bottomRowRect.right - cachedIconBounds.width(), (int) iconTop);
			}
			cachedTextBounds.set(
					bottomRowRect.left,
					bottomRowRect.top,
					(int) (cachedIconBounds.left - bottomTextMarginStart),
					bottomRowRect.bottom);
		} else {
			if (shouldDrawAndroidAutoIcon) {
				cachedIconBounds.offsetTo(bottomRowRect.left, (int) iconTop);
			}
			cachedTextBounds.set(
					(int) (cachedIconBounds.right + bottomTextMarginStart),
					bottomRowRect.top,
					bottomRowRect.right,
					bottomRowRect.bottom
			);
		}

		measuredAAHeight = widgetSize.getHeight();
		measuredAAWidth = widgetSize.getWidth();

		float maxTextSize = getPrimaryTextSizeAA();
		float minTextSize = resources.getDimension(R.dimen.simple_widget_value_minimum_size);
		float textSizeStep = AndroidUtils.spToPx(context, 2);
		String textToDraw = cachedText.toUpperCase();
		float optimalTextSize = findOptimalSingleLineTextSize(textToDraw, textPaint, cachedTextBounds.width(), cachedTextBounds.height(), minTextSize, maxTextSize, textSizeStep);
		textPaint.setTextSize(optimalTextSize);
		cachedTextLayout = buildTextLineStaticLayout(textToDraw, textPaint, cachedTextBounds.width(),
				Gravity.CENTER_VERTICAL | Gravity.START, null, null);

		float widgetNameLineSpacingExtra = -1 * AndroidUtils.spToPx(context, 2);
		cachedWidgetNameTextLayout = buildTextLineStaticLayout(cachedWidgetName, widgetNameTextPaint, cachedWidgetNameTextBounds.width(),
				Gravity.BOTTOM | Gravity.START, widgetNameLineSpacingExtra, TextUtils.TruncateAt.END);
	}
	// endregion
}
