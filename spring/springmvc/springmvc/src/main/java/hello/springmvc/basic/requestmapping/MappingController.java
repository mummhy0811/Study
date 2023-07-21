package hello.springmvc.basic.requestmapping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class MappingController {

    @RequestMapping("/hello-basic") //hello-basic URL 호출이 오면 이 메서드가 실행되도록 매핑
    //method 속성으로 HTTP 메서드를 지정하지 않으면 HTTP 메서드와 무관하게 호출
    public String helloBasic() {
        log.info("helloBasic");
        return "ok";
    }


    // method 특정 HTTP 메서드 요청만 허용 - GET, HEAD, POST, PUT, PATCH, DELETE중에 하나나
    @RequestMapping(value = "/mapping-get-v1", method = RequestMethod.GET)
    public String mappingGetV1() {
        log.info("mappingGetV1");
        return "ok";
    }
    /**
     * 편리한 축약 애노테이션 (코드보기)
     * @GetMapping
     * @PostMapping
     * @PutMapping
     * @DeleteMapping
     * @PatchMapping
     */
    @GetMapping(value = "/mapping-get-v2")
    public String mappingGetV2() {
        log.info("mapping-get-v2");
        return "ok";
    }

    //최근 선호하는 방식
    /**
     * PathVariable 사용
     * 변수명이 같으면 생략 가능 - @PathVariable("userId") String userId -> @PathVariable userId
     */
    @GetMapping("/mapping/{userId}") //URL경로 템플릿화
    public String mappingPath(@PathVariable("userId") String data) {
        log.info("mappingPath userId={}", data);
        return "ok";
    }
    //다중mapping
    @GetMapping("/mapping/users/{userId}/orders/{orderId}")
    public String mappingPath(@PathVariable String userId, @PathVariable Long
            orderId) {
        log.info("mappingPath userId={}, orderId={}", userId, orderId);
        return "ok";
    }
}
