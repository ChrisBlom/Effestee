package blom.effestee;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestConcat.class, TestDeterminize.class, TestIntersect.class,
		TestUnion.class })
public class TestSuite {

}
