package hidingsrc.experiment.runners;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import hidingsrc.core.GraphGenerator;
import hidingsrc.core.GraphSupplier;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.ProfileHidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunProfileHidingSource extends RunHidingSource {
	
	private static final int SIZE_FROM = 500;
	private static final int SIZE_TO = 2500;
	private static final int SIZE_BY = 500;
	private static final int DEGREE_FROM = 4;
	private static final int DEGREE_TO = 12;
	private static final int DEGREE_BY = 2;
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		boolean bots = args.length > 1 && "bot".equals(args[1]);
		List<Heuristic> hs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		
		RunHidingSource r = new RunProfileHidingSource();
		
		r.runMany(times, new GraphSupplier("er", (n,d) -> GraphGenerator.generateErdosRenyiGraph(n,d)), hs, steps);
		r.runMany(times, new GraphSupplier("ws", (n,d) -> GraphGenerator.generateSmallWorldGraph(n,d,.25)), hs, steps);
		r.runMany(times, new GraphSupplier("ba", (n,d) -> GraphGenerator.generateBarabasiAlbertGraph(n,d)), hs,steps);
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "profile-hiding-source";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		GraphSupplier gs = (GraphSupplier)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		new ProfileHidingSourceExperiment(getDataPath(gs.getName()), gs, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps,
				EVADERS_NUM, SIZE_FROM, SIZE_TO, SIZE_BY, DEGREE_FROM, DEGREE_TO, DEGREE_BY).perform();
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(
				new BotsBestProfileAggregator(),
				new EdgeBestProfileAggregator(),
				new BeforeBestProfileAggregator(),
				new EdgeComparisonAggregator(),
				new BotsComparisonAggregator(),
				new ExchangeBestProfileAggregator()
			);
	}
	
	public static abstract class BestProfileAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "profile-best" + suffix();
		}
		
		protected abstract String suffix();
		
		protected Predicate<Row> initialFilter(){
			return r -> true;
		}
		
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "size", "degree");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows.filter(initialFilter()), header, experimentDir);
			res.addIntColumn("delta", r -> r.getInt("rankAfter") - r.getInt("rankBefore"));
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, key(), Utils.aList("rankBefore", "delta"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			// keeping only the best heuristics
			List<String> filterKey = key();
			if (filterKey.contains("heuristic")) {
				filterKey.remove("heuristic");
				Map<List<String>, String> bestHeur = new HashMap<>();
				res.groupByKey(filterKey).forEach((k, rs) ->
						bestHeur.put(k, Utils.argmax(rs, r -> r.getDouble("deltaMean")).get("heuristic")));
				res.filter(r -> r.get("heuristic").equals(bestHeur.get(r.getKey(filterKey))));
			}
			
			MathContext mc = new MathContext(3, RoundingMode.UP);
			res.addColumn("rankBeforeRounded", r -> new BigDecimal(r.getDouble("rankBeforeMean"), mc).toPlainString());
			res.addColumn("deltaRounded", r -> new BigDecimal(r.getDouble("deltaMean"), mc).toPlainString());
			res.addColumn("unify", r ->	r.concatVals("model", "algorithm"));
			res.addColumn("unifyClass", r -> r.get("model"));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm"));
			res.forEach(r -> r.updateDouble("deltaMean", x -> Math.max(x, 1.))); // for log scale
			return res;
		}
	}
	
	public static class BotsBestProfileAggregator extends BestProfileAggregator {
		@Override
		protected String suffix() {
			return "-bots";
		}

		@Override
		protected Predicate<Row> initialFilter() {
			return r -> "bots".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("heurPrint", r -> {
				String[] parts = r.get("heuristic").split("-");
				return Utils.capitalize(parts[0]) + (parts.length > 2 ? " " + parts[1] : "");
			});
			return res;
		}
	}
	
	public static class EdgeBestProfileAggregator extends BestProfileAggregator {
		@Override
		protected String suffix() {
			return "-edge";
		}

		@Override
		protected Predicate<Row> initialFilter() {
			return r -> "edge-add".equals(r.get("heurType")) || "edge-rem".equals(r.get("heurType"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("heurPrint", r ->	edgeHeurFullNamePrint(r.get("heuristic")));
			return res;
		}
	}
	
	public static class BeforeBestProfileAggregator extends BestProfileAggregator {

		@Override
		protected String suffix() {
			return "-before";
		}
		
		@Override
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "size", "degree");
		}
	}
	
	public static abstract class ComparisonProfileAggregator extends BestProfileAggregator {

		@Override
		public String getName() {
			return "profile-comp" + suffix();
		}
		
		@Override
		protected Predicate<Row> initialFilter(){
			return r -> (r.getInt("size") == 500 || r.getInt("size") == 2500)
					&& (r.getInt("degree") == 4 || r.getInt("degree") == 12);
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("unify", r ->	r.concatVals("graph", "model", "algorithm"));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm", "size", "degree"));
			res.addColumn("plotBackground", r -> {
				boolean dark = Utils.aList("Degree", "Closeness", "RandomWalk", "Betweenness")
						.contains(r.get("algorithm"));
				if (suffix().equals("-edge"))
					dark = !dark;
				if (r.get("graph").equals("er"))
					dark = !dark;
				return dark ? "gray90" : "white";
			});
			return res;
		}
	}
	
	public static class EdgeComparisonAggregator extends ComparisonProfileAggregator {
		@Override
		protected String suffix() {
			return "-edge";
		}

		@Override
		protected Predicate<Row> initialFilter() {
			return super.initialFilter()
					.and(r -> "edge-add".equals(r.get("heurType")) || "edge-rem".equals(r.get("heurType")));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("heurPrint", r ->	edgeHeurFullNamePrint(r.get("heuristic")));
			res.addColumn("heurShort", r -> {
				String[] parts = r.get("heuristic").split("-");
				return parts[0].substring(0, 1).toUpperCase() + parts[1].substring(0, 3);
			});
			return res;
		}
	}
	
	public static class BotsComparisonAggregator extends ComparisonProfileAggregator {
		@Override
		protected String suffix() {
			return "-bots";
		}

		@Override
		protected Predicate<Row> initialFilter() {
			return super.initialFilter().and(r -> "bots".equals(r.get("heurType")));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("heurPrint", r -> {
				String[] parts = r.get("heuristic").split("-");
				return Utils.capitalize(parts[0]) + (parts.length > 2 ? " " + parts[1] : "");
			});
			res.addColumn("heurShort", r -> {
				String[] parts = r.get("heuristic").split("-");
				return parts[0].substring(0, 1).toUpperCase() + (parts.length > 2 ? "C" : "");
			});
			return res;
		}
	}
	
	public static class ExchangeBestProfileAggregator extends BestProfileAggregator {
		@Override
		protected String suffix() {
			return "-exchange";
		}
		
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurType", "size", "degree");
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			List<String> fKey = key(); 
			fKey.remove("heurType"); 
			fKey.remove("heuristic");
			Map<List<String>, List<Row>> m = res.groupByKey(fKey);
			res.filter(r -> r.get("heurType").equals("bots"));
			res.addDoubleColumn("exchange", botRow -> {
				Row edgeRow = Utils.argmax(m.get(botRow.getKey(fKey)).stream().filter(r -> !r.equals(botRow)),
						r -> r.getDouble("deltaMean"));
				return (edgeRow.getDouble("deltaMean") / 5) / (botRow.getDouble("deltaMean") / 50);
			});
			MathContext mc = new MathContext(3, RoundingMode.UP);
			res.addColumn("exchangeRounded", r -> new BigDecimal(r.getDouble("exchange"), mc).toPlainString());
			return res;
		}
	}
}
