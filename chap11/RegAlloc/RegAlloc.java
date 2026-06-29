package RegAlloc;

import Assem.InstrList;
import FlowGraph.AssemFlowGraph;
import FlowGraph.FlowGraph;
import FlowGraph.Liveness;
import Temp.Temp;
import Temp.TempList;
import Temp.TempMap;
import Temp.LabelList;
import Temp.Label;
import Mips.Frame;
import Mips.Access;
import Mips.InFrame;
import Assem.OPER;
import Assem.MOVE;
import Assem.LABEL;
import Assem.Targets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Register allocator front-end: builds interference, invokes Color.
 * Supports spilling and maps aliased variables to shared stack offsets.
 */
public class RegAlloc implements TempMap {
  public InstrList instrs;

  private final TempMap resultMap;
  private final TempList spills;
  private final Map<Temp, Integer> spillOffsets = new HashMap<>();

  public RegAlloc(Frame f, InstrList il) {
    InstrList current = il;
    TempMap finalMap = null;
    TempList finalSpills = null;
    TempList regs = allocatableRegs();

    final int maxIterations = 8;
    for (int iter = 0; iter < maxIterations; iter++) {
      FlowGraph fg = new AssemFlowGraph(current);
      Liveness live = new Liveness(fg);
      InterferenceGraph ig = live.interferenceGraph();

      // Compute loop-aware spill weights
      Map<Temp, Double> weights = computeSpillWeights(current);

      Color color = new Color(ig, Frame.regNameMap(), regs, weights);
      TempList roundSpills = color.spills();

      if (roundSpills == null) {
        finalMap = color;
        finalSpills = null;
        break;
      }

      current = rewriteForSpills(f, current, roundSpills, color);
      finalMap = color;
      finalSpills = roundSpills;
    }

    this.instrs = current;
    this.resultMap = finalMap;
    this.spills = finalSpills;
  }

  @Override
  public String tempMap(Temp temp) {
    return resultMap.tempMap(temp);
  }

  public TempList spills() {
    return spills;
  }

  private static TempList append(TempList a, TempList b) {
    if (a == null) return b;
    TempList head = a;
    while (a.tail != null) a = a.tail;
    a.tail = b;
    return head;
  }

  private static TempList allocatableRegs() {
    TempList regs = null;
    regs = append(regs, Frame.callerSaves());
    regs = append(regs, Frame.calleeSaves());
    regs = append(regs, Frame.argRegs());
    regs = new TempList(Frame.RV, regs);
    regs = new TempList(Frame.RV2, regs);
    return regs;
  }

  private Map<Temp, Double> computeSpillWeights(InstrList instrs) {
    List<Assem.Instr> list = new ArrayList<>();
    for (InstrList p = instrs; p != null; p = p.tail) {
      list.add(p.head);
    }

    Map<Label, Integer> labelIndices = new HashMap<>();
    for (int i = 0; i < list.size(); i++) {
      Assem.Instr ins = list.get(i);
      if (ins instanceof LABEL) {
        labelIndices.put(((LABEL) ins).label, i);
      }
    }

    List<int[]> loops = new ArrayList<>();
    for (int i = 0; i < list.size(); i++) {
      Assem.Instr ins = list.get(i);
      Targets jt = ins.jumps();
      if (jt != null && jt.labels != null) {
        for (LabelList ll = jt.labels; ll != null; ll = ll.tail) {
          Label lbl = ll.head;
          if (labelIndices.containsKey(lbl)) {
            int targetIdx = labelIndices.get(lbl);
            if (targetIdx < i) {
              loops.add(new int[]{targetIdx, i});
            }
          }
        }
      }
    }

    int[] depth = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      int d = 0;
      for (int[] loop : loops) {
        if (i >= loop[0] && i <= loop[1]) {
          d++;
        }
      }
      depth[i] = d;
    }

    Map<Temp, Double> weights = new HashMap<>();
    for (int i = 0; i < list.size(); i++) {
      Assem.Instr ins = list.get(i);
      int d = depth[i];
      double occurrenceWeight = (d == 0) ? 1.0 : 10.0 * d;

      TempList defs = ins.def();
      for (TempList p = defs; p != null; p = p.tail) {
        weights.put(p.head, weights.getOrDefault(p.head, 0.0) + occurrenceWeight);
      }
      TempList uses = ins.use();
      for (TempList p = uses; p != null; p = p.tail) {
        weights.put(p.head, weights.getOrDefault(p.head, 0.0) + occurrenceWeight);
      }
    }

    return weights;
  }

  private InstrList rewriteForSpills(Frame f, InstrList body, TempList spilled, Color color) {
    Set<Temp> spilledSet = toSet(spilled);
    ensureSpillSlots(f, spilledSet, color);

    List<Assem.Instr> rewritten = new ArrayList<>();
    for (InstrList p = body; p != null; p = p.tail) {
      Assem.Instr ins = p.head;
      rewriteInstr(ins, spilledSet, rewritten);
    }
    return fromList(rewritten);
  }

  private void rewriteInstr(Assem.Instr ins, Set<Temp> spilledSet, List<Assem.Instr> out) {
    if (ins instanceof LABEL) {
      LABEL l = (LABEL) ins;
      out.add(new LABEL(l.assem, l.label));
      return;
    }

    if (ins instanceof MOVE) {
      MOVE m = (MOVE) ins;
      Temp src = m.src;
      Temp dst = m.dst;

      Map<Temp, Temp> repl = new HashMap<>();
      if (spilledSet.contains(src)) {
        Temp r = new Temp();
        repl.put(src, r);
        out.add(loadFromSpill(src, r));
        src = r;
      }
      if (spilledSet.contains(dst)) {
        Temp r = repl.get(dst);
        if (r == null) {
          r = new Temp();
          repl.put(dst, r);
        }
        dst = r;
      }

      out.add(new MOVE(m.assem, dst, src));

      if (spilledSet.contains(m.dst)) {
        out.add(storeToSpill(m.dst, dst));
      }
      return;
    }

    if (ins instanceof OPER) {
      OPER o = (OPER) ins;
      Map<Temp, Temp> repl = new HashMap<>();
      Set<Temp> loaded = new HashSet<>();

      TempList newSrc = replaceList(o.src, spilledSet, repl, loaded, out, true);
      TempList newDst = replaceList(o.dst, spilledSet, repl, loaded, out, false);

      LabelList labels = null;
      Targets jt = o.jumps();
      if (jt != null) labels = jt.labels;
      out.add(new OPER(o.assem, newDst, newSrc, labels));

      LinkedHashSet<Temp> defsToStore = spilledDefs(o.dst, spilledSet);
      for (Temp t : defsToStore) {
        Temp r = repl.get(t);
        if (r != null) out.add(storeToSpill(t, r));
      }
      return;
    }

    out.add(ins);
  }

  private TempList replaceList(TempList list,
                               Set<Temp> spilledSet,
                               Map<Temp, Temp> repl,
                               Set<Temp> loaded,
                               List<Assem.Instr> out,
                               boolean forUse) {
    if (list == null) return null;
    List<Temp> temps = new ArrayList<>();
    for (TempList p = list; p != null; p = p.tail) {
      Temp t = p.head;
      if (spilledSet.contains(t)) {
        Temp r = repl.get(t);
        if (r == null) {
          r = new Temp();
          repl.put(t, r);
        }
        if (forUse && !loaded.contains(t)) {
          out.add(loadFromSpill(t, r));
          loaded.add(t);
        }
        temps.add(r);
      } else {
        temps.add(t);
      }
    }
    return toTempList(temps);
  }

  private LinkedHashSet<Temp> spilledDefs(TempList defs, Set<Temp> spilledSet) {
    LinkedHashSet<Temp> set = new LinkedHashSet<>();
    for (TempList p = defs; p != null; p = p.tail) {
      Temp t = p.head;
      if (spilledSet.contains(t)) set.add(t);
    }
    return set;
  }

  private OPER loadFromSpill(Temp spilledTemp, Temp into) {
    int off = spillOffsets.get(spilledTemp);
    return new OPER("lw `d0, " + off + "(`s0)\n",
        new TempList(into, null),
        new TempList(Frame.FP, null));
  }

  private OPER storeToSpill(Temp spilledTemp, Temp from) {
    int off = spillOffsets.get(spilledTemp);
    return new OPER("sw `s0, " + off + "(`s1)\n",
        null,
        new TempList(from, new TempList(Frame.FP, null)));
  }

  private void ensureSpillSlots(Frame f, Set<Temp> spilledSet, Color color) {
    for (Temp t : spilledSet) {
      Temp aliasT = color.getAliasTemp(t);
      if (!spillOffsets.containsKey(aliasT)) {
        Access a = f.allocLocal(true);
        if (!(a instanceof InFrame)) {
          throw new RuntimeException("Expected InFrame for spilled temp");
        }
        spillOffsets.put(aliasT, ((InFrame) a).offset);
      }
      spillOffsets.put(t, spillOffsets.get(aliasT));
    }
  }

  private static Set<Temp> toSet(TempList list) {
    Set<Temp> set = new HashSet<>();
    for (TempList p = list; p != null; p = p.tail) {
      set.add(p.head);
    }
    return set;
  }

  private static TempList toTempList(List<Temp> temps) {
    TempList list = null;
    for (int i = temps.size() - 1; i >= 0; i--) {
      list = new TempList(temps.get(i), list);
    }
    return list;
  }

  private static InstrList fromList(List<Assem.Instr> instrs) {
    InstrList head = null;
    InstrList tail = null;
    for (Assem.Instr ins : instrs) {
      InstrList node = new InstrList(ins, null);
      if (head == null) {
        head = tail = node;
      } else {
        tail.tail = node;
        tail = node;
      }
    }
    return head;
  }
}
