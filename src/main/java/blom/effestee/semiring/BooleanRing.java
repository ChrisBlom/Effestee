package blom.effestee.semiring;

import blom.effestee.logic.BooleanTerm;

public class BooleanRing extends SemiRing<Boolean> {

	@Override
	public Boolean zero() {
		return false;
	}

	@Override
	public Boolean one() {
		return true;
	}

	@Override
	public Boolean plus(Boolean x, Boolean y) {
		return x || y;
	}

	@Override
	public Boolean times(Boolean x, Boolean y) {
		return x && y;
	}
}