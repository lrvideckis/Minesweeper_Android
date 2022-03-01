package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.miscHelpers.ComparablePair;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class GetConnectedComponents {

    public static Pair<ArrayList<ArrayList<Pair<Integer, Integer>>>, ArrayList<ArrayList<SortedSet<Integer>>>> getComponentsWithKnownCells(Board<TileWithLogistics> board) throws Exception {
        Dsu disjointSet = new Dsu(board.getRows() * board.getCols());
        boolean[][] unknownStatusSpot = new boolean[board.getRows()][board.getCols()];
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (!board.getCell(i, j).isVisible) {
                    continue;
                }
                for (int[] adj : board.getAdjacentIndexes(i, j)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    TileWithLogistics adjTile = board.getCell(adjI, adjJ);
                    if (adjTile.isVisible || adjTile.isLogicalMine || adjTile.isLogicalFree) {
                        continue;
                    }
                    disjointSet.merge(RowColToIndex.rowColToIndex(i, j, board.getRows(), board.getCols()), RowColToIndex.rowColToIndex(adjI, adjJ, board.getRows(), board.getCols()));
                    unknownStatusSpot[adjI][adjJ] = true;
                }
            }
        }
        boolean[][] visited = new boolean[board.getRows()][board.getCols()];
        ArrayList<ArrayList<Pair<Integer, Integer>>> components = new ArrayList<>();
        ArrayList<ArrayList<TreeSet<Integer>>> mutableAdjList = new ArrayList<>();
        ComparablePair[][] rowColToComponent = new ComparablePair[board.getRows()][board.getCols()];
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (visited[i][j] || !unknownStatusSpot[i][j]) {
                    continue;
                }
                ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
                dfs(board, i, j, component, visited, unknownStatusSpot, disjointSet.find(RowColToIndex.rowColToIndex(i, j, board.getRows(), board.getCols())), disjointSet, rowColToComponent, components.size());
                components.add(component);
                ArrayList<TreeSet<Integer>> currAdjList = new ArrayList<>(component.size());
                for (int k = 0; k < component.size(); ++k) {
                    currAdjList.add(new TreeSet<>());
                }
                mutableAdjList.add(currAdjList);
            }
        }

        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (!board.getCell(i, j).isVisible) {
                    continue;
                }
                for (int[] adj1 : board.getAdjacentIndexes(i, j)) {
                    TileWithLogistics adjTile1 = board.getCell(adj1[0], adj1[1]);
                    if (adjTile1.isVisible || adjTile1.isLogicalMine || adjTile1.isLogicalFree) {
                        continue;
                    }
                    for (int[] adj2 : board.getAdjacentIndexes(i, j)) {
                        if (adj1[0] == adj2[0] && adj1[1] == adj2[1]) {
                            continue;
                        }
                        TileWithLogistics adjTile2 = board.getCell(adj2[0], adj2[1]);
                        if (adjTile2.isVisible || adjTile2.isLogicalMine || adjTile2.isLogicalFree) {
                            continue;
                        }
                        //add edge
                        final ComparablePair node1 = rowColToComponent[adj1[0]][adj1[1]];
                        final ComparablePair node2 = rowColToComponent[adj2[0]][adj2[1]];
                        if (!node1.first.equals(node2.first)) {
                            throw new Exception("edge between 2 nodes not in the same component");
                        }
                        if (node1.second.equals(node2.second)) {
                            throw new Exception("2 different squares have the same index which shouldn't happen");
                        }
                        mutableAdjList.get(node1.first).get(node1.second).add(node2.second);
                        mutableAdjList.get(node2.first).get(node2.second).add(node1.second);
                    }
                }
            }
        }

        //convert adjList to non-modifiable sorted set
        ArrayList<ArrayList<SortedSet<Integer>>> adjList = new ArrayList<>(mutableAdjList.size());
        for (int i = 0; i < mutableAdjList.size(); ++i) {
            ArrayList<SortedSet<Integer>> currRow = new ArrayList<>(mutableAdjList.get(i).size());
            for (int j = 0; j < mutableAdjList.get(i).size(); ++j) {
                currRow.add(Collections.unmodifiableSortedSet(mutableAdjList.get(i).get(j)));
            }
            adjList.add(currRow);
        }

        return new Pair<>(components, adjList);
    }

    private static void dfs(
            final Board<TileWithLogistics> board,
            final int i,
            final int j,
            ArrayList<Pair<Integer, Integer>> component,
            boolean[][] visited,
            final boolean[][] unknownStatusSpot,
            final int ccDsuParent,
            final Dsu disjointSet,
            ComparablePair[][] rowColToComponent,
            final int componentId
    ) {
        rowColToComponent[i][j] = new ComparablePair(componentId, component.size());
        component.add(new Pair<>(i, j));
        visited[i][j] = true;
        for (int[] adj : board.getAdjacentIndexes(i, j)) {
            final int adjI = adj[0], adjJ = adj[1];
            if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccDsuParent != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, board.getRows(), board.getCols()))) {
                continue;
            }
            dfs(board, adjI, adjJ, component, visited, unknownStatusSpot, ccDsuParent, disjointSet, rowColToComponent, componentId);
        }
        for (int di = -2; di <= 2; ++di) {
            for (int dj = -2; dj <= 2; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (board.outOfBounds(adjI, adjJ)) {
                    continue;
                }
                if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccDsuParent != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, board.getRows(), board.getCols()))) {
                    continue;
                }
                dfs(board, adjI, adjJ, component, visited, unknownStatusSpot, ccDsuParent, disjointSet, rowColToComponent, componentId);
            }
        }
    }

    public static Dsu getDsuOfComponentsWithKnownMines(Board<TileWithLogistics> board) throws Exception {
        Dsu disjointSet = new Dsu(board.getRows() * board.getCols());
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (!board.getCell(i, j).isVisible) {
                    continue;
                }
                for (int[] adj : board.getAdjacentIndexes(i, j)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    TileWithLogistics adjTile = board.getCell(adjI, adjJ);
                    if (adjTile.isVisible || adjTile.isLogicalMine) {
                        continue;
                    }
                    disjointSet.merge(RowColToIndex.rowColToIndex(i, j, board.getRows(), board.getCols()), RowColToIndex.rowColToIndex(adjI, adjJ, board.getRows(), board.getCols()));
                }
            }
        }
        return disjointSet;
    }
}
