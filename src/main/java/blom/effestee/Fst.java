package blom.effestee;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import blom.effestee.function.F1;
import blom.effestee.function.F2;
import blom.effestee.function.F1.FstProj;
import blom.effestee.semiring.BooleanRing;
import blom.effestee.semiring.Pair;
import blom.effestee.semiring.SequencesSemiRing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class Fst<L> {

	public enum StateFlag {
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

	final Set<Transition<L>> transitions = new HashSet<Transition<L>>();

	private final StateFactory stateFactory = new StateFactory();

	class StateFactory {

		private int lastIndex = 0;

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

	class RunState {

		// keeps track of which transition was picked (by index)
		private TreeNode<L> paths;

		public RunState(TreeNode<L> root, State initState) {
			this.state = initState;
			this.paths = root;
		}

		@Override
		public String toString() {
			return "RunState [state=" + state + ", position=" + position
					+ ", path=" + paths + "]";
		}

		RunState next(Transition t, TreeNode paths) {

			int advance = t.label == null ? 0 : 1;
			TreeNode<L> addChild = paths.addChild(t.label);
			return new RunState(addChild, t.target, this.position + advance);
		}

		private RunState(TreeNode<L> addChild, State state, int position) {
			this.paths = addChild;
			this.state = state;
			this.position = position;
		}

		State state;

		int position;

	}

	/**
	 * @param other
	 */
	void inplaceConcat(Fst<L> other) {

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
				addTransition(thisAccept, other_to_this.get(otherInitial));
			}
		}

		ArrayDeque<State> stack = new ArrayDeque<>();
		stack.addAll(other.initialStates);

		while (!stack.isEmpty()) {

			State current_foreign = stack.pop();
			State current_this = other_to_this.get(current_foreign);

			for (Transition<L> out_foreign : current_foreign.outgoing) {
				addTransition(out_foreign.label,
						other_to_this.get(out_foreign.source),
						other_to_this.get(out_foreign.target));
				stack.push(out_foreign.target);
			}
		}

	}

	/**
	 * @param other
	 */
	void inplaceUnion(Fst<L> other) {
		Map<State, State> other_to_this = this.stateFactory
				.importStates(other.states);

		addFlag(get(other_to_this, other.initialStates), StateFlag.INITIAL);
		addFlag(get(other_to_this, other.acceptStates), StateFlag.ACCEPT);

		for (Transition<L> t : other.transitions) {
			this.addTransition(t.label, other_to_this.get(t.source),
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

	private void addFlag(Collection<State> states, StateFlag flag) {

		for (State s : states) {
			this.setFlag(s, flag);
		}

	}

	private void setFlag(State s, StateFlag flag) {

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

	<I> TreeNode<L> run(List<I> input, F1<L, I> selector) {

		LinkedList<RunState> q = new LinkedList<RunState>();

		TreeNode<L> root = TreeNode.create();

		for (State initState : initialStates) {
			q.add(new RunState(root, initState));
		}

		while (!q.isEmpty()) {

			RunState current = q.pop();

			if (current.position == input.size()) {
				if (isAccept(current.state)) {
					current.paths.markAccept();
				} else {
					continue;
				}
			}

			for (int i = 0; i < current.state.outgoing.size(); i++) {

				I nextSymbol = input.get(current.position);

				Transition<L> transition = current.state.outgoing.get(i);

				if (transition.label == null
						|| selector.$(transition.label).equals(nextSymbol)) {
					q.add(current.next(transition, current.paths));

				}

			}

		}

		return root;

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

	Transition<L> addTransition(L pair, State source, State target) {
		Transition<L> t = new Transition<L>(pair, source, target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;
	}

	private Transition<L> addTransition(State source, State target) {
		Transition<L> t = new Transition<L>(null, source, target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;

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

	static <L, X> Fst<L> intersect(Fst<L> left, Fst<L> right,
			F1<L, X> intersectOn) {

		Fst<L> intersection = new Fst<L>();

		BooleanRing br = new BooleanRing();

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
			for (Transition<L> fromL : l.outgoing) {
				for (Transition<L> fromR : r.outgoing) {

					L both = intersectable(br, fromL.label, fromR.label,
							intersectOn);
					if (both != null) {

						StatePair targetPair = new StatePair(fromL.target,
								fromR.target);

						State target = resolvePair(targetPair, left, right,
								chart, intersection);

						intersection.addTransition(fromL.label, source, target);

						stack.push(targetPair);

					} else {
						// optimization : remove dead ends
						if (!intersection.isAccept(source)) {
							intersection.remove(source);
						}
					}
				}
			}
		}

		return intersection;
	}

	static <A, B, C, X> Fst<C> compose(Fst<A> left, Fst<B> right,
			F1<A, X> leftSel, F1<B, X> rightSel, F2<A, B, C> compo) {

		Fst<C> intersection = new Fst<>();

		LinkedList<StatePair> stack = new LinkedList<>();
		Map<StatePair, State> chart = new HashMap<>();

		for (State l : left.initialStates) {
			for (State r : right.initialStates) {
				StatePair lr = new StatePair(l, r);
				resolvePair2(lr, left, right, chart, intersection);
				stack.push(lr);
			}
		}

		while (!stack.isEmpty()) {
			State l = stack.peek().fst;
			State r = stack.peek().snd;
			StatePair lr = stack.pop();
			State source = chart.get(lr);

			for (Transition<A> fromL : l.outgoing) {
				for (Transition<B> fromR : r.outgoing) {

					C both = composable(fromL.label, fromR.label, leftSel,
							rightSel, compo);
					if (both != null) {

						StatePair targetPair = new StatePair(fromL.target,
								fromR.target);

						State target = resolvePair2(targetPair, left, right,
								chart, intersection);

						intersection.addTransition(both, source, target);

						stack.push(targetPair);

					} else {
						// optimization : remove dead ends
						if (!intersection.isAccept(source)) {
							intersection.remove(source);
						}
					}
				}
			}
		}

		return intersection;
	}

	private static <A, B, C, X, Y> C composable(A left, B right,
			F1<A, X> leftSel, F1<B, Y> rightSel, F2<A, B, C> compo) {

		X leftProjected = leftSel.$(left);
		Y rightProjected = rightSel.$(right);
		if (leftProjected.equals(rightProjected)) {
			return compo.$(left, right);
		}

		return null;
	}

	private void remove(State source) {
		this.acceptStates.remove(source);
		this.initialStates.remove(source);
		this.states.remove(source);
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
	private static <L> State resolvePair(StatePair lr, Fst<L> left,

	Fst<L> rigth, final Map<StatePair, State> chart, Fst<L> intersection) {
		if (chart.containsKey(lr)) {
			return chart.get(lr);
		} else {

			boolean isInit = left.isInitial(lr.fst) && rigth.isInitial(lr.snd);
			boolean isAccept = left.isAccept(lr.fst) && rigth.isAccept(lr.snd);

			State intersectionState = intersection.addState(
					isInit ? StateFlag.INITIAL : null,
					isAccept ? StateFlag.ACCEPT : null);
			chart.put(lr, intersectionState);
			return intersectionState;
		}
	}

	private static <A, B, C> State resolvePair2(StatePair lr, Fst<A> left,

	Fst<B> rigth, final Map<StatePair, State> chart, Fst<C> intersection) {
		if (chart.containsKey(lr)) {
			return chart.get(lr);
		} else {

			boolean isInit = left.isInitial(lr.fst) && rigth.isInitial(lr.snd);
			boolean isAccept = left.isAccept(lr.fst) && rigth.isAccept(lr.snd);

			State intersectionState = intersection.addState(
					isInit ? StateFlag.INITIAL : null,
					isAccept ? StateFlag.ACCEPT : null);
			chart.put(lr, intersectionState);
			return intersectionState;
		}
	}

	/**
	 * add a new state for the current pair if it doesn't exist yet, //
	 * inheriting accept/initial status from the elements intersectively.
	 * 
	 * @param lr
	 * @param left
	 * @param rigth
	 * @param chart
	 * @param result
	 * @return
	 */
	private static <L> State resolveSet(Collection<State> set, Fst<L> owner,
			final Map<Collection<State>, State> chart, Fst<L> result) {
		if (chart.containsKey(set)) {
			return chart.get(set);
		} else {

			boolean isInit = false;
			for (State s : set) {
				if (owner.isInitial(s)) {
					isInit = true;
					break;
				}
			}

			boolean isAccept = false;
			for (State s : set) {
				if (owner.isAccept(s)) {
					isAccept = true;
					break;
				}
			}
			State intersectionState = result.addState(
					isInit ? StateFlag.INITIAL : null,
					isAccept ? StateFlag.ACCEPT : null);
			chart.put(set, intersectionState);
			return intersectionState;
		}
	}

	private static <L, X> L intersectable(//
			BooleanRing br, //
			L label, //
			L label2, //
			F1<L, X> intersectOn) {
		if (intersectOn.$(label).equals(intersectOn.$(label2))) {
			return label;
		}
		return null;
	}

	public static void main(String[] args) {

		Fst<Pair<Character, Character>> abc2xyz = formTo("abc", "xyz");
		Fst<Pair<Character, Character>> xyz2123 = formTo("xyz", "123");

		F1<Pair<Character, Character>, Character> fstProj = new F1.FstProj<>();
		F1<Pair<Character, Character>, Character> sndProj = new F1.SndProj<>();

		System.out.println(abc2xyz);

		F2<Pair<Character, Character>, Pair<Character, Character>, Pair<Character, Character>> compo = new F2.PairCompo<>();
		Fst<Pair<Character, Character>> abc2123 = compose(abc2xyz, xyz2123,
				sndProj, fstProj, compo);
		System.out.println(abc2123);

		TreeNode<Pair<Character, Character>> paths = abc2123.run(
				Arrays.asList('a', 'b', 'c'), fstProj);

		SequencesSemiRing<Character> c = new SequencesSemiRing<>();

		Iterator<List<Pair<Character, Character>>> iterator = paths.iterator();
		for (; iterator.hasNext();) {
			List<Pair<Character, Character>> path = iterator.next();

			Set<List<Character>> out = c.one();

			for (Pair<Character, Character> step : path) {
				Set<List<Character>> p = Collections.singleton(Collections
						.singletonList(step.snd));
				out = c.times(out, p);
			}
			System.out.println(out);
			assert (out.equals(Arrays.asList("CHRIS".toCharArray())));
		}

	}

	private Set<List<Character>> readPath(List<Pair<Character, Character>> path) {

		return null;
	}

	public static <L> Fst<L> determinize(Fst<L> nondet) {

		Fst<L> det = new Fst<L>();

		ArrayDeque<Collection<State>> stack = new ArrayDeque<>();
		Map<Collection<State>, State> chart = new HashMap<>();

		stack.push(nondet.initialStates);

		Multimap<L, State> transFunction = HashMultimap.create();
		while (!stack.isEmpty()) {

			Collection<State> sourceSet = stack.pop();
			State source = resolveSet(sourceSet, nondet, chart, det);

			// compute the transition function
			transFunction.clear();
			for (State nondetState : sourceSet) {
				// TODO epsilon reachable states
				for (Transition<L> t : nondetState.outgoing) {
					assert (t.label != null);
					transFunction.put(t.label, t.target);
				}
			}

			// add transitions XXX merge with calculation of transition
			// function?
			for (Entry<L, Collection<State>> e : transFunction.asMap()
					.entrySet()) {

				L label = e.getKey();
				Collection<State> targetSet = e.getValue();

				State target = resolveSet(targetSet, nondet, chart, det);
				det.addTransition(label, source, target);

				stack.push(targetSet);
			}

		}

		return det;
	}

	public static Fst<Pair<Character, Character>> fromString(CharSequence string) {

		Fst<Pair<Character, Character>> bla = new Fst<>();
		if (string.length() == 0) {
			bla.addState(StateFlag.ACCEPT, StateFlag.INITIAL);
			return bla;
		}

		State previous = null;

		for (int i = 0; i < string.length(); i++) {
			State source = previous == null ? bla.addStateInitial() : previous;
			State target = i + 1 == string.length() ? bla.addStateAccept()
					: bla.addState();

			char charAt = string.charAt(i);
			Transition<Pair<Character, Character>> transition = bla
					.addTransition(
							Pair.of(Character.toLowerCase(charAt), charAt),
							source, target);
			previous = target;
		}
		return bla;
	}

	public static Fst<Pair<Character, Character>> formTo(CharSequence in,
			CharSequence out) {

		Fst<Pair<Character, Character>> bla = new Fst<>();
		if (in.length() == 0) {
			bla.addState(StateFlag.ACCEPT, StateFlag.INITIAL);
			return bla;
		}

		State previous = null;

		for (int i = 0; i < in.length(); i++) {
			State source = previous == null ? bla.addStateInitial() : previous;
			State target = i + 1 == in.length() ? bla.addStateAccept() : bla
					.addState();

			Transition<Pair<Character, Character>> transition = bla
					.addTransition(Pair.of(in.charAt(i), out.charAt(i)),
							source, target);
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

	public State addState(StateFlag... types) {
		State fresh = this.addState();

		for (StateFlag type : types) {
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

	public <I> boolean acceptIn(F1<L, I> sel, I... c) {

		List<I> in = new ArrayList<>();
		for (I ch : c) {
			in.add(ch);
		}
		return this.run(in, sel).iterator().hasNext();

	}

}