package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.function.F1;
import blom.effestee.semiring.Pair;

public class TestConcat {

	static Fst<Pair<Character, Character>> a = new Fst<>();
	static Fst<Pair<Character, Character>> b = new Fst<>();
	static F1<Pair<Character, Character>, Character> fstP = F1.fstProj();

	static {
		a.addTransition(new Pair<>('a', 'a'), a.addStateInitial(),
				a.addStateAccept());

		System.out.println(a);

		b.addTransition(new Pair<>('b', 'b'), b.addStateInitial(),
				b.addStateAccept());

		System.out.println(b);

	}

	@Test
	public void testEmptyString() {
		Assert.assertFalse(Fst.fromString("123").acceptIn(fstP));
		Assert.assertTrue(Fst.fromString("").acceptIn(fstP));
	}

	@Test
	public void testSingleton() {

		Assert.assertFalse(a.acceptIn(fstP));
		Assert.assertTrue(a.acceptIn(fstP, 'a'));

	}

	@Test
	public void testConcat() {

		Fst ab = new Fst();
		ab.addState(Fst.StateFlag.INITIAL, Fst.StateFlag.ACCEPT);

		System.out.println(ab);

		ab.inplaceConcat(a);
		System.out.println(ab);

		ab.inplaceConcat(b);

		System.out.println(ab);
		Assert.assertFalse(ab.acceptIn(fstP));
		Assert.assertFalse(ab.acceptIn(fstP, 'a'));
		Assert.assertTrue(ab.acceptIn(fstP, 'a', 'b'));
	}
}
