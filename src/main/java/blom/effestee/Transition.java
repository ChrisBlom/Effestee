package blom.effestee;

class Transition<L> implements Comparable<Transition<L>> {

	public final L label;

	public final Fst.State source;
	public final Fst.State target;

	public Transition(L label, Fst.State source, Fst.State target) {
		this.label = label;
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