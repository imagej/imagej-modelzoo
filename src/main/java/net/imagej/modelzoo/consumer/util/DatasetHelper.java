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

package net.imagej.modelzoo.consumer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imagej.modelzoo.consumer.task.Task;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Interval;

public class DatasetHelper {

	private static final AxisType[] axes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME, Axes.CHANNEL };

	public static void assignUnknownDimensions(final Dataset image) {

		final List<AxisType> unusedAxes = new ArrayList<>();
		final List<Integer> unknownIndices = new ArrayList<>();
		for (int j = 0; j < axes.length; j++) {
			boolean knownAxis = false;
			for (int i = 0; i < image.numDimensions(); i++) {
				if (image.axis(i).type() == axes[j]) {
					knownAxis = true;
					break;
				}
			}
			if (!knownAxis) unusedAxes.add(axes[j]);
		}

		for (int i = 0; i < image.numDimensions(); i++) {
			boolean knownAxis = false;
			for (int j = 0; j < axes.length; j++) {
				if (image.axis(i).type() == axes[j]) {
					knownAxis = true;
					break;
				}
			}
			if (!knownAxis) unknownIndices.add(i);
		}

		for (int i = 0; i < unknownIndices.size() && i < unusedAxes.size(); i++) {
			image.axis(unknownIndices.get(i)).setType(unusedAxes.get(i));
		}

	}

	public static AxisType[] getDimensionsAllAssigned(final Dataset image) {

		final List<AxisType> unusedAxes = new ArrayList<>();
		final List<Integer> unknownIndices = new ArrayList<>();
		for (int j = 0; j < axes.length; j++) {
			boolean knownAxis = false;
			for (int i = 0; i < image.numDimensions(); i++) {
				if (image.axis(i).type() == axes[j]) {
					knownAxis = true;
					break;
				}
			}
			if (!knownAxis) unusedAxes.add(axes[j]);
		}

		AxisType[] result = new AxisType[image.numDimensions()];
		for (int i = 0; i < image.numDimensions(); i++) {
			result[i] = image.axis(i).type();
			boolean knownAxis = false;
			for (int j = 0; j < axes.length; j++) {
				if (image.axis(i).type() == axes[j]) {
					knownAxis = true;
					break;
				}
			}
			if (!knownAxis) unknownIndices.add(i);
		}

		for (int i = 0; i < unknownIndices.size() && i < unusedAxes.size(); i++) {
			result[unknownIndices.get(i)] = unusedAxes.get(i);
		}

		return result;

	}

	public static void logDim(final Task task, final String title,
		final Interval img)
	{
		logDim(task, title, img, false);
	}

	public static void debugDim(final Task task, final String title,
		final Interval img)
	{
		logDim(task, title, img, true);
	}

	private static void logDim(final Task task, final String title,
		final Interval img, boolean debug)
	{
		final long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		if (debug) {
			task.debug(title + ": " + Arrays.toString(dims));
		}
		else {
			task.log(title + ": " + Arrays.toString(dims));
		}
	}

}
