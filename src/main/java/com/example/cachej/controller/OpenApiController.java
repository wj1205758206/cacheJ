package com.example.cachej.controller;

import com.example.cachej.domain.Student;
import com.example.cachej.domain.UserInfo;
import com.example.cachej.service.StudentServiceImpl;
import com.example.cachej.service.UserService;
import com.example.cachej.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiController.class);

    @Autowired
    UserServiceImpl userService;
    @Autowired
    StudentServiceImpl studentService;

    @GetMapping("/openapi/getStudent")
    public String getStudentById(@RequestParam(value = "id") int id,
                                 @RequestParam(value = "token") String token) {
        //鉴权（二级缓存查询）
        UserInfo user = userService.getUserByToken(token);
        if (user == null) {
            LOG.warn("[OpenApiController] user not exist! token=" + token);
            return "No Auth!";
        }

        //限流控制
        String qps = user.getQps();
        boolean isAllow = studentService.flowControl(token, qps);
        if (isAllow) {
            Student studentInfo = studentService.getStudentInfo(id);
            return studentInfo.toString();
        }
        return "flow control, limit! ";
    }
}
