package blom.effestee.logic;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import blom.effestee.logic.BoolTerm.Flag;
import blom.effestee.logic.BoolTerm.Polarity;

class Compound<A> extends BoolTerm<A> {

	@SafeVarargs
	Compound(BoolTerm.Polarity p, BoolTerm<A> a, BoolTerm<A> b, BoolTerm<A>... elems) {

		super(Flag.COMPOUND, p);
		nodes.add(a);
		nodes.add(b);
		for (BoolTerm<A> e : elems) {
			nodes.add(e);
		}
	}

	final Set<BoolTerm<A>> nodes = new HashSet<>(0);

	@Override
	boolean accepts(A value) {
		switch (polarity) {
		case Pos:
			for (BoolTerm<A> subExpr : this.nodes) {
				if (!subExpr.accepts(value)) {
					return false;
				}
			}
			return true;
		case Neg:
			for (BoolTerm<A> subExpr : this.nodes) {
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
		for( BoolTerm<A> sub : this.nodes) {
			sub.polarity = polarity.inverse();
		}
	}

	@Override
	public String toString() {
		return "("+ StringUtils.join( this.nodes  , this.polarity == Polarity.Pos ? "&" : "|") +")";
	}

}