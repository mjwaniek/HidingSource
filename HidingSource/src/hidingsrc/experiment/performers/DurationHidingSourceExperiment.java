package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

public class DurationHidingSourceExperiment extends HidingSourceExperiment {

	public static final String DURATION_DESC = "duration";
	
	private int roundsFrom;
	private int roundsTo;
	private int roundsBy;
	
	public DurationHidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, int roundsFrom, int roundsTo, int roundsBy) {
		super(resultsDirPath, g, model, algs, heurs, hidingSteps, evadersNumber);
		this.desc = DURATION_DESC;
		this.roundsFrom = roundsFrom;
		this.roundsTo = roundsTo;
		this.roundsBy = roundsBy;
	}

	@Override
	public String getName() {
		return "durhidingsrc-" + g.getName();
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("algorithm", "evader", "heuristic", "heurType", "rounds", "rankBefore", "rankAfter");
	}
	
	@Override
	protected void perform(ExperimentResult res) {
		Coalition evaders = selectPotentialEvaders(g).getRandom(evadersNumber);
		for (int evader : evaders)
			for (int rounds = roundsFrom; rounds <= roundsTo; rounds += roundsBy){
				model.setDiffusionRounds(rounds);
				Coalition infected = generateInfected(g, evader);
				preprocess(evaders, infected);
				Map<ScoringSourceDetectionAlgorithm, Integer> ranksBefore = new HashMap<>();
				for (ScoringSourceDetectionAlgorithm alg : algorithms)
					ranksBefore.put(alg, alg.getRanking(infected, g).getExAequoPosition(evader, DELTA));
				for (Heuristic h : heuristics) {
					g.startRecordingHistory();
					Coalition ainfected = new Coalition(infected);
					Graph ag = h.hideEvaderMultipleSteps(g, evader, ainfected, hidingSteps, hidingSteps);
					for (ScoringSourceDetectionAlgorithm alg : algorithms)
						res.addRow(alg.getName(), evader, h.getName(), h.getType(), rounds,
								ranksBefore.get(alg), rankingPosition(alg, ag, ainfected, evader));
					g.resetGraph();
				}	
			}
	}
}
