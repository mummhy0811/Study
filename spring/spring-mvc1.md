# 1. 웹 어플리케이션의 이해

## 1-1 웹 서버, 웹 애플리케이션 서버
### 웹 서버(Web Server)
- HTTP 기반으로 동작
- 정적 리소스 제공, 기타 부가기능

### 웹 애플리케이션 서버(WAS - Web Application Server)
- HTTP 기반으로 동작
- 웹 서버 기능 포함 + 정적 리소스 제공 가능
- 프로그램 코드를 실행해서 애플리케이션 로직 수행 -> __동적이 가능!__
    - 동적 HTML, HTTP API(JSON)
    - 서블릿, JSP, 스프링MVC

### 웹 서버 vs 웹 어플리케이션 서버
- 웹 서버는 정적 리소스, was는 애플리케이션 로직
    - 그러나, 웹 서버도 프로그램을 실행하는 기능을 포함하기도 함
    - 웹 어플리케이션 서버도 웹 서버 기능을 제공
    -> 경계가 모호하다
- 자바는 서블릿 컨테이너 기능을 제공하면 WAS
    - 그러나, 최근 서블릿 없이 자바코드를 실행하는 서버 프레임워크도 있음
- => "```WAS는 애플리케이션 코드를 실행하는 데에 더 특화되어있다```" 라고 생각하면 됨


### 웹 시스템 구성 -WAS, DB
- WAS, DB만으로 시스템 구성 가능
- WAS는 정적 리소스, 애플리케이션 로직 모두 제공   
#### ❗ 그러나,
- WAS가 너무 많은 역할을 담당 -> 서버 과부하 우려
- 가장 비싼(복잡한, 중요한) 애플리케이션 로직이 정적 리소스 때문에 수행이 어려울 수 있음
- WAS 장애시 오류 화면도 노출 불가능

### 📍 웹 시스템 구성 - ```WEB, WAS, DB```
- 정적 리소스는 웹 서버가 처리
- 웹 서버는 동적인 처리가 필요하면 WAS에 요청을 위임
- WAS는 중요한 애플리케이션 로직 처리 전담    
-> 효율적인 리소스 관리가 가능
    - 정적 리소스가 많이 사용되면 Web 서버 증설
    - 애플리케이션 리소스가 많이 사용되면 WAS 증설
- 정적 리소스만 제공하는 웹 서버는 잘 죽지 않음 -> WAS,DB 장애시 WEB 서버가 오류 화면 제공 가능
- 회사끼리 정보만 주고받는 등, 화면이 필요 없는 경우엔 굳이 WEB이 필요 없음

<br><br>

## 1-2 서블릿
### 서버에서 의미있는 __비지니스 로직 이외의 업무__ 들(http 요청 메세지 파싱, http 응답 메세지 생성 등)을 처리해줌
``` java
@WebServlet(name = "helloServlet", urlPatterns = "/hello")
public class HelloServlet extends HttpServlet {
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response){
 //애플리케이션 로직
 }
}
```
### 특징
- urlPatterns의 url이 호출되면 서블릿 코드가 실행됨
- response, request를 서블릿이 자동으로 파싱해주기 때문에, 해당 객체를 사용하면 됨 (위 코드의 파라미터)
- 개발자는 HTTP 스펠을 매우 편리하게 사용 가능

### HTTP 요청, 응답 흐름
- HTTP 요청시
    - WAS는 Request, Response 객체를 새로 만들어서 서블릿 객체 호출
    - 개발자는 편리하게 Request객체에서 요청 정보를 꺼내고, Response객체에 응답 정보를 입력할 수 있음
    - WAS는 Response 객체에 담긴 내용으로 HTTP 응답 정보를 생성

### 서블릿 컨테이너
- 서블릿을 지원하는 WAS를 서블릿 컨테이너라고 함
- 서블릿 객체를 생성, 초기화, 호출, 종료하는 생명주기를 관리하는 역할
- 서블릿 객체는 싱글톤으로 관리(객체를 하나만 생성해서 공유)
- 동시 요청을 위한 멀티 쓰레드 처리 지원

<br><br>

## 1-3 동시 요청 - 멀티 쓰레드
### 쓰레드
- 애플리케이션 코드를 순차적으로 실행하는 것
- 쓰레드가 없으면 애플리케이션 실행 불가능
- 쓰레드는 한 번에 하나의 코드 라인만 수행
- 동시 처리가 필요하면 쓰레드 추가 생성 필요

### 요청이 동시에 올 경우 처리 방법
1. 요청 마다 쓰레드 생성
    - 장점
        - 동시 요청 처리 가능
        - 리소스(CPU, 메모리)가 허용할 때까지 처리 가능
        - 쓰레드 하나가 지연 돼도, 나머지 쓰레드 정상 작동
    - 단점
        - 쓰레드 생성 비용 비쌈(시간)
        - 컨텍스트 스위칭 비용 발생
        - 요청이 너무 많으면, 임계점을 넘어 서버가 죽을 수 있음
2. 쓰레드 풀
    - 쓰레드를 미리 만들어 풀에 보관(일정 개수)
    - 들어오는 요청은 모두 쓰레드 풀에 있는 쓰레드를 꺼내서 사용하고, 사용이 끝나면 반납
    - 장점
        - 쓰레드가 미리 생성되어 있기 때문에 쓰레드 생성 및 종료 비용이 절약되고, 응답 시간이 빠름
        - 쓰레드에 제한이 있으므로 너무 많은 요청이 들어오더라도 임계점을 넘지 않음(안전함)

### WAS의 멀티 쓰레드 지원
    - 멀티 쓰레드에 대한 부분은 WAS가 처리
    - 개발자는 멀티 쓰레드 관련 코드를 신경쓸 필요 없음
    - 단, 싱글톤 객체(서블릿, 스프링 빈)는 주의해서 사용 필요

<br><br>

# 2. 서블릿
## 2-1. 서블릿 기초
### 스프링 부트 서블릿 환경 구성
- ```@ServletComponentScan``` : 스프링부트가 서블릿을 직접 등록해서 사용할 수 있도록 지원하는 어노테이션. 메인의 @SpringBootApplication 어노테이션 위에 작성하면 됨.
- ```@WebServlet``` : 해당 클래스가 ServletComponentScan의 대상이라는 것을 명시
    - name:서블릿 이름
    - urlPatterns: URL 매핑
- 일반적으로 서블릿 이름은 클래스명과 동일하되, 첫 글자를 소문자로 하여 작성
- 매핑된 url이 호출되면 service메서드가 실행됨
- 코드 및 화면

    ```java
    @WebServlet(name="helloServlet", urlPatterns = "/hello")
    public class HelloServlet extends HttpServlet {

        //서블릿이 호출되면 이 service 메소드가 실행됨
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

            System.out.println("HelloServlet.service");
            System.out.println("request = " + request);
            System.out.println("response = " + response);

            String username = request.getParameter("username");
            System.out.println("username = " + username);

            //헤더 정보에 들어감
            response.setContentType("text/plain");
            response.setCharacterEncoding("utf-8");
            //http 메세지 바디에 데이터가 들어감
            response.getWriter().write("hello " + username);
        }
    }
    ```
    <img src="images/spring/servlet_log1.jpg" width="30%"/>  <br>
    <img src="images/spring/servlet_html1.jpg" width="30%"/>  