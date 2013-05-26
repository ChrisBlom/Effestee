package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.Fst.Label;
import blom.effestee.Fst.StateType;

public class TestConcat {

	static Fst<Character, Character> a = new Fst<>();
	static Fst<Character, Character> b = new Fst<>();

	static {
		a.addTransition(new Label('a', 'a'), a.addStateInitial(),
				a.addStateAccept());
		
		System.out.println(a);

		b.addTransition(new Label('b', 'b'), b.addStateInitial(),
				b.addStateAccept());

		System.out.println(b);

	}

	@Test
	public void testSingleton() {

		Assert.assertFalse(a.acceptIn());
		Assert.assertTrue(a.acceptIn('a'));
		
	}

	@Test
	public void testConcat() {
		
		Fst ab = new Fst();
		ab.addState(StateType.INITIAL,StateType.ACCEPT);

		System.out.println(ab);
		
		ab.concat(a);
		System.out.println(ab);
		
		ab.concat(b);
		
		
		System.out.println(ab);
		Assert.assertFalse(a.acceptIn());
		Assert.assertFalse(a.acceptIn('a'));
		Assert.assertTrue(a.acceptIn('a','b'));
	}
}
