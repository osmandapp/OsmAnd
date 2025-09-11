package net.osmand.plus.activities;

import static net.osmand.plus.activities.OsmAndBuild.DATE_FORMAT;
import static net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_NAME;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import net.osmand.plus.R;
import net.osmand.plus.base.OsmandListActivity;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContributionVersionActivity extends OsmandListActivity {

	private static ContributionVersionsThread VERSIONS_THREAD = new ContributionVersionsThread();

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({DOWNLOAD_BUILDS_LIST, INSTALL_BUILD, ACTIVITY_TO_INSTALL})
	public @interface OperationType {
	}

	public static final int DOWNLOAD_BUILDS_LIST = 1;
	public static final int INSTALL_BUILD = 2;
	public static final int ACTIVITY_TO_INSTALL = 23;

	public final List<OsmAndBuild> downloadedBuilds = new ArrayList<>();

	public File pathToDownload;
	public Date currentInstalledDate;
	public OsmAndBuild currentSelectedBuild;

	public ProgressDialog progressDlg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pathToDownload = getApp().getAppPath(DOWNLOAD_BUILD_NAME);
		setContentView(R.layout.contribution_version_activity);
		getSupportActionBar().setSubtitle(R.string.select_build_to_install);

		String installDate = getApp().getSettings().CONTRIBUTION_INSTALL_APP_DATE.get();
		if (installDate != null) {
			try {
				currentInstalledDate = DATE_FORMAT.parse(installDate);
			} catch (ParseException e) {
			}
		}

		downloadedBuilds.clear();
		startThreadOperation(DOWNLOAD_BUILDS_LIST, getString(R.string.loading_builds), -1);
	}

	private void startThreadOperation(@OperationType int operationId, String message, int total) {
		progressDlg = new ProgressDialog(this);
		progressDlg.setTitle(getString(R.string.loading_smth, ""));
		progressDlg.setMessage(message);
		if (total != -1) {
			progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDlg.setMax(total);
			progressDlg.setProgress(0);
		} else {
			progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		progressDlg.show();
//		progressFileDlg.setCancelable(false);
		if (VERSIONS_THREAD.getState() == Thread.State.TERMINATED || VERSIONS_THREAD.getOperationId() != operationId) {
			VERSIONS_THREAD = new ContributionVersionsThread();
			VERSIONS_THREAD.setOperationId(operationId);
		}
		VERSIONS_THREAD.setActivity(this);
		if (VERSIONS_THREAD.getState() == Thread.State.NEW) {
			VERSIONS_THREAD.start();
		}
	}

	protected void endThreadOperation(@OperationType int operationId, Exception e) {
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
		if (operationId == DOWNLOAD_BUILDS_LIST) {
			if (e != null) {
				getApp().showToastMessage(getString(R.string.loading_builds_failed) + " : " + e.getMessage());
				finish();
			} else {
				setListAdapter(new OsmandBuildsAdapter(downloadedBuilds));
			}
		} else if (operationId == INSTALL_BUILD) {
			if (currentSelectedBuild != null) {
				Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pathToDownload);
				Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
				intent.setData(uri);
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivityForResult(intent, ACTIVITY_TO_INSTALL);
				updateInstalledApp(false, currentSelectedBuild.date);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (ACTIVITY_TO_INSTALL == requestCode && resultCode != RESULT_OK && currentInstalledDate != null) {
			updateInstalledApp(false, currentInstalledDate);
		}
	}

	private void updateInstalledApp(boolean showMessage, Date d) {
		if (showMessage) {
			app.showToastMessage(MessageFormat.format(getString(R.string.build_installed),
					currentSelectedBuild.tag, AndroidUtils.formatDateTime(app, currentSelectedBuild.date.getTime())));
		}
		settings.CONTRIBUTION_INSTALL_APP_DATE.set(DATE_FORMAT.format(d));
	}

	@Override
	public OsmandBuildsAdapter getListAdapter() {
		return (OsmandBuildsAdapter) super.getListAdapter();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VERSIONS_THREAD.setActivity(null);
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final OsmAndBuild item = (OsmAndBuild) getListAdapter().getItem(position);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(MessageFormat.format(getString(R.string.install_selected_build), item.tag,
				AndroidUtils.formatDateTime(app, item.date.getTime()), item.size));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			currentSelectedBuild = item;
			int kb = (int) (Double.parseDouble(item.size) * 1024);
			startThreadOperation(INSTALL_BUILD, getString(R.string.downloading_build), kb);
		});

		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.show();
	}

	protected class OsmandBuildsAdapter extends ArrayAdapter<OsmAndBuild> implements Filterable {

		public OsmandBuildsAdapter(List<OsmAndBuild> builds) {
			super(ContributionVersionActivity.this, R.layout.download_build_list_item, builds);
		}

		@NonNull
		@Override
		public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.download_build_list_item, parent, false);
			}
			final View row = v;
			OsmAndBuild build = getItem(position);
			TextView tagView = (TextView) row.findViewById(R.id.download_tag);
			tagView.setText(build.tag);

			if (build.date != null) {
				TextView description = (TextView) row.findViewById(R.id.download_descr);
				StringBuilder format = new StringBuilder();
				format.append(AndroidUtils.formatDateTime(app, build.date.getTime()))/*.append(" : ").append(build.size).append(" MB")*/;
				description.setText(format.toString());
				int color = getColor(R.color.text_color_secondary_dark);
				if (currentInstalledDate != null) {
					if (currentInstalledDate.before(build.date)) {
						color = getColor(R.color.color_update);
					}
				}
				description.setTextColor(color);
				tagView.setTextColor(color);
			}

			return row;
		}
	}
}
