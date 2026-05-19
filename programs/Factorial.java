class Factorial{
    public static void main(String[] a){
	System.out.println(new Fact().ComputeFac(10)); // 1. 클래스 선언이 없는 객체를 생성하는 경우
    }
}

class Fac {

    public int ComputeFac(int num){
	int num_aux ;
	boolean flag ;

	flag = num ; // 3. 변수에 호환되지 않는 타입을 대입하는 경우
	if (num) // 4. if 제어문의 조건식에 boolean이 아닌 값을 전달하는 경우
	    num_auxi = 1 ; // 2. 선언되지 않은 변수를 사용하는 경우 (오타)
	else 
	    num_aux = flag * (this.ComputeFac(num-1)) ; // 5. 서로 다른 타입 간에 산술 연산(*)을 시도하는 경우

	return flag ; // 6. 메서드의 선언된 반환 타입과 실제 return 값의 타입이 불일치하는 경우
    }

}
