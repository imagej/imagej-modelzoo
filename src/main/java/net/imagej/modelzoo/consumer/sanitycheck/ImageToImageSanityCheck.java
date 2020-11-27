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
package net.imagej.modelzoo.consumer.sanitycheck;

import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

public class ImageToImageSanityCheck implements SanityCheck {

	@Parameter
	private OpService opService;

	private static final Color colorRow1 = new Color(0xb055aa00, true);
	private static final Color colorRow2 = new Color(0xb0ff00ff, true);
	private static final Color colorRow3 = new Color(0xb000aaff, true);
	private static final String sanityCheckInput = "Sanity Check Input";
	private static final String modelPrediction = "Model Prediction";
	private static final String modelPredictionNormalized = "Prediction (min-norm)";
	private static final String expected = "Expected (GT)";
	private static int numBins = 40;
	private static final Font arial = new JLabel().getFont().deriveFont(Font.PLAIN);

	static class Stats<T extends RealType<T>> {
		String title;
		RandomAccessibleInterval<T> source;
		Pair<T, T> minMax;
		float mean;
		float median;
		float stdDev;
		Stats(RandomAccessibleInterval<T> in, String title, OpService opService) {
			source = in;
			this.title = title;
			IterableInterval<T> iterable = Views.iterable(in);
			minMax = opService.stats().minMax(iterable);
			mean = opService.stats().mean(iterable).getRealFloat();
			median = opService.stats().median(iterable).getRealFloat();
			stdDev = opService.stats().stdDev(iterable).getRealFloat();
		}

		@Override
		public String toString() {
			return title + " min: " + minMax.getA()
					+ " max: " + minMax.getB()
					+ " mean: " + mean
					+ " median: " + median
					+ " stdDev: " + stdDev;
		}
	}

	public ImageToImageSanityCheck(Context context) {
		context.inject(this);
	}

	@Override
	public void checkInteractive(List<?> input, List<?> output, List<?> gt, ModelZooArchive model) {
		if(model.getSpecification().getFormatVersion().compareTo("0.2.1-csbdeep") < 0
				|| model.getSampleInputs() == null
				|| model.getSampleOutputs() == null) {
			compare((RandomAccessibleInterval)input.get(0),
					(RandomAccessibleInterval)output.get(0),
					(RandomAccessibleInterval)gt.get(0),
					null, null, opService);
		} else {
			compare((RandomAccessibleInterval)input.get(0),
					(RandomAccessibleInterval)output.get(0),
					(RandomAccessibleInterval)gt.get(0),
					model,
					opService);
		}
	}

	public static void compare(
			RandomAccessibleInterval input,
			RandomAccessibleInterval output,
			RandomAccessibleInterval groundTruth,
			ModelZooArchive model,
			OpService opService) {

		RandomAccessibleInterval sampleInput = null;
		if(model.getSampleInputs() != null && model.getSampleInputs().size() > 0) {
			sampleInput = (RandomAccessibleInterval) model.getSampleInputs().get(0).getData();
		}
		RandomAccessibleInterval sampleOutput = null;
		if(model.getSampleOutputs() != null && model.getSampleOutputs().size() > 0) {
			sampleOutput = (RandomAccessibleInterval) model.getSampleOutputs().get(0).getData();
		}
		compare(input, output, groundTruth,
				sampleInput,
				sampleOutput,
				opService);
	}

	public static <T extends RealType<T>, U extends RealType<U>, V extends RealType<V>, W extends RealType<W>, X extends RealType<X>> void compare(
			RandomAccessibleInterval<U> input,
			RandomAccessibleInterval<V> output,
			RandomAccessibleInterval<T> gt,
			RandomAccessibleInterval<W> modelDemoInput,
			RandomAccessibleInterval<X> modelDemoOutput,
			OpService opService) {


		Stats<T> statsGT = new Stats<>(gt, expected, opService);
		Stats<U> statsInput = new Stats<>(input, sanityCheckInput, opService);
		Stats<V> statsOutput = new Stats<>(output, modelPrediction, opService);
		Stats<W> statsModelDemoInput = null;
		Stats<X> statsModelDemoOutput = null;
		if(modelDemoInput != null && modelDemoOutput != null) {
			statsModelDemoInput = new Stats<>(modelDemoInput, "Model Demo Input", opService);
			statsModelDemoOutput = new Stats<>(modelDemoOutput, "Model Demo Output", opService);
		}

		RandomAccessibleInterval<FloatType> outputNormalized = normalizeMinimize(statsOutput, statsInput, opService);
		Stats<FloatType> statsOutputNormalized = new Stats<>(outputNormalized, modelPredictionNormalized, opService);

		double mseOut = calculateMSE(statsGT, statsOutputNormalized);
		double psnrOut = calculatePSNR(mseOut, statsGT.minMax);
		double mseIn = calculateMSE(statsGT, statsInput);
		double psnrIn = calculatePSNR(mseIn, statsGT.minMax);

		Object[][] table1Data;
		if(statsModelDemoInput != null) {
			table1Data = new Object[7][7];
		} else {
			table1Data = new Object[5][7];
		}
		addHeader(table1Data[0]);
		addData(table1Data[1], statsInput);
		addData(table1Data[2], statsOutput);
		addData(table1Data[3], statsOutputNormalized);
		addData(table1Data[4], statsGT);
		if(statsModelDemoInput != null) {
			addData(table1Data[5], statsModelDemoInput);
			addData(table1Data[6], statsModelDemoOutput);
		}
		JTable statsTable = getJTable(table1Data);

		JPanel histogramPanel = new JPanel(new MigLayout("fillx, ins 0, gapx 0"));
		addSeries(histogramPanel, statsInput, colorRow1);
		addSeries(histogramPanel, statsGT, colorRow3);
		addSeries(histogramPanel, statsOutput, colorRow2);

		HistogramDataset histogram = new HistogramDataset();
		histogram.setType(HistogramType.RELATIVE_FREQUENCY);
		addSeries(histogram, statsGT);
		addSeries(histogram, statsOutput);
		addSeries(histogram, statsInput);

		ChartPanel chartPanel = getChartPanel(histogram, new Paint[]{colorRow1, colorRow2, colorRow3}, false);

		JPanel summedHistogramPSNRPanel = new JPanel(new MigLayout("fillx"));
		summedHistogramPSNRPanel.setOpaque(false);
		summedHistogramPSNRPanel.add(chartPanel, "newline, width 250px, height 180px");
		summedHistogramPSNRPanel.add(createPSNRPanel(mseIn, psnrIn, mseOut, psnrOut), "");

		JPanel mainPanel = new JPanel(new MigLayout("fillx, flowy"));
		mainPanel.setBackground(Color.white);
		mainPanel.add(getTableInfo(), "growx, wmin 0, width 0");
		mainPanel.add(statsTable, "growx");
		mainPanel.add(histogramPanel, "height 200px, growx, wmin 0, width 0");
		mainPanel.add(summedHistogramPSNRPanel, "spanx, growx");
		mainPanel.add(getPSNRInfo(), "growx, wmin 0, width 0");

		displayInFrame(mainPanel);
	}

	@NotNull
	private static JPanel createPSNRPanel(double mseIn, double psnrIn, double mseOut, double psnrOut) {
		JPanel psnrTablePanel = new JPanel(new MigLayout("alignx center"));
		psnrTablePanel.setBackground(Color.white);
		psnrTablePanel.add(getPSNRTable(mseIn, psnrIn, "GT → input"));
		psnrTablePanel.add(getPSNRTable(mseOut, psnrOut, "GT → prediction (min-norm)"));
		JLabel psnrDifferenceLabel = new JLabel("<html><div style='text-align: center;'>" +
				getColoredValue("Δ MSE", mseOut - mseIn, false) + " " +
				getColoredValue("Δ PSNR", psnrOut - psnrIn, true) +
				"</div>");
		JPanel psnrPanel = new JPanel(new MigLayout());
		psnrPanel.setOpaque(false);
		psnrPanel.add(psnrTablePanel, "alignx center");
		psnrPanel.add(psnrDifferenceLabel, "newline, alignx center");
		return psnrPanel;
	}

	private static void displayInFrame(JPanel panel) {
		JFrame frame = new JFrame("Comparison");
		frame.setPreferredSize(new Dimension(800, 600));
		frame.setBackground(Color.white);
		frame.setContentPane(new JScrollPane(panel));
		frame.pack();
		frame.setVisible(true);
	}

	static <U extends RealType<U>, V extends RealType<V>> RandomAccessibleInterval<FloatType> normalizeMinimize(
			Stats<U> inputStats, Stats<V> targetStats, OpService opService) {
		Img<FloatType> result = opService.create().img(inputStats.source, new FloatType());

		long n = Intervals.numElements(inputStats.source);

		double varianceIn = Math.pow(inputStats.stdDev, 2.);
		double covariance = 0;
		Cursor<U> cursor = Views.iterable(inputStats.source).localizingCursor();
		RandomAccess<V> raOut = targetStats.source.randomAccess();
		while(cursor.hasNext()) {
			cursor.next();
			double xDev = cursor.get().getRealDouble() - inputStats.mean;
			double yDev = raOut.setPositionAndGet(cursor).getRealDouble() - targetStats.mean;
			covariance += (xDev * yDev);
		}
		covariance /= (double)(n - 1);
		double alpha = covariance / (varianceIn+1E-10);
		double beta = targetStats.mean - alpha*inputStats.mean;
		LoopBuilder.setImages(inputStats.source, result).multiThreaded().forEachPixel((in, res) -> {
				res.setReal(alpha * in.getRealDouble() + beta);
		});
		return result;
	}

	@NotNull
	private static JTable getJTable(Object[][] table1Data) {
		TableModel tableModel = new DefaultTableModel(table1Data, new Object[7]);
		JTable table1 = new JTable(tableModel) {
			Border row1Border = BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 10, 0, 0, colorRow1),
					BorderFactory.createMatteBorder(0, 10, 0, 0, Color.white));
			Border row2Border = BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 10, 0, 0, colorRow2),
					BorderFactory.createMatteBorder(0, 10, 0, 0, Color.white));
			Border row3Border = BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 10, 0, 0, colorRow3),
					BorderFactory.createMatteBorder(0, 10, 0, 0, Color.white));
			Border defaultBorder = BorderFactory.createMatteBorder(0, 20, 0, 0, Color.white);
			public Component prepareRenderer(
					TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				JComponent jc = (JComponent)c;
				if(row == 1 && column == 0) jc.setBorder(row1Border);
				else if(row == 2 && column == 0) jc.setBorder(row2Border);
				else if(row == 4 && column == 0) jc.setBorder(row3Border);
				else if(column == 0) jc.setBorder(defaultBorder);
				return c;
			}
		};
		style(table1);
		table1.getColumnModel().getColumn(0).setWidth(170);
		table1.getColumnModel().getColumn(0).setPreferredWidth(170);
		table1.getColumnModel().getColumn(1).setWidth(100);
		table1.getColumnModel().getColumn(1).setPreferredWidth(100);
		return table1;
	}

	private static void addHeader(Object[] row) {
		row[0] = "";
		row[1] = "Dimensions";
		row[2] = "Min";
		row[3] = "Max";
		row[4] = "Mean";
		row[5] = "Median";
		row[6] = "StdDev";
	}

	private static <T extends RealType<T>> void addSeries(HistogramDataset dataset, Stats<T> input) {
		double[] values = new double[entries(input.source)];
		int i = 0;
		for (T realType : Views.iterable(input.source)) {
			values[i++] = realType.getRealDouble();
		}
		dataset.addSeries(input.title, values,  numBins);
	}

	private static <T extends RealType<T>> void addSeries(JPanel panel, Stats<T> input, Color color) {
		HistogramDataset histogram = new HistogramDataset();
		histogram.setType(HistogramType.RELATIVE_FREQUENCY);
		addSeries(histogram, input);
		panel.add(getChartPanel(histogram, new Paint[]{color}, true), "growx");
	}

	private static String getColoredValue(String title, double value, boolean positiveIsGood) {
		String color = "black";
		if(positiveIsGood && value > 0) color = "green";
		if(!positiveIsGood && value < 0) color = "green";
		if(positiveIsGood && value < 0) color = "red";
		if(!positiveIsGood && value > 0) color = "red";
		return "<span style='padding: 10px; font-size: 150%;'>" + title + " <span style='color: " + color + ";'> "
				+ String.format(" %.2f </span></span>", value);
	}

	private static Component getTableInfo() {
		JPanel panel  = new JPanel(new MigLayout("flowy, fillx"));
		panel.add(new JLabel(sanityCheckInput), "gaptop 10px, spanx");
		String style = "spanx, growx, wmin 10px";
		panel.add(makeText("The input image chosen for this sanity check."), style);
		panel.add(new JLabel(modelPrediction), style);
		panel.add(makeText("The result of applying the model to the input image."), style);
		panel.add(new JLabel(modelPredictionNormalized), style);
		panel.add(makeText("In case the input image lives in a different value range than what the network " +
				"expects, the result might be shifted. In order to compare the quality of the result independently of this shift, " +
				"we normalize it to be in the range of the input image."), style);
		panel.setOpaque(false);
		return panel;
	}

	@NotNull
	private static Component makeText(String text) {
		JTextArea textArea = new JTextArea();
		textArea.setText(text);
		textArea.setFont(arial);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		return textArea;
	}

	private static Component getPSNRInfo() {
		String text1 = "<html>" +
				"<b>GT  </b>: <span style='font-weight: normal;'>Ground truth (that's what the perfect result would look like)</span><br>" +
				"<b>MSE </b>: <span style='font-weight: normal;'>Mean squared error (smaller is better)</span><br>" +
				"<b>PSNR</b>: <span style='font-weight: normal;'>Peak signal-to-noise ratio (higher is better)</span><br>";
		String text2 = "Calculating the PSNR between the GT and the Sanity Check input" +
				" is a measure for how noisy the input image is compared to GT." +
				" The PSNR between GT and the model prediction, in contrast, " +
				" represents how noisy the prediction of the trained model is compared to GT." +
				" The difference of these two values can indicate how much noise the trained model" +
				" was able to remove from the given Sanity Check Input - a positive PSNR change indicates" +
				" successful noise reduction.";
		JPanel panel  = new JPanel(new MigLayout("flowy, fillx"));
//		panel.add(new JLabel(text1), "height 50px, growx, spanx, gaptop 10px");
		panel.add(makeText(text2), "growx, spanx, wmin 10px");
		panel.setOpaque(false);
		return panel;
	}

	@NotNull
	private static <T extends RealType<T>, V extends RealType<V>> Component getPSNRTable(double mse, double psnr, String title) {
		Object[][] tableData = new Object[2][2];
		Object[] tableHeader = new Object[2];
		tableData[0][0] = "MSE";
		tableData[1][0] = "PSNR";
		tableData[0][1] = String.format("%.2f", mse);
		tableData[1][1] = String.format("%.2f", psnr);
		TableModel tableModel = new DefaultTableModel(tableData, tableHeader);
		JTable tableGTOut = new JTable(tableModel) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		style(tableGTOut);
		Color color = new Color(240, 240, 240);
		tableGTOut.setBackground(color);
		JPanel panel = new JPanel(new MigLayout("flowy, fill, width 220px"));
		panel.setBackground(color);
		panel.add(new JLabel(title));
		panel.add(tableGTOut, "growx, spanx, pushx");
		return panel;
	}

	private static <T extends RealType<T>> void addData(Object[] row, Stats<T> stats) {
		row[0] = stats.title;
		row[1] = Arrays.toString(Intervals.dimensionsAsIntArray(stats.source));
		row[2] = String.format("%.3f", stats.minMax.getA().getRealFloat());
		row[3] = String.format("%.3f", stats.minMax.getB().getRealFloat());
		row[4] = String.format("%.3f", stats.mean);
		row[5] = String.format("%.3f", stats.median);
		row[6] = String.format("%.3f", stats.stdDev);
	}

	private static void style(JTable table) {
		table.setShowGrid(false);
		table.setRowHeight(30);
		table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
	}

	private static ChartPanel getChartPanel(HistogramDataset dataset, Paint[] paintArray, boolean showLegend) {
		JFreeChart res = ChartFactory.createHistogram("",
				"", "", dataset);
		if(!showLegend) res.removeLegend();
		XYPlot plot = (XYPlot) res.getPlot();
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setBackgroundPaint(Color.white);
		plot.setOutlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		NumberFormat percentInstance = NumberFormat.getPercentInstance();
		percentInstance.setMaximumFractionDigits(0);
		rangeAxis.setNumberFormatOverride(percentInstance);
		rangeAxis.setVisible(false);
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardXYBarPainter());
		plot.setDrawingSupplier(new DefaultDrawingSupplier(
				paintArray,
				DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
		res.setBorderVisible(false);
		ChartPanel chart = new ChartPanel(res);
		chart.setBackground(Color.white);
		chart.setMinimumDrawWidth(0);
		chart.setMaximumDrawWidth(Integer.MAX_VALUE);
		chart.setMinimumDrawHeight(0);
		chart.setMaximumDrawHeight(Integer.MAX_VALUE);
		return chart;
	}

	private static <T extends RealType<T>> int entries(RandomAccessibleInterval<T> img) {
		int res = 1;
		for (int i = 0; i < img.numDimensions(); i++) {
			res *= img.dimension(i);
		}
		return res;
	}

	private static <T extends RealType<T>, U extends RealType<U>> double calculateMSE(Stats<T> gt, Stats<U> output) {
		RandomAccess<U> outputRa = output.source.randomAccess();
		Cursor<T> gtCursor = Views.iterable(gt.source).localizingCursor();
		long numPix = 0;
		float sumSquareDif = 0;
		while(gtCursor.hasNext()) {
			gtCursor.next();
			outputRa.setPosition(gtCursor);
			sumSquareDif += Math.pow(gtCursor.get().getRealFloat() - outputRa.get().getRealFloat(), 2);
			numPix++;
		}
		return sumSquareDif / (double) numPix;
	}

	private static <T extends RealType<T>> double calculatePSNR(double mse, Pair<T, T> minMax) {
		T dif = minMax.getB().copy();
		dif.sub(minMax.getA());
		double minMaxDif = dif.getRealDouble();
		return 20.*Math.log10(minMaxDif) - 10*Math.log10(mse);
	}
}
