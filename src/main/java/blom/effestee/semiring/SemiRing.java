package blom.effestee.semiring;

public interface SemiRing<A> {

	A zero();

	A one();

	A plus(A x, A y);

	A times(A x, A y);

}
