package cn.summerwaves.controller;

import cn.summerwaves.model.User;
import cn.summerwaves.service.IUserService;
import cn.summerwaves.util.EmailUtil;
import cn.summerwaves.util.QiNiuUtil;
import cn.summerwaves.util.SMSUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class UserController {
    private static Logger log = Logger.getLogger(SMSUtil.class);
    @Autowired
    private IUserService userService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String toLogin() {
        return "login";
    }

    //获取主页
    @RequestMapping("/")
    public String welcome() {
        return "welcome";
    }

    //获取email注册页面
    @RequestMapping(value = "/email/user", method = RequestMethod.GET)
    public String toEmailRegister() {
        return "registerByEmail";
    }

    //获取短信注册页面
    @RequestMapping(value = "/sms/user", method = RequestMethod.GET)
    public String toSMSRegister() {
        return "registerBySMS";
    }

    //获取全部用户展示页面
    @RequestMapping(value = "/users",method = RequestMethod.GET)
    public ModelAndView toShowAllUser() {
        ModelAndView mv = new ModelAndView();
        List<User> users = userService.selectIdAvatarUsernameFromAllUser();
        mv.setViewName("showAllUser");
        mv.addObject("users", users);
        return mv;
    }

    //获取单个用户信息页面
    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public ModelAndView toShowUser(@PathVariable("id")int id) {
        ModelAndView mv = new ModelAndView();
        User user = userService.selectUserById(id);
        if (user.getEmail() == null) {
            user.setEmail("请设置邮箱！");
        }
        if (user.getTel() == null) {
            user.setTel("请设置手机号码！");
        }
        mv.addObject("user",user);

        mv.setViewName("showUser");
        return mv;
    }

    //获取修改用户信息页面
    @RequestMapping(value = "/user/modify", method = RequestMethod.GET)
    public ModelAndView toModify(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        int id = Integer.parseInt(request.getParameter("id"));
        mv.addObject("user", userService.selectUserById(id));
        mv.setViewName("modify");
        return mv;
    }

    //修改用户信息
    @RequestMapping(value = "/user/{id}", method = RequestMethod.POST)
    @ResponseBody
    public String Modify(@PathVariable("id") int id, @RequestParam(value = "file")MultipartFile file, User user) throws IOException {
        if (!file.isEmpty()) {
            String type = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf("."));
            String username = user.getUsername();
            InputStream inputStream = file.getInputStream();
            QiNiuUtil.upLoad(inputStream, username + type);
            user.setAvatar(QiNiuUtil.getFileUrl(username + type));
        }
        if (Objects.equals(user.getTel(), "")) {
            user.setTel(null);
        }
        if (Objects.equals(user.getEmail(), "")) {
            user.setEmail(null);
        }
        userService.updateUser(user);
        return "modifySuccess";
    }


/*    @RequestMapping(value = "users", method = RequestMethod.POST)
    public String showAllUser() {

    }*/

    //提交email注册表单
    @RequestMapping(value = "/email/user", method = RequestMethod.POST)
    public ModelAndView registerByEmail(HttpServletRequest request, User user) {
        ModelAndView mv = new ModelAndView();

        //获取JSP参数
/*        String username = request.getParameter("username");
        String password = request.getParameter("password");*/
        String code = request.getParameter("code");
        String userCode = request.getParameter("userCode");
        String username = user.getUsername();
        String password = user.getPassword();

        //防止重复的用户名，以及用户名、密码为空，并要求用户名要不小于2，验证验证码
        if (userService.selectUserByName(username) == null && !username.equals("")
                && !password.equals("") && username.length() >= 2 && code.equals(userCode)) {
            //插入数据库前对密码加盐
            user.setPassword(userService.setPasswordBySalt(username, password));
            userService.insertUser(user);

            mv.addObject("username", user.getUsername());
            mv.addObject("email", user.getEmail());
            mv.setViewName("registerSuccess");
            return mv;
        }
        mv.setViewName("registerFail");
        return mv;
    }

    //提交短信注册表单
    @RequestMapping(value = "/sms/user", method = RequestMethod.POST)
    public ModelAndView registerBySMS(HttpServletRequest request,User user) {
        ModelAndView mv = new ModelAndView();

        //获取JSP参数
/*        String username = request.getParameter("username");
        String password = request.getParameter("password");*/
        String code = request.getParameter("code");
        String userCode = request.getParameter("userCode");
        String username = user.getUsername();
        String password = user.getPassword();

        //防止重复的用户名，以及用户名、密码为空，并要求用户名要不小于2，验证验证码
        if (userService.selectUserByName(username) == null && !username.equals("") &&
                !password.equals("") && username.length() >= 2 && code.equals(userCode)) {

            //插入数据库前对密码加盐
            user.setPassword(userService.setPasswordBySalt(username, password));
            userService.insertUser(user);

            mv.addObject("username", user.getUsername());
            mv.setViewName("registerSuccess");
            return mv;
        }

        mv.setViewName("registerFail");
        return mv;
    }

    //制作短信验证码并发送
    @RequestMapping(value = "/sms/code", method = RequestMethod.POST)
    @ResponseBody
    public String sendSMSCode(@RequestBody Map<String, String> map,HttpSession session) {
        session.invalidate();
        int intCode = (int)((Math.random() * 9 + 1) * 1000);
        String code = String.valueOf(intCode);
        String tel = map.get("tel");
        SMSUtil.sendSMS(tel, code, "5");
//        session.setAttribute("code", code);
        log.error("SMS ResponseBody");
        return code;
    }

    //制作email验证码并发送
    @RequestMapping(value = "/email/code", method = RequestMethod.POST)
    @ResponseBody
    public String sendEmailCode(@RequestBody Map<String, String> map,HttpSession session) {
        session.invalidate();
        int intCode = (int)((Math.random() * 9 + 1) * 1000);
        String code = String.valueOf(intCode);
        String tel = map.get("email");
        EmailUtil.sendEmail(tel, code);
//        session.setAttribute("code", code);
        log.error("Email ResponseBody");
        return code;
    }



   /* @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String getSMSTest(@ModelAttribute User user) {
        return "register";
    }



    @RequestMapping(value = "/sms/test", method = RequestMethod.POST)
    public ModelAndView SMSRegisterTest( HttpServletRequest request,HttpSession session) {
        ModelAndView mv = new ModelAndView();
        User user = new User();
        user.setUsername(request.getParameter("username"));
        user.setPassword(request.getParameter("password"));
        String confirmPwd = request.getParameter("confirmPwd");
*//*        String code = (String) session.getAttribute("code");*//*
        String code = request.getParameter("code");
        String userCode = request.getParameter("userCode");
        log.error("code:" + code + ",userCode:" + userCode);
        if (confirmPwd.equals(user.getPassword()) && code.equals(userCode)) {
            userService.insertUser(user);
            mv.addObject("username", user.getUsername());
            mv.setViewName("registerSuccess");
            return mv;
        }
        mv.setViewName("registerFail");
        return mv;
    }*/


}
