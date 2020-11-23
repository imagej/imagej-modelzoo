package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imglib2.img.Img;
import org.scijava.Context;

public class DefaultModelZooPrediction extends AbstractModelZooPrediction<ImageInput<?>, DefaultPredictionOutput> {

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

	public void setInput(Img image, String axes) {
		addImageInput(new ImageInput<>("input", image, axes));
	}

	public void setInput(String name, Img image, String axes) {
		addImageInput(new ImageInput<>(name, image, axes));
	}
}
