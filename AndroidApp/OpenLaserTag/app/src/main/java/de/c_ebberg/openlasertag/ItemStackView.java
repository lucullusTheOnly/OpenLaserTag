package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.ImageButton;


public class ItemStackView extends ImageButton {
    private Integer item_id=-1;
    private Integer item_list_id=-1;

    private String number_text="1";
    private Integer number=1;
    private int number_color = Color.RED;
    private float number_dimension = 0.3f;
    private TextPaint number_paint;
    private float number_text_width;
    private float number_text_height;

    private String value_text="100";
    private Integer value=100;
    private int value_color = Color.WHITE;
    private float value_dimension = 0.5f;
    private TextPaint value_paint;
    private float value_text_width;
    private float value_text_height;

    private float gray_out_ratio=1.0f;
    private Paint gray_out_paint;

    int contentWidth=100;
    int contentHeight=100;
    int paddingLeft = 0;
    int paddingTop = 0;
    int paddingRight = 0;
    int paddingBottom = 0;

    public ItemStackView(Context context) {
        super(context);
        init(null, 0);
    }

    public ItemStackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ItemStackView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ItemStackView, defStyle, 0);

        number_color = a.getColor(
                R.styleable.ItemStackView_NumberTextColor,
                number_color);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        number_dimension = a.getFloat(R.styleable.ItemStackView_NumberTextDimensionRatio, number_dimension);

        value_color = a.getColor(R.styleable.ItemStackView_ValueTextColor, value_color);
        value_dimension = a.getFloat(R.styleable.ItemStackView_ValueTextDimensionRatio, value_dimension);

        if(a.hasValue(R.styleable.ItemStackView_Value)){
            value_text = a.getString(R.styleable.ItemStackView_Value);
        }
        number = a.getInteger(R.styleable.ItemStackView_Number,number);
        number_text = number.toString();

        a.recycle();

        // Set up a default TextPaint object
        number_paint = new TextPaint();
        number_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        number_paint.setFakeBoldText(true);
        number_paint.setTextAlign(Paint.Align.LEFT);

        value_paint = new TextPaint();
        value_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        value_paint.setFakeBoldText(true);
        value_paint.setTextAlign(Paint.Align.LEFT);

        gray_out_paint = new Paint();
        gray_out_paint.setColor(Color.parseColor("#BB000000"));

        setScaleType(ScaleType.FIT_CENTER);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        number_paint.setTextSize(number_dimension*contentWidth);
        number_paint.setColor(number_color);
        number_text_width = number_paint.measureText(number_text);

        Paint.FontMetrics number_fontMetrics = number_paint.getFontMetrics();
        number_text_height = number_fontMetrics.bottom;

        value_paint.setTextSize(value_dimension*contentWidth);
        value_paint.setColor(value_color);
        value_text_width = value_paint.measureText(value_text);

        Paint.FontMetrics value_fontMetrics = value_paint.getFontMetrics();
        value_text_height = value_fontMetrics.bottom;
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

        invalidateTextPaintAndMeasurements();

        canvas.drawRect(paddingLeft,
                paddingTop,
                paddingLeft + contentWidth,
                paddingTop + (1-gray_out_ratio)*contentHeight,gray_out_paint);


        // Draw the text.
        if(!(value_text.equals("0") || value_text.equals(""))){
            canvas.drawText(value_text,
                    paddingLeft + (contentWidth - value_text_width) * 0.05f,
                    paddingTop + (contentHeight + value_text_height) * 0.85f,
                    value_paint);
        }
        if(!(number_text.equals("0") || number_text.equals("1"))) {
            canvas.drawText(number_text,
                    paddingLeft + (contentWidth - number_text_width) * 0.90f,
                    paddingTop + (contentHeight + number_text_height) * 0.85f,
                    number_paint);
        }

    }

    public void setNumber(Integer _number){
        number = _number;
        number_text = _number.toString();
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public void increaseNumber(Integer amount){
        number+=amount;
        number_text = number.toString();
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public void decreaseNumber(Integer amount){
        number-=amount;
        number_text = number.toString();
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public Integer getNumber(){
        return number;
    }

    public void setNumberColor(Integer color){
        number_color = color;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public Integer getNumberColor(){return number_color;}

    public void setNumberDimensionRatio(float ratio){number_dimension = ratio;}

    public float getNumberDimensionRatio(){return number_dimension;}

    public void setValueText(String value){
        value_text = value;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public String getValueText(){
        return value_text;
    }

    public void setValueColor(Integer color){
        value_color = color;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public void setValueDimensionRatio(float ratio){value_dimension = ratio;}

    public float getValueDimensionRatio(){return value_dimension;}

    public void setItemID(Integer ID){item_id = ID;}

    public Integer getItemID(){return item_id;}

    public void setItemListID(Integer ID){item_list_id = ID;}

    public Integer getItemListID(){return item_list_id;}

    public void setGrayOutRatio(Float ratio){
        gray_out_ratio = ratio;
        invalidate();
    }

}
