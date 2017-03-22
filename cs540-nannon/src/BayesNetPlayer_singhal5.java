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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BayesNetPlayer_singhal5 extends NannonPlayer {

    private static final int TO = 1;
    private static final int WIN = 1;
    private static final int FROM = 0;
    private static final int LOOSE = 0;
    private static final int EFFECT = 2;
    private static final int COUNT_MOVES = ManageMoveEffects.COUNT_OF_MOVE_DESCRIPTORS;
    private static final int COUNT_PIECES = NannonGameBoard.getPiecesPerPlayer() + 1; // extra one to accommodate for null value

    private int[] resultTable;
    private int[][][][][] movesTable;
    private int[][] pipsOnBoardTable;
    private int[][] pipsAtHomeTable;
    private int[][] pipsInDangerTable;
    private int[][][] buildPrimeTable;
    private int[][][] hitOppTable;

    public BayesNetPlayer_singhal5() {
        initialize();
    }

    public BayesNetPlayer_singhal5(NannonGameBoard gameBoard) {
        super(gameBoard);
        initialize();
    }

    // initialize CPT for each random variable
    private void initialize() {
        resultTable = new int[2]; // the only single dimensional table because it has no parents; 2 rows, one each state True and False
        pipsOnBoardTable = new int[2][COUNT_PIECES];
        pipsAtHomeTable = new int[2][COUNT_PIECES];
        pipsInDangerTable = new int[2][COUNT_PIECES];
        hitOppTable = new int[2][COUNT_MOVES][2];
        buildPrimeTable = new int[2][COUNT_MOVES][2];
        movesTable = new int[2][COUNT_PIECES][COUNT_PIECES][COUNT_PIECES][COUNT_MOVES];
        for (int i = 0; i < COUNT_PIECES; i++)
            for (int j = 0; j < COUNT_PIECES; j++)
                for (int k = 0; k < COUNT_PIECES; k++)
                    for (int l = 0; l < COUNT_MOVES; l++)
                        for (int m = 0; m < 2; m++)
                            for (int n = 0; n < 2; n++) {
                                // record two pseudo events (one win and one loss) that'll served as evidence for all other CPT entries
                                resultTable[WIN]++;
                                resultTable[LOOSE]++;
                                // record a win and a loss for the CPTs at the bottom of the causal chain
                                hitOppTable[WIN][l][m]++;
                                hitOppTable[LOOSE][l][m]++;
                                buildPrimeTable[WIN][l][n]++;
                                buildPrimeTable[LOOSE][l][n]++;
                                // record a win and a loss for the combination of this move and board the configuration combination
                                movesTable[WIN][i][j][k][l]++;
                                movesTable[LOOSE][i][j][k][l]++;
                                // record a win and a loss in the CPTs for various pip states on current board
                                pipsOnBoardTable[WIN][i]++;
                                pipsOnBoardTable[LOOSE][i]++;
                                pipsAtHomeTable[WIN][j]++;
                                pipsAtHomeTable[LOOSE][j]++;
                                pipsInDangerTable[WIN][k]++;
                                pipsInDangerTable[LOOSE][k]++;
        }
    }

    @Override
    public String getPlayerName() { return "Shantanu\'s Wise Bayes Player"; }

    @SuppressWarnings("unused")
    @Override
    public List<Integer> chooseMove(int[] boardConfiguration, List<List<Integer>> legalMoves) {

        boolean hitOpp, buildsPrime;
        List<Integer> chosenMove, bestMove = null;
        int move, unsafe, atHome, onBoard, winCount, looseCount;
        double bestNetProb = 0.00, bestWinProb = 0.0, probOfWinningGivenEvidence, probOfLoosingGivenEvidence, probOfEvidence, probOfWinningAndEvidence, probOfLoosingAndEvidence;

        if (legalMoves != null) {
        // iterate over each legal move and pick the move that maximizes the ratio of winning probability to loosing probability
            for (int i = 0; i < legalMoves.size(); i++) {
                chosenMove = legalMoves.get(i);

            // compute values of evidence variables for chosen move
                move = chosenMove.get(EFFECT);
                onBoard = calculatePipsThatAreOnTheBoardIn(boardConfiguration);
                atHome = calculatePipsThatAreAtHomeIn(boardConfiguration);
                unsafe = calculatePipsThatAreUnsafeIn(boardConfiguration);
                hitOpp = ManageMoveEffects.isaHit(chosenMove.get(EFFECT));
                buildsPrime = ManageMoveEffects.createsPrime(chosenMove.get(EFFECT)) || ManageMoveEffects.extendsPrime(chosenMove.get(EFFECT));

            // calculate the probability of winning and loosing given the evidence

                // P(W=true, e1, .. en) = P(W=true) ∏ P(ei | parents(Ei)) ; where i = 1 to n
                probOfWinningAndEvidence = getProbOfWinningAndPipStatesBeforeAndAfterMoveAs(move, unsafe, onBoard, atHome, hitOpp, buildsPrime);
                // P(W=false, e1, .. en) = P(W=false) ∏ P(ei | parents(Ei)) ; where i = 1 to n
                probOfLoosingAndEvidence = getProbOfLoosingAndPipStatesBeforeAndAfterMoveAs(move, unsafe, onBoard, atHome, hitOpp, buildsPrime);

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

    private String getDescriptionFor(Integer move) {
        String str = ManageMoveEffects.convertMoveEffectToString(move);
        if(str.isEmpty()) str = " which is a non descript move";
        else str = str.split(",")[1];
        return str;
    }

    @SuppressWarnings("unused")
    @Override
    public void updateStatistics(boolean didWin, List<int[]> allBoardConfigs, List<Integer> allPossibleMovesCounts, List<List<Integer>> allMovesMade) {
        int[] currentBoard;
        List<Integer> chosenMove;
        boolean hitOpp, buildsPrime;
        int result, move, numberOfPossibleMoves, pipsInDanger, pipsAtHome, pipsOnBoard;

        for (int m = 0; m < allMovesMade.size(); m++) {
            // check if move is defined at index
            if(allMovesMade.get(m) != null) move = allMovesMade.get(m).get(EFFECT); else continue;

            // initialize variables to represent current game state
            result = toIndex(didWin);
            currentBoard = allBoardConfigs.get(m);
            numberOfPossibleMoves = allPossibleMovesCounts.get(m);

            if(numberOfPossibleMoves > 1) {

            // record this win or loss; toIndex(bool):int returns the corresponding row based on a globally enforced convention

                resultTable[result]++;

            // compute each random variable and register the event in each CPT

                pipsOnBoard = calculatePipsThatAreOnTheBoardIn(currentBoard);
                pipsOnBoardTable[result][pipsOnBoard]++;

                pipsAtHome = calculatePipsThatAreAtHomeIn(currentBoard);
                pipsAtHomeTable[result][pipsAtHome]++;

                pipsInDanger = calculatePipsThatAreUnsafeIn(currentBoard);
                pipsInDangerTable[result][pipsInDanger]++;

                hitOpp = ManageMoveEffects.isaHit(move);
                hitOppTable[result][move][toIndex(hitOpp)]++;

                buildsPrime = ManageMoveEffects.createsPrime(move) || ManageMoveEffects.extendsPrime(move);
                buildPrimeTable[result][move][toIndex(buildsPrime)]++;

                movesTable[result][pipsInDanger][pipsOnBoard][pipsAtHome][move]++;
            }
        }
    }

    @Override
    public void reportLearnedModel() {
        List<BoardModel> leaderBoard = new ArrayList<>();

        // capture all binary valued features in a single model
        for (int i = 0; i < 2; i++) {
            leaderBoard.add(new BoardModel("hit opponent pip", String.valueOf(toBool(i)), calcRelProb(hitOppTable, i)));
            leaderBoard.add(new BoardModel("build prime", String.valueOf(toBool(i)), calcRelProb(buildPrimeTable, i)));
        }

        for (int i = 0; i < NannonGameBoard.getPiecesPerPlayer(); i++) {
            leaderBoard.add(new BoardModel("# pips on the board", String.valueOf(i), calcRelProb(pipsOnBoardTable, i)));
            leaderBoard.add(new BoardModel("# pip at home base", String.valueOf(i), calcRelProb(pipsAtHomeTable, i)));
            leaderBoard.add(new BoardModel("# pip that are unsafe", String.valueOf(i), calcRelProb(pipsInDangerTable, i)));
        }

        // capture all values of moves in the evaluation model
        for (int i = 0; i < ManageMoveEffects.COUNT_OF_MOVE_DESCRIPTORS; i++) {
            leaderBoard.add(new BoardModel("move type", "#" + i, calcRelProb(movesTable, i)));
        }

        // sort the evaluation model based on probability
        Collections.sort(leaderBoard);

        // print highest values as top indicators of winning
        System.out.println("\n-------------------------------------------------");
        System.out.println("\n " + getPlayerName() + ": TOP 5 WIN INDICATORS ");
        System.out.println("\n-------------------------------------------------\n");
        System.out.format("%-25s%-15s%-30s\n", "Variable (RV)", "Value", "P(RV | W) / P (RV | L)");
        for (int i = 0; i < leaderBoard.size(); i++) System.out.println(leaderBoard.get(i));

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

    // syntax sugar: overloaded method to hide the intermediate step of marginalization
    private double calcRelProb(int[][][] events, int i) {
        return calcRelProb(marginalize(events), i);
    }

    // syntax sugar: another overloaded method to hide the intermediate step of marginalization
    private double calcRelProb(int[][][][][] events, int i) {
        return calcRelProb(marginalize(events), i);
    }

    // sum up all CPT rows for the same value of first (corresponds to result, win or loose) and last column (corresponds to query variable)
    private int[][] marginalize(int[][][] events) {
        int[][] marginalized = new int[events.length][events[0][0].length];
        for (int i = 0; i < events.length; i++) {
            for (int k = 0; k < events[0][0].length; k++) {
                marginalized[i][k] = 0;
                for (int j = 0; j < events[0].length; j++) {
                    marginalized[i][k] += events[i][j][k];
                }
            }
        }
        return marginalized;
    }

    // sum up all CPT rows for the same value of first (corresponds to result, win or loose) and last column (corresponds to query variable)
    private int[][] marginalize(int[][][][][] events) {
        int[][] marginalized = new int[events.length][events[0][0][0][0].length];
        for (int i = 0; i < events.length; i++) {
            for (int m = 0; m < events[0][0][0][0].length; m++) {
                marginalized[i][m] = 0;
                for (int j = 0; j < events[0].length; j++) {
                    for (int k = 0; k < events[0][0].length; k++) {
                        for (int l = 0; l < events[0][0][0].length; l++) {
                            marginalized[i][m] += events[i][j][k][l][m];
                        }
                    }
                }
            }
        }
        return marginalized;
    }

    // iterate over the board and count all pips that don't belong to a prime
    private int calculatePipsThatAreUnsafeIn(int[] boardConfig) {
        if(boardConfig[0] == 0) return  -1;
        int pip = boardConfig[0], unsafePipCounter = 0;
        int[] board = getJustTheBoardFrom(boardConfig);
        for (int i = 0; i < board.length; i++) {
            // don't increment counter when observing a prime
            if(  board[i] == pip &&  //................................................................... only counting only pips for current player
                    ((i == 0 && board[i+1] == pip)  //....................................................... pip in left corner of board and in a prime
                    || (i == board.length - 1 && board[i-1] == pip)  //...................................... pip in right corner of board and in a prime
                    || (i > 0 && i < board.length - 1 && (board[i-1] == pip || board[i+1] == pip)))) {  //... pip is part of a prime formation
                continue;
            }
            // else increment counter
            else if(board[i] == pip) unsafePipCounter++;
        }
        return unsafePipCounter;
    }

    private int calculatePipsThatAreAtHomeIn(int[] boardConfig) {
        return boardConfig[0] == 0 ? -1 : boardConfig[0] == 1 ? boardConfig[1] : boardConfig[2];
    }

    // subtract pieces at safe or home from the total to compute the ones in play
    private int calculatePipsThatAreOnTheBoardIn(int[] boardConfig) {
        if(boardConfig[0] == 0)
            return -1;
        else if(boardConfig[0] == 1) {
            return NannonGameBoard.getPiecesPerPlayer() - (boardConfig[3] + boardConfig[1]);
        } else {
            return NannonGameBoard.getPiecesPerPlayer() - (boardConfig[4] + boardConfig[2]);
        }
    }

    private int[] getJustTheBoardFrom(int[] boardConfig) {
        return Arrays.copyOfRange(boardConfig, 7, boardConfig.length);
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

    private double getProbOfWinningAndPipStatesBeforeAndAfterMoveAs(int move, int unsafe, int onBoard, int atHome, boolean hitOpp, boolean buildPrime) {
        return  prob(WIN) *
                getProbOfCurrentPipsOnBoardGivenWin(onBoard) *
                getProbOfCurrentPipsAtHomeGivenWin(atHome) *
                getProbOfCurrentPipsInDangerGivenWin(unsafe) *
                getPropOfMoveGivenCurrentBoardValuesAndWin(move, unsafe, onBoard, atHome) *
                getProbOfHittingOppGivenMoveAndWin(toIndex(hitOpp), move) *
                getProbOfBuildingPrimeGivenMoveAndWin(toIndex(buildPrime), move);
    }

    private double getProbOfLoosingAndPipStatesBeforeAndAfterMoveAs(int move, int unsafe, int onBoard, int atHome, boolean hitOpp, boolean buildPrime) {
        return  prob(LOOSE) *
                getProbOfCurrentPipsOnBoardGivenLoss(onBoard) *
                getProbOfCurrentPipsAtHomeGivenLoss(atHome) *
                getProbOfCurrentPipsInDangerGivenLoss(unsafe) *
                getPropOfMoveGivenCurrentBoardValuesAndLoss(move, unsafe, onBoard, atHome) *
                getProbOfHittingOppGivenMoveAndLoss(toIndex(hitOpp), move) *
                getProbOfBuildingPrimeGivenMoveAndLoss(toIndex(buildPrime), move);
    }

    private double prob(int didWin) {
        return ((double)resultTable[didWin]) / total(resultTable);
    }

    private double getPropOfMoveGivenCurrentBoardValuesAndWin(int move, int unsafe, int onBoard, int atHome) {
        int countOfWinsWithTheseManyCurrentPipsInVariousStates = total(movesTable[WIN][unsafe][onBoard][atHome]);
        int countOfWinsWithTheseManyCurrentPipsInVariousStatesAndThisMove = movesTable[WIN][unsafe][onBoard][atHome][move];
        return ((double) countOfWinsWithTheseManyCurrentPipsInVariousStatesAndThisMove) / countOfWinsWithTheseManyCurrentPipsInVariousStates;
    }

    private double getPropOfMoveGivenCurrentBoardValuesAndLoss(int move, int unsafe, int onBoard, int atHome) {
        int countOfLossesWithTheseManyCurrentPipsInVariousStates = total(movesTable[LOOSE][unsafe][onBoard][atHome]);
        int countOfLossesWithTheseManyCurrentPipsInVariousStatesAndThisMove = movesTable[LOOSE][unsafe][onBoard][atHome][move];
        return ((double) countOfLossesWithTheseManyCurrentPipsInVariousStatesAndThisMove) / countOfLossesWithTheseManyCurrentPipsInVariousStates;
    }

    private double getProbOfCurrentPipsOnBoardGivenWin(int onBoard) {
        int countOfWins = total(pipsOnBoardTable[WIN]);
        int countOfWinsAndPipsOnBoard = pipsOnBoardTable[WIN][onBoard];
        return ((double) countOfWinsAndPipsOnBoard) / countOfWins;
    }

    private double getProbOfCurrentPipsOnBoardGivenLoss(int onBoard) {
        int countOfLosses = total(pipsOnBoardTable[LOOSE]);
        int countOfLossesAndPipsOnBoard = pipsOnBoardTable[LOOSE][onBoard];
        return ((double) countOfLossesAndPipsOnBoard) / countOfLosses;
    }

    private double getProbOfCurrentPipsAtHomeGivenWin(int atHome) {
        int countOfWins = total(pipsAtHomeTable[WIN]);
        int countOfWinsAndPipsAtHome = pipsAtHomeTable[WIN][atHome];
        return ((double) countOfWinsAndPipsAtHome) / countOfWins;
    }

    private double getProbOfCurrentPipsAtHomeGivenLoss(int atHome) {
        int countOfLosses = total(pipsAtHomeTable[LOOSE]);
        int countOfLossesAndPipsAtHome = pipsAtHomeTable[LOOSE][atHome];
        return ((double) countOfLossesAndPipsAtHome) / countOfLosses;
    }

    private double getProbOfCurrentPipsInDangerGivenWin(int unsafe) {
        int countOfWins = total(pipsInDangerTable[WIN]);
        int countOfWinsAndPipsInDanger = pipsInDangerTable[WIN][unsafe];
        return ((double) countOfWinsAndPipsInDanger) / countOfWins;
    }

    private double getProbOfCurrentPipsInDangerGivenLoss(int unsafe) {
        int countOfLosses = total(pipsInDangerTable[LOOSE]);
        int countOfLossesAndPipsInDanger = pipsInDangerTable[LOOSE][unsafe];
        return ((double) countOfLossesAndPipsInDanger) / countOfLosses;
    }

    private double getProbOfHittingOppGivenMoveAndWin(int hitOpp, int move) {
        int countOfWinsAndThisMove = total(hitOppTable[WIN][move]);
        int countOfWinsAndThisMoveAndHittingOpp = hitOppTable[WIN][move][hitOpp];
        return ((double) countOfWinsAndThisMoveAndHittingOpp) / countOfWinsAndThisMove;
    }

    private double getProbOfHittingOppGivenMoveAndLoss(int hitOpp, int move) {
        int countOfLossesAndThisMove = total(hitOppTable[LOOSE][move]);
        int countOfLossesAndThisMoveAndHittingOpp = hitOppTable[LOOSE][move][hitOpp];
        return ((double) countOfLossesAndThisMoveAndHittingOpp) / countOfLossesAndThisMove;
    }

    private double getProbOfBuildingPrimeGivenMoveAndWin(int buildPrime, int move) {
        int countOfWinsAndThisMove = total(buildPrimeTable[WIN][move]);
        int countOfWinsAndThisMoveAndBuildingPrime = buildPrimeTable[WIN][move][buildPrime];
        return ((double) countOfWinsAndThisMoveAndBuildingPrime) / countOfWinsAndThisMove;
    }

    private double getProbOfBuildingPrimeGivenMoveAndLoss(int buildPrime, int move) {
        int countOfLossesAndThisMove = total(buildPrimeTable[LOOSE][move]);
        int countOfLossesAndThisMoveAndBuildingPrime = buildPrimeTable[LOOSE][move][buildPrime];
        return ((double) countOfLossesAndThisMoveAndBuildingPrime) / countOfLossesAndThisMove;
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

