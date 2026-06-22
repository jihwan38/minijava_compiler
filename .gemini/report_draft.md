# 과제 리포트 초안 & AI 어시스턴트 협업 기록
(MiniJava-to-MIPS 컴파일러 프로젝트)

이 문서는 컴파일러 프로젝트의 구현 세부사항, 설계 의도, AI 어시스턴트와의 협업 이력, 그리고 테스트 검증 결과를 실시간으로 기록하여 최종 PDF 보고서 작성을 돕는 기록지입니다.

---

## 1. AI 어시스턴트 협업 이력 및 활용 보고
> [과제 요구사항] AI 어시스턴트(Claude, ChatGPT, Gemini 등)를 사용했을 때 반드시 어떻게 사용했는지 보고서에 작성해 주세요.

### 💡 활용 요약 및 프롬프트 전략
* **사용 도구:** Antigravity (Gemini 3.5 Flash High 기반 코딩 Assistant)
* **협업 목표:** 난이도가 높은 중간 표현(IR) 변환, MIPS 패턴 매칭 코드 생성, 그리고 레지스터 할당(Graph Coloring) 최적화 로직의 효율적 구현 및 디버깅 단축.
* **주요 질문 및 해결 사례:**
  - *(실시간 협업을 진행하면서 여기에 프롬프트 질문과 답변 요약을 채워 나갈 예정입니다.)*
  - **사례 1 (설계 뼈대):** 프로젝트 디버깅 및 디렉토리 구조 일관성을 위해 `chap11` 루트를 기준으로 한 자바 패키지 및 소스 루트 구조 제안 및 조율.
  - **사례 2 (레지스터 할당):** 가점 획득을 위해 기본 제공 알고리즘을 확장하여 Loop-aware Spill Weight 및 Briggs/George Coalescing 논리를 추가하는 커스텀 코드 개발 협업.

---

## 2. 모듈별 구현 세부사항 및 설계 의도 (Design Rationale)

### 📌 2.0 [Folder Structure] 통합 단일 패키지 구조 설계 및 의도
* **설계 의도:**
  - 최종 제출 형식인 `chap11_학번_이름.zip`에 최적화되도록 모든 컴파일러 단계(syntaxtree, visitor, Mips, Tree, Canon, Assem, FlowGraph, RegAlloc, Temp, Util)를 `chap11` 폴더 하위에 하나의 패키지 루트 형태로 모았습니다.
  - 여러 폴더에 소스가 흩어져 있을 경우 발생할 수 있는 클래스패스(-classpath) 복잡성과 임포트(import) 참조 오류를 원천 차단하여, 단일 빌드 명령어로 안전하게 동작하는 고이식성의 이점을 지니도록 설계했습니다.
  - **회귀 검증(Regression Test) 완료:** 이송 직후, 5장의 타입 검사기 메인(`TypeCheckMain.java`)을 임시 컴파일하여 6개의 의미 오류 자바 파일을 대상으로 회귀 테스트를 수행한 결과, 각 파일별로 기대되는 에러 개수(TestDuplicate: 5개, TestUndeclared: 7개, TestOverload: 5개, TestAcyclic: 6개, TestExprMismatch: 14개, TestStmtMismatch: 7개)를 완벽하게 정상 포착하고 모두 `TEST PASS`를 획득하여 마이그레이션 중 기능 결함이 없음을 검증했습니다.

### 📌 2.1 [Mips] Mips 스택 프레임 레이아웃 (Chap 6)
* **설계 의도:**
  - MIPS 호출 규약(Calling Convention)에 따라 첫 4개의 인자는 `$a0-$a3` 레지스터로 넘기고, 초과되는 인자는 호출자의 스택 프레임에 오프셋(FP 기준 양수 오프셋)을 할당하여 저장한다.
  - MiniJava의 성격상 모든 변수는 워드 크기(4바이트)를 기본으로 하며, 중첩 클래스나 내부 메서드가 없으므로 static link나 변수의 주소를 따는 'escape' 변수가 없다. 따라서 지역 변수 할당 시 `escapes`가 항상 `false`인 레지스터 보관(`InReg`) 형태로 할당해 메모리 접근 횟수를 줄였다.
  - 함수 진입 및 진출 시 `$ra`, `$fp`를 스택 프레임에 대피시키고, 피호출자 보존 레지스터(`$s0-$s7`)들의 값을 프레임 시작부에 백업하고 에필로그에서 복원하는 시퀀스(`procEntryExit1`)를 정밀하게 구성하였다.

### 📌 2.2 [Translate] IR Tree 변환 모듈 (Chap 7)
* **설계 의도:**
  - AST의 다양한 구문(Statements & Expressions)을 MIPS 추상 기계 언어인 IR Tree로 온전히 번역하기 위해 `Visitor` 패턴 기반의 `IRTranslator`를 설계했다.
  - 객체의 멤버 변수 접근 시 해당 필드의 힙 오프셋을 기호 테이블에서 찾아 `MEM(BINOP(PLUS, TEMP this, CONST offset))` 형태로 변환하여 static method와 필드에 안전하게 접근한다.
  - `If`/`While`문과 같은 조건 분기문을 컴파일할 때 불필요하게 0과 1로 값을 평가(Evaluation)하지 않고, 점프문(`CJUMP`)을 꽂아 `true`와 `false` 타겟 라벨로 즉시 분기시키는 `Cx`(Condition expression) 변환 논리를 완벽하게 차용하였다.

### 📌 2.3 [Codegen] 명령어 선택 및 Maximal Munch (Chap 9)
* **설계 의도:**
  - IR 트리를 MIPS 어셈블리 리스트로 변환하기 위해 `Maximal Munch` 알고리즘을 사용한 `Codegen.java`를 구현했다.
  - IR 트리의 루트부터 아래로 내려가며 가장 큰 타일(예: `MEM(BINOP(PLUS, left, CONST))` 타일)을 MIPS의 `lw/sw` 명령어 패턴으로 먼저 덮어씌워 매칭 횟수를 줄이고 고효율 어셈블리를 유도했다.
  - `$v0`, `$ra`, `$sp`, `$fp` 등 예약된 하드웨어 레지스터와 가상 레지스터(`Temp`)들 사이의 구분을 명확히 하고, `format` 헬퍼를 거치며 `Assem` 객체가 규격화된 스트링을 뽑아내도록 정밀 설계했다.

### 📌 2.4 [RegAlloc] 활성 분석 및 그래프 컬러링 할당 (Chap 10 & 11)
* **설계 의도:**
  - 가상 레지스터(`Temp`)들을 한정된 MIPS 실제 레지스터에 최적으로 매핑하기 위해 활성 분석(`Liveness`)을 통해 변수들의 생명 주기를 산출하고 간섭 그래프(`InterferenceGraph`)를 구축했다.
  - **[가점 최적화]** Spill 대상 선정 시 단순 encounter 순이 아닌, **루프 깊이 가중치 기반 Spill Priority**를 계산하여 루프 밖의 변수부터 메모리에 내보내는(Spilling) 최적의 우선순위 휴리스틱을 작성했다.
  - **[가점 최적화]** Briggs/George 보수적 Coalescing 전략을 구현하여, 간섭하지 않으면서 `move` 명령어로 엮여 있는 가상 레지스터 노드들을 물리 레지스터 할당 전에 안전하게 병합함으로써 대량의 `move` 어셈블리 코드를 원천 제거하였다.

---

## 3. 트러블슈팅 및 버그 해결 기록
*(개발 및 컴파일 테스트 과정에서 발생할 예외와 버그, 그리고 AI와 협업하여 해결한 과정을 아래에 실시간 기록합니다.)*
* **이슈 1:** 
* **해결책:** 

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
