package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.experiment.Experiment;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

/**
 * Experiment with hiding the source of diffusion from source detection algorithms.
 * 
 * @author Marcin Waniek
 */
public class HidingSourceExperiment extends Experiment {

	public static final int HD_GRAPH = 1;
	public static final int HD_MODEL = 2;
	public static final int HD_HIDING_STEPS = 3;
	public static final int HD_EVADER_PERCENTILE = 4;
	public static final int HD_DESC = 5;
	
	public static final int DEF_PERC = 10;
	public static final String DEF_DESC = "standard";
	protected static final int EVADER_MIN_DEGREE = 6;
	protected static final int MIN_INFECTED = 5;
	
	protected static final double DELTA = .000001;
	
	protected Graph g;
	protected EpidemicModel model;
	protected Collection<ScoringSourceDetectionAlgorithm> algorithms;
	protected Collection<Heuristic> heuristics;
	protected Integer hidingSteps;
	protected int evadersNumber;
	protected Integer evaderPercentile;
	protected boolean recordAll;
	protected String desc;
	
	public HidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, int evaderPercentile, boolean recordAll, String desc) {
		super(resultsDirPath);
		this.g = g;
		this.model = model;
		this.algorithms = algs;
		this.heuristics = heurs;
		this.hidingSteps = hidingSteps;
		this.evadersNumber = evadersNumber;
		this.evaderPercentile = evaderPercentile;
		this.recordAll = recordAll;
		this.desc = desc;
	}
	
	public HidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber) {
		this(resultsDirPath, g, model, algs, heurs, hidingSteps, evadersNumber, DEF_PERC, true,
				DEF_DESC);
	}

	@Override
	public String getName() {
		return "hidingsrc-" + g.getName();
	}
	
	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), g.getName(), model.getName(), hidingSteps.toString(),
				evaderPercentile.toString(), desc);
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("algorithm", "evader", "heuristic", "heurType", "step", "ranking");
	}

	@Override
	protected void perform(ExperimentResult res) {
		Coalition evaders = selectPotentialEvaders(g).getRandom(evadersNumber);
		for (int evader : evaders) {
			Coalition infected = generateInfected(g, evader);
			preprocess(evaders, infected);
			Map<ScoringSourceDetectionAlgorithm, Integer> ranksBefore = new HashMap<>();
			for (ScoringSourceDetectionAlgorithm alg : algorithms)
				ranksBefore.put(alg, rankingPosition(alg, g, infected, evader));
			for (Heuristic h : heuristics) {
				g.startRecordingHistory();
				for (ScoringSourceDetectionAlgorithm alg : algorithms)
					res.addRow(alg.getName(), evader, h.getName(), h.getType(), 0, ranksBefore.get(alg));
				Graph ag = g;
				Coalition ainfected = new Coalition(infected);
				int deltaStep = Math.max(hidingSteps / 10, 1);
				for (int step = deltaStep; step <= hidingSteps; step += deltaStep) {
					ag = h.hideEvaderMultipleSteps(ag, evader, ainfected, step, deltaStep);
					if (recordAll || step == hidingSteps)
						for (ScoringSourceDetectionAlgorithm alg : algorithms)
							res.addRow(alg.getName(), evader, h.getName(), h.getType(), step,
									rankingPosition(alg, ag, ainfected, evader));
				}
				g.resetGraph();
			}
		}
	}
	
	protected Coalition selectPotentialEvaders(Graph g) {
		int percentileDegree = g.nodesStream().map(i -> g.getDegree(i)).sorted()
				.skip(g.size() * (100 - evaderPercentile) / 100).findAny().orElse(0);
		return g.nodesStream().boxed().filter(i -> g.getDegree(i) >= Math.max(percentileDegree, EVADER_MIN_DEGREE))
				.collect(Coalition.getCollector());
	}
	
	protected Coalition generateInfected(Graph g, int src) {
		Coalition res = null;
		do {
			res = model.runDiffusion(src, g);
		} while (res.size() < MIN_INFECTED);
		return res;
	}
	
	protected void preprocess(Coalition evaders, Coalition infected) {}
	
	protected int rankingPosition(ScoringSourceDetectionAlgorithm alg, Graph g, Coalition infected, int evader) {
		return alg.getRanking(infected, g).getExAequoPosition(evader, DELTA);
	}
}
