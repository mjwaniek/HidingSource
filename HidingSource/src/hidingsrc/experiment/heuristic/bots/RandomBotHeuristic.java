package hidingsrc.experiment.heuristic.bots;

import hidingsrc.core.Graph;

public class RandomBotHeuristic extends BotHeuristic {

	public RandomBotHeuristic(int linksToContacts, boolean connectClique) {
		super(linksToContacts, connectClique);
	}

	@Override
	protected String coreName() {
		return "random";
	}

	@Override
	protected void connectBot(Graph ag, int evader, int bot, int step) {
		contacts.getRandom(linksToContacts).forEach(i -> ag.addEdge(bot, i));
	}
}
