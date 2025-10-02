package net.osmand.plus.views.mapwidgets.widgets;

import static android.view.View.INVISIBLE;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;

import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;
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

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.controls.ViewChangeProvider.ViewChangeListener;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class SimpleWidget extends TextInfoWidget implements ISupportWidgetResizing, ISupportMultiRow {

	private final SimpleWidgetState widgetState;

	protected OutlinedTextContainer widgetName;
	protected TextState textState;
	private boolean isFullRow;

	public SimpleWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
	                    @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, widgetType, customId, panel);
		widgetState = new SimpleWidgetState(app, customId, widgetType, getDefaultWidgetSize());

		setupViews();
		updateWidgetView();
	}

	private void setupViews() {
		LinearLayout container = (LinearLayout) view;
		container.removeAllViews();

		int layoutId = getContentLayoutId();
		UiUtilities.getInflater(mapActivity, nightMode).inflate(layoutId, container);
		findViews();
		setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(v, mapActivity, widgetType, customId, getWidgetActions(), panel, nightMode, true);
			return true;
		});
		setOnClickListener(getOnClickListener());
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
			if (textViewLayoutParams instanceof FrameLayout.LayoutParams) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
				textView.setGravity(fullRow ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
				params.setMarginStart(dpToPx(app, (shouldShowIcon() || fullRow) ? 36 : 0));
				params.setMarginEnd(dpToPx(app, fullRow ? 36 : 0));
			}
		}
	}

	private void findViews() {
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
		boolean showIcon = shouldShowIcon();
		AndroidUiHelper.updateVisibility(imageView, showIcon);
		updateWidgetName();
		if (isVerticalWidget()) {
			app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
		} else {
			updateValueAlign(false);
		}
	}

	public boolean shouldShowIcon() {
		return widgetState.getShowIconPref().get() || (isSmallSize() && !isVerticalWidget());
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
		if (oldWidgetOrientation != isVerticalWidget()) {
			recreateView();
		}
	}

	public void recreateView() {
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
		view.setVisibility(oldContainer.getVisibility());

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
	public final void updateInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
		boolean shouldHide = shouldHide();
		boolean emptyValueTextView = Algorithms.isEmpty(textView.getText());
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean visible = typeAllowed && !(shouldHide || emptyValueTextView);

		updateVisibility(visible);
		if (typeAllowed && (!shouldHide || emptyValueTextView)) {
			updateSimpleWidgetInfo(drawSettings);
		}
	}

	protected boolean shouldHide() {
		return (!(panel == BOTTOM && visibilityHelper.shouldShowBottomWidgets())) && (isVerticalWidget() && visibilityHelper.shouldHideVerticalWidgets() ||
				panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets());
	}

	protected void updateSimpleWidgetInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
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
				newWidgetName = newWidgetName + ", " + additionalName;
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

	public void setImageDrawable(int res) {
		Drawable imageDrawable = iconsCache.getIcon(res, 0);
		if (shouldShowIcon()) {
			if (imageDrawable != null) {
				imageView.setImageDrawable(imageDrawable);
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

	@Override
	public void updateColors(@NonNull MapInfoLayer.TextState textState) {
		this.textState = textState;
		if (isVerticalWidget()) {
			updateVerticalWidgetColors(textState);
		} else if (WidgetSize.SMALL != getWidgetSizePref().get() && widgetName != null) {
			updateVerticalWidgetColors(textState);
			int typefaceStyle = textState.textBold ? Typeface.BOLD : Typeface.NORMAL;
			widgetName.setTypeface(Typeface.DEFAULT, typefaceStyle);
			textView.setTypeface(Typeface.DEFAULT, typefaceStyle);
			smallTextView.setTypeface(Typeface.DEFAULT, typefaceStyle);
		} else {
			super.updateColors(textState);
		}
	}

	protected void updateVerticalWidgetColors(@NonNull MapInfoLayer.TextState textState) {
		nightMode = textState.night;
		textView.setTextColor(textState.textColor);
		smallTextView.setTextColor(textState.secondaryTextColor);
		widgetName.setTextColor(textState.secondaryTextColor);
		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}
		view.findViewById(R.id.widget_bg).setBackgroundResource(textState.widgetBackgroundId);

		if (bottomDivider != null) {
			bottomDivider.setBackgroundResource(textState.widgetDividerColorId);
		}
		updateTextOutline(textView, textState);
		updateTextOutline(widgetName, textState);
		updateTextOutline(smallTextView, textState);
	}

	@Override
	protected View getContentView() {
		return isVerticalWidget() ? view : container;
	}

	@Override
	public void updateFullRowState(int widgetsCount) {
		boolean fullRow = widgetsCount <= 1;
		if (isFullRow != fullRow) {
			isFullRow = fullRow;
			recreateView();
			if (textState != null) {
				updateColors(textState);
			}
			updateInfo(null);
		}
	}

	private boolean isSmallSize() {
		return getWidgetSizePref().get() == WidgetSize.SMALL;
	}
}
