package hidingsrc.experiment.runners;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import hidingsrc.centrality.*;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.*;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.ExperimentRunner;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.*;
import hidingsrc.experiment.heuristic.bots.*;
import hidingsrc.experiment.heuristic.edge.*;
import hidingsrc.experiment.performers.HidingSourceExperiment;
import hidingsrc.srcdetection.*;
import hidingsrc.utils.Utils;

/**
 * Running experiments with hiding source of diffusion from source detection algorithms.
 * 
 * @author Marcin Waniek
 */
public class RunHidingSource extends ExperimentRunner {
	
	protected static final EpidemicModel EPIDEMIC_MODEL = new SIModel(.15, 5);
	protected static final List<ScoringSourceDetectionAlgorithm> ALGORITHMS = Utils.aList(
		new CentralitySourceDetection(new DegreeCentrality()),
		new CentralitySourceDetection(new EigenvectorCentrality(.00001)),
		new CentralitySourceDetection(new ClosenessCentrality()),
		new CentralitySourceDetection(new RumorCentrality()),
		new MonteCarloSourceDetection(EPIDEMIC_MODEL),
		new RandomWalkSourceDetection(EPIDEMIC_MODEL),
		new CentralitySourceDetection(new BetweennessCentrality())	
	);
	protected static final int EVADERS_NUM = 10;
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? Utils.concat(getBotHeuristics(1), getBotHeuristics(3)) : getEdgeHeuristics(true);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;
		
		RunHidingSource r = new RunHidingSource();
		
		r.runErdosRenyi(n, avgDegree, times, heurs, steps);
		r.runSmallWorld(n, avgDegree, .25, times, heurs, steps);
		r.runBarabasiAlbert(n, avgDegree, times, heurs, steps);
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "hiding-source";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		new HidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps, EVADERS_NUM).perform();
	}
	
	protected static List<Heuristic> getBotHeuristics(int linksToContacts){
		return Utils.aList(new HubBotHeuristic(linksToContacts, false), new HubBotHeuristic(linksToContacts, true),
				new RandomBotHeuristic(linksToContacts, false), new RandomBotHeuristic(linksToContacts, true),
				new DegreeBotHeuristic(linksToContacts, false), new DegreeBotHeuristic(linksToContacts, true));
	}
	
	protected static List<Heuristic> getEdgeHeuristics(boolean mixed){
		List<Heuristic> res = Utils.aList();
		List<RemoveHeuristic> remHeurs = Utils.aList(new RemoveRandom(), new RemoveMaxDegree(), new RemoveMinDegree());
		List<AddHeuristic> addHeurs = Utils.aList(new AddRandom(), new AddMaxDegree(), new AddMinDegree());
		res.addAll(remHeurs);
		res.addAll(addHeurs);
		if (mixed)
			for (RemoveHeuristic remHeur : remHeurs)
				for (AddHeuristic addHeur : addHeurs)
					res.add(new MixHeuristic(remHeur, addHeur));
		return res;
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(
				new BeforeBarsAggregator(),
				new BotsLineAggregator(),
				new BotsHeatmapAggregator(),
				new EdgesLineAggregator(),
				new EdgesHeatmapAggregator(),
				new EdgesMixedHeatmapAggregator()
			);
	}
	
	public static class BeforeBarsAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "standard-before-bars";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			res.filter(r -> r.getInt("step") == 0);
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, Utils.aList("graph", "model", "algorithm"), Utils.aList("ranking"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("unify", r ->	r.concatVals("model") + "-"
					+ (Utils.aList("ba","ws","er").contains(r.get("graph").substring(0, 2)) ? "rand" : "real"));
			res.addColumn("label", r -> r.concatVals("graph", "model"));
			return res;
		}
	}
	
	public static abstract class LineAggregator extends ExperimentAggregator {

		protected abstract Predicate<Row> initialFilter();
		
		protected String prefixName() {
			return "standard-";
		}
		
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurType", "step");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return expand(rows.filter(initialFilter()), header, experimentDir).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, key(), Utils.aList("ranking"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("unify", r ->	r.concatVals("model", "algorithm")
					+ (Utils.aList("ba","ws","er").contains(r.get("graph").substring(0, 2)) ? "rand" : r.get("graph")));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm"));
			return res;
		}
	}

	public static class BotsLineAggregator extends LineAggregator {

		@Override
		public String getName() {
			return prefixName() + "bots-line";
		}
		
		@Override
		protected Predicate<Row> initialFilter() {
			return r -> "bots".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("lineColor", r -> {
				String[] parts = r.get("heuristic").split("-");
				return Utils.capitalize(parts[0]) + (parts.length > 2 ? " " + parts[1] : "");
			});
			res.addColumn("lineType", r -> {
				String[] parts = r.get("heuristic").split("-");
				return parts[parts.length - 1] + " supporter" + (parts[parts.length - 1].equals("1") ? "" : "s");
			});
			return res;
		}
	}
	
	public static class EdgesLineAggregator extends LineAggregator {

		@Override
		public String getName() {
			return prefixName() + "edges-line";
		}
		
		@Override
		protected Predicate<Row> initialFilter(){
			return r -> "edge-add".equals(r.get("heurType")) || "edge-rem".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("lineColor", r -> Utils.capitalize(heurNamePrint(r.get("heuristic"))));
			res.addColumn("lineType", r -> heurTypePrint(r.get("heurType")));
			return res;
		}
	}
	
	public static abstract class HeatmapAggregator extends ExperimentAggregator {

		protected abstract Predicate<Row> initialFilter();
		
		protected String prefixName() {
			return "standard-";
		}
		
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurType");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows.filter(initialFilter()), header, experimentDir);
			List<String> diffKey = key();
			diffKey.add("evader");
			diffKey.remove("step");
			res.addDiffColumn("rankChange", "ranking", diffKey, r -> r.getInt("step") == 0);
			res.keepLastOnly(diffKey, "step");
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, key(), Utils.aList("rankChange"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addDoubleColumn("rounded", r -> Utils.round(r.getDouble("rankChangeMean"), .01));
			res.addColumn("unify", r ->	r.concatVals("model") + "-"
					+ (Utils.aList("ba","ws","er").contains(r.get("graph").substring(0, 2)) ? "rand" : r.get("graph")));
			return res;
		}
	}
	
	public static class BotsHeatmapAggregator extends HeatmapAggregator {

		@Override
		public String getName() {
			return prefixName() + "bots-heat";
		}

		@Override
		protected Predicate<Row> initialFilter() {
			return r -> "bots".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("links", r -> Utils.last(r.get("heuristic").split("-")));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "links"));
			res.addColumn("heurPrint", r -> botsHeurFullNamePrint(r.get("heuristic")));
			return res;
		}
	}
	
	public static class EdgesHeatmapAggregator extends HeatmapAggregator {

		@Override
		public String getName() {
			return prefixName() + "edges-heat";
		}
		
		@Override
		protected Predicate<Row> initialFilter(){
			return r -> "edge-add".equals(r.get("heurType")) || "edge-rem".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("label", r ->	r.concatVals("graph", "model"));
			res.addColumn("heurPrint", r ->	edgeHeurFullNamePrint(r.get("heuristic")));
			return res;
		}
	}
	
	public static class EdgesMixedHeatmapAggregator extends EdgesHeatmapAggregator {

		@Override
		public String getName() {
			return prefixName() + "edges-mixed";
		}
		
		@Override
		protected Predicate<Row> initialFilter() {
			return r -> "edge-add".equals(r.get("heurType")) || "edge-rem".equals(r.get("heurType"))
					|| "edge-mix".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addDoubleColumn("rounded", r -> Utils.round(r.getDouble("rankChangeMean"), .01));
			res.addColumn("unify", r ->	r.concatVals("model", "algorithm"));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm"));
			res.addColumn("remove", r -> {
				switch (r.get("heurType")) {
					case "edge-add": return "";
					case "edge-rem": return "Removing " + heurNamePrint(r.get("heuristic"));
					case "edge-mix": return "Removing " + heurNamePrint(r.get("heuristic").split("\\+")[0]);
				}
				return r.get("heuristic");
			});
			res.addColumn("add", r -> {
				switch (r.get("heurType")) {
					case "edge-rem": return "";
					case "edge-add": return "Adding " + heurNamePrint(r.get("heuristic"));
					case "edge-mix": return "Adding " + heurNamePrint(r.get("heuristic").split("\\+")[1]);
				}
				return r.get("heuristic");
			});
			return res;
		}
	}
	
	protected static ExperimentResult expand(Stream<Row> rows, List<String> header, File experimentDir) {
		ExperimentResult er = new ExperimentResult(experimentDir, header, rows);
		er.expand("graph", HidingSourceExperiment.HD_GRAPH);
		er.expand("model", HidingSourceExperiment.HD_MODEL);
		er.expand("steps", HidingSourceExperiment.HD_HIDING_STEPS);
		er.expand("perc", HidingSourceExperiment.HD_EVADER_PERCENTILE);
		er.expand("desc", HidingSourceExperiment.HD_DESC);
		return er;
	}

	
	protected static String heurTypePrint(String heurType) {
		String res = heurType.split("-")[1];
		switch (res) {
			case "add": return "Adding";
			case "rem": return "Removing";
		}
		return Utils.capitalize(res);
	}
	
	protected static String heurNamePrint(String heur) {
		String[] parts = heur.split("-");
		if (parts[0].equals("add") || parts[0].equals("rem")) {
			switch (parts[1]) {
				case "maxdegr": return "max degree";
				case "mindegr": return "min degree";
			}
			return parts[1];
		} else
			return parts[0];
	}
	
	protected static String edgeHeurFullNamePrint(String heur) {
		switch (heur.split("-")[0]) {
			case "add": return "Adding " + heurNamePrint(heur);
			case "rem": return "Removing " + heurNamePrint(heur);
		}
		return Utils.capitalize(heur);
	}
	
	protected static String botsHeurFullNamePrint(String heur) {
		String[] parts = heur.split("-");
		return Utils.capitalize(parts[0]) + (parts.length > 2 ? " " + parts[1] : "");
	}
	
	protected void listResults() {
		java.util.Map<List<String>, Integer> m = new java.util.HashMap<>();
		streamResults().map(res -> Utils.aList(res.getHeader().get(HidingSourceExperiment.HD_GRAPH),
					res.getRows().get(0).get(0), res.getRows().get(0).get(3)))
			.forEach(key -> m.compute(key, (__, x) -> x == null ? 1 : x + 1));
		m.keySet().stream().sorted((l1, l2) -> (l1.get(0) + l1.get(1)).compareTo(l2.get(0) + l2.get(1)))
			.forEach(key -> System.out.println(key + "\t" + m.get(key)));
	}
	
	public static final String ALGS_ORDER = "Degree,Eigenvector,Closeness,Rumor,MonteCarlo,RandomWalk,Betweenness";
	public static final String BOTS_HEUR_ORDER = "Degree,Hub,Random,Degree clique,Hub clique,Random clique";
	public static final String EDGE_HEUR_ORDER = "Adding max degree,Adding min degree,Adding random,"
														+ "Removing max degree,Removing min degree,Removing random";
	protected static Map<String,String> algsColors(){
		Map<String,String> res = new HashMap<>();
		res.put("Degree", "#e41a1c");
		res.put("Eigenvector", "#377eb8");
		res.put("Closeness", "#4daf4a");
		res.put("Rumor", "#984ea3");
		res.put("MonteCarlo", "#ff7f00");
		res.put("RandomWalk", "#eeee33");
		res.put("Betweenness", "#a65628");
		return res;
	}
	
	protected static Map<String,String> botsColors(){
		Map<String,String> res = new HashMap<>();
		res.put("Degree", "#e41a1c");
		res.put("Hub", "#377eb8");
		res.put("Random", "#4daf4a");
		res.put("Degree clique", "#984ea3");
		res.put("Hub clique", "#ff7f00");
		res.put("Random clique", "#eeee33");
		return res;
	}
	
	protected static Map<String,String> edgeColors(){
		Map<String,String> res = new HashMap<>();
		res.put("Adding max degree", "#1b9e77");
		res.put("Adding min degree", "#d95f02");
		res.put("Adding random", "#7570b3");
		res.put("Removing max degree", "#e7298a");
		res.put("Removing min degree", "#66a61e");
		res.put("Removing random", "#e6ab02");
		return res;
	}
}
