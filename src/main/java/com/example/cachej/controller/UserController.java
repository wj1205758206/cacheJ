package com.example.cachej.controller;


import com.example.cachej.domain.UserInfo;
import com.example.cachej.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserServiceImpl userService;

    @GetMapping("/addUser")
    public String addUser(@RequestParam(value = "id") int id,
                          @RequestParam(value = "username") String username,
                          @RequestParam(value = "product") String product,
                          @RequestParam(value = "department") String department,
                          @RequestParam(value = "token") String token,
                          @RequestParam(value = "qps") String qps) {

        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUsername(username);
        user.setProduct(product);
        user.setDepartment(department);
        user.setToken(token);
        user.setQps(qps);
        userService.addUser(user);

        return "add user success";
    }

    @GetMapping("/getUser")
    public UserInfo getUser(@RequestParam(value = "id") int id) {
        return userService.getUser(id);
    }

    @GetMapping("/getAllUsers")
    public List<UserInfo> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/updateUser")
    public String updateUser(@RequestParam(value = "id") int id,
                             @RequestParam(value = "username") String username,
                             @RequestParam(value = "product") String product,
                             @RequestParam(value = "department") String department,
                             @RequestParam(value = "token") String token,
                             @RequestParam(value = "qps") String qps) {

        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUsername(username);
        user.setProduct(product);
        user.setDepartment(department);
        user.setToken(token);
        user.setQps(qps);
        userService.updateUser(user);
        return "update success";
    }

    @GetMapping("/deleteUser")
    public String deleteUser(@RequestParam(value = "id") int id) {
        userService.deleteUser(id);
        return "delete success";
    }
}
