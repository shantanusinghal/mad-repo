package edu.uw.cs;

import edu.uw.cs.model.Dataset;

/**
 * Created by shantanusinghal on 17/02/17 @ 1:44 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
class NaiveBayesClassifier extends AbstractClassifier {

    private NaiveBayesClassifier(Dataset dataset) {
        // initialize root
        super(new Node(dataset.getClassAttribute()));

        // build the naive bayes network
        buildTree(getRoot(), dataset.getAttributes());

        // build CTP at root, which propagates it down the tree
        getRoot().buildCPT(dataset.getInstances());
    }

    static Classifier learn(Dataset trainData) {
        return new NaiveBayesClassifier(trainData);
    }

}
