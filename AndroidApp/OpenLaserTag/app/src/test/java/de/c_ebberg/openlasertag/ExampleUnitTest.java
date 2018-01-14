package de.c_ebberg.openlasertag;

import android.support.v4.media.MediaMetadataCompat;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test_eval_string_statement_literals() throws Exception {
        String teststring = "hallo+' ich bins '+  hiervorsindkeineLeerzeichen  +'    hier schon'";
        assertEquals("teststring '"+teststring+"' was evaluated to '"+ GameFileLoader.eval_string_expression(teststring,new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, GameFileLoader.Definition>(),new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, Double>()),"hallo ich bins hiervorsindkeineLeerzeichen    hier schon",GameFileLoader.eval_string_expression(teststring,new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, GameFileLoader.Definition>(),new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, Double>()));
    }
}