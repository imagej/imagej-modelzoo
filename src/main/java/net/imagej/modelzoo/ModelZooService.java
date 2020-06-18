package net.imagej.modelzoo;

import io.scif.MissingLibraryException;
import net.imagej.ImageJService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.Location;
import org.scijava.module.ModuleException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ModelZooService extends ImageJService {
	ModelZooArchive open(String location) throws IOException;
	ModelZooArchive open(File location) throws IOException;
	ModelZooArchive open(Location location) throws IOException;
	void save(ModelZooArchive trainedModel, String location) throws IOException;
	void save(ModelZooArchive trainedModel, File location);
	void save(ModelZooArchive trainedModel, Location location);
	<TI extends RealType<TI>, TO extends RealType<TO>> RandomAccessibleInterval<TO> predict(ModelZooArchive<TI, TO> trainedModel, RandomAccessibleInterval<TI> input, String axes) throws FileNotFoundException, MissingLibraryException;
	<TI extends RealType<TI>, TO extends RealType<TO>> void predictInteractive(ModelZooArchive<TI, TO> trainedModel) throws FileNotFoundException, ModuleException;
}
