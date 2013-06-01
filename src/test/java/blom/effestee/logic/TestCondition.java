package blom.effestee.logic;

import static blom.effestee.logic.BoolTerm.and;
import static blom.effestee.logic.BoolTerm.or;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestCondition {

	@Test
	public void test() {
		BoolTerm<Character> isA = BoolTerm.is('a');
		BoolTerm<Character> isB = BoolTerm.is('b');
		BoolTerm<Character> notA = BoolTerm.notIs('a');
		
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
			assertFalse(BoolTerm.and(isA, isB).accepts(c));
		}


		System.out.println(and( isA, or(isA, isA,isA,isA)));
		
	}

}
