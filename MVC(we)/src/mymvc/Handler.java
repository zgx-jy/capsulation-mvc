package mymvc;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * 这是新提取出来的一个类
 * 这个类的目的就是为了支持DispatcherServlet这个小总管干活的
 * 可以理解为这个类中的方法都是"小弟"
 */
public class Handler {

    //属性---->存储请求名字和真实类全名之间的对应关系
    private Map<String,String> realNameMap = new HashMap();
    //属性---->存储每一个Controller类的对象(单例)
    private Map<String,Object> objectMap = new HashMap();
    //属性---->存储某一个Controller对象中的所有方法
    private Map<Object,Map<String, Method>> objectMethodMap = new HashMap();
    //属性---->存储请求直接访问方法的情况   方法名字---真实类名的对应关系
    private Map<String,String> methodWithRealNameMap = new HashMap();

    //设计一个0号小弟 类加载的时候 一次性读取文件中的类名
    boolean loadPropertiesFile(){
        boolean flag = true;//表示文件存在的
        try {
            Properties properties = new Properties();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("abc.properties");
            properties.load(inputStream);
            Enumeration en = properties.propertyNames();//获取全部的key
            while(en.hasMoreElements()){
                String key = (String)en.nextElement();
                String realName = properties.getProperty(key);
                realNameMap.put(key,realName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            flag = false;
        }
        return flag;
    }
    //设计一个0号小弟 类加载的时候 一次性的扫描所有类(xxx.class文件)上面的注解
    //通常我们要扫描的注解应该在Controller类的上面
    //在扫描注解的时候 提供一个包名 controller,service,dao
    void scanAnnotation(String scanPackage){
        if(scanPackage!=null){//证明配置信息存在
            //按照包名中的逗号来拆分
            String[] packageNames = scanPackage.split(",");
            //循环处理每一个包名(也有可能数组就一个长度)
            for(String packageName : packageNames){
                //每一次循环 得到一个名字 packageName
                //包名--->确定一个路径(硬盘上的文件夹)--->加载包中的所有文件
                //包(.)  文件夹表示形式(\)
                URL url = Thread.currentThread().getContextClassLoader().getResource(packageName.replace(".","\\"));
                if(url==null){
                    //证明传递过来的包名可能写错了 不存在
                    continue;
                }
                //根据URL获取包的真实路径了
                String packagePath = url.getPath();
                //根据包的路径 产生一个file对象 映射到硬盘上真实的文件夹
                File packageFile = new File(packagePath);
                //获取当前file对象中的所有子对象(子对象是controller包中的所有class类文件)
                //最好在找寻子文件的过程中做一个筛选(过滤)
//                File[] files = packageFile.listFiles(new FileFilter(){
//                    public boolean accept(File file) {
//                        if(file.isFile() && file.getName().endsWith("class")){
//                            return true;
//                        }
//                        return false;
//                    }
//                });
                File[] files = packageFile.listFiles(file->{
                    if(file.isFile() && file.getName().endsWith("class")){
                        return true;
                    }
                    return false;
                });
                //================================================================
                //以上的部分我们获取到了一个File[] 数组中存储的每一个文件是controller包下的某一些class类
                for(File file : files){//遍历这个File[] 获取里面每一个class类 类中反射读取注解
                    //file--->某一个Controller类上    AtmController.class(内容.java)
                    String fileNameWithEndFix = file.getName();//简单的类名带后缀
                    String fileName = fileNameWithEndFix.substring(0,fileNameWithEndFix.indexOf("."));
                    //这个fileName得到的目的是为了反射获取Class
                    String className = packageName+"."+fileName;//类全名
                    //反射获取Class 获取里面的注解
                    try {
                        Class clazz = Class.forName(className);//clazz一个映射对象 AtmController类
                        RequestMapping classAnnotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                        //类的上面不一定有注解的
                        if(classAnnotation!=null){//类上面有注解
                            //按照原有的方式  请求名字---真实类全名  存储在之前的那个集合里
                            realNameMap.put(classAnnotation.value(),className);
                        }
                        //类的上面无论是否有注解 方法上面肯定有注解的
                        //获取类中全部的方法
                        Method[] methods = clazz.getDeclaredMethods();
                        //循环分析每一个方法上面的注解
                        for(Method method : methods){
                            RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
                            if(methodAnnotation!=null){
                                //直接请求一个方法   请求---找一个方法(类?反射)
                                //方法名字和类名字存一起
                                methodWithRealNameMap.put(methodAnnotation.value(),className);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //设计一个1号小弟 负责将读取到的uri做一个解析
    String parseToRequestContent(String uri){
        return uri.substring(uri.lastIndexOf("/")+1);
    }

    //设计一个2号小弟 负责根据请求名字 找到一个Controller对象
    Object findObject(String requestContent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        //请求名字 找到一个真实对象
        Object obj = objectMap.get(requestContent);
        //如果obj没有 证明从来没用过
        if(obj==null){
            //realNameMap最早加载配置文件时的集合  请求类名---真实类全名对应关系
            //如果没有配置文件  但是类上面有注解  这个集合里面也有信息
            String realClassName = realNameMap.get(requestContent);
            if(realClassName==null){
                //如果realClassName还是空的  要不然就是直接访问了方法  要不然压根不存在
                realClassName = methodWithRealNameMap.get(requestContent);
                if(realClassName==null) {
                    //一会儿自定义异常  类找不到
                    System.out.println(realClassName+"有吗");
                    throw new ControllerNotFoundException(requestContent + "不存在");
                }
            }
            Class clazz = Class.forName(realClassName);
            obj = clazz.newInstance();
            objectMap.put(requestContent,obj);
            //------------------>
            //如果这个对象获取了之后       <obj,Map<方法>>
            //继续将对象中的所有方法都一次性或取出来 备用
            //获取当前obj对象中的所有方法
            Method[] methods = clazz.getDeclaredMethods();
            Map<String,Method> methodMap = new HashMap();//是用来存储这个对象所有方法的
            for(Method method : methods){
                //将method存入刚才那个集合
                methodMap.put(method.getName(),method);
            }
            objectMethodMap.put(obj,methodMap);//上面那个属性 key是对象 值是个存储好多方法的集合
            //------------------>
        }
        return obj;
    }

    //设计一个3号小弟 负责根据接收的method名字找寻方法
    Method findMethod(Object obj,String methodName) {
        //通过obj获取对象中的所有方法
        Map<String,Method> methodMap = this.objectMethodMap.get(obj);
        //找方法
        Method method = methodMap.get(methodName);
        return method;
    }

    //三个小小弟 负责给injectionParameters做支持的
    private Object injectionNormal(Class parameterClazz,Param paramAnnotation,HttpServletRequest request){
        Object result = null;
        //获取注解里面的那个key
        String key = paramAnnotation.value();
        //value = request.getParameter(key);
        String value = request.getParameter(key);
        //value存入最终的那个Object[]
        if(parameterClazz==String.class){
            result = value;
        }else if(parameterClazz==int.class || parameterClazz==Integer.class){
            result = new Integer(value);
        }else if(parameterClazz==float.class || parameterClazz==Float.class){
            result = new Float(value);
        }else if(parameterClazz==double.class || parameterClazz==Double.class){
            result = new Double(value);
        }else if(parameterClazz==boolean.class || parameterClazz==Boolean.class){
            result = new Boolean(value);
        }else{
            throw new IllagelParameterTypeException(parameterClazz.getName()+"参数类型处理不了");
        }
        return result;
    }
    private Map injectionMap(Object obj,HttpServletRequest request){
        //将上面那个obj对象还原成map
        Map map = (Map)obj;
        //获取request中所有的参数把这个map填满
        Enumeration en = request.getParameterNames();
        while(en.hasMoreElements()){
            String key = (String)en.nextElement();//请求中的key
            String value = request.getParameter(key);
            map.put(key,value);
        }
        return map;
    }
    private Object injectionDomain(Object obj,Class parameterClazz,HttpServletRequest request) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //domain
        //反射分析domain中的所有属性
        Field[] fields = parameterClazz.getDeclaredFields();
        for(Field field : fields){
            //遍历每一个属性  获取属性的名字  当做取值的key
            field.setAccessible(true);
            //获取属性名字
            String fieldName = field.getName();
            //去request中取值
            String value = request.getParameter(fieldName);
            //想要把这个value存入domain对象中有问题  属性类型不一定
            //获取属性的类型
            Class fieldType = field.getType();//Integer
            //找寻属性类型对应的构造方法 Integer类中的构造方法
            Constructor con = fieldType.getConstructor(String.class);//new Integer()
            //构造方法执行
            field.set(obj,con.newInstance(value));//new Integer(value);
        }
        return obj;
    }
    //设计一个4号小弟 负责解析方法中的参数 做参数值的自动注入DI  Dependency Injection
    //是否需要参数?   Method  request
    //是否需要返回值? Object[] 目的为了存储当前方法调用时需要的所有参数值
    Object[] injectionParameters(Method method,HttpServletRequest request,HttpServletResponse response) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        //这个小弟的目的 分析method，做参数的解析
        //1.获取method中所有的参数
        Parameter[] parameters = method.getParameters();
        //2.一个严谨的判断
        if(parameters==null && parameters.length==0){
            return null;
        }
        //  Controller方法中传递的参数  基础类型String int float 对象类型domain 集合类型Map
        //3.创建一个Object[] 用于存储最终处理完毕的所有参数值
        Object[] resultValue = new Object[parameters.length];
        //4.遍历每一个参数
        for(int i=0;i<parameters.length;i++){
            Parameter parameter = parameters[i];//某一个参数
            //获取参数类型
            Class parameterClazz = parameter.getType();
            //5.先找寻这个参数前面是否带有注解
            Param paramAnnotation = parameter.getAnnotation(Param.class);
            //判断注解是否存在
            if(paramAnnotation!=null){//有注解 是一个基础类型 String int float
                //找小小弟处理
                resultValue[i] = this.injectionNormal(parameterClazz,paramAnnotation,request);
            }else{//没有注解 是复合类型 map domain
                if(parameterClazz==Map.class || parameterClazz==List.class || parameterClazz==Set.class || parameterClazz.isArray() || parameterClazz==Enum.class){
                    //抛出异常
                    throw new IllagelParameterTypeException(parameterClazz.getName()+"参数类型我处理不了");
                }else{// 具体的map集合 某一个domain
                    if(parameterClazz==HttpServletRequest.class){
                        resultValue[i] = request;continue;
                    }
                    if(parameterClazz== HttpServletResponse.class){
                        resultValue[i] = response;continue;
                    }
                    Object obj = parameterClazz.newInstance();
                    if(obj instanceof Map){
                        resultValue[i] = this.injectionMap(obj,request);
                    }else{
                        resultValue[i] = this.injectionDomain(obj,parameterClazz,request);
                    }
                }
            }
        }
        return resultValue;
    }

    //小小弟 负责解析一下ModelAndView
    private void parseModelAndView(Object obj,ModelAndView mv,HttpServletRequest request){
        //获取mv中的map集合 集合里的信息存入request中
        Map<String,Object> mvMap = mv.getAttributeMap();
        //遍历集合中的元素 存入request作用于
        Iterator<String> it = mvMap.keySet().iterator();
        while(it.hasNext()){
            String key = it.next();
            Object value = mvMap.get(key);
            //将mvMap中的key和value存入request作用域
            request.setAttribute(key,value);
        }
        //继续解析是否带有sessionAttributes注解
        SessionAttributes sessionAttributes = obj.getClass().getAnnotation(SessionAttributes.class);
        if(sessionAttributes!=null){//有一些信息需要存入session里
            String[] attributeNames = sessionAttributes.value();//获取注解里的key
            if(attributeNames.length!=0){
                HttpSession session = request.getSession();
                for(String attributeName : attributeNames){
                    session.setAttribute(attributeName,mvMap.get(attributeName));
                }
            }
        }
    }
    //小小弟 负责处理String类型的
    //  是否需要参数  String类型的"路径"  request  response
    private void handleResponseContent(String methodResult,HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        //严谨性的判断
        if(!"".equals(methodResult) && !"null".equals(methodResult)){
            //如果是正常的路径  到底该转发 还是该重定向
            //根据用户传递过来的字符串 按照:拆分
            String[] value = methodResult.split(":");
            if(value.length == 1){//转发
                request.getRequestDispatcher(methodResult).forward(request,response);
            }else{//重定向     数组的第一个元素  redirect标识   数组的第二个元素重定向的路径
                if("redirect".equals(value[0])){
                    response.sendRedirect(value[1]);//
                }
            }
        }
    }
    //设计一个5号小弟 负责处理最终的响应信息
    //  需要参数  1.给我处理的信息Object  2.request response
    void finalHandleResponse(Object obj,Method method,Object methodResult,HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        //methodResult有可能为void
        if(methodResult!=null){//方法有返回值 需要我们框架帮忙处理
            if(methodResult instanceof ModelAndView){
                //必然需要解析一下ModelAndView对象(集合 存入request里 String路径 获取出来)
                ModelAndView mv = (ModelAndView)methodResult;
                //找一个小小弟 解析一下mv
                this.parseModelAndView(obj,mv,request);
                //解析mv对象只是将里面的map集合处理了  还没有处理最终的String路径
                this.handleResponseContent(mv.getViewName(),request,response);
            }else if(methodResult instanceof String){
                //先来看一看有没有说明性的注解  当做一个普通的值
                ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
                //如果没有注解 交给小弟判断转发 重定向
                if(responseBody!=null){
                    //如果是一个普通的值
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().write((String)methodResult);
                }else{//没有注解 是一个路径
                    String viewName = (String)methodResult;
                    this.handleResponseContent(viewName,request,response);
                }
            }else{
                //当做JSON形式  AJAX+JSON
                ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
                if(responseBody!=null){
                    //有注解 表示一个值 还不是String 需要将这个值变化成JSON返回浏览器
                    JSONObject jsonObject = new JSONObject();//fastjson.jar
                    jsonObject.put("jsonObject",methodResult);//方法返回值的那个集合放入json对象中
                    response.getWriter().write(jsonObject.toJSONString());
                }
            }
        }else{//方法没有返回值 证明方法自己处理响应 框架就无需帮忙了
            System.out.println("OK 你自己干吧");
        }
    }

}
