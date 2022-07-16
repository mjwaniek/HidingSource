package hidingsrc.experiment.runners;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.HidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunSensitivityHidingSource extends RunHidingSource {
	
	private static final String DESC_PERCENTILE = "percentile";
	private static final String DESC_WS_REWIRE = "wsrewire";

	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;

		RunHidingSource r = new RunSensitivityHidingSource();
		
		// Top degree percentile from which the evaders are selected
		for (int perc = 10; perc <= 90; perc += 20) {
			r.runErdosRenyi(n, avgDegree, times, heurs, steps, perc, DESC_PERCENTILE);
			r.runSmallWorld(n, avgDegree, .25, times, heurs, steps, perc, DESC_PERCENTILE);
			r.runBarabasiAlbert(n, avgDegree, times, heurs, steps, perc, DESC_PERCENTILE);
		}
		
		// Watts-Strogatz rewire parameter
		for (double beta = .05; beta <= .5; beta += .05)
			r.runSmallWorld(n, avgDegree, beta, times, heurs, steps, HidingSourceExperiment.DEF_PERC, DESC_WS_REWIRE);
		
		r.aggregateAll();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		int perc = (int)params[3];
		String desc = (String)params[4];
		new HidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps, EVADERS_NUM,
				perc, false, desc).perform();
	}
	
	@Override
	public String getDirectoryName() {
		return "sensitivity-hiding-source";
	}
	
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new PercentileAggregator(), new RewireAggregator());
	}
	
	public static abstract class SensitivityAggregator extends ExperimentAggregator {

		protected abstract List<String> key();
		
		protected abstract boolean filterRes(ExperimentResult res);
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			if (!filterRes(res))
				return Stream.empty();
			res.addColumn("heurClass", r -> r.get("heurType").split("-")[0]);
			List<String> diffKey = key();
			diffKey.add("evader");
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
			res.addColumn("heurPrint", r -> r.get("heurClass").equals("bots")
					? botsHeurFullNamePrint(r.get("heuristic")) : edgeHeurFullNamePrint(r.get("heuristic")));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm", "heurClass"));
			return res;
		}
	}
	
	public static class PercentileAggregator extends SensitivityAggregator {
		
		@Override
		public String getName() {
			return "sensitivity-percentile";
		}

		@Override
		protected boolean filterRes(ExperimentResult res) {
			return res.getHeader().get(HidingSourceExperiment.HD_DESC).equals(DESC_PERCENTILE);
		}

		@Override
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurClass", "perc");
		}
	}
	
	public static class RewireAggregator extends SensitivityAggregator {
		
		@Override
		public String getName() {
			return "sensitivity-wsrewire";
		}

		@Override
		protected boolean filterRes(ExperimentResult res) {
			return res.getHeader().get(HidingSourceExperiment.HD_DESC).equals(DESC_WS_REWIRE);
		}

		@Override
		protected List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurClass");
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addDoubleColumn("beta", r -> Integer.parseInt(r.get("graph").split("-")[3]) / 100.);
			res.addColumn("label", r ->	r.concatVals("model", "algorithm", "heurClass"));
			return res;
		}
	}
}
