package com.codegen.example.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;


public final class CodegenUtil {
    
    public static ThreadLocal<Connection> pools = new ThreadLocal<>() ;
    
    private static String SPLIT = "[\\W_]+";
    
    private static String DATE = "DATE";
    
    private static String NUMBER = "NUMBER";
    
    private static String VARCHAR2 = "VARCHAR2";
    
    private static String DATA_TYPE = "DATA_TYPE";
    
    private static String COLUMN_NAME = "COLUMN_NAME";

    public static String dest(String path) {
        // 取得使用者路徑
        String workSpace = System.getProperty("user.dir");
        // 檔案放置的位置 將\\替換成/變成階層
        String dest = workSpace + "/src/" + path.replaceAll("\\.", "/");
        return dest;
    }
    
    @SuppressWarnings("deprecation")
    public static void write(String dest, String tableName, StringBuilder sb) {
        try {
            FileUtils.write(new File(dest + "/" + tableName + ".java"), sb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() {

        Connection conn = pools.get();
        if (conn == null) {
            try {
                Class.forName("oracle.jdbc.OracleDriver");
                conn = DriverManager.getConnection("jdbc:oracle:thin:@//61.216.84.217:1534/ORCL", "line", "123456");
                pools.set(conn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    public static void close(Connection conn , PreparedStatement pstmt, ResultSet rs) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
    
    public static StringBuilder camel(String name) {
        StringBuilder builder = new StringBuilder();
        String[] words = name.split("[\\W_]+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
            word = word.isEmpty() ? word : word.substring(0,1)+word.substring(1).toLowerCase();
            } else {
            word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
            }
        return builder;
    }
    
    public static StringBuilder voData(StringBuilder sb,ResultSet rs) {
        String line = "";
        try {
            
            // private的 類別及屬性名稱
            while (rs.next()) {
                if(VARCHAR2.equals(rs.getString(DATA_TYPE))) {
                    line = "String";
                }else if(NUMBER.equals(rs.getString(DATA_TYPE))){
                    line = "int";
                }else if(DATE.equals(rs.getString(DATA_TYPE))) {
                    line = "Date";
                }
                sb.append("    /*\n     *" + rs.getString("COMMENTS") + "\n     */\n");
                sb.append("     private " + line + " " + camelCol(rs.getString(COLUMN_NAME)) + ";\n\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb;
    }
    
    public static StringBuilder camelCol(String name) {
        StringBuilder builder = new StringBuilder();
        String[] words = name.split(SPLIT);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder;
    }
    
    public static StringBuilder camelMt(String name) {
        StringBuilder builder = new StringBuilder();
        String[] words = name.split(SPLIT);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder;
    }
    
    public static StringBuilder voDataMt(StringBuilder sb,ResultSet rs) {
        String line = "";
        try {
            while(rs.next()) {
                if("VARCHAR2".equals(rs.getString(DATA_TYPE))) {
                    line = "String";
                }else if("NUMBER".equals(rs.getString(DATA_TYPE))){
                    line = "int";
                }else if(DATE.equals(rs.getString(DATA_TYPE))) {
                    line = "Date";
                }
                sb.append("     public " + line + " " + "get" + camelMt(rs.getString(COLUMN_NAME)) + "() " + "{\n");
                sb.append("         return " + camelCol(rs.getString(COLUMN_NAME)) + ";\n");
                sb.append("     }\n\n");
                sb.append("     public void "+ "set" + camelMt(rs.getString(COLUMN_NAME)) + "(" + line + " " + camelCol(rs.getString(COLUMN_NAME))  + ") " + "{\n");
                sb.append("         this." + camelCol(rs.getString(COLUMN_NAME)) + " = " + camelCol(rs.getString(COLUMN_NAME)) + ";\n");
                sb.append("     }\n");
            }
            sb.append("\n}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb;
    }
}
