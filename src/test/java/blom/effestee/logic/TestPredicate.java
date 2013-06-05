package blom.effestee.logic;

import static blom.effestee.logic.BooleanTerm.and;
import static blom.effestee.logic.BooleanTerm.or;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestPredicate {

	@Test
	public void test() {
		BooleanTerm<Character> isA = BooleanTerm.is('a');
		BooleanTerm<Character> isB = BooleanTerm.is('b');
		BooleanTerm<Character> notA = BooleanTerm.notIs('a');
		
		assertTrue(isA.accepts('a'));
		assertFalse(notA.accepts('a'));
		
		for (char c : "bcde".toCharArray()) {
			assertTrue(notA.accepts(c));
		}
		
		System.out.println(isA);

		for (char c : "ab".toCharArray()) {
			assertTrue(or(isA, isB).accepts(c));
		}

		System.out.println(or(isA, isB));

		for (char c : "ab".toCharArray()) {
			assertFalse(BooleanTerm.and(isA, isB).accepts(c));
		}

		System.out.println(and( isA, or(isA, isA,isA,isA)));
		
	}

}
