package de.c_ebberg.openlasertag;

import org.junit.Test;

import static org.junit.Assert.*;
import de.c_ebberg.openlasertag.StatsManager.*;

/**
 * Created by christian on 12.05.17.
 */

public class StatsManagerTests {
    @Test
    public void GameStatDecodeEncodeTest() throws Exception {
        String test_data = "1{Deaths[(4.9E-324;900)(1.0;40)(1.0;70)(1.0;90)]Damage[(4.9E-324;900)(50.0;10)(100.0;33)(60.0;66)(5.0;100)]}2{InvokedItems[(4.9E-324;900)(1.0;5)(1.0;25)]}3{FiredShots[(4.9E-324;900)(50.0;40)(40.0;50)(3.0;60)(3.0;90)]}";
        StatsManager.getInstance().DecodeStatsCollectionFromString(test_data);
        String encoded_data = StatsManager.getInstance().EncodeStatsCollectionToString();
        assertEquals("GameStat Decode Encode Test failed",test_data,encoded_data);
    }
}
