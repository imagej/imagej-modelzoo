package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
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
			outputs.put(legacyRenaming(node.getName()), asOutput(node));
		});
		return outputs;
	}

	public static String legacyRenaming(String name) {
		if(name.equals("activation_11/Identity")) return "output";
		if(name.equals("out_segment/strided_slice")) return "output_segment";
		if(name.equals("out_denoise/strided_slice")) return "output_denoise";
		return name;
	}

	private Object asOutput(ModelZooNode<?> node) {
		Object data;
		if(ImageNode.class.isAssignableFrom(node.getClass())) {
			data = datasetService().create(((ImageNode) node).getData().getData());
		} else {
			data = node.getData();
		}
		return data;
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

	@Override
	public boolean canRunSanityCheck(ModelZooArchive trainedModel) {
		return trainedModel.getSpecification().getOutputs().size() == 1
				&& trainedModel.getSpecification().getInputs().size() == 1
				&& trainedModel.getSpecification().getInputs().get(0).getDataType().equals(
						trainedModel.getSpecification().getOutputs().get(0).getDataType());
	}

}
