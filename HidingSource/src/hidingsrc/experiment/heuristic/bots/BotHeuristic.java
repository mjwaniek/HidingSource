package hidingsrc.experiment.heuristic.bots;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.experiment.heuristic.Heuristic;

/**
 * Hiding the source of diffusion by adding bots to the network.
 * 
 * @author Marcin Waniek
 */
public abstract class BotHeuristic extends Heuristic {
	
	protected Coalition bots;
	protected Coalition contacts;
	protected int linksToContacts;
	private boolean connectClique;
		
	public BotHeuristic(int linksToContacts, boolean connectClique) {
		this.bots = null;
		this.contacts = null;
		this.linksToContacts = linksToContacts;
		this.connectClique = connectClique;
	}

	@Override
	public String getType() {
		return "bots";
	}
	
	@Override
	public String getName() {
		return coreName() + (connectClique ? "-clique" : "") + "-" + linksToContacts;
	}
	
	protected abstract String coreName();
	
	@Override
	public Graph hideEvader(Graph g, int evader, Coalition infected, int step) {
		return hideEvaderMultipleSteps(g, evader, infected, step, 1);
	}
	
	@Override
	public Graph hideEvaderMultipleSteps(Graph g, int evader, Coalition infected, int lastStep, int k) {
		Graph ag = g.addNodes(k);
		for (int j = 0; j < k; ++j) {
			int step = lastStep - k + 1 + j;
			if (step == 1) {
				bots = new Coalition();
				contacts = getContacts(g, evader, infected);
			}
			int newBot = g.size() + j;	
			if (connectClique)
				bots.forEach(v -> ag.addEdge(v, newBot)); 
			bots.add(newBot);
			infected.add(newBot);
			connectBot(ag, evader, newBot, step);
		}	
		return ag;
	}
	
	protected Coalition getContacts(Graph g, int evader, Coalition infected) {
		return new Coalition(infected).remove(evader);
	}
	
	protected abstract void connectBot(Graph ag, int evader, int bot, int step);
}
