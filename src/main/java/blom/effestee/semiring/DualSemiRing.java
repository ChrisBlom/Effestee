package blom.effestee.semiring;

public class DualSemiRing<A,B> extends
		SemiRing<Pair<A, B>> {

	public final SemiRing<A> fstRing;
	public final SemiRing<B> sndRing;

	public DualSemiRing(SemiRing<A> fstRing, SemiRing<B> sndRing) {
		this.fstRing = fstRing;
		this.sndRing = sndRing;
	}

	@Override
	public Pair<A, B> zero() {
		return Pair.from(fstRing.zero(), sndRing.zero());
	}

	@Override
	public Pair<A, B> one() {
		return Pair.from(fstRing.one(), sndRing.one());
	}

	@Override
	public Pair<A, B> plus(Pair<A, B> x, Pair<A, B> y) {
		return Pair
				.from(fstRing.plus(x.fst, y.fst), sndRing.plus(x.snd, y.snd));
	}

	@Override
	public Pair<A, B> times(Pair<A, B> x, Pair<A, B> y) {
		return Pair.from(fstRing.times(x.fst, y.fst),
				sndRing.times(x.snd, y.snd));
	}

	public SemiRing<Pair<A, B>>.Val wrap(A a, B b) {
		return wrap(Pair.from(a, b));
		
	}

}