package edu.uw.cs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shantanusinghal on 16/02/17 @ 2:17 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
class Predictions {

    private List<String> predictions;
    private int numCorrectPredictions;

    Predictions() {
        numCorrectPredictions = 0;
        predictions = new ArrayList<String>();
    }

    void addPrediction(String actual, String predicted, double probPrediction) {
        predictions.add(format(actual, predicted, probPrediction));
        if(actual.equals(predicted)) numCorrectPredictions++;
    }

    String getPredictions() {
        StringBuilder sb = new StringBuilder();
        for (String prediction : predictions) {
            sb.append(prediction).append("\n");
        }
        return sb.toString();
    }

    int getNumCorrectPredictions() {
        return numCorrectPredictions;
    }

    private String format(String actual, String predicted, double probPrediction) {
        return String.format("%s %s %.12f", predicted, actual, probPrediction);
    }

}
