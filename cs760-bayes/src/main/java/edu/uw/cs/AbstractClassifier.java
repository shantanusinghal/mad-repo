package edu.uw.cs;

import edu.uw.cs.model.Attribute;
import edu.uw.cs.model.Dataset;
import edu.uw.cs.model.Instance;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shantanusinghal on 17/02/17 @ 8:31 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
abstract class AbstractClassifier implements Classifier {

    private Node root;

    AbstractClassifier(Node root) {
        this.root = root;
    }

    public String getStructureInfo() {
        StringBuilder sb = new StringBuilder();
        for (Node child : root.getChildren()) sb.append(child);
        return sb.toString();
    }

    public Predictions predict(final Dataset testData) {
        Node attributeNode;
        String attributeValue;
        double likelihood, priorProb;
        Map<Attribute, String> parentValues;
        Map<String, Double> postProbTable;
        Predictions predictions = new Predictions();

        // iterate over each instance
        for (Instance instance : testData.getInstances()) {

            postProbTable = new HashMap<String, Double>();
            // for each value of the class label
            for (final String classValue : testData.getClassAttribute().getValues()) {

                likelihood = 1.0;
                // for each attribute
                for (Attribute attribute : testData.getAttributes()) {
                    attributeNode = getRoot().find(attribute);
                    attributeValue = instance.getValue(attribute);
                    parentValues = getParentValues(instance, attributeNode, classValue);

                    // multiply-up the probability of the attribute value given the fixed class label and the observed values of any remaining parents
                    likelihood *= attributeNode.getProb(attributeValue, parentValues);
                }

                // take the prior probability of this class label
                priorProb = getRoot().getProb(classValue);

                // and calculate the posterior probability of this class label
                postProbTable.put(classValue, priorProb * likelihood);
            }

            Map.Entry<String, Double> prediction = pickMaxEntry(normalize(postProbTable));
            predictions.addPrediction(instance.getClassLabel(), prediction.getKey(), prediction.getValue());
        }

        return predictions;
    }

    /********************** PROTECTED METHODS **********************/

    Node getRoot() {
        return root;
    }

    void buildTree(Node root, List<Attribute> childAttributes) {
        buildTree(root, childAttributes, Collections.<Attribute, Attribute>emptyMap());
    }

    void buildTree(Node root, List<Attribute> childAttributes, Map<Attribute, Attribute> parentMap) {
        // build naive net by adding all attributes as child nodes to the class root
        for (Attribute attribute : childAttributes) {
            root.addChild(new Node(attribute, root));
        }
        // augment the naive net with additional edges if they exist
        Node childNode, parentNode;
        for (Map.Entry<Attribute, Attribute> parentChildEntry : parentMap.entrySet()) {
            childNode = root.find(parentChildEntry.getKey());
            parentNode = root.find(parentChildEntry.getValue());
            childNode.addParent(parentNode);
        }
    }

    /********************** PRIVATE METHODS **********************/

    private Map<Attribute, String> getParentValues(Instance instance, Node attributeNode, String classValue) {
        Map<Attribute, String> parentValuesMap = new HashMap<Attribute, String>();
        for (Node node : attributeNode.getParents()) {
            Attribute attribute = node.getAttribute();
            parentValuesMap.put(attribute, attribute.isClass() ? classValue : instance.getValue(attribute));
        }
        return parentValuesMap;
    }

    private Map<String, Double> normalize(Map<String, Double> table) {
        double sum = 0;
        Map<String, Double> normTable = new HashMap<String, Double>();
        for (Double val : table.values()) sum += val;
        for (String key : table.keySet()) normTable.put(key, table.get(key) / sum);
        return normTable;
    }

    // TODO fix for self edge weights of -1
    private Map.Entry<String, Double> pickMaxEntry(Map<String, Double> probTable) {
        double max = 0.0; Map.Entry<String, Double> maxEntry = null;
        for (Map.Entry<String, Double> entry : probTable.entrySet()) {
            if(entry.getValue() > max) {
                max = entry.getValue(); maxEntry = entry;
            }}
        return maxEntry;
    }

}
