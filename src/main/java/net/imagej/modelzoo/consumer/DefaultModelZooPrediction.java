package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.prediction.DefaultPredictionOutput;
import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imagej.modelzoo.consumer.sanitycheck.ImageToImageSanityCheck;
import net.imagej.modelzoo.consumer.sanitycheck.SanityCheck;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;

public class DefaultModelZooPrediction extends AbstractModelZooPrediction<ImageInput<?>, DefaultPredictionOutput> implements SingleImagePrediction<DefaultPredictionOutput> {

	public DefaultModelZooPrediction() {
	}

	public DefaultModelZooPrediction(Context context) {
		super(context);
	}

	@Override
	protected DefaultPredictionOutput createOutput(ModelZooModel model) {
		DefaultPredictionOutput outputs = new DefaultPredictionOutput();
		model.getOutputNodes().forEach(node -> {
			if(ImageNode.class.isAssignableFrom(node.getClass())) {
				outputs.put(node.getName(), ((ImageNode)node).getData().getData());
			} else {
				outputs.put(node.getName(), node.getData());
			}
		});
		return outputs;
	}

	public <T extends RealType<T> & NativeType<T>> void setInput(RandomAccessibleInterval<T> image, String axes) {
		setInput(new ImageInput<>("input", image, axes));
	}

	public <T extends RealType<T> & NativeType<T>> void setInput(String name, RandomAccessibleInterval<T> image, String axes) {
		setInput(new ImageInput<>(name, image, axes));
	}

	@Override
	public SanityCheck getSanityCheck() {
		return new ImageToImageSanityCheck(context());
	}
}
