package nl.vu.qa_for_lod;

import java.io.File;

import nl.vu.qa_for_lod.metrics.impl.ClusteringCoefficient;
import nl.vu.qa_for_lod.metrics.impl.Degree;
import nl.vu.qa_for_lod.report.HTMLReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO add a metric to detect when most mappings are 1-1 and some 1-M (then
// they are suspicious)
public class App {
	static Logger logger = LoggerFactory.getLogger(App.class);

	private void process(String dataName) throws Exception {
		File file = new File(dataName);
		if (!file.exists())
			throw new Exception("File not found");

		// Load the graph file
		ExtraLinks extraLinks = new ExtraLinks(dataName);
		logger.info("Number of resources  = " + extraLinks.getResources().size());
		logger.info("Number of statements = " + extraLinks.getStatements().size());

		// Run the analysis
		MetricsExecutor metrics = new MetricsExecutor(extraLinks);
		metrics.addMetric(new Degree());
		metrics.addMetric(new ClusteringCoefficient());

		// Set the list of nodes to check
		for (Resource resource : extraLinks.getResources())
			metrics.addToResourcesQueue(resource);

		// Run all the metrics
		metrics.processQueue();

		// Generate the analysis report
		logger.info("Save execution report");
		HTMLReport report = HTMLReport.createReport(file.getName(), metrics, extraLinks);
		report.writeTo("/tmp/" + file.getName().replace(".","_") + "_report.html");
	}

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.process("data/links-beaches.nt");
	}
}
