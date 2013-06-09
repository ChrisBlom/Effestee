package blom.effestee.semiring;

import java.util.List;

public abstract class SemiRing<A> {

	public abstract A zero();

	public abstract A one();

	public abstract A plus(A x, A y);

	public abstract A times(A x, A y);
	
	public A product(List<A> vals ) {
		A product = one();
		for( A val : vals) {
			product = times( product , val);
		}
		return product;
	}
	
	public A sum(List<A> vals ) {
		A sum = zero();
		for( A val : vals) {
			sum = plus( sum , val);
		}
		return sum;
	}

	public SemiRing<A>.Val wrap(A val) {
		return new Val(val);

	}

	public class Val {

		public final SemiRing<A> ring = SemiRing.this;
		public final A value;

		public Val(A value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value.toString();
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
			Val other = (Val) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

	}

}
