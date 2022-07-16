package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.experiment.heuristic.Heuristic;

/**
 * Hiding the source of diffusion by mixing adding and removing edges.
 * 
 * @author Marcin Waniek
 */
public class MixHeuristic extends Heuristic {

	private RemoveHeuristic remHeur;
	private AddHeuristic addHeur;
	
	public MixHeuristic(RemoveHeuristic remHeur, AddHeuristic addHeur) {
		this.remHeur = remHeur;
		this.addHeur = addHeur;
	}

	@Override
	public String getName() {
		return remHeur.getName() + "+" + addHeur.getName();
	}

	@Override
	public String getType() {
		return "edge-mix";
	}

	@Override
	public Graph hideEvader(Graph g, int evader, Coalition infected, int step) {
		if (!g.isRecordingHistory())
			g.startRecordingHistory();
		if (step % 2 == 0)
			remHeur.hideEvader(g, evader, infected, step);
		else
			addHeur.hideEvader(g, evader, infected, step);
		return g;
	}
}
