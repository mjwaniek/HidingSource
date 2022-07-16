package hidingsrc.experiment.heuristic.bots;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.core.LimitedMemoryRanking;

/**
 * Hiding the source of diffusion by connecting bots to high-degree node.
 * 
 * @author Marcin Waniek
 */
public class HubBotHeuristic extends BotHeuristic {

	protected Coalition hubs;

	public HubBotHeuristic(int linksToContacts, boolean connectClique) {
		super(linksToContacts, connectClique);
	}

	@Override
	protected String coreName() {
		return "hub";
	}
	
	@Override
	protected void connectBot(Graph ag, int evader, int bot, int step) {
		if (step == 1)
			hubs = new LimitedMemoryRanking<>(contacts, i -> ag.getDegree(i), linksToContacts).stream()
					.collect(Coalition.getCollector());
		hubs.forEach(hub -> ag.addEdge(bot, hub));
	}
}
