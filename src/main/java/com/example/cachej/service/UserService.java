package com.example.cachej.service;

import com.example.cachej.domain.User;

import java.util.List;

public interface UserService {

    User getUser(Integer id);

    User addUser(User user);

    void deleteUser(Integer id);

    User updateUser(User user);

    List<User> getAllUsers();
}
