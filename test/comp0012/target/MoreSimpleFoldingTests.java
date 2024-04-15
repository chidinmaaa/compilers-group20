package comp0012.target;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

//more tests for simple folding
public class MoreSimpleFoldingTests {

    MoreSimpleFolding msf = new MoreSimpleFolding();
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    
    @Before
    public void setUpStreams()
    {
        System.setOut(new PrintStream(outContent));
    }
    
    @After
    public void cleanUpStreams()
    {
        System.setOut(null);
    }

    @Test
    public void testSubDouble(){
        msf.simpleSubDouble();
        assertEquals("3.0\r\n", outContent.toString());
    }

    @Test
    public void testMulFloat(){
        msf.simpleMulFloat();
        assertEquals("2125.8025\r\n", outContent.toString());
    }
    @Test
    public void testDivLong(){
        msf.simpleDivLong();
        assertEquals("1\r\n", outContent.toString());
    }
}
