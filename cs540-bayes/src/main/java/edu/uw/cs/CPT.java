package edu.uw.cs;

import edu.uw.cs.model.Attribute;
import edu.uw.cs.model.Instance;

import java.util.*;

/**
 * Created by shantanusinghal on 17/02/17 @ 1:38 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
//class AdvancedCPT {
class CPT {

    private Attribute attribute;
    private List<Attribute> parents = new ArrayList<Attribute>();
    private List<Map<String, Double>> probTables = new ArrayList<Map<String, Double>>();
    private Map<Map<Attribute, String>, Integer> tableMapper = new HashMap<Map<Attribute, String>, Integer>();

    CPT(Attribute attribute, List<Node> parents, boolean useLaPlaceEstimates) {
        this.attribute = attribute;
        for (Node parent : parents) this.parents.add(parent.getAttribute());
        init(useLaPlaceEstimates);
    }

    // TODO clean up the two keys thing!
    void populate(List<Instance> instances) {
        Map<Attribute, String> matchedKey; Map<String, Double> probTable;
        List<Map<Attribute, String>> keys = generateKeys(parents, new ArrayList<Map<Attribute, String>>(), new HashMap<Attribute, String>());
        for (Instance instance : instances) {
            for (String value : attribute.getValues()) {
                // if the attribute value matches - find the matching parent values (key) in order to update the right CPT
                if(instance.matches(attribute, value) && (matchedKey = instance.findMatch(keys)) != null) {
                    (probTable = getProbTable(matchedKey)).put(value, probTable.get(value) + 1);
                    break;
                }
            }
        }
        normalizeProbTables();
    }

    // TODO add illegal access exception is conditionals exist
    double getProb(String value) {
        return getProb(value, Collections.<Attribute, String>emptyMap());
    }

    double getProb(String value, Map<Attribute, String> evidence) {
        return getProbTable(evidence).get(value);
    }

    /********************** PRIVATE METHODS **********************/

    private void init(boolean useLaPlaceEstimates) {
        double seed = useLaPlaceEstimates ? 1 : 0; int index = 0;

        probTables.add(index, getFreshTable(seed));

        for (Map<Attribute, String> key : generateKeys(parents, new ArrayList<Map<Attribute, String>>(), new HashMap<Attribute, String>())) {
            if(index == 0) probTables.clear();
            tableMapper.put(key, index++);
            probTables.add(getFreshTable(seed));
        }
    }

    // TODO handle missing key - not sure if it'd happen though
    private Map<String, Double> getProbTable(Map<Attribute, String> key) {
        return probTables.get(tableMapper.isEmpty() ? 0 : tableMapper.get(key));
    }

    private List<Map<Attribute, String>> generateKeys(List<Attribute> attributes, ArrayList<Map<Attribute, String>> accumulator, HashMap<Attribute, String> seed) {
        if(attributes.isEmpty()) return Collections.emptyList();
        final Attribute attribute = attributes.get(0);
        if(attributes.size() == 1) {
            for (final String value : attribute.getValues()) {
                accumulator.add(new HashMap<Attribute, String>(seed) {{
                    put(attribute, value);
                }});
            }} else {
            for (final String value : attribute.getValues()) {
                generateKeys(attributes.subList(1, attributes.size()), accumulator, new HashMap<Attribute, String>(seed) {{
                    put(attribute, value);
                }});
            }}
        return accumulator;
    }

    private void normalizeProbTables() {
        double sum;
        for (Map<String, Double> probTable : probTables) {
            sum = 0; for (Double val : probTable.values()) sum += val;
            for (String key : probTable.keySet()) probTable.put(key, probTable.get(key) / sum);
        }
    }

    private Map<String, Double> getFreshTable(double seed) {
        Map<String, Double> valueMap = new HashMap<String, Double>();
        for (final String value : attribute.getValues()) valueMap.put(value, seed);
        return valueMap;
    }

}
