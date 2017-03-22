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

public class FullJointProbTablePlayer_singhal5 extends NannonPlayer {

    private static final int TO = 1;
    private static final int FROM = 0;
    private static final int EFFECT = 2;
    private static final int COUNT_IS_A_HIT = 2;
    private static final int COUNT_BREAKS_PRIME = 2;
    private static final int COUNT_BUILD_PRIME = 2;
    private static final int COUNT_FROM_HOME = 2;
    private static final int COUNT_PIECES = NannonGameBoard.getPiecesPerPlayer() + 1; // extra one to accommodate for null value

    private int totalLosingMoves;
    private int totalWinningMoves;
    private int[][][][][][] losingHistory;
    private int[][][][][][] winningHistory;

	public FullJointProbTablePlayer_singhal5() {
		initialize();
	}

    public FullJointProbTablePlayer_singhal5(NannonGameBoard gameBoard) {
		super(gameBoard);
		initialize();
	}

    // initialize the joint distribution table
    private void initialize() {
        losingHistory = new int[COUNT_PIECES][COUNT_PIECES][COUNT_IS_A_HIT][COUNT_BREAKS_PRIME][COUNT_BUILD_PRIME][COUNT_FROM_HOME];
        winningHistory = new int[COUNT_PIECES][COUNT_PIECES][COUNT_IS_A_HIT][COUNT_BREAKS_PRIME][COUNT_BUILD_PRIME][COUNT_FROM_HOME];
        // assuming single occurrence of each event i.e. M-estimate with m = 1
        for (int i = 0; i < COUNT_PIECES; i++)
            for (int j = 0; j < COUNT_PIECES; j++)
                for (int k = 0; k < COUNT_IS_A_HIT; k++)
                    for (int l = 0; l < COUNT_BREAKS_PRIME; l++)
                        for (int m = 0; m < COUNT_BUILD_PRIME; m++)
                            for (int n = 0; n < COUNT_FROM_HOME; n++) {
                                totalLosingMoves++; totalWinningMoves++;
                                winningHistory[i][j][k][l][m][n] = losingHistory[i][j][k][l][m][n] = 1;
                            }
    }

    @Override
    public String getPlayerName() { return "Shantanu\'s FJD Player"; }

	@SuppressWarnings("unused")
	@Override
	public List<Integer> chooseMove(int[] boardConfiguration, List<List<Integer>> legalMoves) {

        List<Integer> bestMove = null;
        boolean isaHit, breaksPrime, buildPrime, extendsPrime, movesFromHome;
        int unsafePipsInCurrent, unsafePipsInResulting, winCount, looseCount;
        double bestNetProb = 0.00, bestWinProb = 0.0, probOfWinningAndCurrentBoard, probOfLoosingAndCurrentBoard;

        if (legalMoves != null) {

            for (int i = 0; i < legalMoves.size(); i++) {
                // iterate over each legal move and pick the move that maximizes the ratio of winning probability to loosing probability
                List<Integer> chosenMove = legalMoves.get(i);

                // compute values of random variables for chosen move
                isaHit = ManageMoveEffects.isaHit(chosenMove.get(EFFECT));
                breaksPrime = ManageMoveEffects.breaksPrime(chosenMove.get(EFFECT));
                buildPrime = ManageMoveEffects.createsPrime(chosenMove.get(EFFECT)) || ManageMoveEffects.extendsPrime(chosenMove.get(EFFECT));
                movesFromHome = chosenMove.get(FROM) == NannonGameBoard.movingFromHOME;
                unsafePipsInCurrent = calculatePipsThatAreUnsafeIn(boardConfiguration);
                unsafePipsInResulting = calculatePipsThatAreUnsafeIn(gameBoard.getNextBoardConfiguration(boardConfiguration, chosenMove));

                // calculate probabilities given the evidence values of random variables
                winCount = winningHistory[unsafePipsInCurrent][unsafePipsInResulting][toIndex(isaHit)][toIndex(breaksPrime)][toIndex(buildPrime)][toIndex(movesFromHome)];
                looseCount = losingHistory[unsafePipsInCurrent][unsafePipsInResulting][toIndex(isaHit)][toIndex(breaksPrime)][toIndex(buildPrime)][toIndex(movesFromHome)];
                probOfWinningAndCurrentBoard = ((double) winCount) / getTotalMoves();
                probOfLoosingAndCurrentBoard = ((double) looseCount) / getTotalMoves();

                // if the relative probability of win to loss is greater than current max, select it as the best move
                if(probOfWinningAndCurrentBoard/probOfLoosingAndCurrentBoard > bestNetProb) {
                    bestMove = legalMoves.get(i);
                    bestWinProb = probOfWinningAndCurrentBoard;
                    bestNetProb = probOfWinningAndCurrentBoard/probOfLoosingAndCurrentBoard;
                }
            }
        }
        Utils.println(String.format("\n%s says: My leanings suggest Move [%d] from %s to %s" +
                ",%s, because it has the best probability of winning at %.2f ", getPlayerName(), bestMove.get(EFFECT),
                convertFrom(bestMove.get(FROM)), convertTo(bestMove.get(TO)), getDescriptionFor(bestMove.get(EFFECT)),
                bestWinProb));
		return bestMove;
	}

    private int getTotalMoves() {
        return totalWinningMoves + totalLosingMoves;
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
        List<Integer> chosenMove;
        int[] currentBoard, resultingBoard;
        int numberOfPossibleMoves, unsafePipsInCurrent, unsafePipsInResulting;
        boolean isaHit, breaksPrime, extendsPrime, buildsPrime, movesFromHome;

        for (int m = 0; m < allMovesMade.size(); m++) {
            // initialize variables to represent current game state
            chosenMove = allMovesMade.get(m);
            currentBoard = allBoardConfigs.get(m);
            numberOfPossibleMoves = allPossibleMovesCounts.get(m);
            resultingBoard = numberOfPossibleMoves < 1 ? currentBoard : gameBoard.getNextBoardConfiguration(currentBoard, chosenMove);

            // learning from forced moves lead to decreased accuracy across multiple experiments, so excluding forced move events
            if(numberOfPossibleMoves > 1) {
                isaHit = ManageMoveEffects.isaHit(chosenMove.get(EFFECT));
                breaksPrime = ManageMoveEffects.breaksPrime(chosenMove.get(EFFECT));
                buildsPrime = ManageMoveEffects.createsPrime(chosenMove.get(EFFECT)) || ManageMoveEffects.extendsPrime(chosenMove.get(EFFECT));
                unsafePipsInCurrent = calculatePipsThatAreUnsafeIn(currentBoard);
                unsafePipsInResulting = calculatePipsThatAreUnsafeIn(resultingBoard);
                movesFromHome = chosenMove.get(FROM) == NannonGameBoard.movingFromHOME;
                // sanity check for empty player
                if(unsafePipsInCurrent >= 0 && unsafePipsInResulting >= 0) {
                    if(didWin) {
                        totalWinningMoves++;
                        winningHistory[unsafePipsInCurrent][unsafePipsInResulting][toIndex(isaHit)][toIndex(breaksPrime)][toIndex(buildsPrime)][toIndex(movesFromHome)]++;
                    } else {
                        totalLosingMoves++;
                        losingHistory[unsafePipsInCurrent][unsafePipsInResulting][toIndex(isaHit)][toIndex(breaksPrime)][toIndex(buildsPrime)][toIndex(movesFromHome)]++;
                    }
                }
            }
        }
	}

    @Override
    public void reportLearnedModel() {
        List<BoardModel> winningModel = new ArrayList<>(); List<BoardModel> losingModel = new ArrayList<>();
        double probOfCurrentBoardAndWinning, probOfCurrentBoardAndLoosing, probOfWinning = 0.0, probOfLoosing = 0.0;

        // iterate over each row of full joint distribution to build a model for easy computation
        for (int current = 0; current < COUNT_PIECES; current++)
            for (int result = 0; result < COUNT_PIECES; result++)
                for (int isHit = 0; isHit < COUNT_IS_A_HIT; isHit++)
                    for (int breakPrime = 0; breakPrime < COUNT_BREAKS_PRIME; breakPrime++)
                        for (int buildsPrime = 0; buildsPrime < COUNT_BUILD_PRIME; buildsPrime++)
                            for (int fromHome = 0; fromHome < COUNT_FROM_HOME; fromHome++) {
                                probOfCurrentBoardAndWinning = ((double) winningHistory[current][result][isHit][breakPrime][buildsPrime][fromHome]) / getTotalMoves();
                                probOfCurrentBoardAndLoosing = ((double) losingHistory[current][result][isHit][breakPrime][buildsPrime][fromHome]) / getTotalMoves();
                                probOfWinning += probOfCurrentBoardAndWinning;
                                probOfLoosing += probOfCurrentBoardAndLoosing;
                                winningModel.add(new BoardModel(current,result,probOfCurrentBoardAndWinning,probOfCurrentBoardAndLoosing,toBool(isHit),toBool(breakPrime),toBool(buildsPrime),toBool(fromHome)));
                                losingModel.add(new BoardModel(current,result,probOfCurrentBoardAndWinning,probOfCurrentBoardAndLoosing,toBool(isHit),toBool(breakPrime),toBool(buildsPrime),toBool(fromHome)));
                            }

        // inform each board state what the total probability of winning and loosing is
        for (BoardModel model : losingModel) {
            model.setProbabilityOfWinning(probOfWinning);
            model.setProbabilityOfLoosing(probOfLoosing);
        }
        for (BoardModel model : winningModel) {
            model.setProbabilityOfWinning(probOfWinning);
            model.setProbabilityOfLoosing(probOfLoosing);
        }

        // print top indicators of winning
        System.out.println("\n-------------------------------------------------");
        System.out.println("\n " + getPlayerName() + ": TOP 5 WIN INDICATORS ");
        System.out.println("\n-------------------------------------------------\n");
        System.out.format("%-15s%-15s%-10s%-15s%-15s%-15s%-8s\n", "Curr Unsafe", "Res Unsafe", "Hit Opp", "Break Prime", "Build Prime", "Move from Home", "Rel Prob");
        Collections.sort(winningModel);
        winningModel.subList(0, 5).forEach(System.out::print);

        // print top indicators of loosing
        System.out.println("\n-------------------------------------------------");
        System.out.println("\n " + getPlayerName() + ": TOP 5 LOSS INDICATORS ");
        System.out.println("\n-------------------------------------------------\n");
        System.out.format("%-15s%-15s%-10s%-15s%-15s%-15s%-8s\n", "Curr Unsafe", "Res Unsafe", "Hit Opp", "Break Prime", "Build Prime", "Move from Home", "Rel Prob");
        Collections.sort(losingModel);
        Collections.reverse(losingModel);
        losingModel.subList(0, 5).forEach(System.out::print);
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

    private int[] getJustTheBoardFrom(int[] boardConfig) {
        return Arrays.copyOfRange(boardConfig, 7, boardConfig.length);
    }

    private int toIndex(boolean bool) {
        return bool ? 1 : 0;
    }

    private boolean toBool(int i) {
        if(i > 1 || i < 0) throw new IllegalStateException("Illegal conversion to boolean");
        return i == 1 ? true : false;
    }

    private class BoardModel implements Comparable<BoardModel> {
        private boolean isaHit;
        private boolean fromHome;
        private boolean breaksPrime;
        private boolean createsPrime;
        private double probOfWinning = -1;
        private double probOfLoosing = -1;
        private int currentUnsafePipCount;
        private int resultingUnsafePipCount;
        private double probOfCurrentBoardAndWinning;
        private double probOfCurrentBoardAndLoosing;

        private BoardModel(int currentUnsafePipCount, int resultingUnsafePipCount, double probOfCurrentBoardAndWinning, double probOfCurrentBoardAndLoosing, boolean isaHit, boolean breaksPrime, boolean buildsPrime, boolean fromHome) {
            this.isaHit = isaHit;
            this.fromHome = fromHome;
            this.breaksPrime = breaksPrime;
            this.createsPrime = buildsPrime;
            this.currentUnsafePipCount = currentUnsafePipCount;
            this.resultingUnsafePipCount = resultingUnsafePipCount;
            this.probOfCurrentBoardAndWinning = probOfCurrentBoardAndWinning;
            this.probOfCurrentBoardAndLoosing = probOfCurrentBoardAndLoosing;
        }

        public double getRelativeProbability() {
            return getProbOfCurrentBoardGivenWin() / getProbOfCurrentBoardGivenLoss();
        }

        private double getProbOfCurrentBoardGivenLoss() {
            if(probOfLoosing < 0) throw new IllegalStateException("First set the probability of loosing!");
            return probOfCurrentBoardAndWinning/probOfWinning;
        }

        private double getProbOfCurrentBoardGivenWin() {
            if(probOfLoosing < 0) throw new IllegalStateException("First set the probability of winning!");
            return probOfCurrentBoardAndLoosing/probOfLoosing;
        }

        public void setProbabilityOfWinning(double probOfWinning) {
            this.probOfWinning = probOfWinning;
        }

        public void setProbabilityOfLoosing(double probOfLoosing) {
            this.probOfLoosing = probOfLoosing;
        }

        @Override
        public int compareTo(BoardModel o) {
            return Double.valueOf(Double.valueOf(o.getRelativeProbability())).compareTo(this.getRelativeProbability());
        }

        @Override
        public String toString() {
            return String.format("%-15s%-15s%-10s%-15s%-15s%-15s%.3f\n", currentUnsafePipCount + 1, resultingUnsafePipCount + 1, trueOrFalse(isaHit), trueOrFalse(breaksPrime), trueOrFalse(createsPrime), trueOrFalse(fromHome), getRelativeProbability());
        }

        private String trueOrFalse(boolean bool) {
            return bool ? "T" : "F";
        }
    }

}
