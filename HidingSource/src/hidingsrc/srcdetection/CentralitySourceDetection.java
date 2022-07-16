package hidingsrc.srcdetection;

import hidingsrc.centrality.Centrality;
import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.core.LWGraph;
import hidingsrc.utils.Utils;

/**
 * Source detection algorithm for SI diffusion, based on centrality in subgraph induced by infected nodes.
 * 
 * @author Marcin Waniek
 */
public class CentralitySourceDetection extends ScoringSourceDetectionAlgorithm {

	private Centrality c;
	private LWGraph<Integer,Void> ig;
	
	
	public CentralitySourceDetection(Centrality c) {
		this.c = c;
		this.ig = null;
	}

	@Override
	public String getName() {
		return Utils.capitalize(c.getName());
	} 

	@Override
	public double computeSingleScore(int i, Coalition active, Graph g, Coalition comparison) {
		prepare(active, g);
		if (g.size() == active.size())
			return c.computeSingleCentrality(i, g);
		else
			return active.contains(i) ? c.computeSingleCentrality(ig.findNode(i), ig) : Double.NEGATIVE_INFINITY;
	}
	
	@Override
	protected void refreshStructures(Coalition active, Graph g) {
		ig = g.getInducedGraph(active);
	}
}
