package systems.symbol.util;

import org.junit.jupiter.api.Test;

public class StepsTest {
    String ONE_STEP_2 = "test";
    String ONE_STEP_3 = "example";
    String TWO_STEP = "test/example";
    String TWO_STEP_EXT = "test/example.test";
    String THREE_STEP = "test/example/oops";
    String TWO_STEP_URL = "http://localhost/test//example//#oops";
    String EXT_STEP_URL = "http://localhost/test//example.test";

    // @Test
    // public void testNormalize() {
    // assert Steps.normalize(TWO_STEP_EXT,false).equals(TWO_STEP_EXT);
    // assert Steps.normalize(TWO_STEP_URL,true).equals(THREE_STEP);
    // assert Steps.normalize(TWO_STEP_URL,false).equals(THREE_STEP);
    // }

    @Test
    public void testParse() {
        Steps steps = new Steps();
        assert steps.parse(TWO_STEP_EXT, false).size() == 2;
        assert steps.parse(TWO_STEP_URL, false).size() == 4;
    }

    @Test
    public void testBack() {
        Steps steps = new Steps(TWO_STEP_EXT);
        steps.back();
        assert steps.size() == 1;
        assert steps.toString().equals(ONE_STEP_2);
    }

    @Test
    public void testStep() {
        Steps steps = new Steps(TWO_STEP);
        assert steps.size() == 2;
        assert steps.step().equals(ONE_STEP_2);
        assert steps.step().equals(ONE_STEP_3);
        assert steps.step().isEmpty();
    }

    @Test
    public void testToExtension() {
        Steps steps = new Steps(EXT_STEP_URL);
        assert steps.getExtension().equals("test");
    }

    @Test
    public void testToString() {
        Steps steps = new Steps(TWO_STEP_URL, true);
        assert steps.toString().equals("test/example/#oops");
    }

}