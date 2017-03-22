import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Created by shantanusinghal on 14/09/16 @ 9:57 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class BuildAndTestDecisionTree {

    public static final int FIRST = 0;
    public static final int SECOND = 1;
    private final static double EPSILON = 0.00001;
    private final static String DEFAULT_CATEGORY = "negative";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) {

        Attribute attributes = null;
        List<Feature> trainingFeatures = null, testingFeatures = null;
        List<String> categories = null;

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {

            /* Read the feature count and parse the features into the data model */
            attributes = readAttributes(br);

            /* Read the two class labels */
            categories = readCategories(br);

            /* Parse the specified number of examples into the data model */
            trainingFeatures = readFeatures(br, attributes.getKeys(), readNumberOfFeatures(br));

        } catch (IOException e) {
            e.printStackTrace();
        }

        DecisionTree decisionTree = new DecisionTree(trainingFeatures, attributes, categories).build(DEFAULT_CATEGORY);

        decisionTree.prettyPrint();

        try (BufferedReader br = new BufferedReader(new FileReader(args[1]))) {
            skipAttributes(br);
            skipCategories(br);
            testingFeatures = readFeatures(br, attributes.getKeys(), readNumberOfFeatures(br));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("\n\nTesting set classification based on the decision tree:\n");
        decisionTree.predict(testingFeatures);

    }

    private static class Attribute {

        private final int limit;
        private Set<String> keys = new LinkedHashSet<>();
        private static Map<String, List<String>> valuesMap;

        public Attribute(int limit) {
            this.limit = limit;
            valuesMap = new HashMap<>(limit);
        }

        public void add(String attribute, String... values) {
            keys.add(attribute);
            valuesMap.put(attribute, new ArrayList<>(Arrays.asList(values)));
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

        public List<String> getValuesFor(String splitAttr) {
            return valuesMap.get(splitAttr);
        }
    }

    private static class Feature {
        private String name;
        private String category;
        private Map<String, String> attributes = new HashMap<>();

        public Feature(String name, String category, Map<String, String> attributes) {
            this.name = name;
            this.category = category;
            this.attributes = attributes;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String valueOf(String attribute) {
            return attributes.get(attribute);
        }

    }

    private static class DecisionTree {
        private Node root;
        private Attribute attributes;
        private List<Feature> features;
        private String defaultCategory;
        private List<String> categories;

        public DecisionTree(List<Feature> features, Attribute attributes, List<String> categories) {
            this.features = features;
            this.attributes = attributes;
            this.categories = categories;
        }

        public DecisionTree build(String defaultCategory) {
            this.defaultCategory = defaultCategory;
            root = new ID3(defaultCategory, attributes, categories).process(new Node(this.attributes.getKeys(), this.features));
            return this;
        }

        public void predict(List<Feature> testingFeatures) {
            boolean incorrect;
            int incorrectCount = 0;
            String predictedCat, colorCode;
            for (Feature feature : testingFeatures) {
                predictedCat = predict(feature);
                incorrect = !predictedCat.equals(feature.getCategory());
                colorCode = incorrect ? ANSI_RED : ANSI_GREEN;
                if(incorrect) incorrectCount ++;
                System.out.println(colorCode + String.format("[%s] Feature (%s) : Predicted Category -> %s",
                        incorrect ? "INCORRECT" : "CORRECT", feature.name, predictedCat) + ANSI_RESET);
            }
            int total = testingFeatures.size();
            System.out.println(String.format("\n%d out of %d predictions were incorrect." +
                    "\nAccuracy of the decision tree is: %.2f%%", incorrectCount, total, ((double)(total - incorrectCount)/(double)total) * 100));
        }

        public void prettyPrint() {
            print(root, "", "", false);
        }

        private String predict(Feature feature) {
            Node node = root;
            while (!node.hasCategory()) node = node.getChildFor(feature.valueOf(node.getSplitAttribute()));
            return node.getCategory();
        }

        private void print(Node node, String branchLabel, String prefix, boolean isTail) {
            Map<String, Node> children = node.getChildren();
            ArrayList<String> attributes = new ArrayList<String>() {{ addAll(node.getChildren().keySet()); }};
            /* print the current node */
            System.out.println(prefix + (isTail ? "└── " : "├── ") + node.getLabel(branchLabel));
            /* recursively print (n-1) children */
            for(int i = 0; i < attributes.size() - 1; i++) {
                print(children.get(attributes.get(i)), attributes.get(i).toString(), prefix + (isTail ? "    " : "│   "), false);
            }
            /* recursively print the last child */
            if (children.size() > 0) {
                String lastChildAttr = attributes.get(attributes.size() - 1);
                print(children.get(lastChildAttr), lastChildAttr.toString(), prefix + (isTail ?"    " : "│   "), true);
            }
        }
    }

    private static class Node {
        private Node parent;
        private String category;
        private String splitAttribute;
        private Map<String, Node> children;

        private List<Feature> features;
        private List<String> attributes;

        public Node(List<String> attributes, List<Feature> features) {
            this.features = features;
            this.attributes = attributes;
            this.children = new HashMap<>();
        }

        public Node(Node parent, List<String> attributes, List<Feature> features) {
            this(attributes, features);
            this.parent = parent;
        }

        public boolean hasCategory() {
            return category != null;
        }

        public List<Feature> getFeatures() {
            return features;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public String getSplitAttribute() {
            return splitAttribute;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void addChild(String attribute, Node node) {
            children.put(attribute, node);
        }

        public void setSplitAttribute(String splitAttribute) {
            this.splitAttribute = splitAttribute;
        }

        public Node getChildFor(String attribute) {
            return children.get(attribute);
        }

        public Map<String,Node> getChildren() {
            return children;
        }

        public String getLabel(String attributeValue) {
            return parent == null ? "ROOT" : (String.format("%s = %s", parent.getSplitAttribute(), attributeValue))
                    + (hasCategory() ? String.format(" : %s (%d)", getCategory().toString().toLowerCase(), getFeatures().size()) : "");
        }

    }

    private static class ID3 {

        private List<String> categories;
        private final Attribute attributes;
        private final String defaultCategory;

        public ID3(String defaultCategory, Attribute attributes, List<String> categories) {
            this.defaultCategory = defaultCategory;
            this.attributes = attributes;
            this.categories = categories;
        }

        private Node process(Node node) {
            if (thereAreNoFeaturesIn(node)) {
                node.setCategory(defaultCategory);
                return node;
            } else if (allFeaturesHaveSameCategoryIn(node)) {
                node.setCategory(node.getFeatures().get(0).getCategory());
                return node;
            } else if (thereAreNoAttributesIn(node)) {
                node.setCategory(defaultCategory);
                return node;
            } else {
                String splitAttr = pickSplitAttributeFor(node);
                node.setSplitAttribute(splitAttr);
                List<String> remainingAttrs = node.getAttributes().stream().filter(f -> !f.equals(splitAttr)).collect(Collectors.toList());
                for (String attrVal : attributes.getValuesFor(splitAttr)) {
                    node.addChild(attrVal, process(new Node(node, remainingAttrs, getQualifyingFeatures(node, attrVal))));
                }
            }
            return node;
        }

        private List<Feature> getQualifyingFeatures(Node node, Object attrValue) {
            return node.getFeatures().stream().filter(f -> f.valueOf(node.getSplitAttribute()).equals(attrValue)).collect(Collectors.toList());
        }

        private boolean thereAreNoAttributesIn(Node node) {
            return node.getAttributes().size() == 0;
        }

        private boolean thereAreNoFeaturesIn(Node node) {
            return node.getFeatures().size() == 0;
        }

        private boolean allFeaturesHaveSameCategoryIn(Node node) {
            return node.getFeatures().stream().collect(toMap(Feature::getCategory, Function.identity(), (f1, f2) -> f1)).keySet().size() == 1;
        }

        private String pickSplitAttributeFor(Node node) {
            String splittingAttr = null;
            double minInfoRem = Double.MAX_VALUE, infoRem;
            for (String attr : node.getAttributes()) {
                if((infoRem = infoRemaining(node.getFeatures(), attr)) == 0) return attr;
                else if (infoRem < minInfoRem ) { minInfoRem = infoRem; splittingAttr = attr; }
            }
            return splittingAttr;
        }

        private double infoRemaining(List<Feature> features, String splitAttr) {
            double infoRemaining = 0.0, totalSize = features.size(), fracSize, firstFracSize, secondFracSize;
            for (String attrVal : attributes.getValuesFor(splitAttr)) {
                List<Feature> filteredFeatures = features.stream()
                        .filter(f -> f.valueOf(splitAttr).equals(attrVal))
                        .collect(Collectors.toList());
                if((fracSize = filteredFeatures.size()) == 0) continue;
                firstFracSize = filteredFeatures.stream().filter(e -> e.getCategory().equals(categories.get(FIRST))).count();
                secondFracSize = filteredFeatures.stream().filter(e -> e.getCategory().equals(categories.get(SECOND))).count();
                if(firstFracSize + secondFracSize != fracSize)
                    throw new IllegalStateException("Encountered corrupt data while processing attribute " + splitAttr);
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

    private static List<Feature> readFeatures(BufferedReader br, List<String> attributes, int numberOfFeatures) throws IOException {
        String currentLine;
        List<Feature> features = new ArrayList<>();
        while(features.size() < numberOfFeatures && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            String[] exampleLine = currentLine.split("\\s+");
            features.add(new Feature(exampleLine[0], exampleLine[1], stitchAsMap(attributes, Arrays.asList(exampleLine).subList(2, exampleLine.length))));
        }
        return features;
    }

    private static int readNumberOfFeatures(BufferedReader br) throws IOException {
        String currentLine;
        int numberOfFeatures = 0;
        while((currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            else { numberOfFeatures = Integer.parseInt(currentLine.trim()); break; }
        }
        return numberOfFeatures;
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

    private static Attribute readAttributes(BufferedReader br) throws IOException {
        String currentLine;
        Attribute attributes = null;
        while ((attributes == null || attributes.actualSize() < attributes.getLimit()) && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            if(attributes == null) attributes = new Attribute(Integer.parseInt(currentLine.trim()));
            else {
                String[] attributeLine = currentLine.split("-");
                attributes.add(attributeLine[0].trim(), attributeLine[1].trim().split("\\s+"));
            }
        }
        return attributes;
    }

    private static void skipAttributes(BufferedReader br) throws IOException {
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




