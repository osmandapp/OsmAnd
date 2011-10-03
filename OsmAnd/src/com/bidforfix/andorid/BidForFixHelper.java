package com.bidforfix.andorid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class BidForFixHelper {

	private static final long DAY = 1000*60*60*24; //A DAY in millis
	
	private List<BFFIssue> bffIssues = new ArrayList<BFFIssue>();
	private final String project;
	private Thread t;
	private Date initialized;

	private final String supportButton;

	private final String cancelButton;

	public static class BFFIssue {
		private final String link;
		private final String name;
		private final String shortname;
		private final String descripton;

		public BFFIssue(JSONObject jsonObject) {
			this.link = getValue(jsonObject, "link");
			this.name = getValue(jsonObject, "name");
			this.shortname = getValue(jsonObject, "short_name");
			this.descripton = getValue(jsonObject, "description");
		}

		private String getValue(JSONObject jsonObject, String key) {
			try {
				return jsonObject.getString(key);
			} catch (JSONException e) {
				// ignore
			}
			return null;
		}

		public BFFIssue(String link, String name, String shortname,
				String descripton) {
			this.link = link;
			this.name = name;
			this.shortname = shortname;
			this.descripton = descripton;
		}

		public String getDescripton() {
			return descripton;
		}

		public String getLink() {
			if (link.startsWith("http://")) {
				return link;
			} else {
				return "http://www.bidforfix.com"+link;
			}
		}

		public String getName() {
			return name;
		}

		public String getShortname() {
			return shortname;
		}
	}

	public BidForFixHelper(String project, String supportButton, String cancelButton) {
		this.project = project;
		this.supportButton = supportButton;
		this.cancelButton = cancelButton;
	}

	private void loadList() {
		if (initialized == null || initialized.before(new Date(System.currentTimeMillis()-DAY))) {
			BufferedReader in = null;
			String url = "http://www.bidforfix.com/p/" + project + "/issues/";
			try {
				URL twitter = new URL(url);
				URLConnection tc = twitter.openConnection();
				in = new BufferedReader(new InputStreamReader(
						tc.getInputStream()));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				JSONTokener json = new JSONTokener(sb.toString());
				JSONObject root = (JSONObject) json.nextValue();
				if (root != null) {
					JSONArray issues = root.getJSONArray("issues");
					for (int i = 0; i < issues.length(); i++) {
						JSONObject jo = (JSONObject) issues.get(i);
						bffIssues.add(new BFFIssue(jo));
					}
				}
				initialized = new Date();
			} catch (MalformedURLException e) {
				initialized = new Date(Long.MAX_VALUE); //bad url, don't try anymore
				Log.w("Bad URL:" + url, e);
			} catch (IOException e) {
				//we can try some more times...
			} catch (JSONException e) {
				//bad json, try in two day again
				initialized = new Date(System.currentTimeMillis()+DAY*2);
				Log.w("Bad JSON while parsing bidforfix output", e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ex) {
						// ignore
					}
				}
			}
		}
	}

	public void generatePreferenceList(final PreferenceScreen screen,
			final String categoryName, final Activity context) {
		t = new Thread("BidForFixThread") {
			@Override
			public void run() {
				loadList();
				// after loading, fill the screen
				if (!bffIssues.isEmpty()) {
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							PreferenceCategory cat = new PreferenceCategory(
									context);
							cat.setTitle(categoryName);
							screen.addPreference(cat);
							for (int i = 0; i < bffIssues.size(); i++) {
								final BFFIssue issue = bffIssues.get(i);
								Preference preference = new Preference(context);
								preference.setTitle(issue.getName());
								preference.setSummary(issue.getDescripton());
								preference
										.setOnPreferenceClickListener(new OnPreferenceClickListener() {
											@Override
											public boolean onPreferenceClick(
													Preference preference) {
												Builder builder = new AlertDialog.Builder(context);
												builder.setTitle(issue.name);
												builder.setMessage(issue.descripton);
												builder.setPositiveButton(supportButton, new OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														//edit the preference
														String url = issue.getLink();  
														try {
														    Intent i = new Intent(Intent.ACTION_VIEW);  
														    i.setData(Uri.parse(url));  
														    context.startActivity(i);
														} catch (ActivityNotFoundException ex) {
															Toast.makeText(context, ex.getMessage() + " for " + url, Toast.LENGTH_LONG).show();
														}
													}
												});
												builder.setNegativeButton(cancelButton, null);
												builder.show();

												return false;
											}
										});
								cat.addPreference(preference);
							}
						}
					});
				}
				t = null;
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public void onDestroy() {
		// close the thread, release resources...
		if (t != null) {
			try {
				t.join(); // wait for the thread
				// TODO we should wait for the runOnUIThread??? is that possible
				// if we are in UI thread now?
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
}
