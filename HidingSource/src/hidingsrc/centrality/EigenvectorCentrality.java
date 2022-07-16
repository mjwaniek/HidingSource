package hidingsrc.centrality;

import hidingsrc.core.Graph;
import hidingsrc.core.MonteCarloAlgorithm;

/**
 * Eigenvector centrality measure - eigenvector entry of the greatest eigenvalue.
 * 
 * @author Marcin Waniek
 */
public class EigenvectorCentrality extends Centrality {
	private Double precision;

	public EigenvectorCentrality(Double precision) {
		this.precision = precision;
	}

	@Override
	public String getName() {
		return "eigenvector";
	}

	@Override
	protected void recountCentrality() {
		new MonteCarloAlgorithm() {
			double ev[] = new double[g.size()];
			
			public double getPrecision() {
				return precision;
			}
			
			protected void preProcess() {
				for (int v : g.nodes())
					ev[v] = 1.;
			}
			
			@Override
			protected void singleMCIteration() {
				double[] tmp = new double[g.size()];			
				for (int v : g.nodes())
					for (Integer w : g.getSuccs(v))
						tmp[v] += ev[w];	
				double norm = 0.;
				for (int v : g.nodes())
					norm += tmp[v] * tmp[v];
				norm = Math.sqrt(norm);
				for (int v : g.nodes())
					ev[v] = tmp[v] / norm;
			}

			@Override
			protected double getControlSum(int iter) {
				double sum = 0;
				for (int v : g.nodes())
					sum += ev[v];
				return sum;
			}
			
			@Override
			protected void postProcess() {
				for (int v : g.nodes())
					values.put(v, ev[v]);
			}
		}.runProcess();
	}

	@Override
	public double computeSingleCentrality(int v, Graph g) {
		return getCentrality(v, g);
	}
}
