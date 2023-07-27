package com.codegen.example;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;

public class codegen {
    
    private static String PATH_JAVABEAN = "com.codegen.example";
    
    private static String COLUMN_NAME = "COLUMN_NAME";
    
    @SuppressWarnings("deprecation")
    public static void toCodegen(String tableName) {
        System.out.println("開始產製 table名為=> " + tableName);
        Connection conn = getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
                // 取得使用者路徑
                String workSpace = System.getProperty("user.dir");
                // 檔案放置的位置 將\\替換成/變成階層
                String dest = workSpace + "/src/" + PATH_JAVABEAN.replaceAll("\\.", "/");
                pstmt = conn.prepareStatement("select b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME  from user_col_comments a, all_tab_columns b WHERE   A.Table_Name = B.Table_Name and A.Column_Name = B.Column_Name and A.Table_Name = " + " ? ");
                pstmt.setString(1, tableName);
                rs = pstmt.executeQuery();
                //建立儲存字串的物件
                StringBuilder sb = new StringBuilder();
                // 欄位名稱及屬性
                voData(sb,rs,tableName);
                // get、set方法
                ResultSet rs1 = pstmt.executeQuery();
                voDataMt(sb,rs1);
                
                //輸出
                FileUtils.write(new File(dest + "/" + camel(tableName) + ".java"), sb);
                System.out.println(tableName + " 產製成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(tableName + " 產製過程發生異常");
        }finally {
            if(conn != null && pstmt != null && rs != null) {
                try {
                    rs.close();
                    pstmt.close();
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
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
    
    public static StringBuilder voData(StringBuilder sb,ResultSet rs,String tableName) {
        String line = "";
        try {
            sb.append("package " + PATH_JAVABEAN + " ;\n\n");
            sb.append("public class " + camel(tableName) + " {\n\n");
            // private的 類別及屬性名稱
            while (rs.next()) {
                if("VARCHAR2".equals(rs.getString("DATA_TYPE"))) {
                    line = "String";
                }else if("NUMBER".equals(rs.getString("DATA_TYPE"))){
                    line = "int";
                }
                sb.append("    /*\n     *" + rs.getString("COMMENTS") + "\n     */\n");
                sb.append("     private " + line + " " + camelCol(rs.getString(COLUMN_NAME)) + ";\n\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb;
    }
    
    public static StringBuilder voDataMt(StringBuilder sb,ResultSet rs) {
        String line = "";
        try {
            while(rs.next()) {
                if("VARCHAR2".equals(rs.getString("DATA_TYPE"))) {
                    line = "String";
                }else if("NUMBER".equals(rs.getString("DATA_TYPE"))){
                    line = "int";
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
    
    public static StringBuilder camelCol(String name) {
        StringBuilder builder = new StringBuilder();
        String[] words = name.split("[\\W_]+");
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
        String[] words = name.split("[\\W_]+");
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
    
    public static void main(String[]args) {
        //放入table名稱
        toCodegen("CRONJOB_BANK_AUTH");
    }
}
