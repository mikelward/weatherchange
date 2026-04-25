package com.mikelward.weatherchange;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout briefing = new LinearLayout(this);
        briefing.setGravity(Gravity.CENTER_HORIZONTAL);
        briefing.setOrientation(LinearLayout.VERTICAL);
        briefing.setPadding(48, 72, 48, 72);
        briefing.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView summary = new TextView(this);
        summary.setText(R.string.briefing_summary);
        summary.setTextSize(18);
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(0, 24, 0, 0);

        TextView nextStep = new TextView(this);
        nextStep.setText(R.string.briefing_next_step);
        nextStep.setTextSize(16);
        nextStep.setGravity(Gravity.CENTER);
        nextStep.setPadding(0, 24, 0, 0);

        briefing.addView(title);
        briefing.addView(summary);
        briefing.addView(nextStep);

        setContentView(briefing);
    }
}
