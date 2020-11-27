/*-
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
package net.imagej.modelzoo.specification;

import io.bioimage.specification.DefaultModelSpecification;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.bioimage.specification.util.SpecificationUtil.asMap;

public class ImageJModelSpecification extends DefaultModelSpecification {

	private final static String idConfigFiji = "fiji";
	private final static String idTraining = "training";
	private final static String idTrainingSource = "source";
	private final static String idTrainingKwargs = "kwargs";

	public ImageJConfigSpecification getImageJConfig() {
		return readConfig(getConfig());
	}

	protected void setTrainingStats(String trainingSource, Map<String, Object> trainingKwargs) {
		ImageJConfigSpecification imageJConfig = getImageJConfig();
		if(imageJConfig == null) imageJConfig = new ImageJConfigSpecification();
		imageJConfig.setTrainingSource(trainingSource);
		imageJConfig.setTrainingKwargs(trainingKwargs);
		setConfig(imageJConfig);
	}

	public void setConfig(ImageJConfigSpecification config) {
		writeConfig(config);
	}

	private ImageJConfigSpecification readConfig(Map<String, Object> config) {
		if(config == null) config = new HashMap<>();
		ImageJConfigSpecification imageJConfig = new ImageJConfigSpecification();
		Map<String, Object> fijiConfig = asMap(config.get(idConfigFiji));
		if(fijiConfig == null) return null;
		Map<String, Object> training = asMap(fijiConfig.get(idTraining));
		if(training == null) return null;
		imageJConfig.setTrainingSource((String) training.get(idTrainingSource));
		imageJConfig.setTrainingKwargs(asMap(training.get(idTrainingKwargs)));
		return imageJConfig;
	}

	private void writeConfig(ImageJConfigSpecification imageJConfig) {
		Map<String, Object> training = new LinkedHashMap<>();
		training.put(idTrainingSource, imageJConfig.getTrainingSource());
		if (imageJConfig.getTrainingKwargs() != null) {
			training.put(idTrainingKwargs, imageJConfig.getTrainingKwargs());
		}
		Map<String, Object> fijiConfig = new LinkedHashMap<>();
		fijiConfig.put(idTraining, training);
		Map<String, Object> config = getConfig();
		if(config == null) config = new HashMap<>();
		config.put(idConfigFiji, fijiConfig);
		setConfig(config);
	}
}
