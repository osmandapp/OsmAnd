package net.osmand.plus.plugins.weather.viewholder;

import static net.osmand.plus.plugins.weather.viewholder.WeatherTotalCacheSizeViewHolder.CacheSizeViewHolderState.DEFAULT;
import static net.osmand.plus.plugins.weather.viewholder.WeatherTotalCacheSizeViewHolder.CacheSizeViewHolderState.INDETERMINATE;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.utils.UiUtilities;

import java.text.DecimalFormat;

public class WeatherTotalCacheSizeViewHolder {

	private final OsmandApplication app;
	private final OfflineForecastHelper helper;
	private final boolean forLocal;

	private final TextView tvSummary;
	private final ImageView ivSecIcon;
	private final ProgressBar pbProgress;

	private CacheSizeViewHolderState currentState;

	public enum CacheSizeViewHolderState {
		DEFAULT, INDETERMINATE
	}

	public WeatherTotalCacheSizeViewHolder(@NonNull OsmandApplication app, @NonNull View view,
	                                       boolean forLocal) {
		this.app = app;
		this.forLocal = forLocal;
		helper = app.getOfflineForecastHelper();

		tvSummary = view.findViewById(android.R.id.summary);
		ivSecIcon = view.findViewById(R.id.secondaryIcon);
		pbProgress = view.findViewById(R.id.progress);
		update();
	}

	public void update() {
		WeatherTotalCacheSize cacheSize = helper.getTotalCacheSize();
		if (forLocal) {
			applyState(cacheSize.isCalculated(true) ? DEFAULT : INDETERMINATE);
		} else {
			boolean indeterminate =
					!cacheSize.isCalculated(false)
							|| helper.isClearOnlineCacheInProgress();
			applyState(indeterminate ? INDETERMINATE : DEFAULT);
		}
	}

	private void applyState(@NonNull CacheSizeViewHolderState newState) {
		if (this.currentState != newState) {
			this.currentState = newState;
			applyState();
		}
	}

	private void applyState() {
		if (currentState == INDETERMINATE) {
			applyIndeterminateState();
		} else {
			applyDefaultState();
		}
	}

	private void applyDefaultState() {
		show(tvSummary);
		hide(pbProgress, ivSecIcon);
		if (!forLocal) {
			UiUtilities iconsCache = app.getUIUtilities();
			ivSecIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark));
			AndroidUiHelper.updateVisibility(ivSecIcon, helper.canClearOnlineCache());
		}
		WeatherTotalCacheSize totalCacheSize = helper.getTotalCacheSize();
		long size = totalCacheSize.get(forLocal);
		tvSummary.setText(formatSize(size));
	}

	private void applyIndeterminateState() {
		show(pbProgress);
		hide(tvSummary, ivSecIcon);
		pbProgress.setIndeterminate(true);
	}

	private void show(@NonNull View ... views) {
		AndroidUiHelper.setVisibility(View.VISIBLE, views);
	}

	private void hide(@NonNull View ... views) {
		AndroidUiHelper.setVisibility(View.GONE, views);
	}

	private String formatSize(long totalSize) {
		DecimalFormat decimalFormat = new DecimalFormat("#.#");
		String size = decimalFormat.format(totalSize / (1024f * 1024f));
		return app.getString(R.string.ltr_or_rtl_combine_via_space, size, "MB");
	}

}
