package blom.effestee.logic;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import blom.effestee.logic.BooleanTerm.Flag;
import blom.effestee.logic.BooleanTerm.Polarity;

public class Connective<A> extends BooleanTerm<A> {

	@SafeVarargs
	Connective(BooleanTerm.Polarity p, BooleanTerm<A> a, BooleanTerm<A> b,
			BooleanTerm<A>... elems) {

		super(Flag.COMPOUND, p);
		subTerms.add(a);
		subTerms.add(b);
		for (BooleanTerm<A> e : elems) {
			subTerms.add(e);
		}
	}

	public Connective(Polarity pos) {
		super(Flag.COMPOUND, pos);
	}

	final Set<BooleanTerm<A>> subTerms = new HashSet<>(0);

	@Override
	boolean accepts(A value) {
		switch (polarity) {
		case Pos:
			for (BooleanTerm<A> subExpr : this.subTerms) {
				if (!subExpr.accepts(value)) {
					return false;
				}
			}
			return true;
		case Neg:
			for (BooleanTerm<A> subExpr : this.subTerms) {
				if (subExpr.accepts(value)) {
					return true;
				}
			}
			return false;
		}
		throw new IllegalStateException();

	}

	@Override
	void negate() {
		this.polarity = polarity.inverse();
		for (BooleanTerm<A> sub : this.subTerms) {
			sub.polarity = polarity.inverse();
		}
	}

	@Override
	public String toString() {
		return "("
				+ StringUtils.join(this.subTerms,
						this.polarity == Polarity.Pos ? "&" : "|") + ")";
	}

}