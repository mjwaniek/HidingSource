package hidingsrc.srcdetection;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.epidemic.EpidemicModel;

/**
 * Source detection algorithm based on random walks, designed for SI diffusion.
 * 
@article{jain2016fast,
  title={Fast rumor source identification via random walks},
  author={Jain, Alankar and Borkar, Vivek and Garg, Dinesh},
  journal={Social Network Analysis and Mining},
  volume={6},
  number={1},
  pages={62},
  year={2016},
  publisher={Springer}
}
 * 
 * @author Marcin Waniek
 */
public class RandomWalkSourceDetection extends ScoringSourceDetectionAlgorithm {
	
	private EpidemicModel em;
	
	public RandomWalkSourceDetection(EpidemicModel em) {
		this.em = em;
	}

	@Override
	public String getName() {
		return "RandomWalk";
	}

	@Override
	protected void recountScores(Coalition active, Graph g) {
		double[] pts = new double[g.size()];
		double[] newPts = new double[g.size()];
		for (int i : active)
			pts[i] = 1.;
		for (int t = em.getDiffusionRounds() - 1; t >= 0; --t) {
			for (int i : active) {
				newPts[i] = (1. - em.getBasicProbability()) * pts[i];
				for (int j : g.getPreds(i))
					if (active.contains(j))
						newPts[i] += em.getBasicProbability() / g.getOutDegree(i) * pts[j];
			}
			double[] tmp = pts;
			pts = newPts;
			newPts = tmp;
		}
		Coalition valid = new Coalition(active);
		for (int i : active)
			if (valid.contains(i))
				for (int j : active)
					if (g.sp().getDistance(i, j) > em.getDiffusionRounds()) {
						valid.remove(i);
						if (!g.isDirected())
							valid.remove(j);
					}
		for (int i : g.nodes())
			if (active.contains(i))
				scores.put(i, valid.contains(i) ? pts[i] : 0.);
			else
				scores.put(i, Double.NEGATIVE_INFINITY);
	}

	@Override
	public double computeSingleScore(int i, Coalition active, Graph g, Coalition comparison) {
		return getScore(i, active, g);
	}
}
