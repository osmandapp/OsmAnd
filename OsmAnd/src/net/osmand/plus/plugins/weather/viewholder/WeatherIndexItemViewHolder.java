package net.osmand.plus.plugins.weather.viewholder;

import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.helpers.FileNameTranslationHelper.getWeatherName;
import static net.osmand.plus.plugins.weather.viewholder.WeatherIndexItemViewHolder.ViewHolderState.DOWNLOADED;
import static net.osmand.plus.plugins.weather.viewholder.WeatherIndexItemViewHolder.ViewHolderState.DOWNLOAD_IN_PROGRESS;
import static net.osmand.plus.plugins.weather.viewholder.WeatherIndexItemViewHolder.ViewHolderState.NEW;
import static net.osmand.plus.plugins.weather.viewholder.WeatherIndexItemViewHolder.ViewHolderState.REMOVE_IN_PROGRESS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.OnCompleteCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.indexitem.WeatherIndexItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class WeatherIndexItemViewHolder {

	private final OsmandApplication app;
	private final FragmentActivity activity;
	private final OfflineForecastHelper offlineForecastHelper;
	private final DownloadIndexesThread downloadThread;
	private final DownloadValidationManager downloadManager;
	private final WeatherIndexItem indexItem;
	private final ApplicationMode appMode;
	private final boolean nightMode;

	private final View view;
	private final TextView tvDescription;
	private final ImageView ivIcon;
	private final ImageView ivSecIcon;
	private final ProgressBar pbProgress;
	private ViewHolderState currentState;

	public enum ViewHolderState {
		NEW, DOWNLOAD_IN_PROGRESS, REMOVE_IN_PROGRESS, DOWNLOADED
	}

	public WeatherIndexItemViewHolder(@NonNull FragmentActivity activity, @NonNull ApplicationMode appMode,
	                                  @NonNull WeatherIndexItem indexItem, boolean nightMode, boolean showDivider) {
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.downloadThread = app.getDownloadThread();
		this.downloadManager = new DownloadValidationManager(app);
		this.offlineForecastHelper = app.getOfflineForecastHelper();
		this.indexItem = indexItem;
		this.activity = activity;
		this.nightMode = nightMode;
		this.appMode = appMode;
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		view = inflater.inflate(R.layout.list_item_icon_and_download, null, false);

		TextView tvTitle = view.findViewById(R.id.title);
		tvDescription = view.findViewById(R.id.description);
		ivIcon = view.findViewById(R.id.icon);
		ivSecIcon = view.findViewById(R.id.secondary_icon);
		pbProgress = view.findViewById(R.id.ProgressBar);
		View divider = view.findViewById(R.id.divider);

		tvTitle.setText(indexItem.getVisibleName(app, app.getRegions(), false));
		ivIcon.setImageResource(WEATHER_FORECAST.getIconResource());
		divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
		setupSelectableBackground(view);
		update();
	}

	public void update() {
		if (downloadThread.isDownloading(indexItem)) {
			applyState(DOWNLOAD_IN_PROGRESS);
			if (downloadThread.isCurrentDownloading(indexItem)) {
				setProgress((int) downloadThread.getCurrentDownloadProgress());
			}
		} else if (offlineForecastHelper.isRemoveLocalForecastInProgress(indexItem.getRegionId())) {
			applyState(REMOVE_IN_PROGRESS);
		} else if (indexItem.isDownloaded()) {
			applyState(DOWNLOADED);
		} else {
			applyState(NEW);
		}
	}

	private void applyState(@NonNull ViewHolderState newState) {
		if (currentState != newState) {
			currentState = newState;
			applyState();
		}
	}

	private void applyState() {
		if (currentState == NEW) {
			applyNewState();
		} else if (currentState == DOWNLOAD_IN_PROGRESS) {
			applyDownloadInProgressState();
		} else if (currentState == DOWNLOADED) {
			applyDownloadedState();
		} else if (currentState == REMOVE_IN_PROGRESS) {
			applyRemoveInProgressState();
		}
	}

	public void setProgress(int progress) {
		pbProgress.setProgress(progress);
	}

	private void applyNewState() {
		show(ivSecIcon, pbProgress);
		hide(tvDescription);

		pbProgress.setIndeterminate(true);
		ivIcon.setColorFilter(getDefaultIconColor());
		calculateIndexItemSize(() -> {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_bold_point);
			String type = WEATHER_FORECAST.getString(app);
			tvDescription.setText(String.format(pattern, type, indexItem.getSizeDescription(app)));
			hide(pbProgress);
			show(tvDescription);
		});
		ivSecIcon.setImageResource(R.drawable.ic_action_gsave_dark);
		ivSecIcon.setColorFilter(getAppModeColor());
		view.setOnClickListener(view -> startDownload());
	}

	private void applyDownloadInProgressState() {
		show(ivSecIcon, pbProgress);
		hide(tvDescription);

		ivIcon.setColorFilter(getDefaultIconColor());
		ivSecIcon.setImageResource(R.drawable.ic_action_remove_dark);
		ivSecIcon.setColorFilter(getDefaultIconColor());
		pbProgress.setIndeterminate(false);
		pbProgress.setProgress(0);
		view.setOnClickListener(view -> stopDownload());
	}

	private void applyDownloadedState() {
		show(tvDescription, ivSecIcon);
		hide(pbProgress);

		ivIcon.setColorFilter(getAppModeColor());
		String pattern = app.getString(R.string.ltr_or_rtl_combine_via_dash);
		String caption = app.getString(R.string.available_until, "").trim();
		String expireDate = OsmAndFormatter.getFormattedDate(app, indexItem.getDataExpireTime());
		tvDescription.setText(String.format(pattern, caption, expireDate));
		ivSecIcon.setImageResource(R.drawable.ic_overflow_menu_white);
		ivSecIcon.setColorFilter(getDefaultIconColor());
		view.setOnClickListener(view -> showPopUpMenu());
	}

	private void applyRemoveInProgressState() {
		show(pbProgress);
		hide(tvDescription, ivSecIcon);

		pbProgress.setIndeterminate(true);
		view.setOnClickListener(view -> {
			// do nothing
		});
	}

	private void showPopUpMenu() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_details)
				.setIcon(getContentIcon(R.drawable.ic_action_info_outlined))
				.setOnClickListener(v -> {
					// not implemented yet
				})
				.create()
		);

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_update)
				.setIcon(getContentIcon(R.drawable.ic_action_refresh_dark))
				.setOnClickListener(v -> startDownload())
				.create()
		);

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> confirmRemove())
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view.findViewById(R.id.secondary_icon);
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider;
		PopUpMenu.show(displayData);
	}

	private void calculateIndexItemSize(OnCompleteCallback onComplete) {
		OfflineForecastHelper helper = app.getOfflineForecastHelper();
		helper.calculateCacheSizeIfNeeded(indexItem, onComplete);
	}

	private void startDownload() {
		downloadManager.startDownload(activity, indexItem);
	}

	private void stopDownload() {
		if (downloadThread.isDownloading(indexItem)) {
			downloadManager.makeSureUserCancelDownload(activity, indexItem);
		}
	}

	private void confirmRemove() {
		confirmWeatherRemove(activity, indexItem);
	}

	public static void confirmWeatherRemove(@NonNull Context context, @NonNull WeatherIndexItem indexItem) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		AlertDialog.Builder confirm = new AlertDialog.Builder(context);

		StringBuilder fileName = new StringBuilder()
				.append(getWeatherName(app, app.getRegions(), indexItem.getRegionId())).append(" ")
				.append(DownloadActivityType.WEATHER_FORECAST.getString(app));
		String message = context.getString(R.string.delete_confirmation_msg, fileName);
		confirm.setMessage(message);

		confirm.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			OfflineForecastHelper helper = app.getOfflineForecastHelper();
			helper.removeLocalForecastAsync(indexItem.getRegionId(), true, true);
		});
		confirm.setNegativeButton(R.string.shared_string_no, null);
		confirm.show();
	}

	@NonNull
	public View getView() {
		return view;
	}

	@ColorInt
	private int getAppModeColor() {
		return appMode.getProfileColor(nightMode);
	}

	@ColorInt
	private int getDefaultIconColor() {
		return ColorUtilities.getDefaultIconColor(app, nightMode);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int resId) {
		UiUtilities iconsCache = app.getUIUtilities();
		return iconsCache.getThemedIcon(resId);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int profileColor = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	private void show(@NonNull View ... views) {
		AndroidUiHelper.setVisibility(View.VISIBLE, views);
	}

	private void hide(@NonNull View ... views) {
		AndroidUiHelper.setVisibility(View.GONE, views);
	}

}
