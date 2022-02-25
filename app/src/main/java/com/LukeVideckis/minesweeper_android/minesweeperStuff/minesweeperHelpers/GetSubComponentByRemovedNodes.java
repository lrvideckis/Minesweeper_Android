package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class GetSubComponentByRemovedNodes {
	public static ArrayList<SortedSet<Integer>> getSubComponentByRemovedNodes(
			SortedSet<Integer> nodes,
			ArrayList<SortedSet<Integer>> adjList,
			boolean[] isRemoved
	) throws Exception {
		final boolean[] visited = new boolean[isRemoved.length];
		for (int i = 0; i < isRemoved.length; ++i) {
			visited[i] = false;
		}
		ArrayList<SortedSet<Integer>> newSubComponents = new ArrayList<>();
		for (int node : nodes) {
			if (isRemoved[node]) {
				continue;
			}
			SortedSet<Integer> currSubComponent = getSubComponent(node, isRemoved, visited, nodes, adjList);
			if (currSubComponent.isEmpty()) {
				continue;
			}
			newSubComponents.add(currSubComponent);
		}
		return newSubComponents;
	}

	//if called twice on the same component, this will return an empty array
	private static SortedSet<Integer> getSubComponent(
			final int startNode,
			boolean[] isRemoved,
			boolean[] visited,
			SortedSet<Integer> nodes,
			ArrayList<SortedSet<Integer>> adjList
	) throws Exception {
		if (isRemoved[startNode]) {
			throw new Exception("start node is removed");
		}
		if (!nodes.contains(startNode)) {
			throw new Exception("start node isn't in list of nodes");
		}
		TreeSet<Integer> component = new TreeSet<>();
		if (visited[startNode]) {
			return Collections.unmodifiableSortedSet(component);
		}
		dfs(startNode, component, nodes, visited, isRemoved, adjList);
		for (int node : component) {
			if (isRemoved[node]) {
				visited[node] = false;
			}
		}
		return Collections.unmodifiableSortedSet(component);
	}

	private static void dfs(
			final int node,
			TreeSet<Integer> component,
			SortedSet<Integer> nodes,
			boolean[] visited,
			boolean[] isRemoved,
			ArrayList<SortedSet<Integer>> adjList
	) {
		component.add(node);
		visited[node] = true;
		if (isRemoved[node]) {
			return;
		}
		for (int to : adjList.get(node)) {
			if (nodes.contains(to) && !visited[to]) {
				dfs(to, component, nodes, visited, isRemoved, adjList);
			}
		}
	}
}
