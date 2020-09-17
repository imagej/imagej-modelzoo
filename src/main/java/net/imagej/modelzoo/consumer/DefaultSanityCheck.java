package net.imagej.modelzoo.consumer;

import net.imagej.Dataset;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
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
import org.scijava.plugin.Parameter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.Arrays;

public class DefaultSanityCheck implements SanityCheck {

	@Parameter
	private OpService opService;

	private static final Color colorRow1 = new Color(0xb055aa00, true);
	private static final Color colorRow2 = new Color(0xb0ff00ff, true);
	private static final Color colorRow3 = new Color(0xb000aaff, true);
	private static final String sanity_check_input = "Sanity Check Input";
	private static final String model_prediction = "Model Prediction";
	private static final String expected = "Expected (GT)";
	private static int numBins = 40;

	public static <T extends RealType<T>, U extends RealType<U>> void compare(Dataset input, Dataset output, Dataset gt, RandomAccessibleInterval<T> modelDemoInput, RandomAccessibleInterval<U> modelDemoOutput, OpService opService) {
		compare(
				(RandomAccessibleInterval)gt,
				(RandomAccessibleInterval)input,
				(RandomAccessibleInterval)output,
				modelDemoInput,
				modelDemoOutput,
				opService);
	}

	public static <T extends RealType<T>, U extends RealType<U>, V extends RealType<V>, W extends RealType<W>, X extends RealType<X>> void compare(
			RandomAccessibleInterval<T> gt,
			RandomAccessibleInterval<U> input,
			RandomAccessibleInterval<V> output,
			RandomAccessibleInterval<W> modelDemoInput,
			RandomAccessibleInterval<X> modelDemoOutput,
			OpService opService) {

		Pair<T, T> minMaxGT = opService.stats().minMax(Views.iterable(gt));
		Pair<U, U> minMaxInput = opService.stats().minMax(Views.iterable(input));
		Pair<V, V> minMaxOutput = opService.stats().minMax(Views.iterable(output));
		Pair<W, W> minMaxModelDemoInput = opService.stats().minMax(Views.iterable(modelDemoInput));
		Pair<X, X> minMaxModelDemoOutput = opService.stats().minMax(Views.iterable(modelDemoOutput));
		double mseOut = calculateMSE(gt, output);
		double psnrOut = calculatePSNR(mseOut, minMaxOutput);
		double mseIn = calculateMSE(gt, input);
		double psnrIn = calculatePSNR(mseOut, minMaxInput);

		Object[][] table1Data = new Object[6][7];
		addHeader(table1Data[0]);
		addData(table1Data[1], input, sanity_check_input, minMaxInput, opService);
		addData(table1Data[2], output, model_prediction, minMaxOutput, opService);
		addData(table1Data[3], gt, expected, minMaxGT, opService);
		addData(table1Data[4], modelDemoInput, "Model Demo Input", minMaxModelDemoInput, opService);
		addData(table1Data[5], modelDemoOutput, "Model Demo Output", minMaxModelDemoOutput, opService);
		JTable table1 = getJTable(table1Data);

		JPanel histogramPanel = new JPanel(new MigLayout("fill, ins 0, gapx 0"));
		addSeries(histogramPanel, input, sanity_check_input, colorRow1);
		addSeries(histogramPanel, gt, expected, colorRow3);
		addSeries(histogramPanel, output, model_prediction, colorRow2);

		JPanel panel1 = new JPanel(new MigLayout("fill, flowy"));
		panel1.setOpaque(false);
		panel1.add(table1, "height 180px, growx, pushx, spanx");
		panel1.add(histogramPanel, "width 500px, height 180px, grow, push, span");

		HistogramDataset histogram = new HistogramDataset();
		histogram.setType(HistogramType.RELATIVE_FREQUENCY);
		addSeries(histogram, input, sanity_check_input);
		addSeries(histogram, output, model_prediction);
		addSeries(histogram, gt, "Expected (GT)");

		JPanel psnrTablePanel = new JPanel(new MigLayout("alignx center"));
		psnrTablePanel.setBackground(Color.white);
		psnrTablePanel.add(getPSNRTable(mseIn, psnrIn, "Comparing GT to input"));
		psnrTablePanel.add(getPSNRTable(mseOut, psnrOut, "Comparing GT to prediction"));

		JPanel panel = new JPanel(new MigLayout("fill"));
		panel.setBackground(Color.white);
		panel.add(panel1, "span, push, grow");
		ChartPanel chartPanel = getChartPanel(histogram, new Paint[]{colorRow1, colorRow2, colorRow3}, false);
		panel.add(chartPanel, "newline, width 250px, height 180px");
		JLabel psnrDifferenceLabel = new JLabel("<html><div style='text-align: center;'>" +
				getColoredValue("Δ MSE", mseOut - mseIn, false) + " " +
				getColoredValue("Δ PSNR", psnrOut - psnrIn, true) +
				"</div>");
		JPanel psnrPanel = new JPanel(new MigLayout());
		psnrPanel.setOpaque(false);
		psnrPanel.add(psnrTablePanel, "alignx center");
		psnrPanel.add(psnrDifferenceLabel, "newline, alignx center");
		panel.add(psnrPanel, "");
		panel.add(getPSNRInfo(), "newline, spanx");

		JFrame frame = new JFrame("Comparison");
		frame.setBackground(Color.white);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
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
				else if(row == 3 && column == 0) jc.setBorder(row3Border);
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

	private static <T extends RealType<T>> void addSeries(HistogramDataset dataset, RandomAccessibleInterval<T> input, String title) {
		double[] values = new double[entries(input)];
		int i = 0;
		for (T realType : Views.iterable(input)) {
			values[i++] = realType.getRealDouble();
		}
		dataset.addSeries(title, values,  numBins);
	}

	private static <T extends RealType<T>> void addSeries(JPanel panel, RandomAccessibleInterval<T> input, String title, Color color) {
		HistogramDataset histogram = new HistogramDataset();
		histogram.setType(HistogramType.RELATIVE_FREQUENCY);
		addSeries(histogram, input, title);
		panel.add(getChartPanel(histogram, new Paint[]{color}, true));
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

	private static Component getPSNRInfo() {
		String text2 = "<html><p style='font-weight: normal;'>Calculating the PSNR between the GT and the Sanity Check input" +
				" is a measure for how noisy the input image is compared to GT." +
				" The PSNR between GT and the model prediction, in contrast, " +
				" represents how noisy the prediction of the trained model is compared to GT." +
				" The difference of these two values can indicate how much noise the trained model" +
				" was able to remove from the given Sanity Check Input - a positive PSNR change indicates" +
				" successful noise reduction.</p>";
		String text1 = "<html>" +
				"<b>GT  </b>: <span style='font-weight: normal;'>Ground truth (that's what the perfect result would look like)</span><br>" +
				"<b>MSE </b>: <span style='font-weight: normal;'>Mean squared error (smaller is better)</span><br>" +
				"<b>PSNR</b>: <span style='font-weight: normal;'>Peak signal-to-noise ratio (higher is better)</span><br>";
		JPanel panel  = new JPanel(new MigLayout("flowy"));
		panel.add(new JLabel(text1), "width 700px, height 50px, gaptop 10px");
		panel.add(new JLabel(text2), "width 700px, height 90px, gaptop 10px");
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

	private static <T extends RealType<T>> void addData(Object[] row, RandomAccessibleInterval<T> img, String title, Pair<T, T> minMax, OpService opService) {
		row[0] = title;
		row[1] = Arrays.toString(Intervals.dimensionsAsIntArray(img));
		row[2] = String.format("%.3f", minMax.getA().getRealFloat());
		row[3] = String.format("%.3f", minMax.getB().getRealFloat());
		row[4] = String.format("%.3f", opService.stats().mean(Views.iterable(img)).getRealFloat());
		row[5] = String.format("%.3f", opService.stats().median(Views.iterable(img)).getRealFloat());
		row[6] = String.format("%.3f", opService.stats().stdDev(Views.iterable(img)).getRealFloat());
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

	private static <T extends RealType<T>, U extends RealType<U>> double calculateMSE(RandomAccessibleInterval<T> gt, RandomAccessibleInterval<U> output) {
		RandomAccess<U> outputRa = output.randomAccess();
		Cursor<T> gtCursor = Views.iterable(gt).localizingCursor();
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

	@Override
	public void checkInteractive(Dataset input, Dataset output, Dataset gt, ModelZooArchive model) {
		compare(input, output, gt, model.getTestInput(), model.getTestOutput(), opService);
	}
}
