package edu.uw.cs;

import edu.uw.cs.model.Attribute;
import edu.uw.cs.model.Dataset;
import edu.uw.cs.utils.GraphUtils;
import edu.uw.cs.utils.ProbUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shantanusinghal on 17/02/17 @ 1:45 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
class TreeAugmentedNBClassifier extends AbstractClassifier{

    private final ProbUtils probUtils;

    private TreeAugmentedNBClassifier(Dataset dataset) {
        // initialize root and other fields
        super(new Node(dataset.getClassAttribute()));
        probUtils = new ProbUtils(dataset);

        // define edge weights between all feature attributes as the conditional mutual information between then given the class attribute
        Map<Attribute.Pair, Double> edgeWeights = calculateEdgeWeights(dataset.getAttributes(), dataset.getClassAttribute());

        // identify augmenting edges by defining the minimum spanning tree in a complete graph of feature nodes with CMI as edge weights
        Map<Attribute, Attribute> augmentingEdgesToFrom = GraphUtils.buildMST(dataset.getAttributes(), edgeWeights);

        // build tree augmented bayes network
        buildTree(getRoot(), dataset.getAttributes(), augmentingEdgesToFrom);

        // build CTP at root, which propagates it down the tree
        getRoot().buildCPT(dataset.getInstances());
    }

    static Classifier learn(Dataset trainData) {
        return new TreeAugmentedNBClassifier(trainData);
    }

    /********************** PRIVATE METHODS **********************/

    private Map<Attribute.Pair, Double> calculateEdgeWeights(List<Attribute> attributes, Attribute classAttribute) {
        Map<Attribute.Pair, Double> cmiTable = new HashMap<Attribute.Pair, Double>();
        for (Attribute a1 : attributes) {
            for (Attribute a2 : attributes) {
                Attribute.Pair pair = Attribute.pair(a1, a2);
                // symmetric attribute pairs map to CMI edge weights
                if(!cmiTable.containsKey(pair)) cmiTable.put(pair, probUtils.calculateCMI(a1, a2, classAttribute));
            }
        }
        return cmiTable;
    }


}
