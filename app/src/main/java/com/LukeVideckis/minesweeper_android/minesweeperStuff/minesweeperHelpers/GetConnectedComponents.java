package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.miscHelpers.MyPair;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class GetConnectedComponents {
    private static int rows, cols;

    public static Pair<ArrayList<ArrayList<Pair<Integer, Integer>>>, ArrayList<ArrayList<SortedSet<Integer>>>> getComponentsWithKnownCells(VisibleTile[][] board) throws Exception {
        Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
        rows = dimensions.first;
        cols = dimensions.second;
        Dsu disjointSet = new Dsu(rows * cols);
        boolean[][] unknownStatusSpot = new boolean[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (!board[i][j].getIsVisible()) {
                    continue;
                }
                for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    VisibleTile adjTile = board[adjI][adjJ];
                    if (adjTile.getIsVisible() || adjTile.getIsLogicalMine() || adjTile.getIsLogicalFree()) {
                        continue;
                    }
                    disjointSet.merge(RowColToIndex.rowColToIndex(i, j, rows, cols), RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols));
                    unknownStatusSpot[adjI][adjJ] = true;
                }
            }
        }
        boolean[][] visited = new boolean[rows][cols];
        ArrayList<ArrayList<Pair<Integer, Integer>>> components = new ArrayList<>();
        ArrayList<ArrayList<TreeSet<Integer>>> mutableAdjList = new ArrayList<>();
        MyPair[][] rowColToComponent = new MyPair[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (visited[i][j] || !unknownStatusSpot[i][j]) {
                    continue;
                }
                ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
                dfs(i, j, component, visited, unknownStatusSpot, disjointSet.find(RowColToIndex.rowColToIndex(i, j, rows, cols)), disjointSet, rowColToComponent, components.size());
                components.add(component);
                ArrayList<TreeSet<Integer>> currAdjList = new ArrayList<>(component.size());
                for (int k = 0; k < component.size(); ++k) {
                    currAdjList.add(new TreeSet<>());
                }
                mutableAdjList.add(currAdjList);
            }
        }

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (!board[i][j].getIsVisible()) {
                    continue;
                }
                for (int[] adj1 : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                    final VisibleTile adjTile1 = board[adj1[0]][adj1[1]];
                    if (adjTile1.getIsVisible() || adjTile1.getIsLogicalMine() || adjTile1.getIsLogicalFree()) {
                        continue;
                    }
                    for (int[] adj2 : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                        if (adj1[0] == adj2[0] && adj1[1] == adj2[1]) {
                            continue;
                        }
                        final VisibleTile adjTile2 = board[adj2[0]][adj2[1]];
                        if (adjTile2.getIsVisible() || adjTile2.getIsLogicalMine() || adjTile2.getIsLogicalFree()) {
                            continue;
                        }
                        //add edge
                        final MyPair node1 = rowColToComponent[adj1[0]][adj1[1]];
                        final MyPair node2 = rowColToComponent[adj2[0]][adj2[1]];
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
            final int i,
            final int j,
            ArrayList<Pair<Integer, Integer>> component,
            boolean[][] visited,
            final boolean[][] unknownStatusSpot,
            final int ccDsuParent,
            final Dsu disjointSet,
            MyPair[][] rowColToComponent,
            final int componentId
    ) {
        rowColToComponent[i][j] = new MyPair(componentId, component.size());
        component.add(new Pair<>(i, j));
        visited[i][j] = true;
        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
            final int adjI = adj[0], adjJ = adj[1];
            if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccDsuParent != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols))) {
                continue;
            }
            dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccDsuParent, disjointSet, rowColToComponent, componentId);
        }
        for (int di = -2; di <= 2; ++di) {
            for (int dj = -2; dj <= 2; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
                    continue;
                }
                if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccDsuParent != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols))) {
                    continue;
                }
                dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccDsuParent, disjointSet, rowColToComponent, componentId);
            }
        }
    }

    public static Dsu getDsuOfComponentsWithKnownMines(VisibleTile[][] board) throws Exception {
        Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
        rows = dimensions.first;
        cols = dimensions.second;
        Dsu disjointSet = new Dsu(rows * cols);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (!board[i][j].getIsVisible()) {
                    continue;
                }
                for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    VisibleTile adjTile = board[adjI][adjJ];
                    if (adjTile.getIsVisible() || adjTile.getIsLogicalMine()) {
                        continue;
                    }
                    disjointSet.merge(RowColToIndex.rowColToIndex(i, j, rows, cols), RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols));
                }
            }
        }
        return disjointSet;
    }
}
