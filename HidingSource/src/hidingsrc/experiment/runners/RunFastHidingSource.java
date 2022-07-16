package hidingsrc.experiment.runners;

import java.util.List;

import hidingsrc.centrality.*;
import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.*;
import hidingsrc.experiment.performers.FastHidingSourceExperiment;
import hidingsrc.srcdetection.*;
import hidingsrc.utils.Utils;

/**
 * Running experiments with hiding source of diffusion from source detection algorithms.
 * 
 * @author Marcin Waniek
 */
public class RunFastHidingSource extends RunHidingSource {
	
	protected static final int DEF_COMP_SIZE = 5000;

	protected static final List<ScoringSourceDetectionAlgorithm> ALGORITHMS = Utils.aList(
		new CentralitySourceDetection(new DegreeCentrality()),
		new CentralitySourceDetection(new EigenvectorCentrality(.00001)),
		new CentralitySourceDetection(new ClosenessCentrality()),
		new CentralitySourceDetection(new RumorCentrality()),
		new MonteCarloSourceDetection(EPIDEMIC_MODEL));

	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int alg = args.length > 3 ? Integer.parseInt(args[3]) % ALGORITHMS.size() : -1;
		int avgDegree = 4;
		
		RunHidingSource r = new RunFastHidingSource();
		
		r.runErdosRenyi(n, avgDegree, times, heurs, steps, alg);
		r.runSmallWorld(n, avgDegree, .25, times, heurs, steps, alg);
		r.runBarabasiAlbert(n, avgDegree, times, heurs, steps, alg);
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "fast-hiding-source";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		int algInd = (int)params[3];
		List<ScoringSourceDetectionAlgorithm> algs = algInd >= 0 ? ALGORITHMS.subList(algInd, algInd + 1) : ALGORITHMS;
		new FastHidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, algs, heurs, steps, EVADERS_NUM, true,
						DEF_COMP_SIZE)
				.perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(
				new FastBeforeBarsAggregator(),
				new LargeBotsLineAggregator(),
				new LargeBotsHeatmapAggregator(),
				new LargeEdgesLineAggregator(),
				new LargeEdgesHeatmapAggregator()
			);
	}

	public static class FastBeforeBarsAggregator extends BeforeBarsAggregator {
		@Override
		public String getName() {
			return "large-before-bars";
		}
	}
	
	public static class LargeBotsLineAggregator extends BotsLineAggregator {
		@Override
		protected String prefixName() {
			return "large-";
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("unify", r -> r.concatVals("model", "algorithm")
					+ ("ba".equals(r.get("graph").substring(0, 2)) ? "ex" :
						Utils.aList("ws", "er").contains(r.get("graph").substring(0, 2)) ? "st" : r.get("graph")));
			return res;
		}
	}
	
	public static class LargeBotsHeatmapAggregator extends BotsHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "large-";
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("unify", r -> r.concatVals("graph", "model"));
			return res;
		}
	}
	
	public static class LargeEdgesLineAggregator extends EdgesLineAggregator {
		@Override
		protected String prefixName() {
			return "large-";
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("unify", r -> r.concatVals("model", "algorithm")
					+ ("ba".equals(r.get("graph").substring(0, 2)) ? "ex" :
						Utils.aList("ws", "er").contains(r.get("graph").substring(0, 2)) ? "st" : r.get("graph")));
			return res;
		}
	}
	
	public static class LargeEdgesHeatmapAggregator extends EdgesHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "large-";
		}

		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("unify", r -> r.concatVals("graph", "model"));
			return res;
		}
	}
}
