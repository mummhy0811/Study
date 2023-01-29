package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model){
        model.addAttribute("data", "hello!!");
        return "hello";
    }

    //mvc 연습
    @GetMapping("hello-mvc")
    public String helloMVC(@RequestParam("name")String name, Model model){
        model.addAttribute("name", name);
        return "hello-template";
    }

    //api 연습
    @GetMapping("hello-string")
    @ResponseBody //html응답 바디에 아래 내용을 직접 넣겠다 -> 코드보기를 해도 html코드가 나오는 것이 아닌 문자가 나옴
    public String helloString(@RequestParam("name")String name){
        return "hello "+name;
    }

    @GetMapping("hello-api")
    @ResponseBody
    public Hello helloApi(@RequestParam("name") String name){
        Hello hello = new Hello();
        hello.setName(name);
        return hello; //객체를 만들어서 넘김 -> Json형태로 나온다

    }

    static class Hello{
        private String name;

        public String getName(){
            return name;
        }
        public void setName(String name){
            this.name=name;
        }
    }
}
