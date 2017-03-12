package edu.uw.cs.utils;

import edu.uw.cs.model.Attribute;

import java.util.*;

/**
 * Created by shantanusinghal on 18/02/17 @ 8:11 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class GraphUtils {

    // returns list of directed edges (to, from)
    public static Map<Attribute, Attribute> buildMST(List<Attribute> nodes, Map<Attribute.Pair, Double> edgeWeights) {
        Set<Attribute> reachedSet = new LinkedHashSet<Attribute>();
        Set<Attribute> unreachedSet = new LinkedHashSet<Attribute>(nodes);
        Map<Attribute, Attribute> mstSet = new HashMap<Attribute, Attribute>();

        // pick the first attribute to root the mst
        Attribute seed = nodes.get(0); // not that picking a random(0, nodes.size()) node gives better results
        reachedSet.add(seed);
        unreachedSet.remove(seed);

        // until all vertices aren't reached keep picking the maximum weighted edge
        Attribute.Pair maxEdge;
        while(!unreachedSet.isEmpty()) {
            maxEdge = findMaxEdgeFromTo(reachedSet, unreachedSet, edgeWeights);
            mstSet.put(maxEdge.getTo(), maxEdge.getFrom());
            reachedSet.add(maxEdge.getTo());
            unreachedSet.remove(maxEdge.getTo());
        }

        return mstSet;
    }

    @SuppressWarnings("unused")
    private static int random(int min, int max) {
        return new Random().nextInt(max - min) + min;
    }

    // TODO what if there is no edge from reached to unreached!
    private static Attribute.Pair findMaxEdgeFromTo(Set<Attribute> reachedSet, Set<Attribute> unreachedSet, Map<Attribute.Pair, Double> edgeWeights) {
        double w, maxWeight = -2.0; Attribute.Pair pair, maxPair = null;
        for (Attribute from : reachedSet) {
            for (Attribute to : unreachedSet) {
                pair = Attribute.pair(from, to);
                if((w = edgeWeights.get(pair)) > maxWeight) {
                    maxWeight = w; maxPair = pair;
                }
            }
        }
        return maxPair;
    }
}
