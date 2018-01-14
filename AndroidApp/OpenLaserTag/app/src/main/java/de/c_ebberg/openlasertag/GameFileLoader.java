package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.widget.GridLayout;
import android.widget.ImageButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 20.02.17.
 * Loads and parses GameFiles
 */

public class GameFileLoader {
    private String LOG_TAG="GameFileLoader";
    private static final boolean DEBUG = true;
    private Handler serial_handler;
    private Handler game_logic_handler;
    private Context context;

    public GameFileLoader(Context _context){
        context=_context;
    }

    public GameInformation load_xml_game_file(String path) throws IOException, XmlPullParserException{
        InputStream file_stream;
        File game_file;
        if(path.startsWith("raw/")){
            try {
                Class res = R.raw.class;
                Field field = res.getField(path.substring(4));
                int rawId = field.getInt(null);
                file_stream = context.getResources().openRawResource(
                        context.getResources().getIdentifier(path.substring(4),
                                "raw", context.getPackageName()));
            } catch(Exception e){
                throw new IOException("No resource named \""+path+"\" found");
            }
        } else {
            game_file = new File(path);
            file_stream = new FileInputStream(game_file);
        }
        XMLGameFileParser XML_parser=new XMLGameFileParser();
        GameInformation gi = XML_parser.parse(file_stream);
        gi.file_name = path;
        return gi;
    }

    public Boolean write_game_info_to_file(GameInformation gi, String path, Boolean overwrite) throws IOException{
        if(!isExternalStorageWritable()) return false;
        File out_file = new File(path);
        File parent = out_file.getParentFile();
        if(!parent.exists()) return false;
        if(out_file.exists() && !overwrite) return false;

        OutputStream outstream = new FileOutputStream(path);
        OutputStreamWriter outwriter = new OutputStreamWriter(outstream);
        outwriter.write(GameInfoToString(gi));
        outwriter.close();

        return true;
    }

    private String GameInfoToString(GameInformation gi){
        String s="<OpenLaserTagGame>\n";
        s+="\t<GameName value=\""+ gi.game_name +"\"/>\n";
        s+="\t<timestamp value=\""+ gi.timestamp +"\"/>\n";
        s+="\t<SketchVersion value=\"" + gi.sketch_version +"\"/>\n";
        s+="\t<AppVersion value=\""+ gi.app_version +"\"/>\n";
        s+="\t<Description>"+  gi.description + "</Description>\n";
        if(gi.sounds.size()>0) {
            s += "\t<Sounds>\n";
            for (Sound sound : gi.sounds.values()) {
                s += "\t\t<Sound name=\"" + sound.name + "\"/>\n";
            }
            s += "\t</Sounds>\n";
        }
        s+="\t<Definitions>\n";
        for(Definition d : gi.definitions.values()){
            switch (d.type){
                case "team":
                    s+="\t\t<Team index=\"" + ((Team)d.value).index.toString()
                            + "\" name=\"" + ((Team)d.value).name
                            + "\" color=\"" + ((Team)d.value).color + "\"/>\n";
                    break;
                case "timer":
                    s+="\t\t<Timer name=\"" + ((Timer)d.value).name
                            + "\" duration=\"" + ((Timer)d.value).duration.toString()
                            + "\" ticks=\"" + ((Timer)d.value).ticks.toString() + "\"/>\n";
                    break;
                case "weapon":
                    s+="\t\t<Weapon index=\"" + ((WeaponInfo)d.value).index.toString()
                            +"\" name=\"" + ((WeaponInfo)d.value).name
                            +"\" damagesign=\"";
                    if(((WeaponInfo)d.value).damage_sign>0) s+="+\" ";
                    else s+="-\" ";
                    s+="shotfrequency=\""+((WeaponInfo)d.value).shot_frequency.toString()
                            +"\" range=\""+((WeaponInfo)d.value).range.toString()
                            +"\" allowed=\""+Boolean.valueOf(((WeaponInfo)d.value).allowed).toString()+"\"/>\n";
                    break;
                case "item":
                    s+="\t\t<Item name=\""+ ((Item)d.value).name
                            +"\" ID=\"" + ((Item)d.value).ID.toString()
                            +"\" icon=\"" + ((Item)d.value).icon_path
                            +"\" invoke_duration=\"" + ((Item)d.value).invoke_duration.toString() + "\"/>\n";
                    break;
            }
        }
        s+="\t</Definitions>\n";
        s+="\t<DamageMapping>\n";
        for(Integer i=0;i<gi.damage_mapping.size();i++){
            s+="\t\t<DamageValue index=\""+ i.toString() +"\" value=\""+ gi.damage_mapping.get(i).toString() +"\"/>\n";
        }
        s+="\t</DamageMapping>\n";
        s+="\t<DurationMapping>\n";
        for(Integer i=0;i<gi.duration_mapping.size();i++){
            s+="\t\t<DurationValue index=\""+ i.toString() +"\" value=\""+ gi.duration_mapping.get(i).toString() +"\"/>\n";
        }
        s+="\t</DurationMapping>\n";
        s+="\t<GameVariables>\n";
        for(String varname : gi.game_variables.keySet()){
            s+="\t\t<Variable name=\"" + varname
                    +"\" type=\"" + gi.game_variables.get(varname).read_type()
                    +"\" value=\"" + gi.game_variables.get(varname).read_value().toString() + "\"/>\n";
        }
        s+="\t</GameVariables>\n";
        s+="\t<SignalCode>\n";
        for(String signalname : gi.signal_code.keySet()){
            s+=signal_code_to_string(gi.signal_code.get(signalname),signalname);
        }
        s+="\t</SignalCode>\n";
        s+="\t<PlayerStats>\n";
        for(String stat : gi.player_stats.keySet()){
            s+="\t\t<Stat name=\"" + stat + "\"/>\n";
        }
        s+="\t</PlayerStats>\n";

        s+="</OpenLaserTagGame>";
        return s;
    }

    private String signal_code_to_string(SignalCode signal, String signalname){
        String s="";
        s+="\t\t<Signal name=\"" + signalname + "\" parallel=\"" + Boolean.valueOf(signal.parallel).toString() + "\"";
        if(signal._static) s+=" static=\"true\"";
        s+=">\n";

        s+= code_list_to_string(signal.commands, 3);
        s+="\t\t</Signal>\n";
        return s;
    }

    private String code_list_to_string(List<Code> codelist, Integer depth){
        String s="";
        for(Code code : codelist){
            if(code.identity.equals("IF")){
                for(Integer i=0;i<depth;i++) s+="\t";
                s+="<IF var=\"" + ((IfStatement)code).var
                        +"\" operator=\"" + ((IfStatement)code).operator
                        +"\" value=\"" + ((IfStatement) code).value +"\">\n";

                s+=code_list_to_string(((IfStatement)code).sub_code, depth+1);
                if(((IfStatement)code).else_code.size()>0){
                    for(Integer i=0;i<depth;i++) s+="\t";
                    s+="<ELSE>\n";
                    s+=code_list_to_string(((IfStatement)code).else_code,depth+1);
                }
                for(Integer i=0;i<depth;i++) s+="\t";
                s+="</IF>\n";
            } else {
                for(Integer i=0;i<depth;i++) s+="\t";
                s+="<Command name=\"" + code.identity + "\"";
                for(Integer i=0; i<((Command)code).parameter.size();i++){
                    s+=" par"+i.toString()+"=\"" + ((Command)code).parameter.get(i) + "\"";
                }
                s+="/>\n";
            }
        }
        return s;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public GameVariable new_game_variable(){
        return new GameVariable();
    }

    public Team new_team_instance(){return new Team();}

    public WeaponInfo new_weapon_instance(){return new WeaponInfo();}

    public Definition new_definition_instance(){return new Definition();}


    public static Double eval(final String str, final Map<String,GameVariable> signal_data, final Map<String,Definition> definitions, final Map<String,GameVariable> variables, final Map<String,Double> ps) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            private double mod(double x, double y)
            {
                double result = x % y;
                return result < 0? result + y : result;
            }
            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x=0;
                double y=0;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') { // functions and variables
                    while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == '.') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if(func.equals("sqrt") || func.equals("sin") || func.equals("cos") || func.equals("tan")) x = parseFactor();
                    if(func.equals("mod")){
                        eat('(');
                        x = parse_argument();
                        eat(',');
                        y = parse_argument();
                        eat(')');
                    }

                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("mod")) x = mod(x , y);
                    else if (func.toLowerCase().equals("true")) x=1;
                    else if (func.toLowerCase().equals("false")) x=0;
                    else if (signal_data.containsKey(func)) {
                        if(signal_data.get(func).type.equals("string")){
                            String s_value="";
                            for(Character c_value : ((String)signal_data.get(func).value).toCharArray()) s_value+=Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        }
                        else if(signal_data.get(func).type.equals("boolean")) x = ((Boolean) signal_data.get(func).value) ? 1.0 : 0.0;
                        else if(signal_data.get(func).type.equals("int")) x = Double.valueOf(signal_data.get(func).value.toString());
                        else x = (Double) signal_data.get(func).value;
                    }
                    else if(func.contains(".")
                            && ((definitions.containsKey("Timer_"+func.substring(0,func.indexOf('.')))
                                && (func.substring(func.indexOf('.')+1).equals("name")||func.substring(func.indexOf('.')+1).equals("duration")||func.substring(func.indexOf('.')+1).equals("ticks"))
                            ))){
                        if(func.substring(func.indexOf('.')+1).equals("name")){
                            String s_value="";
                            for(Character c_value : ((Timer)definitions.get("Timer_"+func.substring(0,func.indexOf('.'))).value).name.toCharArray()) s_value+=Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if(func.substring(func.indexOf('.')+1).equals("duration")){
                            x = ((Timer)definitions.get("Timer_"+func.substring(0,func.indexOf('.'))).value).duration;
                        } else if(func.substring(func.indexOf('.')+1).equals("ticks")){
                            x = ((Timer)definitions.get("Timer_"+func.substring(0,func.indexOf('.'))).value).ticks;
                        }
                    }
                    else if(func.contains(".") && func.contains("Weapon")
                            &&(definitions.containsKey(func.substring(0,func.indexOf('.')))
                            && (func.substring(func.indexOf('.')+1).equals("index")||func.substring(func.indexOf('.')+1).equals("damagesign")||func.substring(func.indexOf('.')+1).equals("shotfrequency")||func.substring(func.indexOf('.')+1).equals("range")))){
                        if(func.substring(func.indexOf('.')+1).equals("name")){
                            String s_value="";
                            for(Character c_value : ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).name.toCharArray()) s_value+=Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if(func.substring(func.indexOf('.')+1).equals("index")){
                            x = ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).index;
                        } else if(func.substring(func.indexOf('.')+1).equals("damagesign")){
                            x = ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).damage_sign;
                        } else if(func.substring(func.indexOf('.')+1).equals("shotfrequency")){
                            x = ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).shot_frequency;
                        } else if(func.substring(func.indexOf('.')+1).equals("range")){
                            x = ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).range;
                        }
                    }
                    else if(func.contains(".") && func.contains("Team_")
                            && definitions.containsKey(func.substring(0,func.indexOf('.')))
                            && (func.substring(func.indexOf('.')+1).equals("index")
                            || func.substring(func.indexOf('.')+1).equals("name"))
                            || func.substring(func.indexOf('.')+1).equals("color")){
                        switch(func.substring(func.indexOf('.')+1)){
                            case "index":
                                x = ((Integer)definitions.get(func.substring(0,func.indexOf('.'))).value).doubleValue();
                                break;
                            case "name":
                            case "color":
                                x = 0.0;
                                break;
                            default:
                                throw new RuntimeException(func.substring(0,func.indexOf('.'))+" contains no member named '"+func.substring(func.indexOf('.')+1));
                        }
                    }
                    else if (variables.containsKey(func)) {
                        if(variables.get(func).read_type().equals("string")){
                            String s_value="";
                            for(Character c_value : ((String)variables.get(func).read_value()).toCharArray()) s_value+=Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        }
                        else if(variables.get(func).read_type().equals("boolean")) x = ((Boolean) variables.get(func).read_value()) ? 1.0 : 0.0;
                        else if(variables.get(func).read_type().equals("int")) x = Double.valueOf(variables.get(func).read_value().toString());
                        else x = (Double) variables.get(func).read_value();
                    }
                    else if (ps.containsKey(func)) x = ps.get(func);
                    else throw new RuntimeException("Unknown function or variable: " + func);
                } else {
                    if(ch==-1) throw new RuntimeException("Unexpected end of string: '"+str+"' at pos="+((Integer) pos).toString());
                    else throw new RuntimeException("Unexpected: '" + (char)ch +"'("+ ch +") evaluating "+str+" at pos="+((Integer) pos).toString());
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }

            double parse_argument(){
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x=0;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') { // functions and variables
                    while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == '.')
                        nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (func.equals("sqrt") || func.equals("sin") || func.equals("cos") || func.equals("tan"))
                        x = parseFactor();

                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.toLowerCase().equals("true")) x = 1;
                    else if (func.toLowerCase().equals("false")) x = 0;
                    else if (signal_data.containsKey(func)) {
                        if (signal_data.get(func).type.equals("string")) {
                            String s_value = "";
                            for (Character c_value : ((String) signal_data.get(func).value).toCharArray())
                                s_value += Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if (signal_data.get(func).type.equals("boolean"))
                            x = ((Boolean) signal_data.get(func).value) ? 1.0 : 0.0;
                        else if (signal_data.get(func).type.equals("int"))
                            x = Double.valueOf(signal_data.get(func).value.toString());
                        else x = (Double) signal_data.get(func).value;
                    } else if (func.contains(".")
                            && ((definitions.containsKey("Timer_" + func.substring(0, func.indexOf('.')))
                            && (func.substring(func.indexOf('.') + 1).equals("name") || func.substring(func.indexOf('.') + 1).equals("duration") || func.substring(func.indexOf('.') + 1).equals("ticks"))
                    ))) {
                        if (func.substring(func.indexOf('.') + 1).equals("name")) {
                            String s_value = "";
                            for (Character c_value : ((Timer) definitions.get("Timer_" + func.substring(0, func.indexOf('.'))).value).name.toCharArray())
                                s_value += Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if (func.substring(func.indexOf('.') + 1).equals("duration")) {
                            x = ((Timer) definitions.get("Timer_" + func.substring(0, func.indexOf('.'))).value).duration;
                        } else if (func.substring(func.indexOf('.') + 1).equals("ticks")) {
                            x = ((Timer) definitions.get("Timer_" + func.substring(0, func.indexOf('.'))).value).ticks;
                        }
                    } else if (func.contains(".") && func.contains("Weapon")
                            && (definitions.containsKey(func.substring(0, func.indexOf('.')))
                            && (func.substring(func.indexOf('.') + 1).equals("index") || func.substring(func.indexOf('.') + 1).equals("damagesign") || func.substring(func.indexOf('.') + 1).equals("shotfrequency") || func.substring(func.indexOf('.') + 1).equals("range")))) {
                        if (func.substring(func.indexOf('.') + 1).equals("name")) {
                            String s_value = "";
                            for (Character c_value : ((WeaponInfo)definitions.get(func.substring(0,func.indexOf('.'))).value).name.toCharArray())
                                s_value += Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if (func.substring(func.indexOf('.') + 1).equals("index")) {
                            x = ((WeaponInfo) definitions.get(func.substring(0, func.indexOf('.'))).value).index;
                        } else if (func.substring(func.indexOf('.') + 1).equals("damagesign")) {
                            x = ((WeaponInfo) definitions.get(func.substring(0, func.indexOf('.'))).value).damage_sign;
                        } else if (func.substring(func.indexOf('.') + 1).equals("shotfrequency")) {
                            x = ((WeaponInfo) definitions.get(func.substring(0, func.indexOf('.'))).value).shot_frequency;
                        } else if (func.substring(func.indexOf('.') + 1).equals("range")) {
                            x = ((WeaponInfo) definitions.get(func.substring(0, func.indexOf('.'))).value).range;
                        }
                    }
                    else if(func.contains(".") && func.contains("Team_")
                            && definitions.containsKey(func.substring(0,func.indexOf('.')))
                            && (func.substring(func.indexOf('.')+1).equals("index")
                            || func.substring(func.indexOf('.')+1).equals("name"))
                            || func.substring(func.indexOf('.')+1).equals("color")){
                        switch(func.substring(func.indexOf('.')+1)){
                            case "index":
                                x = ((Integer)definitions.get(func.substring(0,func.indexOf('.'))).value).doubleValue();
                                break;
                            case "name":
                            case "color":
                                x = 0.0;
                                break;
                            default:
                                throw new RuntimeException(func.substring(0,func.indexOf('.'))+" contains no member named '"+func.substring(func.indexOf('.')+1));
                        }
                    }
                    else if (variables.containsKey(func)) {
                        if (variables.get(func).read_type().equals("string")) {
                            String s_value = "";
                            for (Character c_value : ((String) variables.get(func).read_value()).toCharArray())
                                s_value += Character.getNumericValue(c_value);
                            x = Double.parseDouble(s_value);
                        } else if (variables.get(func).read_type().equals("boolean"))
                            x = ((Boolean) variables.get(func).read_value()) ? 1.0 : 0.0;
                        else if (variables.get(func).read_type().equals("int"))
                            x = Double.valueOf(variables.get(func).read_value().toString());
                        else x = (Double) variables.get(func).read_value();
                    } else if (ps.containsKey(func)) x = ps.get(func);
                    else throw new RuntimeException("Unknown function or variable: " + func);
                } else if(ch==',' || ch==')'){
                    return x;
                } else {
                    if(ch==-1) throw new RuntimeException("Unexpected end of string: '"+str+"' at pos="+((Integer) pos).toString());
                    else throw new RuntimeException("Unexpected: '" + (char)ch +"'("+ ch +") evaluating "+str+" at pos="+((Integer) pos).toString());
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    public static String eval_string_expression(final String str, final Map<String,GameVariable> signal_data, final Map<String,Definition> definitions, final Map<String,GameVariable> variables, final Map<String,Double> ps){
        return new Object() {
            String parse(){
                /*if(!str.contains("+")) return parse_variable();
                else return parse_expression();*/
                nextChar();
                String x = parse_expression();
                if (pos < str.length()) throw new RuntimeException("eval_string_expression: Unexpected: " + (char)ch);
                return x;
            }

            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            String parse_variable(){
                if(str.charAt(0)=='\'' && str.charAt(str.length()-1)=='\'') return str.substring(1,str.length()-2);
                if(signal_data.containsKey(str)){
                    return signal_data.get(str).value.toString();
                } else if(variables.containsKey(str)){
                    return variables.get(str).read_value().toString();
                } else if(ps.containsKey(str)) {
                    return ps.get(str).toString();
                } else if(str.contains(".") && str.substring(0,str.indexOf('.')).contains("Timer_")){
                    if(!definitions.containsKey(str.substring(0,str.indexOf('.')))){
                        throw new RuntimeException("Definitions do not contain a Timer named '"+str.substring(6, str.indexOf('.'))+"'");
                    }
                    switch (str.substring(str.indexOf('.')+1)){
                        case "name":
                            return ((Timer)definitions.get(str.substring(0,str.indexOf('.'))).value).name;
                        case "duration":
                            return ((Timer)definitions.get(str.substring(0,str.indexOf('.'))).value).duration.toString();
                        case "ticks":
                            return ((Timer)definitions.get(str.substring(0,str.indexOf('.'))).value).ticks.toString();
                        default:
                            throw new RuntimeException("Timers don't have a member named '"+str.substring(str.indexOf('.'))+"'");
                    }
                } else if(str.contains(".") && str.substring(0,str.indexOf('.')).contains("Weapon")){
                    if(!definitions.containsKey(str.substring(0,str.indexOf('.')))){
                        throw new RuntimeException("Definitions do not contain a Weapon with index '"+str.substring(6, str.indexOf('.'))+"'");
                    }
                    switch (str.substring(str.indexOf('.')+1)){
                        case "index":
                            return ((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).index.toString();
                        case "name":
                            return ((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).name;
                        case "damagesign":
                            return ((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).damage_sign.toString();
                        case "shotfrequency":
                            return ((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).shot_frequency.toString();
                        case "range":
                            return ((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).range.toString();
                        case "allowed":
                            return Boolean.valueOf(((WeaponInfo)definitions.get(str.substring(0,str.indexOf('.'))).value).allowed).toString();
                        default:
                            throw new RuntimeException("Weapons don't have a member named '"+str.substring(str.indexOf('.')+1)+"'");
                    }
                } else if(str.contains(".") && str.substring(0,str.indexOf('.')).contains("Team_")){
                    if(!definitions.containsKey(str.substring(0,str.indexOf('.')))){
                        throw new RuntimeException("Definitions do not contain a Team with index '"+str.substring(5, str.indexOf('.'))+"'");
                    }
                    switch (str.substring(str.indexOf('.')+1)){
                        case "index":
                            return ((Team)definitions.get(str.substring(0,str.indexOf('.'))).value).index.toString();
                        case "name":
                            return ((Team)definitions.get(str.substring(0,str.indexOf('.'))).value).name;
                        case "color":
                            return ((Team)definitions.get(str.substring(0,str.indexOf('.'))).value).color;
                        default:
                            throw new RuntimeException("Teams don't have a member named '"+str.substring(str.indexOf('.'))+"'");
                    }
                } else{
                    if(str.contains("'")) throw new RuntimeException("Uneven distribution of '");
                    return str;
                }
            }

            String parse_expression(){
                String x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else return x;
                }
            }

            String parseTerm(){
                int startPos = this.pos;

                if (eat('\'')){ // marked string literal
                    startPos = this.pos;
                    while(ch != '\'' && ch != -1) nextChar();
                    if(ch == -1) throw new RuntimeException("Uneven distribution of '");
                    String literal = str.substring(startPos, this.pos);
                    eat('\'');
                    return literal;
                } else if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_'){
                    startPos = this.pos;
                    while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch>='0' && ch <='9') || ch == '_' || ch == '.') nextChar();
                    String var = str.substring(startPos, this.pos);
                    if(signal_data.containsKey(var)){
                        return signal_data.get(var).value.toString();
                    } else if(variables.containsKey(var)){
                        return variables.get(var).read_value().toString();
                    } else if(ps.containsKey(var)) {
                        return ps.get(var).toString();
                    } else if(var.contains(".") && var.contains("Weapon")
                            && (definitions.containsKey(var.substring(0, var.indexOf('.')))
                            && (var.substring(var.indexOf('.') + 1).equals("index") || var.substring(var.indexOf('.') + 1).equals("damagesign") || var.substring(var.indexOf('.') + 1).equals("shotfrequency") || var.substring(var.indexOf('.') + 1).equals("range")))) {
                        if (var.substring(var.indexOf('.') + 1).equals("name")) {
                            return ((WeaponInfo) definitions.get(var.substring(0, var.indexOf('.'))).value).name;
                        } else if (var.substring(var.indexOf('.') + 1).equals("index")) {
                            return ((WeaponInfo) definitions.get(var.substring(0, var.indexOf('.'))).value).index.toString();
                        } else if (var.substring(var.indexOf('.') + 1).equals("damagesign")) {
                            return ((WeaponInfo) definitions.get(var.substring(0, var.indexOf('.'))).value).damage_sign.toString();
                        } else if (var.substring(var.indexOf('.') + 1).equals("shotfrequency")) {
                            return ((WeaponInfo) definitions.get(var.substring(0, var.indexOf('.'))).value).shot_frequency.toString();
                        } else if (var.substring(var.indexOf('.') + 1).equals("range")) {
                            return ((WeaponInfo) definitions.get(var.substring(0, var.indexOf('.'))).value).range.toString();
                        } else {
                            throw new RuntimeException("Weapon Information " + var + " does not exist");
                        }
                    } else if(var.contains(".") && var.contains("Team_")
                            && (definitions.containsKey(var.substring(0,var.indexOf('.'))))
                            && (var.substring(var.indexOf('.')+1).equals("index") || var.substring(var.indexOf('.')+1).equals("name") || var.substring(var.indexOf('.')+1).equals("color"))){
                        switch (var.substring(var.indexOf('.')+1)){
                            case "index": return ((Team)definitions.get(var.substring(0, var.indexOf('.'))).value).index.toString();
                            case "name": return ((Team)definitions.get(var.substring(0, var.indexOf('.'))).value).name;
                            case "color": return ((Team)definitions.get(var.substring(0, var.indexOf('.'))).value).color;
                            default: throw new RuntimeException("Team Information "+ var + "does not exist");
                        }
                    } else{
                        return var;
                    }
                } else {
                    throw new RuntimeException("eval_string_expression: Unexpected character '"+ch+"' at position "+pos);
                }
            }
        }.parse();
    }

    public void set_serial_handler(Handler _serial_handler){
        serial_handler=_serial_handler;
    }

    public void set_game_logic_handler(Handler _handler) {
        game_logic_handler = _handler;
    }
    /*****************************************
     *   XML-Game-File-Parser
     ****************************************/
    /** Parse Tags
     * OpenLaserTagGame
     *      GameName
     *      timestamp
     *      SketchVersion
     *      AppVersion
     *      Definitions
     *          Weapon
     *      GameVariables
     *          Variable
     *      SignalCode
     *          Signal
     *              Command
     *              IF
     *                  Command
     *              ENDIF
     *      PlayerStats
     *          Stat
     *      TeamStats
     *          Stat
     */
    /* TODO: Implement game file validity checker
     */
    private class XMLGameFileParser{
        private final String ns = null;

        GameInformation parse(InputStream in) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
            parser.setInput(in,null);
            parser.nextTag();
            GameInformation gi = readFile(parser);
            in.close();
            for(String signalkey : gi.signal_code.keySet()){
                reference_signal_code(gi,gi.signal_code.get(signalkey).commands);
            }

            if(gi.loaded_members<6){
                throw new XmlPullParserException("Not all required fields where defined correctly");
            }
            gi.player_name = Singleton.getInstance().getCurrentUser();
            // if not already defined, define necessary game variables with default values
            if(!gi.game_variables.containsKey("max_LifePoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(100);
                gi.game_variables.put("max_LifePoints",v);
            }
            if(!gi.game_variables.containsKey("max_ShieldPoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(100);
                gi.game_variables.put("max_ShieldPoints",v);
            }
            if(!gi.game_variables.containsKey("max_AmmoPoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(100);
                gi.game_variables.put("max_AmmoPoints",v);
            }
            if(!gi.game_variables.containsKey("max_AmmoPacks")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(3);
                gi.game_variables.put("max_AmmoPacks",v);
            }
            if(!gi.game_variables.containsKey("max_ExtraLifes")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(3);
                gi.game_variables.put("max_ExtraLifes",v);
            }
            if(!gi.game_variables.containsKey("LifePoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value = gi.game_variables.get("max_LifePoints");
                gi.game_variables.put("LifePoints",v);
            }
            if(!gi.game_variables.containsKey("ShieldPoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value = gi.game_variables.get("max_ShieldPoints");
                gi.game_variables.put("ShieldPoints",v);
            }
            if(!gi.game_variables.containsKey("AmmoPoints")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value = gi.game_variables.get("max_AmmoPoints");
                gi.game_variables.put("AmmoPoints",v);
            }
            if(!gi.game_variables.containsKey("AmmoPacks")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value = gi.game_variables.get("max_AmmoPacks");
                gi.game_variables.put("AmmoPacks",v);
            }
            if(!gi.game_variables.containsKey("ExtraLifes")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value = gi.game_variables.get("max_ExtraLifes");
                gi.game_variables.put("ExtraLifes",v);
            }
            if(!gi.game_variables.containsKey("TeamID")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(0);
                gi.game_variables.put("TeamID",v);
            }
            for(Integer team_count=0;;team_count++){
                if(!gi.definitions.containsKey("Team_"+team_count.toString())){
                    if((Integer)gi.game_variables.get("TeamID").read_value() >= team_count){
                        gi.game_variables.get("TeamID").write_value(team_count - 1);
                    }
                    break;
                }
            }
            if(!gi.game_variables.containsKey("PlayerID")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(0);
                gi.game_variables.put("PlayerID",v);
            }
            if(!gi.game_variables.containsKey("WeaponDamage")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(1);
                gi.game_variables.put("WeaponDamage",v);
            }
            if(!gi.game_variables.containsKey("WeaponType")){
                GameVariable v = new GameVariable();
                v.type="int";
                v.value=Integer.valueOf(0);
                gi.game_variables.put("WeaponType",v);
            }
            if(!gi.definitions.containsKey("Timer_GameTimer")){
                Timer  t= new Timer();
                t.duration = 900;
                t.ticks = 1;
                t.running=false;
                t.name="GameTimer";
                Definition d = new Definition();
                d.type="Timer";
                d.value=t;
                gi.definitions.put("Timer_GameTimer",d);
            } else {
                ((Timer)gi.definitions.get("Timer_GameTimer").value).ticks=1;
            }
            if(gi.damage_mapping.size()==0){
                gi.damage_mapping.addAll(Arrays.asList(default_damage_mapping));
            }
            if(gi.duration_mapping.size()==0){
                gi.duration_mapping.addAll(Arrays.asList(default_duration_mapping));
            }

            return gi;
        }

        private void reference_signal_code(GameInformation gi, List<Code> cmd_list){
            for(Code cmd : cmd_list){
                cmd.definitions = gi.definitions;
                cmd.variables = gi.game_variables;
                cmd.playerstats = gi.player_stats;
                cmd.soundPool = gi.soundPool;
                cmd.sounds = gi.sounds;
                cmd.damage_mapping = gi.damage_mapping;
                cmd.duration_mapping = gi.duration_mapping;
                cmd.gi = gi;
                if(cmd.identity.equals("IF")){
                    reference_signal_code(gi,cmd.get_sub_code());
                }
            }
        }

        private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException();
            }
            int depth = 1;
            while (depth != 0) {
                switch (parser.next()) {
                    case XmlPullParser.END_TAG:
                        depth--;
                        break;
                    case XmlPullParser.START_TAG:
                        depth++;
                        break;
                }
            }
        }

        private GameInformation readFile(XmlPullParser parser) throws XmlPullParserException, IOException {
            GameInformation info = new GameInformation();
            info.sounds = new HashMap<String,Sound>(); // needed, because sounds section is optional, but the map has to be initiated
            info.damage_mapping = new ArrayList<Integer>();
            info.duration_mapping = new ArrayList<Integer>();

            parser.require(XmlPullParser.START_TAG,ns,"OpenLaserTagGame");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                if (name.equals("GameName")) {
                    info.game_name = read_text_value(parser, "GameName");
                    info.loaded_members++;
                } else if (name.equals("timestamp")) {
                    info.timestamp = read_text_value(parser, "timestamp");
                    info.loaded_members++;
                } else if(name.equals("Description")){
                    info.description = read_text(parser,"Description");
                    info.loaded_members++;
                } else if(name.equals("SketchVersion")){
                    info.sketch_version = read_text_value(parser,"SketchVersion");
                } else if(name.equals("AppVersion")){
                    info.app_version = read_text_value(parser,"AppVersion");
                } else if(name.equals("Definitions")) {
                    info.definitions = read_definitions(parser);
                    info.loaded_members++;
                } else if(name.equals("DamageMapping")) {
                    read_damage_map(parser, info.damage_mapping);
                } else if(name.equals("DurationMapping")){
                    read_duration_map(parser, info.duration_mapping);
                } else if(name.equals("Sounds")){
                    info.sounds = read_sounds(parser,info);
                } else if(name.equals("GameVariables")) {
                    info.game_variables = read_game_variables(parser);
                    info.loaded_members++;
                } else if(name.equals("SignalCode")) {
                    info.signal_code = read_signals(parser);
                    info.loaded_members++;
                } else if(name.equals("PlayerStats")) {
                    info.player_stats = read_player_stats(parser);
                } else{
                    skip(parser);
                }
            }
            return info;
        }

        private String read_text_value(XmlPullParser parser, String tag)  throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String value = parser.getAttributeValue(null, "value");
            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, ns, tag);
            return value;
        }

        private String read_text(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG,ns,tag);
            String text=readText(parser);
            parser.require(XmlPullParser.END_TAG,ns,tag);
            return text;
        }

        // For the tags title and summary, extracts their text values.
        private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
            String result = "";
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText();
                parser.nextTag();
            }
            return result;
        }

        private Map<String,Definition> read_definitions(XmlPullParser parser) throws XmlPullParserException, IOException{
            Map<String,Definition> def=new HashMap<String,Definition>();
            parser.require(XmlPullParser.START_TAG,ns,"Definitions");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name=parser.getName();
                if(tag_name.equals("Weapon")) {
                    read_weapon(parser, def);
                } else if(tag_name.equals("Timer")) {
                    read_timer(parser, def);
                } else if(tag_name.equals("Team")) {
                    read_team(parser, def);
                } else if(tag_name.equals("Item")) {
                    read_item(parser, def);
                } else {
                    skip(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"Definitions");
            return def;
        }

        private void read_weapon(XmlPullParser parser,Map<String,Definition> def) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG,ns,"Weapon");
            WeaponInfo weapon = new WeaponInfo();
            try {
                weapon.index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                if(weapon.index>=max_weapon_type){
                    throw new XmlPullParserException("Weapon index out of Bounds: index="+weapon.index.toString()+" max="+Integer.valueOf(max_weapon_type-1).toString());
                }
                String temp = parser.getAttributeValue(null, "damagesign");
                if (temp.equals("-")) {
                    weapon.damage_sign = -1;
                } else {
                    weapon.damage_sign = 1;
                }
                weapon.shot_frequency = Double.parseDouble(parser.getAttributeValue(null, "shotfrequency"));
                weapon.range = Integer.parseInt(parser.getAttributeValue(null, "range"));
                weapon.name = parser.getAttributeValue(null,"name");
                weapon.allowed = (parser.getAttributeValue(null,"allowed")==null)||Boolean.parseBoolean(parser.getAttributeValue(null,"allowed"));
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in definition of Weapon"+weapon.index);
            }
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Weapon");
            if(def.containsKey("Weapon"+Integer.toString(weapon.index))){
                throw new XmlPullParserException("Double Declaration of Weapon"+Integer.toString(weapon.index));
            }
            Definition d = new Definition();
            d.type = "weapon";
            d.value = weapon;
            def.put("Weapon"+Integer.toString(weapon.index),d);
        }

        private void read_timer(XmlPullParser parser, Map<String,Definition> def) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG, ns,"Timer");
            Timer t=new Timer();
            try {
                t.name = parser.getAttributeValue(null, "name");
                t.duration = Integer.parseInt(parser.getAttributeValue(null, "duration"));
                t.ticks = Integer.parseInt(parser.getAttributeValue(null, "ticks"));
                t.running=false;
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in definition of Timer "+t.name);
            }

            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Timer");
            if(def.containsKey("Timer_"+t.name)){
                throw new XmlPullParserException("Double Declaration of Timer "+t.name);
            }
            Definition d = new Definition();
            d.type="timer";
            d.value=t;
            def.put("Timer_"+t.name,d);
        }

        private void read_team(XmlPullParser parser, Map<String,Definition> def) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG,ns,"Team");
            Team t = new Team();
            try{
                t.name = parser.getAttributeValue(null,"name");
                t.index = Integer.parseInt(parser.getAttributeValue(null,"index"));
                t.color = parser.getAttributeValue(null,"color");
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in definition of Team "+t.name);
            }

            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Team");
            if(def.containsKey("Team_"+t.index.toString())){
                throw new XmlPullParserException("Double Declaration of Team with index "+t.index.toString());
            }
            Definition d = new Definition();
            d.type="team";
            d.value = t;
            def.put("Team_"+t.index.toString(),d);
        }

        private void read_item(XmlPullParser parser, Map<String,Definition> def) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG,ns,"Item");
            Item i = new Item();
            try{
                i.name = parser.getAttributeValue(null,"name");
                i.ID = Integer.parseInt(parser.getAttributeValue(null,"ID"));
                i.icon_path = parser.getAttributeValue(null,"icon");
                if(parser.getAttributeValue(null,"invoke_duration")!=null){
                    i.invoke_duration = Integer.parseInt(parser.getAttributeValue(null,"invoke_duration"));
                } else {
                    i.invoke_duration = 2;
                }
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in definition of Item "+i.name);
            }

            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Item");
            if(def.containsKey("Item_"+i.ID.toString())){
                throw new XmlPullParserException("Double Declaration of Item with ID "+i.ID.toString());
            }
            Definition d = new Definition();
            d.type="item";
            d.value = i;
            def.put("Item_"+i.ID.toString(),d);
        }

        private void read_damage_map(XmlPullParser parser,ArrayList<Integer> damage_map) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG,ns,"DamageMapping");
            HashMap<Integer,Integer> temp_map = new HashMap<>();
            while(parser.next() != XmlPullParser.END_TAG){
                if(parser.getEventType() != XmlPullParser.START_TAG){
                    continue;
                }
                String tag_name = parser.getName();
                if(tag_name.equals("DamageValue")){
                    read_damage_mapping(parser, temp_map);
                } else {
                    skip(parser);
                }
            }
            for(Integer i=0;i<mapping_array_size;i++){
                if(temp_map.containsKey(i)){
                    damage_map.add(temp_map.get(i));
                } else {
                    damage_map.add(default_damage_mapping[i]);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"DamageMapping");
        }

        private void read_damage_mapping(XmlPullParser parser, HashMap<Integer,Integer> map) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG,ns,"DamageValue");
            try{
                int index = Integer.parseInt(parser.getAttributeValue(null,"index"));
                int value = Integer.parseInt(parser.getAttributeValue(null,"value"));
                if(map.containsKey(index)){
                    throw new XmlPullParserException("Double declaration of DamageValue with index "+index);
                }
                map.put(index,value);
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in DamageMapping");
            }
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"DamageValue");
        }

        private void read_duration_map(XmlPullParser parser,ArrayList<Integer> duration_map) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG,ns,"DurationMapping");
            HashMap<Integer,Integer> temp_map = new HashMap<>();
            while(parser.next() != XmlPullParser.END_TAG){
                if(parser.getEventType() != XmlPullParser.START_TAG){
                    continue;
                }
                String tag_name = parser.getName();
                if(tag_name.equals("DurationValue")){
                    read_duration_mapping(parser, temp_map);
                } else {
                    skip(parser);
                }
            }
            for(Integer i=0;i<mapping_array_size;i++){
                if(temp_map.containsKey(i)){
                    duration_map.add(temp_map.get(i));
                } else {
                    duration_map.add(default_duration_mapping[i]);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"DurationMapping");
        }

        private void read_duration_mapping(XmlPullParser parser, HashMap<Integer,Integer> map) throws XmlPullParserException, IOException{
            parser.require(XmlPullParser.START_TAG,ns,"DurationValue");
            try{
                int index = Integer.parseInt(parser.getAttributeValue(null,"index"));
                int value = Integer.parseInt(parser.getAttributeValue(null,"value"));
                if(map.containsKey(index)){
                    throw new XmlPullParserException("Double declaration of DurationValue with index "+index);
                }
                map.put(index,value);
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in DurationMapping");
            }
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"DurationValue");
        }

        private Map<String,Sound> read_sounds(XmlPullParser parser,GameInformation info) throws XmlPullParserException, IOException{
            Map<String,Sound> sounds=new HashMap<String,Sound>();
            parser.require(XmlPullParser.START_TAG,ns,"Sounds");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name=parser.getName();
                if(tag_name.equals("Sound")) {
                    read_sound(parser, sounds, info);
                } else {
                    skip(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"Sounds");
            return sounds;
        }

        private void read_sound(XmlPullParser parser, Map<String,Sound> sounds, GameInformation info) throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG,ns,"Sound");
            Sound s = new Sound();
            s.name = parser.getAttributeValue(null,"name");

            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Sound");
            if(sounds.containsKey(s.name)){
                throw new XmlPullParserException("Double Declaration of Sound with name "+s.name);
            }
            try {
                Class res = R.raw.class;
                Field field = res.getField(s.name);
                int rawId = field.getInt(null);     // if a raw resource with this name doesn't exist, an exception is thrown to search for user file

                s.sound_id = info.soundPool.load(context,rawId,1);
                s.resource = true;
            } catch(Exception e){
                // if code reaches here, the requested sound is not a resource, but a userfile
                File sound_folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"OpenLaserTag"+File.separator+"Sounds");
                if(!sound_folder.exists()) throw new XmlPullParserException("Could not find sound folder in app folder");
                ArrayList<String> user_sounds=new ArrayList<String>(Arrays.asList(sound_folder.list()));
                if(!user_sounds.contains(s.name)) throw new XmlPullParserException("Could not find sound \""+s.name+"\" in resources or sounds folder");
                s.sound_id = info.soundPool.load(sound_folder.getAbsolutePath()+File.separator+s.name,1);
                s.resource = false;
            }
            sounds.put(s.name,s);
        }

        private Map<String,GameVariable> read_game_variables(XmlPullParser parser) throws XmlPullParserException, IOException {
            Map<String,GameVariable> var=new HashMap<String,GameVariable>();
            parser.require(XmlPullParser.START_TAG,ns,"GameVariables");
            while(parser.next() != XmlPullParser.END_TAG) {
                if(parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name = parser.getName();
                if(tag_name.equals("Variable")){
                    read_variable(parser,var);
                } else {
                    skip(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"GameVariables");
            return var;
        }

        private void read_variable(XmlPullParser parser, Map<String,GameVariable> var) throws  XmlPullParserException,IOException {
            parser.require(XmlPullParser.START_TAG,ns,"Variable");
            String name=parser.getAttributeValue(null,"name");
            String type=parser.getAttributeValue(null,"type");
            GameVariable variable=new GameVariable();
            try {
                if (type.equals("int")) {
                    variable.value = Integer.valueOf(parser.getAttributeValue(null, "value"));
                    variable.type = "int";
                } else if (type.equals("double")) {
                    variable.value = Double.valueOf(parser.getAttributeValue(null, "value"));
                    variable.type = "double";
                } else if (type.equals("boolean")) {
                    variable.value = Boolean.valueOf(parser.getAttributeValue(null, "value"));
                    variable.type = "boolean";
                } else { // String interpretation
                    variable.value = parser.getAttributeValue(null, "value");
                    variable.type = "string";
                }
            } catch(NumberFormatException e){
                throw new XmlPullParserException("Wrong number format in definition of Variable "+name);
            }
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Variable");

            if(var.containsKey(name)){
                throw new XmlPullParserException("Double Declaration of GameVariable "+name);
            }
            var.put(name,variable);
        }

        private Map<String,SignalCode> read_signals(XmlPullParser parser) throws XmlPullParserException,IOException {
            parser.require(XmlPullParser.START_TAG,ns,"SignalCode");
            Map<String,SignalCode> signals=new HashMap<String,SignalCode>();
            while(parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name=parser.getName();
                if(tag_name.equals("Signal")) {
                    SignalCode signal=new SignalCode();
                    String name = parser.getAttributeValue(null,"name");
                    signal.parallel = false;
                    signal.parallel = Boolean.parseBoolean(parser.getAttributeValue(null,"parallel"));
                    if(parser.getAttributeValue(null,"static")!=null){
                        signal._static = Boolean.parseBoolean(parser.getAttributeValue(null,"static"));
                    }
                    signal.commands = read_signal_code(parser);
                    if(signals.containsKey(name)){
                        throw new XmlPullParserException("Double Declaration of Signal "+name);
                    }
                    signals.put(name,signal);
                } else {
                    skip(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"SignalCode");
            return signals;
        }

        private List<Code> read_signal_code(XmlPullParser parser) throws XmlPullParserException,IOException {
            List<Code> commands= new ArrayList<>();
            while(parser.next()!= XmlPullParser.END_TAG) {
                if(parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name=parser.getName();
                if(tag_name.equals("Command")) {
                    commands.add(read_command(parser));
                } else if(tag_name.equals("IF")) {
                    commands.add(read_if_statement(parser));
                } else {
                    skip(parser);
                }
            }
            return commands;
        }

        private Command read_command(XmlPullParser parser) throws  XmlPullParserException, IOException {
            Command cmd=new Command();
            parser.require(XmlPullParser.START_TAG,ns,"Command");
            cmd.identity = parser.getAttributeValue(null,"name");
            cmd.parameter = new ArrayList<>();
            for(int i=1;i<parser.getAttributeCount();i++){
                cmd.parameter.add(parser.getAttributeValue(i));
            }
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Command");
            return cmd;
        }

        private IfStatement read_if_statement(XmlPullParser parser) throws XmlPullParserException,IOException {
            IfStatement statement = new IfStatement();
            parser.require(XmlPullParser.START_TAG,ns,"IF");
            statement.identity = "IF";
            statement.var = parser.getAttributeValue(null,"var");
            statement.operator = parser.getAttributeValue(null,"operator");
            statement.value = parser.getAttributeValue(null,"value");

            statement.sub_code = read_signal_code(parser);

            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("ELSE")){
                statement.else_code = read_signal_code(parser);
                parser.require(XmlPullParser.END_TAG,ns,"ELSE");
            } else {
                statement.else_code = new ArrayList<>();
            }

            parser.require(XmlPullParser.END_TAG,ns,"IF");
            return statement;
        }

        private Map<String,Double> read_player_stats(XmlPullParser parser) throws XmlPullParserException,IOException {
            Map<String,Double> stats = new HashMap<String,Double>();
            parser.require(XmlPullParser.START_TAG,ns,"PlayerStats");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag_name=parser.getName();
                if(tag_name.equals("Stat")){
                    read_stat(parser, stats);
                } else {
                    skip(parser);
                }
            }
            parser.require(XmlPullParser.END_TAG,ns,"PlayerStats");
            return stats;
        }

        private void read_stat(XmlPullParser parser, Map<String,Double> stats) throws XmlPullParserException,IOException {
            parser.require(XmlPullParser.START_TAG,ns,"Stat");
            String name=parser.getAttributeValue(null,"name");
            double value=0;
            parser.next();
            parser.require(XmlPullParser.END_TAG,ns,"Stat");
            if(stats.containsKey(name)){
                throw new XmlPullParserException("Double Declaration of Stat "+name);
            }
            stats.put(name,value);
        }
    }

    public class GameInformation {
        int loaded_members=0; // after load this should be 6 or the file was erroneous
                            // game_name,description,timestamp,definitions,game_variables and signal_code are counting for this
        Boolean game_running=false;
        Integer game_time=0;

        String player_name="";
        Boolean invincible=false;
        Boolean tagger_enabled = false;

        String game_name;
        String sketch_version;
        String app_version;
        String timestamp;
        String description;
        Map<String,Definition> definitions;
        Map<String,Sound> sounds;
        Map<String,GameVariable> game_variables;
        Map<String,SignalCode> signal_code;
        Map<String,Double> player_stats;
        ArrayList<Integer> damage_mapping;
        ArrayList<Integer> duration_mapping;
        Integer flag_team_ID=-1;

        SoundPool soundPool;
        String file_name;


        public String toString(){
            String s="";
            s+="Name: " + game_name + "\n";
            s+="SketchVersion: " + sketch_version + "\n";
            s+="AppVersion: " + app_version + "\n";
            s+="timestamp" + timestamp + "\n";
            s+="Description: " + description +"\n";
            s+="Definitions:\n";
            Iterator it = definitions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                s+=pair.getKey() + ": " + pair.getValue().toString() + "\n";
            }
            s+="Sounds:\n";
            it = sounds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                s+=pair.getKey() + ": " + pair.getValue().toString() + "\n";
            }
            s+="GameVariables:\n";
            it = game_variables.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                s+=pair.getKey() + ": " + pair.getValue().toString() + "\n";
            }
            s+="SignalCode:\n";
            it = signal_code.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                s+=pair.getKey() + ": " + pair.getValue().toString() + "\n";
            }
            s+="PlayerStats:\n";
            it = player_stats.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                s+=pair.getKey() + ": " + pair.getValue().toString() + "\n";
            }
            return s;
        }

        public GameInformation(){
            soundPool = new SoundPool(20, AudioManager.STREAM_MUSIC,0);
        }
    }

    public class Definition{
        String type;
        Object value;

        public String toString(){
            return value.toString();
        }
    }

    public class WeaponInfo {
        Integer index;
        String name;
        Integer damage_sign;
        Double shot_frequency;
        Integer range;
        boolean allowed;

        public String toString(){
            return "Index=" + Integer.toString(index) + " DamageSign=" + Integer.toString(damage_sign)
                    + " ShotFrequency=" + Double.toString(shot_frequency) + " Range="
                    + Integer.toString(range) + "\n";
        }
    }

    public class Timer{
        String name;
        Integer duration;
        Integer ticks;
        boolean running=false;
        CountDownTimer timer;

        public String toString(){
            return " "+name+"("+duration.toString()+","+
                    ticks.toString()+")";
        }
    }

    public class Team{
        String name;
        Integer index;
        String color;

        public String toString(){
            return " "+name+"["+index.toString()+"] -->"+color;
        }
    }

    public class Item{
        String name;
        String icon_path;
        Integer ID;
        Integer invoke_duration;

        public String toString() { return " "+name+", ID="+ID.toString()+", icon="+icon_path;}
    }

    public class Sound{
        String name;
        Boolean resource;
        String filename;
        int sound_id;
        int stream_id=0;

        public String toString(){
            return resource?" resource: "+name+"\n":" file: "+filename;
        }
    }

    public class GameVariable {
        String type;
        Object value;
        boolean access_active=false;

        String read_type(){
            while(access_active);
            return type;
        }
        Object read_value(){
            while(access_active);
            return value;
        }
        void write_type(String _type){
            while(access_active);
            access_active = true;
            type = _type;
            access_active = false;
        }
        void write_value(Object _value){
            while(access_active);
            access_active = true;
            value = _value;
            access_active = false;
        }
        public String toString(){
            return "Type=" + type + " Value=" + value.toString() + "\n";
        }
    }

    public class SignalCode{
        boolean parallel;
        boolean _static=false;
        boolean running=false;
        List<Code> commands;

        public String toString(){
            String s="parallel="+Boolean.toString(parallel) + "\nCode:\n";
            for(Code element : commands){
                s+=element.toString();
            }
            return s;
        }
    }

    public class Code{
        String identity;
        Map<String,Definition> definitions;
        Map<String,Sound> sounds;
        Map<String,GameVariable> variables;
        Map<String,Double> playerstats;
        SoundPool soundPool;
        ArrayList<Integer> damage_mapping;
        ArrayList<Integer> duration_mapping;
        GameInformation gi;

        public boolean execute(Map<String,GameVariable> signal_data){return true;}

        public List<Code> get_sub_code(){
            return new ArrayList<Code>();
        }
    }

    public class Command extends Code{
        List<String> parameter;

        public String toString(){
            String s="Cmd=" + identity;
            for(int i=0;i< parameter.size();i++){
                s+=" par" + Integer.toString(i) + "=" + parameter.get(i);
            }
            s+="\n";
            return s;
        }

        public boolean execute(Map<String,GameVariable> signal_data){
            try {
                Message msg;
                Bundle msg_data;
                CommandType cmd = CommandType.valueOf(identity);
                switch (cmd) {
                    case DELAY:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DELAY");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DELAY'");
                            break;
                        }
                        try {
                            Thread.sleep(eval(parameter.get(0), signal_data, definitions, variables, playerstats).longValue());
                        } catch(InterruptedException e){
                            Log.e(LOG_TAG,"Interrupted Exception during DELAY execution");
                        }
                        break;
                    case DECREASELIFEPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASELIFEPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASELIFEPOINTS'");
                            break;
                        }
                        if(gi.invincible) break;
                        Integer decrease_amount = eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        variables.get("LifePoints").write_value( ((Integer) variables.get("LifePoints").read_value())
                                - decrease_amount);
                        if((Integer) variables.get("LifePoints").read_value() < 0) variables.get("LifePoints").write_value(0);
                        if((Integer)variables.get("LifePoints").read_value() == 0) {
                            msg = new Message();
                            msg.what=16; // queue player dead signal
                            Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        }
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "l"+variables.get("LifePoints").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        StatsManager.getInstance().AddPointToCurrentStat("Damage",
                                decrease_amount.doubleValue(),
                                gi.game_time);
                        break;}
                    case INCREASELIFEPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASELIFEPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASELIFEPOINTS'");
                            break;
                        }
                        variables.get("LifePoints").write_value(((Integer) variables.get("LifePoints").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("LifePoints").read_value() > (Integer) variables.get("max_LifePoints").read_value())
                            variables.get("LifePoints").write_value( (Integer) variables.get("max_LifePoints").read_value());
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "l"+variables.get(parameter.get(0)).read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case DECREASESHIELDPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASESHIELDPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASESHIELDPOINTS'");
                            break;
                        }
                        if(gi.invincible) break;
                        Integer decrease_amount = eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        variables.get("ShieldPoints").write_value(((Integer) variables.get("ShieldPoints").read_value())
                                - decrease_amount);
                        if((Integer) variables.get("ShieldPoints").read_value() < 0) variables.get("ShieldPoints").write_value( 0);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "s"+variables.get("ShieldPoints").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        StatsManager.getInstance().AddPointToCurrentStat("Damage",
                                decrease_amount.doubleValue(),
                                gi.game_time);
                        break;}
                    case INCREASESHIELDPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASESHIELDPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASESHIELDPOINTS'");
                            break;
                        }
                        variables.get("ShieldPoints").write_value( ((Integer) variables.get("ShieldPoints").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("ShieldPoints").read_value() > (Integer) variables.get("max_ShieldPoints").read_value())
                            variables.get("ShieldPoints").write_value( (Integer) variables.get("max_ShieldPoints").read_value());
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "s"+variables.get("ShieldPoints").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case DECREASEAMMOPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASEAMMOPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASEAMMOPOINTS'");
                            break;
                        }
                        Integer decrease_amount = eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        variables.get("AmmoPoints").write_value( ((Integer) variables.get("AmmoPoints").read_value())
                                - decrease_amount);
                        if((Integer) variables.get("AmmoPoints").read_value() < 0) variables.get("AmmoPoints").write_value(0);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "a"+variables.get("AmmoPoints").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        StatsManager.getInstance().AddPointToCurrentStat("FiredShots",
                                decrease_amount.doubleValue(),
                                gi.game_time);
                        break;}
                    case INCREASEAMMOPOINTS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASEAMMOPOINTS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASEAMMOPOINTS'");
                            break;
                        }
                        variables.get("AmmoPoints").write_value(((Integer) variables.get("AmmoPoints").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("AmmoPoints").read_value() > (Integer) variables.get("max_AmmoPoints").read_value())
                            variables.get("AmmoPoints").write_value((Integer) variables.get("max_AmmoPoints").read_value());
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "a"+variables.get("AmmoPoints").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case CHANGECOLOR:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: CHANGECOLOR");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'CHANGECOLOR'");
                            break;
                        }
                        String color = eval_string_expression(parameter.get(0),signal_data,definitions,variables,playerstats);
                        String[] color_res = context.getResources().getStringArray(R.array.tagger_color_names);
                        boolean flag=false;
                        for(String c : color_res){
                            if(c.equals(color)){
                                flag=true;
                                break;
                            }
                        }
                        if(!flag){
                            Log.e(LOG_TAG,"CHANGECOLOR: no valid color in parameter '"+color+"'");
                            break;
                        }
                        msg = new Message();
                        msg.what=2;
                        msg_data = new Bundle();
                        String color_str="f"+color+"\n";
                        msg_data.putByteArray("WriteData",color_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case CHANGECOLORFROMTEAM:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: CHANGECOLORFROMTEAM");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'CHANGECOLORFROMTEAM'");
                            break;
                        }
                        Integer team_id=eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        if(!definitions.containsKey("Team_"+team_id.toString())){
                            Log.e(LOG_TAG,"During CHANGECOLORFROMTEAM: Team with index "+team_id.toString()+" doesn't exist");
                            break;
                        }
                        String color = ((Team)definitions.get("Team_"+team_id.toString()).value).color;
                        msg = new Message();
                        msg.what=2;
                        msg_data = new Bundle();
                        String color_str="f"+color+"\n";
                        msg_data.putByteArray("WriteData",color_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case DECREASEWEAPONDAMAGE:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASEWEAPONDAMAGE");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASEWEAPONDAMAGE'");
                            break;
                        }
                        Integer new_value = ((Integer) variables.get("WeaponDamage").read_value())
                                - eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        Integer index=0;
                        for(int i=0;i<mapping_array_size-1;i++){
                            if(new_value>=damage_mapping.get(i) && new_value<=damage_mapping.get(i+1)){
                                if(new_value>=damage_mapping.get(i) && new_value<damage_mapping.get(i)+0.5*(damage_mapping.get(i+1)-damage_mapping.get(i))){
                                    new_value = damage_mapping.get(i);
                                    index=i;
                                    break;
                                } else {
                                    new_value = damage_mapping.get(i+1);
                                    index=i+1;
                                    break;
                                }
                            }
                        }
                        if(new_value>damage_mapping.get(mapping_array_size-1)){
                            new_value=damage_mapping.get(mapping_array_size-1);
                            index = mapping_array_size-1;
                        } else if(new_value<damage_mapping.get(0)){
                            new_value = damage_mapping.get(0);
                            index = 0;
                        }
                        variables.get("WeaponDamage").write_value(new_value);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "wd"+index.toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case INCREASEWEAPONDAMAGE:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASEWEAPONDAMAGE");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASEWEAPONDAMAGE'");
                            break;
                        }
                        Integer new_value  = ((Integer) variables.get("WeaponDamage").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        Integer index=0;
                        for(int i=0;i<mapping_array_size-1;i++){
                            if(new_value>=damage_mapping.get(i) && new_value<=damage_mapping.get(i+1)){
                                if(new_value>=damage_mapping.get(i) && new_value<damage_mapping.get(i)+0.5*(damage_mapping.get(i+1)-damage_mapping.get(i))){
                                    new_value = damage_mapping.get(i);
                                    index=i;
                                    break;
                                } else {
                                    new_value = damage_mapping.get(i+1);
                                    index=i+1;
                                    break;
                                }
                            }
                        }
                        if(new_value>damage_mapping.get(mapping_array_size-1)){
                            new_value=damage_mapping.get(mapping_array_size-1);
                            index = mapping_array_size-1;
                        } else if(new_value<damage_mapping.get(0)){
                            new_value = damage_mapping.get(0);
                            index = 0;
                        }
                        variables.get("WeaponDamage").write_value(new_value);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "wd"+index.toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case DECREASEWEAPONDAMAGEBYMAPPINGINDEX:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: DECREASEWEAPONDAMAGEBYMAPPINGINDEX");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASEWEAPONDAMAGEBYMAPPINGINDEX'");
                            break;
                        }
                        Integer new_index = damage_mapping.indexOf(((Integer) variables.get("WeaponDamage").read_value()))
                                - eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        if(new_index<0) new_index=0;
                        variables.get("WeaponDamage").write_value(damage_mapping.get(new_index));
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "wd"+new_index.toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case INCREASEWEAPONDAMAGEBYMAPPINGINDEX: {
                        if(DEBUG) Log.i(LOG_TAG,"Executing: INCREASEWEAPONDAMAGEBYMAPPINGINDEX");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASEWEAPONDAMAGEBYMAPPINGINDEX'");
                            break;
                        }
                        Integer new_index = damage_mapping.indexOf(((Integer) variables.get("WeaponDamage").read_value()))
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        if(new_index>mapping_array_size-1) new_index=mapping_array_size-1;
                        variables.get("WeaponDamage").write_value(damage_mapping.get(new_index));
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "wd"+new_index.toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case CHANGETEAM:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: CHANGETEAM");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'CHANGETEAM'");
                            break;
                        }
                        Integer new_team = Integer.parseInt(parameter.get(0));
                        if(new_team >= max_team_number || new_team < 0){
                            Log.e(LOG_TAG,"Team index out of bounds in command CHANGETEAM");
                            break;
                        }
                        msg = new Message();
                        msg.what=2;
                        msg_data = new Bundle();
                        String change_team_str = "gt"+new_team.toString()+"\n";
                        msg_data.putByteArray("WriteData",change_team_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        variables.get("TeamID").write_value(new_team);
                        break;
                    case DISABLETAGGER:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DISABLETAGGER");
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String disable_tagger_str = "gd\n";
                        msg_data.putByteArray("WriteData",disable_tagger_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        gi.tagger_enabled = false;
                        break;
                    case ENABLETAGGER:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: ENABLETAGGER");
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String enable_tagger_str = "ge\n";
                        msg_data.putByteArray("WriteData",enable_tagger_str.getBytes());
                       msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        gi.tagger_enabled = true;
                        break;
                    case ENABLELASERGUNSIGHT:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: ENABLELASERGUNSIGHT");
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String enable_laser_str = "gle\n";
                        msg_data.putByteArray("WriteData",enable_laser_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case DISABLELASERGUNSIGHT:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DISABLELASERGUNSIGHT");
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String disable_laser_str = "gld\n";
                        msg_data.putByteArray("WriteData",disable_laser_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case SETMUZZLEFIRE:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: SETMUSSLEFIRE");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'SETMUSSLEFIRE'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String mussle_str = "gm";
                        if(parameter.get(0).toLowerCase().equals("true")) mussle_str+="1";
                        else                                              mussle_str+="0";
                        mussle_str+="\n";
                        msg_data.putByteArray("WriteData",mussle_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case DECREASEEXTRALIFES:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASEEXTRALIFES");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASEEXTRALIFES'");
                            break;
                        }
                        if(gi.invincible) break;
                        variables.get("ExtraLifes").write_value(((Integer) variables.get("ExtraLifes").read_value())
                                - eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("ExtraLifes").read_value() < 0) variables.get("ExtraLifes").write_value(0);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "e"+variables.get("ExtraLifes").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case INCREASEEXTRALIFES:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASEEXTRALIFES");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASEEXTRALIFES'");
                            break;
                        }
                        variables.get("ExtraLifes").write_value(((Integer) variables.get("ExtraLifes").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("ExtraLifes").read_value() > (Integer) variables.get("max_ExtraLifes").read_value())
                            variables.get("ExtraLifes").write_value((Integer) variables.get("max_ExtraLifes").read_value());
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "e"+variables.get("ExtraLifes").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case DECREASEAMMOPACKS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: DECREASEAMMOPACKS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'DECREASEAMMOPACKS'");
                            break;
                        }
                        variables.get("AmmoPacks").write_value(((Integer) variables.get("AmmoPacks").read_value())
                                - eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("AmmoPacks").read_value() < 0) variables.get("AmmoPacks").write_value(0);
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "p"+variables.get("AmmoPacks").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case INCREASEAMMOPACKS:{
                        if(DEBUG) Log.i(LOG_TAG, "Executing: INCREASEAMMOPACKS");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'INCREASEAMMOPACKS'");
                            break;
                        }
                        variables.get("AmmoPacks").write_value(((Integer) variables.get("AmmoPacks").read_value())
                                + eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        if((Integer) variables.get("AmmoPacks").read_value() > (Integer) variables.get("max_AmmoPacks").read_value())
                            variables.get("AmmoPacks").write_value((Integer) variables.get("max_AmmoPacks").read_value());
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String p_string = "p"+variables.get("AmmoPacks").read_value().toString()+"\n";
                        msg_data.putByteArray("WriteData",p_string.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;}
                    case SETAMMOINDICATOR:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: SETAMMOINDICATOR");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'SETAMMOINDICATOR'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String ammo_indi_str = "ga";
                        if(parameter.get(0).toLowerCase().equals("true")) ammo_indi_str+="1";
                        else                                              ammo_indi_str+="0";
                        ammo_indi_str+="\n";
                        msg_data.putByteArray("WriteData",ammo_indi_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case SETVARIABLE:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: SETVARIABLE");
                        if(parameter.size()<2){
                            Log.e(LOG_TAG,"Too few parameter in Command 'SETVARIABLE'");
                            break;
                        }
                        // Check for monitored predefined variables for stats
                        Integer old_value = 0;
                        switch(parameter.get(0)){
                            case "LifePoints":
                            case "ShieldPoints":
                            case "AmmoPoints":
                                old_value = (Integer)variables.get(parameter.get(0)).read_value();
                                break;
                        }
                        // Set corresponding variable
                        if(signal_data.containsKey(parameter.get(0))){
                            if(signal_data.get(parameter.get(0)).type.equals("string")){
                                signal_data.get(parameter.get(0)).value = eval_string_expression(parameter.get(1),signal_data,definitions,variables,playerstats);
                            } else if(signal_data.get(parameter.get(0)).type.equals("boolean")){
                                try{
                                    signal_data.get(parameter.get(0)).value = !(eval(parameter.get(1),signal_data,definitions,variables,playerstats)== 0);
                                } catch(RuntimeException e){ // is string-->=0=false
                                    signal_data.get(parameter.get(0)).value = false;
                                }
                            } else if(signal_data.get(parameter.get(0)).type.equals("int")){
                                try{
                                    signal_data.get(parameter.get(0)).value = eval(parameter.get(1),signal_data,definitions,variables,playerstats).intValue();
                                } catch(RuntimeException e){ // is string-->=0
                                    signal_data.get(parameter.get(0)).value = 0;
                                }
                            } else if(signal_data.get(parameter.get(0)).type.equals("double")){
                                try{
                                    signal_data.get(parameter.get(0)).value = eval(parameter.get(1),signal_data,definitions,variables,playerstats);
                                } catch(RuntimeException e){ // is string-->=0=false
                                    signal_data.get(parameter.get(0)).value = 0.0;
                                }
                            }
                        } else if(variables.containsKey(parameter.get(0))){
                            if(variables.get(parameter.get(0)).type.equals("string")){
                                variables.get(parameter.get(0)).write_value(eval_string_expression(parameter.get(1),signal_data,definitions,variables,playerstats));
                            } else if(variables.get(parameter.get(0)).type.equals("boolean")){
                                try{
                                    variables.get(parameter.get(0)).write_value(!(eval(parameter.get(1),signal_data,definitions,variables,playerstats)== 0));
                                } catch(RuntimeException e){ // is string-->=0=false
                                    variables.get(parameter.get(0)).write_value(false);
                                }
                            } else if(variables.get(parameter.get(0)).type.equals("int")){
                                try{
                                    Integer temp = eval(parameter.get(1),signal_data,definitions,variables,playerstats).intValue();
                                    if(gi.invincible && (parameter.get(0).equals("LifePoints") || parameter.get(0).equals("ShieldPoints") || parameter.get(0).equals("ExtraLifes")) && (Integer)variables.get(parameter.get(0)).read_value() > temp) break;
                                    variables.get(parameter.get(0)).write_value(temp);
                                } catch(RuntimeException e){ // is string-->=0
                                    variables.get(parameter.get(0)).write_value(0);
                                }
                            } else if(variables.get(parameter.get(0)).type.equals("double")){
                                try{
                                    variables.get(parameter.get(0)).write_value(eval(parameter.get(1),signal_data,definitions,variables,playerstats));
                                } catch(RuntimeException e){ // is string-->=0=false
                                    variables.get(parameter.get(0)).write_value(0.0);
                                }
                            }

                            switch(parameter.get(0)){
                                case "LifePoints":
                                case "ShieldPoints":
                                case "AmmoPoints":
                                    //old_value = (Integer)variables.get(parameter.get(0)).read_value();
                                    if((Integer)variables.get(parameter.get(0)).read_value() < old_value){
                                        switch(parameter.get(0)){
                                            case "LifePoints":
                                            case "ShieldPoints":
                                                StatsManager.getInstance().AddPointToCurrentStat("Damage",
                                                        ((Integer)variables.get(parameter.get(0)).read_value()).doubleValue()-old_value.doubleValue(),
                                                        gi.game_time);
                                                break;
                                            case "AmmoPoints":
                                                StatsManager.getInstance().AddPointToCurrentStat("FiredShots",
                                                        ((Integer)variables.get(parameter.get(0)).read_value()).doubleValue()-old_value.doubleValue(),
                                                        gi.game_time);
                                                break;
                                        }
                                    }
                                    break;
                            }
                            //enforce special variable bounds and send values to tagger if special variable is set
                            if(parameter.get(0).equals("LifePoints")){
                                Integer new_life = (Integer)variables.get("LifePoints").read_value();
                                if(new_life < 0 || new_life > (Integer)variables.get("max_LifePoints").read_value()){
                                    Log.e(LOG_TAG,"Setting LifePoints out of bound. Setting to 0");
                                    variables.get("LifePoints").write_value(0);
                                    new_life = 0;
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "l"+new_life.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                                if((Integer)variables.get("LifePoints").read_value() == 0){
                                    msg = new Message();
                                    msg.what = 16; // queue Player dead signal
                                    Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                                }
                            } else if(parameter.get(0).equals("ShieldPoints")){
                                Integer new_shield = (Integer)variables.get("ShieldPoints").read_value();
                                if(new_shield < 0 || new_shield > (Integer)variables.get("max_ShieldPoints").read_value()){
                                    Log.e(LOG_TAG,"Setting ShieldPoints out of bounds. Setting to 0");
                                    new_shield = 0;
                                    variables.get("ShieldPoints").write_value(0);
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "s"+new_shield.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("AmmoPoints")){
                                Integer new_ammo = (Integer)variables.get("AmmoPoints").read_value();
                                if(new_ammo<0 || new_ammo > (Integer)variables.get("max_AmmoPoints").read_value()){
                                    Log.e(LOG_TAG,"Setting AmmoPoints out of bounds. Setting to 0");
                                    new_ammo = 0;
                                    variables.get("AmmoPoints").write_value(new_ammo);
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "a"+new_ammo.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("AmmoPacks")){
                                Integer new_ammo_packs = (Integer)variables.get("AmmoPacks").read_value();
                                if(new_ammo_packs < 0 || new_ammo_packs>(Integer)variables.get("max_AmmoPacks").read_value()){
                                    Log.e(LOG_TAG,"Setting AmmoPacks out of bounds. Setting to 0");
                                    new_ammo_packs = 0;
                                    variables.get("AmmoPacks").write_value(new_ammo_packs);
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "p"+new_ammo_packs.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("ExtraLifes")){
                                Integer new_extra_lifes = (Integer)variables.get("ExtraLifes").read_value();
                                if(new_extra_lifes<0 || new_extra_lifes > (Integer)variables.get("max_ExtraLifes").read_value()){
                                    Log.e(LOG_TAG,"Setting ExtraLifes out of bounds. Setting to 0");
                                    new_extra_lifes=0;
                                    variables.get("ExtraLifes").write_value(new_extra_lifes);
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "e"+new_extra_lifes.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("TeamID")){
                                Integer var_new_team = (Integer) variables.get(parameter.get(0)).read_value();
                                if(var_new_team<0 || var_new_team>= max_team_number){
                                    Log.e(LOG_TAG,"TeamID out of bounds during variable setting. Setting to 0");
                                    variables.get(parameter.get(0)).write_value(0);
                                    var_new_team = 0;
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "gt"+var_new_team.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("PlayerID")){
                                Integer new_ID = (Integer)variables.get("PlayerID").read_value();
                                if(new_ID<0 || new_ID>=max_player_ID){
                                    Log.e(LOG_TAG,"PlayerID out of bounds. Settings to 0");
                                    new_ID = 0;
                                    variables.get("PlayerID").write_value(0);
                                }
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "gp"+new_ID.toString()+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("WeaponDamage")){
                                Integer new_value  = ((Integer) variables.get("WeaponDamage").read_value());
                                Integer index=0;
                                for(int i=0;i<mapping_array_size-1;i++){
                                    if(new_value>=damage_mapping.get(i) && new_value<=damage_mapping.get(i+1)){
                                        if(new_value>=damage_mapping.get(i) && new_value<damage_mapping.get(i)+0.5*(damage_mapping.get(i+1)-damage_mapping.get(i))){
                                            new_value = damage_mapping.get(i);
                                            index=i;
                                            break;
                                        } else {
                                            new_value = damage_mapping.get(i+1);
                                            index=i+1;
                                            break;
                                        }
                                    }
                                }
                                if(new_value>damage_mapping.get(mapping_array_size-1)){
                                    new_value=damage_mapping.get(mapping_array_size-1);
                                    index = mapping_array_size-1;
                                } else if(new_value<damage_mapping.get(0)){
                                    new_value = damage_mapping.get(0);
                                    index = 0;
                                }
                                variables.get("WeaponDamage").write_value(new_value);
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "wd"+index+"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            } else if(parameter.get(0).equals("WeaponType")){
                                Integer new_type = (Integer)variables.get("WeaponType").read_value();
                                Double new_freq = 1.0;
                                if(new_type<0 || new_type>=max_weapon_type){
                                    Log.e(LOG_TAG,"WeaponType out of bounds. Setting to 0");
                                    new_type = 0;
                                    new_freq = ((WeaponInfo)definitions.get("Weapon0").value).shot_frequency;
                                } else if(!((WeaponInfo)definitions.get("Weapon"+new_type.toString()).value).allowed) {
                                    Log.e(LOG_TAG,"Set weapon is not allowed");
                                    new_type=-1;
                                    for(Integer i=0;i<4;i++){
                                        if(definitions.containsKey("Weapon"+i.toString())){
                                            if(((WeaponInfo)definitions.get("Weapon"+i.toString()).value).allowed){
                                                new_type=i;
                                            }
                                        }
                                    }
                                    if(new_type.equals(-1)){
                                        Log.e(LOG_TAG,"No allowed weapon. Allowing first weapon");
                                        for(Integer i=0;i<4;i++){
                                            if(definitions.containsKey("Weapon"+i.toString())){
                                                new_type = i;
                                                new_freq = ((WeaponInfo)definitions.get("Weapon"+new_type.toString()).value).shot_frequency;
                                                break;
                                            }
                                        }
                                    } else {
                                        new_freq = ((WeaponInfo)definitions.get("Weapon"+new_type.toString()).value).shot_frequency;
                                    }
                                    Message w_msg = new Message();
                                    w_msg.what = 0;
                                    w_msg.obj = gi;
                                    Singleton.getInstance().getGameActivityHandler().sendMessage(w_msg);
                                } else {
                                    new_freq = ((WeaponInfo)definitions.get("Weapon"+new_type.toString()).value).shot_frequency;
                                }
                                ((GameVariable)variables.get("WeaponType")).write_value(new_type);
                                msg = new Message();
                                msg.what = 2;
                                msg_data = new Bundle();
                                String p_string = "wt"
                                        +new_type.toString()
                                        +"|"
                                        +new_freq.toString()
                                        +"\n";
                                msg_data.putByteArray("WriteData",p_string.getBytes());
                                msg.setData(msg_data);
                                serial_handler.sendMessage(msg);
                            }
                        } else if(playerstats.containsKey(parameter.get(0))){
                            try{
                                Double val = eval(parameter.get(1),signal_data,definitions,variables,playerstats);
                                Double diff = val - playerstats.get(parameter.get(0));
                                StatsManager.getInstance().AddPointToCurrentStat(parameter.get(0),diff,gi.game_time);
                                playerstats.put(parameter.get(0), val);
                            } catch(RuntimeException e){ // string=0
                                playerstats.put(parameter.get(0),0.0);
                            }
                        } else {
                            Log.e(LOG_TAG,"First parameter in Command SETVARIABLE is no known variable!");
                            break;
                        }
                        break;
                    case RETURN:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: RETURN");
                        return false;
                    case BLINKLEDS:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: BLINKLEDS");
                        if(parameter.size()<2){
                            Log.e(LOG_TAG,"Too few parameter in Command 'BLINKLEDS'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String blink_str = "gb"+parameter.get(0)+"|"+parameter.get(1)+"\n";
                        msg_data.putByteArray("WriteData",blink_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case BLINKLEDSUNTILSTOPPED:
                        if(DEBUG) Log.i(LOG_TAG,"Executing: BLINKLEDSUNTILSTOPPED");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command'BLINKLEDSUNTILSTOPPED'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String blink_until_str = "gu"+parameter.get(0)+"\n";
                        msg_data.putByteArray("WriteData",blink_until_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case STOPBLINKLEDS:
                        if(DEBUG) Log.i(LOG_TAG,"Executing: STOPBLINKLEDS");
                        msg = new Message();
                        msg.what = 2;
                        msg_data = new Bundle();
                        String stop_blink_str = "gv\n";
                        msg_data.putByteArray("WriteData",stop_blink_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                        break;
                    case PLAYSOUND:
                        if(DEBUG) Log.i(LOG_TAG, "Executing: PLAYSOUND");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'PLAYSOUND'");
                            break;
                        }
                        if(!sounds.containsKey(parameter.get(0))){
                            Log.e(LOG_TAG,"Sound '"+parameter.get(0)+"' was not defined in game file");
                            break;
                        }
                        if(!Singleton.getInstance().getSoundOn()) break;
                        sounds.get(parameter.get(0)).stream_id = soundPool.play(sounds.get(parameter.get(0)).sound_id,1,1,1,0,1);
                        if(sounds.get(parameter.get(0)).stream_id==0){
                            Log.e(LOG_TAG,"Some unidentified error during soundPool.play(). stream_id==0. Check documentation of Android SoundPool for further information");
                            break;
                        }
                        break;
                    case STOPSOUND:
                        if(DEBUG) Log.i(LOG_TAG,"Executing: STOPSOUND");
                        if(parameter.size()<1) {
                            Log.e(LOG_TAG,"Too few parameter in Command 'STOPSOUND'");
                            break;
                        }
                        if(!sounds.containsKey(parameter.get(0))){
                            Log.e(LOG_TAG,"Sound'"+parameter.get(0)+"' was not defined in game file");
                            break;
                        }
                        soundPool.stop(sounds.get(parameter.get(0)).stream_id);
                        break;
                    case STARTTIMER:
                        if(DEBUG) Log.i(LOG_TAG,"Executing: STARTTIMER");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'STARTTIMER'");
                            break;
                        }
                        if(!definitions.containsKey("Timer_"+parameter.get(0))){
                            Log.e(LOG_TAG,"Command STARTTIMER: Timer "+parameter.get(0)+" not defined");
                            break;
                        }
                        if(((Timer)definitions.get("Timer_"+parameter.get(0)).value).running){
                            Log.e(LOG_TAG,"Command STARTTIMER: Timer "+parameter.get(0) + " is already running");
                            break;
                        }
                        ((Timer )definitions.get("Timer_"+parameter.get(0)).value).timer
                                = new CountDownTimer(((Timer)definitions.get("Timer_"+parameter.get(0)).value).duration*1000,
                                (((Timer)definitions.get("Timer_"+parameter.get(0)).value).ticks==0)?
                                        ((Timer)definitions.get("Timer_"+parameter.get(0)).value).duration*1000:
                                        ((Timer)definitions.get("Timer_"+parameter.get(0)).value).ticks*1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                Message msg = new Message();
                                msg.what = 7;
                                Bundle data= new Bundle();
                                data.putString("type","tick");
                                data.putString("name",((Timer)definitions.get("Timer_"+parameter.get(0)).value).name);
                                data.putLong("millisleft",millisUntilFinished);
                                msg.setData(data);
                                game_logic_handler.sendMessage(msg);
                            }

                            @Override
                            public void onFinish() {
                                Message msg = new Message();
                                msg.what = 7;
                                Bundle data= new Bundle();
                                data.putString("type","finish");
                                data.putString("name",((Timer)definitions.get("Timer_"+parameter.get(0)).value).name);
                                msg.setData(data);
                                game_logic_handler.sendMessage(msg);
                            }
                        }.start();
                        ((Timer)definitions.get("Timer_"+parameter.get(0)).value).running = true;
                        break;
                    case ENDTIMER:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: ENDTIMER");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'ENDTIMER'");
                            break;
                        }
                        if(!definitions.containsKey("Timer_"+parameter.get(0))){
                            Log.e(LOG_TAG,"Command ENDTIMER: Timer "+parameter.get(0)+" not defined");
                            break;
                        }
                        if(!((Timer)definitions.get("Timer_"+parameter.get(0)).value).running){
                            Log.e(LOG_TAG,"Command ENDTIMER: Timer "+parameter.get(0)+" is not running");
                            break;
                        }
                        ((Timer)definitions.get("Timer_"+parameter.get(0)).value).timer.cancel();
                        ((Timer)definitions.get("Timer_"+parameter.get(0)).value).running = false;
                        break;}
                    case PICKUPFLAG:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: PICKUPFLAG");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'PICKUPFLAG'");
                            break;
                        }
                        Integer team_id = eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue();
                        if(team_id<0 || team_id>max_team_number){
                            Log.e(LOG_TAG,"During PICKUPFLAG: TeamID in parameter is invalid!");
                            break;
                        }
                        msg = new Message();
                        msg.what=17;
                        Bundle dat = new Bundle();
                        dat.putInt("TeamID",team_id);
                        msg.setData(dat);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        break;}
                    case LOSEFLAG:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: LOSEFLAG");
                        msg = new Message();
                        msg.what = 8;
                        game_logic_handler.sendMessage(msg);
                        break;}
                    case GETITEM:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: GETITEM");
                        if(parameter.size()<2){
                            Log.e(LOG_TAG,"Too few parameter in Command 'GETITEM'");
                            break;
                        }
                        Message i_msg = new Message();
                        i_msg.what = 11; // queue get item signal
                        Bundle i_dat = new Bundle();
                        i_dat.putInt("ItemID",eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        i_dat.putInt("ItemData",eval(parameter.get(1),signal_data,definitions,variables,playerstats).intValue());
                        i_msg.setData(i_dat);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(i_msg);
                        break;}
                    case INVOKEITEM:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: INVOKEITEM");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'INVOKEITEM'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 10;
                        Bundle data = new Bundle();
                        data.putInt("ItemID",eval(parameter.get(0),signal_data,definitions,variables,playerstats).intValue());
                        data.putInt("ItemData",eval(parameter.get(1),signal_data,definitions,variables,playerstats).intValue());
                        data.putBoolean("CodeInvoke",true);
                        msg.setData(data);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        break;}
                    case QUEUESIGNAL: {
                        if(DEBUG) Log.i(LOG_TAG,"Executing: QUEUESIGNAL");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'QUEUESIGNAL'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 12;
                        Bundle data = new Bundle();
                        data.putString("Signal",parameter.get(0));
                        msg.setData(data);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        break;}
                    case SETWEAPONALLOWED: {
                        if(DEBUG) Log.i(LOG_TAG,"Executing: SETWEAPONALLOWED");
                        if(parameter.size()<2){
                            Log.e(LOG_TAG,"Too few parameter in Command 'SETWEAPONALLOWED'");
                            break;
                        }
                        if(!definitions.containsKey("Weapon"+parameter.get(0))){
                            Log.e(LOG_TAG,"No weapon with index="+parameter.get(0)+" found");
                            break;
                        }
                        ((WeaponInfo) definitions.get("Weapon" + parameter.get(0)).value).allowed = Boolean.parseBoolean(parameter.get(1));
                        msg = new Message();
                        msg.what=0;
                        msg.obj = gi;
                        Singleton.getInstance().getGameActivityHandler().sendMessage(msg);
                        break;}
                    case SENDMESSAGE:{
                        if(DEBUG) Log.i(LOG_TAG,"Executing: SENDMESSAGE");
                        if(parameter.size()<1){
                            Log.e(LOG_TAG,"Too few parameter in Command 'SENDMESSAGE'");
                            break;
                        }
                        msg = new Message();
                        msg.what = 5;
                        msg.obj = gi;
                        Bundle data = new Bundle();
                        data.putString("MessageText",parameter.get(0));
                        if(parameter.size()==3){ // show_duration and type are specified
                            data.putInt("ShowDuration",Integer.parseInt(parameter.get(1)));
                            data.putString("MessageType",parameter.get(2));
                        } else {
                            data.putInt("ShowDuration",message_default_duration);
                            data.putString("MessageType",message_default_type);
                        }
                        msg.setData(data);
                        Singleton.getInstance().getGameActivityHandler().sendMessage(msg);
                        break;}
                }
            } catch(IllegalArgumentException e) {
                Log.e(LOG_TAG, "Executing Signal Code: Invalid command \"" + identity +"\"");
            }
            return true;
        }

        public List<Code> get_sub_code(){
            return new ArrayList<Code>();
        }
    }

    public class IfStatement extends Code{
        String var;
        String operator;
        String value;
        List<Code> sub_code;
        List<Code> else_code;

        public String toString(){
            String s="Cmd="+identity + " var=" + var + " operator=" + operator + " value=" + value
                    + "\nSubcode:\n";
            for(Code element : sub_code){
                s+=element.toString();
            }
            if(else_code.size()>0){
                s+="ELSE\n";
                for(Code element : else_code){
                    s+=element.toString();
                }
            }
            s+="ENDIF\n";
            return s;
        }

        public boolean execute(Map<String,GameVariable> signal_data){
            if(DEBUG) Log.i(LOG_TAG,"Executing IF var=" + var +"  operator=" +operator +"  value="+value);


            if(operator.equals("<")){
                if(!(eval(var,signal_data,definitions,variables,playerstats)<eval(value,signal_data,definitions,variables,playerstats))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else if(operator.equals(">")){
                if(!(eval(var,signal_data,definitions,variables,playerstats)>eval(value,signal_data,definitions,variables,playerstats))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else if(operator.equals("=") || operator.equals("==")){
                if(!(eval(var,signal_data,definitions,variables,playerstats).equals(eval(value,signal_data,definitions,variables,playerstats)))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else if(operator.equals("!=")) {
                if(eval(var,signal_data,definitions,variables,playerstats).equals(eval(value,signal_data,definitions,variables,playerstats))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else if(operator.equals("<=")) {
                if(!(eval(var,signal_data,definitions,variables,playerstats)<=eval(value,signal_data,definitions,variables,playerstats))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else if(operator.equals(">=")) {
                if(!(eval(var,signal_data,definitions,variables,playerstats)>=eval(value,signal_data,definitions,variables,playerstats))) {
                    for(Code cmd : else_code){
                        if(!cmd.execute(signal_data)) return false;
                    }
                    return true;
                }
            } else{
                Log.e(LOG_TAG,"Unknown operator during execution of IF statement: " + operator);
                return false;
            }

            if(DEBUG) Log.i(LOG_TAG,"Executing Subcode");
            for(Code cmd : sub_code){
                if(!cmd.execute(signal_data)){
                    return false;
                }
            }
            return true;
        }

        public List<Code> get_sub_code(){
            return sub_code;
        }
    }


    private enum CommandType{DELAY, DECREASELIFEPOINTS, INCREASELIFEPOINTS, DECREASESHIELDPOINTS,
        INCREASESHIELDPOINTS, DECREASEAMMOPOINTS, INCREASEAMMOPOINTS, CHANGECOLOR, CHANGECOLORFROMTEAM,
        DECREASEWEAPONDAMAGE, INCREASEWEAPONDAMAGE, INCREASEWEAPONDAMAGEBYMAPPINGINDEX,
        DECREASEWEAPONDAMAGEBYMAPPINGINDEX, CHANGETEAM, DISABLETAGGER,
        ENABLETAGGER, ENABLELASERGUNSIGHT, DISABLELASERGUNSIGHT, SETMUZZLEFIRE,
        DECREASEEXTRALIFES, INCREASEEXTRALIFES, DECREASEAMMOPACKS, INCREASEAMMOPACKS,
        SETAMMOINDICATOR, SETVARIABLE, RETURN, BLINKLEDS, BLINKLEDSUNTILSTOPPED, STOPBLINKLEDS,
        PLAYSOUND, STOPSOUND, STARTTIMER, ENDTIMER, PICKUPFLAG, LOSEFLAG, GETITEM, INVOKEITEM,
        QUEUESIGNAL, SETWEAPONALLOWED, SENDMESSAGE}

    private static final Integer[] default_damage_mapping={0,1,5,7,10,15,20,25,30,40,50,60,70,80,90,100};
    private static final Integer[] default_duration_mapping={5,10,15,20,25,30,40,50,60,70,80,90,120,180,240,300};
    public static final int mapping_array_size=16; // this defines the number of possible values. Only change, when changing th number of bits for damage and item data in the arduino sketch
    public static final int max_team_number=4; // only change, when changing number of bits for team_ID in arduino sketch
    public static final int max_weapon_type=4;
    public static final int max_player_ID=32;
    public static final int message_default_duration=5;
    public static final String message_default_type="normal";

    public static Integer get_default_damage(Integer index){
        return default_damage_mapping[index];
    }

    public static int get_default_damage_array_size(){
        return default_damage_mapping.length;
    }

    public static Integer get_default_duration(Integer index){
        return default_damage_mapping[index];
    }

    public static int get_default_duration_array_size(){
        return default_duration_mapping.length;
    }
}
