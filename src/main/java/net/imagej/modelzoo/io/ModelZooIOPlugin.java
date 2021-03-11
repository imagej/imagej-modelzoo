/*
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
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

package net.imagej.modelzoo.io;

import io.bioimage.specification.DefaultModelSpecification;
import io.bioimage.specification.ModelSpecification;
import io.bioimage.specification.io.SpecificationReader;
import io.bioimage.specification.io.SpecificationWriter;
import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.modelzoo.DefaultModelZooArchive;
import net.imagej.modelzoo.ImageTensorSample;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.consumer.model.TensorSample;
import net.imagej.modelzoo.specification.ImageJModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import org.jetbrains.annotations.NotNull;
import org.scijava.app.StatusService;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.io.IOService;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Abstract plugin class for saving and loading bioimage model zoo archives
 *
 * @author Deborah Schmidt
 */
@Plugin(type = IOPlugin.class, priority = 100.0)
public class ModelZooIOPlugin extends AbstractIOPlugin<ModelZooArchive> {

	@Parameter
	private StatusService statusService;

	@Parameter
	private IOService ioService;

	@Parameter
	private DatasetService datasetService;

	// TODO remove when compatible with scifio 0.41.2
	@Parameter
	private DatasetIOService datasetIOService;

	@Override
	public ModelZooArchive open(Location source) throws IOException {
		statusService.showStatus("Opening " + source + "..");

		DefaultModelZooArchive archive = new DefaultModelZooArchive();
		getContext().inject(archive);
		archive.setLocation(source);
		try (ZipFile zf = new ZipFile(source.getURI().getPath())) {
			ZipEntry modelFile = zf.getEntry(SpecificationWriter.getModelFileName());
			if(modelFile == null) {
				// older models had a name.model.yaml file, look for that
				final Enumeration<? extends ZipEntry> entries = zf.entries();
				modelFile = null;
				while ( entries.hasMoreElements() ) {
					final ZipEntry entry = entries.nextElement();
					if(entry.getName().endsWith(".model.yaml")) {
						modelFile = entry;
						break;
					}
				}
				if(modelFile == null) throw new IOException("Can't open " + source + " as bioimage.io model archive: No model.yaml or *.model.yaml file found.");
			}
			InputStream inSpec = zf.getInputStream(modelFile);
			ImageJModelSpecification specification = new ImageJModelSpecification();
			boolean success = SpecificationReader.read(inSpec, specification);
			if(!success) {
				throw new IOException("Could not read model.yaml specification from source " + source);
			}
			inSpec.close();
			archive.setSpecification(specification);
			setSampleImages(archive, specification, zf);
		}
		statusService.showStatus("Done opening " + source + ".");
		return archive;
	}

	@Override
	public ModelZooArchive open(String source) throws IOException {
		statusService.showStatus("Opening " + source + "..");
		Location location = null;
		try {
			LocationService locationService = getContext().service(LocationService.class);
			location = locationService.resolve(source);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return open(location);
	}

	private void setSampleImages(DefaultModelZooArchive archive, DefaultModelSpecification specification, ZipFile zf) throws IOException {
		if(specification.getSampleInputs() != null) {
			List<TensorSample> sampleInputs = new ArrayList<>();
			for (String sampleInput : specification.getSampleInputs()) {
				sampleInputs.add(extractImageSample(zf, sampleInput));
			}
			archive.setSampleInputs(sampleInputs);
		}
		if(specification.getSampleOutputs() != null) {
			List<TensorSample> sampleOutputs = new ArrayList<>();
			for (String sampleOutput : specification.getSampleOutputs()) {
				sampleOutputs.add(extractImageSample(zf,sampleOutput));
			}
			archive.setSampleOutputs(sampleOutputs);
		}
	}

	@NotNull
	private ImageTensorSample extractImageSample(ZipFile zf, String sampleInput) throws IOException {
		return new ImageTensorSample<>((RandomAccessibleInterval)extract(zf,sampleInput), sampleInput);
	}


	@Override
	public void save(ModelZooArchive archive, Location destination) throws IOException {
		save(archive, destination.getURI().getPath());
	}

	@Override
	public void save(ModelZooArchive archive, String destination) throws IOException {
		//FIXME
		statusService.showStatus("Saving " + destination + "..");
		Path destinationPath = Paths.get(destination);
		if(!destinationPath.toFile().exists()) {
			Files.createFile(destinationPath);
		}
		FileLocation sourceFile = (FileLocation) archive.getLocation();
		if(sourceFile != null && sourceFile.getFile().exists() && !sourceFile.getFile().toPath().equals(destinationPath)) {
			Files.copy(sourceFile.getFile().toPath(), destinationPath, REPLACE_EXISTING);
		}
		Path tmpSpec = writeSpecification(archive);
		List<Path> tmpTestInputs = saveSampleTensors(archive.getSampleInputs());
		List<Path> tmpTestOutputs = saveSampleTensors(archive.getSampleOutputs());
		try (FileSystem fileSystem = FileSystems.newFileSystem(destinationPath, null)) {
			Files.copy(tmpSpec, fileSystem.getPath(SpecificationWriter.getModelFileName()), REPLACE_EXISTING);
			copySampleTensors(tmpTestInputs, archive.getSampleInputs(), fileSystem, archive.getSpecification().getSampleInputs());
			copySampleTensors(tmpTestOutputs, archive.getSampleOutputs(), fileSystem, archive.getSpecification().getSampleOutputs());
		}
		Files.delete(tmpSpec);
		cleanup(tmpTestInputs);
		cleanup(tmpTestOutputs);
		archive.setLocation(new FileLocation(destination));
		statusService.showStatus("Done saving " + destination + ".");
	}

	private Path writeSpecification(ModelZooArchive archive) throws IOException {
		Path tmpSpec = Files.createTempFile("spec", ".yaml");
		SpecificationWriter.write(archive.getSpecification(), tmpSpec);
		return tmpSpec;
	}

	private void cleanup(List<Path> tmpData) throws IOException {
		for (Path tmpTestInput : tmpData) {
			Files.delete(tmpTestInput);
		}
	}

	private void copySampleTensors(List<Path> sampleTensorFiles, List<TensorSample> samples, FileSystem fileSystem, List<String> newFileNames) throws IOException {
		for (int i = 0; i < sampleTensorFiles.size(); i++) {
			Path tmpTestInput = sampleTensorFiles.get(i);
			String newPath = samples.get(i).getFileName();
			if(newFileNames != null && newFileNames.size() > i) {
				newPath = newFileNames.get(i);
			}
			Files.copy(tmpTestInput, fileSystem.getPath(newPath), REPLACE_EXISTING);
		}
	}

	private List<Path> saveSampleTensors(List<TensorSample> tensorSamples) throws IOException {
		List<Path> resultPaths = new ArrayList<>();
		if(tensorSamples != null) {
			for (int i = 0; i < tensorSamples.size(); i++) {
				TensorSample sampleInput = tensorSamples.get(i);
				Path tmpTestInput = Files.createTempFile("file", sampleInput.getFileName());
				ioService.save(sampleInput.getData(), tmpTestInput.toFile().getAbsolutePath());
				resultPaths.add(tmpTestInput);
			}
		}
		return resultPaths;
	}

	@Override
	public Class<ModelZooArchive> getDataType() {
		return ModelZooArchive.class;
	}

	@Override
	public boolean supportsOpen(String source) {
		return source.endsWith("bioimage.io.zip");
	}

	@Override
	public boolean supportsSave(String destination) {
		return destination.endsWith("bioimage.io.zip");
	}

	@Override
	public boolean supportsOpen(Location source) {
		return source.getURI().getPath().endsWith("bioimage.io.zip");
	}

	@Override
	public boolean supportsSave(Location destination) {
		return destination.getURI().getPath().endsWith("bioimage.io.zip");
	}

	private Object extract(ZipFile zf, String testInputLocation) throws IOException {
		ZipEntry entry;
		try {
			entry = zf.getEntry(testInputLocation);
		} catch (NullPointerException e) {
			entry = null;
		}
		if (entry == null) return null;
		InputStream inputStream = zf.getInputStream(entry);
		File tmpTestInput = Files.createTempFile("", entry.getName()).toFile();
		byte[] buffer = new byte[8 * 1024];
		int bytesRead;
		OutputStream outStream = new FileOutputStream(tmpTestInput);
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		outStream.close();
		inputStream.close();

		// TODO scifio DatasetIOPlugin fixed in future versions (fixed in 0.41.2)
		//Object result = ioService.open(tmpTestInput.getAbsolutePath());
		Object result = datasetIOService.open(tmpTestInput.getAbsolutePath());

		tmpTestInput.delete();
		return result;
	}

	public void save(String archivePath, ModelSpecification specification, String location) throws IOException {
		DefaultModelZooArchive archive = new DefaultModelZooArchive();
		archive.setLocation(new FileLocation(archivePath));
		archive.setSpecification(specification);
		save(archive, location);
	}
}
