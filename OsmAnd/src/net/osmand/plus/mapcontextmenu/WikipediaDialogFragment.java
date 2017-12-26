package net.osmand.plus.mapcontextmenu;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public class WikipediaDialogFragment extends DialogFragment {

	public static final String PREFERRED_LANGUAGE = "preferred_language";

	private boolean darkTheme;
	private Amenity amenity;

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		OsmandApplication app = getMyApplication();
		darkTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_DARK_THEME;
		int themeId = darkTheme ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = new Dialog(getContext(), getTheme());
		if (!getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_Alpha;
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View mainView = inflater.inflate(R.layout.wikipedia_dialog_fragment, container, false);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		String preferredLanguage = "";
		Bundle args = getArguments();
		if (args != null) {
			preferredLanguage = args.getString(PREFERRED_LANGUAGE);
		}

		if (TextUtils.isEmpty(preferredLanguage)) {
			preferredLanguage = getMyApplication().getLanguage();
		}

		final String title = TextUtils.isEmpty(preferredLanguage) ? amenity.getName() : amenity.getName(preferredLanguage);
		((TextView) mainView.findViewById(R.id.title_text_view)).setText(title);

		String langSelected = amenity.getContentLanguage("content", preferredLanguage, "en");
		if (Algorithms.isEmpty(langSelected)) {
			langSelected = "en";
		}

		String content = amenity.getDescription(langSelected);

		return mainView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.setDismissMessage(null);
		}
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
}
