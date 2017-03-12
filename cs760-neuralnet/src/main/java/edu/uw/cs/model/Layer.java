package edu.uw.cs.model;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by shantanusinghal on 10/03/17 @ 1:52 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Layer {

    private Neuron neuron;
    private double bias = -1;

    /**
     * Returns a vector of neuron outputs when the output of the previous
     * layer ({@code input}) is applied as weighted input to each unit.
     *
     * @param  input output vector of previous layer
     * @param  weights matrix of incoming weights
     *
     * @return a vector of neuron outputs, 0-th index holds bias value if present
     *
     * **/
    public Double[] feed(Double[] input, Double[][] weights) {
        assertThat(neuron, notNullValue());

        // calculate number of neurons as length ot the weight matrix
        int numNeurons = weights.length;

        // initialize the result vector to hold the output for each neuron (possibly including a bias unit)
        Double[] result = new Double[numNeurons + (hasBias() ? 1 : 0)];

        // add the bias unit at 0-th index if one exists
        if(hasBias()) result[0] = bias;

        // result vector is populated with the neuron output
        for(int n = 0; n < numNeurons; n++) {

            // neuron is triggered with input and a weight vector containing all incoming weights to neuron
            result[n + 1] = neuron.trigger(input, weights[n]);
        }

        return result;
    }

    public Layer madeOf(Neuron neuron) {
        this.neuron = neuron;
        return this;
    }

    public Layer withBias(double bias) {
        this.bias = bias;
        return this;
    }

    private boolean hasBias() {
        return bias >= 0;
    }
}
