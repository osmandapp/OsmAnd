package net.osmand.plus.wikimedia;

public class WikiImage {

	private String imageName;
	private String imageStubUrl;
	private String imageHiResUrl;

	public WikiImage(String imageName, String imageStubUrl,
	                 String imageHiResUrl) {
		this.imageName = imageName;
		this.imageStubUrl = imageStubUrl;
		this.imageHiResUrl = imageHiResUrl;
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

}
