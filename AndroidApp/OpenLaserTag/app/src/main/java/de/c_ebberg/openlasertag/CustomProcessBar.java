package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class CustomProcessBar extends View {
    private final String LOG_TAG = "CustomProcessBar";
    private int BarColor = Color.RED;
    private float text_dimension = 0; // TODO: use a default from R.dimen...
    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private int x_text_pos=15;
    private int y_text_pos=7;
    private int value_precision=2;

    private Paint bar_paint;

    private Float process_value;

    class ProcessBarInfo{
        int label_color = Color.BLACK;
        int bar_color = Color.RED;
        int bar_back_color=Color.BLACK;
        String label_string="Process";
        Double process_value= 0.0;
        Double value=0.0;
        Double max_value=1.0;
        Boolean show_value=false;
        Boolean show_process_value=false;
        int value_color = Color.BLACK;
        Paint value_paint;
        Paint label_paint;
        float value_text_width;
        float processvalue_text_width;
        Paint bar_paint;
        Paint bar_back_paint;
        int background_darker=100;
    }

    ArrayList<ProcessBarInfo> registered_processbars=new ArrayList<>();

    public CustomProcessBar(Context context) {
        super(context);
        init(null, 0);
    }

    public CustomProcessBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CustomProcessBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CustomProcessBar, defStyle, 0);

        /*label_string = a.getString(
                R.styleable.CustomProcessBar_LabelString);
        LabelColor = a.getColor(
                R.styleable.CustomProcessBar_LabelColor,
                LabelColor);*/
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        text_dimension = a.getDimension(
                R.styleable.CustomProcessBar_TextDimension,
                text_dimension);

        value_precision = a.getInteger(R.styleable.CustomProcessBar_ValuePreceision,value_precision);

        if(a.hasValue(R.styleable.CustomProcessBar_Bar1)){
            ProcessBarInfo bar = new ProcessBarInfo();
            bar.bar_color = a.getColor(R.styleable.CustomProcessBar_Bar1Color,bar.bar_color);
            bar.label_color = a.getColor(R.styleable.CustomProcessBar_Label1Color,bar.label_color);
            bar.label_string = a.getString(R.styleable.CustomProcessBar_Label1Text);
            bar.bar_paint = new Paint();
            bar.bar_paint.setColor(bar.bar_color);
            bar.label_paint = new Paint();
            bar.label_paint.setColor(bar.label_color);
            bar.label_paint.setTextSize(text_dimension);
            if(a.hasValue(R.styleable.CustomProcessBar_Bar1BackgroundDarker)){
                bar.background_darker = a.getInteger(R.styleable.CustomProcessBar_Bar1BackgroundDarker,bar.background_darker);
            }
            if(a.hasValue(R.styleable.CustomProcessBar_Bar1BackColor)){
                bar.bar_back_color = a.getColor(R.styleable.CustomProcessBar_Bar1BackColor,bar.bar_back_color);
            } else{
                bar.bar_back_color = Color.argb(Color.alpha(bar.bar_color),
                        (Color.red(bar.bar_color)>=bar.background_darker)?Color.red(bar.bar_color)-bar.background_darker:0,
                        (Color.green(bar.bar_color)>=bar.background_darker)?Color.green(bar.bar_color)-bar.background_darker:0,
                        (Color.blue(bar.bar_color)>=bar.background_darker)?Color.blue(bar.bar_color)-bar.background_darker:0);
            }
            bar.bar_back_paint = new Paint();
            bar.bar_back_paint.setColor(bar.bar_back_color);
            bar.show_process_value = a.getBoolean(R.styleable.CustomProcessBar_Bar2ShowProcessValue, false);
            bar.show_value = a.getBoolean(R.styleable.CustomProcessBar_Bar1ShowValue,false);
            bar.value_color = a.getColor(R.styleable.CustomProcessBar_Bar1ValueColor,bar.value_color);
            bar.value_paint = new Paint();
            bar.value_paint.setColor(bar.value_color);
            bar.value_paint.setTextSize(text_dimension);
            bar.value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar1Value,(float)0.0);
            bar.process_value=(double)a.getFloat(R.styleable.CustomProcessBar_Bar1ProcessValue, (float)0.0);
            bar.max_value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar1MaxValue,(float)1.0);
            if(a.hasValue(R.styleable.CustomProcessBar_Bar1Value)){
                bar.process_value = bar.value/bar.max_value;
            }
            registered_processbars.add(bar);
            if(a.hasValue(R.styleable.CustomProcessBar_Bar2)){
                bar = new ProcessBarInfo();
                bar.bar_color = a.getColor(R.styleable.CustomProcessBar_Bar2Color,bar.bar_color);
                bar.label_color = a.getColor(R.styleable.CustomProcessBar_Label2Color,bar.label_color);
                bar.label_string = a.getString(R.styleable.CustomProcessBar_Label2Text);
                bar.bar_paint = new Paint();
                bar.bar_paint.setColor(bar.bar_color);
                bar.label_paint = new Paint();
                bar.label_paint.setColor(bar.label_color);
                bar.label_paint.setTextSize(text_dimension);
                if(a.hasValue(R.styleable.CustomProcessBar_Bar2BackgroundDarker)){
                    bar.background_darker = a.getInteger(R.styleable.CustomProcessBar_Bar2BackgroundDarker,bar.background_darker);
                }
                if(a.hasValue(R.styleable.CustomProcessBar_Bar2BackColor)){
                    bar.bar_back_color = a.getColor(R.styleable.CustomProcessBar_Bar2BackColor,bar.bar_back_color);
                } else{
                    bar.bar_back_color = Color.argb(Color.alpha(bar.bar_color),
                            (Color.red(bar.bar_color)>=bar.background_darker)?Color.red(bar.bar_color)-bar.background_darker:0,
                            (Color.green(bar.bar_color)>=bar.background_darker)?Color.green(bar.bar_color)-bar.background_darker:0,
                            (Color.blue(bar.bar_color)>=bar.background_darker)?Color.blue(bar.bar_color)-bar.background_darker:0);
                }
                bar.bar_back_paint = new Paint();
                bar.bar_back_paint.setColor(bar.bar_back_color);
                bar.show_process_value = a.getBoolean(R.styleable.CustomProcessBar_Bar2ShowProcessValue, false);
                bar.show_value = a.getBoolean(R.styleable.CustomProcessBar_Bar2ShowValue,false);
                bar.value_color = a.getColor(R.styleable.CustomProcessBar_Bar2ValueColor,bar.value_color);
                bar.value_paint = new Paint();
                bar.value_paint.setColor(bar.value_color);
                bar.value_paint.setTextSize(text_dimension);
                bar.process_value=(double)a.getFloat(R.styleable.CustomProcessBar_Bar2ProcessValue, (float)0.0);
                bar.value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar2Value,(float)0.0);
                bar.max_value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar2MaxValue,(float)1.0);
                if(a.hasValue(R.styleable.CustomProcessBar_Bar2Value)){
                    bar.process_value = bar.value/bar.max_value;
                }
                registered_processbars.add(bar);
                if(a.hasValue(R.styleable.CustomProcessBar_Bar3)){
                    bar = new ProcessBarInfo();
                    bar.bar_color = a.getColor(R.styleable.CustomProcessBar_Bar3Color,bar.bar_color);
                    bar.label_color = a.getColor(R.styleable.CustomProcessBar_Label3Color,bar.label_color);
                    bar.label_string = a.getString(R.styleable.CustomProcessBar_Label3Text);
                    bar.bar_paint = new Paint();
                    bar.bar_paint.setColor(bar.bar_color);
                    bar.label_paint = new Paint();
                    bar.label_paint.setColor(bar.label_color);
                    bar.label_paint.setTextSize(text_dimension);
                    if(a.hasValue(R.styleable.CustomProcessBar_Bar3BackgroundDarker)){
                        bar.background_darker = a.getInteger(R.styleable.CustomProcessBar_Bar3BackgroundDarker,bar.background_darker);
                    }
                    if(a.hasValue(R.styleable.CustomProcessBar_Bar3BackColor)){
                        bar.bar_back_color = a.getColor(R.styleable.CustomProcessBar_Bar3BackColor,bar.bar_back_color);
                    } else{
                        bar.bar_back_color = Color.argb(Color.alpha(bar.bar_color),
                                (Color.red(bar.bar_color)>=bar.background_darker)?Color.red(bar.bar_color)-bar.background_darker:0,
                                (Color.green(bar.bar_color)>=bar.background_darker)?Color.green(bar.bar_color)-bar.background_darker:0,
                                (Color.blue(bar.bar_color)>=bar.background_darker)?Color.blue(bar.bar_color)-bar.background_darker:0);
                    }
                    bar.bar_back_paint = new Paint();
                    bar.bar_back_paint.setColor(bar.bar_back_color);
                    bar.show_process_value = a.getBoolean(R.styleable.CustomProcessBar_Bar3ShowProcessValue, false);
                    bar.show_value = a.getBoolean(R.styleable.CustomProcessBar_Bar3ShowValue,false);
                    bar.value_color = a.getColor(R.styleable.CustomProcessBar_Bar3ValueColor,bar.value_color);
                    bar.value_paint = new Paint();
                    bar.value_paint.setColor(bar.value_color);
                    bar.value_paint.setTextSize(text_dimension);
                    bar.process_value=(double)a.getFloat(R.styleable.CustomProcessBar_Bar3ProcessValue, (float)0.0);
                    bar.value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar3Value,(float)0.0);
                    bar.max_value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar3MaxValue,(float)1.0);
                    if(a.hasValue(R.styleable.CustomProcessBar_Bar3Value)){
                        bar.process_value = bar.value/bar.max_value;
                    }
                    registered_processbars.add(bar);
                    if(a.hasValue(R.styleable.CustomProcessBar_Bar4)){
                        bar = new ProcessBarInfo();
                        bar.bar_color = a.getColor(R.styleable.CustomProcessBar_Bar4Color,bar.bar_color);
                        bar.label_color = a.getColor(R.styleable.CustomProcessBar_Label4Color,bar.label_color);
                        bar.label_string = a.getString(R.styleable.CustomProcessBar_Label4Text);
                        bar.bar_paint = new Paint();
                        bar.bar_paint.setColor(bar.bar_color);
                        bar.label_paint = new Paint();
                        bar.label_paint.setColor(bar.label_color);
                        bar.label_paint.setTextSize(text_dimension);
                        if(a.hasValue(R.styleable.CustomProcessBar_Bar4BackgroundDarker)){
                            bar.background_darker = a.getInteger(R.styleable.CustomProcessBar_Bar4BackgroundDarker,bar.background_darker);
                        }
                        if(a.hasValue(R.styleable.CustomProcessBar_Bar4BackColor)){
                            bar.bar_back_color = a.getColor(R.styleable.CustomProcessBar_Bar4BackColor,bar.bar_back_color);
                        } else{
                            bar.bar_back_color = Color.argb(Color.alpha(bar.bar_color),
                                    (Color.red(bar.bar_color)>=bar.background_darker)?Color.red(bar.bar_color)-bar.background_darker:0,
                                    (Color.green(bar.bar_color)>=bar.background_darker)?Color.green(bar.bar_color)-bar.background_darker:0,
                                    (Color.blue(bar.bar_color)>=bar.background_darker)?Color.blue(bar.bar_color)-bar.background_darker:0);
                        }
                        bar.bar_back_paint = new Paint();
                        bar.bar_back_paint.setColor(bar.bar_back_color);
                        bar.show_process_value = a.getBoolean(R.styleable.CustomProcessBar_Bar4ShowProcessValue, false);
                        bar.show_value = a.getBoolean(R.styleable.CustomProcessBar_Bar4ShowValue,false);
                        bar.value_color = a.getColor(R.styleable.CustomProcessBar_Bar4ValueColor,bar.value_color);
                        bar.value_paint = new Paint();
                        bar.value_paint.setColor(bar.value_color);
                        bar.value_paint.setTextSize(text_dimension);
                        bar.process_value=(double)a.getFloat(R.styleable.CustomProcessBar_Bar4ProcessValue, (float)0.0);
                        bar.value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar4Value,(float)0.0);
                        bar.max_value = (double) a.getFloat(R.styleable.CustomProcessBar_Bar4MaxValue,(float)1.0);
                        if(a.hasValue(R.styleable.CustomProcessBar_Bar4Value)){
                            bar.process_value = bar.value/bar.max_value;
                        }
                        registered_processbars.add(bar);
                    }
                }
            }
        }
        /*ProcessBarInfo first_bar=new ProcessBarInfo();
        first_bar.process_value=0.75;
        first_bar.label_paint = new TextPaint();
        first_bar.label_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        first_bar.label_paint.setTextAlign(Paint.Align.LEFT);
        first_bar.label_paint.setColor(first_bar.label_color);
        first_bar.label_paint.setTextSize(text_dimension);
        first_bar.bar_paint = new Paint();
        first_bar.bar_paint.setColor(first_bar.bar_color);
        first_bar.bar_back_color = Color.argb(Color.alpha(first_bar.bar_color),
                (Color.red(first_bar.bar_color)>=first_bar.background_darker)?Color.red(first_bar.bar_color)-first_bar.background_darker:0,
                (Color.green(first_bar.bar_color)>=first_bar.background_darker)?Color.green(first_bar.bar_color)-first_bar.background_darker:0,
                (Color.blue(first_bar.bar_color)>=first_bar.background_darker)?Color.blue(first_bar.bar_color)-first_bar.background_darker:0);
        first_bar.bar_back_paint = new Paint();
        first_bar.bar_back_paint.setColor(first_bar.bar_back_color);
        registered_processbars.add(first_bar);*/

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        bar_paint = new Paint();
        bar_paint.setColor(BarColor);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextWidth=0;
        for(ProcessBarInfo info : registered_processbars) {
            mTextWidth = Math.max(mTextWidth,info.label_paint.measureText(info.label_string));
            info.value_text_width = info.value_paint.measureText(DoubleToString(info.value,value_precision)+" / "+DoubleToString(info.max_value,value_precision));
            info.processvalue_text_width = info.value_paint.measureText(DoubleToString(info.process_value,value_precision));
        }

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw the text.
        for(int i=0;i<registered_processbars.size();i++) {
            canvas.drawText(registered_processbars.get(i).label_string,
                    paddingLeft,
                    paddingTop + contentHeight / (2*registered_processbars.size())+ contentHeight/registered_processbars.size() *i + mTextHeight + y_text_pos,
                    registered_processbars.get(i).label_paint);

            canvas.drawRect(paddingLeft + mTextWidth + x_text_pos,
                    paddingTop+(contentHeight/registered_processbars.size())*i,
                    contentWidth,
                    (contentHeight/registered_processbars.size())*(i+1), registered_processbars.get(i).bar_back_paint);

            canvas.drawRect(paddingLeft + mTextWidth + x_text_pos,
                    paddingTop+(contentHeight/registered_processbars.size())*i,
                    paddingLeft + mTextWidth + x_text_pos + (int) ((contentWidth - paddingLeft - mTextWidth - x_text_pos) * registered_processbars.get(i).process_value),
                    (contentHeight/registered_processbars.size())*(i+1), registered_processbars.get(i).bar_paint);

            if(registered_processbars.get(i).show_process_value){
                canvas.drawText(DoubleToString(registered_processbars.get(i).process_value,value_precision),
                        paddingLeft + + mTextWidth + x_text_pos +(contentWidth-paddingLeft- mTextWidth - x_text_pos)/2 - registered_processbars.get(i).processvalue_text_width/2,
                        paddingTop + contentHeight / (2*registered_processbars.size())+ contentHeight/registered_processbars.size() *i + mTextHeight + y_text_pos,
                        registered_processbars.get(i).value_paint);
            } else if(registered_processbars.get(i).show_value){
                canvas.drawText(DoubleToString(registered_processbars.get(i).value,value_precision)+" / "+DoubleToString(registered_processbars.get(i).max_value,value_precision),
                        paddingLeft + mTextWidth + x_text_pos +(contentWidth-paddingLeft- mTextWidth - x_text_pos)/2 - registered_processbars.get(i).value_text_width/2,
                        paddingTop + contentHeight / (2*registered_processbars.size())+ contentHeight/registered_processbars.size() *i + mTextHeight + y_text_pos,
                        registered_processbars.get(i).value_paint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        int desiredWidth = Float.valueOf(mTextWidth).intValue()+100;
        int desiredHeight = 40*registered_processbars.size();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    private String DoubleToString(Double value, int precision){
        if(precision<0) return "";
        String s = value.toString();
        if(precision==0) return s.substring(0,s.indexOf("."));
        try {
            return s.substring(0, s.indexOf(".") + 1 + precision);
        } catch(StringIndexOutOfBoundsException e){
            return s;
        }
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getLabelString(int index) {
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        return registered_processbars.get(index).label_string;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setLabelString(int index, String exampleString) {
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        registered_processbars.get(index).label_string = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getLabelColor(int index) {
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        return registered_processbars.get(index).label_color;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param labelColor The example color attribute value to use.
     */
    public void setLabelColor(int index, int labelColor) {
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        registered_processbars.get(index).label_color = labelColor;
        registered_processbars.get(index).label_paint.setColor(labelColor);
        invalidateTextPaintAndMeasurements();
    }

    public int getBarColor(int index){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        return registered_processbars.get(index).bar_color;
    }

    public void setBarColor(int index, int barColor){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        registered_processbars.get(index).bar_color = barColor;
        registered_processbars.get(index).bar_paint.setColor(barColor);
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getTextDimension() {
        return text_dimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setTextDimension(float exampleDimension) {
        text_dimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    public void setProcessValue(int index, Double value){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        if(value<=1 && value>=0) registered_processbars.get(index).process_value = value;
        else throw new IllegalArgumentException();
        invalidate();
    }

    public Double getProcessValue(int index){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        return registered_processbars.get(index).process_value;
    }

    public void setMaxValue(int index, Double value){
        if(index<0 || index>=registered_processbars.size()){
            throw new IndexOutOfBoundsException();
        }
        registered_processbars.get(index).max_value = value;
        registered_processbars.get(index).process_value = registered_processbars.get(index).value / registered_processbars.get(index).max_value;
        invalidate();
    }

    public Double getMaxValue(int index){
        if(index<0 || index>=registered_processbars.size()){
            throw new IndexOutOfBoundsException();
        }
        return registered_processbars.get(index).max_value;
    }

    public void setValue(int index, Double value){
        if(index<0 || index>=registered_processbars.size()){
            throw new IndexOutOfBoundsException();
        }
        if(value < 0) throw new IllegalArgumentException("No values < 0 permitted");
        if(value>registered_processbars.get(index).max_value){
            registered_processbars.get(index).value = registered_processbars.get(index).max_value;
        } else {
            registered_processbars.get(index).value = value;
        }
        registered_processbars.get(index).process_value = registered_processbars.get(index).value / registered_processbars.get(index).max_value;
        invalidate();
    }

    public Double getValue(int index){
        if(index<0 || index>=registered_processbars.size()){
            throw new IndexOutOfBoundsException();
        }
        return registered_processbars.get(index).value;
    }

    public void setValuePrecision(Integer prec){
        if(prec < 0) throw new IllegalArgumentException();
        value_precision = prec;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public Integer getValuePrecision(){
        return value_precision;
    }

    public void AddProcessBar(String label_text, int text_color, int bar_color, Double value){
        ProcessBarInfo new_bar = new ProcessBarInfo();
        new_bar.label_string = label_text;
        new_bar.label_color = text_color;
        new_bar.bar_color = bar_color;
        new_bar.process_value = value;
        new_bar.label_paint = new TextPaint();
        new_bar.label_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        new_bar.label_paint.setTextAlign(Paint.Align.LEFT);
        new_bar.label_paint.setColor(new_bar.label_color);
        new_bar.label_paint.setTextSize(text_dimension);
        new_bar.bar_paint = new Paint();
        new_bar.bar_paint.setColor(new_bar.bar_color);
        new_bar.bar_back_color = Color.argb(Color.alpha(new_bar.bar_color),
                (Color.red(new_bar.bar_color)>=new_bar.background_darker)?Color.red(new_bar.bar_color)-new_bar.background_darker:0,
                (Color.green(new_bar.bar_color)>=new_bar.background_darker)?Color.green(new_bar.bar_color)-new_bar.background_darker:0,
                (Color.blue(new_bar.bar_color)>=new_bar.background_darker)?Color.blue(new_bar.bar_color)-new_bar.background_darker:0);
        new_bar.bar_back_paint = new Paint();
        new_bar.bar_back_paint.setColor(new_bar.bar_back_color);

        registered_processbars.add(new_bar);
        invalidateTextPaintAndMeasurements();
    }

    public void AddProcessBar(String label_text, int text_color, int bar_color, Double value, int background_darker){
        ProcessBarInfo new_bar = new ProcessBarInfo();
        new_bar.label_string = label_text;
        new_bar.label_color = text_color;
        new_bar.bar_color = bar_color;
        new_bar.process_value = value;
        new_bar.label_paint = new TextPaint();
        new_bar.label_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        new_bar.label_paint.setTextAlign(Paint.Align.LEFT);
        new_bar.label_paint.setColor(new_bar.label_color);
        new_bar.label_paint.setTextSize(text_dimension);
        new_bar.bar_paint = new Paint();
        new_bar.bar_paint.setColor(new_bar.bar_color);
        new_bar.background_darker = background_darker;
        new_bar.bar_back_color = Color.argb(Color.alpha(new_bar.bar_color),
                (Color.red(new_bar.bar_color)>=new_bar.background_darker)?Color.red(new_bar.bar_color)-new_bar.background_darker:0,
                (Color.green(new_bar.bar_color)>=new_bar.background_darker)?Color.green(new_bar.bar_color)-new_bar.background_darker:0,
                (Color.blue(new_bar.bar_color)>=new_bar.background_darker)?Color.blue(new_bar.bar_color)-new_bar.background_darker:0);
        new_bar.bar_back_paint = new Paint();
        new_bar.bar_back_paint.setColor(new_bar.bar_back_color);

        registered_processbars.add(new_bar);
        invalidateTextPaintAndMeasurements();
    }

    public void RemoveProcessBar(int index){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        registered_processbars.remove(index);
    }

    public void Clear(int index){
        if(index<0 || index>=registered_processbars.size())
            throw new IndexOutOfBoundsException();
        registered_processbars.clear();
    }

}
