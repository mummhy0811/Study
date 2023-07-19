package hello.springmvc.basic;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController //restapi의 rest를 의미 // return 값의 string이 그대로 실행 결과로 반환됨
public class LogTestController {
    //private final Logger log = LoggerFactory.getLogger(getClass()); //@Slf4j와 같은 기능

    @RequestMapping("/log-test")
    public String logTest() {
        String name = "Spring";

        //로그의 레벨 - 상위의 것이 하위 로그를 전부 포함 (TRACE > DEBUG > INFO > WARN > ERROR)
        //application.properties에서 레벨 설정
        log.trace("trace log={}", name); //로컬에서 주로
        log.debug("debug log={}", name); //개발 서버에서 주로
        log.info(" info log={}", name); //운영 서버에서 주로 (기본)
        log.warn(" warn log={}", name); //경고
        log.error("error log={}", name); //에러
        //로그를 사용하지 않아도 a+b 계산 로직이 먼저 실행됨, 이런 방식으로 사용하면 X
        log.debug("String concat log=" + name);
        return "ok";
    }
}
