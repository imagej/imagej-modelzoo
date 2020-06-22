package net.imagej.modelzoo;

import io.scif.MissingLibraryException;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.Location;

import java.io.FileNotFoundException;

public interface ModelZooArchive<TI extends RealType<TI>, TO extends RealType<TO>> {
	/**
	 * @return the source of the archive
	 */
	Location getSource();

	/**
	 * @return the specification of the model
	 */
	ModelSpecification getSpecification();

	/**
	 * @return an exemplary input image
	 */
	RandomAccessibleInterval<TI> getTestInput();

	/**
	 * @return an image matching the prediction of {@link #getTestInput()}
	 */
	RandomAccessibleInterval<TO> getTestOutput();

	/**
	 * @return an instance of the model specified by {@link #getSpecification()} and loaded from {@link #getSource()}
	 * @throws FileNotFoundException in case the model source is not found
	 */
	ModelZooModel createModelInstance() throws FileNotFoundException, MissingLibraryException;

	void setTestInput(RandomAccessibleInterval<TI> testInput);

	void setTestOutput(RandomAccessibleInterval<TO> testOutput);
}
