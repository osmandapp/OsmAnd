package net.osmand.plus.plugins.rastermaps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.DownloadType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import java.text.MessageFormat;

public class TilesDownloadProgressFragment extends BaseOsmAndFragment implements TilesDownloadListener {

	public static final String TAG = TilesDownloadProgressFragment.class.getSimpleName();

	private static final int BYTES_TO_MB = 1024 * 1024;

	public static final String KEY_LEFT_LON = "left_lon";
	public static final String KEY_TOP_LAT = "top_lat";
	public static final String KEY_RIGHT_LON = "right_lon";
	public static final String KEY_BOTTOM_LAT = "bottom_lat";
	public static final String KEY_MIN_ZOOM = "min_zoom";
	public static final String KEY_MAX_ZOOM = "max_zoom";
	public static final String KEY_DOWNLOAD_TYPE = "download_type";
	public static final String KEY_MISSING_TILES = "missing_tiles";
	public static final String KEY_MISSING_SIZE_MB = "missing_size_mb";

	private static final String KEY_PROGRESS = "progress";
	private static final String KEY_DOWNLOADED_SIZE_MB = "downloaded_size_mb";
	private static final String KEY_DOWNLOADED_TILES_NUMBER = "downloaded_tiles_number";

	private OsmandApplication app;
	private DownloadTilesHelper downloadTilesHelper;
	private boolean nightMode;

	private View view;

	private QuadRect latLonRect;
	private int minZoom;
	private int maxZoom;
	private DownloadType downloadType;
	private boolean approximate;

	private int progress;
	private float downloadedSizeMb;
	private long downloadedTilesNumber;

	private long totalTilesNumber;
	private float approxSizeMb;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				dismiss(true);
			}
		});

		app = requireMyApplication();
		downloadTilesHelper = app.getDownloadTilesHelper();
		nightMode = isNightMode(true);

		Bundle args = getArguments();
		if (args != null) {
			readArgs(args);
		}

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
	}

	private void restoreState(@NonNull Bundle savedState) {
		progress = savedState.getInt(KEY_PROGRESS);
		downloadedSizeMb = savedState.getFloat(KEY_DOWNLOADED_SIZE_MB);
		downloadedTilesNumber = savedState.getLong(KEY_DOWNLOADED_TILES_NUMBER);
	}

	private void readArgs(@NonNull Bundle args) {
		minZoom = args.getInt(KEY_MIN_ZOOM);
		maxZoom = args.getInt(KEY_MAX_ZOOM);
		latLonRect = new QuadRect();
		latLonRect.left = args.getDouble(KEY_LEFT_LON);
		latLonRect.top = args.getDouble(KEY_TOP_LAT);
		latLonRect.right = args.getDouble(KEY_RIGHT_LON);
		latLonRect.bottom = args.getDouble(KEY_BOTTOM_LAT);
		downloadType = DownloadType.valueOf(args.getString(KEY_DOWNLOAD_TYPE));
		if (downloadType == DownloadType.ONLY_MISSING) {
			if (args.containsKey(KEY_MISSING_TILES)) {
				approximate = true;
				totalTilesNumber = args.getLong(KEY_MISSING_TILES);
				approxSizeMb = args.getFloat(KEY_MISSING_SIZE_MB);
			} else {
				totalTilesNumber = -1;
				approxSizeMb = -1;
			}
		} else {
			ITileSource tileSource = requireSettings().getMapTileSource(false);
			boolean ellipticYTile = tileSource.isEllipticYTile();
			totalTilesNumber = DownloadTilesHelper.getTilesNumber(minZoom, maxZoom, latLonRect, ellipticYTile);
			approxSizeMb = DownloadTilesHelper.getApproxTilesSizeMb(minZoom, maxZoom, latLonRect, tileSource,
					app.getResourceManager().getBitmapTilesCache());
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		view = themedInflater.inflate(R.layout.tiles_download_progress_fragment, container, false);

		setupToolbar();
		updateProgress();
		updateDownloadSize();
		updateTilesNumber();
		setupCancelCloseButton(downloadTilesHelper.isDownloadFinished());

		return view;
	}

	private void setupToolbar() {
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);
		View toolbar = view.findViewById(R.id.toolbar);

		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(v -> dismiss(true));
		UiUtilities.rotateImageByLayoutDirection(backButton);

		View helpButton = toolbar.findViewById(R.id.help_button);
		helpButton.setOnClickListener(v -> {
			Context context = getContext();
			if (context != null) {
				AndroidUtils.openUrl(context, R.string.docs_map_download_tiles, nightMode);
			}
		});
	}

	private void updateProgress() {
		MessageFormat format = new MessageFormat("{0}% {1}");
		String text;
		if (progress == 100) {
			String completeString = getString(R.string.shared_string_complete);
			text = format.format(new Object[] {progress, completeString});
		} else if (downloadTilesHelper.isDownloadFinished()) {
			text = getString(R.string.download_complete);
		} else {
			String downloadedString = getString(R.string.shared_string_download_successful).toLowerCase();
			text = format.format(new Object[] {progress, downloadedString});
		}
		((TextView) view.findViewById(R.id.percent_progress)).setText(text);

		ProgressBar progressBar = view.findViewById(R.id.progress_bar);
		progressBar.setProgress(downloadTilesHelper.isDownloadFinished() ? 100 : progress);
	}

	@SuppressLint("StringFormatMatches")
	private void updateDownloadSize() {
		String downloadedString = getString(R.string.shared_string_download_successful);
		String downloadedSize = getSizeMb(downloadedSizeMb);
		String expectedSize = MessageFormat.format("(~{0})", getSizeMb(approxSizeMb));
		String fullText;
		boolean showExpectedSize = progress != 100 && !downloadTilesHelper.isDownloadFinished() && approxSizeMb != -1;
		if (!showExpectedSize) {
			fullText = getString(R.string.ltr_or_rtl_combine_via_colon, downloadedString, downloadedSize);
		} else {
			fullText = getString(R.string.ltr_or_rtl_combine_via_colon,
					downloadedString,
					getString(R.string.ltr_or_rtl_combine_via_space, downloadedSize, expectedSize));
		}

		Spannable spannable = new SpannableString(fullText);

		Typeface bold = FontCache.getRobotoMedium(app);
		setSpan(spannable, new CustomTypefaceSpan(bold), fullText.indexOf(downloadedSize), downloadedSize.length());

		if (showExpectedSize) {
			ForegroundColorSpan span = new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode));
			setSpan(spannable, span, fullText.indexOf(expectedSize), expectedSize.length());
		}

		TextView downloadedText = view.findViewById(R.id.downloaded_size);
		downloadedText.setText(spannable);
	}

	private void updateTilesNumber() {
		String tilesString = getString(R.string.shared_sting_tiles);
		String downloadedNumber = formatNumber(downloadedTilesNumber, 0);

		TextView tvDownloaded = view.findViewById(R.id.downloaded_number);
		if (totalTilesNumber == -1) {
			tvDownloaded.setText(getString(R.string.ltr_or_rtl_combine_via_colon, tilesString, downloadedNumber));
		} else {
			String totalFormat = approximate ? "(~{0})" : "({0})";
			String totalNumber = MessageFormat.format(totalFormat, formatNumber(totalTilesNumber, 0));
			String fullText = getString(R.string.ltr_or_rtl_combine_via_colon,
					tilesString,
					getString(R.string.ltr_or_rtl_combine_via_space, downloadedNumber, totalNumber));

			Spannable spannable = new SpannableString(fullText);
			ForegroundColorSpan span = new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode));
			setSpan(spannable, span, fullText.indexOf(totalNumber), totalNumber.length());
			tvDownloaded.setText(spannable);
		}
	}

	private void setupCancelCloseButton(boolean downloadFinished) {
		View cancelButton = view.findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(v -> dismiss(true));
		int buttonTextId = downloadFinished
				? R.string.shared_string_close
				: R.string.shared_string_cancel;
		UiUtilities.setupDialogButton(nightMode, cancelButton, DialogButtonType.SECONDARY, buttonTextId);
	}

	public void dismiss(boolean showWarningIfDownloading) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (showWarningIfDownloading && !downloadTilesHelper.isDownloadFinished()) {
				StopDownloadBottomSheetDialogFragment.showInstance(fragmentManager, this);
			} else {
				downloadTilesHelper.clearDownload();
				if (!fragmentManager.isStateSaved()) {
					fragmentManager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
				}
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		downloadTilesHelper.setListener(this);
		if (!downloadTilesHelper.isDownloadStarted()) {
			ITileSource tileSource = app.getSettings().getMapTileSource(false);
			downloadTilesHelper.downloadTiles(minZoom, maxZoom, latLonRect, tileSource, downloadType);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		downloadTilesHelper.setListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_PROGRESS, progress);
		outState.putLong(KEY_DOWNLOADED_TILES_NUMBER, downloadedTilesNumber);
		outState.putFloat(KEY_DOWNLOADED_SIZE_MB, downloadedSizeMb);
	}

	@Override
	public void onTileDownloaded(long tileNumber, long cumulativeTilesSize) {
		downloadedTilesNumber = tileNumber + 1;
		progress = BasicProgressAsyncTask.normalizeProgress((int) ((float) downloadedTilesNumber / totalTilesNumber * 100));
		downloadedSizeMb = (float) cumulativeTilesSize / BYTES_TO_MB;

		updateProgress();
		updateDownloadSize();
		updateTilesNumber();

		if (progress == 100 && !approximate) {
			setupCancelCloseButton(true);
			app.getOsmandMap().getMapView().refreshMap();
		}
	}

	@Override
	public void onSuccessfulFinish() {
		if (progress != 100 || approximate) {
			updateProgress();
			updateDownloadSize();
			updateTilesNumber();
			setupCancelCloseButton(true);
			app.getOsmandMap().getMapView().refreshMap();
		}
	}

	@Override
	public void onDownloadFailed() {
		dismiss(false);
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@SuppressLint("StringFormatMatches")
	@NonNull
	private String getSizeMb(float size) {
		String formattedSize = formatNumber(size, 2);
		return getString(R.string.shared_string_memory_mb_desc, formattedSize);
	}

	@NonNull
	private String formatNumber(float number, int decimalPlacesNumber) {
		return OsmAndFormatter.formatValue(number, "", false, decimalPlacesNumber, app).value;
	}

	private void setSpan(@NonNull Spannable spannable, @NonNull CharacterStyle span, int start, int length) {
		spannable.setSpan(span, start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Bundle args) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TilesDownloadProgressFragment fragment = new TilesDownloadProgressFragment();
			fragment.setArguments(args);

			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}