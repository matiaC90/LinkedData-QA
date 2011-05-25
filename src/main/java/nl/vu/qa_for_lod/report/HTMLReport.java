/**
 * 
 */
package nl.vu.qa_for_lod.report;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import com.googlecode.charts4j.Shape;
import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.ExtraLinks;
import nl.vu.qa_for_lod.MetricsExecutor;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.metrics.Distribution.DistributionAxis;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class HTMLReport {
	private final DecimalFormat df = new DecimalFormat("###.##");
	private final StringBuffer buffer = new StringBuffer();

	/**
	 * @param datasetName
	 */
	public HTMLReport(String datasetName) {
		buffer.append("<!DOCTYPE HTML><html><head><title>Execution report for ");
		buffer.append(datasetName).append("</title>");
		buffer.append("<style type=\"text/css\">");
		buffer.append("body {color: #000000;font-family: Helvetica,Arial,sans-serif;font-size: small;}");
		buffer.append("h1 {background-color: #E5ECF9;border-top: 1px solid #3366CC;font-size: 130%;font-weight: bold;margin: 2em 0 0 -10px;padding: 1px 3px;}");
		buffer.append("td {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 6px 12px;text-align: left;vertical-align: top;}");
		buffer.append("img {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 20px 20px;}");
		buffer.append("</style></head>");
	}
	
	/**
	 * @param datasetName
	 * @param executor
	 * @param extraLinks
	 * @return
	 */
	public static HTMLReport createReport(String datasetName, MetricsExecutor executor, ExtraLinks extraLinks) {
		HTMLReport report = new HTMLReport(datasetName);
		report.appendMetricStatuses(executor);
		report.appendHallOfFame(executor);
		report.appendDistributions(executor);
		report.close();
		return report;
	}

	/**
	 * @param executor
	 */
	private void appendDistributions(MetricsExecutor executor) {
		buffer.append("<h1>Metric distributions</h1>");
		for (Entry<Metric, MetricData> entry : executor.metricsData()) {
			// Get the distributions
			Distribution observedDistribution = entry.getValue().getDistribution(MetricState.AFTER);
			Distribution idealDistribution = entry.getKey().getIdealDistribution(observedDistribution);

			// Normalise the distributions
			observedDistribution.normalize();
			idealDistribution.normalize();

			// Generate the chart
			GChart chart = getChart(entry.getKey().getName(), observedDistribution, idealDistribution);
			buffer.append("<img src=\"").append(chart.toURLForHTML()).append("\"/><br/>");
		}
	}

	/**
	 * @param name
	 * @param observed
	 * @param ideal
	 */
	private GChart getChart(String name, Distribution observed, Distribution ideal) {
		// Get the list of keys
		TreeSet<Double> keys = new TreeSet<Double>();
		keys.addAll(observed.keySet());
		keys.addAll(ideal.keySet());

		// Get the list of values for every key
		List<Double> observedData = new ArrayList<Double>();
		List<Double> idealData = new ArrayList<Double>();
		double d = 0;
		for (Double key : keys) {
			observedData.add(observed.get(key));
			idealData.add(ideal.get(key));
			d += ideal.get(key);
		}
		Line d1 = Plots.newLine(DataUtil.scale(observedData), Color.BLUE, "observed");
		Line d2 = Plots.newLine(DataUtil.scale(idealData), Color.RED, "ideal");
		d1.addShapeMarkers(Shape.CIRCLE, Color.BLUE, 6);
		d2.addShapeMarkers(Shape.CIRCLE, Color.RED, 6);

		LineChart chart = GCharts.newLineChart(d1, d2);
		AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 13, AxisTextAlignment.CENTER);
		AxisLabels count = AxisLabelsFactory.newNumericRangeAxisLabels(0, observed.max(DistributionAxis.Y));
		count.setAxisStyle(axisStyle);
		chart.addYAxisLabels(count);
		AxisLabels values = AxisLabelsFactory.newNumericRangeAxisLabels(keys.first(), keys.last());
		values.setAxisStyle(axisStyle);
		chart.addXAxisLabels(values);
		chart.setSize(650, 400);
		chart.setTitle("Observed and ideal distributions for " + name, Color.BLACK, 16);

		return chart;

	}

	/**
	 * @param uri
	 * @param fileName
	 * @throws IOException
	 */
	protected void saveURIToFile(String uri, String fileName) throws IOException {
		URL url = new URL(uri);
		URLConnection urlConnection = url.openConnection();
		InputStream in = new BufferedInputStream(urlConnection.getInputStream());
		byte[] datad = new byte[urlConnection.getContentLength()];
		int bytesRead = 0;
		int offset = 0;
		while (offset < urlConnection.getContentLength()) {
			bytesRead = in.read(datad, offset, datad.length - offset);
			if (bytesRead == -1)
				break;
			offset += bytesRead;
		}
		in.close();

		FileOutputStream out = new FileOutputStream(fileName);
		out.write(datad);
		out.flush();
		out.close();
	}

	/**
	 * @param executor
	 * 
	 */
	private void appendMetricStatuses(MetricsExecutor executor) {
		buffer.append("<h1>Metric statuses</h1>");
		buffer.append("<table><tr>");
		buffer.append("<th>Metric name</th>");
		buffer.append("<th>Status</th>");
		buffer.append("<th>Improvement</th>");
		buffer.append("</tr>");
		for (Entry<Metric, MetricData> entry : executor.metricsData()) {
			buffer.append("<tr>");
			buffer.append("<td>").append(entry.getKey().getName()).append("</td>");
			buffer.append("<td>").append(entry.getValue().isGreen() ? "green " : "red ").append("</td>");
			buffer.append("<td>").append(df.format(100 - entry.getValue().getRatioDistanceChange())).append("</td>");
			buffer.append("</tr>");
		}
		buffer.append("</table>");
	}

	/**
	 * @param executor
	 * @param extraLinks
	 * 
	 */
	private void appendHallOfFame(MetricsExecutor executor) {
		// Compute the hall of fame
		Map<Resource, Double> scores = new HashMap<Resource, Double>();
		for (Entry<Metric, MetricData> entry : executor.metricsData()) {
			List<Resource> suspects = entry.getValue().getSuspiciousNodes();
			for (int index = 0; index < suspects.size(); index++) {
				Resource key = suspects.get(index);
				if (!scores.containsKey(key))
					scores.put(key, new Double(0));
				scores.put(key, scores.get(key) + suspects.size() - index);
			}
		}

		// Get an ordered list of scores
		TreeSet<Double> keys = new TreeSet<Double>();
		keys.addAll(scores.values());
		double max = executor.metricsData().size() * scores.size() * 1.0d;

		// Insert the HTML code
		buffer.append("<h1>Most suspicious nodes</h1>");
		buffer.append("<table><tr>");
		buffer.append("<th>Score</th>");
		buffer.append("<th>Resource</th>");
		buffer.append("</tr>");
		int size = 10;
		for (Double key : keys.descendingSet()) {
			for (Entry<Resource, Double> entry : scores.entrySet()) {
				if (size > 0 && entry.getValue().equals(key)) {
					buffer.append("<tr>");
					buffer.append("<td>").append(df.format(entry.getValue() / max)).append("</td>");
					buffer.append("<td>").append(entry.getKey()).append("</td>");
					buffer.append("</tr>");
					size--;
				}
			}
		}
		buffer.append("</table>");
	}

	/**
	 * 
	 */
	private void close() {
		buffer.append("</html>");
	}

	/**
	 * @param fileName
	 * @throws IOException
	 */
	public void writeTo(String fileName) throws IOException {
		Writer writer = new FileWriter(fileName);
		writer.write(buffer.toString());
		writer.close();
	}

}
