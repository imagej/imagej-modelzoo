package net.imagej.modelzoo.consumer.model.node.processor;

import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import io.bioimage.specification.TransformationSpecification;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class DefaultImageNodePreprocessor<T extends TransformationSpecification> implements ImageNodeProcessor<T>, NodePreprocessor<T> {

	private ImageNode imageNode;

	@Override
	public void setup(ImageNode imageNode, ModelZooModel model)  {
		this.imageNode = imageNode;
	}

	@Override
	public ImageNode getImageNode() {
		return imageNode;
	}

	@Override
	public void run(ModelZooPredictionOptions.Values options) {
		ImageDataReference<?> outReference = getImageNode().getData();
		ImageDataReference<?> result = process(getImageNode().getData(), outReference);
		getImageNode().setData(result);
	}

	protected abstract <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<?> process(ImageDataReference<I> in, ImageDataReference<O> outType);

}
