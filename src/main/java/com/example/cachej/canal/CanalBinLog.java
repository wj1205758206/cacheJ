package com.example.cachej.canal;

import com.example.cachej.domain.UserInfo;


import java.util.List;

/**
 * kafka中接收到的binlog示例
 * {
 * "data":[
 * {
 * "id":"14",
 * "username":"zhangs11an",
 * "product":"ffg",
 * "department":"fggg",
 * "token":"111",
 * "qps":"1"
 * }
 * ],
 * "database":"user_info",
 * "es":1657275193000,
 * "id":1,
 * "isDdl":false,
 * "mysqlType":{
 * "id":"bigint(20)",
 * "username":"varchar(64)",
 * "product":"varchar(256)",
 * "department":"varchar(256)",
 * "token":"varchar(256)",
 * "qps":"varchar(256)"
 * },
 * "old":null,
 * "pkNames":[
 * "id"
 * ],
 * "sql":"",
 * "sqlType":{
 * "id":-5,
 * "username":12,
 * "product":12,
 * "department":12,
 * "token":12,
 * "qps":12
 * },
 * "table":"user_info",
 * "ts":1657276332539,
 * "type":"INSERT"
 * }
 */
public class CanalBinLog {
    //数据
    private List<UserInfo> data;
    //数据库名称
    private String database;
    private long es;
    //递增，从1开始
    private int id;
    //是否是DDL语句
    private boolean isDdl;
    //表结构的字段类型
    private MysqlType mysqlType;
    //UPDATE语句，旧数据
    private String old;
    //主键名称
    private List<String> pkNames;
    //sql语句
    private String sql;
    private SqlType sqlType;
    //表名
    private String table;
    private long ts;
    //增删改查 insert/update/delete
    private String type;

    public List<UserInfo> getData() {
        return data;
    }

    public void setData(List<UserInfo> data) {
        this.data = data;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public long getEs() {
        return es;
    }

    public void setEs(long es) {
        this.es = es;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDdl() {
        return isDdl;
    }

    public void setDdl(boolean ddl) {
        isDdl = ddl;
    }

    public MysqlType getMysqlType() {
        return mysqlType;
    }

    public void setMysqlType(MysqlType mysqlType) {
        this.mysqlType = mysqlType;
    }

    public String getOld() {
        return old;
    }

    public void setOld(String old) {
        this.old = old;
    }

    public List<String> getPkNames() {
        return pkNames;
    }

    public void setPkNames(List<String> pkNames) {
        this.pkNames = pkNames;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
