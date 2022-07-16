package hidingsrc.experiment.runners;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.DurationHidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunDurationHidingSource extends RunHidingSource {

	private static final int ROUNDS_FROM = 5;
	private static final int ROUNDS_TO = 30;
	private static final int ROUNDS_BY = 5;
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;
		
		RunHidingSource r = new RunDurationHidingSource();

		r.runErdosRenyi(n, avgDegree, times, heurs, steps);
		r.runSmallWorld(n, avgDegree, .25, times, heurs, steps);
		r.runBarabasiAlbert(n, avgDegree, times, heurs, steps);
		
		r.aggregateAll();
	}

	@Override
	public String getDirectoryName() {
		return "duration-hiding-source";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		new DurationHidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps, EVADERS_NUM,
				ROUNDS_FROM, ROUNDS_TO, ROUNDS_BY).perform();
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new DurationLineAggregator());
	}
	
	public static class DurationLineAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "duration-line";
		}

		private List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurClass", "rounds");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			
			res.addRowsDontPrint(res.stream().map(r -> {
				Row nr = new Row(r);
				nr.set("heurType", "before");
				nr.set("rankAfter", r.get("rankBefore"));
				return nr;
			}).collect(Collectors.toList()));
			
			res.forEach(r -> r.update("model", s -> s.substring(0, s.lastIndexOf('-'))));
			res.addColumn("heurClass", r -> {
				switch (r.get("heurType").split("-")[0]) {
					case "bots": return "After adding nodes";
					case "edge": return "After modifying edges";
					case "before": return "Before hiding";
				}
				return r.get("heurType");
			});
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, key(), Utils.aList("rankAfter"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			// keeping only the best heuristics
			List<String> filterKey = key();
			filterKey.remove("heuristic");
			Map<List<String>, String> bestHeur = new HashMap<>();
			res.groupByKey(filterKey).forEach((k, rs) ->
					bestHeur.put(k, Utils.argmax(rs, r -> r.getDouble("rankAfterMean")).get("heuristic")));
			res.filter(r -> r.get("heuristic").equals(bestHeur.get(r.getKey(filterKey))));

			res.addColumn("unify", r ->	r.concatVals("model"));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm"));
			return res;
		}
	}
}
