package mymvc;

import java.util.HashMap;
import java.util.Map;

/**
 * model  数据模型( 存储数据 map)\
 * void 视图  用来展示( 转发路径  string)*/
public class ModelAndView
{
    //两个属性
    private String viewName;//视图的响应路径
    private Map<String,Object> attributeMap = new HashMap<>();

    //用来存储最终响应路径
    public void setViewName(String viewName)
    {
        this.viewName = viewName;
    }

    //这个方法每一次将一组键值对填入jihe
    public void addAttribute(String key,Object value)
    {
        this.attributeMap.put(key,value);
    }

    //获取值的方法  方法留个框架来使用
    String getViewName(){
        return this.viewName;
    }
    Object getAttrribute(String key){
        return this.attributeMap.get(key);
    }
    Map<String , Object> getAttributeMap(){
        return this.attributeMap;
    }

}
