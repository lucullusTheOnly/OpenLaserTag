package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class HumanView extends View {
    private final String LOG_TAG="HumamView";
    private Context _context;
    private String mExampleString=" ";
    private float IDTextDimension = 0;
    private VectorDrawableCompat humanDrawable;
    private ArrayList<Rel_Point> positions = new ArrayList<>();
    private ArrayList<String> receiver = new ArrayList<>();
    private Integer inner_radius = 20;
    private Integer outer_radius = 40;
    private Paint positions_paint;
    private Paint sel_positions_paint;
    private Double human_aspect_ratio;
    private Double x_scaling;
    private Double y_scaling;
    private Double x_offset;
    private Double y_offset;
    private float free_pos_radius=50;

    float touched_x, touched_y;
    boolean touched = false;
    private Rel_Point touched_point=null;

    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();

    int contentWidth = getWidth() - paddingLeft - paddingRight;
    int contentHeight = getHeight() - paddingTop - paddingBottom;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private class Rel_Point{
        Double x=0.0;
        Double y=0.0;
        Boolean selected=false;
        String position="";
        Boolean has_receiver=false;
        Integer receiver_ID=-1;// -1 is invalid id

        Paint free_pos_paint;

        public Rel_Point(){}

        public Rel_Point(Rel_Point p){
            x = p.x;
            y = p.y;
            selected = p.selected;
            position = p.position;
            has_receiver = p.has_receiver;
            receiver_ID = p.receiver_ID;
            free_pos_paint = new Paint();
            free_pos_paint.setColor(0x55000000);
        }

        public Rel_Point(Double _x, Double _y){
            x=_x;
            y=_y;
            free_pos_paint = new Paint();
            free_pos_paint.setColor(0x55000000);
        }

        public Rel_Point(Double _x, Double _y, String _position){
            x=_x;
            y=_y;
            position =_position;
            free_pos_paint = new Paint();
            free_pos_paint.setColor(0x55000000);
        }
    }

    HumanViewConfigChangedListener event_listener;
    public interface HumanViewConfigChangedListener {
        void onConfigChanged(String new_config);
    }

    public HumanView(Context context) {
        super(context);
        init(context,null, 0);
    }

    public HumanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs, 0);
    }

    public HumanView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context,attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        _context = context;
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.HumanView, defStyle, 0);

        positions.add(new Rel_Point(0.5,0.05, "Head"));//Kopf
        positions.add(new Rel_Point(0.5,0.3, "Chest"));//Brust Mitte
        positions.add(new Rel_Point(0.40,0.45,"BellyLeft"));// bauch links
        positions.add(new Rel_Point(0.60,0.45,"BellyRight"));// bauch rechts
        positions.add(new Rel_Point(0.40,0.2,"ShoulderLeft"));//schulter links
        positions.add(new Rel_Point(0.60,0.2,"ShoulderRight"));//schulter rechts
        positions.add(new Rel_Point(0.34,0.85,"KneeLeft")); // Knie links
        positions.add(new Rel_Point(0.66,0.85,"KneeRight")); // Knie rechts
        positions.add(new Rel_Point(0.23,0.35,"ArmLeft")); // Arm links
        positions.add(new Rel_Point(0.775,0.35,"ArmRight")); // arm rechts

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        IDTextDimension = a.getDimension(
                R.styleable.HumanView_IDTextDimension,
                IDTextDimension);

        if (a.hasValue(R.styleable.HumanView_ImageSrc)) {
            /*humanDrawable = VectorDrawableCompat.create(getResources(),
                    a.getResourceId(R.styleable.HumanView_ImageSrc,R.drawable.ic_human),
                    null);*/
            humanDrawable = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_human, context.getTheme());
            /*humanDrawable = a.getDrawable(
                    R.styleable.HumanView_ImageSrc);*/
            humanDrawable.setCallback(this);
            human_aspect_ratio = humanDrawable.getIntrinsicWidth()/Integer.valueOf(humanDrawable.getIntrinsicHeight()).doubleValue();
        }
        if(a.hasValue(R.styleable.HumanView_ReceiverPositions)){
            String rec_pos = a.getString(R.styleable.HumanView_ReceiverPositions)+",";
            for(Rel_Point p : positions){
                if(rec_pos.contains(p.position)){
                    p.has_receiver=true;
                    try{
                        p.receiver_ID = Integer.parseInt(rec_pos.substring(rec_pos.indexOf(p.position)+p.position.length(),rec_pos.indexOf(",",rec_pos.indexOf(p.position))));
                    } catch(NumberFormatException e){
                        p.receiver_ID = -1;
                    }
                }
            }
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setColor(Color.parseColor("#FFFFD002"));

        positions_paint = new Paint();
        positions_paint.setColor(Color.parseColor("#FFFFD002"));

        sel_positions_paint = new Paint();
        sel_positions_paint.setColor(Color.parseColor("#FFFFD002"));
        sel_positions_paint.setStyle(Paint.Style.STROKE);
        sel_positions_paint.setStrokeWidth(7);


        /*try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            event_listener = (HumanViewConfigChangedListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()+ " must implement HumanViewConfigChangedListener");
        }*/

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(IDTextDimension);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onSizeChanged (int w,
                        int h,
                        int oldw,
                        int oldh){
        super.onSizeChanged(w,h,oldw,oldh);

        contentWidth = w - paddingLeft - paddingRight;
        contentHeight = h - paddingTop - paddingBottom;

        if(contentWidth/Integer.valueOf(contentHeight).doubleValue()<human_aspect_ratio){// nach width ausrichten
            humanDrawable.setBounds(paddingLeft,
                    Double.valueOf(paddingTop + (contentHeight-contentWidth/human_aspect_ratio)/2).intValue(),
                    paddingLeft + contentWidth,
                    Double.valueOf(paddingTop + contentWidth/human_aspect_ratio+(contentHeight-contentWidth/human_aspect_ratio)/2).intValue());
            x_scaling=1.0;
            y_scaling=(contentWidth/human_aspect_ratio)/contentHeight;
            x_offset = 0.0;
            y_offset = (contentHeight-contentWidth/human_aspect_ratio)/2;
        } else{ // nach height ausrichten
            humanDrawable.setBounds(Double.valueOf(paddingLeft + (contentWidth-contentHeight*human_aspect_ratio)/2).intValue(),
                    paddingTop,
                    Double.valueOf(paddingLeft + contentHeight*human_aspect_ratio + (contentWidth-contentHeight*human_aspect_ratio)/2).intValue(),
                    paddingTop + contentHeight);
            x_scaling = (contentHeight*human_aspect_ratio)/contentWidth;
            y_scaling = 1.0;
            x_offset = (contentWidth-contentHeight*human_aspect_ratio)/2;
            y_offset = 0.0;
        }
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

        // Draw the example drawable on top of the text.
        if (humanDrawable != null) {
            humanDrawable.draw(canvas);
            for(Rel_Point p : positions){
                if(p.has_receiver) {
                    if (touched && touched_point == p) {
                        canvas.drawCircle(touched_x, touched_y, inner_radius, positions_paint);
                        canvas.drawCircle(touched_x, touched_y, outer_radius, sel_positions_paint);
                        canvas.drawCircle(Double.valueOf((paddingLeft + p.x * contentWidth)*x_scaling+x_offset).floatValue(), Double.valueOf((paddingTop + p.y * contentHeight)*y_scaling+y_offset).floatValue(),free_pos_radius,p.free_pos_paint);
                    } else {
                        canvas.drawCircle(Double.valueOf((paddingLeft + p.x * contentWidth)*x_scaling+x_offset).floatValue(), Double.valueOf((paddingTop + p.y * contentHeight)*y_scaling+y_offset).floatValue(), inner_radius, positions_paint);
                        if (p.selected)
                            canvas.drawCircle(Double.valueOf((paddingLeft + p.x * contentWidth)*x_scaling+x_offset).floatValue(), Double.valueOf((paddingTop + p.y * contentHeight)*y_scaling+y_offset).floatValue(), outer_radius, sel_positions_paint);
                        if(p.receiver_ID!=-1)
                            canvas.drawText(p.receiver_ID.toString(),
                                Double.valueOf((paddingLeft + p.x * contentWidth)*x_scaling+x_offset+outer_radius).floatValue(),
                                Double.valueOf((paddingTop + p.y * contentHeight)*y_scaling+y_offset).floatValue(),mTextPaint);
                    }
                } else if(touched){
                    canvas.drawCircle(Double.valueOf((paddingLeft + p.x * contentWidth)*x_scaling+x_offset).floatValue(), Double.valueOf((paddingTop + p.y * contentHeight)*y_scaling+y_offset).floatValue(),free_pos_radius,p.free_pos_paint);
                }
            }
            invalidateTextPaintAndMeasurements();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touched_x = event.getX();
        touched_y = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Boolean flag=false;
                for(Rel_Point p : positions){
                    if(Math.sqrt(Math.pow((paddingLeft+p.x*contentWidth)*x_scaling+x_offset-touched_x,2) + Math.pow((paddingTop+p.y*contentHeight)*y_scaling+y_offset-touched_y,2))<outer_radius && !flag){
                        p.selected=true;
                        flag=true;
                        touched=true;
                        touched_point=p;
                    } else{
                        p.selected = false;
                    }
                }
                this.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                //touched = true;
                if(touched) {
                    //touched_point.x = (double) (touched_x / contentWidth - paddingLeft);
                    //touched_point.y = (double) (touched_y / contentHeight - paddingTop);
                    this.invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if(touched) {
                    //touched_point.x = (double) (touched_x / contentWidth - paddingLeft);
                    //touched_point.y = (double) (touched_y / contentHeight - paddingTop);
                    for(Rel_Point p : positions){
                        if(Math.sqrt(Math.pow((paddingLeft+p.x*contentWidth)*x_scaling+x_offset-touched_x,2) + Math.pow((paddingTop+p.y*contentHeight)*y_scaling+y_offset-touched_y,2))<outer_radius) {
                            Rel_Point new_point = new Rel_Point(p);
                            if(p.has_receiver){
                                p.has_receiver = true;
                                p.selected = true;
                                p.receiver_ID = touched_point.receiver_ID;
                                touched_point.has_receiver = true;
                                touched_point.selected = false;
                                touched_point.receiver_ID = new_point.receiver_ID;
                            } else {
                                p.has_receiver = true;
                                p.selected = true;
                                p.receiver_ID = touched_point.receiver_ID;
                                touched_point.has_receiver = false;
                                touched_point.selected = false;
                                touched_point.receiver_ID = -1;
                            }
                            event_listener.onConfigChanged(getReceivers());
                            break;
                        }
                    }
                }
                this.invalidate();
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

    public Boolean addReceiver(String position, Integer receiverID){
        if(receiverID<0) return false;
        for(Rel_Point p : positions){
            if(p.position.equals(position) && !p.has_receiver){
                p.has_receiver=true;
                p.receiver_ID = receiverID;
                event_listener.onConfigChanged(getReceivers());
                invalidate();
                return true;
            }
        }
        return false;
    }

    public Boolean addReceiver(Integer receiverID){
        if(receiverID<0) return false;
        for(Rel_Point p : positions){
            if(!p.has_receiver){
                p.has_receiver=true;
                p.receiver_ID=receiverID;
                event_listener.onConfigChanged(getReceivers());
                invalidate();
                return true;
            }
        }
        return false;
    }

    public Integer removeSelectedReceiver(){
        for(Rel_Point p : positions){
            if(p.selected){
                p.selected=false;
                p.has_receiver=false;
                Integer id = p.receiver_ID;
                p.receiver_ID=-1;
                event_listener.onConfigChanged(getReceivers());
                invalidate();
                return id;
            }
        }
        return -1;
    }

    public Boolean removeReceiver(Integer receiverID){
        for(Rel_Point p : positions){
            if(p.receiver_ID.equals(receiverID)){
                p.selected=false;
                p.has_receiver=false;
                p.receiver_ID=-1;
                event_listener.onConfigChanged(getReceivers());
                invalidate();
                return true;
            }
        }
        return false;
    }

    public Integer getIDatPosition(String position){
        for(Rel_Point p : positions){
            if(p.position.equals(position)){
                return p.receiver_ID;
            }
        }
        return -1;
    }

    public Boolean setReceivers(String pos_string){
        if(pos_string.length()==0 || pos_string.equals(",")){
            for(Rel_Point p : positions){
                p.has_receiver = false;
                p.receiver_ID = -1;
                p.selected = false;
                invalidate();
            }
            return true;
        }
        pos_string+=",";
        for(Rel_Point p : positions){
            if(pos_string.contains(p.position)){
                p.has_receiver=true;
                p.selected = false;
                try{
                    p.receiver_ID = Integer.parseInt(pos_string.substring(pos_string.indexOf(p.position)+p.position.length(),pos_string.indexOf(",",pos_string.indexOf(p.position))));
                } catch(NumberFormatException e){
                    invalidate();
                    return false;
                } catch(StringIndexOutOfBoundsException e){
                    invalidate();
                    return false;
                }
            } else {
                p.has_receiver = false;
                p.receiver_ID = -1;
                p.selected = false;
            }
        }
        event_listener.onConfigChanged(getReceivers());
        invalidate();
        return true;
    }

    public String getReceivers(){
        String s="";
        for(Rel_Point p : positions){
            if(p.has_receiver){
                s+=p.position+p.receiver_ID.toString()+",";
            }
        }
        if(s.length()>0) s=s.substring(0,s.length()-1);
        return s;
    }

    public ArrayList<Integer> getUsedReceiverIDs(){
        ArrayList<Integer> used_receiver = new ArrayList<>();
        for(Rel_Point p : positions){
            if(p.has_receiver){
                used_receiver.add(p.receiver_ID);
            }
        }
        return used_receiver;
    }

    public void setOnConfigChangedListener(HumanViewConfigChangedListener listener){
        event_listener = listener;
    }

}
