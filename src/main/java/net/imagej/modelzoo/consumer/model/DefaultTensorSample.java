package net.imagej.modelzoo.consumer.model;

public class DefaultTensorSample implements TensorSample {
	private final Object data;
	private final String fileName;

	public DefaultTensorSample(Object data, String fileName) {
		this.data = data;
		this.fileName = fileName;
	}

	@Override
	public Object getData() {
		return data;
	}

	@Override
	public String getFileName() {
		return fileName;
	}
}
