package com.rai220.securityalarmbot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.rai220.securityalarmbot.model.TimeStats;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.Converters;

import java.util.List;

/**
 *
 */

public class GraphActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        List<TimeStats> timeStatsList = PrefsController.instance.getPrefs().getTimeStatsList();
        if (!timeStatsList.isEmpty()) {
            // battery temperature graph
            List<Float> batteryTemperatureList = Converters.TIME_STATS_TO_TEMPERATURE_LIST.apply(timeStatsList);
            DataPoint[] batTempDataPoints = new DataPoint[batteryTemperatureList.size()];
            int i = 0;
            for (Float batteryTemperature : batteryTemperatureList) {
                batTempDataPoints[i++] = new DataPoint(i, batteryTemperature);
            }

            GraphView tempGraph = (GraphView) findViewById(R.id.temperature_graph);

            LineGraphSeries<DataPoint> batTempSeries = new LineGraphSeries<>(batTempDataPoints);
            batTempSeries.setDrawDataPoints(true);
            tempGraph.addSeries(batTempSeries);

            // battery level graph
//            List<Float> batteryLevelList = Converters.TIME_STATS_TO_BATTERY_LEVEL.apply(timeStatsList);
//            DataPoint[] batLevelDataPoints = new DataPoint[batteryLevelList.size()];
//            i = 0;
//            for (Float batteryLevel: batteryLevelList) {
//                batLevelDataPoints[i++] = new DataPoint(i, batteryLevel);
//            }
//            LineGraphSeries<DataPoint> batLevelSeries = new LineGraphSeries<>(batLevelDataPoints);
//            GraphView lvlGraph = (GraphView) findViewById(R.id.level_graph);
//            lvlGraph.addSeries(batLevelSeries);
        }

    }

    public void onCloseClick(View view) {
        finish();
    }
}
