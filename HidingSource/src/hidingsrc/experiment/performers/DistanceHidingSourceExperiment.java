package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

/**
 * Experiment with the effects of hiding from source detection algorithms on nearby nodes.
 * 
 * @author Marcin Waniek
 */
public class DistanceHidingSourceExperiment extends HidingSourceExperiment {

	public static final String DISTANCE_DESC = "distance";
	
	private int maxDistance;
	
	public DistanceHidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, int maxDistance) {
		super(resultsDirPath, g, model, algs, heurs, hidingSteps, evadersNumber, DEF_PERC, false, DISTANCE_DESC);
		this.maxDistance = maxDistance;
	}

	@Override
	public String getName() {
		return "distancehidingsrc-" + g.getName();
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("algorithm", "evader", "heuristic", "heurType", "node", "distance", "rankChange");
	}

	@Override
	protected void perform(ExperimentResult res) {
		Coalition evaders = selectPotentialEvaders(g).getRandom(evadersNumber);
		for (int evader : evaders) {
			Coalition infected = generateInfected(g, evader);
			preprocess(evaders, infected);
			Map<ScoringSourceDetectionAlgorithm, Integer> rankBefore = new HashMap<>();
			for (ScoringSourceDetectionAlgorithm alg : algorithms) {
				rankBefore.put(alg, rankingPosition(alg, g, infected, evader));
			}
			Map<Integer, Integer> nearbyNodes = g.nodesStream()
					.filter(i -> infected.contains(i) && g.sp().getDistance(evader, i) <= maxDistance)
					.boxed().collect(Collectors.toMap(i -> i, i -> (int)g.sp().getDistance(evader, i)));
			for (Heuristic h : heuristics) {
				g.startRecordingHistory();
				for (int i : nearbyNodes.keySet()) {
					Coalition ainfected = new Coalition(infected);
					Graph ag = h.hideEvaderMultipleSteps(g, i, ainfected, hidingSteps, hidingSteps); 
					for (ScoringSourceDetectionAlgorithm alg : algorithms)
							res.addRow(alg.getName(), evader, h.getName(), h.getType(), i, nearbyNodes.get(i),
									rankingPosition(alg, ag, ainfected, evader) - rankBefore.get(alg));
					g.resetGraph();
				}
			}
		}
	}
}
