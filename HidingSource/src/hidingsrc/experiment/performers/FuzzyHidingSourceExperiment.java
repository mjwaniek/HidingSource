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

public class FuzzyHidingSourceExperiment extends HidingSourceExperiment {
	
	public static final String FUZZY_DESC = "fuzzy";
	
	private int knowledgeFrom;
	private int knowledgeTo;
	private int knowledgeBy;

	public FuzzyHidingSourceExperiment(String resultsDirPath, Graph g, EpidemicModel model,
			Collection<ScoringSourceDetectionAlgorithm> algs, Collection<Heuristic> heurs, int hidingSteps,
			int evadersNumber, int knowledgeFrom, int knowledgeTo, int knowledgeBy) {
		super(resultsDirPath, g, model, algs, heurs, hidingSteps, evadersNumber, DEF_PERC, false, FUZZY_DESC);
		this.knowledgeFrom = knowledgeFrom;
		this.knowledgeTo = knowledgeTo;
		this.knowledgeBy = knowledgeBy;
	}

	@Override
	public String getName() {
		return "fuzzyhidingsrc-" + g.getName();
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("algorithm", "evader", "heuristic", "heurType", "knowledge", "rankBefore", "rankAfter");
	}

	@Override
	protected void perform(ExperimentResult res) {
		Coalition evaders = selectPotentialEvaders(g).getRandom(evadersNumber);
		for (int evader : evaders)
			for (int knowledge = knowledgeFrom; knowledge >= knowledgeTo; knowledge -= knowledgeBy){
				double visibleProb = knowledge / 100.;
				Coalition infected = generateInfected(g, evader);
				preprocess(evaders, infected);
				Graph beforeView = generateView(visibleProb, g);
				Map<ScoringSourceDetectionAlgorithm, Integer> ranksBefore = new HashMap<>();
				for (ScoringSourceDetectionAlgorithm alg : algorithms)
					ranksBefore.put(alg, alg.getRanking(infected, beforeView).getExAequoPosition(evader, DELTA));
				for (Heuristic h : heuristics) {
					g.startRecordingHistory();
					Coalition ainfected = new Coalition(infected);
					Graph ag = h.hideEvaderMultipleSteps(g, evader, ainfected, hidingSteps, hidingSteps);
					Graph heurView = generateView(visibleProb, ag);
					Coalition infView = generateView(visibleProb, ainfected);
					for (ScoringSourceDetectionAlgorithm alg : algorithms)
						res.addRow(alg.getName(), evader, h.getName(), h.getType(), knowledge,
								ranksBefore.get(alg), rankingPosition(alg, heurView, infView, evader));
					g.resetGraph();
				}
			}
	}
	
	protected Graph generateView(double visibleProb, Graph g) {
		Graph view = new Graph(g);
		g.edgesStream().filter(__ -> Utils.RAND.nextDouble() > visibleProb).forEach(e -> view.removeEdge(e));
		return view;
	}
	
	protected Coalition generateView(double visibleProb, Coalition infected) {
		return infected.stream().filter(__ -> Utils.RAND.nextDouble() <= visibleProb).boxed()
				.collect(Coalition.getCollector());
	}
}
