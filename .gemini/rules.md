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

## 7. Development Logging & Report Requirements
* **MUST record every workspace modification in `report_draft.md` in extreme detail (Development Log format):**
  - **Specific File Paths & Scope:** Always state the exact relative path of the modified/created/renamed files (e.g., `chap11/Temp/Label.java`), along with the class name, method name, or field name being modified.
  - **Precise Code Diffs (Line-by-Line):** Every code mutation must be documented in a strict `Before` vs `After` format. Do not summarize or omit lines; show the exact lines before the change and the exact lines after the change, including precise line numbers where the changes occurred.
  - **Comprehensive Troubleshooting Log:** Every single compilation warning, compilation error, runtime crash, logic bug, or shell error must be copied in its entirety (full stack trace, stderr, stdout). Document the root cause analysis, hypothetical solutions, and the step-by-step resolution process.
  - **Command Execution & Test Logs:** Document the exact command executed (e.g., compile commands, test runs) and copy-paste the raw, unfiltered stdout/stderr outputs of compiler tests, driver runs, and regression tests directly into the log as immutable execution proof.
  - **Action Log Chronology:** Maintain a strict chronological timeline of actions taken, ensuring that no file is modified or script is run without being recorded in the log immediately after execution.
  - **Meticulous AI Collaboration History:** Log the exact prompts fed to the AI assistant, the specific code or architecture ideas suggested by the AI, and the dynamic feedback loop/discussions between the human developer and the AI (e.g., design adjustments, code reviews, rejected suggestions) in complete detail.
  - **Detailed Module Design & Rationale:** For every compiler phase, document the precise design principles, class layouts, custom methods, data structures, and the exact algorithms used (e.g., George/Briggs coalescing rules, loop-aware spill heuristics). Explain how references (e.g., `translate_ref.txt`) were integrated, detailing any variations, improvements, or architectural adjustments made.
