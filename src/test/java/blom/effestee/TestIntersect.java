package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.function.F1;
import blom.effestee.semiring.Pair;

public class TestIntersect {

	
	static F1<Pair<Character,Character> , Character> fstP = F1.fstProj();

	static Fst<Pair<Character, Character>> abcde = new Fst<>();
	static Fst<Pair<Character, Character>> cdefg = new Fst<>();

	static {
		abcde.inplaceUnion(Fst.fromString("a"));
		abcde.inplaceUnion(Fst.fromString("b"));
		abcde.inplaceUnion(Fst.fromString("c"));
		abcde.inplaceUnion(Fst.fromString("d"));
		abcde.inplaceUnion(Fst.fromString("e"));

		cdefg.inplaceUnion(Fst.fromString("c"));
		cdefg.inplaceUnion(Fst.fromString("d"));
		cdefg.inplaceUnion(Fst.fromString("e"));
		cdefg.inplaceUnion(Fst.fromString("f"));
		cdefg.inplaceUnion(Fst.fromString("g"));
	}

	@Test
	public void testIntersectSingleStep() {

		System.out.println(abcde);
		for (char c : "abcde".toCharArray()) {
			Assert.assertTrue(abcde.acceptIn(fstP,c));
		}

		System.out.println(cdefg);
		for (char c : "cdefg".toCharArray()) {
			Assert.assertTrue(cdefg.acceptIn(fstP,c));
		}

		Fst<Pair<Character, Character>> cde = Fst.intersect(abcde, cdefg,fstP);
		System.out.println(cde);

		for (char c : "cde".toCharArray()) {
			Assert.assertTrue(cde.acceptIn(fstP,c));
		}

	}

	@Test
	public void testIntersectTwoStep() {

		Fst<Pair<Character, Character>> ab_xy = new Fst<>();
		ab_xy.inplaceUnion(Fst.fromString("ab"));
		ab_xy.inplaceUnion(Fst.fromString("xy"));
		System.out.println(ab_xy);

		Fst<Pair<Character, Character>> bc_xy = new Fst<>();
		bc_xy.inplaceUnion(Fst.fromString("bc"));
		bc_xy.inplaceUnion(Fst.fromString("xy"));
		System.out.println(bc_xy);

		Fst<Pair<Character, Character>> xy = Fst.intersect(ab_xy, bc_xy,fstP);
		System.out.println(xy);

		Assert.assertTrue(xy.acceptIn(fstP,'x','y'));

	}

}
