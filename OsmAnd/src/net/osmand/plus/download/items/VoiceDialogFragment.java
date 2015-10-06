package net.osmand.plus.download.items;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.items.ItemsListBuilder.VoicePromptsType;

import org.apache.commons.logging.Log;

public class VoiceDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(VoiceDialogFragment.class);
	public static final String TAG = "VoiceDialogFragment";
	private static final String VOICE_PROMPT_TYPE_DLG_KEY = "voice_prompt_type_dlg_key";
	private VoicePromptsType voicePromptsType = VoicePromptsType.NONE;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = ((OsmandApplication) getActivity().getApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

		String value = null;
		try {
			if (savedInstanceState != null) {
				value = savedInstanceState.getString(VOICE_PROMPT_TYPE_DLG_KEY);
				if (value != null) {
					voicePromptsType = VoicePromptsType.valueOf(value);
				}
			}
			if (voicePromptsType == VoicePromptsType.NONE) {
				value = getArguments().getString(VOICE_PROMPT_TYPE_DLG_KEY);
				if (value != null) {
					voicePromptsType = VoicePromptsType.valueOf(value);
				}
			}
		} catch (IllegalArgumentException e) {
			LOG.warn("VOICE_PROMPT_TYPE_DLG_KEY=" + value);
		}

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		if (voicePromptsType != VoicePromptsType.NONE) {
			Fragment fragment = getChildFragmentManager().findFragmentById(R.id.fragmentContainer);
			if (fragment == null) {
				getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer,
						VoiceItemsFragment.createInstance(voicePromptsType)).commit();
			}

			ItemsListBuilder builder = getDownloadActivity().getItemsBuilder();
			toolbar.setTitle(builder.getVoicePromtName(voicePromptsType));
		}
		((DownloadActivity)getActivity()).initFreeVersionBanner(view);

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(VOICE_PROMPT_TYPE_DLG_KEY, voicePromptsType.name());
		super.onSaveInstanceState(outState);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static VoiceDialogFragment createInstance(VoicePromptsType voicePromptsType) {
		Bundle bundle = new Bundle();
		bundle.putString(VOICE_PROMPT_TYPE_DLG_KEY, voicePromptsType.name());
		VoiceDialogFragment fragment = new VoiceDialogFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}


