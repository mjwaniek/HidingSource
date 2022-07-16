package hidingsrc.experiment.heuristic.bots;

import java.util.List;

import hidingsrc.core.Graph;
import hidingsrc.core.Ranking;

public class DegreeBotHeuristic extends BotHeuristic {

	private List<Integer> rank;
	
	public DegreeBotHeuristic(int linksToContacts, boolean connectClique) {
		super(linksToContacts, connectClique);
		this.rank = null;
	}

	@Override
	protected String coreName() {
		return "degree";
	}

	@Override
	protected void connectBot(Graph ag, int evader, int bot, int step) {
		if (step == 1)
			rank = computeContactsRank(ag);
		for (int i = 0; i < linksToContacts; ++i) {
			if (rank.isEmpty())
				rank = computeContactsRank(ag);
			ag.addEdge(bot, rank.remove(0));
		}
	}
	
	private List<Integer> computeContactsRank(Graph ag){
		return new Ranking<>(contacts, i -> ag.getDegree(i)).getList();
	}
}
