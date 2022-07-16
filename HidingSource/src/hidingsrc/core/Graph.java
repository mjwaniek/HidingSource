package hidingsrc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Representation of a graph.
 * 
 * @author Marcin Waniek
 */
public class Graph {
	
	private String name;
	private int n;
	private int m;
	private boolean directed;
	
	private ArrayList<Coalition> succs;
	private ArrayList<Coalition> preds;
	
	private Stack<Change> history;
	private ShortestPaths shortestPaths;
	private Collection<GraphChangeListener> listeners;
	
	public Graph(String name, int n, boolean directed){
		this.name = name;
		this.n = n;
		this.m = 0;
		this.directed = directed;
		this.history = null;
		
		this.succs = new ArrayList<>();
		for(int i = 0; i < n; ++i)
			this.succs.add(new Coalition());
		if (directed){
			this.preds = new ArrayList<>();
			for(int i = 0; i < n; ++i)
				this.preds.add(new Coalition());
		} else
			this.preds = null;
		
		this.shortestPaths = null;
		this.listeners = new ArrayList<>();
	}
	
	public Graph(String name, int n){
		this(name, n, false);
	}
	
	public Graph(Graph g){
		this(g.name, g.n, g.directed);
		for (int i : g.nodes())
			for (Integer j : g.getSuccs(i))
				addEdge(i, j);		
	}

	public void subscribe(GraphChangeListener listener){
		unsubscribe(listener);
		listeners.add(listener);
	}
	
	public void unsubscribe(GraphChangeListener listener){
		listeners.remove(listener);
	}
	
	protected void notifyListenersAdd(Edge e){
		for (GraphChangeListener listener : listeners)
			listener.notifyAdd(this, e);
	}
	
	protected void notifyListenersRemove(Edge e){
		for (GraphChangeListener listener : listeners)
			listener.notifyRemove(this, e);
	}
	
	protected void notifyListenersOther(Edge e){
		for (GraphChangeListener listener : listeners)
			listener.notifyOther(this, e);
	}
	
	protected void notifyListenersReset(){
		for (GraphChangeListener listener : listeners)
			listener.notifyReset(this);
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int size(){
		return n;
	}
	
	public Integer edgesCount() {
		return m;
	}
	
	public Long nonEdgesCount() {
		if (isDirected())
			return (long)n * (n-1) - m;
		else
			return (long)n * (n-1) / 2 - m;
	}
	
	public Boolean isDirected(){
		return directed;
	}
	
	public Edge e(int i, int j) {
		return new Edge(i, j, directed);
	}
	
	public boolean addEdge(int i, int j){
		if (i != j && !containsEdge(i, j)){
			performAddEdge(i, j);
			if (history != null)
				history.push(new Addition(i, j));
			notifyListenersAdd(new Edge(i, j, directed));
			return true;
		} else
			return false;
	}
	
	public boolean addEdge(Edge e){
		return addEdge(e.i(), e.j());
	}
	
	public boolean removeEdge(int i, int j){
		if (i != j && containsEdge(i, j)){
			performRemoveEdge(i, j);
			if (history != null)
				history.push(new Removal(i, j));
			notifyListenersRemove(new Edge(i, j, directed));
			return true;
		} else
			return false;
	}
	
	public boolean removeEdge(Edge e){
		return removeEdge(e.i(), e.j());
	}
	
	public boolean swapEdge(int i, int j){
		if (containsEdge(i, j))
			return removeEdge(i, j);
		else
			return addEdge(i, j);
	}
	
	public boolean swapEdge(Edge e){
		return swapEdge(e.i(), e.j());
	}
	
	protected void performAddEdge(int i, int j){
		succs.get(i).add(j);
		if (directed)
			preds.get(j).add(i);
		else
			succs.get(j).add(i);
		++m;
	}
	
	protected void performRemoveEdge(int i, int j){
		succs.get(i).remove(j);
		if (directed)
			preds.get(j).remove(i);
		else
			succs.get(j).remove(i);
		--m;
	}
	
	public void disconnectNode(int i) {
		for (int j : new Coalition(getSuccs(i)))
			removeEdge(i, j);
		if (directed)
			for (int j : new Coalition(getPreds(i)))
				removeEdge(j, i);
	}
	
	public boolean containsEdge(int i, int j){
		return i < n && j < n && getSuccs(i).contains(j);
	}
	
	public boolean containsEdge(Edge e){
		return containsEdge(e.i(), e.j());
	}
	
	public Graph addNodes(int k) {
		Graph res = new Graph(name, n + k, directed);
		edgesStream().forEach(e -> res.addEdge(e));
		return res;
	}
	
	public Coalition getSuccs(int i){
		return succs.get(i);
	}
	
	public Coalition getPreds(int i){
		if (directed)
			return preds.get(i);
		else
			return getSuccs(i);
	}
	
	public Coalition getNeighs(int i){
		if (directed)
			return Coalition.add(getSuccs(i), getPreds(i));
		else
			return succs.get(i);
	}
	
	public int getOutDegree(int i){
		return getSuccs(i).size();
	}
	
	public int getInDegree(int i){
		return getPreds(i).size();
	}

	public int getDegree(int i){
		return getOutDegree(i) + (directed ? getInDegree(i) : 0);
	}
	
	public Iterable<Integer> nodes(){
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return nodesStream().iterator();
			}
		};
	}
	
	public IntStream nodesStream(){
		return IntStream.range(0, size());
	}
	
	public Coalition nodesCoalition(){
		return Coalition.getFull(n);
	}
	
	public Iterable<Edge> edges(){
		return new Iterable<Edge>() {
			@Override
			public Iterator<Edge> iterator() {
				return new EdgesIterator();
			}
		};
	}
	
	public Stream<Edge> edgesStream(){
		return StreamSupport.stream(edges().spliterator(), false);
	}
	
	public ShortestPaths sp() {
		if (shortestPaths == null)
			shortestPaths = ShortestPaths.construct(this);
		return shortestPaths;
	}
	
	public void startRecordingHistory(){
		history = new Stack<>();
	}
	
	public void stopRecordingHistory(){
		history = null;
	}
	
	public boolean isRecordingHistory(){
		return history != null;
	}
	
	public Edge getLastChange() {
		return history.peek().getEdge();
	}
	
	public Stream<Edge> getChanges() {
		return history.stream().map(c -> c.getEdge());
	}
	
	public void resetGraph(){
		if (history != null){
			while (!history.empty())
				history.pop().revert();
			notifyListenersReset();
		}
	}
	
	public void revertChanges(int k){
		if (history != null){
			for (int i = 0; i < k; ++i)
				if (!history.empty())
					history.pop().revert();
		}
	}
	
	public int historySize(){
		return history == null ? 0 : history.size();
	}
	
	public List<Coalition> getConnectedComponents(){
		return getConnectedComponents(null);
	}
	
	public List<Coalition> getConnectedComponents(Coalition allowed){
		List<Coalition> res = new ArrayList<>();
		Coalition rest = allowed == null ? Coalition.getFull(n) : new Coalition(allowed);
		while (!rest.isEmpty()){
			Coalition c = getConnectedComponent(rest.getAny(), allowed);
			rest.remove(c);
			res.add(c);
		}
		return res;
	}
	
	public Coalition getConnectedComponent(int v){
		return getConnectedComponent(v, null);
	}
	
	public Coalition getConnectedComponent(int v, Coalition allowed){
		Coalition res = new Coalition();
		Coalition q = new Coalition(v);
		while (!q.isEmpty()){
			int i = q.removeAny();
			res.add(i);
			for (int j : getSuccs(i))
				if (!res.contains(j) && (allowed == null || allowed.contains(j)))
					q.add(j);
			if (directed)
				for (int j : getPreds(i))
					if (!res.contains(j) && (allowed == null || allowed.contains(j)))
						q.add(j);
		}
		return res;
	}
	
	public LWGraph<Integer,Void> getInducedGraph(Coalition c){
		LWGraph<Integer,Void> res = new LWGraph<Integer,Void>("induced(" + this.name + ")",
				c.asList(), this.directed);
		for (Integer i : c)
			for (Integer j : getSuccs(i))
				if ((i < j || isDirected()) && c.contains(j))
					res.addLWEdge(i, j);
		return res;
	}
	
	public LWGraph<Integer,Void> getInducedGraph(IntPredicate p){
		Coalition c = new Coalition();
		this.nodesStream().filter(p).forEach(v -> c.add(v));
		return getInducedGraph(c);
	}
	
	public void forceConnectivity(BiFunction<Coalition, IntStream, Edge> choose, Consumer<Edge> handler){
		Coalition last = null;
		for (Coalition c : getConnectedComponents()){
			if (last != null) {
				Edge e = choose.apply(c, last.stream());
				addEdge(e);
				handler.accept(e);
			}
			last = c;
		}
	}
	
	public void forceConnectivity() {
		forceConnectivity((c, rest) -> e(c.getRandom().intValue(), rest.findAny().getAsInt()), e -> {});
	}
	
	public double getAverageDegree() {
		return nodesStream().mapToDouble(i -> getDegree(i)).sum() / n;
	}
	
	@Override
	public String toString() {
		return "(" + name + ",n=" + n + ",m=" + m + (isDirected() ? ",directed" : "") + ")";
	}
	
	private abstract class Change {
		
		protected int i;
		protected int j;
		
		public Change(int i, int j) {
			this.i = i;
			this.j = j;
		}

		public Edge getEdge() {
			return Graph.this.e(i, j);
		}
		
		public abstract void revert();
	}
	
	private class Addition extends Change {
		
		public Addition(int i, int j) {
			super(i, j);
		}
		
		@Override
		public void revert() {
			performRemoveEdge(i, j);
			notifyListenersRemove(new Edge(i, j, directed));
		}
	}
	
	private class Removal extends Change {
		
		public Removal(int i, int j) {
			super(i, j);
		}
		
		@Override
		public void revert() {
			performAddEdge(i, j);
			notifyListenersAdd(new Edge(i, j, directed));
		}
	}
	
	private class EdgesIterator implements Iterator<Edge>{
		
		private int i;
		private int next;
		private Iterator<Integer> iter;
		
		public EdgesIterator() {
			this.i = 0;
			this.next = 0;
			this.iter = getSuccs(i).iterator();
			findNext();
		}

		@Override
		public boolean hasNext() {
			return i < n;
		}

		@Override
		public Edge next() {
			Edge res = e(i, next);
			findNext();
			return res;
		}
		
		private void findNext(){
			do {
				while (iter.hasNext()){
					next = iter.next();
					if (i < next || directed)
						return;
				}
				++i;
				if (i < n)
					iter = getSuccs(i).iterator();
			} while (i < n);
		}
	}
}
