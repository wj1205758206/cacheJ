package com.example.cachej.mapper;

import com.example.cachej.domain.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    User getUser(Integer id);

    void addUser(User user);

    void deleteUser(Integer id);

    void updateUser(User user);

    List<User> getAllUsers();
}
