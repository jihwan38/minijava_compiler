class TestAcyclic {
    public static void main(String[] a){
        System.out.println(1);
    }
}

// 1. 자기 자신을 상속 
class A extends A {}

// 2, 3. 두 클래스 간의 순환 상속
class B extends C {}
class C extends B {}

// 4, 5, 6. 세 클래스가 꼬여 있는 순환 상속
class D extends E {}
class E extends F {}
class F extends D {}
