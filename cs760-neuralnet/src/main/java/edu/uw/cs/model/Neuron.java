package edu.uw.cs.model;

import edu.uw.cs.TransferFunction;
import edu.uw.cs.TransferFunctions;

/**
 * Created by shantanusinghal on 03/03/17 @ 2:57 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Neuron {

    private final TransferFunction transferFunction;

    private Neuron(TransferFunction transferFunction) {
        this.transferFunction = transferFunction;
    }

    public static Neuron SIGMOIDAL = new Neuron(TransferFunctions.SIGMOID);

    public Double trigger(Double[] input, Double[] weights) {
        return transferFunction.apply(dotProduct(input, weights));
    }

    private double dotProduct(Double[] a, Double[] b) {
        if(a != null && b != null && a.length != b.length) throw new IllegalStateException("Incompatible input and weight vectors");
        double sum = 0.0;
        for (int i = 0 ; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

}
