package blom.effestee;

import org.junit.Test;

import junit.framework.Assert;
import blom.effestee.function.F1;
import blom.effestee.semiring.Pair;

public class TestUnion {

	static Fst<Pair<Character, Character>> a = new Fst<>();
	static Fst<Pair<Character, Character>> b = new Fst<>();

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
		
		Fst<Pair<Character, Character>> ab = new Fst<>();
		ab.inplaceUnion(a);
		ab.inplaceUnion(b);
		
		F1<Pair<Character,Character> , Character> fstP = F1.fstProj();

		System.out.println(ab);
		Assert.assertFalse(ab.acceptIn(fstP));
		Assert.assertTrue(ab.acceptIn(fstP,'a'));
		Assert.assertTrue(ab.acceptIn(fstP,'b'));
		Assert.assertFalse(ab.acceptIn(fstP,'a','b'));
	}
}
