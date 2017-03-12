package edu.uw.cs;

import edu.uw.cs.model.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by shantanusinghal on 03/03/17 @ 3:05 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class NeuralNet {

    private final int L;
    private final int FIRST = 0;
    private final int SECOND = 1;
    private final int numInputNodes;
    private final double BIAS = 1.0;

    private double threshold;
    private Double[][][] weights;
    private String[] classValues;

    /**
     * Used by the static inner class {@link edu.uw.cs.NeuralNet.Builder} to construct an
     untrained {@code NeuralNet} instance. All initial weights for all units including bias
     are set in the range (-0.1, 0.1)
     *
     * <p>The weight matrix encapsulates the network structure by tracking the all the incoming
     weights to a unit. Because the input layer doesn't have any incoming weights it is not
     captured in this matrix. The global constant {@link edu.uw.cs.NeuralNet#L} denoting the
     number layers also ignores the input layer and counts only hidden and output layers. The
     dimensions of the weight matrix in-order are
     * <ol>
     * <li> each layers
     * <li> each unit (excluding the bias) in the layer
     * <li> each incoming weight to the unit (weight for bias at 0-th)
     * </ol>
     *
     * @param  builder provides specifications for building the neural net skeleton
     *
     * @throws AssertionError If builder doesn't specify exactly one output unit
     *
     * **/
    private NeuralNet(Builder builder) {
        assertThat("Specified network only supports single output unit", builder.outputNodes, is(1));
        numInputNodes = builder.inputNodes; threshold = builder.threshold;

        // number of layers is counted as number of hidden layers plus one (for the output layer)
        L = builder.hiddenLayers.size() + 1;

        // initialize the weight matrix structure
        initWeightsMatrix(builder.hiddenLayers, numInputNodes, builder.outputNodes);

        // initialize all the weights (including bias) randomly
        resetWeights();
    }

    /**
     * Randomly set initial weights for all units including bias in the range (-0.1, 0.1)
     *
     * **/
    void resetWeights() {
        for(int n, i, l = 0; l < L; l++) for(n = 0; n < weights[l].length; n++) for(i = 0; i < weights[l][n].length; weights[l][n][i++] = random(-0.1, 0.1));
    }

    /**
     * Trains the neural network using n-fold stratified cross validation. All data instances are
     divided into {@code n} samples of equal sizes (if possible).
     *
     * In each epoch the net is trained on a set of {@code n - 1} folds, and the fold left out is used for testing.
     *
     * @param  dataset provides specifications for building the neural net skeleton, not null
     * @param  numFolds specifies the number of folds in cross validation
     * @param  numEpochs specifies the number of training iterations in each fold
     * @param  learningRate specifies the step size for gradient descent optimization
     *
     * @return the net accuracy measured across all folds of cross validation
     *
     * @throws IllegalStateException If dataset doesn't match the networks specifications
     *
     * **/
    Double SCVTraining(Dataset dataset, int numFolds, int numEpochs, double learningRate) {
        sanityCheck(dataset);
        Set<Prediction> predictions = new TreeSet<Prediction>(); Instance[] trainData, testData; int i = 0;
        classValues = new String[]{dataset.getClassValueAt(FIRST), dataset.getClassValueAt(SECOND)};

        // partition dataset into stratified folds for cross-validation training
        Instance[][] folds = getStratifiedFolds(numFolds, dataset.getInstances());

        // repeat training and validation over given number of folds
        for(int e, f = 0; f < numFolds; f++) {

            // build testing and training sets
            testData = folds[f]; trainData = joinExcept(folds, f);

            // repeat training for given number of epochs
            for(e = 0; e < numEpochs; e++) {
                printStatus(numEpochs, e, f);

                // in each epoch train over the entire training set online
                onlineTraining(trainData, dataset.getAttributes(), learningRate);
            }
            clearStatusLine();

            // add predictions for this fold's test instances to the complete list of predictions
            predictions.addAll(test(f, testData, dataset.getAttributes()));

            // print as predictions as possible without jumping indexes
            i = livePrint(predictions, i);
        }

        return measureAccuracy(predictions);
    }

    /**
     * Returns the accuracy as the portion of correct predictions over all predictions

     * @param  predictions list of predictions being analyzed, not null

     * @return the net accuracy, 0.0 if predictions list is empty
     *
     * **/
    public Double measureAccuracy(Set<Prediction> predictions) {
        double correct = 0.0, total = predictions.size();
        for (Prediction prediction : predictions) if (prediction.isCorrect()) correct++;
        return total == 0 ? 0.0 : correct / total;
    }

    /********************** PRIVATE METHODS - TRAINING **********************/

    private void initWeightsMatrix(List<Integer> hiddenLayerSizes, int numInputNodes, int numOutputNodes) {
        int numNeuron, sizePrevLayer, l, n;

        // initialize the first dimension of the weight matrix to hold all the layers (hidden and output)
        weights = new Double[L][][];

        // skip input layer ∵ there are no incoming weights

        // initialize weight matrix for each hidden layer(s)
        for(l = 0; l < hiddenLayerSizes.size(); l++) {
            numNeuron = hiddenLayerSizes.get(l);

            // allocate space for number of neurons specified in this layer
            weights[l] = new Double[numNeuron][];

            // number of incoming edge weights for a unit equal to the size of the previous layer
            // plus one for the bias unit (at zero-th index)
            sizePrevLayer = (l == 0 ? numInputNodes : weights[l - 1].length) + 1;

            // for each neuron allocate space for all incoming edge weights
            for(n = 0; n < numNeuron; n++) weights[l][n] = new Double[sizePrevLayer];
        }

        assertThat("There should only be one output node!", numOutputNodes, is(1));

        // initialize weight matrix for output layer
        weights[L - 1] = new Double[numOutputNodes][(l == 0 ? numInputNodes : weights[L - 2].length) + 1];
    }

    /**
     * Trains the neural network online using stochastic gradient descent.
     *
     * <p>The attribute values of the training instance are fed as input to the network
     * ({@link NeuralNet#forwardProp(Double[])}). At each step we calculate the error for
     * the output unit using the squared error function. If any error exists use backpropagation
     * ({@link NeuralNet#backProp(Double[][], int, double)}) to update each of the weights so
     * as to bring the actual output closer to the target output, thereby minimizing the error.</p>
     *
     * @param  instances list of training instances
     * @param  attributes list of input features common to all instances
     * @param  learningRate specifies the step size for gradient descent optimization
     *
     * **/
    private void onlineTraining(Instance[] instances, List<Attribute> attributes, double learningRate) {
        Double[] input;

        // for each training instance
        for (Instance instance : instances) {

            // build the input vector from the instance attributes
            input = getAttributeValueVector(instance, attributes);

            // feed the input to the neural and adjust weights to correct for prediction errors
            int expected = instance.getClassValueIndex();

            // apply input to the neural net to get output matrix
            Double[][] outputs = forwardProp(input);

            // get the prediction from the output matrix
            int predicted = getPredictedIndexFrom(outputs);

            // calculate the halved squared error in prediction
            double error = 0.5 * Math.pow((expected - predicted), 2);

            // back-propagate the error if any exists
            if (error > 0.0) weights = backProp(outputs, expected, learningRate);
        }
    }

    /**
     * The specified input is fed forward through the network, as the output of a layer
     * becomes the input of the next layer.
     *
     * @param  input specifies the values at the input layer
     *
     * @return a matrix of neuron outputs, including any bias units (at 0-th index)
     *
     * **/
    private Double[][] forwardProp(final Double[] input) {
        Double[] inputVector;
        Layer layer = new Layer().withBias(BIAS).madeOf(Neuron.SIGMOIDAL);

        /**
         * NOTE:
         *
         * size of outputs matrix is L + 1
         * because outputs matrix holds values for all layers, including the input layer
         *
         * size of weights matrix is L
         * because weights matrix doesn't count the input layer and starts at the first hidden layer
         *
         * **/
        // the outputs matrix holds the output of each unit (2nd dimension), including the bias, in each layer (1st dimension)
        Double[][] outputs = new Double[L + 1][];

        // the input vector for the first hidden layer is formed by joining
        // the list of input values with the bias value (at 0-th index)
        inputVector = new ArrayList<Double>() {{
            add(BIAS);
            Collections.addAll(this, input);
        }}.toArray(new Double[0]);

        // the input vector for the first hidden layer is also the output vector of the input layer
        outputs[0] = inputVector;

        // iterate forwards from the first layer
        for(int l = 0; l < L; l++) {

            // output of a layer becomes the input of the next layer
            // Also: cache the output of each layer to use in back-propagation step
            outputs[l + 1] = inputVector = layer.feed(inputVector, weights[l]);
        }

        return outputs;
    }

    /**
     * The error value of the output layer is propagated backwards and the weights are adjusted
     * to minimize the error function.
     *
     * <p>For each node {@code j} in layer {@code l}, we compute an "error term" {@code derivativeJ}
     * that measures how much that node was "responsible" for any errors in the output. Note that
     * we use the original weights, not the updated weights, when calculating the error term.</p>
     *
     * <p>For each weight incoming from a node in previous layer {@code i} we take steps of
     * gradient descent to reduce the error for the neuron and the network as a whole.</p>
     *
     * @param  outputs matrix of neuron outputs from forward pass
     * @param  target specified the expected output
     * @param  learningRate specifies the step size for gradient descent optimization
     *
     * @return a matrix of updated weights
     *
     * **/
    private Double[][][] backProp(Double[][] outputs, int target, double learningRate) {
        int l, i, j; Double outputI, derivativeJ, gradientJI; int numNeurons;

        Double[][] derivatives = new Double[L][];

        Double[][][] newWeights = new Double[L][][];

        // iterate backwards from the output layer
        for(l = L - 1; l >= 0; l--) {
            numNeurons = weights[l].length;
            derivatives[l] = new Double[numNeurons];
            newWeights[l] = new Double[numNeurons][];

            // for each neuron in the layer
            for(j = 0; j < numNeurons; j++) {
                newWeights[l][j] = new Double[weights[l][j].length];

                // derivative is calculated for each neuron using the chain rule and it's values are cached in the derivatives matrix
                derivativeJ = derivatives[l][j] = derivative(l, j, outputs, target, derivatives);

                // for each incoming weight edge on the neuron
                for(i = 0; i < weights[l][j].length; i++) {

                    // output of neuron i (from the previous layer) is
                    // for input layer and bias units - it's fixed value
                    // for hidden and output layer units - it's sigmoidal output
                    outputI = outputs[l][i];

                    // gradient is calculated as Δ wji = η * δj * oi
                    gradientJI = learningRate * derivativeJ * outputI;

                    // each weight is adjusted by the gradient
                    newWeights[l][j][i] = weights[l][j][i] + gradientJI;
                }
            }
        }

        return newWeights;
    }

    /**
     * Returns predictions corresponding to the list of test instances

     * @param  fold specifies the fold to which the all the test instances belong
     * @param  testInstances list of instances, not null
     * @param  attributes list of input features common to all instances, not null

     * @return list of predictions
     *
     * **/
    private List<Prediction> test(int fold, Instance[] testInstances, List<Attribute> attributes) {
        List<Prediction> predictions = new ArrayList<Prediction>();

        for (Instance instance : testInstances) predictions.add(test(fold, instance, attributes));

        return predictions;
    }

    /**
     * Returns the prediction obtained by feeding the test instance forward
     * through the network (without learning from it)
     *
     * @param  fold specifies the fold of test instance
     * @param  testInstance instances being tested, not null
     * @param  attributes list of input features, not null

     * @return prediction for test instance
     *
     * **/
    private Prediction test(int fold, Instance testInstance, List<Attribute> attributes) {
        Double[] input; Double[][] result; int predictedClass; double confidenceScore;

        // build the input vector from the instance attributes
        input = getAttributeValueVector(testInstance, attributes);

        // get the results from the neural net without back propagating the error
        result = forwardProp(input);

        // get the predicted class from the result
        predictedClass = getPredictedIndexFrom(result);

        // get the confidence measure from the result
        confidenceScore = getConfidenceFrom(result);

        return new Prediction(fold, testInstance, confidenceScore, classValues[predictedClass]);
    }

    private Double derivative(int l, int j, Double[][] outputs, int target, Double[][] derivatives) {
        Double derivative, outputJ = outputs[l + 1][j + 1];
        if(l == L - 1) {
            derivative = outputJ * (1 - outputJ) * (target - outputJ);
        } else {
            derivative = outputJ * (1 - outputJ) * sumBackPropErrors(l , j, derivatives);
        }
        return derivative;
    }

    private Double sumBackPropErrors(int l, int j, Double[][] derivatives) {
        Double derivativeK, weightKJ, sum = 0.0;
        for(int k = 0; k < weights[l + 1].length; k++) {
            weightKJ = weights[l + 1][k][j];
            derivativeK = derivatives[l + 1][k];
            sum += derivativeK * weightKJ;
        }
        return sum;
    }

    private double getConfidenceFrom(Double[][] outputs) {
//        return Math.abs(0.5 - outputs[L][1]) * 200;
        return outputs[L][1];
    }

    private int getPredictedIndexFrom(Double[][] outputs) {
        // if the sigmoidal output of the output neuron is less than THRESHOLD the predicted class
        // is one listed FIRST in the dataset else the SECOND
        return outputs[L][1] < threshold ? FIRST : SECOND;
    }

    private Double[] getAttributeValueVector(Instance instance, List<Attribute> attributes) {
        Double[] values = new Double[numInputNodes];
        for (int i = 0; i < numInputNodes; i++) values[i] = instance.getValue(attributes.get(i));
        return values;
    }

    /********************** PRIVATE METHODS - CROSS-VALIDATION **********************/

    private Instance[][] getStratifiedFolds(int numFolds, List<Instance> instances) {
        int extraNeg, extraPos, minPos, minNeg;
        double foldSize = (double) instances.size() / (double) numFolds;

        // separate out positive and negative instances
        List<Instance> positives = new ArrayList<Instance>();
        List<Instance> negatives = new ArrayList<Instance>();
        for (Instance instance : instances) {
            if(instance.getClassValueIndex() == FIRST) positives.add(instance);
            else negatives.add(instance);
        }

        // calculate minimum number of positive and negative instances allocated to each fold
        minNeg = (int) Math.floor(((double) negatives.size() / (double) instances.size()) * foldSize);
        minPos = (int) Math.floor(((double) positives.size() / (double) instances.size()) * foldSize);

        // calculate the unallocated number of positive and negative instances
        extraNeg = negatives.size() - (minNeg * numFolds);
        extraPos = positives.size() - (minPos * numFolds);

        /**
         * Create folds that are representative of all strata of the dataset (stratified).
         *
         * Each fold is composed of the following
         *
         *  - minimum number of positive instances
         *  - minimum number of negative instances
         *  - if the number of extra instances is more than the number of folds
         *      - then 1 extra positive and negative instance
         *  - else
         *      - if there are excess positive instances
         *          - then: 1 extra positive instance
         *      - else if there are excess negative instances
         *          - then: 1 extra positive instance
         *
         *  NOTE: that at any point only a maximum of 1 extra instance is included per fold
         */
        Instance[][] stratifiedFolds = new Instance[numFolds][];
        for (int p = 0, n = 0, i = 0; i < numFolds; i++) {
            stratifiedFolds[i] =  union(
                positives.subList(p, (p += minPos + (extraPos-- > 0 ? 1 : 0))),
                negatives.subList(n, (n += minNeg + ((extraPos + 1 + extraNeg-- >= (numFolds - i)) ? 1 : ++extraNeg > 0 && extraPos < 0 && extraNeg-- > 0 ? 1 : 0))));
        }

        return stratifiedFolds;
    }

    /********************** PRIVATE METHODS - UTILITY / SUGAR **********************/

    private static double random(double min, double max) {
        return Math.random() * max + min;
    }

    private Instance[] union(List<Instance> list1, List<Instance> list2) {
        List<Instance> union = new ArrayList<Instance>(list1); union.addAll(list2);
        return union.toArray(new Instance[0]);
    }

    private Instance[] joinExcept(Instance[][] folds, int except) {
        List<Instance> joinedInstances = new ArrayList<Instance>();
        for (int i = 0; i < folds.length; i++) if(i != except) Collections.addAll(joinedInstances, folds[i]);
        return joinedInstances.toArray(new Instance[0]);
    }

    private void sanityCheck(Dataset dataset) {
        if(dataset.getClassValues().size() != 2) throw new IllegalStateException("Specified network only supports binary classification");
        if(dataset.getAttributes().size() != numInputNodes) throw new IllegalStateException("Specified network requires exactly " + " number of input attributes");
    }

    private void clearStatusLine() {
        System.out.print("\r\033[2K");
    }

    private void printStatus(int total, int done, int fold) {
        int percentDone = (int) (((double)(done + 1)/(double) total) * 100);
        System.out.print(String.format("\rTraining Fold %d Epoch %d/%d [%s%s] %d%%", fold, done+1, total, StringUtils.repeat("#", percentDone/4), StringUtils.repeat(" ", (100-percentDone)/4), percentDone));
    }

    private int livePrint(Set<Prediction> predictions, int i) {
        for (Prediction prediction : predictions) {
            if(prediction.getIndex() == i) {
                System.out.println(prediction); i++;
            }
        }
        return i;
    }

    /********************** BUILDER **********************/

    static class Builder {
        private int inputNodes = 0;
        private int outputNodes = 0;
        private double threshold = 0.5;
        private List<Integer> hiddenLayers = new ArrayList<Integer>();

        NeuralNet build() {
            if(inputNodes <= 0) throw new IllegalStateException("You forgot to include input nodes");
            if(outputNodes == 0) throw new IllegalStateException("You forgot to include an output node");
            return new NeuralNet(this);
        }

        Builder withOneOutputNode() {
            this.outputNodes = 1;
            return this;
        }

        Builder withTheseManyInputNodes(int inputNodes) {
            this.inputNodes = inputNodes;
            return this;
        }

        Builder withHiddenLayerOfSize(int size) {
            hiddenLayers.add(size);
            return this;
        }

        Builder withOutputThreshold(double threshold) {
            this.threshold = threshold;
            return this;
        }
    }


}
