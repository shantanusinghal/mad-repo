package edu.uw.cs;

import edu.uw.cs.model.Dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by shantanusinghal on 15/02/17 @ 8:05 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class BayesRunner {

    private static Dataset testData = null;
    private static Dataset trainData = null;

    public static void main(String[] args) {

        if(!args[2].toLowerCase().equals("n") && !args[2].toLowerCase().equals("t"))
            throw new IllegalStateException("Classifier flag should be either \'n\' or \'t\'");

        try {
            trainData = Dataset.readFrom(new File(args[0]));
            testData = Dataset.readFrom(new File(args[1]));
        } catch (FileNotFoundException e) {
            System.out.println("File not found! Please check the path specified");
        } catch (IOException e) {
            System.out.println("Specified file doesn't conform to ARFF specifications");
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }

        Classifier model = args[2].toLowerCase().equals("n") ?
                NaiveBayesClassifier.learn(trainData) :
                TreeAugmentedNBClassifier.learn(trainData);

        Predictions predictions = model.predict(testData);

        System.out.println(model.getStructureInfo());
        System.out.println(predictions.getPredictions());
        System.out.println(predictions.getNumCorrectPredictions());
    }

}
