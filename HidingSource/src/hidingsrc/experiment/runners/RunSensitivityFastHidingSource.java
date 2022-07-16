package hidingsrc.experiment.runners;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.FastHidingSourceExperiment;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

public class RunSensitivityFastHidingSource extends RunFastHidingSource {

	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int alg = args.length > 3 ? Integer.parseInt(args[3]) % ALGORITHMS.size() : -1;
		int avgDegree = 4;
		
		RunHidingSource r = new RunSensitivityFastHidingSource();
		
		for (int compSize = 1000; compSize <= 9000; compSize += 2000) {
			r.runErdosRenyi(n, avgDegree, times, heurs, steps, alg, compSize);
			r.runSmallWorld(n, avgDegree, .25, times, heurs, steps, alg, compSize);
			r.runBarabasiAlbert(n, avgDegree, times, heurs, steps, alg, compSize);
		}
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "sensitivity-fast-hiding-source";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		int algInd = (int)params[3];
		int compSize = (int)params[4];
		List<ScoringSourceDetectionAlgorithm> algs = algInd >= 0 ? ALGORITHMS.subList(algInd, algInd + 1) : ALGORITHMS;
		new FastHidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, algs, heurs, steps, EVADERS_NUM, false,
						compSize)
				.perform();
	}
	
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new SensitivityFastAggregator());
	}
	
	public static class SensitivityFastAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "sensitivity-fast";
		}

		private List<String> key(){
			return Utils.aList("graph", "model", "algorithm", "heuristic", "heurClass", "step", "compSize");
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			res.addColumn("heurClass", r -> r.get("heurType").split("-")[0]);
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
			res.addColumn("heurPrint", r -> r.get("heurClass").equals("bots")
					? botsHeurFullNamePrint(r.get("heuristic")) : edgeHeurFullNamePrint(r.get("heuristic")));
			res.addColumn("label", r ->	r.concatVals("graph", "model", "algorithm", "heurClass"));
			return res;
		}
	}
	
	protected static ExperimentResult expand(Stream<Row> rows, List<String> header, File experimentDir) {
		ExperimentResult er = RunHidingSource.expand(rows, header, experimentDir);
		er.expand("compSize", FastHidingSourceExperiment.HD_COMP_SIZE);
		return er;
	}
}
