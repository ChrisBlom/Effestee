package blom.effestee.logic;

class Condition<A> extends BoolTerm<A> {

	public final A value;

	Condition(BoolTerm.Polarity polarity, A value) {
		super(Flag.BASIC, polarity);
		this.value = value;
	}

	public Condition<A> copy() {
		return new Condition<>(this.polarity, this.value);
	}

	void negate() {
//		return new Condition<>(polarity.inverse(), value);
		this.polarity = polarity.inverse();
	}

	@Override
	public String toString() {
		return (this.polarity == Polarity.Pos ? "=" : "!" ) + value ;
	}

	@Override
	boolean accepts(A value) {
		switch (polarity) {
		case Pos:
			return this.value.equals(value);
		case Neg:
			return !this.value.equals(value);
		}
		throw new IllegalStateException();
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Condition other = (Condition) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}