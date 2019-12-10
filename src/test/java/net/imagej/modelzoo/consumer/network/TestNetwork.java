
package net.imagej.modelzoo.consumer.network;

import net.imagej.modelzoo.consumer.network.model.tensorflow.TensorFlowModel;
import net.imagej.Dataset;
import net.imglib2.type.numeric.RealType;
import org.tensorflow.framework.TensorInfo;

public abstract class TestNetwork<T extends RealType<T>> extends
		TensorFlowModel<T>
{

	long[] inputShape;
	long[] outputShape;
	private final int inputCount = 1;
	private final int outputCount = 1;

	@Override
	public void loadInputNode(final Dataset dataset) {
		super.loadInputNode( dataset);
		if (inputCount > 0) {
			inputNode.setName("input");
			inputNode.setNodeShape(inputShape);
			inputNode.initializeNodeMapping();
		}
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		super.loadOutputNode(dataset);
		if (outputCount > 0) {
			outputNode.setName("output");
			outputNode.setNodeShape(outputShape);
			outputNode.initializeNodeMapping();
		}
	}

	@Override
	protected void logTensorShape(String title, final TensorInfo tensorInfo) {
		log.info("cannot log tensorinfo shape of test networks");
	}

}
