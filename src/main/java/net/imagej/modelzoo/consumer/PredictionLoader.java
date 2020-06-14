/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
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

package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.Model;
import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModel;
import net.imagej.modelzoo.consumer.util.IOHelper;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PredictionLoader implements Runnable {

	private File modelFile;

	private String modelUrl;

	private Model model;

	@Parameter
	private Context context;

	@Parameter
	private PrefService prefService;

	@Parameter
	private LogService log;

	public PredictionLoader(Context context) {
		context.inject(this);
	}

	public enum NetworkInputSourceType {UNSET, FILE, URL}

	private NetworkInputSourceType networkInputSourceType = NetworkInputSourceType.UNSET;
	private String cacheName;
	private String modelFileKey;

	private String modelFileUrl = "";

	@Override
	public void run() {
		cacheName = this.getClass().getSimpleName();
		modelFileKey = getModelFileKey();
		model = new TensorFlowModel();
		context.inject(model);
		model.loadLibrary();
		if (!model.libraryLoaded()) {
			log.error("TensorFlow library could not be loaded");
			return;
		}

		solveModelSource();

		if (modelFileUrl.isEmpty()) return;
		try {
			model.loadModel(modelFileUrl, cacheName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void setModelFromFile(File modelFile) {
		this.modelFile = modelFile;
	}

	public void setModelFromURL(String url) {
		this.modelUrl = url;
	}

	private void solveModelSource() {
		if (modelFileUrl.isEmpty()) modelFileChanged();
		if (modelFileUrl.isEmpty()) modelUrlChanged();
	}

	private void updateCacheName() {
		switch (networkInputSourceType) {
			case UNSET:
			default:
				break;
			case FILE:
				cacheName = getFileCacheName(this.getClass(), modelFile);
				if (cacheName != null) savePreferences();
				break;
			case URL:
				cacheName = getUrlCacheName(this.getClass(), modelUrl);
				if (cacheName != null) savePreferences();
				break;
		}
	}

	private static String getUrlCacheName(Class commandClass, String modelUrl) {
		try {
			return IOHelper.getUrlCacheName(commandClass, modelUrl);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getFileCacheName(Class commandClass, File modelFile) {
		try {
			return IOHelper.getFileCacheName(commandClass, modelFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void modelFileChanged() {
		if (modelFile != null) {
			if (modelFile.exists()) {
				modelUrl = null;
				networkInputSourceType = NetworkInputSourceType.FILE;
				modelFileUrl = modelFile.getAbsolutePath();
				modelChanged();
			} else {
				log.error("Model file " + modelFile.getAbsolutePath() + " does not exist.");
			}
		}
	}

	private void modelUrlChanged() {
		if (modelUrl != null && modelUrl.length() > "https://".length()) {
			if (IOHelper.urlExists(modelUrl)) {
				modelFile = null;
				networkInputSourceType = NetworkInputSourceType.URL;
				modelFileUrl = modelUrl;
				modelChanged();
			}
		}
	}

	private void modelChanged() {
		updateCacheName();
		savePreferences();
	}

	private void savePreferences() {
		if (modelFile != null) {
			prefService.put(this.getClass(), modelFileKey, modelFile.getAbsolutePath());
		}
	}

	String getModelFileKey() {
		return this.getClass().getSimpleName() + "_modelfile";
	}

	public Model getModel() {
		return model;
	}

}
