package com.example.martin.systemengineeringgsm;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Martin on 2016-05-19.
 */
public class GUI {

    private int width, height;
    private Context context;

    public GUI(int width, int height, Context context){
        this.width = width;
        this.height = height;
        this.context = context;
    }

    public Button largeBtn(String text){
        Button btn = new Button(context);

        btn.setText(text);
        btn.setTextColor(context.getResources().getColor(android.R.color.black));

        int largeWidth = width/2;
        int largeHeight = height/10;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(largeWidth, largeHeight);
        btn.setLayoutParams(params);

        return btn;
    }
}
