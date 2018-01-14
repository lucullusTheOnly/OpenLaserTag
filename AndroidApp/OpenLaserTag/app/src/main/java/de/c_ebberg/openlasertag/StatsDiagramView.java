package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * TODO: document your custom view class.
 */
public class StatsDiagramView extends View {
    private static final String LOG_TAG = "StatDiagramView";
    int paddingLeft = 0;
    int paddingTop = 0;
    int paddingRight = 0;
    int paddingBottom = 0;
    int contentWidth=1;
    int contentHeight=1;
    float diagram_width;
    float diagram_height;
    float coord_left;
    float coord_bottom;
    float coord_height;
    float coord_width;
    Path coord_y_path;
    Path coord_x_path;

    AxisManager axisManager;

    float pointer_length = 20;
    float line_stroke_width = 5f;

    Paint testpaint;
    int testcolor=Color.BLACK;
    Paint text_paint;
    int textcolor=Color.BLACK;
    float text_heigth;
    Paint value_paint;
    int valuecolor=Color.BLUE;
    Paint game_marker_paint;
    int gamemarkercolor=Color.RED;
    float game_marker_text_heigth;

    ArrayList<Stat> current_stat = null;
    ArrayList<Double> values = null;
    ArrayList<GameMarker> game_marker = null;
    Double max_value=0.0;
    Integer max_time=0;
    Integer min_duration=0;
    Integer binning_length=5;
    Integer zoom_window_low=0;
    Integer zoom_window_high=1;
    float zoom=1f;
    float coord_y_factor;
    float coord_x_factor;

    Double overall_total;
    Double overall_average;
    Double view_total;
    Double view_average;

    boolean touched = false;
    float touched_x=0, touched_y=0;
    Integer ontouch_zoom_window_low =0, ontouch_zoom_window_high=0;

    private static final Integer[] ticks_number={4,8,15};
    private static final Integer[] ticks_values={1, 2, 5};

    OnStatsDiagramViewListener mListener=null;
    public interface OnStatsDiagramViewListener {
        void onDataViewPositionChange(Double view_total, Double view_average);
    }

    public class GameMarker{
        Integer time;
        Integer game_number;
        public GameMarker(Integer _time, Integer _game_number){time=_time;game_number=_game_number;}
    }

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
        private Integer game_number;

        public Stat(){list = new ArrayList<>();}

        public Double getTotal(){return total;}
        public Double getAverage(){return average;}
        public Integer getTotalGames(){return total_games;}
        public ArrayList<Stat_Point> getPoints(){return new ArrayList<>(list);}
        public Integer getDuration(){return duration;}
    }

    protected class AxisManager{
        AxisManager(TextPaint _axis_textpaint){axis_textpaint = _axis_textpaint;}
        class TimeTick{
            Integer time;
            String text;
            boolean big_tick;
            TimeTick(Integer _time, boolean big){time=_time; text = formatTimeForTick(time);big_tick = big;}
        }
        class ValueTick{
            Double value;
            String text;
            boolean big_tick;
            ValueTick(Double _value, int precision, boolean big){value=_value; text = DoubleToString(value,precision);big_tick = big;}
        }

        ArrayList<TimeTick> x_ticks = new ArrayList<>();
        Integer min_time=0;
        Integer max_time=0;
        Double min_value=0.0;
        Double max_value=0.0;
        Double value_ticks=1.0;
        Double value_subticks=4.0;
        Integer time_ticks=1;
        Integer time_subticks=1;
        TextPaint axis_textpaint;

        ArrayList<ValueTick> y_ticks = new ArrayList<>();

        public void ConfigAxisTicks(Integer _min_time, Integer _max_time, Double _min_value, Double _max_value, float coord_width, float coord_height){
            if(_min_time>_max_time || _min_value > _max_value) throw new IllegalArgumentException();
            x_ticks = new ArrayList<>();
            y_ticks = new ArrayList<>();
            min_time = _min_time; max_time = _max_time; min_value = _min_value; max_value = _max_value;

            // fill ticks lists
            //y-Axis
            Double y_exp = getExponent(max_value);
            Double y_big_value = y_exp;
            boolean flag = false;
            while(y_big_value > min_value){
                if(flag) y_ticks.add(0, new ValueTick(y_big_value, 1, true));
                Double y_small_value = round(y_big_value - y_exp/value_subticks);
                while(y_small_value > y_big_value - y_exp && y_small_value > min_value){
                    y_ticks.add(0, new ValueTick(y_small_value, 3, false));
                    y_small_value = round(y_small_value - y_exp/value_subticks);
                }
                y_big_value = round(y_big_value - y_exp/value_ticks);
                flag = true;
            }
            y_big_value = y_exp;
            while(y_big_value < max_value){
                y_ticks.add(new ValueTick(y_big_value, 1, true));
                Double y_small_value = round(y_big_value + y_exp/value_subticks);
                while(y_small_value < y_big_value + y_exp && y_small_value < max_value){
                    y_ticks.add(new ValueTick(y_small_value, 3, false));
                    y_small_value = round(y_small_value + y_exp/value_subticks);
                }
                y_big_value = round(y_big_value + y_exp/value_ticks);
            }

            //x-Axis
            Integer x_exp = getTimeUnitBase(max_time);
            if (x_exp == 1) x_exp = 10;
            Integer x_big_value = x_exp;
            flag = false;
            while (x_big_value > min_time) {
                if (flag) x_ticks.add(0, new TimeTick(x_big_value, true));
                if (getSmallerTimeUnitBase(x_exp) != -1) {
                    Integer x_small_exp = getSmallerTimeUnitBase(x_exp);
                    Integer x_small_value = x_big_value - x_small_exp / time_subticks;
                    while (x_small_value > x_big_value - x_exp && x_small_value > min_time) {
                        x_ticks.add(0, new TimeTick(x_small_value, false));
                        x_small_value -= x_small_exp / time_subticks;
                    }
                }
                x_big_value -= x_exp / time_ticks;
                flag = true;
            }
            x_big_value = x_exp;
            while (x_big_value < max_time) {
                x_ticks.add(new TimeTick(x_big_value, true));
                if (getSmallerTimeUnitBase(x_exp) != -1) {
                    Integer x_small_exp = getSmallerTimeUnitBase(x_exp);
                    Integer x_small_value = x_big_value + x_small_exp / time_subticks;
                    while (x_small_value < x_big_value + x_exp && x_small_value < max_time) {
                        x_ticks.add(new TimeTick(x_small_value, false));
                        x_small_value += x_small_exp / time_subticks;
                    }
                }
                x_big_value += x_exp / time_ticks;
            }
        }

        protected String formatTimeForTick(Integer time){
            if(time % (60*60*24) == 0) return Integer.valueOf(time/(60*60*24)).toString()+"d";
            else if(time / (60*60*24) > 0) return Integer.valueOf((time-(time/(60*60*24))*60*60*24)/(60*60)).toString()+"h";

            if(time % (60*60)    == 0) return Integer.valueOf(time/(60*60)).toString()+"h";
            else if(time / (60*60)    > 0) return Integer.valueOf((time-(time/(60*60))*60*60)/60).toString()+"m";

            if(time % 60         == 0) return Integer.valueOf(time/60).toString()+"m";
            else if(time / 60         > 0) return Integer.valueOf(time-(time/60)*60).toString()+"s";

            return time.toString()+"s";
        }
        protected Double getExponent(Double value){
            Double exponent = 1.0;
            if(exponent < value){
                while(exponent <= value) exponent*=10;
                exponent/=10;
            } else {
                while(exponent > value) exponent/=10;
            }
            return exponent;
        }
        protected Integer getTimeUnitBase(Integer value){
            if(value/(60*60*24) >= 1) return 60*60*24;
            if(value/(60*60)    >= 1) return 60*60;
            if(value/60         >= 1) return 60;
            return 1;
        }
        protected Integer getSmallerTimeUnitBase(Integer base){
            switch(base){
                case 60: return 20;
                case 60*60: return 60*20;
                case 60*60*24: return 60*60*4;
                default:
                case 1: return -1;
            }
        }
        protected String DoubleToString(Double value, int precision){
            if(precision<0) return "";
            String s = value.toString();
            if(precision==0) return s.substring(0,s.indexOf("."));
            Integer pos;
            Integer prec=0;
            boolean counting=false;
            for(pos=0;pos <s.length();pos++){
                if(s.charAt(pos) >= '1' && s.charAt(pos) <='9') counting=true;
                if(counting && s.charAt(pos)!='.') prec++;
                if(prec>=precision) break;
            }
            if(pos < s.indexOf(".")) return s.substring(0,s.indexOf("."));
            if(pos+1<s.length()) return s.substring(0,pos+1);
            else return s;
        }
        protected Double round(Double value){
            String s = value.toString();
            Integer nines=0;
            for(Integer i=0;i<s.length();i++){
                if(s.charAt(i)=='9') nines++;
            }
            if(nines>= 10) return value+1e-13;
            else return value;
        }
    }

    private class Point{
        float x=0;
        float y=0;

        Point(){}
        Point(float _x, float _y){x = _x; y = _y;}

        Point substract(Point p2){return new Point(x-p2.x, y-p2.y);}
        Point add(Point p2){return new Point(x+p2.x, y+p2.y);}
        Point multiply(float a){return new Point(x*a, y*a);}

        private Point rotatePointAroundPoint(double alpha, Point c){
            Point p = new Point();
            p.x = Double.valueOf(c.x + (x - c.x)*Math.cos(alpha) - (y - c.y)*Math.sin(alpha)).floatValue();
            p.y = Double.valueOf(c.y + (x - c.x)*Math.sin(alpha) + (y - c.y)*Math.cos(alpha)).floatValue();
            return p;
        }

        float length(){return Double.valueOf(Math.sqrt(Math.pow(x,2)+Math.pow(y,2))).floatValue();}
    }

    public StatsDiagramView(Context context) {
        super(context);
        init(null, 0);
    }

    public StatsDiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public StatsDiagramView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.StatsDiagramView, defStyle, 0);

        testcolor = a.getColor(R.styleable.StatsDiagramView_StatsDiagramCoordinateColor,testcolor);
        valuecolor = a.getColor(R.styleable.StatsDiagramView_StatsDiagramValueColor,testcolor);
        gamemarkercolor = a.getColor(R.styleable.StatsDiagramView_StatsDiagramGameMarkerColor,gamemarkercolor);
        textcolor = a.getColor(R.styleable.StatsDiagramView_StatsDiagramTextColor,textcolor);

        a.recycle();

        testpaint = new Paint();
        testpaint.setColor(testcolor);
        testpaint.setStrokeWidth(line_stroke_width);
        testpaint.setStrokeJoin(Paint.Join.ROUND);

        value_paint = new Paint();
        value_paint.setColor(valuecolor);
        value_paint.setStrokeWidth(1);

        game_marker_paint = new TextPaint();
        game_marker_paint.setColor(gamemarkercolor);
        game_marker_paint.setStrokeWidth(2);
        game_marker_paint.setTextSize(18);
        game_marker_text_heigth = game_marker_paint.getFontMetrics().bottom + game_marker_paint.getFontMetrics().ascent;

        text_paint = new TextPaint();
        text_paint.setTextSize(23);
        text_paint.setColor(textcolor);

        Paint.FontMetrics fontMetrics = text_paint.getFontMetrics();
        text_heigth = fontMetrics.bottom + fontMetrics.ascent;

        coord_x_path = new Path();
        coord_x_path.moveTo(0.1f*contentWidth, 0.9f*contentHeight);
        coord_x_path.lineTo(0.97f*contentWidth, 0.9f*contentHeight);

        axisManager = new AxisManager((TextPaint)text_paint);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw data
        if(values!=null){
            Integer offset=0;
            for(Integer i=0;i<values.size();i++){
                if(i*binning_length < zoom_window_low || i*binning_length+1 > zoom_window_high) {offset++;continue;}
                canvas.drawRect(coord_left + (i-offset)*binning_length.floatValue() / coord_x_factor,
                        coord_bottom - values.get(i).floatValue() / coord_y_factor,
                        coord_left + (i-offset+1)*binning_length.floatValue() / coord_x_factor,
                        coord_bottom, value_paint);
                canvas.drawRect(coord_left + (i-offset)*binning_length.floatValue() / coord_x_factor + line_stroke_width/2,
                        coord_bottom - values.get(i).floatValue() / coord_y_factor + line_stroke_width/2,
                        coord_left + (i-offset+1)*binning_length.floatValue() / coord_x_factor - line_stroke_width/2,
                        coord_bottom - line_stroke_width/2, value_paint);
            }

        }

        // Draw coordinates
        drawPointer(canvas, coord_left - line_stroke_width/2f, coord_bottom + line_stroke_width/2f, coord_left - line_stroke_width/2f + diagram_width, coord_bottom + line_stroke_width/2f,testpaint);
        drawPointer(canvas, coord_left - line_stroke_width/2f, coord_bottom + line_stroke_width/2f, coord_left - line_stroke_width/2f, coord_bottom + line_stroke_width/2f - diagram_height,testpaint);
        canvas.drawTextOnPath("time",coord_x_path,coord_width-text_paint.measureText("time"),-5*text_heigth,text_paint);

        // Draw ticks
        for(AxisManager.TimeTick tick : axisManager.x_ticks){
            if(tick.time<zoom_window_low) continue;
            if(tick.big_tick){
                canvas.drawLine(coord_left + (tick.time.floatValue()-zoom_window_low) / coord_x_factor, coord_bottom - 2f*line_stroke_width,
                        coord_left + (tick.time.floatValue()-zoom_window_low) / coord_x_factor, coord_bottom + 3f*line_stroke_width, testpaint);
                canvas.drawText(tick.text,
                        coord_left + (tick.time.floatValue()-zoom_window_low) / coord_x_factor - text_paint.measureText(tick.text)/2f,
                        coord_bottom + 3f*line_stroke_width - 2*text_heigth,
                        text_paint);
            } else{
                canvas.drawLine(coord_left + (tick.time.floatValue()-zoom_window_low) / coord_x_factor, coord_bottom - 1f*line_stroke_width,
                        coord_left + (tick.time.floatValue()-zoom_window_low) / coord_x_factor, coord_bottom + 2f*line_stroke_width, testpaint);
                /*canvas.drawText(tick.text,
                        coord_left + tick.time.floatValue() / coord_x_factor - text_paint.measureText(tick.text)/2f,
                        coord_bottom + 2f*line_stroke_width - 2*text_heigth,
                        text_paint);*/
            }
        }
        for(AxisManager.ValueTick tick : axisManager.y_ticks){
            if(tick.big_tick){
                canvas.drawLine(coord_left - 3f*line_stroke_width, coord_bottom - tick.value.floatValue() / coord_y_factor,
                        coord_left + 2f*line_stroke_width, coord_bottom - tick.value.floatValue() / coord_y_factor, testpaint);
                canvas.drawText(tick.text,
                        coord_left - 3f*line_stroke_width - text_paint.measureText(tick.text),
                        coord_bottom - tick.value.floatValue() / coord_y_factor - text_heigth/2f,
                        text_paint);
            } else {
                canvas.drawLine(coord_left - 2f*line_stroke_width, coord_bottom - tick.value.floatValue() / coord_y_factor,
                        coord_left + 1f*line_stroke_width, coord_bottom - tick.value.floatValue() / coord_y_factor, testpaint);
                /*canvas.drawText(tick.text,
                        coord_left - 2f*line_stroke_width - text_paint.measureText(tick.text),
                        coord_bottom - tick.value.floatValue() / coord_y_factor - text_heigth/2f,
                        text_paint);*/
            }
        }
        /*canvas.drawLine(coord_left - 3f*line_stroke_width, coord_bottom - max_value.floatValue() / coord_y_factor,
                coord_left + 2f*line_stroke_width, coord_bottom - max_value.floatValue() / coord_y_factor, testpaint);
        canvas.drawText(max_value.toString(),
                coord_left - 3f*line_stroke_width - text_paint.measureText(max_value.toString()),
                coord_bottom - max_value.floatValue() / coord_y_factor + text_heigth/2f,
                text_paint);
        canvas.drawLine(coord_left + max_time.floatValue() / coord_x_factor, coord_bottom - 2f*line_stroke_width,
                coord_left + max_time.floatValue() / coord_x_factor, coord_bottom + 3f*line_stroke_width, testpaint);
        canvas.drawText(max_time.toString(),
                coord_left + max_time.floatValue() / coord_x_factor - text_paint.measureText(max_time.toString())/2f,
                coord_bottom + 3f*line_stroke_width - text_heigth,
                text_paint);*/

        // Draw Game Marker
        if(game_marker!=null) {
            for (Integer i = 0; i < game_marker.size(); i++) {
                if(game_marker.get(i).time < zoom_window_low || game_marker.get(i).time > zoom_window_high) continue;
                canvas.drawLine(coord_left + (game_marker.get(i).time.floatValue()-zoom_window_low) / coord_x_factor,
                        coord_bottom - 2f * line_stroke_width,
                        coord_left + (game_marker.get(i).time.floatValue()-zoom_window_low) / coord_x_factor,
                        coord_bottom + 3f * line_stroke_width, game_marker_paint);
                canvas.drawText(game_marker.get(i).game_number.toString(),
                        coord_left + (game_marker.get(i).time.floatValue()-zoom_window_low) / coord_x_factor - line_stroke_width - game_marker_paint.measureText(game_marker.get(i).game_number.toString()),
                        coord_bottom - 2f * line_stroke_width - game_marker_text_heigth / 2f,
                        game_marker_paint);
            }
        }
    }

    @Override
    protected void onSizeChanged (int w,
                                  int h,
                                  int oldw,
                                  int oldh){
        super.onSizeChanged(w,h,oldw,oldh);

        contentWidth = w - paddingLeft - paddingRight;
        contentHeight = h - paddingTop - paddingBottom;

        diagram_width = 0.88f*contentWidth;
        diagram_height = 0.85f*contentHeight;

        coord_left = 0.1f*contentWidth + line_stroke_width;
        coord_bottom = 0.9f*contentHeight - line_stroke_width;
        coord_width = diagram_width - 2*pointer_length;
        coord_height = diagram_height - 2*pointer_length;

        coord_x_path = new Path();
        coord_x_path.moveTo(coord_left, coord_bottom);
        coord_x_path.lineTo(coord_left + coord_width, coord_bottom);
        coord_y_path = new Path();
        coord_y_path.moveTo(coord_left, coord_bottom);
        coord_y_path.lineTo(coord_left, coord_bottom - coord_height);

        coord_y_factor = max_value.floatValue()/coord_height;
        coord_x_factor = Integer.valueOf(zoom_window_high-zoom_window_low).floatValue()/coord_width;

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touched = true;
                touched_x = event.getX();
                touched_y = event.getY();
                ontouch_zoom_window_low =zoom_window_low;
                ontouch_zoom_window_high = zoom_window_high;
                break;
            case MotionEvent.ACTION_MOVE:
                if(touched) {
                    if(ontouch_zoom_window_high-Float.valueOf((event.getX()-touched_x)*coord_x_factor).intValue() > max_time){
                        zoom_window_low = max_time - (zoom_window_high - zoom_window_low);
                        zoom_window_high = max_time;
                        touched_x = event.getX();
                    } else if(ontouch_zoom_window_low - Float.valueOf((event.getX()-touched_x)*coord_x_factor).intValue() < 0){
                        zoom_window_high = zoom_window_high - zoom_window_low;
                        zoom_window_low = 0;
                        touched_x = event.getX();
                    } else {
                        zoom_window_low = ontouch_zoom_window_low - Float.valueOf((event.getX() - touched_x)*coord_x_factor).intValue();
                        zoom_window_high = ontouch_zoom_window_high - Float.valueOf((event.getX() - touched_x)*coord_x_factor).intValue();
                    }
                    axisManager.ConfigAxisTicks(zoom_window_low, zoom_window_high, 0.0, max_value,coord_width,coord_height);
                    view_total=0.0;
                    for(Integer i=zoom_window_low/binning_length;i<values.size() && i<zoom_window_high/binning_length+1;i++){
                        view_total += values.get(i);
                    }
                    view_average = view_total/(zoom_window_high - zoom_window_low);
                    if(mListener!=null){
                        mListener.onDataViewPositionChange(view_total, view_average);
                    }
                    this.invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                touched=false;
                break;
            case MotionEvent.ACTION_CANCEL:
                touched = false;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                touched = false;
                break;
            default:
        }
        return true; // processed
    }

    private void drawPointer(Canvas canvas, float from_x, float from_y, float to_x, float to_y, Paint paint){
        Point f = new Point(from_x, from_y);
        Point t = new Point(to_x, to_y);
        Point v = t.substract(f);
        Point p = f.add(v.multiply(1-pointer_length/v.length()));
        double alpha = Math.PI*(35.0)/180d;

        canvas.drawLine(from_x, from_y, to_x, to_y, paint);
        Point n = p.rotatePointAroundPoint(alpha, t);
        canvas.drawLine(t.x, t.y, n.x, n.y, paint);
        Point n2 = p.rotatePointAroundPoint(-alpha,t);
        canvas.drawLine(t.x, t.y, n2.x, n2.y, paint);

    }

    public void setCurrentStat(HashMap<Integer,StatsManager.Stat> list){
        current_stat = new ArrayList<>();
        boolean flag=false;
        overall_total=0.0;
        for(Integer num : list.keySet()){
            Stat stat = new Stat();
            for(StatsManager.Stat_Point point : list.get(num).getPoints()){
                stat.list.add(new Stat_Point(point.value,point.time));
            }
            stat.average = list.get(num).getAverage();
            stat.game_number = num;
            stat.duration = list.get(num).getDuration();
            if(!flag) {min_duration = stat.duration;flag=true;}
            else if(stat.duration < min_duration) min_duration = stat.duration;
            stat.total = list.get(num).getTotal();
            overall_total += stat.total;

            current_stat.add(stat);
        }
        binning_length = 5;
        DecodeStatsToValues(binning_length, 100f, true);
        overall_average = overall_total / max_time;
        view_average = overall_average;
        view_total = overall_total;
        this.invalidate();
    }

    public void setBinningLength(Integer bin_length){
        if(bin_length==0) bin_length=1;
        binning_length = bin_length;
        DecodeStatsToValues(bin_length, zoom, false);
        this.invalidate();
    }

    public void setZoom(float zoom_value){
        UpdateDiagramForZoomWindow(false,zoom_value);
        this.invalidate();
    }

    public Integer getMinDuration(){return min_duration;}

    public Double getOverallTotal(){return overall_total;}
    public Double getOverallAverage(){return overall_average;}
    public Double getViewTotal(){return view_total;}
    public Double getViewAverage(){return view_average;}
    public void setOnStatsDiagramViewListener(OnStatsDiagramViewListener listener){mListener = listener;}

    private void DecodeStatsToValues(Integer bin_length, float zoom_value, boolean reset_zoom_position){
        Integer time_pos=bin_length;
        Integer time_offset=0;
        Integer rest_time=0;
        max_value = 0.0;
        game_marker = new ArrayList<>();
        values = new ArrayList<>();
        Double value=0.0;
        boolean flag = false;
        for(Stat stat : current_stat){
            for(Stat_Point point : stat.list){
                while(point.time - time_pos > bin_length){
                    values.add(0.0);
                    time_pos += bin_length;
                }
                if(point.time < time_pos){
                    value += point.value;
                } else {
                    values.add(value);
                    if(value > max_value) max_value = value;
                    value=point.value;
                    time_pos += bin_length;
                }
            }
            rest_time = time_pos - stat.duration;
            time_offset += stat.duration;
            if(rest_time==0) {values.add(value); value = 0.0; time_pos = bin_length;}
            else time_pos = rest_time;

            game_marker.add(new GameMarker(time_pos + time_offset,stat.game_number));
        }

        max_time = time_pos + time_offset;

        UpdateDiagramForZoomWindow(reset_zoom_position, zoom_value);
    }

    private void UpdateDiagramForZoomWindow(boolean reset_zoom_position, float zoom_value){
        if(reset_zoom_position) {zoom_window_low = 0;zoom_window_high = max_time;}
        else {
            Integer window_length = Float.valueOf(max_time / (zoom_value / 100f)).intValue();
            zoom = zoom_value;
            if (window_length == 0) window_length = 1;
            if (window_length > max_time) {
                zoom_window_low = 0;
                zoom_window_high = max_time;
                zoom = 100f;
            } else {
                Integer window_middle = (zoom_window_high - zoom_window_low) / 2;
                zoom_window_low = window_middle - window_length / 2;
                zoom_window_high = window_middle + window_length / 2;

                if (zoom_window_high > max_time) {
                    zoom_window_low -= zoom_window_high - max_time;
                    zoom_window_high = max_time;
                } else if (zoom_window_low < 0) {
                    zoom_window_high += Math.abs(zoom_window_low);
                    zoom_window_low = 0;
                }
            }
        }

        coord_y_factor = max_value.floatValue()/coord_height;
        coord_x_factor = Integer.valueOf(zoom_window_high-zoom_window_low).floatValue()/coord_width;
        axisManager.ConfigAxisTicks(zoom_window_low, zoom_window_high, 0.0, max_value, coord_width, coord_height);
        view_total=0.0;
        for(Integer i=zoom_window_low/binning_length;i<values.size() && i<zoom_window_high/binning_length+1;i++){
            view_total += values.get(i);
        }
        view_average = view_total/(zoom_window_high - zoom_window_low);
        if(mListener!=null){
            mListener.onDataViewPositionChange(view_total, view_average);
        }
    }

}
