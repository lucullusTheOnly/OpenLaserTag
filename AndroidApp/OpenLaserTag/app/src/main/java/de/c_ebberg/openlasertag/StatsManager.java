package de.c_ebberg.openlasertag;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by christian on 05.05.17.
 */

public class StatsManager {
    private static final String LOG_TAG = "StatsManager";
    private static StatsManager mInstance = null;

    public static StatsManager getInstance(){
        if(mInstance == null)
        {
            mInstance = new StatsManager();
        }
        return mInstance;
    }

    // Class Definitions

    public class Stat_Point {
        Double value;
        Integer time; // in seconds

        public Stat_Point(Double _value, Integer _time){value = _value; time = _time;}
    }

    public class Stat{
        private ArrayList<Stat_Point> list;
        private Double total=0.0;
        private Double average=0.0;
        private Integer duration;
        private Integer total_games = 1;

        public Stat(){list = new ArrayList<>();}

        public Double getTotal(){return total;}
        public Double getAverage(){return average;}
        public Integer getTotalGames(){return total_games;}
        public ArrayList<Stat_Point> getPoints(){return new ArrayList<>(list);}
        public Integer getDuration(){return duration;}
    }

    public class Game_Stat {
        private Integer game_number;
        private Integer game_duration;
        private HashMap<String, Stat> game_stats=new HashMap<>();

        Game_Stat(Integer _game_number, Integer _game_duration){
            game_number = _game_number; game_duration = _game_duration;
        }

        Game_Stat(Integer _game_number){game_number = _game_number;}

        Boolean InitStat(String name){
            if(game_stats.containsKey(name)){
                return false;
            }
            Stat temp = new Stat();
            temp.duration = game_duration;
            game_stats.put(name, temp);
            return true;
        }

        Boolean AddPoint(String stat_name, Double value, Integer time){
            if(!game_stats.containsKey(stat_name)){
                return false;
            }
            game_stats.get(stat_name).list.add(new Stat_Point(value, time));
            return true;
        }

        String EncodeToString(){
            // Deaths[(value;time)(value;time)]Damage[...]
            String data="";
            for(String key : game_stats.keySet()){
                data += key + "[";
                ArrayList<Stat_Point> list = game_stats.get(key).list;
                data += "(" + Double.valueOf(Double.MIN_VALUE).toString() + ";" + game_duration.toString() + ")";
                for(Stat_Point point : list){
                    data += "(" + point.value.toString() + ";" + point.time.toString() + ")";
                }
                data += "]";
            }

            return data;
        }

        void DecodeFromString(String data) throws IllegalArgumentException{
            game_stats.clear();

            String value="";
            Integer pos = 0;
            Integer state = 0;
            String stat_name = "";
            Stat stat = new Stat();
            Double point_value=0.0;
            Integer point_time;
            boolean first_point = true;
            boolean first_stat = true;
            while(pos<data.length()){
                switch(state){
                    case 0: //stats
                        if(data.charAt(pos)=='['){state=1; stat = new Stat(); stat_name = value; value=""; break;}
                        value+=data.charAt(pos);
                        break;
                    case 1: //points
                        if(data.charAt(pos)=='('){state=2; break;}
                        if(data.charAt(pos)==']'){state=0; game_stats.put(stat_name,stat); first_point=true; break;}
                        break;
                    case 2: // value
                        if(data.charAt(pos)==';'){state=3; point_value = Double.parseDouble(value); value=""; break;}
                        value+=data.charAt(pos);
                        break;
                    case 3: // time
                        if(data.charAt(pos)==')'){
                            state=1;
                            point_time = Integer.parseInt(value);
                            value = "";
                            if(first_point){
                                if(first_stat) {
                                    game_duration = point_time;
                                    stat.duration = game_duration;
                                    first_point = false;
                                    first_stat = false;
                                } else{
                                    if(!game_duration.equals(point_time)){
                                        throw new IllegalArgumentException("Different game times in Stats for the same game: game_duration="+game_duration.toString()+"  stat_duration="+point_time.toString());
                                    } else {
                                        stat.duration = game_duration;
                                        first_point = false;
                                    }
                                }
                            } else {
                                stat.list.add(new Stat_Point(point_value, point_time));
                            }
                            break;
                        }
                        value+=data.charAt(pos);
                        break;
                }
                pos++;
            }

            // Calculate total and average
            for(Stat s : game_stats.values()){
                Double total=0.0;
                for(Stat_Point p : s.list){
                    total += p.value;
                }
                s.average = total / (game_duration/60.0); // average per minute
                s.total = total;
            }
        }

        public Integer getGameDuration(){return game_duration;}
        public Set<String> getStatNameSet(){
            return game_stats.keySet();
        }
    }


    // Variable Declaration
    private HashMap<Integer, Game_Stat> game_stats_collection;
    private Integer collection_length=-1;
    private String current_user="";
    private Game_Stat current_game_stat=null;
    private Double average_unit=60.0;

    // Method Definitions
    public Boolean InitStat(String name){
        if(current_game_stat==null) return false;
        return current_game_stat.InitStat(name);
    }

    public Integer InitGameStats(String user_name, Integer game_duration, Context context)throws IOException{
        Integer num = getStatsCollectionLength(user_name, context);
        current_game_stat = new Game_Stat(num, game_duration);
        current_user = user_name;
        collection_length = num;
        return num;
    }

    public Boolean IsCurrentGameStatSet(){
        return current_game_stat!=null;
    }

    public Boolean AbortCurrentGameStats(){
        if(current_game_stat== null) return false;
        current_game_stat = null;
        return true;
    }

    public void CommitCurrentGameStats(Context context) throws IOException{
        if(current_game_stat == null) return;
        FileInputStream in_stream = context.openFileInput("StatsCollection_"+current_user+".txt");
        InputStreamReader InputRead= new InputStreamReader(in_stream);
        char[] inputBuffer= new char[512];
        String read_data="";
        int charRead;
        while ((charRead=InputRead.read(inputBuffer))>0) {
            // char to string conversion
            read_data +=String.copyValueOf(inputBuffer,0,charRead);
        }
        InputRead.close();
        if(!read_data.contains("|")){
            throw new RuntimeException("StatsCollection File of User "+current_user +" is corrupt! "+read_data);
        }
        String out_data = Integer.valueOf(collection_length+1).toString()
                + read_data.substring(read_data.indexOf("|"))
                + current_game_stat.game_number.toString()+"{" + current_game_stat.EncodeToString() + "}";

        FileOutputStream stream;
        stream = context.openFileOutput("StatsCollection_"+current_user+".txt",Context.MODE_PRIVATE);
        OutputStreamWriter outputWriter=new OutputStreamWriter(stream);
        outputWriter.write(out_data);
        outputWriter.close();
        current_game_stat = null;
        collection_length++;
    }

    public void LoadUserStatsCollection(String user_name, Context context) throws IOException{
        FileOutputStream stream;
        FileInputStream in_stream;
        OutputStreamWriter outputWriter;
        try{
            in_stream = context.openFileInput("StatsCollection_"+user_name+".txt");
        } catch(FileNotFoundException e){
            Log.i(LOG_TAG,"File not existing. Creating new StatsCollection for user "+user_name);
            stream = context.openFileOutput("StatsCollection_" + user_name + ".txt", Context.MODE_PRIVATE);
            outputWriter=new OutputStreamWriter(stream);
            outputWriter.write("0|");
            outputWriter.close();
            game_stats_collection = new HashMap<>();
            collection_length = 0;
            return;
        }

        String file_content="";
        InputStreamReader InputRead= new InputStreamReader(in_stream);
        char[] inputBuffer= new char[512];
        int charRead;
        while ((charRead=InputRead.read(inputBuffer))>0) {
            // char to string conversion
            file_content +=String.copyValueOf(inputBuffer,0,charRead);
        }
        InputRead.close();
        if(!file_content.contains("|")){
            throw new RuntimeException("StatsCollection File of User "+user_name +" is corrupt! ");
        }
        collection_length = Integer.parseInt(file_content.substring(0,file_content.indexOf("|")));
        current_user = user_name;
        current_game_stat = null;
        DecodeStatsCollectionFromString(file_content.substring(file_content.indexOf("|")+1));
    }

    public void UnloadUserStatsCollection(){
        collection_length = -1;
        current_user = "";
        game_stats_collection = null;
        current_game_stat = null;
    }

    public void RemoveUserStatsCollection(String user, Context context) throws IOException{
        context.deleteFile("StatsCollection_"+user+".txt");
        if(current_user.equals(user)){
            current_user = "";
            collection_length = -1;
            current_game_stat = null;
            game_stats_collection = null;
        }
    }

    public HashMap<Integer,Game_Stat> getUserStatsCollection(){
        return game_stats_collection;
    }

    public Game_Stat getCurrentGameStats(){
        return current_game_stat;
    }

    public HashMap<String,Stat> getOverallStatsMapWithoutPoints(){
        if(game_stats_collection==null) return null;
        HashMap<String,ArrayList<Stat>> stat_col = new HashMap<>();
        for(Integer i : game_stats_collection.keySet()){
            for(String stat_key : game_stats_collection.get(i).game_stats.keySet()){
                if(stat_col.keySet().contains(stat_key)){
                    stat_col.get(stat_key).add(game_stats_collection.get(i).game_stats.get(stat_key));
                } else {
                    ArrayList<Stat> temp = new ArrayList<>();
                    temp.add(game_stats_collection.get(i).game_stats.get(stat_key));
                    stat_col.put(stat_key, temp);
                }
            }
        }

        // calculate overall total and average for each stat
        HashMap<String, Stat> overall_map = new HashMap<>();
        for(String key : stat_col.keySet()){
            Double total=0.0;
            Integer total_duration=0;
            Integer games = 0;
            for(Stat stat : stat_col.get(key)){
                total += stat.getTotal();
                total_duration += stat.duration;
                games++;
            }
            Stat temp = new Stat();
            temp.total = total;
            temp.average = total / total_duration.doubleValue() * average_unit;
            temp.total_games = games;
            overall_map.put(key,temp);
        }
        return overall_map;
    }

    public Boolean AddPointToCurrentStat(String name, Double value, Integer time){
        if(current_game_stat == null) return false;
        return current_game_stat.AddPoint(name, value, time);
    }

    protected String EncodeStatsCollectionToString(){
        String data="";
        if(game_stats_collection==null) return data;
        for(Integer game_num : game_stats_collection.keySet()){
            String en = game_stats_collection.get(game_num).EncodeToString();
            data += game_num.toString() + "{" + en + "}";
        }
        return data;
    }

    protected void DecodeStatsCollectionFromString(String data){
        game_stats_collection = new HashMap<>();
        Integer pos = 0;
        String value = "";
        while(pos<data.length()){
            if(data.charAt(pos)=='{'){
                if(data.indexOf("}",pos)==-1 || data.length()<=pos+2) throw new IllegalArgumentException("Uneven parenthesis '}'");
                Integer game_num = Integer.parseInt(value);
                value="";
                Game_Stat new_stat = new Game_Stat(game_num);
                game_stats_collection.put(game_num,new_stat);
                new_stat.DecodeFromString(data.substring(pos+1,data.indexOf("}",pos)));
                pos = data.indexOf("}",pos)+1;
                continue;
            }
            value+=data.charAt(pos);
            pos++;
        }
    }

    public Integer getStatsCollectionLength(String user_name, Context context) throws IOException{
        FileInputStream in_stream;
        try {
            in_stream = context.openFileInput("StatsCollection_" + user_name + ".txt");
        } catch(FileNotFoundException e){
            FileOutputStream out_stream = context.openFileOutput("StatsCollection_" + user_name + ".txt",Context.MODE_PRIVATE);
            OutputStreamWriter outputWriter=new OutputStreamWriter(out_stream);
            outputWriter.write("0|");
            outputWriter.close();
            return 0;
        }

        InputStreamReader InputRead= new InputStreamReader(in_stream);
        char[] inputBuffer= new char[32];
        String read_data="";
        int charRead;
        while ((charRead=InputRead.read(inputBuffer))>0 && !read_data.contains("|")) {
            // char to string conversion
            read_data +=String.copyValueOf(inputBuffer,0,charRead);
        }
        InputRead.close();
        return Integer.parseInt(read_data.substring(0,read_data.indexOf("|")));
    }

    public void InitDefaultGameStats(){
        InitStat("Deaths");
        InitStat("Damage");
        InitStat("FiredShots");
        InitStat("ReceivedShots");
        InitStat("InvokedItems");
    }

    public HashMap<Integer,Stat> getAllStatOccurences(String stat_name){
        if(game_stats_collection==null) return null;
        HashMap<Integer, Stat> map = new HashMap<>();
        for(Game_Stat g : game_stats_collection.values()){
            if(g.game_stats.containsKey(stat_name)){
                map.put(g.game_number,g.game_stats.get(stat_name));
            }
        }
        return map;
    }

    public void test_de_and_encode(){
        String data = "1{Deaths[(1.0;40)(1.0;70)(1.0;90)]Damage[(50.0;10)(100.0;33)(60.0;66)(5.0;100)]}2{InvokedItems[(1.0;5)(1.0;25)]}3{FiredShots[(50.0;40)(40.0;50)(3.0;60)(3.0;90)]}";
        Log.i(LOG_TAG,"De- and Encode Test with string: '"+data+"'");
        DecodeStatsCollectionFromString(data);
        Log.i(LOG_TAG,"TestStats: "+EncodeStatsCollectionToString());
    }

    public void test_data_save_and_read(Context context) throws IOException{
        String data = "4|1{Deaths[(1.0;40)(1.0;70)(1.0;90)]Damage[(50.0;10)(100.0;33)(60.0;66)(5.0;100)]}2{InvokedItems[(1.0;5)(1.0;25)]}3{FiredShots[(50.0;40)(40.0;50)(3.0;60)(3.0;90)]}";
        Log.i(LOG_TAG,"SaveReadTest  in: '"+data+"'");
        FileOutputStream stream;
        stream = context.openFileOutput("testfile.txt",Context.MODE_PRIVATE);
        OutputStreamWriter outputWriter=new OutputStreamWriter(stream);
        outputWriter.write(data);
        outputWriter.close();

        FileInputStream in_stream = context.openFileInput("testfile.txt");
        InputStreamReader InputRead= new InputStreamReader(in_stream);
        char[] inputBuffer= new char[512];
        String read_data="";
        int charRead;
        while ((charRead=InputRead.read(inputBuffer))>0) {
            // char to string conversion
            read_data +=String.copyValueOf(inputBuffer,0,charRead);
        }
        InputRead.close();
        Log.i(LOG_TAG,"SaveReadTest out: '"+read_data+"'");
    }

    public void write_test_data_to_user_file(String user_name, Context context){
        game_stats_collection = new HashMap<>();
        HashMap<Integer, Stat> test_data = test_stats_detail_view_with_hashmap();
        Integer max_game=0;
        for(Integer i : test_data.keySet()){
            if(i>max_game) max_game = i;
        }

        for(Integer i=0; i<=max_game;i++){
            if(test_data.containsKey(i)){
                game_stats_collection.put(i,new Game_Stat(i, test_data.get(i).duration));
                game_stats_collection.get(i).game_stats.put("TestStat", test_data.get(i));
            } else {
                game_stats_collection.put(i,new Game_Stat(i,10));
            }
        }
        collection_length = max_game+1;
        current_user = user_name;

        try {
            FileOutputStream stream = context.openFileOutput("StatsCollection_" + user_name + ".txt", Context.MODE_PRIVATE);
            OutputStreamWriter outputWriter=new OutputStreamWriter(stream);
            outputWriter.write(collection_length.toString()+"|");
            outputWriter.write(EncodeStatsCollectionToString());
            outputWriter.close();

        } catch(IOException e){
            Log.e(LOG_TAG,"IOException during write_test_data_to_user_file");
        }
    }

    public HashMap<Integer,Stat> test_stats_detail_view_with_hashmap(){
        HashMap<Integer,Stat> map = new HashMap<>();
        Stat temp = new Stat();
        temp.duration = 1200;
        temp.list.add(new Stat_Point(3.0,2));
        temp.list.add(new Stat_Point(3.0,3));
        temp.list.add(new Stat_Point(3.0,4));
        temp.list.add(new Stat_Point(3.0,6));
        temp.list.add(new Stat_Point(3.0,10));
        temp.list.add(new Stat_Point(3.0,14));
        temp.list.add(new Stat_Point(3.0,18));
        temp.list.add(new Stat_Point(3.0,22));
        temp.list.add(new Stat_Point(3.0,26));
        temp.list.add(new Stat_Point(3.0,30));
        temp.list.add(new Stat_Point(3.0,34));
        temp.list.add(new Stat_Point(3.0,41));
        temp.list.add(new Stat_Point(3.0,43));
        temp.list.add(new Stat_Point(3.0,44));
        temp.list.add(new Stat_Point(3.0,54));
        temp.list.add(new Stat_Point(3.0,55));
        temp.list.add(new Stat_Point(3.0,59));
        temp.list.add(new Stat_Point(0.0,60));
        temp.list.add(new Stat_Point(3.0,62));
        temp.list.add(new Stat_Point(3.0,63));
        temp.list.add(new Stat_Point(3.0,64));
        temp.list.add(new Stat_Point(3.0,66));
        temp.list.add(new Stat_Point(3.0,70));
        temp.list.add(new Stat_Point(3.0,74));
        temp.list.add(new Stat_Point(3.0,78));
        temp.list.add(new Stat_Point(3.0,82));
        temp.list.add(new Stat_Point(3.0,86));
        temp.list.add(new Stat_Point(3.0,90));
        temp.list.add(new Stat_Point(3.0,94));
        temp.list.add(new Stat_Point(3.0,101));
        temp.list.add(new Stat_Point(3.0,103));
        temp.list.add(new Stat_Point(3.0,104));
        temp.list.add(new Stat_Point(3.0,114));
        temp.list.add(new Stat_Point(3.0,115));
        temp.list.add(new Stat_Point(3.0,119));
        temp.list.add(new Stat_Point(0.0,120));
        temp.list.add(new Stat_Point(3.0,122));
        temp.list.add(new Stat_Point(3.0,123));
        temp.list.add(new Stat_Point(3.0,124));
        temp.list.add(new Stat_Point(3.0,126));
        temp.list.add(new Stat_Point(3.0,130));
        temp.list.add(new Stat_Point(3.0,134));
        temp.list.add(new Stat_Point(3.0,138));
        temp.list.add(new Stat_Point(3.0,142));
        temp.list.add(new Stat_Point(3.0,146));
        temp.list.add(new Stat_Point(3.0,150));
        temp.list.add(new Stat_Point(3.0,154));
        temp.list.add(new Stat_Point(3.0,161));
        temp.list.add(new Stat_Point(3.0,163));
        temp.list.add(new Stat_Point(3.0,164));
        temp.list.add(new Stat_Point(3.0,174));
        temp.list.add(new Stat_Point(3.0,175));
        temp.list.add(new Stat_Point(3.0,179));
        temp.list.add(new Stat_Point(0.0,180));
        temp.list.add(new Stat_Point(3.0,182));
        temp.list.add(new Stat_Point(3.0,183));
        temp.list.add(new Stat_Point(3.0,184));
        temp.list.add(new Stat_Point(3.0,186));
        temp.list.add(new Stat_Point(3.0,190));
        temp.list.add(new Stat_Point(3.0,194));
        temp.list.add(new Stat_Point(3.0,198));
        temp.list.add(new Stat_Point(3.0,202));
        temp.list.add(new Stat_Point(3.0,206));
        temp.list.add(new Stat_Point(3.0,210));
        temp.list.add(new Stat_Point(3.0,214));
        temp.list.add(new Stat_Point(3.0,221));
        temp.list.add(new Stat_Point(3.0,223));
        temp.list.add(new Stat_Point(3.0,224));
        temp.list.add(new Stat_Point(3.0,234));
        temp.list.add(new Stat_Point(3.0,235));
        temp.list.add(new Stat_Point(3.0,239));
        temp.list.add(new Stat_Point(0.0,240));
        temp.list.add(new Stat_Point(3.0,242));
        temp.list.add(new Stat_Point(3.0,243));
        temp.list.add(new Stat_Point(3.0,244));
        temp.list.add(new Stat_Point(3.0,246));
        temp.list.add(new Stat_Point(3.0,250));
        temp.list.add(new Stat_Point(3.0,254));
        temp.list.add(new Stat_Point(3.0,258));
        temp.list.add(new Stat_Point(3.0,262));
        temp.list.add(new Stat_Point(3.0,266));
        temp.list.add(new Stat_Point(3.0,270));
        temp.list.add(new Stat_Point(3.0,274));
        temp.list.add(new Stat_Point(3.0,281));
        temp.list.add(new Stat_Point(3.0,283));
        temp.list.add(new Stat_Point(3.0,284));
        temp.list.add(new Stat_Point(3.0,294));
        temp.list.add(new Stat_Point(3.0,295));
        temp.list.add(new Stat_Point(3.0,299));
        temp.list.add(new Stat_Point(0.0,300));
        temp.list.add(new Stat_Point(3.0,302));
        temp.list.add(new Stat_Point(3.0,303));
        temp.list.add(new Stat_Point(3.0,304));
        temp.list.add(new Stat_Point(3.0,306));
        temp.list.add(new Stat_Point(3.0,310));
        temp.list.add(new Stat_Point(3.0,314));
        temp.list.add(new Stat_Point(3.0,318));
        temp.list.add(new Stat_Point(3.0,322));
        temp.list.add(new Stat_Point(3.0,326));
        temp.list.add(new Stat_Point(3.0,330));
        temp.list.add(new Stat_Point(3.0,334));
        temp.list.add(new Stat_Point(3.0,341));
        temp.list.add(new Stat_Point(3.0,343));
        temp.list.add(new Stat_Point(3.0,344));
        temp.list.add(new Stat_Point(3.0,354));
        temp.list.add(new Stat_Point(3.0,355));
        temp.list.add(new Stat_Point(3.0,359));
        temp.list.add(new Stat_Point(0.0,360));
        temp.list.add(new Stat_Point(3.0,362));
        temp.list.add(new Stat_Point(3.0,363));
        temp.list.add(new Stat_Point(3.0,364));
        temp.list.add(new Stat_Point(3.0,366));
        temp.list.add(new Stat_Point(3.0,370));
        temp.list.add(new Stat_Point(3.0,374));
        temp.list.add(new Stat_Point(3.0,378));
        temp.list.add(new Stat_Point(3.0,382));
        temp.list.add(new Stat_Point(3.0,386));
        temp.list.add(new Stat_Point(3.0,390));
        temp.list.add(new Stat_Point(3.0,394));
        temp.list.add(new Stat_Point(3.0,401));
        temp.list.add(new Stat_Point(3.0,403));
        temp.list.add(new Stat_Point(3.0,404));
        temp.list.add(new Stat_Point(3.0,414));
        temp.list.add(new Stat_Point(3.0,415));
        temp.list.add(new Stat_Point(3.0,419));
        temp.list.add(new Stat_Point(0.0,420));
        temp.list.add(new Stat_Point(3.0,422));
        temp.list.add(new Stat_Point(3.0,423));
        temp.list.add(new Stat_Point(3.0,424));
        temp.list.add(new Stat_Point(3.0,426));
        temp.list.add(new Stat_Point(3.0,430));
        temp.list.add(new Stat_Point(3.0,434));
        temp.list.add(new Stat_Point(3.0,438));
        temp.list.add(new Stat_Point(3.0,442));
        temp.list.add(new Stat_Point(3.0,446));
        temp.list.add(new Stat_Point(3.0,450));
        temp.list.add(new Stat_Point(3.0,454));
        temp.list.add(new Stat_Point(3.0,461));
        temp.list.add(new Stat_Point(3.0,463));
        temp.list.add(new Stat_Point(3.0,464));
        temp.list.add(new Stat_Point(3.0,474));
        temp.list.add(new Stat_Point(3.0,475));
        temp.list.add(new Stat_Point(3.0,479));
        temp.list.add(new Stat_Point(0.0,480));
        temp.list.add(new Stat_Point(3.0,482));
        temp.list.add(new Stat_Point(3.0,483));
        temp.list.add(new Stat_Point(3.0,484));
        temp.list.add(new Stat_Point(3.0,486));
        temp.list.add(new Stat_Point(3.0,490));
        temp.list.add(new Stat_Point(3.0,494));
        temp.list.add(new Stat_Point(3.0,498));
        temp.list.add(new Stat_Point(3.0,502));
        temp.list.add(new Stat_Point(3.0,506));
        temp.list.add(new Stat_Point(3.0,510));
        temp.list.add(new Stat_Point(3.0,514));
        temp.list.add(new Stat_Point(3.0,521));
        temp.list.add(new Stat_Point(3.0,523));
        temp.list.add(new Stat_Point(3.0,524));
        temp.list.add(new Stat_Point(3.0,534));
        temp.list.add(new Stat_Point(3.0,535));
        temp.list.add(new Stat_Point(3.0,539));
        temp.list.add(new Stat_Point(0.0,540));
        temp.list.add(new Stat_Point(3.0,542));
        temp.list.add(new Stat_Point(3.0,543));
        temp.list.add(new Stat_Point(3.0,544));
        temp.list.add(new Stat_Point(3.0,546));
        temp.list.add(new Stat_Point(3.0,550));
        temp.list.add(new Stat_Point(3.0,554));
        temp.list.add(new Stat_Point(3.0,558));
        temp.list.add(new Stat_Point(3.0,562));
        temp.list.add(new Stat_Point(3.0,566));
        temp.list.add(new Stat_Point(3.0,570));
        temp.list.add(new Stat_Point(3.0,574));
        temp.list.add(new Stat_Point(3.0,581));
        temp.list.add(new Stat_Point(3.0,583));
        temp.list.add(new Stat_Point(3.0,584));
        temp.list.add(new Stat_Point(3.0,594));
        temp.list.add(new Stat_Point(3.0,595));
        temp.list.add(new Stat_Point(3.0,599));
        temp.list.add(new Stat_Point(0.0,600));
        temp.list.add(new Stat_Point(3.0,602));
        temp.list.add(new Stat_Point(3.0,603));
        temp.list.add(new Stat_Point(3.0,604));
        temp.list.add(new Stat_Point(3.0,606));
        temp.list.add(new Stat_Point(3.0,610));
        temp.list.add(new Stat_Point(3.0,614));
        temp.list.add(new Stat_Point(3.0,618));
        temp.list.add(new Stat_Point(3.0,622));
        temp.list.add(new Stat_Point(3.0,626));
        temp.list.add(new Stat_Point(3.0,630));
        temp.list.add(new Stat_Point(3.0,634));
        temp.list.add(new Stat_Point(3.0,641));
        temp.list.add(new Stat_Point(3.0,643));
        temp.list.add(new Stat_Point(3.0,644));
        temp.list.add(new Stat_Point(3.0,654));
        temp.list.add(new Stat_Point(3.0,655));
        temp.list.add(new Stat_Point(3.0,659));
        temp.list.add(new Stat_Point(0.0,660));
        temp.list.add(new Stat_Point(3.0,662));
        temp.list.add(new Stat_Point(3.0,663));
        temp.list.add(new Stat_Point(3.0,664));
        temp.list.add(new Stat_Point(3.0,666));
        temp.list.add(new Stat_Point(3.0,670));
        temp.list.add(new Stat_Point(3.0,674));
        temp.list.add(new Stat_Point(3.0,678));
        temp.list.add(new Stat_Point(3.0,682));
        temp.list.add(new Stat_Point(3.0,686));
        temp.list.add(new Stat_Point(3.0,690));
        temp.list.add(new Stat_Point(3.0,694));
        temp.list.add(new Stat_Point(3.0,701));
        temp.list.add(new Stat_Point(3.0,703));
        temp.list.add(new Stat_Point(3.0,704));
        temp.list.add(new Stat_Point(3.0,714));
        temp.list.add(new Stat_Point(3.0,715));
        temp.list.add(new Stat_Point(3.0,719));
        temp.list.add(new Stat_Point(0.0,720));
        temp.list.add(new Stat_Point(3.0,722));
        temp.list.add(new Stat_Point(3.0,723));
        temp.list.add(new Stat_Point(3.0,724));
        temp.list.add(new Stat_Point(3.0,726));
        temp.list.add(new Stat_Point(3.0,730));
        temp.list.add(new Stat_Point(3.0,734));
        temp.list.add(new Stat_Point(3.0,738));
        temp.list.add(new Stat_Point(3.0,742));
        temp.list.add(new Stat_Point(3.0,746));
        temp.list.add(new Stat_Point(3.0,750));
        temp.list.add(new Stat_Point(3.0,754));
        temp.list.add(new Stat_Point(3.0,761));
        temp.list.add(new Stat_Point(3.0,763));
        temp.list.add(new Stat_Point(3.0,764));
        temp.list.add(new Stat_Point(3.0,774));
        temp.list.add(new Stat_Point(3.0,775));
        temp.list.add(new Stat_Point(3.0,779));
        temp.list.add(new Stat_Point(0.0,780));
        temp.list.add(new Stat_Point(3.0,782));
        temp.list.add(new Stat_Point(3.0,783));
        temp.list.add(new Stat_Point(3.0,784));
        temp.list.add(new Stat_Point(3.0,786));
        temp.list.add(new Stat_Point(3.0,790));
        temp.list.add(new Stat_Point(3.0,794));
        temp.list.add(new Stat_Point(3.0,798));
        temp.list.add(new Stat_Point(3.0,802));
        temp.list.add(new Stat_Point(3.0,806));
        temp.list.add(new Stat_Point(3.0,810));
        temp.list.add(new Stat_Point(3.0,814));
        temp.list.add(new Stat_Point(3.0,821));
        temp.list.add(new Stat_Point(3.0,823));
        temp.list.add(new Stat_Point(3.0,824));
        temp.list.add(new Stat_Point(3.0,834));
        temp.list.add(new Stat_Point(3.0,835));
        temp.list.add(new Stat_Point(3.0,839));
        temp.list.add(new Stat_Point(0.0,840));
        temp.list.add(new Stat_Point(3.0,842));
        temp.list.add(new Stat_Point(3.0,843));
        temp.list.add(new Stat_Point(3.0,844));
        temp.list.add(new Stat_Point(3.0,846));
        temp.list.add(new Stat_Point(3.0,850));
        temp.list.add(new Stat_Point(3.0,854));
        temp.list.add(new Stat_Point(3.0,858));
        temp.list.add(new Stat_Point(3.0,862));
        temp.list.add(new Stat_Point(3.0,866));
        temp.list.add(new Stat_Point(3.0,870));
        temp.list.add(new Stat_Point(3.0,874));
        temp.list.add(new Stat_Point(3.0,881));
        temp.list.add(new Stat_Point(3.0,883));
        temp.list.add(new Stat_Point(3.0,884));
        temp.list.add(new Stat_Point(3.0,894));
        temp.list.add(new Stat_Point(3.0,895));
        temp.list.add(new Stat_Point(3.0,899));
        temp.list.add(new Stat_Point(0.0,900));
        temp.list.add(new Stat_Point(3.0,902));
        temp.list.add(new Stat_Point(3.0,903));
        temp.list.add(new Stat_Point(3.0,904));
        temp.list.add(new Stat_Point(3.0,906));
        temp.list.add(new Stat_Point(3.0,910));
        temp.list.add(new Stat_Point(3.0,914));
        temp.list.add(new Stat_Point(3.0,918));
        temp.list.add(new Stat_Point(3.0,922));
        temp.list.add(new Stat_Point(3.0,926));
        temp.list.add(new Stat_Point(3.0,930));
        temp.list.add(new Stat_Point(3.0,934));
        temp.list.add(new Stat_Point(3.0,941));
        temp.list.add(new Stat_Point(3.0,943));
        temp.list.add(new Stat_Point(3.0,944));
        temp.list.add(new Stat_Point(3.0,954));
        temp.list.add(new Stat_Point(3.0,955));
        temp.list.add(new Stat_Point(3.0,959));
        temp.list.add(new Stat_Point(0.0,960));
        temp.list.add(new Stat_Point(3.0,962));
        temp.list.add(new Stat_Point(3.0,963));
        temp.list.add(new Stat_Point(3.0,964));
        temp.list.add(new Stat_Point(3.0,966));
        temp.list.add(new Stat_Point(3.0,970));
        temp.list.add(new Stat_Point(3.0,974));
        temp.list.add(new Stat_Point(3.0,978));
        temp.list.add(new Stat_Point(3.0,982));
        temp.list.add(new Stat_Point(3.0,986));
        temp.list.add(new Stat_Point(3.0,990));
        temp.list.add(new Stat_Point(3.0,994));
        temp.list.add(new Stat_Point(3.0,1001));
        temp.list.add(new Stat_Point(3.0,1003));
        temp.list.add(new Stat_Point(3.0,1004));
        temp.list.add(new Stat_Point(3.0,1014));
        temp.list.add(new Stat_Point(3.0,1015));
        temp.list.add(new Stat_Point(3.0,1019));
        temp.list.add(new Stat_Point(0.0,1020));
        temp.list.add(new Stat_Point(3.0,1022));
        temp.list.add(new Stat_Point(3.0,1023));
        temp.list.add(new Stat_Point(3.0,1024));
        temp.list.add(new Stat_Point(3.0,1026));
        temp.list.add(new Stat_Point(3.0,1030));
        temp.list.add(new Stat_Point(3.0,1034));
        temp.list.add(new Stat_Point(3.0,1038));
        temp.list.add(new Stat_Point(3.0,1042));
        temp.list.add(new Stat_Point(3.0,1046));
        temp.list.add(new Stat_Point(3.0,1050));
        temp.list.add(new Stat_Point(3.0,1054));
        temp.list.add(new Stat_Point(3.0,1061));
        temp.list.add(new Stat_Point(3.0,1063));
        temp.list.add(new Stat_Point(3.0,1064));
        temp.list.add(new Stat_Point(3.0,1074));
        temp.list.add(new Stat_Point(3.0,1075));
        temp.list.add(new Stat_Point(3.0,1079));
        temp.list.add(new Stat_Point(0.0,1080));
        temp.list.add(new Stat_Point(3.0,1082));
        temp.list.add(new Stat_Point(3.0,1083));
        temp.list.add(new Stat_Point(3.0,1084));
        temp.list.add(new Stat_Point(3.0,1086));
        temp.list.add(new Stat_Point(3.0,1090));
        temp.list.add(new Stat_Point(3.0,1094));
        temp.list.add(new Stat_Point(3.0,1098));
        temp.list.add(new Stat_Point(3.0,1102));
        temp.list.add(new Stat_Point(3.0,1106));
        temp.list.add(new Stat_Point(3.0,1110));
        temp.list.add(new Stat_Point(3.0,1114));
        temp.list.add(new Stat_Point(3.0,1121));
        temp.list.add(new Stat_Point(3.0,1123));
        temp.list.add(new Stat_Point(3.0,1124));
        temp.list.add(new Stat_Point(3.0,1134));
        temp.list.add(new Stat_Point(3.0,1135));
        temp.list.add(new Stat_Point(3.0,1139));
        temp.list.add(new Stat_Point(0.0,1140));
        temp.list.add(new Stat_Point(3.0,1142));
        temp.list.add(new Stat_Point(3.0,1143));
        temp.list.add(new Stat_Point(3.0,1144));
        temp.list.add(new Stat_Point(3.0,1146));
        temp.list.add(new Stat_Point(3.0,1150));
        temp.list.add(new Stat_Point(3.0,1154));
        temp.list.add(new Stat_Point(3.0,1158));
        temp.list.add(new Stat_Point(3.0,1162));
        temp.list.add(new Stat_Point(3.0,1166));
        temp.list.add(new Stat_Point(3.0,1170));
        temp.list.add(new Stat_Point(3.0,1174));
        temp.list.add(new Stat_Point(3.0,1181));
        temp.list.add(new Stat_Point(3.0,1183));
        temp.list.add(new Stat_Point(3.0,1184));
        temp.list.add(new Stat_Point(3.0,1194));
        temp.list.add(new Stat_Point(3.0,1195));
        temp.list.add(new Stat_Point(3.0,1199));
        temp.list.add(new Stat_Point(0.0,1200));

        for(Stat_Point p : temp.list){
            temp.total += p.value;
        }
        temp.average = temp.total/temp.duration;
        map.put(0,temp);

        temp = new Stat();
        temp.duration = 1200;
        temp.list.add(new Stat_Point(3.0,2));
        temp.list.add(new Stat_Point(3.0,3));
        temp.list.add(new Stat_Point(3.0,4));
        temp.list.add(new Stat_Point(3.0,6));
        temp.list.add(new Stat_Point(3.0,10));
        temp.list.add(new Stat_Point(3.0,14));
        temp.list.add(new Stat_Point(3.0,18));
        temp.list.add(new Stat_Point(3.0,22));
        temp.list.add(new Stat_Point(3.0,26));
        temp.list.add(new Stat_Point(3.0,30));
        temp.list.add(new Stat_Point(3.0,34));
        temp.list.add(new Stat_Point(3.0,41));
        temp.list.add(new Stat_Point(3.0,43));
        temp.list.add(new Stat_Point(3.0,44));
        temp.list.add(new Stat_Point(3.0,54));
        temp.list.add(new Stat_Point(3.0,55));
        temp.list.add(new Stat_Point(3.0,59));
        temp.list.add(new Stat_Point(0.0,60));
        temp.list.add(new Stat_Point(3.0,62));
        temp.list.add(new Stat_Point(3.0,63));
        temp.list.add(new Stat_Point(3.0,64));
        temp.list.add(new Stat_Point(3.0,66));
        temp.list.add(new Stat_Point(3.0,70));
        temp.list.add(new Stat_Point(3.0,74));
        temp.list.add(new Stat_Point(3.0,78));
        temp.list.add(new Stat_Point(3.0,82));
        temp.list.add(new Stat_Point(3.0,86));
        temp.list.add(new Stat_Point(3.0,90));
        temp.list.add(new Stat_Point(3.0,94));
        temp.list.add(new Stat_Point(3.0,101));
        temp.list.add(new Stat_Point(3.0,103));
        temp.list.add(new Stat_Point(3.0,104));
        temp.list.add(new Stat_Point(3.0,114));
        temp.list.add(new Stat_Point(3.0,115));
        temp.list.add(new Stat_Point(3.0,119));
        temp.list.add(new Stat_Point(0.0,120));
        temp.list.add(new Stat_Point(3.0,122));
        temp.list.add(new Stat_Point(3.0,123));
        temp.list.add(new Stat_Point(3.0,124));
        temp.list.add(new Stat_Point(3.0,126));
        temp.list.add(new Stat_Point(3.0,130));
        temp.list.add(new Stat_Point(3.0,134));
        temp.list.add(new Stat_Point(3.0,138));
        temp.list.add(new Stat_Point(3.0,142));
        temp.list.add(new Stat_Point(3.0,146));
        temp.list.add(new Stat_Point(3.0,150));
        temp.list.add(new Stat_Point(3.0,154));
        temp.list.add(new Stat_Point(3.0,161));
        temp.list.add(new Stat_Point(3.0,163));
        temp.list.add(new Stat_Point(3.0,164));
        temp.list.add(new Stat_Point(3.0,174));
        temp.list.add(new Stat_Point(3.0,175));
        temp.list.add(new Stat_Point(3.0,179));
        temp.list.add(new Stat_Point(0.0,180));
        temp.list.add(new Stat_Point(3.0,182));
        temp.list.add(new Stat_Point(3.0,183));
        temp.list.add(new Stat_Point(3.0,184));
        temp.list.add(new Stat_Point(3.0,186));
        temp.list.add(new Stat_Point(3.0,190));
        temp.list.add(new Stat_Point(3.0,194));
        temp.list.add(new Stat_Point(3.0,198));
        temp.list.add(new Stat_Point(3.0,202));
        temp.list.add(new Stat_Point(3.0,206));
        temp.list.add(new Stat_Point(3.0,210));
        temp.list.add(new Stat_Point(3.0,214));
        temp.list.add(new Stat_Point(3.0,221));
        temp.list.add(new Stat_Point(3.0,223));
        temp.list.add(new Stat_Point(3.0,224));
        temp.list.add(new Stat_Point(3.0,234));
        temp.list.add(new Stat_Point(3.0,235));
        temp.list.add(new Stat_Point(3.0,239));
        temp.list.add(new Stat_Point(0.0,240));
        temp.list.add(new Stat_Point(3.0,242));
        temp.list.add(new Stat_Point(3.0,243));
        temp.list.add(new Stat_Point(3.0,244));
        temp.list.add(new Stat_Point(3.0,246));
        temp.list.add(new Stat_Point(3.0,250));
        temp.list.add(new Stat_Point(3.0,254));
        temp.list.add(new Stat_Point(3.0,258));
        temp.list.add(new Stat_Point(3.0,262));
        temp.list.add(new Stat_Point(3.0,266));
        temp.list.add(new Stat_Point(3.0,270));
        temp.list.add(new Stat_Point(3.0,274));
        temp.list.add(new Stat_Point(3.0,281));
        temp.list.add(new Stat_Point(3.0,283));
        temp.list.add(new Stat_Point(3.0,284));
        temp.list.add(new Stat_Point(3.0,294));
        temp.list.add(new Stat_Point(3.0,295));
        temp.list.add(new Stat_Point(3.0,299));
        temp.list.add(new Stat_Point(0.0,300));
        temp.list.add(new Stat_Point(3.0,302));
        temp.list.add(new Stat_Point(3.0,303));
        temp.list.add(new Stat_Point(3.0,304));
        temp.list.add(new Stat_Point(3.0,306));
        temp.list.add(new Stat_Point(3.0,310));
        temp.list.add(new Stat_Point(3.0,314));
        temp.list.add(new Stat_Point(3.0,318));
        temp.list.add(new Stat_Point(3.0,322));
        temp.list.add(new Stat_Point(3.0,326));
        temp.list.add(new Stat_Point(3.0,330));
        temp.list.add(new Stat_Point(3.0,334));
        temp.list.add(new Stat_Point(3.0,341));
        temp.list.add(new Stat_Point(3.0,343));
        temp.list.add(new Stat_Point(3.0,344));
        temp.list.add(new Stat_Point(3.0,354));
        temp.list.add(new Stat_Point(3.0,355));
        temp.list.add(new Stat_Point(3.0,359));
        temp.list.add(new Stat_Point(0.0,360));
        temp.list.add(new Stat_Point(3.0,362));
        temp.list.add(new Stat_Point(3.0,363));
        temp.list.add(new Stat_Point(3.0,364));
        temp.list.add(new Stat_Point(3.0,366));
        temp.list.add(new Stat_Point(3.0,370));
        temp.list.add(new Stat_Point(3.0,374));
        temp.list.add(new Stat_Point(3.0,378));
        temp.list.add(new Stat_Point(3.0,382));
        temp.list.add(new Stat_Point(3.0,386));
        temp.list.add(new Stat_Point(3.0,390));
        temp.list.add(new Stat_Point(3.0,394));
        temp.list.add(new Stat_Point(3.0,401));
        temp.list.add(new Stat_Point(3.0,403));
        temp.list.add(new Stat_Point(3.0,404));
        temp.list.add(new Stat_Point(3.0,414));
        temp.list.add(new Stat_Point(3.0,415));
        temp.list.add(new Stat_Point(3.0,419));
        temp.list.add(new Stat_Point(0.0,420));
        temp.list.add(new Stat_Point(3.0,422));
        temp.list.add(new Stat_Point(3.0,423));
        temp.list.add(new Stat_Point(3.0,424));
        temp.list.add(new Stat_Point(3.0,426));
        temp.list.add(new Stat_Point(3.0,430));
        temp.list.add(new Stat_Point(3.0,434));
        temp.list.add(new Stat_Point(3.0,438));
        temp.list.add(new Stat_Point(3.0,442));
        temp.list.add(new Stat_Point(3.0,446));
        temp.list.add(new Stat_Point(3.0,450));
        temp.list.add(new Stat_Point(3.0,454));
        temp.list.add(new Stat_Point(3.0,461));
        temp.list.add(new Stat_Point(3.0,463));
        temp.list.add(new Stat_Point(3.0,464));
        temp.list.add(new Stat_Point(3.0,474));
        temp.list.add(new Stat_Point(3.0,475));
        temp.list.add(new Stat_Point(3.0,479));
        temp.list.add(new Stat_Point(0.0,480));
        temp.list.add(new Stat_Point(3.0,482));
        temp.list.add(new Stat_Point(3.0,483));
        temp.list.add(new Stat_Point(3.0,484));
        temp.list.add(new Stat_Point(3.0,486));
        temp.list.add(new Stat_Point(3.0,490));
        temp.list.add(new Stat_Point(3.0,494));
        temp.list.add(new Stat_Point(3.0,498));
        temp.list.add(new Stat_Point(3.0,502));
        temp.list.add(new Stat_Point(3.0,506));
        temp.list.add(new Stat_Point(3.0,510));
        temp.list.add(new Stat_Point(3.0,514));
        temp.list.add(new Stat_Point(3.0,521));
        temp.list.add(new Stat_Point(3.0,523));
        temp.list.add(new Stat_Point(3.0,524));
        temp.list.add(new Stat_Point(3.0,534));
        temp.list.add(new Stat_Point(3.0,535));
        temp.list.add(new Stat_Point(3.0,539));
        temp.list.add(new Stat_Point(0.0,540));
        temp.list.add(new Stat_Point(3.0,542));
        temp.list.add(new Stat_Point(3.0,543));
        temp.list.add(new Stat_Point(3.0,544));
        temp.list.add(new Stat_Point(3.0,546));
        temp.list.add(new Stat_Point(3.0,550));
        temp.list.add(new Stat_Point(3.0,554));
        temp.list.add(new Stat_Point(3.0,558));
        temp.list.add(new Stat_Point(3.0,562));
        temp.list.add(new Stat_Point(3.0,566));
        temp.list.add(new Stat_Point(3.0,570));
        temp.list.add(new Stat_Point(3.0,574));
        temp.list.add(new Stat_Point(3.0,581));
        temp.list.add(new Stat_Point(3.0,583));
        temp.list.add(new Stat_Point(3.0,584));
        temp.list.add(new Stat_Point(3.0,594));
        temp.list.add(new Stat_Point(3.0,595));
        temp.list.add(new Stat_Point(3.0,599));
        temp.list.add(new Stat_Point(0.0,600));
        temp.list.add(new Stat_Point(3.0,602));
        temp.list.add(new Stat_Point(3.0,603));
        temp.list.add(new Stat_Point(3.0,604));
        temp.list.add(new Stat_Point(3.0,606));
        temp.list.add(new Stat_Point(3.0,610));
        temp.list.add(new Stat_Point(3.0,614));
        temp.list.add(new Stat_Point(3.0,618));
        temp.list.add(new Stat_Point(3.0,622));
        temp.list.add(new Stat_Point(3.0,626));
        temp.list.add(new Stat_Point(3.0,630));
        temp.list.add(new Stat_Point(3.0,634));
        temp.list.add(new Stat_Point(3.0,641));
        temp.list.add(new Stat_Point(3.0,643));
        temp.list.add(new Stat_Point(3.0,644));
        temp.list.add(new Stat_Point(3.0,654));
        temp.list.add(new Stat_Point(3.0,655));
        temp.list.add(new Stat_Point(3.0,659));
        temp.list.add(new Stat_Point(0.0,660));
        temp.list.add(new Stat_Point(3.0,662));
        temp.list.add(new Stat_Point(3.0,663));
        temp.list.add(new Stat_Point(3.0,664));
        temp.list.add(new Stat_Point(3.0,666));
        temp.list.add(new Stat_Point(3.0,670));
        temp.list.add(new Stat_Point(3.0,674));
        temp.list.add(new Stat_Point(3.0,678));
        temp.list.add(new Stat_Point(3.0,682));
        temp.list.add(new Stat_Point(3.0,686));
        temp.list.add(new Stat_Point(3.0,690));
        temp.list.add(new Stat_Point(3.0,694));
        temp.list.add(new Stat_Point(3.0,701));
        temp.list.add(new Stat_Point(3.0,703));
        temp.list.add(new Stat_Point(3.0,704));
        temp.list.add(new Stat_Point(3.0,714));
        temp.list.add(new Stat_Point(3.0,715));
        temp.list.add(new Stat_Point(3.0,719));
        temp.list.add(new Stat_Point(0.0,720));
        temp.list.add(new Stat_Point(3.0,722));
        temp.list.add(new Stat_Point(3.0,723));
        temp.list.add(new Stat_Point(3.0,724));
        temp.list.add(new Stat_Point(3.0,726));
        temp.list.add(new Stat_Point(3.0,730));
        temp.list.add(new Stat_Point(3.0,734));
        temp.list.add(new Stat_Point(3.0,738));
        temp.list.add(new Stat_Point(3.0,742));
        temp.list.add(new Stat_Point(3.0,746));
        temp.list.add(new Stat_Point(3.0,750));
        temp.list.add(new Stat_Point(3.0,754));
        temp.list.add(new Stat_Point(3.0,761));
        temp.list.add(new Stat_Point(3.0,763));
        temp.list.add(new Stat_Point(3.0,764));
        temp.list.add(new Stat_Point(3.0,774));
        temp.list.add(new Stat_Point(3.0,775));
        temp.list.add(new Stat_Point(3.0,779));
        temp.list.add(new Stat_Point(0.0,780));
        temp.list.add(new Stat_Point(3.0,782));
        temp.list.add(new Stat_Point(3.0,783));
        temp.list.add(new Stat_Point(3.0,784));
        temp.list.add(new Stat_Point(3.0,786));
        temp.list.add(new Stat_Point(3.0,790));
        temp.list.add(new Stat_Point(3.0,794));
        temp.list.add(new Stat_Point(3.0,798));
        temp.list.add(new Stat_Point(3.0,802));
        temp.list.add(new Stat_Point(3.0,806));
        temp.list.add(new Stat_Point(3.0,810));
        temp.list.add(new Stat_Point(3.0,814));
        temp.list.add(new Stat_Point(3.0,821));
        temp.list.add(new Stat_Point(3.0,823));
        temp.list.add(new Stat_Point(3.0,824));
        temp.list.add(new Stat_Point(3.0,834));
        temp.list.add(new Stat_Point(3.0,835));
        temp.list.add(new Stat_Point(3.0,839));
        temp.list.add(new Stat_Point(0.0,840));
        temp.list.add(new Stat_Point(3.0,842));
        temp.list.add(new Stat_Point(3.0,843));
        temp.list.add(new Stat_Point(3.0,844));
        temp.list.add(new Stat_Point(3.0,846));
        temp.list.add(new Stat_Point(3.0,850));
        temp.list.add(new Stat_Point(3.0,854));
        temp.list.add(new Stat_Point(3.0,858));
        temp.list.add(new Stat_Point(3.0,862));
        temp.list.add(new Stat_Point(3.0,866));
        temp.list.add(new Stat_Point(3.0,870));
        temp.list.add(new Stat_Point(3.0,874));
        temp.list.add(new Stat_Point(3.0,881));
        temp.list.add(new Stat_Point(3.0,883));
        temp.list.add(new Stat_Point(3.0,884));
        temp.list.add(new Stat_Point(3.0,894));
        temp.list.add(new Stat_Point(3.0,895));
        temp.list.add(new Stat_Point(3.0,899));
        temp.list.add(new Stat_Point(0.0,900));
        temp.list.add(new Stat_Point(3.0,902));
        temp.list.add(new Stat_Point(3.0,903));
        temp.list.add(new Stat_Point(3.0,904));
        temp.list.add(new Stat_Point(3.0,906));
        temp.list.add(new Stat_Point(3.0,910));
        temp.list.add(new Stat_Point(3.0,914));
        temp.list.add(new Stat_Point(3.0,918));
        temp.list.add(new Stat_Point(3.0,922));
        temp.list.add(new Stat_Point(3.0,926));
        temp.list.add(new Stat_Point(3.0,930));
        temp.list.add(new Stat_Point(3.0,934));
        temp.list.add(new Stat_Point(3.0,941));
        temp.list.add(new Stat_Point(3.0,943));
        temp.list.add(new Stat_Point(3.0,944));
        temp.list.add(new Stat_Point(3.0,954));
        temp.list.add(new Stat_Point(3.0,955));
        temp.list.add(new Stat_Point(3.0,959));
        temp.list.add(new Stat_Point(0.0,960));
        temp.list.add(new Stat_Point(3.0,962));
        temp.list.add(new Stat_Point(3.0,963));
        temp.list.add(new Stat_Point(3.0,964));
        temp.list.add(new Stat_Point(3.0,966));
        temp.list.add(new Stat_Point(3.0,970));
        temp.list.add(new Stat_Point(3.0,974));
        temp.list.add(new Stat_Point(3.0,978));
        temp.list.add(new Stat_Point(3.0,982));
        temp.list.add(new Stat_Point(3.0,986));
        temp.list.add(new Stat_Point(3.0,990));
        temp.list.add(new Stat_Point(3.0,994));
        temp.list.add(new Stat_Point(3.0,1001));
        temp.list.add(new Stat_Point(3.0,1003));
        temp.list.add(new Stat_Point(3.0,1004));
        temp.list.add(new Stat_Point(3.0,1014));
        temp.list.add(new Stat_Point(3.0,1015));
        temp.list.add(new Stat_Point(3.0,1019));
        temp.list.add(new Stat_Point(0.0,1020));
        temp.list.add(new Stat_Point(3.0,1022));
        temp.list.add(new Stat_Point(3.0,1023));
        temp.list.add(new Stat_Point(3.0,1024));
        temp.list.add(new Stat_Point(3.0,1026));
        temp.list.add(new Stat_Point(3.0,1030));
        temp.list.add(new Stat_Point(3.0,1034));
        temp.list.add(new Stat_Point(3.0,1038));
        temp.list.add(new Stat_Point(3.0,1042));
        temp.list.add(new Stat_Point(3.0,1046));
        temp.list.add(new Stat_Point(3.0,1050));
        temp.list.add(new Stat_Point(3.0,1054));
        temp.list.add(new Stat_Point(3.0,1061));
        temp.list.add(new Stat_Point(3.0,1063));
        temp.list.add(new Stat_Point(3.0,1064));
        temp.list.add(new Stat_Point(3.0,1074));
        temp.list.add(new Stat_Point(3.0,1075));
        temp.list.add(new Stat_Point(3.0,1079));
        temp.list.add(new Stat_Point(0.0,1080));
        temp.list.add(new Stat_Point(3.0,1082));
        temp.list.add(new Stat_Point(3.0,1083));
        temp.list.add(new Stat_Point(3.0,1084));
        temp.list.add(new Stat_Point(3.0,1086));
        temp.list.add(new Stat_Point(3.0,1090));
        temp.list.add(new Stat_Point(3.0,1094));
        temp.list.add(new Stat_Point(3.0,1098));
        temp.list.add(new Stat_Point(3.0,1102));
        temp.list.add(new Stat_Point(3.0,1106));
        temp.list.add(new Stat_Point(3.0,1110));
        temp.list.add(new Stat_Point(3.0,1114));
        temp.list.add(new Stat_Point(3.0,1121));
        temp.list.add(new Stat_Point(3.0,1123));
        temp.list.add(new Stat_Point(3.0,1124));
        temp.list.add(new Stat_Point(3.0,1134));
        temp.list.add(new Stat_Point(3.0,1135));
        temp.list.add(new Stat_Point(3.0,1139));
        temp.list.add(new Stat_Point(0.0,1140));
        temp.list.add(new Stat_Point(3.0,1142));
        temp.list.add(new Stat_Point(3.0,1143));
        temp.list.add(new Stat_Point(3.0,1144));
        temp.list.add(new Stat_Point(3.0,1146));
        temp.list.add(new Stat_Point(3.0,1150));
        temp.list.add(new Stat_Point(3.0,1154));
        temp.list.add(new Stat_Point(3.0,1158));
        temp.list.add(new Stat_Point(3.0,1162));
        temp.list.add(new Stat_Point(3.0,1166));
        temp.list.add(new Stat_Point(3.0,1170));
        temp.list.add(new Stat_Point(3.0,1174));
        temp.list.add(new Stat_Point(3.0,1181));
        temp.list.add(new Stat_Point(3.0,1183));
        temp.list.add(new Stat_Point(3.0,1184));
        temp.list.add(new Stat_Point(3.0,1194));
        temp.list.add(new Stat_Point(3.0,1195));
        temp.list.add(new Stat_Point(3.0,1199));
        temp.list.add(new Stat_Point(0.0,1200));

        for(Stat_Point p : temp.list){
            temp.total += p.value;
        }
        temp.average = temp.total/temp.duration;
        map.put(1,temp);

        temp = new Stat();
        temp.duration = 1200;
        temp.list.add(new Stat_Point(3.0,2));
        temp.list.add(new Stat_Point(3.0,3));
        temp.list.add(new Stat_Point(3.0,4));
        temp.list.add(new Stat_Point(3.0,6));
        temp.list.add(new Stat_Point(3.0,10));
        temp.list.add(new Stat_Point(3.0,14));
        temp.list.add(new Stat_Point(3.0,18));
        temp.list.add(new Stat_Point(3.0,22));
        temp.list.add(new Stat_Point(3.0,26));
        temp.list.add(new Stat_Point(3.0,30));
        temp.list.add(new Stat_Point(3.0,34));
        temp.list.add(new Stat_Point(3.0,41));
        temp.list.add(new Stat_Point(3.0,43));
        temp.list.add(new Stat_Point(3.0,44));
        temp.list.add(new Stat_Point(3.0,54));
        temp.list.add(new Stat_Point(3.0,55));
        temp.list.add(new Stat_Point(3.0,59));
        temp.list.add(new Stat_Point(0.0,60));
        temp.list.add(new Stat_Point(3.0,62));
        temp.list.add(new Stat_Point(3.0,63));
        temp.list.add(new Stat_Point(3.0,64));
        temp.list.add(new Stat_Point(3.0,66));
        temp.list.add(new Stat_Point(3.0,70));
        temp.list.add(new Stat_Point(3.0,74));
        temp.list.add(new Stat_Point(3.0,78));
        temp.list.add(new Stat_Point(3.0,82));
        temp.list.add(new Stat_Point(3.0,86));
        temp.list.add(new Stat_Point(3.0,90));
        temp.list.add(new Stat_Point(3.0,94));
        temp.list.add(new Stat_Point(3.0,101));
        temp.list.add(new Stat_Point(3.0,103));
        temp.list.add(new Stat_Point(3.0,104));
        temp.list.add(new Stat_Point(3.0,114));
        temp.list.add(new Stat_Point(3.0,115));
        temp.list.add(new Stat_Point(3.0,119));
        temp.list.add(new Stat_Point(0.0,120));
        temp.list.add(new Stat_Point(3.0,122));
        temp.list.add(new Stat_Point(3.0,123));
        temp.list.add(new Stat_Point(3.0,124));
        temp.list.add(new Stat_Point(3.0,126));
        temp.list.add(new Stat_Point(3.0,130));
        temp.list.add(new Stat_Point(3.0,134));
        temp.list.add(new Stat_Point(3.0,138));
        temp.list.add(new Stat_Point(3.0,142));
        temp.list.add(new Stat_Point(3.0,146));
        temp.list.add(new Stat_Point(3.0,150));
        temp.list.add(new Stat_Point(3.0,154));
        temp.list.add(new Stat_Point(3.0,161));
        temp.list.add(new Stat_Point(3.0,163));
        temp.list.add(new Stat_Point(3.0,164));
        temp.list.add(new Stat_Point(3.0,174));
        temp.list.add(new Stat_Point(3.0,175));
        temp.list.add(new Stat_Point(3.0,179));
        temp.list.add(new Stat_Point(0.0,180));
        temp.list.add(new Stat_Point(3.0,182));
        temp.list.add(new Stat_Point(3.0,183));
        temp.list.add(new Stat_Point(3.0,184));
        temp.list.add(new Stat_Point(3.0,186));
        temp.list.add(new Stat_Point(3.0,190));
        temp.list.add(new Stat_Point(3.0,194));
        temp.list.add(new Stat_Point(3.0,198));
        temp.list.add(new Stat_Point(3.0,202));
        temp.list.add(new Stat_Point(3.0,206));
        temp.list.add(new Stat_Point(3.0,210));
        temp.list.add(new Stat_Point(3.0,214));
        temp.list.add(new Stat_Point(3.0,221));
        temp.list.add(new Stat_Point(3.0,223));
        temp.list.add(new Stat_Point(3.0,224));
        temp.list.add(new Stat_Point(3.0,234));
        temp.list.add(new Stat_Point(3.0,235));
        temp.list.add(new Stat_Point(3.0,239));
        temp.list.add(new Stat_Point(0.0,240));
        temp.list.add(new Stat_Point(3.0,242));
        temp.list.add(new Stat_Point(3.0,243));
        temp.list.add(new Stat_Point(3.0,244));
        temp.list.add(new Stat_Point(3.0,246));
        temp.list.add(new Stat_Point(3.0,250));
        temp.list.add(new Stat_Point(3.0,254));
        temp.list.add(new Stat_Point(3.0,258));
        temp.list.add(new Stat_Point(3.0,262));
        temp.list.add(new Stat_Point(3.0,266));
        temp.list.add(new Stat_Point(3.0,270));
        temp.list.add(new Stat_Point(3.0,274));
        temp.list.add(new Stat_Point(3.0,281));
        temp.list.add(new Stat_Point(3.0,283));
        temp.list.add(new Stat_Point(3.0,284));
        temp.list.add(new Stat_Point(3.0,294));
        temp.list.add(new Stat_Point(3.0,295));
        temp.list.add(new Stat_Point(3.0,299));
        temp.list.add(new Stat_Point(0.0,300));
        temp.list.add(new Stat_Point(3.0,302));
        temp.list.add(new Stat_Point(3.0,303));
        temp.list.add(new Stat_Point(3.0,304));
        temp.list.add(new Stat_Point(3.0,306));
        temp.list.add(new Stat_Point(3.0,310));
        temp.list.add(new Stat_Point(3.0,314));
        temp.list.add(new Stat_Point(3.0,318));
        temp.list.add(new Stat_Point(3.0,322));
        temp.list.add(new Stat_Point(3.0,326));
        temp.list.add(new Stat_Point(3.0,330));
        temp.list.add(new Stat_Point(3.0,334));
        temp.list.add(new Stat_Point(3.0,341));
        temp.list.add(new Stat_Point(3.0,343));
        temp.list.add(new Stat_Point(3.0,344));
        temp.list.add(new Stat_Point(3.0,354));
        temp.list.add(new Stat_Point(3.0,355));
        temp.list.add(new Stat_Point(3.0,359));
        temp.list.add(new Stat_Point(0.0,360));
        temp.list.add(new Stat_Point(3.0,362));
        temp.list.add(new Stat_Point(3.0,363));
        temp.list.add(new Stat_Point(3.0,364));
        temp.list.add(new Stat_Point(3.0,366));
        temp.list.add(new Stat_Point(3.0,370));
        temp.list.add(new Stat_Point(3.0,374));
        temp.list.add(new Stat_Point(3.0,378));
        temp.list.add(new Stat_Point(3.0,382));
        temp.list.add(new Stat_Point(3.0,386));
        temp.list.add(new Stat_Point(3.0,390));
        temp.list.add(new Stat_Point(3.0,394));
        temp.list.add(new Stat_Point(3.0,401));
        temp.list.add(new Stat_Point(3.0,403));
        temp.list.add(new Stat_Point(3.0,404));
        temp.list.add(new Stat_Point(3.0,414));
        temp.list.add(new Stat_Point(3.0,415));
        temp.list.add(new Stat_Point(3.0,419));
        temp.list.add(new Stat_Point(0.0,420));
        temp.list.add(new Stat_Point(3.0,422));
        temp.list.add(new Stat_Point(3.0,423));
        temp.list.add(new Stat_Point(3.0,424));
        temp.list.add(new Stat_Point(3.0,426));
        temp.list.add(new Stat_Point(3.0,430));
        temp.list.add(new Stat_Point(3.0,434));
        temp.list.add(new Stat_Point(3.0,438));
        temp.list.add(new Stat_Point(3.0,442));
        temp.list.add(new Stat_Point(3.0,446));
        temp.list.add(new Stat_Point(3.0,450));
        temp.list.add(new Stat_Point(3.0,454));
        temp.list.add(new Stat_Point(3.0,461));
        temp.list.add(new Stat_Point(3.0,463));
        temp.list.add(new Stat_Point(3.0,464));
        temp.list.add(new Stat_Point(3.0,474));
        temp.list.add(new Stat_Point(3.0,475));
        temp.list.add(new Stat_Point(3.0,479));
        temp.list.add(new Stat_Point(0.0,480));
        temp.list.add(new Stat_Point(3.0,482));
        temp.list.add(new Stat_Point(3.0,483));
        temp.list.add(new Stat_Point(3.0,484));
        temp.list.add(new Stat_Point(3.0,486));
        temp.list.add(new Stat_Point(3.0,490));
        temp.list.add(new Stat_Point(3.0,494));
        temp.list.add(new Stat_Point(3.0,498));
        temp.list.add(new Stat_Point(3.0,502));
        temp.list.add(new Stat_Point(3.0,506));
        temp.list.add(new Stat_Point(3.0,510));
        temp.list.add(new Stat_Point(3.0,514));
        temp.list.add(new Stat_Point(3.0,521));
        temp.list.add(new Stat_Point(3.0,523));
        temp.list.add(new Stat_Point(3.0,524));
        temp.list.add(new Stat_Point(3.0,534));
        temp.list.add(new Stat_Point(3.0,535));
        temp.list.add(new Stat_Point(3.0,539));
        temp.list.add(new Stat_Point(0.0,540));
        temp.list.add(new Stat_Point(3.0,542));
        temp.list.add(new Stat_Point(3.0,543));
        temp.list.add(new Stat_Point(3.0,544));
        temp.list.add(new Stat_Point(3.0,546));
        temp.list.add(new Stat_Point(3.0,550));
        temp.list.add(new Stat_Point(3.0,554));
        temp.list.add(new Stat_Point(3.0,558));
        temp.list.add(new Stat_Point(3.0,562));
        temp.list.add(new Stat_Point(3.0,566));
        temp.list.add(new Stat_Point(3.0,570));
        temp.list.add(new Stat_Point(3.0,574));
        temp.list.add(new Stat_Point(3.0,581));
        temp.list.add(new Stat_Point(3.0,583));
        temp.list.add(new Stat_Point(3.0,584));
        temp.list.add(new Stat_Point(3.0,594));
        temp.list.add(new Stat_Point(3.0,595));
        temp.list.add(new Stat_Point(3.0,599));
        temp.list.add(new Stat_Point(0.0,600));
        temp.list.add(new Stat_Point(3.0,602));
        temp.list.add(new Stat_Point(3.0,603));
        temp.list.add(new Stat_Point(3.0,604));
        temp.list.add(new Stat_Point(3.0,606));
        temp.list.add(new Stat_Point(3.0,610));
        temp.list.add(new Stat_Point(3.0,614));
        temp.list.add(new Stat_Point(3.0,618));
        temp.list.add(new Stat_Point(3.0,622));
        temp.list.add(new Stat_Point(3.0,626));
        temp.list.add(new Stat_Point(3.0,630));
        temp.list.add(new Stat_Point(3.0,634));
        temp.list.add(new Stat_Point(3.0,641));
        temp.list.add(new Stat_Point(3.0,643));
        temp.list.add(new Stat_Point(3.0,644));
        temp.list.add(new Stat_Point(3.0,654));
        temp.list.add(new Stat_Point(3.0,655));
        temp.list.add(new Stat_Point(3.0,659));
        temp.list.add(new Stat_Point(0.0,660));
        temp.list.add(new Stat_Point(3.0,662));
        temp.list.add(new Stat_Point(3.0,663));
        temp.list.add(new Stat_Point(3.0,664));
        temp.list.add(new Stat_Point(3.0,666));
        temp.list.add(new Stat_Point(3.0,670));
        temp.list.add(new Stat_Point(3.0,674));
        temp.list.add(new Stat_Point(3.0,678));
        temp.list.add(new Stat_Point(3.0,682));
        temp.list.add(new Stat_Point(3.0,686));
        temp.list.add(new Stat_Point(3.0,690));
        temp.list.add(new Stat_Point(3.0,694));
        temp.list.add(new Stat_Point(3.0,701));
        temp.list.add(new Stat_Point(3.0,703));
        temp.list.add(new Stat_Point(3.0,704));
        temp.list.add(new Stat_Point(3.0,714));
        temp.list.add(new Stat_Point(3.0,715));
        temp.list.add(new Stat_Point(3.0,719));
        temp.list.add(new Stat_Point(0.0,720));
        temp.list.add(new Stat_Point(3.0,722));
        temp.list.add(new Stat_Point(3.0,723));
        temp.list.add(new Stat_Point(3.0,724));
        temp.list.add(new Stat_Point(3.0,726));
        temp.list.add(new Stat_Point(3.0,730));
        temp.list.add(new Stat_Point(3.0,734));
        temp.list.add(new Stat_Point(3.0,738));
        temp.list.add(new Stat_Point(3.0,742));
        temp.list.add(new Stat_Point(3.0,746));
        temp.list.add(new Stat_Point(3.0,750));
        temp.list.add(new Stat_Point(3.0,754));
        temp.list.add(new Stat_Point(3.0,761));
        temp.list.add(new Stat_Point(3.0,763));
        temp.list.add(new Stat_Point(3.0,764));
        temp.list.add(new Stat_Point(3.0,774));
        temp.list.add(new Stat_Point(3.0,775));
        temp.list.add(new Stat_Point(3.0,779));
        temp.list.add(new Stat_Point(0.0,780));
        temp.list.add(new Stat_Point(3.0,782));
        temp.list.add(new Stat_Point(3.0,783));
        temp.list.add(new Stat_Point(3.0,784));
        temp.list.add(new Stat_Point(3.0,786));
        temp.list.add(new Stat_Point(3.0,790));
        temp.list.add(new Stat_Point(3.0,794));
        temp.list.add(new Stat_Point(3.0,798));
        temp.list.add(new Stat_Point(3.0,802));
        temp.list.add(new Stat_Point(3.0,806));
        temp.list.add(new Stat_Point(3.0,810));
        temp.list.add(new Stat_Point(3.0,814));
        temp.list.add(new Stat_Point(3.0,821));
        temp.list.add(new Stat_Point(3.0,823));
        temp.list.add(new Stat_Point(3.0,824));
        temp.list.add(new Stat_Point(3.0,834));
        temp.list.add(new Stat_Point(3.0,835));
        temp.list.add(new Stat_Point(3.0,839));
        temp.list.add(new Stat_Point(0.0,840));
        temp.list.add(new Stat_Point(3.0,842));
        temp.list.add(new Stat_Point(3.0,843));
        temp.list.add(new Stat_Point(3.0,844));
        temp.list.add(new Stat_Point(3.0,846));
        temp.list.add(new Stat_Point(3.0,850));
        temp.list.add(new Stat_Point(3.0,854));
        temp.list.add(new Stat_Point(3.0,858));
        temp.list.add(new Stat_Point(3.0,862));
        temp.list.add(new Stat_Point(3.0,866));
        temp.list.add(new Stat_Point(3.0,870));
        temp.list.add(new Stat_Point(3.0,874));
        temp.list.add(new Stat_Point(3.0,881));
        temp.list.add(new Stat_Point(3.0,883));
        temp.list.add(new Stat_Point(3.0,884));
        temp.list.add(new Stat_Point(3.0,894));
        temp.list.add(new Stat_Point(3.0,895));
        temp.list.add(new Stat_Point(3.0,899));
        temp.list.add(new Stat_Point(0.0,900));
        temp.list.add(new Stat_Point(3.0,902));
        temp.list.add(new Stat_Point(3.0,903));
        temp.list.add(new Stat_Point(3.0,904));
        temp.list.add(new Stat_Point(3.0,906));
        temp.list.add(new Stat_Point(3.0,910));
        temp.list.add(new Stat_Point(3.0,914));
        temp.list.add(new Stat_Point(3.0,918));
        temp.list.add(new Stat_Point(3.0,922));
        temp.list.add(new Stat_Point(3.0,926));
        temp.list.add(new Stat_Point(3.0,930));
        temp.list.add(new Stat_Point(3.0,934));
        temp.list.add(new Stat_Point(3.0,941));
        temp.list.add(new Stat_Point(3.0,943));
        temp.list.add(new Stat_Point(3.0,944));
        temp.list.add(new Stat_Point(3.0,954));
        temp.list.add(new Stat_Point(3.0,955));
        temp.list.add(new Stat_Point(3.0,959));
        temp.list.add(new Stat_Point(0.0,960));
        temp.list.add(new Stat_Point(3.0,962));
        temp.list.add(new Stat_Point(3.0,963));
        temp.list.add(new Stat_Point(3.0,964));
        temp.list.add(new Stat_Point(3.0,966));
        temp.list.add(new Stat_Point(3.0,970));
        temp.list.add(new Stat_Point(3.0,974));
        temp.list.add(new Stat_Point(3.0,978));
        temp.list.add(new Stat_Point(3.0,982));
        temp.list.add(new Stat_Point(3.0,986));
        temp.list.add(new Stat_Point(3.0,990));
        temp.list.add(new Stat_Point(3.0,994));
        temp.list.add(new Stat_Point(3.0,1001));
        temp.list.add(new Stat_Point(3.0,1003));
        temp.list.add(new Stat_Point(3.0,1004));
        temp.list.add(new Stat_Point(3.0,1014));
        temp.list.add(new Stat_Point(3.0,1015));
        temp.list.add(new Stat_Point(3.0,1019));
        temp.list.add(new Stat_Point(0.0,1020));
        temp.list.add(new Stat_Point(3.0,1022));
        temp.list.add(new Stat_Point(3.0,1023));
        temp.list.add(new Stat_Point(3.0,1024));
        temp.list.add(new Stat_Point(3.0,1026));
        temp.list.add(new Stat_Point(3.0,1030));
        temp.list.add(new Stat_Point(3.0,1034));
        temp.list.add(new Stat_Point(3.0,1038));
        temp.list.add(new Stat_Point(3.0,1042));
        temp.list.add(new Stat_Point(3.0,1046));
        temp.list.add(new Stat_Point(3.0,1050));
        temp.list.add(new Stat_Point(3.0,1054));
        temp.list.add(new Stat_Point(3.0,1061));
        temp.list.add(new Stat_Point(3.0,1063));
        temp.list.add(new Stat_Point(3.0,1064));
        temp.list.add(new Stat_Point(3.0,1074));
        temp.list.add(new Stat_Point(3.0,1075));
        temp.list.add(new Stat_Point(3.0,1079));
        temp.list.add(new Stat_Point(0.0,1080));
        temp.list.add(new Stat_Point(3.0,1082));
        temp.list.add(new Stat_Point(3.0,1083));
        temp.list.add(new Stat_Point(3.0,1084));
        temp.list.add(new Stat_Point(3.0,1086));
        temp.list.add(new Stat_Point(3.0,1090));
        temp.list.add(new Stat_Point(3.0,1094));
        temp.list.add(new Stat_Point(3.0,1098));
        temp.list.add(new Stat_Point(3.0,1102));
        temp.list.add(new Stat_Point(3.0,1106));
        temp.list.add(new Stat_Point(3.0,1110));
        temp.list.add(new Stat_Point(3.0,1114));
        temp.list.add(new Stat_Point(3.0,1121));
        temp.list.add(new Stat_Point(3.0,1123));
        temp.list.add(new Stat_Point(3.0,1124));
        temp.list.add(new Stat_Point(3.0,1134));
        temp.list.add(new Stat_Point(3.0,1135));
        temp.list.add(new Stat_Point(3.0,1139));
        temp.list.add(new Stat_Point(0.0,1140));
        temp.list.add(new Stat_Point(3.0,1142));
        temp.list.add(new Stat_Point(3.0,1143));
        temp.list.add(new Stat_Point(3.0,1144));
        temp.list.add(new Stat_Point(3.0,1146));
        temp.list.add(new Stat_Point(3.0,1150));
        temp.list.add(new Stat_Point(3.0,1154));
        temp.list.add(new Stat_Point(3.0,1158));
        temp.list.add(new Stat_Point(3.0,1162));
        temp.list.add(new Stat_Point(3.0,1166));
        temp.list.add(new Stat_Point(3.0,1170));
        temp.list.add(new Stat_Point(3.0,1174));
        temp.list.add(new Stat_Point(3.0,1181));
        temp.list.add(new Stat_Point(3.0,1183));
        temp.list.add(new Stat_Point(3.0,1184));
        temp.list.add(new Stat_Point(3.0,1194));
        temp.list.add(new Stat_Point(3.0,1195));
        temp.list.add(new Stat_Point(3.0,1199));
        temp.list.add(new Stat_Point(0.0,1200));

        for(Stat_Point p : temp.list){
            temp.total += p.value;
        }
        temp.average = temp.total/temp.duration;
        map.put(3,temp);

        temp = new Stat();
        temp.duration = 1200;
        temp.list.add(new Stat_Point(3.0,2));
        temp.list.add(new Stat_Point(3.0,3));
        temp.list.add(new Stat_Point(3.0,4));
        temp.list.add(new Stat_Point(3.0,6));
        temp.list.add(new Stat_Point(3.0,10));
        temp.list.add(new Stat_Point(3.0,14));
        temp.list.add(new Stat_Point(3.0,18));
        temp.list.add(new Stat_Point(3.0,22));
        temp.list.add(new Stat_Point(3.0,26));
        temp.list.add(new Stat_Point(3.0,30));
        temp.list.add(new Stat_Point(3.0,34));
        temp.list.add(new Stat_Point(3.0,41));
        temp.list.add(new Stat_Point(3.0,43));
        temp.list.add(new Stat_Point(3.0,44));
        temp.list.add(new Stat_Point(3.0,54));
        temp.list.add(new Stat_Point(3.0,55));
        temp.list.add(new Stat_Point(3.0,59));
        temp.list.add(new Stat_Point(0.0,60));
        temp.list.add(new Stat_Point(3.0,62));
        temp.list.add(new Stat_Point(3.0,63));
        temp.list.add(new Stat_Point(3.0,64));
        temp.list.add(new Stat_Point(3.0,66));
        temp.list.add(new Stat_Point(3.0,70));
        temp.list.add(new Stat_Point(3.0,74));
        temp.list.add(new Stat_Point(3.0,78));
        temp.list.add(new Stat_Point(3.0,82));
        temp.list.add(new Stat_Point(3.0,86));
        temp.list.add(new Stat_Point(3.0,90));
        temp.list.add(new Stat_Point(3.0,94));
        temp.list.add(new Stat_Point(3.0,101));
        temp.list.add(new Stat_Point(3.0,103));
        temp.list.add(new Stat_Point(3.0,104));
        temp.list.add(new Stat_Point(3.0,114));
        temp.list.add(new Stat_Point(3.0,115));
        temp.list.add(new Stat_Point(3.0,119));
        temp.list.add(new Stat_Point(0.0,120));
        temp.list.add(new Stat_Point(3.0,122));
        temp.list.add(new Stat_Point(3.0,123));
        temp.list.add(new Stat_Point(3.0,124));
        temp.list.add(new Stat_Point(3.0,126));
        temp.list.add(new Stat_Point(3.0,130));
        temp.list.add(new Stat_Point(3.0,134));
        temp.list.add(new Stat_Point(3.0,138));
        temp.list.add(new Stat_Point(3.0,142));
        temp.list.add(new Stat_Point(3.0,146));
        temp.list.add(new Stat_Point(3.0,150));
        temp.list.add(new Stat_Point(3.0,154));
        temp.list.add(new Stat_Point(3.0,161));
        temp.list.add(new Stat_Point(3.0,163));
        temp.list.add(new Stat_Point(3.0,164));
        temp.list.add(new Stat_Point(3.0,174));
        temp.list.add(new Stat_Point(3.0,175));
        temp.list.add(new Stat_Point(3.0,179));
        temp.list.add(new Stat_Point(0.0,180));
        temp.list.add(new Stat_Point(3.0,182));
        temp.list.add(new Stat_Point(3.0,183));
        temp.list.add(new Stat_Point(3.0,184));
        temp.list.add(new Stat_Point(3.0,186));
        temp.list.add(new Stat_Point(3.0,190));
        temp.list.add(new Stat_Point(3.0,194));
        temp.list.add(new Stat_Point(3.0,198));
        temp.list.add(new Stat_Point(3.0,202));
        temp.list.add(new Stat_Point(3.0,206));
        temp.list.add(new Stat_Point(3.0,210));
        temp.list.add(new Stat_Point(3.0,214));
        temp.list.add(new Stat_Point(3.0,221));
        temp.list.add(new Stat_Point(3.0,223));
        temp.list.add(new Stat_Point(3.0,224));
        temp.list.add(new Stat_Point(3.0,234));
        temp.list.add(new Stat_Point(3.0,235));
        temp.list.add(new Stat_Point(3.0,239));
        temp.list.add(new Stat_Point(0.0,240));
        temp.list.add(new Stat_Point(3.0,242));
        temp.list.add(new Stat_Point(3.0,243));
        temp.list.add(new Stat_Point(3.0,244));
        temp.list.add(new Stat_Point(3.0,246));
        temp.list.add(new Stat_Point(3.0,250));
        temp.list.add(new Stat_Point(3.0,254));
        temp.list.add(new Stat_Point(3.0,258));
        temp.list.add(new Stat_Point(3.0,262));
        temp.list.add(new Stat_Point(3.0,266));
        temp.list.add(new Stat_Point(3.0,270));
        temp.list.add(new Stat_Point(3.0,274));
        temp.list.add(new Stat_Point(3.0,281));
        temp.list.add(new Stat_Point(3.0,283));
        temp.list.add(new Stat_Point(3.0,284));
        temp.list.add(new Stat_Point(3.0,294));
        temp.list.add(new Stat_Point(3.0,295));
        temp.list.add(new Stat_Point(3.0,299));
        temp.list.add(new Stat_Point(0.0,300));
        temp.list.add(new Stat_Point(3.0,302));
        temp.list.add(new Stat_Point(3.0,303));
        temp.list.add(new Stat_Point(3.0,304));
        temp.list.add(new Stat_Point(3.0,306));
        temp.list.add(new Stat_Point(3.0,310));
        temp.list.add(new Stat_Point(3.0,314));
        temp.list.add(new Stat_Point(3.0,318));
        temp.list.add(new Stat_Point(3.0,322));
        temp.list.add(new Stat_Point(3.0,326));
        temp.list.add(new Stat_Point(3.0,330));
        temp.list.add(new Stat_Point(3.0,334));
        temp.list.add(new Stat_Point(3.0,341));
        temp.list.add(new Stat_Point(3.0,343));
        temp.list.add(new Stat_Point(3.0,344));
        temp.list.add(new Stat_Point(3.0,354));
        temp.list.add(new Stat_Point(3.0,355));
        temp.list.add(new Stat_Point(3.0,359));
        temp.list.add(new Stat_Point(0.0,360));
        temp.list.add(new Stat_Point(3.0,362));
        temp.list.add(new Stat_Point(3.0,363));
        temp.list.add(new Stat_Point(3.0,364));
        temp.list.add(new Stat_Point(3.0,366));
        temp.list.add(new Stat_Point(3.0,370));
        temp.list.add(new Stat_Point(3.0,374));
        temp.list.add(new Stat_Point(3.0,378));
        temp.list.add(new Stat_Point(3.0,382));
        temp.list.add(new Stat_Point(3.0,386));
        temp.list.add(new Stat_Point(3.0,390));
        temp.list.add(new Stat_Point(3.0,394));
        temp.list.add(new Stat_Point(3.0,401));
        temp.list.add(new Stat_Point(3.0,403));
        temp.list.add(new Stat_Point(3.0,404));
        temp.list.add(new Stat_Point(3.0,414));
        temp.list.add(new Stat_Point(3.0,415));
        temp.list.add(new Stat_Point(3.0,419));
        temp.list.add(new Stat_Point(0.0,420));
        temp.list.add(new Stat_Point(3.0,422));
        temp.list.add(new Stat_Point(3.0,423));
        temp.list.add(new Stat_Point(3.0,424));
        temp.list.add(new Stat_Point(3.0,426));
        temp.list.add(new Stat_Point(3.0,430));
        temp.list.add(new Stat_Point(3.0,434));
        temp.list.add(new Stat_Point(3.0,438));
        temp.list.add(new Stat_Point(3.0,442));
        temp.list.add(new Stat_Point(3.0,446));
        temp.list.add(new Stat_Point(3.0,450));
        temp.list.add(new Stat_Point(3.0,454));
        temp.list.add(new Stat_Point(3.0,461));
        temp.list.add(new Stat_Point(3.0,463));
        temp.list.add(new Stat_Point(3.0,464));
        temp.list.add(new Stat_Point(3.0,474));
        temp.list.add(new Stat_Point(3.0,475));
        temp.list.add(new Stat_Point(3.0,479));
        temp.list.add(new Stat_Point(0.0,480));
        temp.list.add(new Stat_Point(3.0,482));
        temp.list.add(new Stat_Point(3.0,483));
        temp.list.add(new Stat_Point(3.0,484));
        temp.list.add(new Stat_Point(3.0,486));
        temp.list.add(new Stat_Point(3.0,490));
        temp.list.add(new Stat_Point(3.0,494));
        temp.list.add(new Stat_Point(3.0,498));
        temp.list.add(new Stat_Point(3.0,502));
        temp.list.add(new Stat_Point(3.0,506));
        temp.list.add(new Stat_Point(3.0,510));
        temp.list.add(new Stat_Point(3.0,514));
        temp.list.add(new Stat_Point(3.0,521));
        temp.list.add(new Stat_Point(3.0,523));
        temp.list.add(new Stat_Point(3.0,524));
        temp.list.add(new Stat_Point(3.0,534));
        temp.list.add(new Stat_Point(3.0,535));
        temp.list.add(new Stat_Point(3.0,539));
        temp.list.add(new Stat_Point(0.0,540));
        temp.list.add(new Stat_Point(3.0,542));
        temp.list.add(new Stat_Point(3.0,543));
        temp.list.add(new Stat_Point(3.0,544));
        temp.list.add(new Stat_Point(3.0,546));
        temp.list.add(new Stat_Point(3.0,550));
        temp.list.add(new Stat_Point(3.0,554));
        temp.list.add(new Stat_Point(3.0,558));
        temp.list.add(new Stat_Point(3.0,562));
        temp.list.add(new Stat_Point(3.0,566));
        temp.list.add(new Stat_Point(3.0,570));
        temp.list.add(new Stat_Point(3.0,574));
        temp.list.add(new Stat_Point(3.0,581));
        temp.list.add(new Stat_Point(3.0,583));
        temp.list.add(new Stat_Point(3.0,584));
        temp.list.add(new Stat_Point(3.0,594));
        temp.list.add(new Stat_Point(3.0,595));
        temp.list.add(new Stat_Point(3.0,599));
        temp.list.add(new Stat_Point(0.0,600));
        temp.list.add(new Stat_Point(3.0,602));
        temp.list.add(new Stat_Point(3.0,603));
        temp.list.add(new Stat_Point(3.0,604));
        temp.list.add(new Stat_Point(3.0,606));
        temp.list.add(new Stat_Point(3.0,610));
        temp.list.add(new Stat_Point(3.0,614));
        temp.list.add(new Stat_Point(3.0,618));
        temp.list.add(new Stat_Point(3.0,622));
        temp.list.add(new Stat_Point(3.0,626));
        temp.list.add(new Stat_Point(3.0,630));
        temp.list.add(new Stat_Point(3.0,634));
        temp.list.add(new Stat_Point(3.0,641));
        temp.list.add(new Stat_Point(3.0,643));
        temp.list.add(new Stat_Point(3.0,644));
        temp.list.add(new Stat_Point(3.0,654));
        temp.list.add(new Stat_Point(3.0,655));
        temp.list.add(new Stat_Point(3.0,659));
        temp.list.add(new Stat_Point(0.0,660));
        temp.list.add(new Stat_Point(3.0,662));
        temp.list.add(new Stat_Point(3.0,663));
        temp.list.add(new Stat_Point(3.0,664));
        temp.list.add(new Stat_Point(3.0,666));
        temp.list.add(new Stat_Point(3.0,670));
        temp.list.add(new Stat_Point(3.0,674));
        temp.list.add(new Stat_Point(3.0,678));
        temp.list.add(new Stat_Point(3.0,682));
        temp.list.add(new Stat_Point(3.0,686));
        temp.list.add(new Stat_Point(3.0,690));
        temp.list.add(new Stat_Point(3.0,694));
        temp.list.add(new Stat_Point(3.0,701));
        temp.list.add(new Stat_Point(3.0,703));
        temp.list.add(new Stat_Point(3.0,704));
        temp.list.add(new Stat_Point(3.0,714));
        temp.list.add(new Stat_Point(3.0,715));
        temp.list.add(new Stat_Point(3.0,719));
        temp.list.add(new Stat_Point(0.0,720));
        temp.list.add(new Stat_Point(3.0,722));
        temp.list.add(new Stat_Point(3.0,723));
        temp.list.add(new Stat_Point(3.0,724));
        temp.list.add(new Stat_Point(3.0,726));
        temp.list.add(new Stat_Point(3.0,730));
        temp.list.add(new Stat_Point(3.0,734));
        temp.list.add(new Stat_Point(3.0,738));
        temp.list.add(new Stat_Point(3.0,742));
        temp.list.add(new Stat_Point(3.0,746));
        temp.list.add(new Stat_Point(3.0,750));
        temp.list.add(new Stat_Point(3.0,754));
        temp.list.add(new Stat_Point(3.0,761));
        temp.list.add(new Stat_Point(3.0,763));
        temp.list.add(new Stat_Point(3.0,764));
        temp.list.add(new Stat_Point(3.0,774));
        temp.list.add(new Stat_Point(3.0,775));
        temp.list.add(new Stat_Point(3.0,779));
        temp.list.add(new Stat_Point(0.0,780));
        temp.list.add(new Stat_Point(3.0,782));
        temp.list.add(new Stat_Point(3.0,783));
        temp.list.add(new Stat_Point(3.0,784));
        temp.list.add(new Stat_Point(3.0,786));
        temp.list.add(new Stat_Point(3.0,790));
        temp.list.add(new Stat_Point(3.0,794));
        temp.list.add(new Stat_Point(3.0,798));
        temp.list.add(new Stat_Point(3.0,802));
        temp.list.add(new Stat_Point(3.0,806));
        temp.list.add(new Stat_Point(3.0,810));
        temp.list.add(new Stat_Point(3.0,814));
        temp.list.add(new Stat_Point(3.0,821));
        temp.list.add(new Stat_Point(3.0,823));
        temp.list.add(new Stat_Point(3.0,824));
        temp.list.add(new Stat_Point(3.0,834));
        temp.list.add(new Stat_Point(3.0,835));
        temp.list.add(new Stat_Point(3.0,839));
        temp.list.add(new Stat_Point(0.0,840));
        temp.list.add(new Stat_Point(3.0,842));
        temp.list.add(new Stat_Point(3.0,843));
        temp.list.add(new Stat_Point(3.0,844));
        temp.list.add(new Stat_Point(3.0,846));
        temp.list.add(new Stat_Point(3.0,850));
        temp.list.add(new Stat_Point(3.0,854));
        temp.list.add(new Stat_Point(3.0,858));
        temp.list.add(new Stat_Point(3.0,862));
        temp.list.add(new Stat_Point(3.0,866));
        temp.list.add(new Stat_Point(3.0,870));
        temp.list.add(new Stat_Point(3.0,874));
        temp.list.add(new Stat_Point(3.0,881));
        temp.list.add(new Stat_Point(3.0,883));
        temp.list.add(new Stat_Point(3.0,884));
        temp.list.add(new Stat_Point(3.0,894));
        temp.list.add(new Stat_Point(3.0,895));
        temp.list.add(new Stat_Point(3.0,899));
        temp.list.add(new Stat_Point(0.0,900));
        temp.list.add(new Stat_Point(3.0,902));
        temp.list.add(new Stat_Point(3.0,903));
        temp.list.add(new Stat_Point(3.0,904));
        temp.list.add(new Stat_Point(3.0,906));
        temp.list.add(new Stat_Point(3.0,910));
        temp.list.add(new Stat_Point(3.0,914));
        temp.list.add(new Stat_Point(3.0,918));
        temp.list.add(new Stat_Point(3.0,922));
        temp.list.add(new Stat_Point(3.0,926));
        temp.list.add(new Stat_Point(3.0,930));
        temp.list.add(new Stat_Point(3.0,934));
        temp.list.add(new Stat_Point(3.0,941));
        temp.list.add(new Stat_Point(3.0,943));
        temp.list.add(new Stat_Point(3.0,944));
        temp.list.add(new Stat_Point(3.0,954));
        temp.list.add(new Stat_Point(3.0,955));
        temp.list.add(new Stat_Point(3.0,959));
        temp.list.add(new Stat_Point(0.0,960));
        temp.list.add(new Stat_Point(3.0,962));
        temp.list.add(new Stat_Point(3.0,963));
        temp.list.add(new Stat_Point(3.0,964));
        temp.list.add(new Stat_Point(3.0,966));
        temp.list.add(new Stat_Point(3.0,970));
        temp.list.add(new Stat_Point(3.0,974));
        temp.list.add(new Stat_Point(3.0,978));
        temp.list.add(new Stat_Point(3.0,982));
        temp.list.add(new Stat_Point(3.0,986));
        temp.list.add(new Stat_Point(3.0,990));
        temp.list.add(new Stat_Point(3.0,994));
        temp.list.add(new Stat_Point(3.0,1001));
        temp.list.add(new Stat_Point(3.0,1003));
        temp.list.add(new Stat_Point(3.0,1004));
        temp.list.add(new Stat_Point(3.0,1014));
        temp.list.add(new Stat_Point(3.0,1015));
        temp.list.add(new Stat_Point(3.0,1019));
        temp.list.add(new Stat_Point(0.0,1020));
        temp.list.add(new Stat_Point(3.0,1022));
        temp.list.add(new Stat_Point(3.0,1023));
        temp.list.add(new Stat_Point(3.0,1024));
        temp.list.add(new Stat_Point(3.0,1026));
        temp.list.add(new Stat_Point(3.0,1030));
        temp.list.add(new Stat_Point(3.0,1034));
        temp.list.add(new Stat_Point(3.0,1038));
        temp.list.add(new Stat_Point(3.0,1042));
        temp.list.add(new Stat_Point(3.0,1046));
        temp.list.add(new Stat_Point(3.0,1050));
        temp.list.add(new Stat_Point(3.0,1054));
        temp.list.add(new Stat_Point(3.0,1061));
        temp.list.add(new Stat_Point(3.0,1063));
        temp.list.add(new Stat_Point(3.0,1064));
        temp.list.add(new Stat_Point(3.0,1074));
        temp.list.add(new Stat_Point(3.0,1075));
        temp.list.add(new Stat_Point(3.0,1079));
        temp.list.add(new Stat_Point(0.0,1080));
        temp.list.add(new Stat_Point(3.0,1082));
        temp.list.add(new Stat_Point(3.0,1083));
        temp.list.add(new Stat_Point(3.0,1084));
        temp.list.add(new Stat_Point(3.0,1086));
        temp.list.add(new Stat_Point(3.0,1090));
        temp.list.add(new Stat_Point(3.0,1094));
        temp.list.add(new Stat_Point(3.0,1098));
        temp.list.add(new Stat_Point(3.0,1102));
        temp.list.add(new Stat_Point(3.0,1106));
        temp.list.add(new Stat_Point(3.0,1110));
        temp.list.add(new Stat_Point(3.0,1114));
        temp.list.add(new Stat_Point(3.0,1121));
        temp.list.add(new Stat_Point(3.0,1123));
        temp.list.add(new Stat_Point(3.0,1124));
        temp.list.add(new Stat_Point(3.0,1134));
        temp.list.add(new Stat_Point(3.0,1135));
        temp.list.add(new Stat_Point(3.0,1139));
        temp.list.add(new Stat_Point(0.0,1140));
        temp.list.add(new Stat_Point(3.0,1142));
        temp.list.add(new Stat_Point(3.0,1143));
        temp.list.add(new Stat_Point(3.0,1144));
        temp.list.add(new Stat_Point(3.0,1146));
        temp.list.add(new Stat_Point(3.0,1150));
        temp.list.add(new Stat_Point(3.0,1154));
        temp.list.add(new Stat_Point(3.0,1158));
        temp.list.add(new Stat_Point(3.0,1162));
        temp.list.add(new Stat_Point(3.0,1166));
        temp.list.add(new Stat_Point(3.0,1170));
        temp.list.add(new Stat_Point(3.0,1174));
        temp.list.add(new Stat_Point(3.0,1181));
        temp.list.add(new Stat_Point(3.0,1183));
        temp.list.add(new Stat_Point(3.0,1184));
        temp.list.add(new Stat_Point(3.0,1194));
        temp.list.add(new Stat_Point(3.0,1195));
        temp.list.add(new Stat_Point(3.0,1199));
        temp.list.add(new Stat_Point(0.0,1200));

        for(Stat_Point p : temp.list){
            temp.total += p.value;
        }
        temp.average = temp.total/temp.duration;
        map.put(4,temp);

        temp = new Stat();
        temp.duration = 1200;
        temp.list.add(new Stat_Point(3.0,2));
        temp.list.add(new Stat_Point(3.0,3));
        temp.list.add(new Stat_Point(3.0,4));
        temp.list.add(new Stat_Point(3.0,6));
        temp.list.add(new Stat_Point(3.0,10));
        temp.list.add(new Stat_Point(3.0,14));
        temp.list.add(new Stat_Point(3.0,18));
        temp.list.add(new Stat_Point(3.0,22));
        temp.list.add(new Stat_Point(3.0,26));
        temp.list.add(new Stat_Point(3.0,30));
        temp.list.add(new Stat_Point(3.0,34));
        temp.list.add(new Stat_Point(3.0,41));
        temp.list.add(new Stat_Point(3.0,43));
        temp.list.add(new Stat_Point(3.0,44));
        temp.list.add(new Stat_Point(3.0,54));
        temp.list.add(new Stat_Point(3.0,55));
        temp.list.add(new Stat_Point(3.0,59));
        temp.list.add(new Stat_Point(0.0,60));
        temp.list.add(new Stat_Point(3.0,62));
        temp.list.add(new Stat_Point(3.0,63));
        temp.list.add(new Stat_Point(3.0,64));
        temp.list.add(new Stat_Point(3.0,66));
        temp.list.add(new Stat_Point(3.0,70));
        temp.list.add(new Stat_Point(3.0,74));
        temp.list.add(new Stat_Point(3.0,78));
        temp.list.add(new Stat_Point(3.0,82));
        temp.list.add(new Stat_Point(3.0,86));
        temp.list.add(new Stat_Point(3.0,90));
        temp.list.add(new Stat_Point(3.0,94));
        temp.list.add(new Stat_Point(3.0,101));
        temp.list.add(new Stat_Point(3.0,103));
        temp.list.add(new Stat_Point(3.0,104));
        temp.list.add(new Stat_Point(3.0,114));
        temp.list.add(new Stat_Point(3.0,115));
        temp.list.add(new Stat_Point(3.0,119));
        temp.list.add(new Stat_Point(0.0,120));
        temp.list.add(new Stat_Point(3.0,122));
        temp.list.add(new Stat_Point(3.0,123));
        temp.list.add(new Stat_Point(3.0,124));
        temp.list.add(new Stat_Point(3.0,126));
        temp.list.add(new Stat_Point(3.0,130));
        temp.list.add(new Stat_Point(3.0,134));
        temp.list.add(new Stat_Point(3.0,138));
        temp.list.add(new Stat_Point(3.0,142));
        temp.list.add(new Stat_Point(3.0,146));
        temp.list.add(new Stat_Point(3.0,150));
        temp.list.add(new Stat_Point(3.0,154));
        temp.list.add(new Stat_Point(3.0,161));
        temp.list.add(new Stat_Point(3.0,163));
        temp.list.add(new Stat_Point(3.0,164));
        temp.list.add(new Stat_Point(3.0,174));
        temp.list.add(new Stat_Point(3.0,175));
        temp.list.add(new Stat_Point(3.0,179));
        temp.list.add(new Stat_Point(0.0,180));
        temp.list.add(new Stat_Point(3.0,182));
        temp.list.add(new Stat_Point(3.0,183));
        temp.list.add(new Stat_Point(3.0,184));
        temp.list.add(new Stat_Point(3.0,186));
        temp.list.add(new Stat_Point(3.0,190));
        temp.list.add(new Stat_Point(3.0,194));
        temp.list.add(new Stat_Point(3.0,198));
        temp.list.add(new Stat_Point(3.0,202));
        temp.list.add(new Stat_Point(3.0,206));
        temp.list.add(new Stat_Point(3.0,210));
        temp.list.add(new Stat_Point(3.0,214));
        temp.list.add(new Stat_Point(3.0,221));
        temp.list.add(new Stat_Point(3.0,223));
        temp.list.add(new Stat_Point(3.0,224));
        temp.list.add(new Stat_Point(3.0,234));
        temp.list.add(new Stat_Point(3.0,235));
        temp.list.add(new Stat_Point(3.0,239));
        temp.list.add(new Stat_Point(0.0,240));
        temp.list.add(new Stat_Point(3.0,242));
        temp.list.add(new Stat_Point(3.0,243));
        temp.list.add(new Stat_Point(3.0,244));
        temp.list.add(new Stat_Point(3.0,246));
        temp.list.add(new Stat_Point(3.0,250));
        temp.list.add(new Stat_Point(3.0,254));
        temp.list.add(new Stat_Point(3.0,258));
        temp.list.add(new Stat_Point(3.0,262));
        temp.list.add(new Stat_Point(3.0,266));
        temp.list.add(new Stat_Point(3.0,270));
        temp.list.add(new Stat_Point(3.0,274));
        temp.list.add(new Stat_Point(3.0,281));
        temp.list.add(new Stat_Point(3.0,283));
        temp.list.add(new Stat_Point(3.0,284));
        temp.list.add(new Stat_Point(3.0,294));
        temp.list.add(new Stat_Point(3.0,295));
        temp.list.add(new Stat_Point(3.0,299));
        temp.list.add(new Stat_Point(0.0,300));
        temp.list.add(new Stat_Point(3.0,302));
        temp.list.add(new Stat_Point(3.0,303));
        temp.list.add(new Stat_Point(3.0,304));
        temp.list.add(new Stat_Point(3.0,306));
        temp.list.add(new Stat_Point(3.0,310));
        temp.list.add(new Stat_Point(3.0,314));
        temp.list.add(new Stat_Point(3.0,318));
        temp.list.add(new Stat_Point(3.0,322));
        temp.list.add(new Stat_Point(3.0,326));
        temp.list.add(new Stat_Point(3.0,330));
        temp.list.add(new Stat_Point(3.0,334));
        temp.list.add(new Stat_Point(3.0,341));
        temp.list.add(new Stat_Point(3.0,343));
        temp.list.add(new Stat_Point(3.0,344));
        temp.list.add(new Stat_Point(3.0,354));
        temp.list.add(new Stat_Point(3.0,355));
        temp.list.add(new Stat_Point(3.0,359));
        temp.list.add(new Stat_Point(0.0,360));
        temp.list.add(new Stat_Point(3.0,362));
        temp.list.add(new Stat_Point(3.0,363));
        temp.list.add(new Stat_Point(3.0,364));
        temp.list.add(new Stat_Point(3.0,366));
        temp.list.add(new Stat_Point(3.0,370));
        temp.list.add(new Stat_Point(3.0,374));
        temp.list.add(new Stat_Point(3.0,378));
        temp.list.add(new Stat_Point(3.0,382));
        temp.list.add(new Stat_Point(3.0,386));
        temp.list.add(new Stat_Point(3.0,390));
        temp.list.add(new Stat_Point(3.0,394));
        temp.list.add(new Stat_Point(3.0,401));
        temp.list.add(new Stat_Point(3.0,403));
        temp.list.add(new Stat_Point(3.0,404));
        temp.list.add(new Stat_Point(3.0,414));
        temp.list.add(new Stat_Point(3.0,415));
        temp.list.add(new Stat_Point(3.0,419));
        temp.list.add(new Stat_Point(0.0,420));
        temp.list.add(new Stat_Point(3.0,422));
        temp.list.add(new Stat_Point(3.0,423));
        temp.list.add(new Stat_Point(3.0,424));
        temp.list.add(new Stat_Point(3.0,426));
        temp.list.add(new Stat_Point(3.0,430));
        temp.list.add(new Stat_Point(3.0,434));
        temp.list.add(new Stat_Point(3.0,438));
        temp.list.add(new Stat_Point(3.0,442));
        temp.list.add(new Stat_Point(3.0,446));
        temp.list.add(new Stat_Point(3.0,450));
        temp.list.add(new Stat_Point(3.0,454));
        temp.list.add(new Stat_Point(3.0,461));
        temp.list.add(new Stat_Point(3.0,463));
        temp.list.add(new Stat_Point(3.0,464));
        temp.list.add(new Stat_Point(3.0,474));
        temp.list.add(new Stat_Point(3.0,475));
        temp.list.add(new Stat_Point(3.0,479));
        temp.list.add(new Stat_Point(0.0,480));
        temp.list.add(new Stat_Point(3.0,482));
        temp.list.add(new Stat_Point(3.0,483));
        temp.list.add(new Stat_Point(3.0,484));
        temp.list.add(new Stat_Point(3.0,486));
        temp.list.add(new Stat_Point(3.0,490));
        temp.list.add(new Stat_Point(3.0,494));
        temp.list.add(new Stat_Point(3.0,498));
        temp.list.add(new Stat_Point(3.0,502));
        temp.list.add(new Stat_Point(3.0,506));
        temp.list.add(new Stat_Point(3.0,510));
        temp.list.add(new Stat_Point(3.0,514));
        temp.list.add(new Stat_Point(3.0,521));
        temp.list.add(new Stat_Point(3.0,523));
        temp.list.add(new Stat_Point(3.0,524));
        temp.list.add(new Stat_Point(3.0,534));
        temp.list.add(new Stat_Point(3.0,535));
        temp.list.add(new Stat_Point(3.0,539));
        temp.list.add(new Stat_Point(0.0,540));
        temp.list.add(new Stat_Point(3.0,542));
        temp.list.add(new Stat_Point(3.0,543));
        temp.list.add(new Stat_Point(3.0,544));
        temp.list.add(new Stat_Point(3.0,546));
        temp.list.add(new Stat_Point(3.0,550));
        temp.list.add(new Stat_Point(3.0,554));
        temp.list.add(new Stat_Point(3.0,558));
        temp.list.add(new Stat_Point(3.0,562));
        temp.list.add(new Stat_Point(3.0,566));
        temp.list.add(new Stat_Point(3.0,570));
        temp.list.add(new Stat_Point(3.0,574));
        temp.list.add(new Stat_Point(3.0,581));
        temp.list.add(new Stat_Point(3.0,583));
        temp.list.add(new Stat_Point(3.0,584));
        temp.list.add(new Stat_Point(3.0,594));
        temp.list.add(new Stat_Point(3.0,595));
        temp.list.add(new Stat_Point(3.0,599));
        temp.list.add(new Stat_Point(0.0,600));
        temp.list.add(new Stat_Point(3.0,602));
        temp.list.add(new Stat_Point(3.0,603));
        temp.list.add(new Stat_Point(3.0,604));
        temp.list.add(new Stat_Point(3.0,606));
        temp.list.add(new Stat_Point(3.0,610));
        temp.list.add(new Stat_Point(3.0,614));
        temp.list.add(new Stat_Point(3.0,618));
        temp.list.add(new Stat_Point(3.0,622));
        temp.list.add(new Stat_Point(3.0,626));
        temp.list.add(new Stat_Point(3.0,630));
        temp.list.add(new Stat_Point(3.0,634));
        temp.list.add(new Stat_Point(3.0,641));
        temp.list.add(new Stat_Point(3.0,643));
        temp.list.add(new Stat_Point(3.0,644));
        temp.list.add(new Stat_Point(3.0,654));
        temp.list.add(new Stat_Point(3.0,655));
        temp.list.add(new Stat_Point(3.0,659));
        temp.list.add(new Stat_Point(0.0,660));
        temp.list.add(new Stat_Point(3.0,662));
        temp.list.add(new Stat_Point(3.0,663));
        temp.list.add(new Stat_Point(3.0,664));
        temp.list.add(new Stat_Point(3.0,666));
        temp.list.add(new Stat_Point(3.0,670));
        temp.list.add(new Stat_Point(3.0,674));
        temp.list.add(new Stat_Point(3.0,678));
        temp.list.add(new Stat_Point(3.0,682));
        temp.list.add(new Stat_Point(3.0,686));
        temp.list.add(new Stat_Point(3.0,690));
        temp.list.add(new Stat_Point(3.0,694));
        temp.list.add(new Stat_Point(3.0,701));
        temp.list.add(new Stat_Point(3.0,703));
        temp.list.add(new Stat_Point(3.0,704));
        temp.list.add(new Stat_Point(3.0,714));
        temp.list.add(new Stat_Point(3.0,715));
        temp.list.add(new Stat_Point(3.0,719));
        temp.list.add(new Stat_Point(0.0,720));
        temp.list.add(new Stat_Point(3.0,722));
        temp.list.add(new Stat_Point(3.0,723));
        temp.list.add(new Stat_Point(3.0,724));
        temp.list.add(new Stat_Point(3.0,726));
        temp.list.add(new Stat_Point(3.0,730));
        temp.list.add(new Stat_Point(3.0,734));
        temp.list.add(new Stat_Point(3.0,738));
        temp.list.add(new Stat_Point(3.0,742));
        temp.list.add(new Stat_Point(3.0,746));
        temp.list.add(new Stat_Point(3.0,750));
        temp.list.add(new Stat_Point(3.0,754));
        temp.list.add(new Stat_Point(3.0,761));
        temp.list.add(new Stat_Point(3.0,763));
        temp.list.add(new Stat_Point(3.0,764));
        temp.list.add(new Stat_Point(3.0,774));
        temp.list.add(new Stat_Point(3.0,775));
        temp.list.add(new Stat_Point(3.0,779));
        temp.list.add(new Stat_Point(0.0,780));
        temp.list.add(new Stat_Point(3.0,782));
        temp.list.add(new Stat_Point(3.0,783));
        temp.list.add(new Stat_Point(3.0,784));
        temp.list.add(new Stat_Point(3.0,786));
        temp.list.add(new Stat_Point(3.0,790));
        temp.list.add(new Stat_Point(3.0,794));
        temp.list.add(new Stat_Point(3.0,798));
        temp.list.add(new Stat_Point(3.0,802));
        temp.list.add(new Stat_Point(3.0,806));
        temp.list.add(new Stat_Point(3.0,810));
        temp.list.add(new Stat_Point(3.0,814));
        temp.list.add(new Stat_Point(3.0,821));
        temp.list.add(new Stat_Point(3.0,823));
        temp.list.add(new Stat_Point(3.0,824));
        temp.list.add(new Stat_Point(3.0,834));
        temp.list.add(new Stat_Point(3.0,835));
        temp.list.add(new Stat_Point(3.0,839));
        temp.list.add(new Stat_Point(0.0,840));
        temp.list.add(new Stat_Point(3.0,842));
        temp.list.add(new Stat_Point(3.0,843));
        temp.list.add(new Stat_Point(3.0,844));
        temp.list.add(new Stat_Point(3.0,846));
        temp.list.add(new Stat_Point(3.0,850));
        temp.list.add(new Stat_Point(3.0,854));
        temp.list.add(new Stat_Point(3.0,858));
        temp.list.add(new Stat_Point(3.0,862));
        temp.list.add(new Stat_Point(3.0,866));
        temp.list.add(new Stat_Point(3.0,870));
        temp.list.add(new Stat_Point(3.0,874));
        temp.list.add(new Stat_Point(3.0,881));
        temp.list.add(new Stat_Point(3.0,883));
        temp.list.add(new Stat_Point(3.0,884));
        temp.list.add(new Stat_Point(3.0,894));
        temp.list.add(new Stat_Point(3.0,895));
        temp.list.add(new Stat_Point(3.0,899));
        temp.list.add(new Stat_Point(0.0,900));
        temp.list.add(new Stat_Point(3.0,902));
        temp.list.add(new Stat_Point(3.0,903));
        temp.list.add(new Stat_Point(3.0,904));
        temp.list.add(new Stat_Point(3.0,906));
        temp.list.add(new Stat_Point(3.0,910));
        temp.list.add(new Stat_Point(3.0,914));
        temp.list.add(new Stat_Point(3.0,918));
        temp.list.add(new Stat_Point(3.0,922));
        temp.list.add(new Stat_Point(3.0,926));
        temp.list.add(new Stat_Point(3.0,930));
        temp.list.add(new Stat_Point(3.0,934));
        temp.list.add(new Stat_Point(3.0,941));
        temp.list.add(new Stat_Point(3.0,943));
        temp.list.add(new Stat_Point(3.0,944));
        temp.list.add(new Stat_Point(3.0,954));
        temp.list.add(new Stat_Point(3.0,955));
        temp.list.add(new Stat_Point(3.0,959));
        temp.list.add(new Stat_Point(0.0,960));
        temp.list.add(new Stat_Point(3.0,962));
        temp.list.add(new Stat_Point(3.0,963));
        temp.list.add(new Stat_Point(3.0,964));
        temp.list.add(new Stat_Point(3.0,966));
        temp.list.add(new Stat_Point(3.0,970));
        temp.list.add(new Stat_Point(3.0,974));
        temp.list.add(new Stat_Point(3.0,978));
        temp.list.add(new Stat_Point(3.0,982));
        temp.list.add(new Stat_Point(3.0,986));
        temp.list.add(new Stat_Point(3.0,990));
        temp.list.add(new Stat_Point(3.0,994));
        temp.list.add(new Stat_Point(3.0,1001));
        temp.list.add(new Stat_Point(3.0,1003));
        temp.list.add(new Stat_Point(3.0,1004));
        temp.list.add(new Stat_Point(3.0,1014));
        temp.list.add(new Stat_Point(3.0,1015));
        temp.list.add(new Stat_Point(3.0,1019));
        temp.list.add(new Stat_Point(0.0,1020));
        temp.list.add(new Stat_Point(3.0,1022));
        temp.list.add(new Stat_Point(3.0,1023));
        temp.list.add(new Stat_Point(3.0,1024));
        temp.list.add(new Stat_Point(3.0,1026));
        temp.list.add(new Stat_Point(3.0,1030));
        temp.list.add(new Stat_Point(3.0,1034));
        temp.list.add(new Stat_Point(3.0,1038));
        temp.list.add(new Stat_Point(3.0,1042));
        temp.list.add(new Stat_Point(3.0,1046));
        temp.list.add(new Stat_Point(3.0,1050));
        temp.list.add(new Stat_Point(3.0,1054));
        temp.list.add(new Stat_Point(3.0,1061));
        temp.list.add(new Stat_Point(3.0,1063));
        temp.list.add(new Stat_Point(3.0,1064));
        temp.list.add(new Stat_Point(3.0,1074));
        temp.list.add(new Stat_Point(3.0,1075));
        temp.list.add(new Stat_Point(3.0,1079));
        temp.list.add(new Stat_Point(0.0,1080));
        temp.list.add(new Stat_Point(3.0,1082));
        temp.list.add(new Stat_Point(3.0,1083));
        temp.list.add(new Stat_Point(3.0,1084));
        temp.list.add(new Stat_Point(3.0,1086));
        temp.list.add(new Stat_Point(3.0,1090));
        temp.list.add(new Stat_Point(3.0,1094));
        temp.list.add(new Stat_Point(3.0,1098));
        temp.list.add(new Stat_Point(3.0,1102));
        temp.list.add(new Stat_Point(3.0,1106));
        temp.list.add(new Stat_Point(3.0,1110));
        temp.list.add(new Stat_Point(3.0,1114));
        temp.list.add(new Stat_Point(3.0,1121));
        temp.list.add(new Stat_Point(3.0,1123));
        temp.list.add(new Stat_Point(3.0,1124));
        temp.list.add(new Stat_Point(3.0,1134));
        temp.list.add(new Stat_Point(3.0,1135));
        temp.list.add(new Stat_Point(3.0,1139));
        temp.list.add(new Stat_Point(0.0,1140));
        temp.list.add(new Stat_Point(3.0,1142));
        temp.list.add(new Stat_Point(3.0,1143));
        temp.list.add(new Stat_Point(3.0,1144));
        temp.list.add(new Stat_Point(3.0,1146));
        temp.list.add(new Stat_Point(3.0,1150));
        temp.list.add(new Stat_Point(3.0,1154));
        temp.list.add(new Stat_Point(3.0,1158));
        temp.list.add(new Stat_Point(3.0,1162));
        temp.list.add(new Stat_Point(3.0,1166));
        temp.list.add(new Stat_Point(3.0,1170));
        temp.list.add(new Stat_Point(3.0,1174));
        temp.list.add(new Stat_Point(3.0,1181));
        temp.list.add(new Stat_Point(3.0,1183));
        temp.list.add(new Stat_Point(3.0,1184));
        temp.list.add(new Stat_Point(3.0,1194));
        temp.list.add(new Stat_Point(3.0,1195));
        temp.list.add(new Stat_Point(3.0,1199));
        temp.list.add(new Stat_Point(0.0,1200));

        for(Stat_Point p : temp.list){
            temp.total += p.value;
        }
        temp.average = temp.total/temp.duration;
        map.put(6,temp);

        return map;
    }

    // Enums
    enum AverageUnit{PERSECOND, PERMINUTE, PERHOUR, PERGAME}
}
