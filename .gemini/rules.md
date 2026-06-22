# MiniJava-to-MIPS Compiler Development Rules

## 1. Safety & Workspace Control
* **MUST NOT execute commands or modify files without explicit permission:** Do not run background commands or mutate the workspace automatically. Only perform actions when specifically asked to do so.

## 2. JavaCC & Parsing Rules
* **MUST verify JavaCC syntax before answering:** Always search the web or consult documentation to confirm the exact JavaCC syntax and rules before providing any instructions or code snippets related to JavaCC.

## 3. Strict Adherence to References & Optimization Strategy
* **MUST thoroughly reference `.gemini/reference/` contents:** Always consult `translate_ref.txt`, `codegen_ref.txt`, and `regalloc_ref.txt` before any code implementation.
* **MUST strictly implement the bonus points (+5) optimizations:**
  - **Coalescing:** Implement safe register coalescing using George/Briggs strategy inside the graph coloring algorithm.
  - **Loop-Aware Spill Priority:** Use the formula `(DefUse outside loop + 10 * DefUse inside loop) / degree` to rank spill candidates.

## 4. Test-Driven Development (TDD) & Correctness
* **MUST utilize TDD and automated testing:** Verify correctness after implementing each module. Always run the automated testing script (`test_all.ps1`) to check compiler output correctness against MARS outputs.

## 5. Directory & Package Consistency
* **MUST maintain the unified `chap11` directory structure:** Keep all packages under `chap11/` (`syntaxtree`, `visitor`, `Mips`, `Tree`, `Canon`, `Assem`, `FlowGraph`, `RegAlloc`, `Temp`, `Util`).
* **MUST maintain naming consistency:** Follow standard Java/Appel compiler terminology (`Temp`, `Label`, `Access`, `InFrame`, `InReg`, etc.) and MIPS architectural naming conventions (`$zero`, `$v0`, `$a0-$a3`, `$t0-$t9`, `$s0-$s7`, `$sp`, `$fp`, `$ra`).

## 6. Software Architecture (SOLID Principles)
* **Single Responsibility Principle (SRP):** Keep compiler phases decoupled (e.g., parsing, type checking, IR translation, liveness analysis, register allocation should not pollute each other's state).
* **Open-Closed Principle (OCP):** Extend existing MIPS/RegAlloc templates gracefully without breaking baseline classes.
* **Liskov Substitution Principle (LSP):** Ensure any newly implemented `Access` or `Instr` classes can seamlessly substitute their abstract base parents.
