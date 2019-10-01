package net.osmand.plus.base;

public abstract class TopSheetDialogFragment extends OsmAndSheetDialogFragment {
	
	@Override
	protected SheetDialogType getSheetDialogType() {
		return SheetDialogType.TOP;
	}
}
