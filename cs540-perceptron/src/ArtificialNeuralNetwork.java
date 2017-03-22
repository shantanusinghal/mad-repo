import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by shantanusinghal on 14/09/16 @ 9:57 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class ArtificialNeuralNetwork {

    private static final String CROWN = "\u265B";
    private final static double EPSILON = 0.001;
    private static final String HELP = "\n\njava ArtificialNeuralNetwork <trainSetFilename> <tuneSetFilename> <testSetFilename> \n";

    public static void main(String[] args) {

        Feature features = null; List<String> categories = null; List<Tuple> trainingTuples = null, testingTuples = null, tuneTuples = null;

        /* read and parseFeature the training trainTuples */
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            features = readTuples(br);
            categories = readCategories(br);
            trainingTuples = readTuples(br, features.getKeys(), readNumberOfTuples(br));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("You forgot to specify the training data file as program input! Use this format:" + HELP); return;
        } catch (IOException e) {
            System.out.println("There was an error while reading the testing data, please check and try again"); return;
        }

        /* read and parseFeature the tune trainTuples */
        try (BufferedReader br = new BufferedReader(new FileReader(args[1]))) {
            skipFeatures(br);
            skipCategories(br);
            tuneTuples = readTuples(br, features.getKeys(), readNumberOfTuples(br));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("You forgot to specify the testing data file as program input! Use this format:" + HELP); return;
        } catch (IOException e) {
            System.out.println("There was an error while reading the testing data, please check and try again"); return;
        }

        /* read and parseFeature the testing trainTuples */
        try (BufferedReader br = new BufferedReader(new FileReader(args[2]))) {
            skipFeatures(br);
            skipCategories(br);
            testingTuples = readTuples(br, features.getKeys(), readNumberOfTuples(br));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("You forgot to specify the testing data file as program input! Use this format:" + HELP); return;
        } catch (IOException e) {
            System.out.println("There was an error while reading the testing data, please check and try again"); return;
        }

        int numberOfEpochs = 1000;
        ANN neuralNet = new ANN(features, categories, trainingTuples, tuneTuples, testingTuples, -1, 0, 0.1);
        neuralNet.trainFor(numberOfEpochs);

    }

    private static class ANN {
        private final double threshold;
        private Feature features;
        private Perceptron outputUnit;
        private List<Tuple> tuneTuples;
        private List<Tuple> testTuples;
        private List<Tuple> trainTuples;
        private List<String> categories;
        private Map<Integer, InputUnit> inputUnits;

        private Map<Integer, Double> testSetAccuracies = new HashMap<>();
        private Map<Integer, Double> tuneSetAccuracies = new HashMap<>();
        private Map<Integer, Double> trainSetAccuracies = new HashMap<>();
        private Map<Integer, List<Double>> historyOfWeights = new HashMap<>();

        public final double LEARNING_RATE;

        public ANN(Feature features, List<String> categories, List<Tuple> trainTuples, List<Tuple> tuneTuples, List<Tuple> testTuples, int bias, double threshold, double learningRate) {
            this.threshold = threshold;
            initInputUnits(features);
            this.features = features;
            this.categories = categories;
            this.tuneTuples = tuneTuples;
            this.testTuples = testTuples;
            this.trainTuples = trainTuples;
            this.LEARNING_RATE = learningRate;
            outputUnit = new Perceptron(bias, threshold, features.actualSize());
        }

        public void trainFor(int numberOfEpochs) {
            Tuple example;
            List<Integer> xi;
            List<Tuple> examples;
            for (int epoch = 1; epoch <= numberOfEpochs; epoch++) {
                // permute the order of the training examples at the start of every epoch
                examples = getShuffledInputFor(epoch);
                // initialize all the weights of the output unit to zero
                outputUnit.setThreshold(threshold);
                outputUnit.initWeights(features.actualSize());
                for (int i = 0; i < examples.size(); i++) {
                    example = examples.get(i);
                    xi = getInputVectorFrom(example);
                    initializeInputUnitsWithVector(xi);
                    forwardPropagate();
                    if(!isPredictionCorrectFor(example))
                        backPropagate(xi, example);
                }
                // at every 50th epoch record the state of the neural net and it's accuracies
                if(epoch % 50 == 0) {
                    historyOfWeights.put(epoch, getWeights());
                    tuneSetAccuracies.put(epoch, calcAccuracy(tuneTuples));
                    testSetAccuracies.put(epoch, calcAccuracy(testTuples));
                    trainSetAccuracies.put(epoch, calcAccuracy(trainTuples));
                    reportAccuraciesAt(epoch);
                }
            }
            // report on the details of the epoch with the best accuracy at the end of training
            System.out.println("\n-------------------------------------------------");
            System.out.println("\n\t " + CROWN + " WINNER " + CROWN);
            System.out.println("\n-------------------------------------------------\n");
            int bestEpoch = findLargest(tuneSetAccuracies);
            reportAccuraciesAt(bestEpoch);
            reportWeightsAt(bestEpoch);
            System.out.println("\n-------------------------------------------------\n");
        }

        // TODO write a better description
        // correct the weights
        private void backPropagate(List<Integer> inputVector, Tuple example) {
            Map<Integer, Double> currentWeights = outputUnit.getWeights();
            Map<Integer, Double> newWeights = new HashMap<>(currentWeights.size());

            int perceptronOutput = outputUnit.output();
            int correctCategory = parseCategory(example.getCategory());
            int featureValue; double currentWeight, adjustedWeight;

            // calculate the new threshold by the perceptron learning rule using the current threshold as the current weight and the bias as the feature value
            double newThreshold = perceptronLearningRule(outputUnit.getThreshold(), correctCategory, perceptronOutput, outputUnit.getBias());

            // for each weight vector use the perceptron learning rule to adjust the weight
            for (int i = 0; i < currentWeights.size(); i++) {
                featureValue = inputVector.get(i);
                currentWeight = currentWeights.get(i);
                adjustedWeight = perceptronLearningRule(currentWeight, correctCategory, perceptronOutput, featureValue);
                newWeights.put(i, adjustedWeight);
            }

            outputUnit.setWeights(newWeights);
            outputUnit.setThreshold(newThreshold);
        }

        // get vector of weights (ordered by the occurrence of features) with the threshold specified at the 0th index.
        private List<Double> getWeights() {
            List<Double> weights = new ArrayList<>();
            weights.add(outputUnit.getThreshold());
            for (int i = 0; i < inputUnits.size(); i++) weights.add(outputUnit.getWeights().get(i));
            return weights;
        }

        // returns a random permutation of the training data that is specific to each epoch (to help in debugging)
        private List<Tuple> getShuffledInputFor(int epoch) {
            List<Tuple> examples = new ArrayList<>(trainTuples.size());
            examples.addAll(trainTuples);
            Collections.shuffle(examples, new Random(epoch));
            return examples;
        }

        // returns the index corresponding to the highest accuracy value in the map provided
        private int findLargest(Map<Integer, Double> accuracies) {
            int maxIndex = -1;
            double maxVal = 0.0;
            for (Map.Entry<Integer, Double> entry : accuracies.entrySet()) {
                if(entry.getValue() > maxVal) {
                    maxVal = entry.getValue();
                    maxIndex = entry.getKey();
                }
            }
//            Assert.that(maxIndex % 50 == 0, "Somehow we've chosen a max (=" + maxIndex + ") that's not valid!");
            return maxIndex;
        }

        private void reportAccuraciesAt(int epoch) {
            System.out.println(String.format("Epoch %s: trainFor = %.2f%% tune = %.2f%% test = %.2f%%", epoch, trainSetAccuracies.get(epoch),
                    tuneSetAccuracies.get(epoch), testSetAccuracies.get(epoch)));
        }

        private void reportWeightsAt(int epoch) {
            List<Double> weights = historyOfWeights.get(epoch);
            List<String> featuresKeys = features.getKeys();
            System.out.println("\nThe corresponding weights and threshold values are\n");
            Double weight, threshold = weights.get(0);
            for(int i = 0; i < featuresKeys.size(); i++) {
                weight = weights.get(i + 1);
                System.out.println(String.format("Wgt = %.2f %s", zeroCheck(weight), featuresKeys.get(i)));
            }
            System.out.println(String.format("Threshold = %.2f", zeroCheck(threshold)));
        }

        // convert negative zeros into regular zero
        private double zeroCheck(Double weight) {
            return Math.abs(weight) < EPSILON ? 0 : weight;
        }

        // returns the accuracy as the percentage of correct predictions in the total predictions
        private double calcAccuracy(List<Tuple> tuples) {
            Tuple tuple;
            int totalCorrect = 0;
            for (int i = 0; i < tuples.size(); i++) {
                tuple = tuples.get(i);
                initializeInputUnitsWithVector(getInputVectorFrom(tuple));
                forwardPropagate();
                if(isPredictionCorrectFor(tuple)) totalCorrect++;
            }
            return (((double) totalCorrect) / tuples.size()) * 100;
        }

        // using the perceptron learning rule defined in eq 18.7 of textbook
        private double perceptronLearningRule(double currentWeight, int categoryOfExample, int perceptronOutput, int featureValue) {
            return currentWeight + (LEARNING_RATE * (categoryOfExample - perceptronOutput) * featureValue);
        }

        // fire the activation function of the perceptron and compare the output with the known category of the training example
        private boolean isPredictionCorrectFor(Tuple example) {
            return categories.get(outputUnit.output()).equals(example.getCategory());
        }

        // collect the signals from all the input units and calculate the weighted sum at the output perceptron
        private void forwardPropagate() {
            List<Double> inVector = new ArrayList<>();
            for (int i = 0; i < inputUnits.size(); i++) inVector.add(inputUnits.get(i).output());
            outputUnit.receiveSignal(inVector);
        }

        // initialize input vector with values from the input vector
        private void initializeInputUnitsWithVector(List<Integer> inputVector) {
//            Assert.that(inputVector.size() == inputUnits.size(), "Mismatch of input units (#" + inputUnits.size() + ") and input vector " + inputVector);
            for (int i = 0; i < inputUnits.size(); i++) {
                inputUnits.get(i).init(inputVector.get(i));
            }
        }

        // converts example tuple into init input vector
        public List<Integer> getInputVectorFrom(Tuple tuple) {
            return features.getKeys().stream().map(key -> parseFeature(key, tuple.valueOf(key))).collect(Collectors.toList());
        }

        // parses binary features into 0s (first value) and 1s (second value)
        private int parseFeature(String featKey, String featVal) {
            List<String> allFeatValues = features.getValuesFor(featKey);
//            Assert.that(allFeatValues.size() == 2, "This ain't right!");
            return featVal.equals(allFeatValues.get(0)) ? 0 : 1;
        }

        private int parseCategory(String str) {
//            Assert.that(categories.size() == 2, "This ain't right!");
            return str.equals(categories.get(0)) ? 0 : 1;
        }

        private void initInputUnits(Feature features) {
            inputUnits = new HashMap<>(features.actualSize());
            for (int i = 0; i < features.actualSize(); i++) {
                inputUnits.put(i, new InputUnit());
            }
        }


    }

    private static class InputUnit {

        private int a;

        public InputUnit() {
            this.a = -1;
        }
        
        public void init(int val) {
            if(val != 1 && val != 0) throw new IllegalStateException("Value of input unit should be 1 or 0");
            else this.a = val;
        }

        public double output() {
            if(a < 0) throw new IllegalStateException("This input unit was never initialized before being used!");
            return a;
        }

    }

    private static class Perceptron {

        private int bias;
        private double in;
        private double threshold;
        private Map<Integer, Double> inWeights;

        public Perceptron(int bias, double threshold, int sizeIn) {
            in = 0.0;
            this.bias = bias;
            this.threshold = threshold;
            initWeights(sizeIn);
        }

        public int getBias() {
            return bias;
        }

        public Double getThreshold() {
            return threshold;
        }

        public Map<Integer, Double> getWeights() {
            return inWeights;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public void setWeights(Map<Integer, Double> weights) {
            this.inWeights = weights;
        }

        // TODO find init better name from the text
        public void receiveSignal(List<Double> inList) {
//            Assert.that(inList.size() == inWeights.size(), "Mismatch receiveSignal input and weights vectors");
            // add the bias to the weighted sum of the input signals
            in = bias * threshold;
            for (int i = 0; i < inList.size(); i++) {
                in += inList.get(i) * inWeights.get(i);
            }
        }

        // TODO verify that activation function is correct
        public int output() {
            return in >= threshold ? 1 : 0;
        }

        public void initWeights(int size) {
            inWeights = new HashMap<>(size);
            for (int i = 0; i < size; i++) { inWeights.put(i,0.0); }
        }

    }

    private static class Feature {

        private final int limit;
        private Set<String> keys = new LinkedHashSet<>();
        private static Map<String, List<String>> map;

        public Feature(int limit) {
            this.limit = limit;
            map = new HashMap<>(limit);
        }

        public void add(String feature, String... values) {
            keys.add(feature);
            map.put(feature, new ArrayList<>(Arrays.asList(values)));
        }

        public int getLimit() {
            return limit;
        }

        public int actualSize() {
            return map.size();
        }

        public List<String> getKeys() {
            return Arrays.asList(keys.toArray(new String[0]));
        }

        public List<String> getValuesFor(String key) {
            return map.get(key);
        }
    }

    private static class Tuple {
        private String name;
        private String category;
        private Map<String, String> features = new HashMap<>();

        public Tuple(String name, String category, Map<String, String> features) {
            this.name = name;
            this.category = category;
            this.features = features;
        }

        public String getCategory() {
            return category;
        }

        public String valueOf(String feature) {
            return features.get(feature);
        }

    }

    /* I/O procedures */

    private static List<Tuple> readTuples(BufferedReader br, List<String> features, int numberOfFeatures) throws IOException {
        String currentLine;
        List<Tuple> tuples = new ArrayList<>();
        while(tuples.size() < numberOfFeatures && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            String[] tupleLine = currentLine.split("\\s+");
            tuples.add(new Tuple(tupleLine[0], tupleLine[1], stitchAsMap(features, Arrays.asList(tupleLine).subList(2, tupleLine.length))));
        }
        return tuples;
    }

    private static int readNumberOfTuples(BufferedReader br) throws IOException {
        String currentLine;
        int numberOfTuples = 0;
        while((currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            else { numberOfTuples = Integer.parseInt(currentLine.trim()); break; }
        }
        return numberOfTuples;
    }

    private static List<String> readCategories(BufferedReader br) throws IOException {
        String currentLine;
        List<String> categories = new ArrayList<>(2);
        while(categories.size() < 2 && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            categories.add(currentLine.trim());
        }
        return categories;
    }

    private static Feature readTuples(BufferedReader br) throws IOException {
        String currentLine;
        Feature feature = null;
        while ((feature == null || feature.actualSize() < feature.getLimit()) && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            if(feature == null) feature = new Feature(Integer.parseInt(currentLine.trim()));
            else {
                String[] featureLine = currentLine.split("-");
                feature.add(featureLine[0].trim(), featureLine[1].trim().split("\\s+"));
            }
        }
        return feature;
    }

    private static void skipFeatures(BufferedReader br) throws IOException {
        String currentLine;
        int current = -1, total = 0;
        while ( (current < total) && ((currentLine = br.readLine()) != null) ) {
            if(shouldSkip(currentLine)) continue;
            if(current < 0) { total = Integer.parseInt(currentLine.trim()); current = 0; }
            else current++;
        }
        return;
    }

    private static void skipCategories(BufferedReader br) throws IOException {
        int current = 0;
        String currentLine;
        while(current < 2 && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            current++;
        }
        return;
    }
    private static boolean shouldSkip(String line) {
        return line.startsWith("//") || line.trim().isEmpty();
    }

    private static Map<String, String> stitchAsMap(List<String> keys, List<String> values) {
        Iterator<String> keysItr = keys.iterator();
        Iterator<String> valuesItr = values.iterator();
        Map<String, String> map = new HashMap<>();
        while(keysItr.hasNext() && valuesItr.hasNext()) map.put(keysItr.next(), valuesItr.next());
        if(keysItr.hasNext() || valuesItr.hasNext()) throw new IllegalStateException("Irregular count of keys and values while stitching");
        return map;
    }

}




