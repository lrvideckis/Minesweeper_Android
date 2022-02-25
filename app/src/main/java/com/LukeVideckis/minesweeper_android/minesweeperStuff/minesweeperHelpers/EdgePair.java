package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class EdgePair {
    public static Pair<MyPair, MyPair> getPairOfEdges(
            final SortedSet<Integer> subComponent,
            final int componentPos,
            final boolean[] isRemoved,
            final ArrayList<ArrayList<SortedSet<Integer>>> adjList
    ) throws Exception {
        TreeSet<MyPair> edges = new TreeSet<>();
        for (int node : subComponent) {
            if (isRemoved[node]) {
                continue;
            }
            for (int next : adjList.get(componentPos).get(node)) {
                if (!subComponent.contains(next) || isRemoved[next]) {
                    continue;
                }
                int u = node;
                int v = next;
                if (u > v) {
                    int temp = u;
                    u = v;
                    v = temp;
                }
                edges.add(new MyPair(u, v));
            }
        }
        Pair<MyPair, MyPair> edgePairWithSmallestLargestComponent = null;
        int smallestLargestComponentSize = (int) 1e9;
        for (MyPair edge1 : edges) {
            for (MyPair edge2 : edges) {
                if (isRemoved[edge1.first] ||
                        isRemoved[edge1.second] ||
                        isRemoved[edge2.first] ||
                        isRemoved[edge2.second]
                ) {
                    throw new Exception("edge with a removed node, but this was checked earlier");
                }
                isRemoved[edge1.first] = isRemoved[edge1.second] = isRemoved[edge2.first] = isRemoved[edge2.second] = true;
                TreeSet<Integer> visited = new TreeSet<>();
                int numberOfComponents = 0;
                int maxComponentSize = 0;
                for (int node : subComponent) {
                    if (isRemoved[node] || visited.contains(node)) {
                        continue;
                    }
                    ++numberOfComponents;
                    TreeSet<Integer> nodesInComponent = new TreeSet<>();
                    dfs(node, subComponent, componentPos, isRemoved, visited, adjList, nodesInComponent);
                    maxComponentSize = Math.max(maxComponentSize, nodesInComponent.size());
                }
                isRemoved[edge1.first] = isRemoved[edge1.second] = isRemoved[edge2.first] = isRemoved[edge2.second] = false;
                if (numberOfComponents > 1 && smallestLargestComponentSize > maxComponentSize) {
                    smallestLargestComponentSize = maxComponentSize;
                    edgePairWithSmallestLargestComponent = new Pair<>(edge1, edge2);
                }
            }
        }
        return edgePairWithSmallestLargestComponent;
    }

    private static void dfs(
            int node,
            final SortedSet<Integer> subComponent,
            final int componentPos,
            final boolean[] isRemoved,
            final TreeSet<Integer> visited,
            final ArrayList<ArrayList<SortedSet<Integer>>> adjList,
            final TreeSet<Integer> nodesInComponent
    ) {
        nodesInComponent.add(node);
        if (isRemoved[node]) {
            return;
        }
        visited.add(node);
        for (int next : adjList.get(componentPos).get(node)) {
            if (visited.contains(next) || !subComponent.contains(next)) {
                continue;
            }
            dfs(next, subComponent, componentPos, isRemoved, visited, adjList, nodesInComponent);
        }
    }
}
