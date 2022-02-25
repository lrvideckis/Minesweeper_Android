package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.Arrays;

public class Dsu {
    private final int[] parent;

    public Dsu(int size) {
        parent = new int[size];
        Arrays.fill(parent, -1);
    }

    public int find(int node) {
        if (parent[node] < 0) {
            return node;
        }
        return parent[node] = find(parent[node]);
    }

    public void merge(int x, int y) {
        if ((x = find(x)) == (y = find(y))) return;
        if (parent[y] < parent[x]) {
            int temp = x;
            //noinspection SuspiciousNameCombination
            x = y;
            y = temp;
        }
        parent[x] += parent[y];
        parent[y] = x;
    }
}
