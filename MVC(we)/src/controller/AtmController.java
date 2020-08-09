package controller;

import domain.User;
import mymvc.*;
import service.AtmService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 控制层
 *
 * 很多规则
 *      继承父类        不用了
 *      重写方法        不用了
 *      底层单例模式 延迟加载机制还在
 *      方法参数随意  domain 具体的map 单个的值 必须加@Param    request  response
 *      返回值         void String ModeAndView

 */
@RequestMapping("AtmController.do")
@SessionAttributes("name")
public class AtmController {

    private AtmService service = new AtmService();

    //设计一个方法 专门用来做登录那个功能的控制
    //方法是底层那个DispatcherServlet帮我们调用的
    //浏览器发送请求 带着参数---->都去到了DispatcherServlet类中的service方法
    //Dispatcher可以获取到请求的参数，获取到参数以后，参数的值给我们装入变量中
    @RequestMapping("login")
    public ModelAndView login(User user) {
        //1.接收请求的参数 以前利用request 现在利用变量 自动DI  带注解 domain对象 map集合
        //2.比较名字和密码----是一个业务方法做的事情
        String result = service.login(user.getName(),user.getPass());
        ModelAndView mv = new ModelAndView();
        //3.根据登录结果控制响应
        if (result.equals("登录成功")){
            //request.setAttribute("name",name);
            mv.addAttribute("name",user.getName());
            mv.setViewName("welcome.jsp");//转发和重定向 String表示一个路径 | 如果不是以上两种情况 就一个值
        }else {
            //request.setAttribute("result",result);
            //想要将上面这一行的值带走  不能用request   需要将这个值存入一个容器里   框架去找容器拿值
            //容器应该是个什么类型的???    map集合
            //我们把值存入map集合  集合需要交给框架  怎么交?????--->返回值
            //  1.必须给框架一个响应的路径               String  表示需要框架帮忙处理响应路径
            //  2.路径+还有可能有一些key-value需要给框架  String+Map 一次性返回?  对象嘛(路径 map)
            //  3.什么都不给框架???    write();        void 表示不需要框架处理
            mv.addAttribute("result",result);
            mv.setViewName("index.jsp");
        }
        return mv;

        //我想要接收请求的参数值
        //  现在不用自己主动接收  采用方式是IOC思想  别人:小框架DispatcherServlet
        //  我想要值  我就放一个空的变量(目的是为了存值)
        //          小框架通过request接收来的  小框架给我存入我的变量里
        //  现在的需求相反
        //  我想给值  我就把这个值放在一个变量里(变量的目的是存放值 让框架拿走)
        //          小框架去我的变量内取值  通过request.setAttribute();存入request带走


        //=================================================================
        //System.out.println("AtmController类中的login方法执行了"+user.getName()+"--"+user.getPass());
        //1.接收请求的参数  name  pass

        //以前通过String name = request.getParameter("name")方式是一个主动获取数据的方式
        //改变为想要数据 不主动  数据的管理权交给别人

        //利用IOC和DI的设计思想

        //2.做业务(自己不做 找小弟帮忙 Service层的某一个方法)
//        String result = service.login(user.getName(),user.getPass());//对象 集合
        //3.拿到业务的结果 给予响应(写回浏览器 转发JSP 重定向)
        //return "welcome.jsp";//必须得知道该如何转发  forward  现在通过一个基础的String
    }

    //设计一个方法 专门用来做查询那个功能的控制
    @ResponseBody
    public String query(@Param("name")String name,@Param("pass")int pass,HttpServletResponse response){
        System.out.println("AtmController的查询执行了"+name+"--"+pass);
        //1.接收请求的参数  name
        //2.找业务层的方法做事   result = service.query(name);
        //3.给响应(转发)
        //return "query.jsp";
        return "haha";
    }

}
