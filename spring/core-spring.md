# 스프링 핵심 원리

## 스프링이란?
- 자바 언어 기반의 프레임워크
-  __객체 지향 언어__ 가 가진 강력한 특징을 살려냄
- \>> 좋은 객체 지향 애플리케이션을 개발할 수 있게 돕는 프레임워크 <<

<br>
<hr>
<br>

## 제어의 역전 IoC(Inversion of Control)
- 프로그램의 __제어 흐름__ 을 직접 제어하는 것이 아니라 __외부에서 관리__ 하는 것을 뜻함
- 기존 프로그램은 클라이언트 구현 객체가 스스로 필요한 서버 구현 객체를 생성하고, 연결하고 실행(구현 객체가 프로그램의 제어 흐름을 스스로 조종함)
- ``` AppConfig```가 등장한 후, 구현 객체는 자신의 로직을 실행하는 역할만 담당하게 되고 제어 흐름에 대한 모든 권한은 AppConfig에 부여

<br>

## 의존관계 주입 DI(Dependency Injection)
- 애플리케이션 실행 시점(런타임)에 외부에서 실제 구현 객체를 생성하고 클라이언트에 전달해서 클라이언트와 서버의 실제 의존관계가 연결되는 것을 ``의존관계 주입`` 이라고 함
- 객체 인스턴스를 생성하고, 그 참조값을 전달해서 연결됨
- 클라이언트 코드를 변경하지 않고, 클라이언트가 호출하는 대상의 타입 인스턴스 변경 가능
- 정적인 클래스 의존관계를 변경하지 않고, 동적인 객체 인스턴스 의존관계를 쉽게 변경 가능

💥 참고    

    ✔ 정적인 클래스 의존관계   
    - 클래스가 사용하는 import 코드만 보고 의존관계 파악 가능   
    - 애플리케이션을 실행하지 않아도 분석 가능   
    ✔ 동적인 객체 인스턴스 의존관계   
    - 애플리케이션 실행 시점에 실제 생성된 객체 인스턴스의 참조가 연결된 의존 관계 ( -> appConfig에서 일어나는 동작같은?)   
 

## IoC 컨테이너, DI 컨테이너
- 객체를 생성하고 관리하면서 의존관계를 연결해주는 것을 IoC컨테이너 또는 DI컨테이너라고 함
- 어샘블러, 오브젝트 팩토리 등으로 불리기도 함

<br>
<hr>
<br>

## 스프링 컨테이너
- ```ApplicationContext``` 를 스프링 컨테이너라고 함
- 스프링 컨테이너는 ```@Configuration``` 이 붙은 appConfig를 구성 정보로 사용하며, 여기서 ```@Bean``` 이 붙은 메서드를 모두 호출하여 반환된 객체를 스프링 컨테이너에 등록함
- 스프링 컨테이너에 등록된 객체를 __스프링 빈__ 이라고 함
- 스프링 빈은 @Bean이 붙은 메서드의 명을 스프링 빈의 이름으로 사용
- ```applicationContext.getBean()``` 메서드를 통해 필요한 스프링 빈(객체)를 찾음

<br>

### 스프링 컨테이너 생성
``` java
ApplicationContext applicationContext = new AnnotationCongifApplicationContext(AppConfig.class);
```
- ApplicationContext는 인터페이스
- 스프링 컨테이너는 XML을 기반으로 만들 수 있고, 애노테이션 기반의 자바 설정 클래스로도 만들 수 있음
- ```AppConfig```를 사용했던 방식이 애노테이션 기반의 자바 설정 클래스로 스프링 컨데이너를 만든 것.

### 과정
1. ```new AnnotationCongifApplicationContext(AppConfig.class)```를 통해 AppConfig정보를 파라미터로 넘기며(구성 정보를 지정해주며) __스프링 컨테이너 생성__
2. 스프링 컨테이는 파라미터로 넘어온 설정 클래스 정보를 사용하여 __스프링빈 등록__.  (```@Bean``` 탐색)
    - ```@Bean```을 찾아가 빈 이름에는 메서드 이름을, 빈 객체에는 리턴값을 저장
    - ```@Bean(name="memberService2")``` 와 같이 빈 이름 직접 부여 가능 -> 빈 이름이 중복되지 않도록 주의 필요
3. 스프링 컨테이너는 설정 정보를 참고해서 __의존관계를 주입(DI)__ 함. (@Bean 리턴값의 파라미터를 통해)

<br>

## 스프링빈 조회 - 테스트코드로
1. 모든 빈 출력
    - 스프링에 등록된 모든 빈 정보를 출력
    - ```ac.getBeanDefinitionNames()``` : 스프링에 등록된 모든 빈 이름을 조회
    - ```ac.getBean()``` : 빈 이름으로 빈 객체(인스턴스)를 조회
2. 애플리케이션 빈 출력
    - 내가 등록한 빈만 출력
    - ROLE_APPLICATION : 일반적으로 사용자가 정의한 빈
    - ROLE_INFRASTRUCTURE : 스프링이 내부에서 사용하는 빈
    ``` java
        if (beanDefinition.getRole() == BeanDefinition.ROLE_APPLICATION) { 
            Object bean = ac.getBean(beanDefinitionName);
            System.out.println("name=" + beanDefinitionName + " object=" +bean);
        }
    ```
3. 기본 조회
    - 빈 이름으로 조회: ```ac.getBean(빈 이름, 타입)```
    - 빈 이름 없이 타입으로 조회: ```ac.getBean(타입)```
    - 조회 대상 스프링빈이 없으면 예외 발생 -> ```NoSuchBeanDefinitionException: No bean named 'xxxxx' available```
    - 예외 조회(예외가 발생해야 테스트 통과) -> ```Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> ac.getBean("xxxxx", MemberService.class));```   
4. 동일한 타입이 둘 이상인 타입조회
    - 타입으로 조회시 같은 타입의 스프링 빈이 둘 이상이면 오류가 발생 (```NoUniqueBeanDefinitionException```)-> 빈 이름을 지정하면 됨
    - ```ac.getBeansOfType()``` : 해당 타입의 모든 빈 조회
    ```java
        Map<String, MemberRepository> beansOfType = ac.getBeansOfType(MemberRepository.class);
        System.out.println("beansOfType = " + beansOfType);
    ```
5. 상속관계인 경우의 조회
    - 부모 타입으로 조회하면 자식 타입도 함께 조회됨
    - 자바 객체 최고의 부모인 ```Object```타입으로 조회하면 모든 스프링빈 조회 가능
    - 부모 타입으로 조회시, 자식이 둘 이상이면 중복오류 발생 -> 빈 이름 지정 또는 특정 하위타입으로 조회 필요


<br>

---

<br>

## BeanFactory 와 ApplicationContext

### BeanFactory
- 스프링 컨테이너의 최상위 인터페이스
- 스프링빈을 관리하고 조회하는 역할 담당
- ```getBean()``` 을 제공

### ApplicationContext
- BeanFactory 기능을 모두 상속받아서 제공
- 빈을 관리하고 검색하는 기능(BeanFactory의 기능)은 물론, 부가기능까지 제공
- 부가 기능
    - MessageSource (메세지 소스를 이융한 국제화 기능) 
    - EnvironmentCapable (환경변수): 로컬, 개발, 운영등을 구분해서 처리
    - ApplicationEventPublisher (애플리케이션 이벤트): 이벤트를 발행하고 구독하는 모델을 편리하게 지원
    - ResourceLoader (편리한 리소스 조회): 파일, 클래스패스, 외부 등에서 리소스를 편리하게 조회

<br>

---

<br>


## 싱글톤 패턴과 컨테이너
- 스프링 없는 순수한 DI컨테이너인 AppConfig는 요청할 때마다 객체를 새로 생성 -> 메모리 낭비가 심하다
- 해결방안: 객체를 1개만 생성하여 이를 공유하도록 설계한다 -> ```싱글톤 패턴```

<br>

### 싱글톤 패턴
- 클래스의 인스턴스가 1개만 생성되도록 보장하는 디자인 패턴

    ``` java
    public class SingletonService {

        private static final SingletonService instance = new SingletonService();
        //private static으로 선언 -> 자기 안에 딱 하나만 존재. 객체를 생성해서 instance에 참조를 넣어둠.

        public static SingletonService getInstance(){ //조회
            return instance;
        }

        private SingletonService(){ }
    }
    ```

- static 영역애 객체 isntance를 미리 하나 생성 (java가 실행될 때)
- 해당 인스턴스가 필요하면 ```getInstance()``` 를 통해서만 조회 가능 -> 항상 같은 인스턴스 반환
- 생성자를 ``private`` 로 설정하여 외부에서 new 키워드로 새 객체 인스턴스가 생성되는 것을 막아 딱 한 개의 인스턴스만 존재하도록 함.

- __싱글톤 패턴의 문제점__
    - 싱글톤 패턴 구현 코드 자체가 김
    - 의존관계상 클라이언트가 구체 클래스에 의존 -> DIP 위반, OCP 원칙을 위반할 가능성이 높음
    - 테스트 힘듦
    - 내부 속성 변경이나 초기화가 어려움
    - private 생성자 사용으로 자식 클래스를 만들기 어려움
    - 유연성이 떨어짐
    - 안티패턴으로 불리기도 함

### 싱글톤 컨테이너
- 싱글톤 컨테이너는 싱클톤 패턴을 적용하지 않아도, 객체 인스턴스를 싱글톤으로 관리
- ```싱글톤 레지스트리``` : 싱글톤 객체를 생성하고 관리하는 기능
- 스프링 컨테이너가 싱글톤 컨테이너 역할을 함
    - 싱글톤 패턴을 위한 긴 코드가 들어가지 않아도 됨
    - DIP, OCP, 테스트, private 생성자로부터 자유롭게 싱글톤 사용 가능
- __싱글톤 방식의 주의점__
    - 객체 인스턴스를 하나만 생성해서 공유하는 싱글톤 방식은 여러 클라이언트가 하나의 같은 객체 인스턴스를 공유하기 때문에 싱그톤 객체는 상태를 유지(stateful)하게 설계하면 안됨
    - ```무상태(stateless)```로 설계해야 함
        - 특정 클라이언트에 의존적인 필드가 있으면 안됨
        - 특정 클라이언트가 값을 변경할 수 있는 필드가 있으면 안됨
        - 가급적 읽기만 가능해야함
        - 필드 대신 자바에서 공유되지 않는 지역변수, 파라미터, ThreadLocal 등을 사용해야 함.
    - 스프링빈의 필드에 __공유값__ 을 설정하면 __큰 장애__ 가 발생할 수 있음
