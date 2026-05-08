# MiniJava Type Checker (Chap 5) - Implementation Plan & Report Draft

작성하고 계신 보고서의 흐름을 유지하면서, `miniJava-typesystem_ko.pdf`의 공식 타입 규칙과 **교수님의 실제 참조(Reference) 구현 방식(교재 5.2절 가이드라인)**을 완벽하게 반영한 구현 계획입니다.

---

## 📝 과제 진행 현황 및 리포트 초안

### 0. 파일 및 폴더 복사 (✅ 완료)
chap5 폴더를 생성한 뒤, 이전 chap4의 `syntaxtree` 폴더, `visitor` 폴더, `Main.java`, `MiniJavaParser.jj`를 옮겼다.
JavaCC 환경 같은 경우 이전 프로젝트 1과 같이 시스템 전역 환경 변수를 설정하는 방식 대신 프로젝트 디렉토리 내에 직접 `javacc.jar`를 포함시키는 방식을 채택하였다.

### 1. 초기 빌드 테스트 (✅ 완료)
본격적인 타입 검사기를 구현하기 전에 `MiniJavaParser.jj`가 소스코드들을 제대로 생성되는지 확인하였다.
정상적으로 생성된 것을 확인하였고 이후 `javac *.java` 명령어를 통해 전체 java 파일들을 컴파일하였다. 그리고 정상적으로 Pretty Print가 되는 것을 확인하기 위해 `cat ../programs/Factorial.java | java Main` 명령어를 통해 `Factorial.java` 를 테스트하였다.

### 2. 다중 파일 처리(Batch Processing)를 위한 파서 설정 및 Main 구동부 수정 (✅ 완료)
본격적인 타입 검사기(Type Checker)를 구현하기에 앞서, 여러 개의 MiniJava 예제 파일들을 한 번에 테스트할 수 있도록 파서의 환경과 프로그램의 진입점(`Main.java`)을 수정하였다.

**가. JavaCC 파서의 정적(Static) 옵션 해제**
기본적으로 JavaCC는 정적(Static) 파서를 생성하도록 설정되어 있다. 하지만 여러 파일을 일괄적으로 처리하기 위해 `for` 반복문 내부에서 `new MiniJavaParser(in)`를 매번 새로 인스턴스화할 경우, "Second call to constructor of static parser" 에러가 발생하게 된다. 이를 해결하기 위해 `MiniJavaParser.jj`의 `options` 블록에 `STATIC = false;` 옵션을 명시적으로 추가한 뒤 파서를 재생성하였다. 이를 통해 매 파일마다 독립적인 파서 객체를 생성하여 안전하게 파싱할 수 있게 되었다.

**나. Main.java 다중 파일 처리 로직 구현**
파서 설정을 변경한 뒤, `Main.java`를 수정하여 터미널을 통해 입력받은 모든 파일 경로(`args`)를 `for`문으로 순회하도록 구현하였다. 
현재는 타입 검사기 방문자(Visitor)가 구현되기 전이므로, 우선 `root.accept(new PrettyPrintVisitor())`를 통해 구문 트리가 정상적으로 빌드되었는지 테스트하였다. 향후 기호 테이블과 타입 검사 방문자가 완성되면, 해당 부분을 주석 해제하여 일괄적인 타입 검사 결과를 출력(`Successfully type checked for...` 등)하도록 확장 가능한 구조를 마련하였다. 전체 8개의 예제 프로그램에 대해 일괄 테스트를 진행한 결과, 파싱 단계에서 아무런 오류 없이 정상 구동됨을 확인하였다.

### 3. 기호 테이블 (Symbol Table) 자료구조 설계 (✅ 완료)
MiniJava의 타입 검사를 위한 기호 테이블(Symbol Table)의 자료구조를 설계하였다.
교재(Modern Compiler Implementation in Java)의 5.1절에서는 범용 컴파일러를 위한 `Symbol` 및 `Table` 구조를 소개하지만, 5.2절(Type-Checking MiniJava)에서는 언어의 단순한 스코핑 룰을 감안하여 변수 식별자를 `String` 자체로 활용하는 방식을 제안하고 있다.
이에 따라 교수님의 레퍼런스 구현 구조를 채택하여, 별도의 외부 클래스를 만들지 않고 **`TypeCheckVisitor` 클래스 내부에 정적 중첩 클래스(Static Nested Class)로 `ClassInfo`와 `MethodInfo`를 정의**하였다. 
- 변수 메타데이터는 AST의 `syntaxtree.Type` 객체를 그대로 Value로 사용하였으며, 식별자 이름(`String`)을 Key로 하는 자바의 기본 `HashMap`과 `LinkedHashMap`(파라미터 순서 보장용)을 활용하여 전체 글로벌 맵(`classTable`)을 구축하였다.
- 이 방식은 과도한 오버엔지니어링(예: `Optional` 남용, `Symbol` 패키지 분리)을 피하고 직관성과 유지보수성을 극대화한 정석적인 MiniJava 타입 체커 설계이다.

---

## 🚀 앞으로 진행할 구현 계획 (Implementation Plan)

### 4. 에러 처리기 및 기호 수집(Collect) 헬퍼 메서드 구현
- **목표:** `TypeCheckVisitor` 내부에 기호 테이블 조작을 위한 유틸리티 메서드를 추가한다.
- **세부 작업:**
  - `ErrorMsg` 클래스(또는 헬퍼 메서드)를 추가하여 "is already defined" 등의 컴파일 에러 메시지를 통일된 포맷으로 출력.
  - `ClassInfo` 내에 `addVar()`, `addMethod()` 구현 (중복 선언 시 false 반환).
  - `MethodInfo` 내에 `addParam()`, `addLocal()` 구현 (중복 선언 시 false 반환).

### 5. 단일 TypeCheckVisitor 순회 구현 (COLLECT / CHECK 두 단계 통합)
- **목표:** 하나의 Visitor 클래스 안에서 AST를 순회하며 타입 검사를 완료한다.
- **세부 작업 (Phase.COLLECT):**
  - AST를 첫 번째로 순회하며 클래스, 메서드, 변수 선언을 `HashMap` 기반의 기호 테이블에 수집.
  - `distinct` 제약: 선언 시 이름 중복(클래스명, 메서드명, 지역변수명 등) 검사 및 에러 출력.
  - `noOverloading` 제약: 메서드 오버라이딩 시 부모와 시그니처가 정확히 일치하는지 검사.
- **세부 작업 (Phase.CHECK):**
  - 논문의 보조 함수(`fields`, `methodtype`, `isSubtype`, `acyclic`)를 프라이빗 헬퍼 메서드로 구현하여 활용.
  - AST를 두 번째로 순회하며 공식 타입 규칙(7.6 문장, 7.7 식) 검증.
  - 대입문(`id = e`), 메서드 호출(`p.id(...)`) 등의 서브타입(`≤`) 일치 여부 검사.

### 6. 최종 테스트 및 오류 디버깅
- **목표:** 8개의 정상 프로그램과 PDF의 모든 예외 상황(실패 예제)을 잡아내는지 테스트한다.
- **세부 작업:**
  - `java Main ../programs/*.java` 실행 후 결과 캡처 및 보고서 완성.
