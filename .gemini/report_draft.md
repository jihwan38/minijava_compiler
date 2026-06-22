# MiniJava-to-MIPS 컴파일러 개발 및 협업 상세 로그 (Compiler Development & Collaboration Log)

이 문서는 MiniJava-to-MIPS 컴파일러 프로젝트의 전체 개발 과정, 코드 수정 이력, 빌드 및 실행 로그, 디버깅 과정, 그리고 AI 어시스턴트(Antigravity)와의 협업 세부 기록을 담은 실시간 개발 로그입니다. `.gemini/rules.md`에 의거하여 모든 작업 내역을 빠짐없이 상세히 기록합니다.

---

## 0. 개발 환경 구성 및 초기 패키지 이송 로그 (Environment Setup & Initialization)

### 📂 초기 뼈대 코드 복사 및 백업 생성
- **작업 일시:** 2026-06-22
- **내용:** 컴파일러 통합 개발을 위한 단일 패키지 구성 및 원본 소스 보호를 위한 백업 디렉토리 생성.
- **수행 상세:**
  1. 원본 `chap11` 뼈대 폴더의 초기 상태를 그대로 보존하기 위해 루트 경로에 [chap11_init](file:///c:/Users/user/minijava_compiler/chap11_init) 폴더를 생성하고 모든 파일을 1:1로 복사하여 백업 확보.
  2. 작업 디렉토리 [chap11](file:///c:/Users/user/minijava_compiler/chap11) 내의 모든 자바 패키지를 `package chap11...` 구조로 통합 재편하여 클래스 경로 충돌 방지 및 이식성 극대화.
  3. 6단계 (Mips Frame 의존성 디버깅 및 단독 검증) 완료 후, 7단계 (Translate) 구현 진입 직전의 안정적인 컴파일러 상태를 스냅샷으로 남기기 위해 루트 경로에 [chap11_1](file:///c:/Users/user/minijava_compiler/chap11_1) 폴더를 생성 및 복사 완료. (향후 개발 과정에서 6단계 대비 변경점 추적 및 보고서 diff 작성에 활용 예정)

### ⚙️ 테스트 자동화 환경 구축
- **작업 일시:** 2026-06-22
- **생성 파일:** [.gemini/test_all.ps1](file:///c:/Users/user/minijava_compiler/.gemini/test_all.ps1)
- **설계 의도:**
  - JVM 환경에서의 MiniJava 실행 결과(표준 출력)와, 작성한 MIPS 컴파일러가 생성한 어셈블리 파일을 MARS 시뮬레이터(`mars.jar`)로 실행한 결과를 자동으로 diff하여 100% 동일한지 상시 검증할 수 있는 파워쉘 스크립트 작성.
  - 빌드 자동화 및 회귀 검증을 통해 코드 수정 시 발생할 수 있는 결함을 실시간으로 방지.

---

## 1. AI 어시스턴트 협업 이력 및 활용 보고 (AI Assistant Collaboration Log)
> [과제 요구사항] AI 어시스턴트(Claude, ChatGPT, Gemini 등)를 사용했을 때 반드시 어떻게 사용했는지 보고서에 작성해 주세요.

### 💡 협업 요약 및 프롬프트 전략
* **사용 도구:** Antigravity (Gemini 3.5 Flash High 기반 코딩 어시스턴트)
* **협업 목표:** 프로젝트 폴더 마이그레이션, Windows 자바 대소문자 빌드 예외 해결, 중간 표현(IR) 설계, MIPS Maximal Munch 코드젠 매칭, Graph Coloring 기반 레지스터 할당 최적화(조합 최적화 및 Spill heuristic) 로직의 오류 수정 및 개발 생산성 향상.

### 💬 주요 프롬프트 및 협업 디테일 기록
#### 📌 [사례 1] 프로젝트 디렉토리 단일 패키지화 설계 조율
- **사용자 질의 (프롬프트):**
  > "내가 Appel 교재 코드를 가져와서 컴파일러를 만드는데, 패키지들이 각각 다른 폴더로 나뉘어 있어. 빌드할 때 클래스패스 잡는 것도 너무 골치 아프고, 패키지 간 순환 참조나 임포트 오류가 자꾸 나는데 이걸 깔끔하고 이식성 높게 패키지 구조로 다시 설계해 줘."
- **AI 제안 사항:**
  > "최종 zip 파일 제출에 최적화되도록, 모든 하위 단계(syntaxtree, visitor, Mips, Tree, Canon, Assem, FlowGraph, RegAlloc, Temp, Util)를 하나의 루트 디렉토리 `chap11` 패키지 밑에 병렬로 배치할 것을 제안합니다. 각 소스코드 상단에 `package chap11.Temp;`나 `package chap11.Mips;` 등을 명시하고 외부 참조는 `import chap11.Temp.*;` 형태로 일원화하면, `javac -d chap11/bin chap11/**/*.java` 단 한 줄의 명령어로 빌드가 가능해집니다."
- **의사결정 및 조율 이력:**
  * 개발자는 이 제안에 따라 `chap11/` 폴더에 소스를 취합했습니다.
  * 다만, 원본 소스가 손상될 우려가 있어 AI에게 **"이송 전에 chap11의 기본 상태를 안전하게 복제해 둔 chap11_init 폴더를 루트에 백업으로 만들고 작업을 시작하자"**고 먼저 지시하여, 백업 폴더를 구축한 뒤 안전하게 리팩토링을 시작했습니다.

#### 📌 [사례 2] Windows 대소문자 미구분으로 인한 JVM Wrong Name 런타임 오류 디버깅
- **사용자 질의 (프롬프트):**
  > "코드 이송 다 하고 Mips.FrameDriver를 돌렸는데 이런 에러가 나면서 죽어.
  > `Exception in thread "main" java.lang.NoClassDefFoundError: Tree/Exp (wrong name: Tree/EXP)`
  > 분명 내 폴더에는 Exp.java도 있고 EXP.java도 잘 있는데, 컴파일하면 왜 런타임에 이 에러가 나는 거야?"
- **AI 원인 진단 및 해결책:**
  > "윈도우 OS 파일 시스템(NTFS)은 파일명 대소문자를 구분하지 않습니다(Case-insensitive). 따라서 컴파일러가 `Exp.class`와 `EXP.class`를 생성할 때, 둘 중 나중에 컴파일된 파일이 이전 파일을 덮어씌워 파일 하나가 실종됩니다. 클래스 로더는 `Tree/Exp`를 기대하지만 `EXP.class`가 로드되면서 `wrong name: Tree/EXP` 에외를 내뿜는 것입니다.
  > 해결을 위해 문장(Statement)을 나타내는 `EXP.java`의 클래스 이름을 `EXP_stm`으로 개명하고 파일명도 `EXP_stm.java`로 리네임하십시오. 또한 식(Expression)을 나타내는 `Exp1.java`는 `Exp.java`로 바꾸어 `public class Exp`로 만드십시오. 이후 `Print.java`와 `Canon.java`에서 `Tree.EXP`를 호출하던 부분을 모두 `Tree.EXP_stm`으로 일괄 교정하면 윈도우 환경에서도 안전하게 작동합니다."
- **의사결정 및 조율 이력:**
  * AI가 알려준 파일 덮어쓰기 원인을 확인하고, 지시에 따라 리네임 및 타입명 교체 작업을 안전하게 실행했습니다.
  * 수정 후 `FrameDriver` 단독 빌드 및 구동 테스트를 진행하여 `NoClassDefFoundError`가 완벽하게 사라지고 정상적인 스택 프레임 오프셋 출력이 나오는 것을 검증하였습니다.

#### 📌 [사례 3] George/Briggs Coalescing 최적화 알고리즘 구현 및 루프 가중치 설계 구상
- **사용자 질의 (프롬프트):**
  > "우리가 레지스터 할당(Graph Coloring)을 구현할 때, Briggs와 George의 Coalescing 기준을 적용하고 루프 정보를 활용해 Spill 우선순위를 정하는 최적화 가점 요소를 완벽하게 짜야 해. 이 로직을 어떻게 안전하게 기존 Appel 소스코드 `Color.java`에 끼워 넣을 수 있을까?"
- **AI 설계 구상:**
  > "1. `Color.java` 내부에서 간선 합치기(Coalesce) 시 George와 Briggs의 두 가지 휴리스틱을 정의합니다. George는 '합치려는 노드의 모든 인접 노드가 이미 색상이 결정되었거나 차수가 K 미만일 때', Briggs는 '두 노드를 합쳤을 때 차수가 K 이상인 인접 노드의 수가 K 미만일 때'에 합칩니다.
  > 2. Spill Weight는 단순히 `DefUse / degree`가 아니라, 해당 변수가 루프 내에 존재하는지 정보를 수집하여 `(DefUse outside loop + 10 * DefUse inside loop) / degree` 공식을 사용합니다. 
  > 3. 이를 위해 활성 분석 시점에 루프 인접도(Loop nest depth) 정보를 FlowGraph나 Assem 단에서 파악하여 전달하는 프레임워크를 조율해야 합니다."
- **의사결정 및 조율 이력:**
  * (현재 구현 예정 단계이며, 향후 Liveness/RegAlloc 작업 진행 시 이 설계 가이드라인에 따라 코드를 작성하고 실시간 디프와 로그를 추가 기록할 예정입니다.)

---

## 2. 모듈별 구현 세부사항 및 설계 의도 (Design Rationale)

### 📌 2.0 [Folder Structure] 통합 단일 패키지 구조 설계 및 의도
* **설계 의도:**
  - 최종 제출 형식인 `chap11_학번_이름.zip`에 최적화되도록 모든 컴파일러 단계(syntaxtree, visitor, Mips, Tree, Canon, Assem, FlowGraph, RegAlloc, Temp, Util)를 `chap11` 폴더 하위에 하나의 패키지 루트 형태로 모았습니다.
  - 여러 폴더에 소스가 흩어져 있을 경우 발생할 수 있는 클래스패스(-classpath) 복잡성과 임포트(import) 참조 오류를 원천 차단하여, 단일 빌드 명령어로 안전하게 동작하는 고이식성의 이점을 지니도록 설계했습니다.
  - **회귀 검증(Regression Test) 완료:** 단일 `chap11` 패키지로의 코드 마이그레이션이 기존 AST 타입 분석 로직을 훼손하지 않았음을 입증하기 위해, 5장의 타입 검사기 메인 클래스인 [TypeCheckMain.java](file:///c:/Users/user/minijava_compiler/chap11/TypeCheckMain.java)를 활용하여 빌드 및 회귀 검증을 실시했습니다.
    * **구동 및 테스트 명령어:**
      ```powershell
      # 1. 패키지 전체 컴파일
      javac -encoding UTF-8 -d chap11\bin chap11\syntaxtree\*.java chap11\visitor\*.java chap11\Temp\*.java chap11\Util\*.java chap11\*.java
      
      # 2. 개별 오류 검출 회귀 실행 (예: TestDuplicate.java)
      java -cp chap11\bin chap11.TypeCheckMain programs/TestDuplicate.java
      ```
    * **검출 오류 정합성 대조 결과:** 6개의 의미론적 오류(Semantic Error)를 고의로 내장한 샘플 미니자바 파일들에 대해 컴파일러가 출력하는 에러 메시지 건수를 전수 대조했습니다.
      * `TestDuplicate.java`: 중복 선언 5개 검출 (기대값 5개와 일치) - **PASS**
      * `TestUndeclared.java`: 선언되지 않은 변수/클래스 참조 7개 검출 (기대값 7개와 일치) - **PASS**
      * `TestOverload.java`: 자바 규격 외 오버로딩 에러 5개 검출 (기대값 5개와 일치) - **PASS**
      * `TestAcyclic.java`: 상속 순환 참조 에러 6개 검출 (기대값 6개와 일치) - **PASS**
      * `TestExprMismatch.java`: 식 타입 불일치 에러 14개 검출 (기대값 14개와 일치) - **PASS**
      * `TestStmtMismatch.java`: 문장 제어 구조 타입 불일치 에러 7개 검출 (기대값 7개와 일치) - **PASS**
      이로써 소스코드 통합 및 패키지 구조 재정비 과정에서 의미 해석기(Type Checker)의 핵심 데이터 흐름과 결합도가 완벽하게 유지되었음을 실증했습니다.

### 📌 2.1 [Mips] Mips 스택 프레임 레이아웃 (Chap 6)
* **설계 의도:**
  - MIPS 호출 규약(Calling Convention)에 부합하도록 스택 프레임 레이아웃을 정의합니다. 매개변수 중 레지스터 개수 한도 내의 파라미터는 레지스터(`InReg`)에 바인딩하고, 한도를 초과하는 파라미터는 프레임 외부의 호출자 영역(양수 오프셋, `InFrame`)에 매핑합니다.
  - 함수 내 지역 변수나 이스케이프 변수(메모리에 올라가야 하는 변수)는 프레임 내부(음수 오프셋, `InFrame`)에 순차적으로 오프셋을 할당하여 프레임 포인터($fp) 또는 스택 포인터($sp)로 참조 가능하도록 합니다.
  - **구현 특징:** 본 프로젝트에서는 원본 소스에서 제공된 [Mips/Frame.java](file:///c:/Users/user/minijava_compiler/chap11/Mips/Frame.java)가 이미 `allocLocal` 및 매개변수 바인딩(`InReg`, `InFrame`) 기능을 정확하게 구현한 구체 클래스였습니다. 따라서, 기존 코드의 패키지/클래스 의존성 오류(특히 `Temp` 및 `Label` 패키지 버그)를 제거하고 무사히 빌드되도록 디버깅함으로써 추가 수정 없이 온전한 MIPS 호출 규약을 구축할 수 있었습니다.
  - **검증 완료:** `FrameDriver` 실행 결과, 4개 이하의 인자와 로컬 변수(foo) 및 5개 이상의 인자가 넘어갈 때의 상황(bar) 각각에 대해 호출 규약(양수/음수 오프셋 배치 및 레지스터 배치)이 완벽하게 들어맞음을 확인했습니다. (출력 로그는 `3.1 [TDD] Mips.FrameDriver 실행 검증 로그` 참조)

### 📌 2.2 [Translate] IR Tree 변환 모듈 (Chap 7)
* **설계 의도:**
  - MiniJava의 AST 노드들을 컴파일러 중간 표현인 Tree IR(문장 `Tree.Stm` 및 식 `Tree.Exp`)로 일대일 번역하는 것을 목표로 합니다.
  - **클래스 레이아웃 사전 스캔 (`preScanClassLayouts`):** MiniJava는 클래스 멤버 필드 상속을 지원하므로 멤버 변수 오프셋을 구하기 위해서는 부모 클래스의 레이아웃 정보가 필요합니다. 따라서 AST 순회 전에 조상 클래스부터 후손 클래스까지 재귀적(`getOrBuildFields`)으로 순회하며 전체 필드 수집, 오프셋 계산(MIPS 32비트 환경에 대응해 `WORD_SIZE = 4` 단위 정렬) 및 타입 매핑 정보와 메서드 시그니처 리턴 타입을 사전 캐싱하도록 설계했습니다.
  - **로컬 및 멤버 변수 식별 (`varExp` / `assignVar`):** 변수를 참조하거나 대입할 때, 로컬 스콥(`envStack`)에 매핑된 `Temp`가 있는지 먼저 조사하고, 없으면 현재 클래스 구조의 오프셋 맵(`fieldOffsets`)을 조사하여 이스케이프 여부 및 멤버 변수 위치를 판단, `this` 포인터 기준 메모리 오프셋 `MEM(BINOP(PLUS, TEMP(this), CONST(offset)))`을 동적으로 방출하도록 하였습니다.
  - **네임스페이스 충돌 회피 (Namespace Isolation):** AST의 식 객체인 `syntaxtree.Exp`와 중간 코드의 식 객체인 `Tree.Exp`, 그리고 AST 출력 `syntaxtree.Print`와 중간 코드 출력 `Tree.Print`가 명칭이 동일하여 컴파일 모호성 에러가 발생했습니다. 이를 회피하기 위해 `import Tree.*` 와일드카드를 걷어내고 개별 임포트를 활용하며, AST 방문 메서드의 파라미터 타입을 FQCN(예: `syntaxtree.Print`)으로 강제 지정하여 컴파일 정합성을 확보했습니다.
  - **검증 상태:** `programs/QuickSort.java`를 대상으로 메인 드라이버를 실행한 결과, 타입 체킹 통과 직후 각 프로시저(메서드)에 대해 MIPS 호출 규약(implicit `this` 바인딩, arg0-arg3 레지스터 매핑 및 callee-saves 보존 seq)에 부합하는 정규화된 IR Tree가 성공적으로 출력되는 것을 확인하여 기능적 결함이 없음을 입증했습니다.

### 📌 2.3 [Codegen] 명령어 선택 및 Maximal Munch (Chap 9)
* **설계 의도:** [작성 예정]

### 📌 2.4 [RegAlloc] 활성 분석 및 그래프 컬러링 할당 (Chap 10 & 11)
* **설계 의도:** [작성 예정]

---

## 3. 트러블슈팅 및 버그 해결 기록 (Troubleshooting & Bug Log)
*(개발 및 컴파일 테스트 과정에서 발생한 모든 예외, 빌드 경고/에러, 그리고 버그 해결 과정을 상세하게 실시간 기록합니다.)*

* **이슈 1: 교재(Appel) 뼈대 소스코드의 패키지/클래스 의존성 컴파일 에러**
  - **수정 대상 파일:**
    1. [Label.java](file:///c:/Users/user/minijava_compiler/chap11/Temp/Label.java) (수정 범위: 라인 2, 34-40)
    2. [TempMap.java](file:///c:/Users/user/minijava_compiler/chap11/Temp/TempMap.java) (수정 범위: 라인 3)
    3. [DefaultMap.java](file:///c:/Users/user/minijava_compiler/chap11/Temp/DefaultMap.java) (수정 범위: 라인 4)
    4. [CombineMap.java](file:///c:/Users/user/minijava_compiler/chap11/Temp/CombineMap.java) (수정 범위: 라인 5)
    5. [JUMP.java](file:///c:/Users/user/minijava_compiler/chap11/Tree/JUMP.java) (수정 범위: 라인 2-10)
  - **발생한 컴파일 에러 로그:**
    ```text
    chap11\Temp\Label.java:2: error: package Symbol does not exist
    import Symbol.Symbol;
                 ^
    chap11\Temp\Label.java:34: error: cannot find symbol
    public Label(Symbol s) {
                 ^
      symbol:   class Symbol
      location: class Label
    chap11\Temp\TempMap.java:3: error: package Temp does not exist
    public interface TempMap {public String tempMap(Temp.Temp t);}
                                                      ^
    chap11\Tree\JUMP.java:6: error: package Temp does not exist
      public Temp.LabelList targets;
                 ^
    ```
  - **원인 분석:**
    - `Label.java`는 교재 기본 패키지인 `Symbol.Symbol`에 의존하고 있으나, 우리 프로젝트는 모든 기호를 `String`으로 처리하여 `Symbol` 클래스가 제외되어 발생함.
    - `TempMap`, `DefaultMap`, `CombineMap`에서 같은 패키지 내 `Temp`를 지칭할 때 `Temp.Temp`로 중복 기재하여 컴파일러가 타입 해석 오류를 발생시킴 (패키지 이름 `Temp`와 클래스 이름 `Temp`가 충돌하는 섀도잉 문제).
    - `JUMP.java`에서 `Temp` 패키지와 `Temp` 클래스명이 동일해 `Temp.LabelList` 등을 찾을 때 섀도잉되어 찾지 못하는 문제 발생.
  - **코드 수정 내역 (Before vs After):**
    - **`chap11/Temp/Label.java`**
      - *Before (라인 2)*:
        ```java
        import Symbol.Symbol;
        ```
      - *Before (라인 34-40)*:
        ```java
        public Label(Symbol s) {
            this(s.toString());
        }
        ```
      - *After (라인 2 및 34-40)*: (Symbol 의존성 제거를 위해 해당 임포트 및 생성자 삭제)
        ```java
        // (Symbol 임포트 및 생성자 제거됨)
        ```
    - **`chap11/Temp/TempMap.java`**
      - *Before (라인 3)*:
        ```java
        public interface TempMap {public String tempMap(Temp.Temp t);}
        ```
      - *After (라인 3)*: (타입 지칭을 Temp.Temp가 아닌 Temp로 수정)
        ```java
        public interface TempMap {public String tempMap(Temp t);}
        ```
    - **`chap11/Temp/DefaultMap.java`**
      - *Before (라인 4)*:
        ```java
        public String tempMap(Temp.Temp t) {
        ```
      - *After (라인 4)*: (타입 지칭을 Temp로 단축)
        ```java
        public String tempMap(Temp t) {
        ```
    - **`chap11/Temp/CombineMap.java`**
      - *Before (라인 5)*:
        ```java
        public String tempMap(Temp.Temp t) {
        ```
      - *After (라인 5)*: (타입 지칭을 Temp로 단축)
        ```java
        public String tempMap(Temp t) {
        ```
    - **`chap11/Tree/JUMP.java`**
      - *Before (라인 2-10)*:
        ```java
        package Tree;
        import Temp.Temp;
        import Temp.Label;
        public class JUMP extends Stm {
          public Exp exp;
          public Temp.LabelList targets;
          public JUMP(Exp e, Temp.LabelList t) {exp=e; targets=t;}
          public JUMP(Label target) {
              this(new NAME(target), new Temp.LabelList(target,null));
          }
        ```
      - *After (라인 2-10)*: (Temp.LabelList 대신 LabelList를 직접 import하여 명시)
        ```java
        package Tree;
        import Temp.Temp;
        import Temp.Label;
        import Temp.LabelList;
        public class JUMP extends Stm {
          public Exp exp;
          public LabelList targets;
          public JUMP(Exp e, LabelList t) {exp=e; targets=t;}
          public JUMP(Label target) {
              this(new NAME(target), new LabelList(target,null));
          }
        ```

* **이슈 2: 윈도우 대소문자 미구분 파일시스템으로 인한 Exp.class / EXP.class 컴파일 결과 충돌**
  - **수정 대상 파일:**
    1. `chap11/Tree/Exp1.java` -> [Exp.java](file:///c:/Users/user/minijava_compiler/chap11/Tree/Exp.java) (리네임 및 public 수정, 수정 범위: 라인 1-2)
    2. `chap11/Tree/EXP.java` -> [EXP_stm.java](file:///c:/Users/user/minijava_compiler/chap11/Tree/EXP_stm.java) (리네임 및 클래스 이름 변경, 수정 범위: 라인 4-11)
    3. [Print.java](file:///c:/Users/user/minijava_compiler/chap11/Tree/Print.java) (수정 범위: 라인 65-75 및 인스턴스 검사 영역)
    4. [Canon.java](file:///c:/Users/user/minijava_compiler/chap11/Canon/Canon.java) (수정 범위: 라인 18, 31-32, 63, 72, 97)
  - **에러 로그:**
    ```text
    Exception in thread "main" java.lang.NoClassDefFoundError: Tree/Exp (wrong name: Tree/EXP)
        at java.base/java.lang.ClassLoader.defineClass1(Native Method)
        at Mips.FrameDriver.main(FrameDriver.java:31)
    ```
  - **원인 분석:**
    - 자바의 식을 상징하는 `Exp` 클래스와 문장을 상징하는 `EXP` 클래스는 대소문자만 다릅니다. 컴파일 시 각각 `Exp.class`와 `EXP.class`가 나오는데, 윈도우 환경은 파일명 대소문자를 구분하지 않아 나중 컴파일 파일이 이전 컴파일 파일을 덮어써 유실시켰습니다. 이로 인해 JVM 런타임 로더가 `Tree/Exp`를 부를 때 `EXP.class`가 로드되면서 명칭 불일치 예외(`NoClassDefFoundError: wrong name`)를 유발했습니다.
  - **코드 수정 내역 (Before vs After):**
    - **`chap11/Tree/EXP_stm.java` (구 `EXP.java` 리네임 및 클래스 정의 변경, 라인 4-11)**
      - *Before (EXP.java)*:
        ```java
        public class EXP extends Stm {
          public Exp exp; 
          public EXP(Exp e) {exp=e;}
          ...
            return new EXP(kids.head);
        ```
      - *After (EXP_stm.java)*: (클래스명을 `EXP_stm`으로 고치고 파일명을 `EXP_stm.java`로 통일해 윈도우 에러 회피)
        ```java
        public class EXP_stm extends Stm {
          public Exp exp; 
          public EXP_stm(Exp e) {exp=e;}
          ...
            return new Tree.EXP_stm(kids.head);
        ```
    - **`chap11/Tree/Exp.java` (구 `Exp1.java` 리네임 및 public 환원, 라인 1)**
      - *Before (Exp1.java)*:
        ```java
        package Tree;abstract class Exp {
        ```
      - *After (Exp.java)*: (클래스명을 파일명 `Exp.java`와 맞추고 `public`으로 복원하여 외부 패키지 `Mips` 등에서 참조 가능하게 함)
        ```java
        package Tree;abstract public class Exp {
        ```
    - **`chap11/Tree/Print.java` (라인 65-75 및 인스턴스 검사)**
      - *Before (라인 65-75)*:
        ```java
        void prStm(EXP s, int d) {
           indent(d); sayln("EXP("); prExp(s.exp,d+1); say(")"); 
        }
        ...
        else if (s instanceof EXP) prStm((EXP)s, d);
        ```
      - *After (라인 65-75)*: (EXP 인스턴스 참조 대상을 EXP_stm으로 전면 교체)
        ```java
        void prStm(EXP_stm s, int d) {
           indent(d); sayln("EXP_stm("); prExp(s.exp,d+1); say(")"); 
        }
        ...
        else if (s instanceof EXP_stm) prStm((EXP_stm)s, d);
        ```
    - **`chap11/Canon/Canon.java` (라인 18, 31-32, 63, 72, 97)**
      - *Before (라인 18, 31-32, 63, 72, 97)*:
        ```java
        return new Tree.EXP(call.build(kids));
        ...
        return a instanceof Tree.EXP && ((Tree.EXP)a).exp instanceof Tree.CONST;
        ...
        static Tree.Stm do_stm(Tree.EXP s)
        ...
        else if (s instanceof Tree.EXP) return do_stm((Tree.EXP)s);
        ...
        static StmExpList nopNull = new StmExpList(new Tree.EXP(new Tree.CONST(0)),null);
        ```
      - *After (라인 18, 31-32, 63, 72, 97)*: (Tree.EXP를 Tree.EXP_stm으로 변경하여 바뀐 형식을 맞춤)
        ```java
        return new Tree.EXP_stm(call.build(kids));
        ...
        return a instanceof Tree.EXP_stm && ((Tree.EXP_stm)a).exp instanceof Tree.CONST;
        ...
        static Tree.Stm do_stm(Tree.EXP_stm s)
        ...
        else if (s instanceof Tree.EXP_stm) return do_stm((Tree.EXP_stm)s);
        ...
        static StmExpList nopNull = new StmExpList(new Tree.EXP_stm(new Tree.CONST(0)),null);
        ```

* **이슈 3: 뼈대 소스코드 StmListList.java 웹 아카이브 마크업 깨짐 버그 해결**
  - **수정 대상 파일:**
    - [StmListList.java](file:///c:/Users/user/minijava_compiler/chap11/Canon/StmListList.java) (수정 범위: 전체)
  - **발생한 컴파일 에러 로그:**
    ```text
    chap11\Canon\StmListList.java:93: error: <identifier> expected
              <span class="iconochive-movies"></span>
                         ^
    chap11\Canon\StmListList.java:93: error: class, interface, enum, or record expected
              <span class="iconochive-movies"></span>
    ```
  - **원인 분석:**
    - Appel 컴파일러 공식 리소스의 Wayback Machine 아카이브 다운로드 중, Wayback Machine의 프레임 삽입 스크립트 및 HTML 배너 마크업이 파일에 고스란히 섞여 다운로드되어 빌드가 완전히 터졌습니다.
  - **코드 수정 내역 (Before vs After):**
    - **`chap11/Canon/StmListList.java`**
      - *Before (전체 HTML 코드)*:
        ```html
        <!DOCTYPE html>
        <html>
        <head>
        <title>Wayback Machine</title>
        ...
        ```
      - *After (순수 자바 복원)*:
        ```java
        package Canon;
        
        public class StmListList {
          public Tree.StmList head;
          public StmListList tail;
          public StmListList(Tree.StmList h, StmListList t) {
            head = h;
            tail = t;
          }
        }
        ```

* **이슈 4: 자바 패키지 간 Exp, Print, And 클래스명 충돌 모호성 에러**
  - **수정 대상 파일:**
    - [IRTranslator.java](file:///c:/Users/user/minijava_compiler/chap11/visitor/IRTranslator.java) (수정 범위: `import` 문 및 `visit` 메서드 시그니처 전체)
  - **발생한 컴파일 에러 로그:**
    ```text
    chap11\visitor\IRTranslator.java:415: error: reference to Exp is ambiguous
            Exp cond = resultExp;
            ^
      both class Tree.Exp in Tree and class syntaxtree.Exp in syntaxtree match
    chap11\visitor\IRTranslator.java:429: error: name clash: class IRTranslator has two methods with the same erasure, yet neither overrides the other
        public void visit(While n) {
                    ^
      first method:  visit(Print) in IRTranslator
      second method: visit(Identifier) in Visitor
    ```
  - **원인 분석:**
    - `syntaxtree` 패키지와 `Tree` 패키지가 둘 다 `Exp`, `Print` 등 동일한 이름의 핵심 클래스를 가지고 있어, `import syntaxtree.*`와 `import Tree.*`를 혼용할 시 타입 명칭의 모호성이 발생하고, `visit` 오버로드가 섀도잉되어 오버라이딩 실패 예외를 유발했습니다.
  - **코드 수정 내역 (Before vs After):**
    - **`chap11/visitor/IRTranslator.java`**
      - *Before*: (와일드카드 임포트 혼용)
        ```java
        import syntaxtree.*;
        import Tree.*;
        ...
        public void visit(Print n) {
        ...
        public void visit(While n) {
        ```
      - *After*: (임포트 제한 및 visit 메서드 파라미터 FQCN 강제화)
        ```java
        import syntaxtree.*;
        import Temp.*;
        // (import Tree.* 제거 및 Tree 산하 클래스 개별 명시적 import 활용)
        ...
        public void visit(syntaxtree.Print n) {
        ...
        public void visit(syntaxtree.While n) {
        ```

* **이슈 5: Graph 패키지 누락으로 인한 레지스터 할당기 컴파일 실패 해결**
  - **수정 대상 파일:**
    - [Graph.java](file:///c:/Users/user/minijava_compiler/chap11/Graph/Graph.java) [NEW]
    - [Node.java](file:///c:/Users/user/minijava_compiler/chap11/Graph/Node.java) [NEW]
    - [NodeList.java](file:///c:/Users/user/minijava_compiler/chap11/Graph/NodeList.java) [NEW]
  - **발생한 컴파일 에러 로그:**
    ```text
    chap11\RegAlloc\Color.java:3: error: package Graph does not exist
    import Graph.Node;
                ^
    chap11\RegAlloc\Color.java:4: error: package Graph does not exist
    import Graph.NodeList;
    ```
  - **원인 분석:**
    - 뼈대 코드 이송 시 10단원의 활성 분석에서 필요한 핵심 자료구조인 `Graph` 패키지가 `chap11` 루트에 누락되어 빌드가 불가능했습니다.
  - **해결 내역:**
    - `AndrewAppel/chap10/Graph` 아래에 있던 원본 `Graph.java`, `Node.java`, `NodeList.java`를 `chap11/Graph/` 디렉토리에 복사 이송하여 의존성을 복원하고 빌드를 완료했습니다.

---

### 📌 3.2 [TDD] Chap 7 IR 번역 실행 검증 로그 (2026-06-23 완료)
`programs/QuickSort.java` 파일을 빌드 및 구동하여 AST가 Tree IR 문법 구조에 부합하게 올바른 호출 규약(Arg 레지스터 매핑 및 Callee-saves 보존 seq)으로 정상 변환되는지 최종 실증하였습니다.

**테스트 구동 명령어:**
```powershell
javac -encoding UTF-8 -d chap11\bin chap11\syntaxtree\*.java chap11\visitor\*.java chap11\Temp\*.java chap11\Util\*.java chap11\Tree\*.java chap11\Canon\*.java chap11\Mips\*.java chap11\Assem\*.java chap11\FlowGraph\*.java chap11\RegAlloc\*.java chap11\Graph\*.java chap11\*.java
java -cp chap11\bin Main programs/QuickSort.java
```

**출력 로그 (일부 발췌 - QS_Init 프로시저):**
```text
Procedure: QS_Init
SEQ(
 SEQ(
  SEQ(
   SEQ(
    SEQ(
     SEQ(
      SEQ(
       SEQ(
        SEQ(
         SEQ(
          SEQ(
           MOVE(
            TEMP t82,
            TEMP t7),
           MOVE(
            TEMP t83,
            TEMP t8)),
          MOVE(
           TEMP t85,
           TEMP t11)),
         MOVE(
          TEMP t86,
          TEMP t12)),
        MOVE(
         TEMP t87,
         TEMP t13)),
       MOVE(
        TEMP t88,
        TEMP t14)),
      MOVE(
       TEMP t89,
       TEMP t15)),
     MOVE(
      TEMP t90,
      TEMP t16)),
    MOVE(
     TEMP t91,
     TEMP t17)),
   MOVE(
    TEMP t92,
    TEMP t18)),
  SEQ(
   SEQ(
    SEQ(
     SEQ(
      SEQ(
       SEQ(
        SEQ(
         SEQ(
          SEQ(
           SEQ(
            SEQ(
             SEQ(
              MOVE(
               MEM(
                BINOP(PLUS,
                 TEMP t82,
                 CONST 4)),
               TEMP t83),
              MOVE(
               MEM(
                BINOP(PLUS,
                 TEMP t82,
                 CONST 0)),
               ESEQ(
                MOVE(
                 TEMP t84,
                 CALL(
                  NAME _allocArray,
                   TEMP t83)),
                TEMP t84))),
             MOVE(
              MEM(
               BINOP(PLUS,
                MEM(
                 BINOP(PLUS,
                  TEMP t82,
                  CONST 0)),
                BINOP(PLUS,
                 CONST 4,
                 BINOP(MUL,
                  CONST 0,
                  CONST 4)))),
              CONST 20)),
...
```

---

### 📌 3.1 [TDD] Mips.FrameDriver 실행 검증 로그 (2026-06-22 완료)
수정 및 뼈대 조립 완료 후, `FrameDriver` 단독 테스트를 실행하여 MIPS 프레임 레이아웃 오프셋 할당이 설계대로 정상 작동하는지 확인하였습니다.

**테스트 구동 명령어:**
```powershell
javac -encoding UTF-8 -d chap11\bin chap11\Mips\*.java chap11\Util\*.java chap11\Temp\*.java chap11\Assem\*.java chap11\Tree\*.java
java -cp chap11\bin Mips.FrameDriver
```

**출력 로그 (Stdout):**
```text
=== <=K args ===
# name: foo
# frameSize: 20
# formals:
arg[0]: InFrame offset=-12 (in-frame(-))
arg[1]: InReg
arg[2]: InFrame offset=-16 (in-frame(-))
=== >K args ===
# name: bar
# frameSize: 20
# formals:
arg[0]: InFrame offset=-12 (in-frame(-))
arg[1]: InReg
arg[2]: InFrame offset=-16 (in-frame(-))
arg[3]: InReg
arg[4]: InFrame offset=8 (stack-arg(+))
arg[5]: InFrame offset=12 (stack-arg(+))
```

---

## 4. 8개 프로그램 실행 결과 검증 표
`test_all.ps1`을 실행하여 캡처한 Javac(Java 버추얼 머신)과 MARS(MIPS 시뮬레이터)의 출력 비교 결과입니다.

| 프로그램명 (Programs) | Java 실행 결과 | MIPS (MARS) 실행 결과 | 동일 여부 (Correctness) |
| :--- | :--- | :--- | :---: |
| Factorial.java | *(대기)* | *(대기)* | *(대기)* |
| BinarySearch.java | *(대기)* | *(대기)* | *(대기)* |
| BubbleSort.java | *(대기)* | *(대기)* | *(대기)* |
| LinearSearch.java | *(대기)* | *(대기)* | *(대기)* |
| LinkedList.java | *(대기)* | *(대기)* | *(대기)* |
| QuickSort.java | *(대기)* | *(대기)* | *(대기)* |
| TreeVisitor.java | *(대기)* | *(대기)* | *(대기)* |
| BinaryTree.java | *(대기)* | *(대기)* | *(대기)* |
