/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imagej.modelzoo.consumer.model.tensorflow.train;

import io.bioimage.specification.io.SpecificationWriter;
import io.scif.img.ImgSaver;
import net.imagej.modelzoo.ImageTensorSample;
import io.bioimage.specification.ModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.scijava.Context;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipOutputStream;

import static net.imagej.modelzoo.consumer.model.tensorflow.train.IOUtils.unZipAll;
import static net.imagej.modelzoo.consumer.model.tensorflow.train.IOUtils.zipFile;

public abstract class AbstractOutputHandler {
	private File mostRecentModelDir;
	private File bestModelDir;
	private boolean noCheckpointSaved = true;
	private Tensor< String > checkpointPrefix;
	private boolean checkpointExists;
	private ImgSaver imgSaver;

	private final String savedModelBundleName = "tf_saved_model_bundle";
	private String variablesLocation = savedModelBundleName + File.separator + "variables";

	protected AbstractOutputHandler(Context context) {
		imgSaver = new ImgSaver(context);
	}

	protected abstract String getShortName();

	protected abstract ModelSpecification createSpecification(String name);

	public synchronized File exportLatestTrainedModel() throws IOException {
		if(noCheckpointSaved()) return null;
		ModelSpecification last_checkpoint = createSpecification("last checkpoint");
		SpecificationWriter.write(last_checkpoint, getMostRecentModelDir());
		return saveTrainedModel(getMostRecentModelDir(), getSavedModelBundleName());
	}

	protected String getSavedModelBundleName() {
		return savedModelBundleName;
	}

	protected synchronized File saveTrainedModel(File checkpointDir, String weightsLocation) throws IOException {
		Path tmp = Files.createTempDirectory("export");
		FileUtils.copyDirectory(checkpointDir, tmp.toFile());
		File weights = new File(tmp.toFile(), "tf_saved_model_bundle.zip");
		FileOutputStream fosWeights = new FileOutputStream(weights);
		ZipOutputStream zipOutWeights = new ZipOutputStream(fosWeights);
		File weightsUnpacked = new File(tmp.toFile(), weightsLocation);
		zipFile(weightsUnpacked, null, zipOutWeights);
		zipOutWeights.close();
		fosWeights.close();
		FileUtils.deleteDirectory(weightsUnpacked);
		Path out = Files.createTempFile(getShortName() + "-", ".bioimage.io.zip");
		FileOutputStream fos = new FileOutputStream(out.toFile());
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		zipFile(tmp.toFile(), null, zipOut);
		zipOut.close();
		fos.close();
		FileUtils.deleteDirectory(tmp.toFile());
		return out.toFile();
	}

	protected String getTimestamp() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		return df.format(new Date());
	}

	public File exportBestTrainedModel() throws IOException {
		if(noCheckpointSaved()) return null;
		return saveTrainedModel(getBestModelDir(), getSavedModelBundleName());
	}

	protected File getBestModelDir() {
		return bestModelDir;
	}

	protected void copyBestModel() {
		try {
			FileUtils.copyDirectory(mostRecentModelDir, bestModelDir);
			ModelSpecification lowest_loss = createSpecification("lowest loss");
			SpecificationWriter.write(lowest_loss, bestModelDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void createSavedModelDirs(InputStream predictionGraphFileStream) throws IOException {
		bestModelDir = Files.createTempDirectory(getShortName() + "-best-").toFile();
		String checkpointDir = Files.createTempDirectory(getShortName() + "-latest-").toAbsolutePath().toString() + File.separator + variablesLocation;
		checkpointPrefix = Tensors.create(Paths.get(checkpointDir, "variables").toString());
		new File(checkpointDir).mkdirs();
		mostRecentModelDir = new File(checkpointDir).getParentFile().getParentFile();
		checkpointExists = false;
		byte[] predictionGraphDef = IOUtils.toByteArray( predictionGraphFileStream );
		FileUtils.writeByteArrayToFile(new File(new File(checkpointDir).getParentFile(), "saved_model.pb"), predictionGraphDef);
		FileUtils.writeByteArrayToFile(new File(mostRecentModelDir, "training_model.pb"), predictionGraphDef);
	}

	public void createSavedModelDirsFromExisting(File trainedModel) throws IOException {
		mostRecentModelDir = trainedModel;
		bestModelDir = Files.createTempDirectory(getShortName() + "-best-").toFile();
		String checkpointDir = mostRecentModelDir.getAbsolutePath() + File.separator + variablesLocation;
		checkpointPrefix = Tensors.create(Paths.get(checkpointDir, "variables").toString());
		checkpointExists = true;
		byte[] predictionGraphDef = IOUtils.toByteArray( new FileInputStream(new File(new File(checkpointDir), "saved_model.pb")));
		FileUtils.writeByteArrayToFile(new File(mostRecentModelDir, "training_graph.pb"), predictionGraphDef);
	}

	protected void loadUntrainedGraph(Graph graph, InputStream graphFileStream) throws IOException {
		byte[] graphDef = IOUtils.toByteArray(graphFileStream);
		graph.importGraphDef( graphDef );
	}

	public File loadTrainedGraph(Graph graph, File zipFile) throws IOException {

		File trainedModel = Files.createTempDirectory(getShortName() + "-imported-model").toFile();
		unZipAll(zipFile, trainedModel);

		byte[] graphDef = new byte[ 0 ];
		try {
			graphDef = IOUtils.toByteArray( new FileInputStream(new File(trainedModel, "training_graph.pb")));
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		graph.importGraphDef( graphDef );
		return trainedModel;
	}


	public void initTensors(Session sess) {
		if (checkpointExists) {
			sess.runner()
					.feed("save/Const", checkpointPrefix)
					.addTarget("save/restore_all").run();
		} else {
			sess.runner().addTarget("init").run();
		}
	}

	protected synchronized void saveCheckpoint(Session sess,
	                              List<ImageTensorSample<?>> inputs,
	                              List<ImageTensorSample<?>> outputs) {
		sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/control_dependency").run();
		noCheckpointSaved = false;
		inputs.forEach(this::saveToRecentModelDir);
		outputs.forEach(this::saveToRecentModelDir);
	}

	private void saveToRecentModelDir(ImageTensorSample<?> input) {
		File imgIn = new File(mostRecentModelDir, input.getFileName());
		if(imgIn.exists()) imgIn.delete();
		imgSaver.saveImg(imgIn.getAbsolutePath(),
				toImg(input.getData()));
	}

	private Img<?> toImg(RandomAccessibleInterval<? extends RealType<?>> input) {
		ArrayImg<FloatType, ?> res = new ArrayImgFactory<>(new FloatType()).create(input);
		LoopBuilder.setImages(input, res).multiThreaded().forEachPixel((in, out) -> {
			out.setReal(in.getRealDouble());
		});
		return res;
	}

	public String getSavedModelBundlePackage() {
		return savedModelBundleName + ".zip";
	}

	public File getMostRecentModelDir() {
		return mostRecentModelDir;
	}

	protected boolean noCheckpointSaved() {
		return noCheckpointSaved;
	}

	public void dispose() {
		if(checkpointPrefix != null) checkpointPrefix.close();
		try {
			if(bestModelDir != null && bestModelDir.exists()) FileUtils.deleteDirectory(bestModelDir);
			if(mostRecentModelDir != null && mostRecentModelDir.exists()) FileUtils.deleteDirectory(mostRecentModelDir);
		} catch (IOException ignored) {
		}
	}
}
