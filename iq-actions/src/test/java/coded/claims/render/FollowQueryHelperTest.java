package systems.symbol.render;

import systems.symbol.iq.AbstractIQTest;
import systems.symbol.rdf4j.iq.KBMS;
import org.testng.annotations.Test;

public class FollowQueryHelperTest extends AbstractIQTest {

    @Test
    public void testStuff() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        TripleRenderer renderer = new TripleRenderer(kbms);

    }

}