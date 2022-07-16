package hidingsrc.experiment.performers;

import java.util.Collection;
import java.util.List;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.core.Ranking;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.srcdetection.ScoringSourceDetectionAlgorithm;
import hidingsrc.utils.Utils;

public class FastHidingSourceExperiment extends HidingSourceExperiment {
	
	public static final int HD_COMP_SIZE = 6;
	
	private Integer comparisonGroupSize;
	private Coalition comparisonGroup;

	public FastHidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, boolean recordAll, int comparisonGroupSize) {
		super(resultsDirPath, g, model, algs, heurs, hidingSteps, evadersNumber, DEF_PERC, false, DEF_DESC);
		this.comparisonGroup = null;
		this.comparisonGroupSize = comparisonGroupSize;
	}
	
	@Override
	public String getName() {
		return "fasthidingsrc-" + g.getName();
	}
	
	@Override
	protected List<String> getHeader() {
		return Utils.concat(super.getHeader(), Utils.aList(comparisonGroupSize.toString()));
	}
	
	@Override
	protected void preprocess(Coalition evaders, Coalition infected) {
		comparisonGroup = infected.stream().boxed().sorted((i,j) -> Integer.compare(g.getDegree(j), g.getDegree(i)))
				.limit(comparisonGroupSize).collect(Coalition.getCollector());
		comparisonGroup.add(infected.stream().filter(i -> !comparisonGroup.contains(i)).boxed()
				.collect(Coalition.getCollector()).getRandom(comparisonGroupSize));
		comparisonGroup.add(evaders);
	}
	
	@Override
	protected int rankingPosition(ScoringSourceDetectionAlgorithm alg, Graph g, Coalition infected, int evader) {
		return new Ranking<>(comparisonGroup, i -> alg.computeSingleScore(i, infected, g, comparisonGroup))
				.getExAequoPosition(evader, DELTA);
	}
}
