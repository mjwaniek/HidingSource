package hidingsrc.experiment.runners;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.DistanceHidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunDistanceHidingSource extends RunHidingSource {

	private static final int MAX_DISTANCE = 5;
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> hs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;
		
		RunHidingSource r = new RunDistanceHidingSource();
		
		r.runErdosRenyi(n, avgDegree, times, hs, steps);
		r.runSmallWorld(n, avgDegree, .25, times, hs, steps);
		r.runBarabasiAlbert(n, avgDegree, times, hs, steps);
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "distance-hiding-source";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		new DistanceHidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps, EVADERS_NUM,
				MAX_DISTANCE).perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new DistanceLineAggregator());
	}
	
	public static class DistanceLineAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "distance-line";
		}

		private List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurClass", "distance");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			res.addColumn("heurClass", r -> r.get("heurType").split("-")[0]);
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, key(), Utils.aList("rankChange"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			// keeping only the best heuristics
			Map<List<String>, String> bestHeur = new HashMap<>();
			List<String> filterKey = key();
			filterKey.remove("heuristic");
			filterKey.remove("distance");
			res.groupByKey(filterKey).forEach((k, rs) -> bestHeur.put(k,
					Utils.argmax(rs.stream().filter(r -> r.getInt("distance") == 0), r -> r.getDouble("rankChangeMean"))
							.get("heuristic")));
			res.filter(r -> r.get("heuristic").equals(bestHeur.get(r.getKey(filterKey))));

			res.addColumn("label", r ->	r.concatVals("graph", "model", "heurClass"));
			return res;
		}
	}
}
