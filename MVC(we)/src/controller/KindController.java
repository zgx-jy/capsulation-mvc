package controller;

import mymvc.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class KindController
{
    //一个方法种类的查询
    @RequestMapping("kindUpdate.do")
    public String kindQuery(HashMap<String,Object> map)
    {
        System.out.println("KindController的查询方法执行了"+map);
        map.get("name");
        map.get("pass");
        return "kindQuery.jsp";
    }

    @RequestMapping("kindUpdate.do")
    public String kindUpdate(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException
    {
        String name = request.getParameter("name");
        String pass = request.getParameter("pass");
        System.out.println("kindController的修改种类执行了"+name+"--"+pass);
        return "kindQuery.jsp";
    }
}
