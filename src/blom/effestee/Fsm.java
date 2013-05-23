package blom.effestee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

class Fst<I, O> {

	static class State {
		@Override
		public String toString() {
			return "" + index;
		}

		public final int index;
		public List<Transition> outgoing = new LinkedList<>();

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
	}

	final Set<State> states = new HashSet<>();
	final Set<State> initial = new HashSet<>();
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
	}

	static class Transition<In, Out> {

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
			return String.format("%s -%s-> %s", source, label, target);
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

		RunState next(State s, int transitionIndex) {

			// TODO : use something clever to share paths
			List<Integer> newPath = new LinkedList<>(this.path);
			newPath.add(transitionIndex);
			return new RunState(newPath, s, this.position + 1);
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

	void concat(Fst<I, O> other) {

		Map<State, State> other_to_this = this.stateFactory
				.importStates(other.states);

		Stack<State> stack = new Stack<>();
		stack.addAll(other.initial);

		// for (State other_initial : other.initial) {
		// this.makeInitial(other_to_this.get(other_initial));
		// }

		for (State accept : this.acceptStates) {
			this.setAccept(accept, false);
		}

		while (!stack.isEmpty()) {
			State current_foreign = stack.pop();

			if (other.isAccept(current_foreign)) {
				this.setAccept(other_to_this.get(current_foreign), true);
			}

			for (Transition<I, O> out_foreign : current_foreign.outgoing) {
				addTransition(out_foreign.label.copy(),
						other_to_this.get(out_foreign.source),
						other_to_this.get(out_foreign.target));
				stack.push(out_foreign.target);
			}
		}

	}

	private void makeInitial(State state) {
		this.initial.add(state);
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

		for (State initState : initial) {
			q.add(new RunState(initState));
		}

		List<int[]> solutions = new ArrayList<int[]>();

		while (!q.isEmpty()) {

			RunState current = q.pop();

			if (isAccept(current.state)) {
				solutions.add(toIntArray(current.path));
			}

			if (current.position == input.size()) {
				continue;
			}

			for (int i = 0; i < current.state.outgoing.size(); i++) {

				I nextSymbol = input.get(current.position);

				Transition<I, O> transition = current.state.outgoing.get(i);
				if (transition.label.acceptIn(nextSymbol)) {
					q.add(current.next(transition.target, i));

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
		this.initial.add(newState);
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

		State s = this.initial.iterator().next();
		for (int i = 0; i < path.length; i++) {
			Transition transition = s.outgoing.get(path[i]);
			out.add((O) transition.label.outputSymbol);
			s = transition.target;
		}
		return out;

	}

	public static void main(String[] args) {

		Fst<Character, Character> chris = fromString("chris");
		Fst<Character, Character> blom = fromString("blom");

		chris.concat(blom);
		
		List<int[]> paths = chris.run(Arrays.asList('c', 'h', 'r', 'i', 's',
				'b', 'l', 'o', 'm'));
		for (int[] path : paths) {
			List<Character> out = chris.readPath(path);
			System.out.println(out);
			assert (out.equals(Arrays.asList("CHRIS".toCharArray())));
		}

	}

	private static Fst<Character, Character> fromString(String test) {
		State previous = null;
		Fst<Character, Character> bla = new Fst<>();
		for (int i = 0; i < test.length(); i++) {

			State source = previous == null ? bla.addStateInitial() : previous;
			State target = i + 1 == test.length() ? bla.addStateAccept() : bla
					.addState();
			Transition<Character, Character> transition = bla.addTransition(
					new Label<>(test.charAt(i), Character.toUpperCase(test
							.charAt(i))), source, target);
			previous = target;
		}
		return bla;
	}
}