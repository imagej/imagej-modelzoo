package net.imagej.modelzoo.specification;

public class DefaultWeightsSpecification implements WeightsSpecification {

	private String source;
	private String sha256;
	private String timestamp;

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public String getSha256() {
		return sha256;
	}

	@Override
	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	@Override
	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}
