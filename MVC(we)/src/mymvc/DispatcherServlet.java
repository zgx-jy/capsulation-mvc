package mymvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**

 - 这个类是我们通过分析提炼出来的
 - 这个类中应该只有一个方法
 - 方法就是service方法    这个类是那个唯一的"小总管"
 - 小总管帮我们做请求的分发
 - 某一个具体的Controller类中具体的方法
 *
 - Servlet对象的生命周期问题
 - 1.Tomcat容器帮我们进行管理
 - 2.Servlet对象是一个单例的
 - 3.Servlet对象是延迟加载的
 - 4.用init  service  destroy方法标识对象 创建 使用 销毁
 */

public class DispatcherServlet extends HttpServlet
{
    //当前这个小总管内  添加一个小弟对象作为属性
    private Handler handler = new Handler();

    public void init(ServletConfig config)
    {
        boolean flag = handler.loadPropertiesFile();
        if (flag == false) ///  如果properties 文件不存在 扫描注解
        {
            String scanPackage = config.getInitParameter("scanPackage");
            handler.scanAnnotation((scanPackage));//加载的时候扫描所有的注解
        }
    }

    //小总管  接收所有的.do请求  分发给不同类中的不同方法
    protected void service(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException
    {
        try {
            //0.找到请求的类
            String uri = request.getRequestURI();  //  /20200511MyMVC/AtmController.do

            //1.找1号小弟帮我们解析一下uri
            String requestContent = handler.parseToRequestContent(uri); //  /AtmController.do   query.do

            //2找到请求的方法
            String methodName = request.getParameter("method");//login

            //3.判断方法名 有没有
            if (methodName == null)
            {
                //证明我们没有传方法名字 uri请求的名字就是方法名字
                methodName = requestContent.substring(0,requestContent.indexOf("."));
            }
            System.out.println(requestContent+"执行+1");
            System.out.println(requestContent+"执行+1");
            //4.通过请求名字  找到真实类  找到执行的那个Controller对象
            Object obj = handler.findObject(requestContent);

            //5找到可以执行的那个方法
            Method method = handler.findMethod(obj,methodName);
            //6.找到一个小弟 做方法参数值的DI
            Object[] parameterValues = handler.injectionParameters(method,request,response);

            //7执行方法
            Object methodResult = method.invoke(obj,parameterValues);
            //8.处理返回的响应信息
            handler.finalHandleResponse(obj,method,methodResult,request,response);
           // request.getRequestDispatcher(methodName).forward(request,response);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ControllerNotFoundException e) {
            e.printStackTrace();
        }
    }

}
