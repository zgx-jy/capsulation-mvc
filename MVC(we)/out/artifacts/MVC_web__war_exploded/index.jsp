<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>$Title$</title>
  </head>
  <body>
  <!--Controller个数跟service业务层dao持久层个数一致的-->

  <!--请求的时候需要携带两个信息  类名  方法名-->
  <!--  XxxController.do?method=xxx   -->
  <!--  HTTP协议  有规则-->
  <!--  ip:port/com/content.do?key=value&key=value  -->
  <!--  : . ? 这几个符号不能出现多次   -->
  ${result}
  <a href="AtmController.do?method=login&name=zzt&pass=123">测试1</a><br>

  <a href="AtmController.do?method=query&name=zzt&pass=123">测试2(模拟一个ATM的查询余额)</a><br>

  <a href="kindQuery.do?name=zzt&pass=123">测试3(模拟一个购物系统的种类查询)</a><br>

  <a href="kindUpdate.do?name=zzt&pass=123">测试4(模拟一个购物系统的种类修改)</a><br>
  </body>
</html>
