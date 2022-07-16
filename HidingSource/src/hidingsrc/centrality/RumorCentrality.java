package hidingsrc.centrality;

import java.util.Stack;

import hidingsrc.core.BreadthFirstSearch;
import hidingsrc.core.Graph;
import hidingsrc.utils.Ref;

/**
 * Rumor centrality measure, based on Shah and Zaman (2011).
 * 
@article{shah2011rumors,
  title={Rumors in a network: Who's the culprit?},
  author={Shah, Devavrat and Zaman, Tauhid},
  journal={IEEE Transactions on information theory},
  volume={57},
  number={8},
  pages={5163--5181},
  year={2011},
  publisher={IEEE}
}
 * 
 * @author Marcin Waniek
 */
public class RumorCentrality extends Centrality {

	@Override
	public String getName() {
		return "rumor";
	}
	@Override
	public double computeSingleCentrality(int v, Graph g) {
		Ref<Double> res = new Ref<>();
		new BreadthFirstSearch() {
			Stack<Integer> reverseOrder = new Stack<>();
			
			@Override
			public void process(int v) {
				reverseOrder.add(v);
			}
			
			@Override
			public void postProcessRoot(Integer root) {
				int[] subtreeSize = new int[g.size()];
				while (!reverseOrder.isEmpty()) {
					int i = reverseOrder.pop();
					subtreeSize[i] += 1;
					if (parent[i] != null)
						subtreeSize[parent[i]] += subtreeSize[i];
				}
				double acc = 1.;
				for (int i : g.nodes())
					acc = acc * (i+1) / subtreeSize[i];
				res.set(acc);
			}
		}.runSearch(g, v);
		return res.get();
	}
}
