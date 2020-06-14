package net.imagej.modelzoo.consumer.network.model;

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.task.Task;
import net.imglib2.EuclideanSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImageNode<T extends RealType<T>> extends DefaultModelZooNode<RandomAccessibleInterval<T>> implements EuclideanSpace {

	private RandomAccessibleInterval<T> data;

	protected final List<ModelZooAxis> axes = new ArrayList<>();
	private List<AxisType> mapping;

	public void clearAxes() {
		axes.clear();
	}

	public void addAxis(ModelZooAxis axis) {
		axes.add(axis);
	}

	public AxisType[] getMapping() {
		return axes.stream().map(ModelZooAxis::getType).toArray(AxisType[]::new);
	}

	public void printMapping() {
		printMapping(null);
	}

	public void printMapping(Task task) {
		Consumer<String> logFunction = System.out::println;
		if(task != null) logFunction = task::log;
		logFunction.accept("Mapping of tensor " + getName() + ": ");
//		if (image != null) {
//			final AxisType[] axes = new AxisType[this.image.size()];
//			for (int i = 0; i < this.image.size(); i++) {
//				axes[i] = this.image.get(i).getType();
//			}
//			logFunction.accept("   datasetAxes:" + Arrays.toString(axes));
//		}
		logFunction.accept("   nodeAxes:" + axes.toString());
//		logFunction.accept("   mapping:" + finalMapping.toString());
	}

	public void setData(RandomAccessibleInterval<T> input) {
		this.data = input;
	}

	public RandomAccessibleInterval<T> getData() {
		return data;
	}

	public AxisType[] getAxesArray() {
		AxisType[] res = new AxisType[axes.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = axes.get(i).getType();
		}
		return res;
	}

	public AxisType[] getDataAxesArray() {
		AxisType[] res = new AxisType[data.numDimensions()];
		for (int i = 0; i < res.length; i++) {
			res[i] = getDataAxis(i).getType();
		}
		return res;
	}

	public void setDataMapping(List<AxisType> axisTypes) {
		this.mapping = axisTypes;
	}

	public List<AxisType> getDataMapping() {
		return mapping;
	}

	public int[] getMappingIndices() {
		int[] res = new int[axes.size()];

		int i = 0;
		// set mapping for axes which exist in the input image
		for (; mapping != null && i < mapping.size(); i++) {
			AxisType axis = mapping.get(i);
			for (int j = 0; j < axes.size(); j++) {
				if(axes.get(j).getType().equals(axis)) {
					res[i] = j;
				}
			}
		}
		// set mapping for axes which will be added to the image to fit the tensor
		for (int j = i; j < res.length; j++) {
			for (int k = 0; k < res.length; k++) {
				if(!ArrayUtils.contains(res, k)) res[j] = k;
			}
		}
		return res;
	}

	public ModelZooAxis getDataAxis(int index) {
		return getAxes().get(getMappingIndices()[index]);
	}

	@Override
	public int numDimensions() {
		return axes.size();
	}

	public List<ModelZooAxis> getAxes() {
		return axes;
	}
}

