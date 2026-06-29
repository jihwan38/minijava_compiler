package RegAlloc;

import Graph.Node;
import Graph.NodeList;
import Temp.Temp;
import Temp.TempList;
import Temp.TempMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph-coloring allocator with George/Briggs Coalescing and Loop-Aware Spilling.
 */
public class Color implements TempMap {
  private final InterferenceGraph ig;
  private final TempMap initial;
  private final List<String> registerNames;
  private final Map<Temp, String> assign = new HashMap<>();
  private TempList spills;
  private final Map<Node, Node> aliasMap = new HashMap<>();

  public Color(InterferenceGraph ig, TempMap initial, TempList registers, Map<Temp, Double> weights) {
    this.ig = ig;
    this.initial = initial;
    this.registerNames = toRegisterNames(initial, registers);
    color(weights);
  }

  /** Returns the list of spilled temps (may be null). */
  public TempList spills() {
    return spills;
  }

  @Override
  public String tempMap(Temp t) {
    String a = assign.get(t);
    if (a != null) return a;
    return initial == null ? null : initial.tempMap(t);
  }

  private Node getAlias(Node n) {
    Node current = n;
    while (aliasMap.containsKey(current)) {
      current = aliasMap.get(current);
    }
    return current;
  }

  public Temp getAliasTemp(Temp t) {
    Node n = ig.tnode(t);
    if (n == null) return t;
    Node rep = getAlias(n);
    Temp repTemp = ig.gtemp(rep);
    return repTemp != null ? repTemp : t;
  }

  private static class CoalescedGraph {
    final Map<Node, Set<Node>> adjList = new HashMap<>();
    
    CoalescedGraph(List<Node> allNodes, Set<Node> precolored, Color colorObj) {
      for (Node n : allNodes) {
        adjList.put(n, new HashSet<>());
      }
      for (Node u : allNodes) {
        Node uRep = colorObj.getAlias(u);
        for (NodeList adj = u.succ(); adj != null; adj = adj.tail) {
          Node v = adj.head;
          Node vRep = colorObj.getAlias(v);
          if (uRep != vRep) {
            adjList.get(uRep).add(vRep);
            adjList.get(vRep).add(uRep);
          }
        }
      }
    }
    
    int degree(Node n) {
      return adjList.containsKey(n) ? adjList.get(n).size() : 0;
    }
    
    Set<Node> adj(Node n) {
      return adjList.getOrDefault(n, new HashSet<>());
    }
    
    boolean interferes(Node u, Node v) {
      return adjList.containsKey(u) && adjList.get(u).contains(v);
    }
  }

  private boolean checkGeorge(CoalescedGraph cg, Node nonPre, Node pre, Set<Node> precolored, int K) {
    for (Node t : cg.adj(nonPre)) {
      if (precolored.contains(t)) continue;
      if (cg.interferes(t, pre)) continue;
      if (cg.degree(t) < K) continue;
      return false;
    }
    return true;
  }

  private boolean checkBriggs(CoalescedGraph cg, Node u, Node v, int K) {
    int significantDegreeCount = 0;
    Set<Node> unionNeighbors = new HashSet<>(cg.adj(u));
    unionNeighbors.addAll(cg.adj(v));
    for (Node t : unionNeighbors) {
      if (cg.degree(t) >= K) {
        significantDegreeCount++;
      }
    }
    return significantDegreeCount < K;
  }

  private void color(Map<Temp, Double> weights) {
    List<Node> allNodes = new ArrayList<>();
    for (NodeList nl = ig.nodes(); nl != null; nl = nl.tail) {
      allNodes.add(nl.head);
    }

    // Identify precolored nodes
    Set<Node> precolored = new HashSet<>();
    for (Node n : allNodes) {
      Temp t = ig.gtemp(n);
      if (t != null && initial != null) {
        String name = initial.tempMap(t);
        if (name != null) {
          precolored.add(n);
          assign.put(t, name); // record fixed color
        }
      }
    }

    int K = registerNames.size();

    // 1. Coalescing Loop using George and Briggs
    boolean coalescedAny;
    do {
      coalescedAny = false;
      CoalescedGraph cg = new CoalescedGraph(allNodes, precolored, this);
      
      for (MoveList ml = ig.moves(); ml != null; ml = ml.tail) {
        Node u = getAlias(ml.src);
        Node v = getAlias(ml.dst);
        
        if (u == v) continue;
        if (cg.interferes(u, v)) continue;
        
        boolean uPre = precolored.contains(u);
        boolean vPre = precolored.contains(v);
        
        if (uPre && vPre) continue; // both precolored
        
        boolean safe = false;
        if (uPre || vPre) {
          Node pre = uPre ? u : v;
          Node nonPre = uPre ? v : u;
          safe = checkGeorge(cg, nonPre, pre, precolored, K);
          if (safe) {
            aliasMap.put(nonPre, pre);
          }
        } else {
          safe = checkBriggs(cg, u, v, K);
          if (safe) {
            aliasMap.put(v, u);
          }
        }
        
        if (safe) {
          coalescedAny = true;
          break; // rebuild graph and scan again
        }
      }
    } while (coalescedAny);

    // Build the final coalesced graph for active nodes
    CoalescedGraph cg = new CoalescedGraph(allNodes, precolored, this);
    List<Node> activeNodes = new ArrayList<>();
    for (Node n : allNodes) {
      if (getAlias(n) == n) {
        activeNodes.add(n);
      }
    }

    Map<Node, Integer> degree = new HashMap<>();
    for (Node n : activeNodes) {
      degree.put(n, cg.degree(n));
    }

    Set<Node> removed = new HashSet<>();
    Deque<Node> stack = new ArrayDeque<>();

    // Worklist: active nodes with degree < K and not precolored
    List<Node> worklist = new ArrayList<>();
    for (Node n : activeNodes) {
      if (!precolored.contains(n) && degree.get(n) < K) {
        worklist.add(n);
      }
    }

    // Simplify phase
    while (removed.size() < activeNodes.size()) {
      Node pick = null;
      if (!worklist.isEmpty()) {
        pick = worklist.remove(worklist.size() - 1); // pop
      } else {
        // No low-degree nodes: pick non-precolored, non-removed node with lowest loop-aware spill priority
        double minPriority = Double.MAX_VALUE;
        for (Node n : activeNodes) {
          if (!removed.contains(n) && !precolored.contains(n)) {
            double w = weights.getOrDefault(ig.gtemp(n), 0.0);
            int deg = degree.getOrDefault(n, 0);
            double priority = w / (deg + 1); // avoid division by zero
            if (priority < minPriority) {
              minPriority = priority;
              pick = n;
            }
          }
        }
        if (pick == null) {
          break; // only precolored nodes remain
        }
      }

      stack.push(pick);
      removed.add(pick);

      // Decrement neighbor degrees and add newly-low-degree neighbors to worklist
      for (Node m : cg.adj(pick)) {
        if (!removed.contains(m)) {
          int d = degree.getOrDefault(m, 0);
          if (d > 0) degree.put(m, d - 1);
          if (!precolored.contains(m) && degree.get(m) < K && !worklist.contains(m)) {
            worklist.add(m);
          }
        }
      }
    }

    // Select phase: assign colors while popping from stack
    Set<Temp> spilledReps = new HashSet<>();
    while (!stack.isEmpty()) {
      Node n = stack.pop();
      Temp t = ig.gtemp(n);
      if (t == null) continue;
      Set<String> forbidden = new HashSet<>();
      
      for (Node m : cg.adj(n)) {
        Temp mt = ig.gtemp(m);
        if (mt != null) {
          String cname = assign.get(mt);
          if (cname == null && initial != null) cname = initial.tempMap(mt);
          if (cname != null) forbidden.add(cname);
        }
      }

      String chosen = chooseColor(forbidden);
      if (chosen != null) {
        assign.put(t, chosen);
      } else {
        spilledReps.add(t);
      }
    }

    // Propagate colors/spills to original nodes
    for (Node n : allNodes) {
      Temp t = ig.gtemp(n);
      if (t != null) {
        Node rep = getAlias(n);
        Temp repTemp = ig.gtemp(rep);
        if (repTemp != null && spilledReps.contains(repTemp)) {
          spills = new TempList(t, spills);
        } else if (repTemp != null) {
          String cname = assign.get(repTemp);
          if (cname != null) {
            assign.put(t, cname);
          }
        }
      }
    }
  }

  private String chooseColor(Set<String> forbidden) {
    for (String name : registerNames) {
      if (!forbidden.contains(name)) return name;
    }
    return null;
  }

  private static List<String> toRegisterNames(TempMap initial, TempList regs) {
    List<String> names = new ArrayList<>();
    for (TempList p = regs; p != null; p = p.tail) {
      Temp r = p.head;
      if (r != null) {
        String nm = initial == null ? null : initial.tempMap(r);
        if (nm != null) names.add(nm);
      }
    }
    return names;
  }
}

