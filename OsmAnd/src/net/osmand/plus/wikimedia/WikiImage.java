package net.osmand.plus.wikimedia;

public class WikiImage {

	private String imageName;
	private String imageStubUrl;
	private String imageHiResUrl;
	private String datatype;

	public WikiImage(String imageName, String imageStubUrl,
	                 String imageHiResUrl, String datatype) {
		this.imageName = imageName;
		this.imageStubUrl = imageStubUrl;
		this.imageHiResUrl = imageHiResUrl;
		this.datatype = datatype;
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

	public String getDatatype() {
		return datatype;
	}
}
