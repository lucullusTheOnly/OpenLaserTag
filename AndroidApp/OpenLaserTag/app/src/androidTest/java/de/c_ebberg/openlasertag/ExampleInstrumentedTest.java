package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("de.c_ebberg.openlasertag", appContext.getPackageName());
    }

    @Test
    public void TestSharedPrefsContainCurrentUser() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        assertEquals(true,sharedPref.contains("CurrentUser"));
    }

    @Test
    public void test_getExponent(){
        Context appContext = InstrumentationRegistry.getTargetContext();
        StatsDiagramView v = new StatsDiagramView(appContext);
        Double[] test_values = {0.34, 1.998, 324.503, 1000.0, 999.9};
        Double[] exponents = {0.1, 1.0, 100.0, 1000.0, 100.0};
        for(Integer i=0;i<test_values.length;i++){
            assertEquals(exponents[i], v.axisManager.getExponent(test_values[i]));
        }
    }

    @Test
    public void test_formatTimeForTick(){
        Context appContext = InstrumentationRegistry.getTargetContext();
        StatsDiagramView v = new StatsDiagramView(appContext);
        Integer[] test_values = {4, 60, 61, 119, 120, 60*60, 60*60+60, 60*60*24-60*60, 60*60*24, 60*60*24+60*60};
        String[] strings = {"4s","1m","1s","59s","2m","1h","1m","23h","1d","1h"};
        for(Integer i=0;i<test_values.length;i++){
            assertEquals("TimeTickText("+i.toString()+") was '"+v.axisManager.formatTimeForTick(test_values[i])+"' instead of '"+strings[i]+"'",true, v.axisManager.formatTimeForTick(test_values[i]).equals(strings[i]));
        }
        //assertEquals("TimeTickText was '"+v.axisManager.formatTimeForTick(test_values[1])+"' instead of '"+strings[1]+"'",true, v.axisManager.formatTimeForTick(test_values[1]).equals(strings[1]));
    }

    @Test
    public void test_eval_string_statement_variable() throws Exception {
        String teststring = "hallo+' testliteral '+testvariable";
        HashMap<String,GameFileLoader.GameVariable> var = new HashMap<>();

        GameFileLoader loader = new GameFileLoader(InstrumentationRegistry.getTargetContext());
        GameFileLoader.GameVariable v = loader.new_game_variable();
        v.type = "string";
        v.value = "Dies ist der Inhalt der Testvariable";
        var.put("testvariable",v);

        assertEquals("teststring '"+teststring+"' was evaluated to '"+ GameFileLoader.eval_string_expression(teststring,var,new HashMap<String, GameFileLoader.Definition>(),new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, Double>()),"hallo testliteral Dies ist der Inhalt der Testvariable",GameFileLoader.eval_string_expression(teststring,var,new HashMap<String, GameFileLoader.Definition>(),new HashMap<String, GameFileLoader.GameVariable>(),new HashMap<String, Double>()));
    }
}
