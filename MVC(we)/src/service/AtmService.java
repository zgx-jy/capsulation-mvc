package service;

public class AtmService
{
    public String login(String name,int pass)
    {
        //理论上应该是调用DAO的方法  读取数据
        if (name.equals("zzt") && pass == 123)
        {
            return "登录成功";

        }
        return "用户名或密码错误 ";
    }
}
