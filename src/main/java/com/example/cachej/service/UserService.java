package com.example.cachej.service;


import com.example.cachej.domain.UserInfo;

import java.util.List;

public interface UserService {

    UserInfo getUser(Integer id);

    UserInfo addUser(UserInfo user);

    void deleteUser(Integer id);

    UserInfo updateUser(UserInfo user);

    List<UserInfo> getAllUsers();

    UserInfo getUserByToken(String token);
}
