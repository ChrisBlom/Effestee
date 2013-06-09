package blom.effestee;

import org.junit.Test;

import junit.framework.Assert;
import blom.effestee.semiring.Pair;

public class TestUnion {

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
