package hidingsrc.experiment.runners;

import java.util.List;

import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.HidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunAnyHidingSource extends RunHidingSource {

	private static final String DESC_ANY = "any";
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;

		RunHidingSource r = new RunAnyHidingSource();
		
		r.runErdosRenyi(n, avgDegree, times, heurs, steps);
		r.runSmallWorld(n, avgDegree, .25, times, heurs, steps);
		r.runBarabasiAlbert(n, avgDegree, times, heurs, steps);
		
		r.aggregateAll();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		new HidingSourceExperiment(getDataPath(g), g, EPIDEMIC_MODEL, ALGORITHMS, heurs, steps, EVADERS_NUM, 100, false, DESC_ANY)
				.perform();
	}

	@Override
	public String getDirectoryName() {
		return "any-hiding-source";
	}
	
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new AnyBeforeBarsAggregator(), new LargeBotsLineAggregator(), new LargeBotsHeatmapAggregator(),
				new LargeEdgesLineAggregator(), new LargeEdgesHeatmapAggregator());
	}
	
	public static class AnyBeforeBarsAggregator extends BeforeBarsAggregator {
		@Override
		public String getName() {
			return "any-before-bars";
		}
	}
	
	public static class LargeBotsLineAggregator extends BotsLineAggregator {
		@Override
		protected String prefixName() {
			return "any-";
		}
	}
	
	public static class LargeBotsHeatmapAggregator extends BotsHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "any-";
		}
	}
	
	public static class LargeEdgesLineAggregator extends EdgesLineAggregator {
		@Override
		protected String prefixName() {
			return "any-";
		}
	}
	
	public static class LargeEdgesHeatmapAggregator extends EdgesHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "any-";
		}
	}
}
