package dev.jvmguard.agent.tree;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TreeMergeSplitTest {

    private static AgentTransactionTree child(AgentTransactionTree parent, String name, long count, long time) {
        AgentTransactionTree template = new AgentTransactionTree(null, AgentTransactionInfo.create(name, 1, 0), "policy");
        AgentTransactionTree node = parent.getOrCreateChild(template);
        node.addCount(count);
        node.addTime(time);
        return node;
    }

    private static Map<String, long[]> collect(AgentTransactionTree root) {
        Map<String, long[]> map = new HashMap<>();
        root.visit(new Tree.Visitor<>() {
            @Override
            public boolean preVisit(AgentTransactionTree tree) {
                if (tree.getInfo() != null) {
                    map.put(tree.getName(), new long[] {tree.getCount(), tree.getTime()});
                }
                return true;
            }

            @Override
            public void postVisit(AgentTransactionTree tree) {
            }
        });
        return map;
    }

    @Test
    void mergeSumsCountsAndTimesAcrossTrees() throws Exception {
        AgentTransactionTree first = new AgentTransactionTree();
        AgentTransactionTree a1 = child(first, "a", 1, 100);
        child(a1, "b", 2, 200);

        AgentTransactionTree second = new AgentTransactionTree();
        AgentTransactionTree a2 = child(second, "a", 10, 1000);
        child(a2, "b", 20, 2000);
        child(a2, "c", 3, 300);

        MergeVisitor visitor = new MergeVisitor(true);
        visitor.init(first);
        first.visit(visitor);
        visitor.init(second);
        second.visit(visitor);

        Map<String, long[]> merged = collect(visitor.getResult());
        assertArrayEquals(new long[]{11, 1100}, merged.get("a"));
        assertArrayEquals(new long[]{22, 2200}, merged.get("b"));
        assertArrayEquals(new long[]{3, 300}, merged.get("c"));
    }

    @Test
    void mergeWithStopAtZeroSkipsEmptySubtrees() throws Exception {
        AgentTransactionTree source = new AgentTransactionTree();
        AgentTransactionTree empty = child(source, "empty", 0, 0);
        child(empty, "nested", 1, 100);

        MergeVisitor visitor = new MergeVisitor(true);
        visitor.init(source);
        source.visit(visitor);

        Map<String, long[]> merged = collect(visitor.getResult());
        assertNull(merged.get("empty"));
        assertNull(merged.get("nested"), "children of empty nodes are skipped too");
    }

    @Test
    void splitMovesDataAndClearsSource() throws Exception {
        AgentTransactionTree root = new AgentTransactionTree();
        AgentTransactionTree a = child(root, "a", 1, 100);
        AgentTransactionTree b = child(a, "b", 2, 200);

        AgentTransactionTree destination = new AgentTransactionTree();
        a.visit(new PolicySplitVisitor().init(destination));

        assertEquals(1, destination.getCount());
        assertEquals(100, destination.getTime());
        Map<String, long[]> moved = collect(destination);
        assertArrayEquals(new long[]{2, 200}, moved.get("b"));

        assertEquals(0, a.getCount());
        assertEquals(0, a.getTime());
        assertEquals(0, b.getCount());
        assertEquals(0, b.getTime());
    }
}
