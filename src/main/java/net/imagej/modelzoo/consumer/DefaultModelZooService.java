package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.Model;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

@Plugin(type = Service.class)
public class DefaultModelZooService extends AbstractService implements ModelZooService {

	@Parameter
	private Context context;

	@Override
	public RandomAccessibleInterval predict(String modelLocation, RandomAccessibleInterval input, String mapping) {
		SingleOutputPrediction prediction = new SingleOutputPrediction(context);
		prediction.setModelFile(modelLocation);
		Model model = prediction.loadModel();
		InputImageNode inputNode = model.getInputNodes().get(0);
		prediction.setInput(inputNode.getName(), input, mapping);
		prediction.setNumberOfTiles(8);
		prediction.run();
		return prediction.getOutput();
	}
}
