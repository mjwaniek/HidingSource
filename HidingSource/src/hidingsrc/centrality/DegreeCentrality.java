package hidingsrc.centrality;

import hidingsrc.core.Graph;

/**
 * Degree centrality measure - number of neighbours.
 * 
 * @author Marcin Waniek
 */
public class DegreeCentrality extends Centrality {
	
	@Override
	public String getName() {
		return "degree";
	}

	@Override
	public double computeSingleCentrality(int v, Graph g) {
		if (g.isDirected())
			return (double)g.getDegree(v) / (2. * (g.size() - 1));
		else
			return (double)g.getDegree(v) / (g.size() - 1);
	}
}
