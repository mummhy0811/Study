# Spring 기초

컨트롤러에서 외부 요청을 받고
->   서비스 비지니스 로직을 만들고 ->
리포지토리에서 데이터 저장하고

<br>

## DI (Dependencies Injection: 의존성 주입)
- 객체 의존관계를 외부에서 넣어주는 것
- @Autowired가 있으면 스프링이 연관된 객체를 스프링 컨테이너에서 찾아서 넣어줌.
- 생성자가 하나일 경우 @Autowired 생략 가능
- 기존 코드를 전혀 손대지 않고, 설정만으로 구현 클래스를 변경할 수 있음.

- 방법 세 가지
    1. 필드 주입 -> 외부에서 수정 불가. 권장x
    2. setter 주입 -> public으로 만들어지기 때문에 권장x
    3. 생성자 주입 -
    
<br><br>

# < 스프링빈>
## 스프링빈 등록 방법1 - 컴포넌트 스캔과 자동 의존관계 설정   
- ```@Component```: 해당 애노테이션이 있으면 스프링 빈으로 자동 등록
- 컴포넌트 스캔 대상: 메인함수가 있는 패키지와 그 하위 패키지들만 스캔

> 스프링은 스프링 컨테이너에 스프링 빈을 등록할 때, 기본으로 **싱글톤**으로 등록   
> __싱글톤__ : 한 번만 등록하는 것. 유일하게 하나   

<br> 

## 스프링빈 등록 방법2 - 자바 코드로 직접 등록
- 컨트롤러에만 ```@Controller```, ```@Autowired``` 애노테이션 사용.   
- 메인 함수가 있는 파일과 동일한 경로에 파일을 하나 더 생성하고 ```@Config```, ```@Bean``` 애노테이션 사용
- 정형화되지 않거나, 상황에 따라 구현 클래스를 변경해야할 경우 자바 코드로 직접 스프링빈을 등록




<br>  <br>  

# < 스프링 DB 접근 기술 >
## 스프링 통합 테스트
- 기존 자바 테스트 코드에 @SpringBootTest와 @Transactional 입력하면 됨   
- ```@Transactional``` : 테스트를 실행 전에 트랜잭션을 먼저 실행하고,테스트가 끝나면 롤백을 해줌. 따라서 db에는 흔적을 남기지 않으므로 다음 테스트에 영향을 주지 않음.   
    = java 테스트 코드의 beforeEach와 afterEach역할을 하는 것!

<br> 

##  JPA 
- EntityManager을 통해 모든 것이 동작
- Service 계층에 @Transactional 필수. 데이터 저장/변경시에 필요함

### Entity 매핑
- 매핑은 annotation으로

    ### @Entity
    - @Entity가 붙으면 JPA가 관리하는 엔티티가 되는 것임

    ### @Id
    - 기본 키(pk)임을 나타냄

    ### @GeneratedValue(strategy = GenerationType.IDENTITY)
    - pk의 생성 규칙을 나타냄
    - ```strategy = GenerationType.IDENTITY``` : auto_increment를 위해 필수

    ### @Column
    - 매핑될 컬럼명이 다를 경우 사용


<br>

##  스프링 데이터 JPA 
- JPA를 편하게 사용하도록 돕는 기술
- 리포지토리에 구현 클래스 없이 인터페이스만으로 개발을 할 수 있게 도움
- CRUD 처리를 위한 공통 인터페이스 제공

### 구현 방법
SpringDataRepository를 만든 후 ```JpaRepository<T, ID>```인터페이스를 상속받음 -> 공통 메소드 사용 가능
- JpaRepository를 상속받으면 사용할 수 있는 주요 메소드
    - delete(T)
    - findById(ID)
    - findAll
- 쿼리 메소드: 메소드 이름만으로 쿼리를 생성해줌
    ``` java
    //메소드
    List<Member> findByEmailAndName(String email, string name);
    // 생성되는 쿼리 -> select m from Member m where m.email - ?1 and m.name ?2
    ```

<br> <br>

# < AOP >
Aspect Oriented Programming
- 흩어져있는 공통 관심사를 모듈화할 수 있는 프로그래밍 기법
- 공통 관심사항(부가 가능) / 핵심 관심사항(주요 기능) 구분
- 스프링빈에 등록 필요
- 공통 관심사항 작성 후 원하는곳에 적용
    - @Around("execution(* hello.hellospring..*(..))") -> hello.hellospring 패키지 하위 파일들에 모두 적용