package blom.effestee;

import junit.framework.Assert;

import org.junit.Test;

import blom.effestee.Fst.StateFlag;
import blom.effestee.semiring.Pair;

public class TestDeterminize {

	static Fst<Pair<Character, Character>> aaaaaa = new Fst<>();
	static Fst<Pair<Character, Character>> cdefg = new Fst<>();

	static {
		aaaaaa.inplaceUnion(Fst.fromString("a"));
		aaaaaa.inplaceUnion(Fst.fromString("a"));
		aaaaaa.inplaceUnion(Fst.fromString("a"));
		aaaaaa.inplaceUnion(Fst.fromString("a"));
		aaaaaa.inplaceUnion(Fst.fromString("a"));

		cdefg.inplaceUnion(Fst.fromString("b"));
		cdefg.inplaceUnion(Fst.fromString("b"));
		cdefg.inplaceUnion(Fst.fromString("b"));
		cdefg.inplaceUnion(Fst.fromString("b"));
		cdefg.inplaceUnion(Fst.fromString("b"));
	}

	@Test
	public void testDeterminize() {

		System.out.println(aaaaaa);

		Fst<Pair<Character, Character>> a = Fst.determinize(aaaaaa);

		System.out.println("a:\n" + a);

	}

}
