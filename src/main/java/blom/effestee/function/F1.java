package blom.effestee.function;

import blom.effestee.semiring.Pair;

public abstract class F1<T1, T2> {

	public abstract T2 $(T1 in);

	public static <A, B> FstProj<A, B> fstProj() {
		return new FstProj<>();
	}
	public static <A, B> SndProj<A, B> sndProj() {
		return new SndProj<>();
	}
	
	public static class FstProj<A, B> extends F1<Pair<A, B>, A> {
		@Override
		public A $(Pair<A, B> in) {
			return in.fst;
		}
	}

	public static class SndProj<A, B> extends F1<Pair<A, B>, B> {
		@Override
		public B $(Pair<A, B> in) {
			return in.snd;
		}
	}

}
