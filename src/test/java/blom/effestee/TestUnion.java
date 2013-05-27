package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.Fst.Label;
import blom.effestee.Fst.StateType;

public class TestUnion {

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
	public void testUnion() {
		
		Fst<Character, Character> ab = new Fst<>();

		ab.inplaceUnion(a);
		ab.inplaceUnion(b);
		
		System.out.println(ab);
		Assert.assertFalse(ab.acceptIn());
		Assert.assertTrue(ab.acceptIn('a'));
		Assert.assertTrue(ab.acceptIn('b'));
		Assert.assertFalse(ab.acceptIn('a','b'));
	}
}
