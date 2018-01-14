package de.c_ebberg.openlasertag;

import android.app.Dialog;
import android.os.Handler;
import android.os.Message;
import android.widget.GridLayout;
import android.widget.ListView;

/**
 * Created by christian on 12.04.17.
 */

class Singleton {
    private static Singleton mInstance = null;

    private Handler game_logic_handler=null;
    private GameLogicThread game_logic_thread=null;
    private Handler serial_handler=null;
    private SerialThread serial_thread=null;
    private Handler game_activity_handler=null;
    private String current_user="";
    private String current_alias="";
    private Boolean serial_connected=false;
    private String user_files_path="";
    private Boolean sound_on=true;

    private GameActivityStatus gameActivityStatus=null;
    private TaggerConfigActivityStatus taggerConfigActivityStatus=null;
    private StatsActivityStatus statsActivityStatus=null;
    private GameFileLoader.GameInformation game_info=null;
    private LoadGameActivityStatus loadGameActivityStatus=null;

    public class GameActivityStatus{
        Boolean game_running=false;
        Boolean game_over=false;
        GameActivity.MessageList message_list=null;
        GameActivity.MessageAdapter message_adapter=null;
        GameActivity.ItemList item_list=null;
        Integer pre_countdown_status=-1;

        public GameActivityStatus(Boolean running, GameActivity.MessageList m_list, GameActivity.MessageAdapter m_adapter, GameActivity.ItemList i_list,Integer countdown_status){
            game_running = running;
            message_list = m_list;
            message_adapter = m_adapter;
            item_list = i_list;
            pre_countdown_status = countdown_status;
        }
    }

    public GameActivityStatus createNewGameActivityStatus(Boolean running, GameActivity.MessageList m_list, GameActivity.MessageAdapter m_adapter, GameActivity.ItemList i_list, Integer countdown_status){return new GameActivityStatus(running,m_list,m_adapter,i_list,countdown_status);}

    public class TaggerConfigActivityStatus{
        Integer active_tab=0;

        public TaggerConfigActivityStatus(Integer tab_index){
            active_tab = tab_index;
        }
    }

    public TaggerConfigActivityStatus createNewTaggerConfigActivityStatus(Integer tab_index){return new TaggerConfigActivityStatus(tab_index);}


    public class StatsActivityStatus{
        boolean state = false;
        String user_name="";
        String current_stats_name="";
    }

    public StatsActivityStatus createNewStatsActivityStatus(){return new StatsActivityStatus();}

    public class LoadGameActivityStatus{
        Integer active_tab=0;
        String current_folder;
        LoadGameActivityStatus(Integer tab, String _current_folder){active_tab=tab; current_folder=_current_folder;}
    }
    public LoadGameActivityStatus createNewLoadGameActivityStatus(Integer tab, String folder){return new LoadGameActivityStatus(tab, folder);}

    public static Singleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new Singleton();
        }
        return mInstance;
    }

    public Handler getSerialHandler(){
        return this.serial_handler;
    }

    public void setSerialHandler(Handler value){
        serial_handler = value;
    }

    public SerialThread getSerialThread(){return serial_thread;}

    public void setSerialThread(SerialThread thread){serial_thread = thread;}

    public Handler getGameLogicHandler(){
        return this.game_logic_handler;
    }

    public void setGameLogicHandler(Handler value){
        game_logic_handler = value;
    }

    public GameLogicThread getGameLogicThread(){return game_logic_thread;}

    public void setGameLogicThread(GameLogicThread thread){game_logic_thread = thread;}

    public Handler getGameActivityHandler(){ return this.game_activity_handler;}

    public void setGameActivityHandler(Handler value) { game_activity_handler = value; }

    public String getCurrentUser(){return current_user;}

    public void setCurrentUser(String user){current_user = user;}

    public String getCurrentAlias(){return current_alias;}

    public void setCurrentAlias(String alias){current_alias = alias;}

    public Boolean getSerialConnected(){return serial_connected;}

    public void setSerialConnected(Boolean connected){serial_connected = connected;}

    public String getUserFilesPath(){return user_files_path;}

    public void setUserFilesPath(String path){user_files_path = path;}

    public Boolean getSoundOn(){return sound_on;}

    public void setSoundOn(Boolean on){sound_on = on;}

    public GameActivityStatus getGameActivityStatus(){return gameActivityStatus;}

    public void setGameActivityStatus(GameActivityStatus status){gameActivityStatus = status;}

    public TaggerConfigActivityStatus getTaggerConfigActivityStatus(){return taggerConfigActivityStatus;}

    public void setTaggerConfigActivityStatus(TaggerConfigActivityStatus status){taggerConfigActivityStatus = status;}

    public StatsActivityStatus getStatsActivityStatus(){return statsActivityStatus;}

    public void setStatsActivityStatus(StatsActivityStatus status){statsActivityStatus = status;}

    public GameFileLoader.GameInformation getGameInfo(){return game_info;}

    public void setGameInfo(GameFileLoader.GameInformation info){game_info = info;}

    public LoadGameActivityStatus getLoadGameActivityStatus(){return loadGameActivityStatus;}

    public void setLoadGameActivityStatus(LoadGameActivityStatus status){loadGameActivityStatus = status;}

}
