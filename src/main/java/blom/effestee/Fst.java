package blom.effestee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

class Fst<I, O> {

	public enum StateType {
		INITIAL, ACCEPT;
	}

	static class State implements Comparable<State> {
		@Override
		public String toString() {
			return "" + index;
		}

		public final int index;
		private List<Transition> outgoing = new ArrayList<>(0);

		State(int index) {
			this.index = index;
		}

		@Override
		public int hashCode() {
			return index;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			State other = (State) obj;
			if (index != other.index)
				return false;
			return true;
		}

		@Override
		public int compareTo(State o) {
			return Integer.compare(this.index, o.index);
		}
	}

	final Set<State> states = new HashSet<>();
	final Set<State> initialStates = new HashSet<>();
	final Set<State> acceptStates = new HashSet<>();

	final Set<Transition<I, O>> transitions = new HashSet<Transition<I, O>>();
	private final StateFactory stateFactory = new StateFactory();

	class StateFactory {

		int lastIndex = 0;

		State create() {
			return new State(++lastIndex);
		}

		Map<State, State> importStates(Collection<State> foreign) {
			Map<State, State> foreignToNative = new HashMap<>();

			for (State state_other : foreign) {
				State imported = create();
				Fst.this.states.add(imported);
				foreignToNative.put(state_other, imported);
			}

			return foreignToNative;
		}

	}

	static class Label<I, O> {

		public final I inputSymbol;
		public final O outputSymbol;

		public Label(I inputSymbol, O outputSymbol) {
			this.inputSymbol = inputSymbol;
			this.outputSymbol = outputSymbol;
		}

		boolean acceptIn(I inSymbol) {
			return inputSymbol.equals(inSymbol);
		}

		@Override
		public String toString() {
			return String.format("(%s,%s)", inputSymbol, outputSymbol);
		}

		public Label<I, O> copy() {
			return new Label<>(this.inputSymbol, this.outputSymbol);
		}

		public static <I, O> Label<I, O> epsilon() {
			return null;
		}
	}

	static class Transition<In, Out> implements Comparable<Transition<In, Out>> {

		public final Label<In, Out> label;

		State source;
		State target;

		public Transition(Label<In, Out> label, State source, State target) {
			this.label = label;
			this.source = source;
			this.target = target;
		}

		@Override
		public String toString() {
			return String.format("%s -%s-> %s", source, label == null ? " --- "
					: label, target);
		}

		@Override
		public int compareTo(Transition<In, Out> o) {

			int onSource = this.source.compareTo(o.source);

			if (onSource == 0) {
				return this.target.compareTo(o.target);
			} else {
				return onSource;
			}
		}
	}

	class RunState {

		public RunState(State initState) {
			this.state = initState;
		}

		@Override
		public String toString() {
			return "RunState [state=" + state + ", position=" + position
					+ ", path=" + path + "]";
		}

		RunState next(State s, int transitionIndex, Label l) {

			// TODO : use something clever to share paths
			List<Integer> newPath = new LinkedList<>(this.path);
			newPath.add(transitionIndex);
			int advance = l == null ? 0 : 1;
			return new RunState(newPath, s, this.position + advance);
		}

		private RunState(List<Integer> path, State state, int position) {
			this.path = path;
			this.state = state;
			this.position = position;
		}

		// copy on write!
		private List<Integer> path = new LinkedList();

		State state;

		int position;

	}

	/**
	 * @param other
	 */
	void inplaceConcat(Fst<I, O> other) {

		Map<State, State> other_to_this = this.stateFactory
				.importStates(other.states);

		Set<State> oldThisAccept = new HashSet<>(this.acceptStates);
		this.acceptStates.clear();

		for (Entry<State, State> importPair : other_to_this.entrySet()) {
			this.setAccept(importPair.getValue(),
					other.isAccept(importPair.getKey()));
		}

		for (State thisAccept : oldThisAccept) {
			// connect accept states to initial states
			for (State otherInitial : other.initialStates) {
				addTransition(null, thisAccept, other_to_this.get(otherInitial));
			}
		}

		Stack<State> stack = new Stack<>();
		stack.addAll(other.initialStates);

		while (!stack.isEmpty()) {

			State current_foreign = stack.pop();
			State current_this = other_to_this.get(current_foreign);

			for (Transition<I, O> out_foreign : current_foreign.outgoing) {
				addTransition(out_foreign.label.copy(),
						other_to_this.get(out_foreign.source),
						other_to_this.get(out_foreign.target));
				stack.push(out_foreign.target);
			}
		}

	}

	/**
	 * @param other
	 */
	void inplaceUnion(Fst<I, O> other) {

		Map<State, State> other_to_this = this.stateFactory
				.importStates(other.states);

		addFlag(get(other_to_this, other.initialStates), StateType.INITIAL);
		addFlag(get(other_to_this, other.acceptStates), StateType.ACCEPT);

		for (Transition<I, O> t : other.transitions) {
			this.addTransition(t.label.copy(), other_to_this.get(t.source),
					other_to_this.get(t.target));
		}
	}

	List<State> get(Map<State, State> map, Collection<State> states) {

		List<State> mapped = new ArrayList<>(states.size());
		for (State s : states) {
			mapped.add(map.get(s));
		}
		return mapped;
	}

	private void addFlag(Collection<State> states, StateType flag) {

		for (State s : states) {
			this.setFlag(s, flag);
		}

	}

	private void setFlag(State s, StateType flag) {

		switch (flag) {
		case INITIAL:
			initialStates.add(s);
			break;
		case ACCEPT:
			acceptStates.add(s);
		}

	}

	private void makeInitial(State state) {
		this.initialStates.add(state);
	}

	private void setAccept(State state, boolean isAccept) {
		if (isAccept) {
			this.acceptStates.add(state);
		} else {
			this.acceptStates.remove(state);
		}

	}

	List<int[]> run(List<I> input) {

		LinkedList<RunState> q = new LinkedList<RunState>();

		for (State initState : initialStates) {
			q.add(new RunState(initState));
		}

		List<int[]> solutions = new ArrayList<int[]>();

		while (!q.isEmpty()) {

			RunState current = q.pop();

			if (current.position == input.size()) {
				if (isAccept(current.state)) {
					solutions.add(toIntArray(current.path));
				} else {
					continue;
				}
			}

			for (int i = 0; i < current.state.outgoing.size(); i++) {

				I nextSymbol = input.get(current.position);

				Transition<I, O> transition = current.state.outgoing.get(i);

				if (transition.label == null
						|| transition.label.acceptIn(nextSymbol)) {
					q.add(current.next(transition.target, i, transition.label));

				}

			}

		}

		return solutions;

	}

	private int[] toIntArray(List<Integer> pathList) {
		int[] path = new int[pathList.size()];
		int i = 0;
		for (Integer transitionIndex : pathList) {
			path[i++] = transitionIndex;
		}
		return path;
	}

	private boolean isAccept(State state) {
		return this.acceptStates.contains(state);
	}

	State addState() {
		State newState = this.stateFactory.create();
		this.states.add(newState);
		return newState;
	}

	State addStateInitial() {
		State newState = this.addState();
		this.initialStates.add(newState);
		return newState;
	}

	State addStateAccept() {
		State newState = this.addState();
		this.acceptStates.add(newState);
		return newState;
	}

	Transition<I, O> addTransition(Label<I, O> label, State source, State target) {
		Transition<I, O> t = new Transition<>(label, source, target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;
	}

	List<O> readPath(int[] path) {

		LinkedList<O> out = new LinkedList<>();

		State s = this.initialStates.iterator().next();
		for (int i = 0; i < path.length; i++) {
			Transition transition = s.outgoing.get(path[i]);

			if (transition.label != null) {
				out.add((O) transition.label.outputSymbol);
			}
			s = transition.target;
		}
		return out;

	}

	static class StatePair {

		final State fst;
		final State snd;

		public StatePair(State fst, State snd) {
			super();
			this.fst = fst;
			this.snd = snd;
		}

		@Override
		public String toString() {
			return "<" + fst + "," + snd + ">";
		}

	}

	static <I, O> Fst<I, O> intersect(Fst<I, O> left, Fst<I, O> right) {

		Fst<I, O> intersection = new Fst<I, O>();

		LinkedList<StatePair> stack = new LinkedList<>();
		Map<StatePair, State> chart = new HashMap<>();

		for (State l : left.initialStates) {
			for (State r : right.initialStates) {
				StatePair lr = new StatePair(l, r);
				resolvePair(lr, left, right, chart, intersection);
				stack.push(lr);
			}
		}

		while (!stack.isEmpty()) {
			State l = stack.peek().fst;
			State r = stack.peek().snd;
			StatePair lr = stack.pop();
			State source = chart.get(lr);

			// TODO efficient intersection
			for (Transition fromL : l.outgoing) {
				for (Transition fromR : r.outgoing) {

					Label both = intersectable(fromL.label, fromR.label);
					if (both != null) {

						StatePair targetPair = new StatePair(fromL.target,
								fromR.target);

						State target = resolvePair(targetPair, left, right, chart,
								intersection);

						intersection.addTransition(both, source, target);

						stack.push(targetPair);

					}

				}
			}
		}

		return intersection;
	}

	/**
	 * add a new state for the current pair if it doesn't exist yet, //
	 * inheriting accept/initial status from the elements intersectively.
	 * 
	 * @param lr
	 * @param left
	 * @param rigth
	 * @param chart
	 * @param intersection
	 * @return
	 */
	private static <I, O> State resolvePair(StatePair lr, Fst<I, O> left,
			Fst<I, O> rigth, final Map<StatePair, State> chart,
			Fst<I, O> intersection) {
		if (chart.containsKey(lr)) {
			return chart.get(lr);
		} else {

			boolean isInit = left.isInitial(lr.fst) && rigth.isInitial(lr.snd);
			boolean isAccept = left.isAccept(lr.fst) && rigth.isAccept(lr.snd);

			State intersectionState = intersection.addState(
					isInit ? StateType.INITIAL : null,
					isAccept ? StateType.ACCEPT : null);
			chart.put(lr, intersectionState);
			return intersectionState;
		}
	}

	private static Label intersectable(Label label, Label label2) {
		if (label.inputSymbol.equals(label2.inputSymbol)
				&& label.outputSymbol.equals(label2.outputSymbol)) {
			return label.copy();
		}
		return null;
	}

	public static void main(String[] args) {

		Fst<Character, Character> chris = fromString("chris");
		Fst<Character, Character> blom = fromString("blom");
		//
		chris.inplaceConcat(blom);

		List<Character> ch = Arrays.asList('c', 'h', 'r', 'i', 's');

		List<Character> chbl = Arrays.asList('c', 'h', 'r', 'i', 's', 'b', 'l',
				'o', 'm');

		System.out.println(chris);

		List<int[]> paths = chris.run(chbl);
		for (int[] path : paths) {
			List<Character> out = chris.readPath(path);
			System.out.println(out);
			assert (out.equals(Arrays.asList("CHRIS".toCharArray())));
		}

	}

	public static Fst<Character, Character> fromString(CharSequence string) {

		Fst<Character, Character> bla = new Fst<>();
		if (string.length() == 0) {
			bla.addState(StateType.ACCEPT, StateType.INITIAL);
			return bla;
		}

		State previous = null;

		for (int i = 0; i < string.length(); i++) {
			State source = previous == null ? bla.addStateInitial() : previous;
			State target = i + 1 == string.length() ? bla.addStateAccept()
					: bla.addState();

			char charAt = string.charAt(i);
			Transition<Character, Character> transition = bla.addTransition(
					new Label<>(charAt, charAt), source, target);
			previous = target;
		}
		return bla;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		List<State> states = new ArrayList<>(this.states);
		Collections.sort(states);

		for (State s : this.states) {

			sb.append(s.index)
					.append(isAccept(s) ? "#" : isInitial(s) ? ">" : " ")
					.append(s.outgoing).append("\n");
		}
		return sb.toString();

	}

	private boolean isInitial(State s) {
		return initialStates.contains(s);
	}

	public boolean acceptIn(I... args) {
		return !this.run(Arrays.asList(args)).isEmpty();
	}

	public State addState(StateType... types) {
		State fresh = this.addState();

		for (StateType type : types) {
			if (type != null) {
				switch (type) {
				case INITIAL:
					this.initialStates.add(fresh);
					break;
				case ACCEPT:
					this.acceptStates.add(fresh);
					break;
				}
			}

		}

		return fresh;
	}
}