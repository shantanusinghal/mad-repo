import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by shantanusinghal on 13/09/16 @ 2:40 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class HW0 {

    public static final int FIRST = 0;
    public static final int SECOND = 1;

    public static void main(String[] args) {

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {

            /* Read the feature count and parse the features into the data model */
            Features features = readFeatures(br);

            /* Read the two class labels */
            List<String> classLabels = readClassLabels(br);

            /* Read the example data-set length */
            int numberOfExamples = readNumberOfExamples(br);

            /* Parse the specified number of examples into the data model */
            List<Example> examples = readExamples(br, features, numberOfExamples);

            /* Display high level statistics about the example data */
            double firstClassLabelCount, secondClassLabelCount = 0;

            System.out.println(String.format("There are %d features in the dataset.", features.actualSize()));
            System.out.println(String.format("There are %d examples.", numberOfExamples));
            System.out.println(String.format("%.0f have output label \'%s\', %.0f have output label '%s'.",
                (firstClassLabelCount = examples.stream().filter(e -> e.getClassLabel().equals(classLabels.get(FIRST))).count()), classLabels.get(FIRST),
                (secondClassLabelCount = examples.stream().filter(e -> e.getClassLabel().equals(classLabels.get(SECOND))).count()), classLabels.get(SECOND)));
            for(String attribute: features.getAttributes()) {
                System.out.println(String.format("Feature \'%s\':\n" +
                        "\tIn examples labeled \'%s\', %.2f%% were \'%s\' and %.2f%% were \'%s\'\n" +
                        "\tIn examples labeled \'%s\', %.2f%% were \'%s\' and %.2f%% were \'%s\'",
                    attribute,
                    classLabels.get(FIRST),
                    examples.stream()
                        .filter(e -> e.getClassLabel().equals(classLabels.get(FIRST)))
                        .filter(e -> e.getFeatures().get(attribute).equals(features.getFirstValueFor(attribute)))
                        .count() * 100 / firstClassLabelCount,
                    features.getFirstValueFor(attribute),
                    examples.stream()
                        .filter(e -> e.getClassLabel().equals(classLabels.get(FIRST)))
                        .filter(e -> e.getFeatures().get(attribute).equals(features.getSecondValueFor(attribute)))
                        .count() * 100 / firstClassLabelCount,
                    features.getSecondValueFor(attribute),
                    classLabels.get(SECOND),
                    examples.stream()
                        .filter(e -> e.getClassLabel().equals(classLabels.get(SECOND)))
                        .filter(e -> e.getFeatures().get(attribute).equals(features.getFirstValueFor(attribute)))
                        .count() * 100 / secondClassLabelCount,
                    features.getFirstValueFor(attribute),
                    examples.stream()
                        .filter(e -> e.getClassLabel().equals(classLabels.get(SECOND)))
                        .filter(e -> e.getFeatures().get(attribute).equals(features.getSecondValueFor(attribute)))
                        .count() * 100 / secondClassLabelCount,
                    features.getSecondValueFor(attribute)));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static List<Example> readExamples(BufferedReader br, Features features, int numberOfExamples) throws IOException {
        String currentLine;
        List<Example> examples = new ArrayList<>();
        while(examples.size() < numberOfExamples && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            String[] exampleLine = currentLine.split("\\s+");
            examples.add(new Example(exampleLine[0], exampleLine[1], stitchAsMap(features.getAttributes(), Arrays.asList(exampleLine).subList(2, exampleLine.length))));
        }
        return examples;
    }

    private static int readNumberOfExamples(BufferedReader br) throws IOException {
        String currentLine;
        int numberOfExamples = 0;
        while((currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            else { numberOfExamples = Integer.parseInt(currentLine.trim()); break; }
        }
        return numberOfExamples;
    }

    private static List<String> readClassLabels(BufferedReader br) throws IOException {
        String currentLine;
        List<String> classLabels = new ArrayList<>(2);
        while(classLabels.size() < 2 && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            classLabels.add(currentLine.trim());
        }
        return classLabels;
    }

    private static Features readFeatures(BufferedReader br) throws IOException {
        String currentLine;
        Features features = null;
        while ((features == null || features.actualSize() < features.getLimit()) && (currentLine = br.readLine()) != null) {
            if(shouldSkip(currentLine)) continue;
            if(features == null) features = new Features(Integer.parseInt(currentLine.trim()));
            else {
                String[] featureLine = currentLine.split("-");
                features.add(featureLine[0].trim(), featureLine[1].trim().split("\\s+"));
            }
        }
        return features;
    }

    private static boolean shouldSkip(String line) {
        return line.startsWith("//") || line.trim().isEmpty();
    }

    private static Map<String, String> stitchAsMap(Set<String> keys, List<String> values) {
        Iterator<String> keysItr = keys.iterator();
        Iterator<String> valuesItr = values.iterator();
        Map<String, String> map = new HashMap<>();
        while(keysItr.hasNext() && valuesItr.hasNext()) map.put(keysItr.next(), valuesItr.next());
        if(keysItr.hasNext() || valuesItr.hasNext()) System.out.println("something went wrong here!!");
        return map;
    }

    private static class Example {
        private String name;
        private String classLabel;
        private Map<String, String> features = new HashMap<>();

        public Example(String name, String classLabel, Map<String, String> features) {
            this.name = name;
            this.classLabel = classLabel;
            this.features = features;
        }

        public String getClassLabel() {
            return classLabel;
        }

        public Map<String, String> getFeatures() {
            return features;
        }
    }

    private static class Features {

        private final int limit;
        private Set<String> attributes = new LinkedHashSet<>(); // contains (ins) ordered set of attributes
        private static Map<String, List<String>> featureMap; // maps feature attributes to it's two possible values

        public Features(int limit) {
            this.limit = limit;
            featureMap = new HashMap<>(limit);
        }

        public void add(String attribute, String... values) {
            attributes.add(attribute);
            featureMap.put(attribute, new ArrayList<>(Arrays.asList(values)));
        }

        public int getLimit() {
            return limit;
        }

        public int actualSize() {
            return featureMap.size();
        }

        public Set<String> getAttributes() {
            return attributes;
        }

        public String getFirstValueFor(String name) {
            return featureMap.get(name).get(0);
        }

        public String getSecondValueFor(String name) {
            return featureMap.get(name).get(1);
        }
    }

}
