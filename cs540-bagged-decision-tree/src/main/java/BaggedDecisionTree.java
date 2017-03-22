import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by shantanusinghal on 14/09/16 @ 9:57 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
@SuppressWarnings("Duplicates")
public class BaggedDecisionTree {

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int LEAF_NODES = 0;
    private static final int CURRENT_DEPTH = 1;
    private static final int MAX_DEPTH = 2;
    private static final int DEPTH_LIMIT = 4;
    private final static double EPSILON = 0.00001;

    public static void main(String[] args) {
        System.out.println(maxProduct(new double[]{2,3,0.5,0.5,-10,-10}));
    }

    public static double maxProduct(double[] nums) {
        double[] max = new double[nums.length];
        double[] min = new double[nums.length];

        max[0] = min[0] = nums[0];
        double result = nums[0];

        for(int i=1; i<nums.length; i++){
            if(nums[i]>0){
                max[i]=Math.max(nums[i], max[i-1]*nums[i]);
                min[i]=Math.min(nums[i], min[i-1]*nums[i]);
            }else{
                max[i]=Math.max(nums[i], min[i-1]*nums[i]);
                min[i]=Math.min(nums[i], max[i-1]*nums[i]);
            }

            result = Math.max(result, max[i]);
        }

        return result;
    }

    public static void main1(String[] args) {

        new ID3(null, null);
        Feature features;
        DecisionTree decisionTree;
        List<String> categories = null;
        List<Tuple> trainTuples, testTuples, tuneTuples;
        String[][] lTunePredictions = new String[51][298];
        String[][] lTestPredictions = new String[51][298];
        String[][] tunePredictions = new String[101][298];
        String[][] testPredictions = new String[101][298];
        Map<Integer, Double> lTuneAccuracy = new HashMap<>();
        Map<Integer, Double> lTestAccuracy = new HashMap<>();

        /* read the tuning and testing data-sets */
        try (BufferedReader br = new BufferedReader(new FileReader(args[1]));
             BufferedReader br2 = new BufferedReader(new FileReader(args[2]))) {
            features = readTuples(br); skipFeatures(br2);
            skipCategories(br); skipCategories(br2);
            tuneTuples = readTuples(br, features.getKeys(), readNumberOfTuples(br));
            testTuples = readTuples(br2, features.getKeys(), readNumberOfTuples(br2));
        } catch (Exception e) {
            System.out.println("There was an error while reading the train/test data!! Aborting operation ...");
            return;
        }

        /* for each training-set variant */
        for(int i = 1; i <= 101; i++) {
            /* read all tuple values */
            try (BufferedReader br = new BufferedReader(new FileReader(args[0] + "wine-b-1.data"))) {
                features = readTuples(br);
                categories = readCategories(br);
                trainTuples = readTuples(br, features.getKeys(), readNumberOfTuples(br));
            } catch (Exception e) {
                System.out.println("There was an error while reading the training data from wine-b-" + i + ".data; This record has been skipped");
                continue;
            }

            /* build DT using this bootstrapped sample */
            decisionTree = new DecisionTree(features, categories).buildWith(trainTuples);

            /* record predictions of DT for all tune and test sets */
            for(int j = 0, k = 0; j < tuneTuples.size() || k < testTuples.size(); j++, k++) {
                if(j < tuneTuples.size()) tunePredictions[i - 1][j] = decisionTree.predict(tuneTuples.get(j));
                if(k < testTuples.size()) testPredictions[i - 1][k] = decisionTree.predict(testTuples.get(k));
        }}

        /* for all tune and test tuples calculate the frequency of each predictions across all DTs */
        List<Map<String, Integer>> tuneFreq = getCategoryFrequenciesOfIn(categories, tunePredictions);
        List<Map<String, Integer>> testFreq = getCategoryFrequenciesOfIn(categories, testPredictions);

        /* identify two categories for threshold calculations */
        String majorityLCategory = String.valueOf(categories.stream().sorted().toArray()[0]);
        String minorityLCategory = String.valueOf(categories.stream().sorted().toArray()[1]);

        /* build threshold based predictions for tune and test sets */
        for (int i = 0, l = 1; l <= 101; i++, l += 2) {
            for (int j = 0; j < 298; j++) {
                lTunePredictions[i][j] = tuneFreq.get(j).get(majorityLCategory) >= l ? majorityLCategory : minorityLCategory;
                lTestPredictions[i][j] = testFreq.get(j).get(majorityLCategory) >= l ? majorityLCategory : minorityLCategory;
            }
        }

        /* calculate frequencies of threshold based predictions */
        int countTune, countTest;
        for (int i = 0, l = 1; i <= 50; i++, l+=2) {
            countTune = countTest = 0;
            for (int j = 0; j < 298; j++) {
                if(tuneTuples.get(j).getCategory().equals(lTunePredictions[i][j])) countTune++;
                if(testTuples.get(j).getCategory().equals(lTestPredictions[i][j])) countTest++;
            }
            lTuneAccuracy.put(l, ((countTune * 100.0) / 298.0));
            lTestAccuracy.put(l, ((countTest * 100.0) / 298.0));
        }

//        countTune = countTest = 0;
//        for (int j = 0; j < 298; j++) {
//            if(tuneTuples.get(j).getCategory().equals(tunePredictions[0][j])) countTune++;
//            if(testTuples.get(j).getCategory().equals(testPredictions[0][j])) countTest++;
//        }
//        lTuneAccuracy.put(0, ((countTune * 100.0) / 298.0));
//        lTestAccuracy.put(0, ((countTest * 100.0) / 298.0));

        System.out.println("\n////// TUNE ///////\n");
        lTuneAccuracy.forEach((k, v) -> System.out.println(String.format("%d\t\t%.2f",k,v)));
        System.out.println("\n////// TEST ///////\n");
        lTestAccuracy.forEach((k, v) -> System.out.println(String.format("%d\t\t%.2f",k,v)));

    }

    private static List<Map<String, Integer>> getCategoryFrequenciesOfIn(List<String> categories, String[][] predictions) {
        String[] columnCut;
        List<Map<String, Integer>> frequencies = new ArrayList<>();
        for(int j = 0; j < 298; j++) {
            columnCut = new String[101];
            for (int i = 0; i < 101; i++) {
                columnCut[i] = predictions[i][j];
            }
            Map<String, Integer> map = Arrays.asList(columnCut).stream().collect(groupingBy(Function.identity(), summingInt(e -> 1)));
            categories.stream().filter(category -> !map.containsKey(category)).forEach(category -> map.put(category, 0));
            frequencies.add(map);
        }
        return frequencies;
    }


    private static class DecisionTree {
        private Node root;
        private Feature features;
        private List<String> categories;

        public DecisionTree(Feature features, List<String> categories) {
            root = Node.NULL;
            this.features = features;
            this.categories = categories;
        }

        public DecisionTree buildWith(List<Tuple> tuples) {
            root = new ID3(features, categories).process(new Node(this.features.getKeys(), tuples), 0);
            return this;
        }

        public DecisionTree buildWith(List<Tuple> tuples, int maxDepth) {
            root = new ID3(features, categories, maxDepth).process(new Node(this.features.getKeys(), tuples), 0);
            return this;
        }

        public String predict(Tuple tuple) {
            Node node = root;
            while (node != null && !node.hasCategory()) node = node.getChildFor(tuple.valueOf(node.getSplittingFeature()));
            return node == null ? "??" : node.getCategory();
        }

    }

    private static class Feature {

        private final int limit;
        private Set<String> keys = new LinkedHashSet<>();
        private static Map<String, List<String>> valuesMap;

        public Feature(int limit) {
            this.limit = limit;
            valuesMap = new HashMap<>(limit);
        }

        public void add(String feature, String... values) {
            keys.add(feature);
            valuesMap.put(feature, new ArrayList<>(Arrays.asList(values)));
        }

        public int getLimit() {
            return limit;
        }

        public int actualSize() {
            return valuesMap.size();
        }

        public List<String> getKeys() {
            return Arrays.asList(keys.toArray(new String[0]));
        }

        public List<String> getValuesFor(String splitFeat) {
            return valuesMap.get(splitFeat);
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

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String valueOf(String feature) {
            return features.get(feature);
        }

    }

    private static class Node {
        private Node parent;
        private String category;
        private String splittingFeature;
        private Map<String, Node> children;

        private List<Tuple> tuples;
        private List<String> features;

        public static final Node NULL = new Node(Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        public Node(List<String> features, List<Tuple> tuples) {
            this.parent = Node.NULL;
            this.tuples = tuples;
            this.features = features;
            this.features.sort(Comparator.naturalOrder());
            this.children = new HashMap<>();
        }

        public Node(Node parent, List<String> features, List<Tuple> tuples) {
            this(features, tuples);
            this.parent = parent;
        }

        public boolean hasCategory() {
            return category != null;
        }

        public List<Tuple> getTuples() {
            return tuples;
        }

        public List<String> getFeatures() {
            return features;
        }

        public String getSplittingFeature() {
            return splittingFeature;
        }

        public String getCategory() {
            return category;
        }

        public Node getParent() {
            return parent;
        }

        public int getInteriorNodesInSubtree() {
            return !hasCategory() ? children.values().stream().mapToInt(Node::getInteriorNodesInSubtree).sum() + 1 : 0;
        }

        public int getLeafNodesInSubtree() {
            return !hasCategory() ? children.values().stream().mapToInt(Node::getLeafNodesInSubtree).sum() : 1;
        }

        public String getMajorityCategory() {
            return getTuples().stream()
                    .collect(groupingBy(e -> e.getCategory(), counting()))
                    .entrySet()
                    .stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .get().getKey();
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void addChild(String feature, Node node) {
            children.put(feature, node);
        }

        public void setSplittingFeature(String splittingFeature) {
            this.splittingFeature = splittingFeature;
        }

        public Node getChildFor(String feature) {
            return children.get(feature);
        }

        public Map<String,Node> getChildren() {
            return children;
        }

        public String getLabel(String featureValue) {
            return Node.NULL.equals(parent) ? "ROOT" : (String.format("%s = %s", parent.getSplittingFeature(), featureValue))
                    + (hasCategory() ? String.format(" : %s (%d)", getCategory().toString().toLowerCase(), getTuples().size()) : "");
        }
    }

    private static class ID3 {

        private final Feature features;
        private List<String> categories;
        private int maxDepth;
        private static boolean DEPTH_IS_DEFINED = false;

        public ID3(Feature features, List<String> categories) {
            this.features = features;
            this.categories = categories;
        }

        public ID3(Feature features, List<String> categories, int maxDepth) {
            this(features, categories);
            this.maxDepth = maxDepth;
            this.DEPTH_IS_DEFINED = true;
        }

        private Node process(Node node, int depth) {
            if(DEPTH_IS_DEFINED && depth >= maxDepth) {
                node.setCategory(node.getMajorityCategory());
                return node;
            }
            else if (thereAreNoMoreTuplesIn(node)) {
                node.setCategory(node.getParent().getMajorityCategory());
                return node;
            } else if (allTuplesHaveSameCategoryIn(node)) {
                node.setCategory(node.getTuples().get(0).getCategory());
                return node;
            } else if(thereAreNoFeaturesIn(node)) {
                node.setCategory(node.getMajorityCategory());
                return node;
            } else {
                String splitFeat = pickSplittingFeatureFor(node);
                node.setSplittingFeature(splitFeat);
                List<String> remainingFeats = node.getFeatures().stream().filter(f -> !f.equals(splitFeat)).collect(Collectors.toList());
                for (String featVal : features.getValuesFor(splitFeat)) {
                    node.addChild(featVal, process(new Node(node, remainingFeats, getQualifyingFeatures(node, featVal)), depth + 1));
                }
            }
            return node;
        }

        private List<Tuple> getQualifyingFeatures(Node node, Object featValue) {
            return node.getTuples().stream().filter(f -> f.valueOf(node.getSplittingFeature()).equals(featValue)).collect(Collectors.toList());
        }

        private boolean thereAreNoFeaturesIn(Node node) {
            return node.getFeatures().size() == 0;
        }

        private boolean thereAreNoMoreTuplesIn(Node node) {
            return node.getTuples().size() == 0;
        }

        private boolean allTuplesHaveSameCategoryIn(Node node) {
            return node.getTuples().stream().collect(toMap(Tuple::getCategory, Function.identity(), (f1, f2) -> f1)).keySet().size() == 1;
        }

        private String pickSplittingFeatureFor(Node node) {
            String splittingFeat = null;
            double minInfoRem = Double.MAX_VALUE, infoRem;
            for (String feat : node.getFeatures()) {
                if((infoRem = infoRemaining(node.getTuples(), feat)) == 0) return feat;
                else if (infoRem < minInfoRem ) { minInfoRem = infoRem; splittingFeat = feat; }
            }
            return splittingFeat;
        }

        public double infoRemaining(List<Tuple> tuples, String splitFeat) {
            double infoRemaining = 0.0, totalSize = tuples.size(), fracSize, firstFracSize, secondFracSize;
            for (String featVal : features.getValuesFor(splitFeat)) {
                List<Tuple> filteredTuples = tuples.stream()
                        .filter(f -> f.valueOf(splitFeat).equals(featVal))
                        .collect(Collectors.toList());
                if((fracSize = filteredTuples.size()) == 0) continue;
                firstFracSize = filteredTuples.stream().filter(e -> e.getCategory().equals(categories.get(FIRST))).count();
                secondFracSize = filteredTuples.stream().filter(e -> e.getCategory().equals(categories.get(SECOND))).count();
                if(firstFracSize + secondFracSize != fracSize)
                    throw new IllegalStateException("Encountered corrupt data while processing feature " + splitFeat);
                infoRemaining += (fracSize/totalSize) * infoNeeded((firstFracSize/fracSize), (secondFracSize/fracSize));
            }
            return infoRemaining;
        }

        private double infoNeeded(double firstFrac, double secondFrac) {
            if(Math.abs(1.0 - (firstFrac + secondFrac)) > EPSILON) throw new IllegalArgumentException();
            else return (firstFrac == 0 || secondFrac == 0) ? 0 : ( -1 * firstFrac * lg2(firstFrac) ) + ( -1 * secondFrac * lg2(secondFrac) );
        }

        private double lg2(double n) {
            return (Math.log(n) / Math.log(2));
        }

    }

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




