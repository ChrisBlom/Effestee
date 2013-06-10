package blom.effestee.function;

import blom.effestee.semiring.Pair;

public interface F2<X, Y, Z> {

	public abstract Z $(X x, Y y);

	public static class PairCompo<A, B, C> implements
			F2<Pair<A, B>, Pair<B, C>, Pair<A, C>> {

		@Override
		public Pair<A, C> $(Pair<A, B> x, Pair<B, C> y) {
			return new Pair<>(x.fst, y.snd);
		}

	}

}
