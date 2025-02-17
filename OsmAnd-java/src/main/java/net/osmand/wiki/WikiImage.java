package net.osmand.wiki;

public class WikiImage {

	private static final String WIKIMEDIA_COMMONS_URL = "https://commons.wikimedia.org/wiki/";
	private static final String WIKIMEDIA_FILE = "File:";

	private final String wikiMediaTag;
	private final String imageName;
	private final String imageStubUrl;
	private final String imageIconUrl;
	private final String imageHiResUrl;
	private long mediaId = -1;

	private final Metadata metadata = new Metadata();

	public WikiImage(String wikiMediaTag, String imageName, String imageStubUrl, String imageHiResUrl, String imageIconUrl) {
		this.wikiMediaTag = wikiMediaTag;
		this.imageName = imageName;
		this.imageStubUrl = imageStubUrl;
		this.imageHiResUrl = imageHiResUrl;
		this.imageIconUrl = imageIconUrl;
	}

	public void setMediaId(long mediaId) {
		this.mediaId = mediaId;
	}

	public long getMediaId() {
		return mediaId;
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

	public String getImageIconUrl() {
		return imageIconUrl;
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