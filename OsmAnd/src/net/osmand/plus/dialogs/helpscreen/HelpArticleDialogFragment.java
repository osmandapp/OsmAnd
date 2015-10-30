package net.osmand.plus.dialogs.helpscreen;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpArticleDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(HelpArticleDialogFragment.class);

	public static final String FILE_NAME = "url";
	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = (getOsmandApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_help_article, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		String fileContents = getAssetAsString(getArguments().getString(FILE_NAME), getActivity());
		webView = (WebView) view.findViewById(R.id.webView);
		if (savedInstanceState != null) {
			webView.restoreState(savedInstanceState);
		} else {
			webView.loadDataWithBaseURL("http://osmand.net", fileContents, null, "utf-8", null);
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
	}

	private OsmandApplication getOsmandApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static HelpArticleDialogFragment createInstance(String fileName) {
		Bundle args = new Bundle();
		args.putString(FILE_NAME, fileName);
		final HelpArticleDialogFragment helpArticleDialogFragment = new HelpArticleDialogFragment();
		helpArticleDialogFragment.setArguments(args);
		return helpArticleDialogFragment;
	}

	public String getAssetAsString(String filename, Context context) {
		StringBuilder buf = new StringBuilder();
		InputStream is;
		BufferedReader in = null;
		try {
			is = context.getAssets().open(filename);
			in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String str;

			while ((str = in.readLine()) != null) {
				buf.append(str);
			}
		} catch (IOException e) {
			LOG.error(null, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LOG.error(null, e);
				}
			}
		}
		return buf.toString();
	}
}
