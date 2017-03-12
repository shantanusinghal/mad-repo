package edu.uw.cs;

/**
 * Created by shantanusinghal on 03/03/17 @ 3:16 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class TransferFunctions {

    private TransferFunctions() {}

    public static final TransferFunction SIGMOID = new TransferFunction() {
        public double apply(Double x) {
            return (1.0 / (1 + Math.exp(-x)));
        }
    };

}
