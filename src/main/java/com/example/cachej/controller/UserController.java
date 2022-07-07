package com.example.cachej.controller;

import com.example.cachej.domain.User;
import com.example.cachej.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserServiceImpl userService;

    @GetMapping("/addUser")
    public String addUser(@RequestParam(value = "username") String username,
                          @RequestParam(value = "age") int age,
                          @RequestParam(value = "mobile") String mobile) {

        User user = new User();
        user.setAge(age);
        user.setUsername(username);
        user.setMobile(mobile);
        userService.addUser(user);

        return "add user success";
    }

    @GetMapping("/getUser")
    public User getUser(@RequestParam(value = "id") int id) {
        return userService.getUser(id);
    }

    @GetMapping("/getAllUsers")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/updateUser")
    public String updateUser(@RequestParam(value = "id") int id,
                           @RequestParam(value = "username") String username,
                           @RequestParam(value = "age") int age,
                           @RequestParam(value = "mobile") String mobile) {

        User user = new User();
        user.setId(id);
        user.setAge(age);
        user.setUsername(username);
        user.setMobile(mobile);
        userService.updateUser(user);
        return "update success";
    }

    @GetMapping("/deleteUser")
    public String deleteUser(@RequestParam(value = "id") int id){
        userService.deleteUser(id);
        return "delete success";
    }
}
