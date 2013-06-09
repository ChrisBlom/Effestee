package blom.effestee.semiring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blom.effestee.logic.BooleanTerm;

public class SequencesSemiRing<A> extends SemiRing<Set<List<A>>> {

	static <X> List<X> concat(List<X> x, List<X> y) {
		ArrayList concat = new ArrayList<>(x.size() + y.size());
		concat.addAll(x);
		concat.addAll(y);
		return concat;
	}

	@Override
	public Set<List<A>> plus(Set<List<A>> x, Set<List<A>> y) {
		Set<List<A>> union = new HashSet<>();
		union.addAll(x);
		union.addAll(y);
		return union;
	}

	@Override
	public Set<List<A>> times(Set<List<A>> x, Set<List<A>> y) {
		Set<List<A>> res = new HashSet<>();
		for (List<A> a : x) {
			for (List<A> b : y) {
				res.add(concat(a, b));
			}
		}
		return res;
	}

	@Override
	public Set<List<A>> zero() {
		return Collections.EMPTY_SET;
	}

	@Override
	public Set<List<A>> one() {
		return Collections.singleton((List<A>) Collections.EMPTY_LIST);
	}

	public SemiRing<Set<List<A>>>.Val wrapSimple(A val) {
		return wrap(Collections.singleton(Collections.singletonList(val)));
	}

}
