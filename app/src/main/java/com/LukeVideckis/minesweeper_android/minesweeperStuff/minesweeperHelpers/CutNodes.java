package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class CutNodes {
    public static TreeSet<Integer> getCutNodes(SortedSet<Integer> nodes, ArrayList<SortedSet<Integer>> adjList, boolean[] isRemoved) throws Exception {
        //TODO: change these to Trees, for better complexity
        //initialize variables for finding cut nodes
        final int[] minTime = new int[isRemoved.length];
        final int[] timeIn = new int[isRemoved.length];
        final boolean[] visited = new boolean[isRemoved.length];
        for (int i = 0; i < isRemoved.length; ++i) {
            minTime[i] = timeIn[i] = isRemoved.length + 10;
            visited[i] = false;
        }
        final MutableInt currTime = new MutableInt(0);
        TreeSet<Integer> allCutNodes = new TreeSet<>();
        for (SortedSet<Integer> component : GetSubComponentByRemovedNodes.getSubComponentByRemovedNodes(nodes, adjList, isRemoved)) {
            Integer startNode = null;
            for (int node : component) {
                if (!isRemoved[node]) {
                    startNode = node;
                    break;
                }
            }
            if (startNode == null) {
                throw new Exception("each component should have at least 1 non-removed node");
            }
            for (int currCutNode : getCutNodesForASingleComponent(startNode, nodes, adjList, isRemoved, visited, timeIn, minTime, currTime, component)) {
                if (allCutNodes.contains(currCutNode)) {
                    throw new Exception("duplicate cut node");
                }
                allCutNodes.add(currCutNode);
            }
        }
        return allCutNodes;
    }

    //when calling this multiple times on the same component, this will only give correct results the first time
    //this is because I don't re-initialize the member variables for finding cut nodes
    //this returns an empty ArrayList when called more than once on the same component
    private static TreeSet<Integer> getCutNodesForASingleComponent(
            int startNode,
            SortedSet<Integer> nodes,
            ArrayList<SortedSet<Integer>> adjList,
            boolean[] isRemoved,
            boolean[] visited,
            int[] timeIn,
            int[] minTime,
            MutableInt currTime,
            SortedSet<Integer> component
    ) throws Exception {
        if (isRemoved[startNode]) {
            throw new Exception("start node is removed");
        }
        if (!nodes.contains(startNode)) {
            throw new Exception("start node isn't in list of nodes");
        }
        TreeSet<Integer> allCutNodes = new TreeSet<>();
        if (visited[startNode]) {
            return allCutNodes;
        }
        dfsCutNodes(startNode, startNode, allCutNodes, component, adjList, visited, timeIn, minTime, currTime);
        for (int node : component) {
            if (isRemoved[node]) {
                visited[node] = false;
            }
        }
        return allCutNodes;
    }

    private static void dfsCutNodes(
            final int node,
            final int prev,
            TreeSet<Integer> allCutNodes,
            SortedSet<Integer> component,
            ArrayList<SortedSet<Integer>> adjList,
            boolean[] visited,
            int[] timeIn,
            int[] minTime,
            MutableInt currTime
    ) throws Exception {
        if (!component.contains(node)) {
            throw new Exception("component doesn't contain node");
        }
        visited[node] = true;
        currTime.addWith(1);
        timeIn[node] = minTime[node] = currTime.get();
        int numChildren = 0;
        for (int to : adjList.get(node)) {
            if (!component.contains(to)) {
                continue;
            }
            if (to != prev) {
                minTime[node] = Math.min(minTime[node], timeIn[to]);
            }
            if (visited[to]) continue;
            numChildren++;
            if (node == prev && numChildren > 1) {
                allCutNodes.add(node);
            }
            dfsCutNodes(to, node, allCutNodes, component, adjList, visited, timeIn, minTime, currTime);
            minTime[node] = Math.min(minTime[node], minTime[to]);
            if (node != prev && minTime[to] >= timeIn[node]) {
                allCutNodes.add(node);
            }
        }
    }
}
