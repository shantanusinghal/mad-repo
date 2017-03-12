package edu.uw.cs.utils;

import edu.uw.cs.model.Attribute;
import edu.uw.cs.model.Dataset;
import edu.uw.cs.model.Instance;

import java.util.*;

/**
 * Created by shantanusinghal on 18/02/17 @ 7:30 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class ProbUtils {

    private Dataset dataset;
    private static final Map<Attribute, String> NO_EVIDENCE = Collections.emptyMap();

    public ProbUtils(Dataset dataset) {
        this.dataset = dataset;
    }

    // self edges are weighted as -1
    public Double calculateCMI(final Attribute a1, final Attribute a2, final Attribute classAttribute) {
        if(a1.equals(a2)) {
            return -1.0;
        } else {
            HashMap<Attribute, String> Y, X1, X2, X1AndX2AndY, X1AndX2;
            double probX1AndX2AndY, probX1AndX2GivenY, probX1GivenY, probX2GivenY, cmi = 0.0;
            for (final String y : classAttribute.getValues()) {
                Y = new HashMap<Attribute, String>() {{ put(classAttribute, y); }};
                for (final String x1 : a1.getValues()) {
                    X1 = new HashMap<Attribute, String>() {{ put(a1, x1); }};
                    for (final String x2 : a2.getValues()) {
                        X2 = new HashMap<Attribute, String>() {{ put(a2, x2); }};
                        X1AndX2 = new HashMap<Attribute, String>(X1) {{ put(a2, x2); }};
                        X1AndX2AndY = new HashMap<Attribute, String>(X1AndX2) {{ put(classAttribute, y); }};

                        probX1GivenY = getProb(X1, Y);
                        probX2GivenY = getProb(X2, Y);
                        probX1AndX2AndY = getProb(X1AndX2AndY);
                        probX1AndX2GivenY = getProb(X1AndX2, Y);

                        cmi += probX1AndX2AndY * lg2(probX1AndX2GivenY / (probX1GivenY * probX2GivenY));
                    }}}
            return cmi;
        }
    }

    /********************** PRIVATE METHODS **********************/

    private double getProb(Map<Attribute, String> query) {
        return getProb(query, NO_EVIDENCE);
    }

    private double getProb(Map<Attribute, String> query, Map<Attribute, String> evidence) {
        double lEstimate = getTotalCombinations(query);
        List<Instance> sampleSpace = filter(getAllInstances(), evidence);
        List<Instance> matchingSamples = filter(sampleSpace, query);

        // using LaPlace estimate of 1 for each attribute combination
        return ( matchingSamples.size() + 1 ) / ( sampleSpace.size() + lEstimate ) ;
    }

    private List<Instance> filter(List<Instance> list, Map<Attribute, String> predicate) {
        List<Instance> filteredList = new ArrayList<Instance>();
        for (Instance instance : list) if (instance.matches(predicate)) filteredList.add(instance);
        return filteredList;
    }

    private double getTotalCombinations(Map<Attribute, String> map) {
        int total = 1;
        for (Attribute attribute : map.keySet()) total *= attribute.getValues().size();
        return total;
    }

    // TODO validate
    private double lg2(double n) {
        if(n <= 0.0) throw new RuntimeException("I wonder how/why this would have happened!?");
        return (Math.log(n) / Math.log(2));
    }

    private List<Instance> getAllInstances() {
        return dataset.getInstances();
    }

}
