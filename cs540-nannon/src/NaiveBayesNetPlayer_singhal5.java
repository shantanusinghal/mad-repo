/**
 * Copyrighted 2013 by Jude Shavlik.  Maybe be freely used for non-profit educational purposes.
 */

/*
 * A player that simply random chooses from among the possible moves.
 * 
 * NOTE: I (Jude) recommend you COPY THIS TO YOUR PLAYER (NannonPlayer_yourLoginName.java) AND THEN EDIT IN THAT FILE.
 * 
 *       Be sure to change "Random-Move Player" in getPlayerName() to something unique to you!
 */

import java.util.*;

public class NaiveBayesNetPlayer_singhal5 extends NannonPlayer {

    private static final int TO = 1;
    private static final int WIN = 1;
    private static final int FROM = 0;
    private static final int LOOSE = 0;
    private static final int EFFECT = 2;

    private int[] resultTable;
    private int[][] movesTable;
    private int[][] hitOppTable;
    private int[][] breakPrimeTable;
    private int[][] createsPrimeTable;
    private int[][] extendsPrimeTable;
    private int[][] moveToSafetyTable;
    private int[][] moveFromHomeTable;

    public NaiveBayesNetPlayer_singhal5() {
        initialize();
    }

    public NaiveBayesNetPlayer_singhal5(NannonGameBoard gameBoard) {
        super(gameBoard);
        initialize();
    }

    // initialize CPT for each random variable
    private void initialize() {
        // initialize each array with 1s to indicate a single occurrence of each event
        movesTable = new int[2][ManageMoveEffects.COUNT_OF_MOVE_DESCRIPTORS];
        hitOppTable = new int[][]{{1,1},{1,1}};
        breakPrimeTable = new int[][]{{1,1},{1,1}};
        createsPrimeTable = new int[][]{{1,1},{1,1}};
        extendsPrimeTable = new int[][]{{1,1},{1,1}};
        moveToSafetyTable = new int[][]{{1,1},{1,1}};
        moveFromHomeTable = new int[][]{{1,1},{1,1}};
        // total wins and losses sum up to 10 from above initializations
        resultTable = new int[]{10,10};
        // initialize CPT for moves with each possible value of move effect
        for (int i = 0; i < ManageMoveEffects.COUNT_OF_MOVE_DESCRIPTORS; i++) {
            resultTable[WIN]++;
            resultTable[LOOSE]++;
            movesTable[WIN][i] = movesTable[LOOSE][i] = 1;
        }
    }

    @Override
    public String getPlayerName() { return "Shantanu\'s Naive Bayes Player"; }

    @SuppressWarnings("unused")
    @Override
    public List<Integer> chooseMove(int[] currentBoardConfiguration, List<List<Integer>> legalMoves) {

        int move;
        List<Integer> chosenMove, bestMove = null;
        boolean isaHit, breaksPrime, createsPrime, moveToSafety, moveFromHome, extendsPrime;
        double bestNetProb = 0.00, bestWinProb = 0.0, probOfWinningGivenEvidence, probOfLoosingGivenEvidence, probOfEvidence, probOfWinningAndEvidence, probOfLoosingAndEvidence;

        if (legalMoves != null) {
        // iterate over each legal move and pick the move that maximizes the ratio of winning probability to loosing probability
            for (int i = 0; i < legalMoves.size(); i++) {
                chosenMove = legalMoves.get(i);

            // compute values of evidence variables for chosen move
                move = chosenMove.get(EFFECT);
                isaHit = ManageMoveEffects.isaHit(chosenMove.get(EFFECT));
                breaksPrime = ManageMoveEffects.breaksPrime(chosenMove.get(EFFECT));
                createsPrime = ManageMoveEffects.createsPrime(chosenMove.get(EFFECT));
                extendsPrime = ManageMoveEffects.extendsPrime(chosenMove.get(EFFECT));
                moveToSafety = chosenMove.get(TO).equals(NannonGameBoard.movingToSAFETY);
                moveFromHome = chosenMove.get(FROM).equals(NannonGameBoard.movingFromHOME);

            // calculate the probability of winning and loosing given the evidence

                // P(W=true, e1, .. en) = P(W=true) ∏ P(ei | W=true) ; where i = 1 to n
                probOfWinningAndEvidence = getProbOfWinningAndPipStatesBeforeAndAfterMoveAs(move, isaHit, breaksPrime, createsPrime, extendsPrime, moveFromHome, moveToSafety);
                // P(W=false, e1, .. en) = P(W=false) ∏ P(ei | W=false) ; where i = 1 to n
                probOfLoosingAndEvidence = getProbOfLoosingAndPipStatesBeforeAndAfterMoveAs(move, isaHit, breaksPrime, createsPrime, extendsPrime, moveFromHome, moveToSafety);

                // P(e1, .. en) = P(W = true, e1, .. en) + P(W = false, e1, .. en)
                probOfEvidence = probOfWinningAndEvidence + probOfLoosingAndEvidence;
                // P(W = true | e1, .. en) = P(W = true, e1, .. en) / P(e1, .. en)
                probOfWinningGivenEvidence = probOfWinningAndEvidence / probOfEvidence;
                // P(W = false | e1, .. en) = P(W = false, e1, .. en) / P(e1, .. en)
                probOfLoosingGivenEvidence = probOfLoosingAndEvidence / probOfEvidence;

                // if the relative probability of win to loss is greater than current max, select it as the best move
                if(probOfWinningGivenEvidence/probOfLoosingGivenEvidence > bestNetProb) {
                    bestMove = legalMoves.get(i);
                    bestWinProb = probOfWinningGivenEvidence;
                    bestNetProb = probOfWinningGivenEvidence/probOfLoosingGivenEvidence;
                }
            }

        }
        Utils.println(String.format("\n%s says: My leanings suggest Move [%d] from %s to %s" +
                        ",%s, because it has the best probability of winning at %.2f ", getPlayerName(), bestMove.get(EFFECT),
                convertFrom(bestMove.get(FROM)), convertTo(bestMove.get(TO)), getDescriptionFor(bestMove.get(EFFECT)),
                bestWinProb));
        return bestMove;
    }

    @SuppressWarnings("unused")
    @Override
    public void updateStatistics(boolean didWin, List<int[]> allBoardConfigs, List<Integer> allPossibleMovesCounts, List<List<Integer>> allMovesMade) {
        List<Integer> chosenMove;
        int numberOfPossibleMoves, moveEffect;
        int[] currentBoard, resultingBoard;
        boolean isaHit, breaksPrime, createsPrime, moveToSafety, moveFromHome, extendsPrime;

        for (int m = 0; m < allMovesMade.size(); m++) {
            // initialize variables to represent current game state
            chosenMove = allMovesMade.get(m);
            numberOfPossibleMoves = allPossibleMovesCounts.get(m);

            if(numberOfPossibleMoves > 1) {

                // record this win or loss; toIndex(bool):int returns the corresponding row based on a globally enforced convention

                resultTable[toIndex(didWin)]++;

                // compute each random variable and register the event in each CPT

                moveEffect = chosenMove.get(EFFECT);
                movesTable[toIndex(didWin)][moveEffect]++;

                isaHit = ManageMoveEffects.isaHit(chosenMove.get(EFFECT));
                hitOppTable[toIndex(didWin)][toIndex(isaHit)]++;

                breaksPrime = ManageMoveEffects.breaksPrime(chosenMove.get(EFFECT));
                breakPrimeTable[toIndex(didWin)][toIndex(breaksPrime)]++;

                createsPrime = ManageMoveEffects.createsPrime(chosenMove.get(EFFECT));
                createsPrimeTable[toIndex(didWin)][toIndex(createsPrime)]++;

                extendsPrime = ManageMoveEffects.extendsPrime(chosenMove.get(EFFECT));
                extendsPrimeTable[toIndex(didWin)][toIndex(extendsPrime)]++;

                moveFromHome = chosenMove.get(FROM).equals(NannonGameBoard.movingFromHOME);
                moveFromHomeTable[toIndex(didWin)][toIndex(moveFromHome)]++;

                moveToSafety = chosenMove.get(TO).equals(NannonGameBoard.movingToSAFETY);
                moveToSafetyTable[toIndex(didWin)][toIndex(moveToSafety)]++;

            }
        }
    }

    @Override
    public void reportLearnedModel() {
        List<BoardModel> leaderBoard = new ArrayList<>();

        // capture all binary valued features in a single model
        for (int i = 0; i < 2; i++) {
            leaderBoard.add(new BoardModel("Hit Opponent Pip", String.valueOf(toBool(i)), calcRelProb(hitOppTable, i)));
            leaderBoard.add(new BoardModel("Break Prime", String.valueOf(toBool(i)), calcRelProb(breakPrimeTable, i)));
            leaderBoard.add(new BoardModel("Create Prime", String.valueOf(toBool(i)), calcRelProb(createsPrimeTable, i)));
            leaderBoard.add(new BoardModel("Extend Prime", String.valueOf(toBool(i)), calcRelProb(extendsPrimeTable, i)));
            leaderBoard.add(new BoardModel("Move From Home", String.valueOf(toBool(i)), calcRelProb(moveFromHomeTable, i)));
            leaderBoard.add(new BoardModel("Move To Safety", String.valueOf(toBool(i)), calcRelProb(moveToSafetyTable, i)));
        }

        // capture all values of moves in the evaluation model
        for (int i = 0; i < ManageMoveEffects.COUNT_OF_MOVE_DESCRIPTORS; i++) {
            leaderBoard.add(new BoardModel("Move", "#" + i, calcRelProb(movesTable, i)));
        }

        // sort the evaluation model based on probability
        Collections.sort(leaderBoard);

        // print highest values as top indicators of winning
        System.out.println("\n-------------------------------------------------");
        System.out.println("\n " + getPlayerName() + ": TOP 5 WIN INDICATORS ");
        System.out.println("\n-------------------------------------------------\n");
        System.out.format("%-25s%-15s%-30s\n", "Variable (RV)", "Value", "P(RV | W) / P (RV | L)");
        for (int i = 0; i < 5; i++) System.out.println(leaderBoard.get(i));

        // print lowest values as top indicators of loosing
        System.out.println("\n-------------------------------------------------");
        System.out.println("\n " + getPlayerName() + ": TOP 5 LOSS INDICATORS ");
        System.out.println("\n-------------------------------------------------\n");
        System.out.format("%-25s%-15s%-30s\n", "Variable (RV)", "Value", "P(RV | W) / P (RV | L)");
        for (int i = leaderBoard.size() - 1; i > leaderBoard.size() - 6; i--) System.out.println(leaderBoard.get(i));
    }

    // prob( randomVariable(s) | win) / prob(randomVariable(s) | loss)
    private double calcRelProb(int[][] events, int i) {
        return (((double)events[WIN][i])/total(events[WIN])) / (((double)events[LOOSE][i])/total(events[LOOSE]));
    }

    // convention: index 1 is for WIN value of T and 0 is for F
    private int toIndex(boolean didWin) {
        return didWin ? WIN : LOOSE;
    }

    // adheres to the convention defined by -> toIndex(bool):int
    private boolean toBool(int i) {
        if(i > 1 || i < 0) throw new IllegalStateException("Illegal conversion to boolean");
        else return i == 1;
    }

    private String getDescriptionFor(Integer move) {
        String str = ManageMoveEffects.convertMoveEffectToString(move);
        if(str.isEmpty()) str = " which is a non descript move";
        else str = str.split(",")[1];
        return str;
    }

    private double getProbOfWinningAndPipStatesBeforeAndAfterMoveAs(int move, boolean isaHit, boolean breaksPrime, boolean createsPrime, boolean extendsPrime, boolean moveFromHome, boolean moveToSafety) {
        return  prob(WIN) *
                getProbOfMoveGivenWin(move) *
                getProbOfHittingAnOpponentGivenWin(toIndex(isaHit)) *
                getProbOfBreakingPrimeGivenWin(toIndex(breaksPrime)) *
                getProbOfCreatingPrimeGivenWin(toIndex(createsPrime)) *
                getProbOfExtendingPrimeGivenWin(toIndex(extendsPrime)) *
                getProbOfMovingFromHomeGivenWin(toIndex(moveFromHome)) *
                getProbOfMovingToSafetyGivenWin(toIndex(moveToSafety));
    }

    private double getProbOfLoosingAndPipStatesBeforeAndAfterMoveAs(int move, boolean isaHit, boolean breaksPrime, boolean createsPrime, boolean extendsPrime, boolean moveFromHome, boolean moveToSafety) {
        return  prob(WIN) *
                getProbOfMoveGivenLoss(move) *
                getProbOfHittingAnOpponentGivenLoss(toIndex(isaHit)) *
                getProbOfBreakingPrimeGivenLoss(toIndex(breaksPrime)) *
                getProbOfCreatingPrimeGivenLoss(toIndex(createsPrime)) *
                getProbOfExtendingPrimeGivenLoss(toIndex(extendsPrime)) *
                getProbOfMovingFromHomeGivenLoss(toIndex(moveFromHome)) *
                getProbOfMovingToSafetyGivenLoss(toIndex(moveToSafety));
    }

    private double prob(int didWin) {
        return ((double)resultTable[didWin]) / total(resultTable);
    }

    private double getProbOfMoveGivenWin(int move) {
        int countOfWins = total(movesTable[WIN]);
        int countOfWinsAndThisMove = movesTable[WIN][move];
        return ((double) countOfWinsAndThisMove) / countOfWins;
    }

    private double getProbOfMoveGivenLoss(int move) {
        int countOfLosses = total(movesTable[LOOSE]);
        int countOfLossesAndThisMove = movesTable[LOOSE][move];
        return ((double) countOfLossesAndThisMove) / countOfLosses;
    }

    private double getProbOfHittingAnOpponentGivenWin(int isaHit) {
        int countOfWins = total(hitOppTable[WIN]);
        int countOfWinsAndOpponentHit = hitOppTable[WIN][isaHit];
        return ((double) countOfWinsAndOpponentHit) / countOfWins;
    }

    private double getProbOfHittingAnOpponentGivenLoss(int isaHit) {
        int countOfLosses = total(hitOppTable[LOOSE]);
        int countOfLossesAndOpponentHit = hitOppTable[LOOSE][isaHit];
        return ((double) countOfLossesAndOpponentHit) / countOfLosses;
    }

    private double getProbOfBreakingPrimeGivenWin(int breakPrime) {
        int countOfWins = total(breakPrimeTable[WIN]);
        int countOfWinsAndBreakingPrime = breakPrimeTable[WIN][breakPrime];
        return ((double) countOfWinsAndBreakingPrime) / countOfWins;
    }

    private double getProbOfBreakingPrimeGivenLoss(int breakPrime) {
        int countOfLosses = total(breakPrimeTable[LOOSE]);
        int countOfLossesAndBreakingPrime = breakPrimeTable[LOOSE][breakPrime];
        return ((double) countOfLossesAndBreakingPrime) / countOfLosses;
    }

    private double getProbOfCreatingPrimeGivenWin(int createPrime) {
        int countOfWins = total(createsPrimeTable[WIN]);
        int countOfWinsAndCreatingPrime = createsPrimeTable[WIN][createPrime];
        return ((double) countOfWinsAndCreatingPrime) / countOfWins;
    }

    private double getProbOfCreatingPrimeGivenLoss(int createPrime) {
        int countOfLosses = total(createsPrimeTable[LOOSE]);
        int countOfLossesAndCreatingPrime = createsPrimeTable[LOOSE][createPrime];
        return ((double) countOfLossesAndCreatingPrime) / countOfLosses;
    }

    private double getProbOfExtendingPrimeGivenWin(int extendPrime) {
        int countOfWins = total(extendsPrimeTable[WIN]);
        int countOfWinsAndExtendingPrime = extendsPrimeTable[WIN][extendPrime];
        return ((double) countOfWinsAndExtendingPrime) / countOfWins;
    }

    private double getProbOfExtendingPrimeGivenLoss(int extendPrime) {
        int countOfLosses = total(extendsPrimeTable[LOOSE]);
        int countOfLossesAndExtendingPrime = extendsPrimeTable[LOOSE][extendPrime];
        return ((double) countOfLossesAndExtendingPrime) / countOfLosses;
    }

    private double getProbOfMovingFromHomeGivenWin(int moveFromHome) {
        int countOfWins = total(moveFromHomeTable[WIN]);
        int countOfWinsAndMovesFromHome = moveFromHomeTable[WIN][moveFromHome];
        return ((double) countOfWinsAndMovesFromHome) / countOfWins;
    }

    private double getProbOfMovingFromHomeGivenLoss(int moveFromHome) {
        int countOfLosses = total(moveFromHomeTable[LOOSE]);
        int countOfLossesAndMovesFromHome = moveFromHomeTable[LOOSE][moveFromHome];
        return ((double) countOfLossesAndMovesFromHome) / countOfLosses;
    }

    private double getProbOfMovingToSafetyGivenWin(int moveToSafety) {
        int countOfWins = total(moveToSafetyTable[WIN]);
        int countOfWinsAndMovesToSafety = moveToSafetyTable[WIN][moveToSafety];
        return ((double) countOfWinsAndMovesToSafety) / countOfWins;
    }

    private double getProbOfMovingToSafetyGivenLoss(int moveToSafety) {
        int countOfLosses = total(moveToSafetyTable[LOOSE]);
        int countOfLossesAndMovesToSafety = moveToSafetyTable[LOOSE][moveToSafety];
        return ((double) countOfLossesAndMovesToSafety) / countOfLosses;
    }

    private int total(int[] events) {
        int sum = 0;
        for (int i = 0; i < events.length; i++) sum += events[i];
        return sum;
    }

    private class BoardModel implements Comparable<BoardModel> {
        private String value;
        private String variable;
        private double probability;

        public BoardModel(String variable, String value, double probability) {
            this.variable = variable;
            this.value = value;
            this.probability = probability;
        }

        public Double getProbability() {
            return Double.valueOf(probability);
        }

        @Override
        public int compareTo(BoardModel o) {
            return o.getProbability().compareTo(this.getProbability());
        }

        @Override
        public String toString() {
            return String.format("%-25s%-15s%.3f\n", variable, value, getProbability());
        }

    }

}