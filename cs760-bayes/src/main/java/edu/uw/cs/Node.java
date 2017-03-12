package edu.uw.cs;

import edu.uw.cs.model.Attribute;
import edu.uw.cs.model.Instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by shantanusinghal on 17/02/17 @ 1:22 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
class Node {

    private CPT cpt;
    private Attribute attribute;
    private List<Node> parents = new ArrayList<Node>();
    private List<Node> children = new ArrayList<Node>();
    // TODO design cpt data-structure

    // TODO is there a way to remove this duplication!!!! arrrgh!
    Node(Attribute attribute, Node... parentNodes) {
        addParent(parentNodes);
        this.attribute = attribute;
    }

    void buildCPT(List<Instance> instances) {
        initializeCPT();
        cpt.populate(instances);
        for (Node child : children) child.buildCPT(instances);
    }

    Attribute getAttribute() {
        return attribute;
    }

    List<Node> getParents() {
        return parents;
    }

    List<Node> getChildren() {
        return children;
    }

    // TODO check: if this becomes public then we can't eagerly initialize CPT
    void addParent(Node... nodes) {
        Collections.addAll(parents, nodes);
    }

    void addChild(Node node) {
        children.add(node);
    }

    Node find(Attribute queryAttribute) {
        Node n;
        if(this.attribute.equals(queryAttribute)) return this;
        else {
            for (Node child : children) {
                if ((n = child.find(queryAttribute)) != null) return n;
            }
        }
        return null;
    }

    double getProb(String value) {
        return cpt.getProb(value);
    }

    double getProb(String value, Map<Attribute, String> evidence) {
        return cpt.getProb(value, evidence);
    }

    @Override
    public String toString() {
        String parentsList = "";
        for (Node parent : parents) parentsList = " " + parent.getAttribute().getName() + parentsList;
        return attribute.getName() + parentsList + "\n";
    }

    private void initializeCPT() {
        cpt = new CPT(attribute, parents, true);
    }

}
