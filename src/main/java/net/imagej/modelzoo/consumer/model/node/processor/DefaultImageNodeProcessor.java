package net.imagej.modelzoo.consumer.model;

import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class DefaultImageNodeProcessor<T extends TransformationSpecification> extends DefaultNodeProcessor<T> implements ImageNodeProcessor<T> {

	private InputImageNode processorInputReference;
	private ImageNode imageNode;

	@Override
	public void setup(ImageNode imageNode, InputImageNode processorInputReference)  {
		this.imageNode = imageNode;
		this.processorInputReference = processorInputReference;
	}

	@Override
	public InputImageNode getProcessorInputReference() {
		return processorInputReference;
	}

	@Override
	public ImageNode getImageNode() {
		return imageNode;
	}

	@Override
	public void run() {
		ImageDataReference<?> outReference = getImageNode().getData();
		if(getProcessorInputReference() != null && getProcessorInputReference().getData() != null && getProcessorInputReference().getData().getDataType() != null) {
			outReference = getProcessorInputReference().getData();
		}
		ImageDataReference<?> result = process(getImageNode().getData().getData(), outReference);
		getImageNode().setData(result);
	}

	protected abstract <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType);
}
