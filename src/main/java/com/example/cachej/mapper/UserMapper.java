package com.example.cachej.mapper;


import com.example.cachej.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    UserInfo getUser(Integer id);

    void addUser(UserInfo user);

    void deleteUser(Integer id);

    void updateUser(UserInfo user);

    List<UserInfo> getAllUsers();

    UserInfo getUserByToken(String token);
}
