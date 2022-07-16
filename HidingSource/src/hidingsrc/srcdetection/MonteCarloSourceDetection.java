package hidingsrc.srcdetection;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.EpidemicModel;
import hidingsrc.utils.Utils;

/**
 * Monte Carlo source detection algorithm (soft margin version).
 * 
@article{antulov2015identification,
  title={Identification of patient zero in static and temporal networks: Robustness and limitations},
  author={Antulov-Fantulin, Nino and Lan{\v{c}}i{\'c}, Alen and {\v{S}}muc, Tomislav and {\v{S}}tefan{\v{c}}i{\'c}, Hrvoje and {\v{S}}iki{\'c}, Mile},
  journal={Physical review letters},
  volume={114},
  number={24},
  pages={248701},
  year={2015},
  publisher={APS}
}
 *
 * @author Marcin Waniek
 */
public class MonteCarloSourceDetection extends ScoringSourceDetectionAlgorithm {
	
	private static final int SAMPLES = 100;
	private static final double CONVERGENCE_THRESHOLD = .05;

	private EpidemicModel em;
	private Coalition candidates;
	
	public MonteCarloSourceDetection(EpidemicModel em) {
		this.em = em;
		this.candidates = null;
	}

	@Override
	public String getName() {
		return "MonteCarlo";
	}

	@Override
	protected void recountScores(Coalition active, Graph g) {
		if (candidates == null)
			candidates = g.nodesCoalition();
		double[][] jaccs = new double[g.size()][SAMPLES];
		for (int i : candidates)
			for (int sample = 0; sample < SAMPLES; ++sample)
				jaccs[i][sample] = jaccard(active, em.runDiffusion(i, g));
		double a = .5;
		double[] pts = computePoints(jaccs, a);
		do {
			a /= 2.;
			double[] newPts = computePoints(jaccs, a);
			if (converged(newPts, pts))
				break;
			pts = newPts;
		} while (a >= Math.pow(.5, 15));
		for (int i : g.nodes())
			if (candidates.contains(i))
				scores.put(i, pts[i]);
			else
				scores.put(i, Double.NEGATIVE_INFINITY);
		candidates = null;
	}
	
	private double jaccard(Coalition active, Coalition simulated) {
		return (double) active.inplaceIntersect(simulated).count() / active.inplaceAdd(simulated).count(); 
	}
	
	private double[] computePoints(double[][]jaccs, double a) {
		double[] res = new double[jaccs.length];
		for (int i : candidates) {
			for (int sample = 0; sample < SAMPLES; ++sample)
				res[i] += Math.exp(-(jaccs[i][sample] - 1.) * (jaccs[i][sample] - 1.) / (a * a));
			res[i] /= SAMPLES;
		}
		return res;
	}
	
	private boolean converged(double[] newPts, double[] oldPts) {
		int best = Utils.argmax(candidates, i -> newPts[i]);
		return Math.abs(newPts[best] - oldPts[best]) < CONVERGENCE_THRESHOLD;
	}

	@Override
	public double computeSingleScore(int i, Coalition active, Graph g, Coalition comparison) {
		this.candidates = comparison;
		return getScore(i, active, g);
	}
}