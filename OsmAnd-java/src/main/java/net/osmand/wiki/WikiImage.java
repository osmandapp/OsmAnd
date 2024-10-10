package net.osmand.wiki;

public class WikiImage {

	private static final String WIKIMEDIA_COMMONS_URL = "https://commons.wikimedia.org/wiki/";
	private static final String WIKIMEDIA_FILE = "File:";

	private final String wikiMediaTag;
	private final String imageName;
	private final String imageStubUrl;
	private final String imageHiResUrl;

	private final Metadata metadata = new Metadata();

	public WikiImage(String wikiMediaTag, String imageName, String imageStubUrl, String imageHiResUrl) {
		this.wikiMediaTag = wikiMediaTag;
		this.imageName = imageName;
		this.imageStubUrl = imageStubUrl;
		this.imageHiResUrl = imageHiResUrl;
	}

	public String getUrlWithCommonAttributions() {
		return WIKIMEDIA_COMMONS_URL + WIKIMEDIA_FILE + wikiMediaTag;
	}

	public String getImageName() {
		return imageName;
	}

	public String getImageStubUrl() {
		return imageStubUrl;
	}

	public String getImageHiResUrl() {
		return imageHiResUrl;
	}

	public String getWikiMediaTag() {
		return wikiMediaTag;
	}

	public Metadata getMetadata() {
		return metadata;
	}
}