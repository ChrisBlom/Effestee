package blom.effestee.semiring;

public class ProbMaxRing extends SemiRing<Double> {

	@Override
	public Double zero() {
		return 0d;
	}

	@Override
	public Double one() {
		return 1d;
	}

	@Override
	public Double plus(Double x, Double y) {
		return Math.max(x, y);
	}

	@Override
	public Double times(Double x, Double y) {
		return x * y;
	}

}
