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

import blom.effestee.semiring.BooleanRing;
import blom.effestee.semiring.DualSemiRing;
import blom.effestee.semiring.Pair;
import blom.effestee.semiring.SemiRing;
import blom.effestee.semiring.SemiRing.Val;
import blom.effestee.semiring.SequencesSemiRing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class Fst<I, O> {

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

	final Set<Transition<Pair<I, O>>> transitions = new HashSet<Transition<Pair<I, O>>>();

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
		private TreeNode<Transition<Pair<I, O>>> paths;

		public RunState(TreeNode<Transition<Pair<I, O>>> root, State initState) {
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
			TreeNode addChild = paths.addChild(t);
			return new RunState(addChild, t.target, this.position + advance);
		}

		private RunState(TreeNode<Transition<Pair<I, O>>> path, State state,
				int position) {
			this.paths = path;
			this.state = state;
			this.position = position;
		}

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
				addTransition(thisAccept, other_to_this.get(otherInitial));
			}
		}

		Stack<State> stack = new Stack<>();
		stack.addAll(other.initialStates);

		while (!stack.isEmpty()) {

			State current_foreign = stack.pop();
			State current_this = other_to_this.get(current_foreign);

			for (Transition<Pair<I, O>> out_foreign : current_foreign.outgoing) {
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
	void inplaceUnion(Fst<I, O> other) {

		Map<State, State> other_to_this = this.stateFactory
				.importStates(other.states);

		addFlag(get(other_to_this, other.initialStates), StateFlag.INITIAL);
		addFlag(get(other_to_this, other.acceptStates), StateFlag.ACCEPT);

		for (Transition<Pair<I, O>> t : other.transitions) {
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

	TreeNode<Transition<Pair<I, O>>> run(List<I> input) {

		LinkedList<RunState> q = new LinkedList<RunState>();

		TreeNode<Transition<Pair<I, O>>> root = TreeNode.create();

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

				Transition<Pair<I, O>> transition = current.state.outgoing
						.get(i);

				if (transition.label == null
						|| transition.label.value.fst.equals(nextSymbol)) {
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

	SemiRing<Pair<I, O>> s = new DualSemiRing(null, null);

	Transition<Pair<I, O>> addTransition(Pair<I, O> pair, State source,
			State target) {
		Transition<Pair<I, O>> t = new Transition<Pair<I, O>>(s.wrap(pair),
				source, target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;
	}

	private Transition<Pair<I, O>> addTransition(Val wrap, State source,
			State target) {
		Transition<Pair<I, O>> t = new Transition<Pair<I, O>>(wrap, source,
				target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;

	}

	private Transition<Pair<I, O>> addTransition(State source, State target) {
		Transition<Pair<I, O>> t = new Transition<Pair<I, O>>(null, source,
				target);
		source.outgoing.add(t);
		this.transitions.add(t);
		return t;

	}

	Set<List<O>> readPath(List<Transition<Pair<I, O>>> path) {

		SequencesSemiRing<O> lsr = new SequencesSemiRing<>();

		Set<List<O>> out = lsr.one();

		List<Set<List<O>>> labels = new ArrayList<>();

		for (Transition<Pair<I, O>> t : path) {
			if (t.label != null) {
				labels.add(Collections.singleton(Collections
						.singletonList(t.label.value.snd)));
			}
		}

		return lsr.product(labels);

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
			for (Transition<Pair<I, O>> fromL : l.outgoing) {
				for (Transition<Pair<I, O>> fromR : r.outgoing) {

					SemiRing<Pair<I, O>>.Val both = intersectable(br,
							fromL.label, fromR.label);
					if (both != null) {

						StatePair targetPair = new StatePair(fromL.target,
								fromR.target);

						State target = resolvePair(targetPair, left, right,
								chart, intersection);

						intersection.addTransition(
								fromL.label.ring.wrap(both.value), source,
								target);

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

	static <A, B, C> Fst<A, C> compose(Fst<A, B> left, Fst<B, C> right) {

		Fst<A, C> intersection = new Fst<>();

		BooleanRing br = new BooleanRing();

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

			// TODO efficient intersection
			for (Transition<Pair<A, B>> fromL : l.outgoing) {
				for (Transition<Pair<B, C>> fromR : r.outgoing) {

					Pair<A, C> both = composable(br, fromL.label,
							fromR.label);
					if (both != null) {

						StatePair targetPair = new StatePair(fromL.target,
								fromR.target);

						State target = resolvePair2(targetPair, left, right,
								chart, intersection);

						intersection
								.addTransition(both, source, target);

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

	private static <A, B, C> Pair<A, C> composable(
			BooleanRing br, SemiRing<Pair<A, B>>.Val l,
			SemiRing<Pair<B, C>>.Val r) {

		if( l.value.snd.equals( r.value.fst) ) {
			return Pair.from( l.value.fst , r.value.snd	);
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
	private static <I, O> State resolvePair(StatePair lr, Fst<I, O> left,

	Fst<I, O> rigth, final Map<StatePair, State> chart, Fst<I, O> intersection) {
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

	private static <A, B, C> State resolvePair2(StatePair lr, Fst<A, B> left,

	Fst<B, C> rigth, final Map<StatePair, State> chart, Fst<A, C> intersection) {
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
	private static <I, O> State resolveSet(Collection<State> set,
			Fst<I, O> owner, final Map<Collection<State>, State> chart,
			Fst<I, O> result) {
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

	private static <I, O> SemiRing<Pair<I, O>>.Val intersectable(//
			BooleanRing br, //
			SemiRing<Pair<I, O>>.Val label, //
			SemiRing<Pair<I, O>>.Val label2 //
	) {
		if (br.times(label.value.fst.equals(label2.value.fst),
				label.value.snd.equals(label2.value.snd))) {
			return label;
		}
		return null;
	}

	public static void main(String[] args) {

		Fst<Character, Character> abc2xyz = formTo("abc", "xyz");
		Fst<Character, Character> xyz2123 = formTo( "xyz","123");

		Fst<Character, Character> abc2123 = compose(abc2xyz, xyz2123);
		System.out.println(abc2123);

		TreeNode<Transition<Pair<Character, Character>>> paths = abc2123
				.run( Arrays.asList('a','b','c'));

		Iterator<List<Transition<Pair<Character, Character>>>> iterator = paths
				.iterator();
		for (; iterator.hasNext();) {
			List<Transition<Pair<Character, Character>>> path = iterator.next();
			Set<List<Character>> out = abc2123.readPath(path);
			System.out.println(out);
			assert (out.equals(Arrays.asList("CHRIS".toCharArray())));
		}

	}

	public static <I, O> Fst<I, O> determinize(Fst<I, O> nondet) {

		Fst<I, O> det = new Fst<I, O>();

		ArrayDeque<Collection<State>> stack = new ArrayDeque<>();
		Map<Collection<State>, State> chart = new HashMap<>();

		stack.push(nondet.initialStates);

		Multimap<SemiRing<Pair<I, O>>.Val, State> transFunction = HashMultimap
				.create();
		while (!stack.isEmpty()) {

			Collection<State> sourceSet = stack.pop();

			State source = resolveSet(sourceSet, nondet, chart, det);

			// compute the transition function
			transFunction.clear();
			for (State nondetState : sourceSet) {
				// TODO epsilon reachable states
				for (Transition<Pair<I, O>> t : nondetState.outgoing) {
					assert (t.label != null);
					transFunction.put(t.label, t.target);
				}
			}

			// add transitions XXX merge with calculation of transition
			// function?
			for (Entry<SemiRing<Pair<I, O>>.Val, Collection<State>> e : transFunction
					.asMap().entrySet()) {

				SemiRing<Pair<I, O>>.Val label = e.getKey();
				Collection<State> targetSet = e.getValue();

				State target = resolveSet(targetSet, nondet, chart, det);

				det.addTransition(label, source, target);

				stack.push(targetSet);
			}

		}

		return det;
	}

	public static Fst<Character, Character> fromString(CharSequence string) {

		Fst<Character, Character> bla = new Fst<>();
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
							Pair.from(Character.toLowerCase(charAt), charAt),
							source, target);
			previous = target;
		}
		return bla;
	}
	
	public static Fst<Character, Character> formTo(CharSequence in,CharSequence out) {

		Fst<Character, Character> bla = new Fst<>();
		if (in.length() == 0) {
			bla.addState(StateFlag.ACCEPT, StateFlag.INITIAL);
			return bla;
		}

		State previous = null;

		for (int i = 0; i < in.length(); i++) {
			State source = previous == null ? bla.addStateInitial() : previous;
			State target = i + 1 == in.length() ? bla.addStateAccept()
					: bla.addState();

			Transition<Pair<Character, Character>> transition = bla
					.addTransition(
							Pair.from(in.charAt(i),out.charAt(i)),
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

	public boolean acceptIn(I... c) {

		List<I> in = new ArrayList<>();
		for (I ch : c) {
			in.add(ch);
		}
		return this.run(in).iterator().hasNext();

	}
}