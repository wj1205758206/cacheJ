package com.example.cachej.mapper;


import com.example.cachej.domain.Student;
import com.example.cachej.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StudentMapper {

    Student getStudentInfo(Integer id);


}
