package edu.uw.cs.model;

/**
 * Created by shantanusinghal on 09/03/17 @ 11:58 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Prediction implements Comparable<Prediction> {

    private int fold;
    private int index;
    private double confidence;
    private String actualClass;
    private String predictedClass;

    private final String RED = "\u001B[31m";
    private final String GREEN = "\u001B[32m";
    private final String RESET = "\u001B[0m";

    public Prediction(int fold, Instance instance, double confidence, String predictedClass) {
        this.fold = fold;
        this.confidence = confidence;
        this.index = instance.getIndex();
        this.actualClass = instance.getClassLabel();
        this.predictedClass = predictedClass;
    }

    public int getFold() {
        return fold;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getActualClass() {
        return actualClass;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public int getIndex() {
        return index;
    }

    public boolean isCorrect() {
        return predictedClass.equals(actualClass);
    }

    public int compareTo(Prediction o) {
        if(this.index == o.getIndex()) {
            return 0;
        }
        else
            return this.index > o.getIndex() ? 1 : -1;
    }

    @Override
    public String toString() {
        return String.format("%s%d %s %s %.8f%s", isCorrect() ? GREEN : RED, fold, predictedClass, actualClass, confidence, RESET);
    }
}
