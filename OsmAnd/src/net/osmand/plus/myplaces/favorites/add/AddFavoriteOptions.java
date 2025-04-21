package net.osmand.plus.myplaces.favorites.add;

public class AddFavoriteOptions {

	public boolean lookupAddress = false;
	public boolean sortAndSave = false;
	public boolean saveAsync = false;

	public AddFavoriteOptions enableAll() {
		return setLookupAddress(true).setSortAndSave(true).setSaveAsync(true);
	}

	public AddFavoriteOptions setLookupAddress(boolean lookupAddress) {
		this.lookupAddress = lookupAddress;
		return this;
	}

	public AddFavoriteOptions setSortAndSave(boolean sortAndSave) {
		this.sortAndSave = sortAndSave;
		return this;
	}

	public AddFavoriteOptions setSaveAsync(boolean saveAsync) {
		this.saveAsync = saveAsync;
		return this;
	}
}
