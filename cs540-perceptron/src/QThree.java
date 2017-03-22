/**
 * Created by shantanusinghal on 21/11/16 @ 1:38 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class QThree {

    public static final int CAT = 1;
    public static final int FEAT = 0;
    public static final double x0 = -1;
    public static final double w0 = 1.5;

    public static void main(String[] args) {
        double a, in, w1, err, totalError;
        int[] weightSpace = {-4,-2,-1,0,1,2,4};
        int[][] ex = new int[][] {{1,1},{2,0},{3,0}};

        for (int i = 0; i < weightSpace.length; i++) {
            totalError = 0;
            w1 = weightSpace[i];
            for (int j = 0; j < ex.length; j++) {
                in = (x0 * w0) + (ex[j][FEAT] * w1);
                a = sigmoid(in);
                err = Math.pow((ex[j][CAT] - a), 2);
                System.out.println("For w1=" + w1 + " and ex" + j+1);
                System.out.println(String.format("in = %.4f\na = %.4f\nerr = %.4f\n",in,a,err));
//                System.out.println("receiveSignal = " + in + "\ninit = " + a + "\nerr = " + err);
                totalError += err;
            }
            System.out.println(">>> Total error for w1=" + w1 + " is " + totalError);
        }
    }

    private static double sigmoid(double in) {
        return 1 / (1 + Math.exp((-1 * in)));
    }

}
