package blom.effestee;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import blom.effestee.function.F1;
import blom.effestee.semiring.Pair;
import blom.effestee.semiring.SequencesSemiRing;

public class Main {

	public static void main(String[] args) {

		Fst<Pair<Character, Character>> hello = Fst.formTo("test!", "hello");
		Fst<Pair<Character, Character>> world = Fst.formTo("test!", "world");

		F1<Pair<Character, Character>, Character> fstProj = new F1.FstProj<>();
		F1<Pair<Character, Character>, Character> sndProj = new F1.SndProj<>();

		hello.mapLabels(new F1<Pair<Character, Character>, Pair<Character, Double>>() {

			@Override
			public Pair<Character, Double> $(Pair<Character, Character> in) {
				return Pair.of( in.fst,(double) ((int)in.snd));
			}

		});

		hello.inplaceUnion(world);
		System.out.println(hello);

		List<Character> asList = toList("test!");
		List<TreeNode<Pair<Character, Character>>> paths = hello.getPaths(
				asList, fstProj);

		System.out.println(run(paths, sndProj));
		System.out.println(paths.size() + "-----------------------");
	}

	private static List<Character> toList(final String string) {
		return new AbstractList<Character>() {

			@Override
			public Character get(int index) {
				return string.charAt(index);
			}

			@Override
			public int size() {
				return string.length();
			}
		};

	}

	private static <Label, Out> Set<List<Out>> run(List<TreeNode<Label>> paths,
			F1<Label, Out> selector) {
		SequencesSemiRing<Out> c = new SequencesSemiRing<>();

		Set<List<Out>> allResults = c.zero();
		for (TreeNode<Label> path : paths) {
			Set<List<Out>> result = c.one();

			for (Label step : path.getPath()) {
				Set<List<Out>> p = Collections.singleton(Collections
						.singletonList(selector.$(step)));
				result = c.times(result, p);
			}

			allResults = c.plus(allResults, result);
		}

		return allResults;
	}
}
