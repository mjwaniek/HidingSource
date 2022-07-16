package hidingsrc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hidingsrc.utils.ProbabilityDistribution;
import hidingsrc.utils.Utils;

/**
 * Class for generating graphs using different models and modifying them.
 * 
 * @author Marcin Waniek
 */
public class GraphGenerator {
	
	public static Graph combine(List<Graph> gs){
		int n = gs.stream().mapToInt(g -> g.size()).sum();
		String name = gs.stream().map(g -> g.getName()).reduce((s1,s2) -> s1 + "+" + s2).orElse("G");
		Graph res = new Graph(name, n);
		int offset = 0;
		for (Graph g : gs) {
			for (Edge e : g.edges())
				res.addEdge(e.i() + offset, e.j() + offset);
			offset += g.size();
		}
		return res;
	}
	
	public static Graph randomlyDistort(Graph g, double prob){
		Graph res = new Graph(g.getName(), g.size());
		for (int i : g.nodes())
			for (int j : g.nodes())
				if (i < j || (g.isDirected() && i != j)){
					double r = Utils.RAND.nextDouble();
					if ((!g.containsEdge(i, j) && r <= prob) || (g.containsEdge(i, j) && r > prob)) 
						res.addEdge(i, j);
				}
		return res;
	}
	
	public static Graph randomlyRemove(Graph g, double prob){
		Graph res = new Graph(g.getName(), g.size());
		for (int i : g.nodes())
			for (int j : g.nodes())
				if ((i < j || (g.isDirected() && i != j))
						&& g.containsEdge(i, j) && Utils.RAND.nextDouble() > prob) 
					res.addEdge(i, j);
		return res;
	}

	public static Graph randomlyAdd(Graph g, double prob){
		Graph res = new Graph(g.getName(), g.size());
		for (int i : g.nodes())
			for (int j : g.nodes())
				if ((i < j || (g.isDirected() && i != j))
						&& (g.containsEdge(i, j) || Utils.RAND.nextDouble() <= prob)) 
					res.addEdge(i, j);
		return res;
	}

	public static Graph generateCycle(int n) {
		return generateCycle(n, false);
	}
	
	public static Graph generateCycle(int n, boolean directed) {
		Graph res = new Graph("cycle(" + n + ")", n, directed);
		for (int i : res.nodes()) {
			res.addEdge(i, (i + 1) % n);
			if (directed)
				res.addEdge((i + 1) % n, i);
		}
		return res;
	}

	public static Graph generateClique(int n) {
		return generateClique(n, false);
	}
	
	public static Graph generateClique(int n, boolean directed) {
		Graph res = new Graph("clique(" + n + ")", n, directed);
		for (int i : res.nodes())
			for (int j = i + 1; j < n; ++j){
				res.addEdge(i, j);
				if (directed)
					res.addEdge(j, i);
			}
		return res;
	}
	
	public static Graph generateGrid(int width, int height) {
		return generateGrid(width, height, false);
	}
	
	public static Graph generateGrid(int width, int height, boolean directed) {
		Graph res = new Graph("grid(" + width + "," + height + ")", width * height, directed);
		for (int i = 0; i < height; ++i)
			for (int j = 0; j < width; ++j){
				int num = i * width + j;
				if (j + 1 < width) {
					res.addEdge(num, num + 1);
					if (directed)
						res.addEdge(num + 1, num);
				}
				if (i + 1 < height) {
					res.addEdge(num, num + width);
					if (directed)
						res.addEdge(num + width, num);
				}
			}
		return res;
	}
	
/*
@article{barabasi1999emergence,
	title={Emergence of scaling in random networks},
	author={Barab{\'a}si, Albert-L{\'a}szl{\'o} and Albert, R{\'e}ka},
	journal={science},
	volume={286},
	number={5439},
	pages={509--512},
	year={1999},
	publisher={American Association for the Advancement of Science}
}
*/
	public static Graph generateBarabasiAlbertGraph(int n, int avgDegree){
		return generateBarabasiAlbertGraph(n, avgDegree, false);
	}
	
	public static Graph generateBarabasiAlbertGraph(int n, int avgDegree, boolean directed){
		String name = "ba-" + n + "-" + avgDegree;
		if (directed)
			name = "d" + name;
		int m = avgDegree / 2; // how many edges are added with each node
		Graph res = new Graph(name, n, directed);
		ProbabilityDistribution<Integer> pd = new ProbabilityDistribution<>(
				Utils.asList(IntStream.range(0, n).boxed()), i -> (double)res.getDegree(i));
		for (int i = 0; i <= m; ++i)
			for (int j = 0; j < i; ++j) {
				res.addEdge(i, j);
				if (directed)
					res.addEdge(j, i);
			}
		for (int i = m + 1; i < n; ++i) {
			for (int edge = 0; edge < m; ++edge){
				int fi = i;
				int j = pd.drawFiltered(k -> k != fi && !res.containsEdge(fi, k));
				res.addEdge(i, j);
				if (directed)
					res.addEdge(j, i);
			}
			pd.reset();
		}
		return res;
	}
	
/* 
@article{newman2003structure,
	title={The structure and function of complex networks},
	author={Newman, Mark EJ},
	journal={SIAM review},
	volume={45},
	number={2},
	pages={167--256},
	year={2003},
	publisher={SIAM}
}
*/
	public static Graph generateScaleFreeConfigurationModel(int n, double alpha) {
		return generateScaleFreeConfigurationModel(n, alpha, 1, n-1);
	}
	
	public static Graph generateScaleFreeConfigurationModel(int n, double alpha, int kMin, int kMax) {
		while (true) {
			String name = "sfc-" + n + "-" + Math.round(alpha)
					+ (kMin != 1 || kMax != n-1 ? "-" + kMin + "-" + kMax : "");
			Graph res = new Graph(name, n);
			List<Integer> degrees = new ArrayList<>();
			int dSum = 0;
			for (int i = 0; i < n; ++i) {
				int d = powerLaw(alpha, kMin, kMax);
				degrees.add(d);
				dSum += d;
			}
			if (dSum % 2 != 0) {
				int d = degrees.get(0);
				if (degrees.get(0) < kMax)
					degrees.set(0, ++d);
				else
					degrees.set(0, --d);				
			}
			List<Integer> stubs = new ArrayList<>();
			for (int v : res.nodes())
				for (int i = 0; i < degrees.get(v); ++i)
					stubs.add(v);
			int tries = 0;
			while (!stubs.isEmpty() && tries++ < 5) {
				Collections.shuffle(stubs);
				List<Integer> failures = new ArrayList<>();
				for (int i = 0; i < stubs.size(); i += 2)
					if (!res.addEdge(stubs.get(i), stubs.get(i + 1))) {
						failures.add(stubs.get(i));
						failures.add(stubs.get(i + 1));
					}
				stubs = failures;
			}
			res.forceConnectivity();
			if (stubs.isEmpty())
				return res;
		}
	}
	
	private static int powerLaw(double alpha, int kMin, int kMax){
		double norm = 1./(Utils.hurwiczZeta(alpha, kMin, .00001)
						-Utils.hurwiczZeta(alpha, kMax+1, .00001));
		double rng = Utils.RAND.nextDouble();
		double prob = 0.;		
		for (int k = kMin; k <= kMax; ++k) {
			prob += Math.pow((double)k, -alpha) * norm;
			if (rng <= prob)
				return k;			
		}
		return 0;
	}
	
/*
@article{erdds1959random,
	title={On random graphs I.},
	author={Erd{\H{o}}s, Paul and R{\'e}nyi, Alfr{\'e}d},
	journal={Publ. Math. Debrecen},
	volume={6},
	pages={290--297},
	year={1959}
}
*/
	
	public static Graph generateErdosRenyiGraph(int n, Integer avgDegree){
		return generateErdosRenyiGraph(n, avgDegree, false);
	}
	
	public static Graph generateErdosRenyiGraph(int n, Integer avgDegree, boolean directed){
		String name = "er-" + n + "-" + avgDegree;
		double prob = (double) avgDegree / (n - 1);
		if (directed) {
			name = "d" + name;
			prob /= 2.;
		}
		Graph res = new Graph(name, n, directed);
		for (int i : res.nodes())
			for (int j = i + 1; j < n; ++j){
				if (Utils.RAND.nextDouble() <= prob)
					res.addEdge(i, j);
				if (directed && Utils.RAND.nextDouble() <= prob)
					res.addEdge(j, i);
			}
		res.forceConnectivity();
		return res;
	}
	
/*
@article{watts1998collective,
	title={Collective dynamics of small-world networks},
	author={Watts, Duncan J and Strogatz, Steven H},
	journal={nature},
	volume={393},
	number={6684},
	pages={440--442},
	year={1998},
	publisher={Nature Publishing Group}
}
*/

	public static Graph generateSmallWorldGraph(int n, int avgDegree, double beta){
		return generateSmallWorldGraph(n, avgDegree, beta, false);
	}
	
	public static Graph generateSmallWorldGraph(int n, int avgDegree, double beta, boolean directed){
		String name = "ws-" + n + "-" + avgDegree + "-" + Math.round(beta * 100.);
		if (directed)
			name = "d" + name;
		Graph res = new Graph(name, n, directed);
		for (int i : res.nodes())
			for (int j = i + 1; j <= i + avgDegree / 2; ++j) {
				res.addEdge(i, j % n);
				if (directed)
					res.addEdge(j % n, i);
			}
		if (avgDegree < n - 1)
			for (Edge e : res.edgesStream().collect(Collectors.toList()))
				if (Utils.RAND.nextDouble() < beta && res.getOutDegree(e.i()) < res.size() - 1){
					int i = e.i();
					int j = i;
					if (res.getOutDegree(i) > res.size() / 1000)
						j = Utils.getRandom(
								res.nodesStream().filter(k -> k != i && !res.getSuccs(i).contains(k)).boxed(),
								n - res.getOutDegree(i) - 1);
					else
						while (j == i || res.getSuccs(i).contains(j))
							j = Utils.RAND.nextInt(res.size());
					res.removeEdge(e);
					res.addEdge(i, j);
				}
		res.forceConnectivity();
		return res;
	}

/*
@article{prufer1918neuer,
	author = {Pr\"ufer, H},
	year = {1918},
	month = {01},
	title = {Neuer Beweis eines Satzes \"uber Permutationen},
	volume = {27},
	journal = {Archiv der Mathematik und Physik. 3. Reihe}
}
 */
	
	public static Graph generatePruferTree(int n) {
		int[] a = new int[n-2];
		for (int i = 0; i < a.length; ++i)
			a[i] = Utils.RAND.nextInt(n);
		int[] degr = new int[n];
		for (int i = 0; i < degr.length; ++i)
			degr[i] = 1;
		for (int v : a)
			degr[v] += 1;
		Graph res = new Graph("pr-" + n, n);
		for (int v : a)
			for (int w : res.nodes())
				if (degr[w] == 1) {
					res.addEdge(v, w);
					degr[v] -= 1;
					degr[w] -= 1;
					break;
				}
		List<Integer> last = res.nodesStream().filter(i -> degr[i] == 1).boxed().collect(Collectors.toList());	
		res.addEdge(last.get(0), last.get(1));
		return res;
	}
	
/*
@inproceedings{waniek2017construction,
  title={On the construction of covert networks},
  author={Waniek, Marcin and Michalak, Tomasz P and Rahwan, Talal and Wooldridge, Michael},
  booktitle={Proceedings of the 16th Conference on Autonomous Agents and MultiAgent Systems},
  pages={1341--1349},
  year={2017},
  organization={International Foundation for Autonomous Agents and Multiagent Systems}
}
 */
	public static Graph getCaptainNetwork(int n, int leadersNumber, int captGroupSize){
		Graph res = new Graph("capt-" + n + "-" + leadersNumber + "-" + captGroupSize, n);
		Integer iter = 0;
		Integer captGroupNumber = Math.max(leadersNumber, 2);
		
		List<Integer> leaders = new ArrayList<>();
		for (int i = 0; i < leadersNumber; ++i)
			leaders.add(iter++);
		List<List<Integer>> captains = new ArrayList<>();
		for (int i = 0; i < captGroupNumber; ++i){
			List<Integer> group = new ArrayList<>();
			for (int j = 0; j < captGroupSize; ++j)
				group.add(iter++);
			captains.add(group);
		}
		List<Integer> members = new LinkedList<>();
		while (iter < n)
			members.add(iter++);
		
		for (Integer l1 : leaders)
			for (Integer l2 : leaders)
				if (l1 != l2)
					res.addEdge(l1, l2);
		for (int i = 0; i < captGroupNumber; ++i)
			for (Integer c : captains.get(i))
				if (leadersNumber > 1)
					res.addEdge(leaders.get(i), c);
				else
					res.addEdge(leaders.get(0), c);
		for (int i = 0; i < captGroupNumber; ++i)
			for (int j = i+1; j < captGroupNumber; ++j)
				for (Integer ci : captains.get(i))
					for (Integer cj : captains.get(j))
						res.addEdge(ci, cj);
		int j = 0;
		for (Integer a : members){
			for (int i = 0; i < captGroupNumber; ++i)
				res.addEdge(a, captains.get(i).get(j));
			j = (j + 1) % captGroupSize;
		}
		
		return res;
	}

/*
@article{golub2012does,
  title={Does homophily predict consensus times? Testing a model of network structure via a dynamic process},
  author={Golub, Benjamin and Jackson, Matthew O},
  journal={Review of Network Economics},
  volume={11},
  number={3},
  year={2012},
  publisher={De Gruyter}
}
 */
	public static Graph generateIslandsNetwork(String name, int nodes,
			double[] classDist, double[][] classConn, double[][] numAttrMeans, double[][] numAttrSds){
		Graph g = new Graph(name, nodes);
		int[] cls = new int[nodes];
		int cFirst = 0;
		int cLast = 0;
		for (int i = 0; i < classDist.length; ++i) {
			cFirst = cLast;
			cLast += classDist[i] * nodes;
			if (i == classDist.length - 1)
				cLast = nodes;
			for (int v = cFirst; v < cLast; ++v)
				cls[v] = i;
		}
		for (int v = 0; v < nodes; ++v)
			for (int w = v + 1; w < nodes; ++w)
				if (Utils.RAND.nextDouble() < classConn[cls[v]][cls[w]])
					g.addEdge(v, w);
		g.forceConnectivity();
		return g;
	}
	
	public static Graph generateIslandsNetwork(int cNum, int cSize,
			double dSame, double dOther, int attributes, double attrSd, boolean homophily){
		int n = cNum * cSize;
		String name = (homophily ? "isl-" : "islu-") + n;
		double[] cd = new double[cNum];
		for (int i = 0; i < cNum; ++i)
			cd[i] = 1. / cNum;
		double[][] cc = new double[cNum][cNum];
		for (int i = 0; i < cNum; ++i)
			cc[i][i] = dSame / cSize;
		for (int i = 0; i < cNum; ++i)
			for (int j = i + i; j < cNum; ++j)
				cc[i][j] = dOther / cSize;
		
		double[][] ms = new double[attributes][cNum];
		for (int i = 0; i < attributes; ++i)
			for (int j = 0; j < cNum; ++j)
				if (homophily)
					ms[i][j] = Utils.RAND.nextDouble();
				else
					ms[i][j] = .5;
		double[][] sd = new double[attributes][cNum];
		for (int i = 0; i < attributes; ++i)
			for (int j = 0; j < cNum; ++j)
				sd[i][j] = attrSd;
		return generateIslandsNetwork(name, n, cd, cc, ms, sd);
	}
}