package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

@Plugin(type = Service.class)
public class DefaultModelZooService extends AbstractService implements ModelZooService {

	@Parameter
	Context context;

	@Override
	public RandomAccessibleInterval predict(RandomAccessibleInterval input, String modelLocation) {
		SingleOutputPrediction prediction = new SingleOutputPrediction(context);
		prediction.setModelFile(modelLocation);
		Model model = prediction.loadModel();
		InputImageNode inputNode = model.getInputNodes().get(0);
		prediction.setInput(inputNode.getName(), input);
		prediction.setNumberOfTiles(8);
		prediction.run();
		return prediction.getOutput();
	}
}
