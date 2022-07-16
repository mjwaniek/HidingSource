package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.core.GraphSupplier;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.experiment.ExperimentResult;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

public class ProfileHidingSourceExperiment extends HidingSourceExperiment {

	public static final String PROFILE_DESC = "profile";
	
	private GraphSupplier gs;
	private int sizeFrom;
	private int sizeTo;
	private int sizeBy;
	private int degreeFrom;
	private int degreeTo;
	private int degreeBy;
	
	public ProfileHidingSourceExperiment(String resultsDirPath, GraphSupplier gs, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, int sizeFrom, int sizeTo, int sizeBy, int degreeFrom, int degreeTo, int degreeBy) {
		super(resultsDirPath, null, model, algs, heurs, hidingSteps, evadersNumber);
		this.desc = PROFILE_DESC;
		this.gs = gs;
		this.sizeFrom = sizeFrom;
		this.sizeTo = sizeTo;
		this.sizeBy = sizeBy;
		this.degreeFrom = degreeFrom;
		this.degreeTo = degreeTo;
		this.degreeBy = degreeBy;
	}

	@Override
	public String getName() {
		return "profhidingsrc-" + gs.getName();
	}
	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), gs.getName(), model.getName(), hidingSteps.toString(),
				evaderPercentile.toString(), desc);
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("algorithm", "evader", "heuristic", "heurType", "size", "degree", "rankBefore", "rankAfter");
	}
	
	@Override
	protected void perform(ExperimentResult res) {
		for (int size = sizeFrom; size <= sizeTo; size += sizeBy)
			for (int degree = degreeFrom; degree <= degreeTo; degree += degreeBy) {
				Graph g = gs.generate(size, degree);
				Coalition evaders = selectPotentialEvaders(g).getRandom(evadersNumber);
				for (int evader : evaders) {
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
							res.addRow(alg.getName(), evader, h.getName(), h.getType(), size, degree,
									ranksBefore.get(alg), rankingPosition(alg, ag, ainfected, evader));
						g.resetGraph();
					}	
				}
			}
	}
}
