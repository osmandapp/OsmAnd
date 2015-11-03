package net.osmand.plus.activities;

import android.os.Bundle;

import net.osmand.plus.dialogs.helpscreen.HelpScreenDialogFragment;


public class HelpActivity extends OsmandActionBarActivity {

	public static final String DIALOG = "dialog";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getSupportFragmentManager().findFragmentByTag(DIALOG) == null) {
			new HelpScreenDialogFragment().show(getSupportFragmentManager(), DIALOG);
		}
	}
}
