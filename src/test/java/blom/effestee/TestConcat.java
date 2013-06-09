package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.Fst.StateFlag;
import blom.effestee.semiring.Pair;

public class TestConcat {

	static Fst<Character, Character> a = new Fst<>();
	static Fst<Character, Character> b = new Fst<>();

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
		Assert.assertFalse(Fst.fromString("123").acceptIn());
		Assert.assertTrue(Fst.fromString("").acceptIn());
	}

	@Test
	public void testSingleton() {

		Assert.assertFalse(a.acceptIn());
		Assert.assertTrue(a.acceptIn('a'));

	}

	@Test
	public void testConcat() {

		Fst ab = new Fst();
		ab.addState(StateFlag.INITIAL, StateFlag.ACCEPT);

		System.out.println(ab);

		ab.inplaceConcat(a);
		System.out.println(ab);

		ab.inplaceConcat(b);
		
		System.out.println(ab);
		Assert.assertFalse(ab.acceptIn());
		Assert.assertFalse(ab.acceptIn('a'));
		Assert.assertTrue(ab.acceptIn('a', 'b'));
	}
}
