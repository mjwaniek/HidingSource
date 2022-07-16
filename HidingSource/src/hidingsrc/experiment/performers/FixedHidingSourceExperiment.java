package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

public class FixedHidingSourceExperiment extends HidingSourceExperiment {
	
	public static final String FIXED_DESC = "fixed";
	
	private int evader;
	private Coalition infected;
	
	public FixedHidingSourceExperiment(String resultsDirPath, Graph g, Collection<ScoringSourceDetectionAlgorithm> algs,
			Collection<Heuristic> heurs, int hidingSteps, int evader, Coalition infected) {
		super(resultsDirPath, g, null, algs, heurs, hidingSteps, 1, 0, true, FIXED_DESC);
		this.evader = evader;
		this.infected = infected;
	}

	@Override
	public String getName() {
		return "fixedhidingsrc-" + g.getName();
	}
	
	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), g.getName(), "fixed", hidingSteps.toString(), evaderPercentile.toString(), desc);
	}

	@Override
	protected void perform(ExperimentResult res) {
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
