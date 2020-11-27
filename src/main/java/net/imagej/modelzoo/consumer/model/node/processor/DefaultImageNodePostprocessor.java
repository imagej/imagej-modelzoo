package net.imagej.modelzoo.consumer.model.node.processor;

import io.bioimage.specification.TransformationSpecification;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class DefaultImageNodePostprocessor<T extends TransformationSpecification> implements ImageNodeProcessor<T>, NodePostprocessor<T> {

	private ImageNode imageNode;
	private ModelZooPredictionOptions.Values options;

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
		this.options = options;
		ImageDataReference<?> outReference = getImageNode().getData();
		ImageDataReference<?> result = process(getImageNode().getData(), outReference);
		getImageNode().setData(result);
	}

	protected abstract <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<?> process(ImageDataReference<I> in, ImageDataReference<O> outType);

	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> boolean sameType(ImageDataReference<I> in, ImageDataReference<O> out) {
		return sameType(in.getDataType(), out.getDataType());
	}

	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> boolean sameType(I inType, O outType) {
		return inType.getClass().equals(outType);
	}

	protected ModelZooPredictionOptions.Values getOptions() {
		return options;
	}
}
