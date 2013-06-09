package blom.effestee.semiring;

import blom.effestee.logic.BooleanTerm;

public class BooleanTermRing<A> extends SemiRing<BooleanTerm<A>> {

	@Override
	public BooleanTerm<A> zero() {
		return BooleanTerm.falsum();
	}

	@Override
	public BooleanTerm<A> one() {
		return BooleanTerm.verum();
	}

	@Override
	public BooleanTerm<A> plus(BooleanTerm<A> x, BooleanTerm<A> y) {

		return BooleanTerm.or(x, y);
	}

	@Override
	public BooleanTerm<A> times(BooleanTerm<A> x, BooleanTerm<A> y) {
		return BooleanTerm.and(x, y);
	}

}