package edu.uw.cs;

import edu.uw.cs.model.Dataset;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by shantanusinghal on 03/03/17 @ 2:52 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class NeuralNetRunner {

    private static Dataset trainData = null;

    public static void main(String[] args) {
        if(args.length != 4)
            throw new IllegalStateException("Ensure Syntax: neuralnet <trainfile> <num_folds> <learning_rate> <num_epochs>");

        try {
            trainData = Dataset.readFrom(new File(args[0]));
        } catch (FileNotFoundException e) {
            System.out.println("File not found! Please check the path specified");
        } catch (IOException e) {
            System.out.println("Specified file doesn't conform to ARFF specifications");
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }

        NeuralNet net = new NeuralNet.Builder()
                .withOneOutputNode()
                .withOutputThreshold(0.5)
                .withTheseManyInputNodes(trainData.getAttributes().size())
                .withHiddenLayerOfSize(trainData.getAttributes().size())
                .build();

        int numFolds = Integer.valueOf(args[1]);
        int numEpochs = Integer.valueOf(args[3]);
        double learningRate = Double.valueOf(args[2]);

        double accuracy = net.SCVTraining(trainData, numFolds, numEpochs, learningRate);

        System.out.println(String.format("\nFinal Accuracy after %d folds of cross validation is %.2f%%", numEpochs, accuracy*100));
    }

}
