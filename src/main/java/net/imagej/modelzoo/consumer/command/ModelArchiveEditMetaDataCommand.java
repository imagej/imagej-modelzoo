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
package net.imagej.modelzoo.consumer.command;

import io.bioimage.specification.AuthorSpecification;
import io.bioimage.specification.DefaultAuthorSpecification;
import io.bioimage.specification.ModelSpecification;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, name = "Edit bioimage.io model metadata", initializer = "initParameters")
public class ModelArchiveEditMetaDataCommand implements Command {

	@Parameter(label = "Trained model name", persist = false, required = false, description = "Please only use letters, numbers, _, - or spaces, maximal 36 characters")
	String name;

	@Parameter(label = "Description", persist = false, required = false, style = TextWidget.AREA_STYLE)
	String description;

	@Parameter(label = "Authors", persist = false, required = false, description = "Please list the creators of the model archive (comma separated)", style = TextWidget.AREA_STYLE)
	String authors;

	@Parameter(label = "Tags (comma separated)", persist = false, required = false, style = TextWidget.AREA_STYLE)
	String tags;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private ModelZooService modelZooService;

	private static final int MAX_CHAR = 36;

	@Override
	public void run() {
		ModelSpecification specification = archive.getSpecification();
		specification.setName(conform(name));
		specification.setAuthors(Arrays.stream(authors.split("\n")).map(author -> {
			AuthorSpecification authorSpec = new DefaultAuthorSpecification();
			authorSpec.setName(author);
			return authorSpec;
		}).collect(Collectors.toList()));
		specification.setTags(stringToList(tags, true));
		specification.setDescription(description);
	}

	private String conform(String name) {
		int maxLength = Math.min(name.length(), MAX_CHAR);
		return name.substring(0, maxLength);
	}

	private void initParameters() {
		if(archive != null) {
			ModelSpecification specification = archive.getSpecification();
			List<String> tags = specification.getTags();
			this.tags = listToString(tags);
			this.authors = specification.getAuthors().stream()
					.map(author -> author.getName()).reduce("", (a,b)->{
						return a+"\n"+b;
					});
			this.description = specification.getDescription();
			this.name = specification.getName();
		}
	}

	private String listToString(List<String> list) {
		if(list == null) return null;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if(i != 0) str.append(", ");
			str.append(list.get(i));
		}
		return str.toString();
	}

	private List<String> stringToList(String input, boolean forceLowerCase) {
		String[] res = input.split(",");
		for (int i = 0; i < res.length; i++) {
			res[i] = res[i].trim();
			if(forceLowerCase) res[i] = res[i].toLowerCase();
		}
		return Arrays.asList(res);
	}

}
