package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.utils.Utils;

public class AddRandom extends AddHeuristic {

	@Override
	public String getName() {
		return "add-random";
	}

	@Override
	protected double score(int i, Graph g, int evader, Coalition infected) {
		return Utils.RAND.nextDouble();
	}
}
