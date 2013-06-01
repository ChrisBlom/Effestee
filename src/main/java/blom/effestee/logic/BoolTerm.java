package blom.effestee.logic;


public abstract class BoolTerm<A> {

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

	final Flag flag;
	Polarity polarity;

	// static <A> Expression<A> simplify(Expression<A> e) {
	// // basic expression cannot be simplified further
	// if (e.flag == Flag.BASIC) {
	// return e;
	// }
	//
	// switch (e.polarity) {
	// case Neg:
	// break;
	// case Pos:
	// break;
	// default:
	// break;
	// }
	// }

	public BoolTerm(blom.effestee.logic.BoolTerm.Flag flag, Polarity p) {
		this.flag = flag;
		this.polarity = p;
	}

	static <A> BoolTerm<A> is(A value) {
		return new Condition<A>(Polarity.Pos, value);
	}

	static <A> BoolTerm<A> notIs(A value) {
		return new Condition<A>(Polarity.Neg, value);
	}

	@SafeVarargs
	static <A> BoolTerm<A> or(BoolTerm<A> a, BoolTerm<A> b, BoolTerm<A>... more) {
		return new Compound<>(Polarity.Neg, a, b, more);
	}

	@SafeVarargs
	static <A> BoolTerm<A> and(BoolTerm<A> a, BoolTerm<A> b, BoolTerm<A>... more) {
		return new Compound<>(Polarity.Pos, a, b, more);
	}

	abstract void negate();

	abstract boolean accepts(A value);

}
