package hidingsrc.experiment.runners;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import hidingsrc.core.Graph;
import hidingsrc.core.GraphGenerator;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.epidemic.LinearThresholdModel;
import hidingsrc.epidemic.SIROneShotModel;
import hidingsrc.experiment.ExperimentAggregator;
import hidingsrc.experiment.Row;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.experiment.performers.HidingSourceExperiment;
import hidingsrc.utils.Utils;

public class RunAltModelHidingSource extends RunHidingSource {
	
	protected static final String GOEL_ETAL_DESC = "goel-etal";
	protected static final double GOEL_ETAL_ALPHA = 2.3;
	protected static final Function<Graph, EpidemicModel> GOEL_ETAL_MODEL_GEN =
			g -> new SIROneShotModel(.5 / g.getAverageDegree());
			
	protected static final String KLEINBERG_DESC = "kleinberg";
	protected static final Function<Graph, EpidemicModel> KLEINBERG_MODEL_GEN = __ -> new LinearThresholdModel();
	
	protected static final String ISLANDS_DESC = "islands";
	protected static final Function<Graph, EpidemicModel> ISLANDS_MODEL_GEN = __ -> EPIDEMIC_MODEL;
	
	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 1;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		boolean bots = args.length > 2 && "bot".equals(args[2]);
		List<Heuristic> heurs = bots ? getBotHeuristics(3) : getEdgeHeuristics(false);
		int steps = bots ? 50 : 5;
		int avgDegree = 4;
		
		RunHidingSource r = new RunAltModelHidingSource();
		
		// Based on Goel, Anderson, Hofman, Watts "The Structural Virality of Online Diffusion"
		r.runScaleFreeConfigurationModel(n, GOEL_ETAL_ALPHA, times, heurs, steps, GOEL_ETAL_MODEL_GEN, GOEL_ETAL_DESC);
		
		// Based on Kleinberg "Cascading behavior in networks: Algorithmic and economic issues"
		r.runScaleFreeConfigurationModel(n, GOEL_ETAL_ALPHA, times, heurs, steps, KLEINBERG_MODEL_GEN, KLEINBERG_DESC);
		r.runErdosRenyi(n, avgDegree, times, heurs, steps, KLEINBERG_MODEL_GEN, KLEINBERG_DESC);
		r.runSmallWorld(n, avgDegree, .25, times, heurs, steps, KLEINBERG_MODEL_GEN, KLEINBERG_DESC);
		r.runBarabasiAlbert(n, avgDegree, times, heurs, steps, KLEINBERG_MODEL_GEN, KLEINBERG_DESC);
		
		// Strong community structure
		for (int i = 0; i < times; ++i)
			r.runSingle(GraphGenerator.generateIslandsNetwork(10, n / 10, avgDegree - 1, 1, 1, 1, false),
					heurs, steps, ISLANDS_MODEL_GEN, ISLANDS_DESC);
		
		r.aggregateAll();
	}
	
	@Override
	public String getDirectoryName() {
		return "altmodel-hiding-source";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		List<Heuristic> heurs = (List<Heuristic>) params[1];
		int steps = (int)params[2];
		Function<Graph, EpidemicModel> modelGen = (Function<Graph, EpidemicModel>)params[3];
		String desc = (String)params[4];
		new HidingSourceExperiment(getDataPath(g), g, modelGen.apply(g), ALGORITHMS, heurs, steps, EVADERS_NUM,
				HidingSourceExperiment.DEF_PERC, true, desc).perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new AltBotsLineAggregator(), new AltEdgesLineAggregator(),
				new AltBotsHeatmapAggregator(), new AltEdgesHeatmapAggregator());
	}
	

	public static class AltBotsLineAggregator extends BotsLineAggregator {
		@Override
		protected String prefixName() {
			return "altmodel-";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return fixModel(super.processEvery(rows, header, experimentDir));
		}
	}
	
	public static class AltBotsHeatmapAggregator extends BotsHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "altmodel-";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return fixModel(super.processEvery(rows, header, experimentDir));
		}
	}
	
	public static class AltEdgesLineAggregator extends EdgesLineAggregator {
		@Override
		protected String prefixName() {
			return "altmodel-";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return fixModel(super.processEvery(rows, header, experimentDir));
		}
	}
	
	public static class AltEdgesHeatmapAggregator extends EdgesHeatmapAggregator {
		@Override
		protected String prefixName() {
			return "altmodel-";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return fixModel(super.processEvery(rows, header, experimentDir));
		}
	}
	
	private static Stream<Row> fixModel(Stream<Row> rows){
		return rows.peek(r -> r.set("model", r.get("model").startsWith("SIR-OneShot") ? "SIR-OneShot" : r.get("model")));
	}
}
