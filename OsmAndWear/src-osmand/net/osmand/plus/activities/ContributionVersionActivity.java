package net.osmand.plus.activities;

import static net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_NAME;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.OsmandListActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContributionVersionActivity extends OsmandListActivity {

	private static ContributionVersionActivityThread thread = new ContributionVersionActivityThread();
	private static final int DOWNLOAD_BUILDS_LIST = 1;
	private static final int INSTALL_BUILD = 2;
	private static final int ACTIVITY_TO_INSTALL = 23;

	private static final String URL_TO_RETRIEVE_BUILDS = "https://osmand.net/builds";
	private static final String URL_GET_BUILD = "https://osmand.net/";

	private ProgressDialog progressDlg;
	private Date currentInstalledDate;

	private final List<OsmAndBuild> downloadedBuilds = new ArrayList<>();
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
	private File pathToDownload;
	private OsmAndBuild currentSelectedBuild = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pathToDownload = getMyApplication().getAppPath(DOWNLOAD_BUILD_NAME);
		setContentView(R.layout.default_list_view);
		getSupportActionBar().setSubtitle(R.string.select_build_to_install);

		String installDate = getMyApplication().getSettings().CONTRIBUTION_INSTALL_APP_DATE.get();
		if (installDate != null) {
			try {
				currentInstalledDate = dateFormat.parse(installDate);
			} catch (ParseException e) {
			}
		}

		downloadedBuilds.clear();
		startThreadOperation(DOWNLOAD_BUILDS_LIST, getString(R.string.loading_builds), -1);
	}

	private void startThreadOperation(int operationId, String message, int total) {

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
		if (thread.getState() == Thread.State.TERMINATED || thread.getOperationId() != operationId) {
			thread = new ContributionVersionActivityThread();
			thread.setOperationId(operationId);
		}
		thread.setActivity(this);
		if (thread.getState() == Thread.State.NEW) {
			thread.start();
		}
	}

	protected void endThreadOperation(int operationId, Exception e) {
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
		if (operationId == DOWNLOAD_BUILDS_LIST) {
			if (e != null) {
				Toast.makeText(this, getString(R.string.loading_builds_failed) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
				finish();
			} else {
				setListAdapter(new OsmandBuildsAdapter(downloadedBuilds));
			}
		} else if (operationId == INSTALL_BUILD) {
			if (currentSelectedBuild != null) {
				Intent intent;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pathToDownload);
					intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
					intent.setData(apkUri);
				} else {
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(pathToDownload), "application/vnd.android.package-archive");
				}
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
			Toast.makeText(this,
					MessageFormat.format(getString(R.string.build_installed), currentSelectedBuild.tag,
							AndroidUtils.formatDateTime(getMyApplication(), currentSelectedBuild.date.getTime())),
					Toast.LENGTH_LONG).show();
		}
		getMyApplication().getSettings().CONTRIBUTION_INSTALL_APP_DATE.set(dateFormat.format(d));
	}

	protected void executeThreadOperation(int operationId) throws Exception {
		if (operationId == DOWNLOAD_BUILDS_LIST) {
			URLConnection connection = NetworkUtils.getHttpURLConnection(URL_TO_RETRIEVE_BUILDS);
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setInput(connection.getInputStream(), "UTF-8");
			int next;
			while ((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (next == XmlPullParser.START_TAG && parser.getName().equals("build")) { //$NON-NLS-1$
					if ("osmand".equalsIgnoreCase(parser.getAttributeValue(null, "type"))) {

						String path = parser.getAttributeValue(null, "path"); //$NON-NLS-1$
						String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
						String date = parser.getAttributeValue(null, "timestamp"); //$NON-NLS-1$
						String tag = parser.getAttributeValue(null, "tag"); //$NON-NLS-1$
						Date d = null;
						if (date != null) {
							try {
								d = new Date(Long.parseLong(date));
							} catch (RuntimeException e) {
								e.printStackTrace();
							}
						}
						OsmAndBuild build = new OsmAndBuild(path, size, d, tag);
						if (!Version.isFreeVersion(getMyApplication()) || path.contains("default")) {
							downloadedBuilds.add(build);
						}
					}
				}
			}
		} else if (operationId == INSTALL_BUILD) {
			URLConnection connection = NetworkUtils.getHttpURLConnection(URL_GET_BUILD + currentSelectedBuild.path);
			if (pathToDownload.exists()) {
				pathToDownload.delete();
			}
			int bufflen = 16000;
			byte[] buffer = new byte[bufflen];
			InputStream is = connection.getInputStream();
			FileOutputStream fout = new FileOutputStream(pathToDownload);
			try {
				int totalRead = 0;
				int read;
				while ((read = is.read(buffer, 0, bufflen)) != -1) {
					fout.write(buffer, 0, read);
					totalRead += read;
					if (totalRead > 1024) {
						progressDlg.incrementProgressBy(totalRead / 1024);
						totalRead %= 1024;
					}
				}
			} finally {
				fout.close();
				is.close();
			}
		}
	}

	@Override
	public OsmandBuildsAdapter getListAdapter() {
		return (OsmandBuildsAdapter) super.getListAdapter();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		thread.setActivity(null);
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
				AndroidUtils.formatDateTime(getMyApplication(), item.date.getTime()), item.size));
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				currentSelectedBuild = item;
				int kb = (int) (Double.parseDouble(item.size) * 1024);
				startThreadOperation(INSTALL_BUILD, getString(R.string.downloading_build), kb);
			}
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
				format.append(AndroidUtils.formatDateTime(getMyApplication(), build.date.getTime()))/*.append(" : ").append(build.size).append(" MB")*/;
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

	private static class ContributionVersionActivityThread extends Thread {
		private ContributionVersionActivity activity;
		private int operationId;

		public void setActivity(ContributionVersionActivity activity) {
			this.activity = activity;
		}

		public int getOperationId() {
			return operationId;
		}

		public void setOperationId(int operationId) {
			this.operationId = operationId;
		}

		@Override
		public void run() {
			Exception ex = null;
			try {
				if (this.activity != null) {
					this.activity.executeThreadOperation(operationId);
				}
			} catch (Exception e) {
				ex = e;
			}
			final Exception e = ex;
			if (this.activity != null) {
				this.activity.runOnUiThread(() -> {
					if (activity != null) {
						activity.endThreadOperation(operationId, e);
					}
				});
			}
		}
	}

	private static class OsmAndBuild {
		public String path;
		public String size;
		public Date date;
		public String tag;

		public OsmAndBuild(String path, String size, Date date, String tag) {
			super();
			this.path = path;
			this.size = size;
			this.date = date;
			this.tag = tag;
		}
	}
}
