package blom.effestee;

interface SemiRing<A> {

	A zero();

	A one();

	A plus(A x, A y);

	A times(A x, A y);

	static class BooleanRing implements SemiRing<Boolean> {

		@Override
		public Boolean zero() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Boolean one() {
			// TODO Auto-generated method stub
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

}
