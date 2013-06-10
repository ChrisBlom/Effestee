package blom.effestee.logic;

public abstract class BooleanTerm<A> {

	public enum Flag {
		BASIC, COMPOUND
	};

	enum Polarity {
		Pos, Neg;

		public Polarity inverse() {
			switch (this) {
			case Neg:
				return Pos;
			case Pos:
				return Neg;
			}
			throw new IllegalStateException();
		}
	}

	public static final <A> BooleanTerm<A> verum() {
		return new Connective<A>(Polarity.Pos);
	}

	public final Flag flag;
	public Polarity polarity;

	public BooleanTerm(blom.effestee.logic.BooleanTerm.Flag flag, Polarity p) {
		this.flag = flag;
		this.polarity = p;
	}

	public static <A> BooleanTerm<A> is(A value) {
		return new Predicate<A>(Polarity.Pos, value);
	}

	public static <A> BooleanTerm<A> notIs(A value) {
		return new Predicate<A>(Polarity.Neg, value);
	}

	@SafeVarargs
	public static <A> BooleanTerm<A> or(BooleanTerm<A> a, BooleanTerm<A> b,
			BooleanTerm<A>... more) {
		return new Connective<>(Polarity.Neg, a, b, more);
	}

	@SafeVarargs
	public static <A> BooleanTerm<A> and(BooleanTerm<A> a, BooleanTerm<A> b,
			BooleanTerm<A>... more) {
		return new Connective<>(Polarity.Pos, a, b, more);
	}

	abstract void negate();

	abstract boolean accepts(A value);

	public static <A> BooleanTerm<A> falsum() {
		return new Connective<>(Polarity.Neg);
	}
	
	abstract boolean satisfiable();

}
