package blom.effestee;

import blom.effestee.semiring.SemiRing;
import blom.effestee.semiring.SemiRing.Val;

class Transition<L> implements Comparable<Transition<L>> {

	public final SemiRing<L>.Val label;

	public final Fst.State source;
	public final Fst.State target;

	public Transition(SemiRing<L>.Val val, Fst.State source, Fst.State target) {
		this.label = val;
		this.source = source;
		this.target = target;
	}

	@Override
	public String toString() {
		return String.format("%s -%s-> %s", source, label == null ? " --- "
				: label, target);
	}

	@Override
	public int compareTo(Transition<L> o) {

		int onSource = this.source.compareTo(o.source);

		if (onSource == 0) {
			return this.target.compareTo(o.target);
		} else {
			return onSource;
		}
	}


	
}