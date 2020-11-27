package net.imagej.modelzoo.plugin.transformation.postprocessing;

import io.bioimage.specification.transformation.ScaleMinMaxTransformation;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.plugin.transformation.preprocessing.NormalizePercentilePreprocessing;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Plugin(type = NodePostprocessor.class, name = ScaleMinMaxTransformation.name)
public class ScaleMinMaxPostprocessing
		extends DefaultImageNodePostprocessor<ScaleMinMaxTransformation> {

	@Parameter
	private OpService opService;

	private Number minPercentile;
	private Number maxPercentile;
	private Number minVal;
	private Number factor;
	private ModelZooModel model;
	private String inputReferenceName;
	private InputImageNode inputReference;

	@Override
	protected <I extends RealType<I> & NativeType<I>,
			O extends RealType<O> & NativeType<O>> ImageDataReference<?> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		calculateGainOffset(in.getData());
		RandomAccessibleInterval<O> out;
		O resOutType = outType.getDataType();
		if(inputReference != null && getOptions().convertIntoInputFormat()) {
			resOutType = inputReference.getOriginalDataType();
		}
		if(sameType(in.getDataType(), resOutType)) {
			out = (RandomAccessibleInterval<O>) in.getData();
		} else {
			out = opService.create().img(in.getData(), resOutType);
		}
		LoopBuilder.setImages(in.getData(), out).multiThreaded().forEachPixel((i, o) -> {
			o.setReal(i.getRealFloat()*factor.floatValue() + minVal.floatValue());
		});
		return new DefaultImageDataReference<>(out, resOutType);
	}

	private <I extends RealType<I> & NativeType<I>> void calculateGainOffset(RandomAccessibleInterval<I> in) {
		NormalizePercentilePreprocessing preprocessor = null;
		inputReference = null;
		for (ModelZooNode<?> inputNode : model.getInputNodes()) {
			if(inputNode.getName().equals(inputReferenceName)) {
				inputReference = (InputImageNode) inputNode;
			}
		}
		if(inputReference != null) {
			for (NodeProcessor processor : inputReference.getProcessors()) {
				if(processor instanceof NormalizePercentilePreprocessing) {
					preprocessor = (NormalizePercentilePreprocessing) processor;
					break;
				}
			}
		}
		if(preprocessor != null
				&& isEquals(preprocessor.getMinPercentile(), minPercentile)
				&& isEquals(preprocessor.getMaxPercentile(), maxPercentile)) {
			minVal = preprocessor.getMinVal();
			factor = 1./preprocessor.getFactor().floatValue();
		} else {
			throw new NotImplementedException();
		}
	}

	private boolean isEquals(Number lowerPercentile, Number minPercentile) {
		return Float.compare(lowerPercentile.floatValue(), minPercentile.floatValue()) == 0;
	}

	@Override
	public void setup(ImageNode imageNode, ModelZooModel model)  {
		super.setup(imageNode, model);
		this.model = model;
	}

	@Override
	public void readSpecification(ScaleMinMaxTransformation specification) {
		minPercentile = specification.getMinPercentile();
		maxPercentile = specification.getMaxPercentile();
		inputReferenceName = specification.getReferenceInput();
	}
}
