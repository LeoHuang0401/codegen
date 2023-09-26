package com.codegen.example.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
                conn = DriverManager.getConnection("", "", "");
                pools.set(conn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    public static void close(Connection conn , PreparedStatement pstmt, ResultSet rs, ResultSet rsPk) {
        try {
            if (rsPk != null) {
                rsPk.close();
            }
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Class檔名大寫駝峰
     * @param name
     * @return
     */
    public static StringBuilder camel(String name) {
        StringBuilder builder = new StringBuilder();
        String[] words = name.split(SPLIT);
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
    
    /**
     * 屬性欄位
     * @param sb
     * @param rs
     * @param pkList
     * @param isPk
     * @param pkName
     * @return
     */
    public static StringBuilder voData(StringBuilder sb,ResultSet rs, List<String> pkList, String pkName) {
        try {
            // private的 類別及屬性名稱
            while (rs.next()) {
                String type = changeDataType(rs.getString(DATA_TYPE));
                if (pkList != null && pkList.size() == 3) {
                    sb.append("    /*\n     *" + pkList.get(1) + "\n     */\n");
                    sb.append("     @Id\n");
                    sb.append("     private " + pkList.get(2) + " " + camelCol(pkList.get(0)) + ";\n\n");
                    pkList.clear();
                }else if (pkList != null && pkName != null && pkList.contains(pkName)) {
                    sb.append("    /*\n     *" + pkName + "\n     */\n");
                    sb.append("     @EmbeddedId\n");
                    sb.append("     private " + pkName + " " + camelCol(pkName) + ";\n\n");
                    pkList.remove(pkName);
                }
                sb.append("    /*\n     *" + rs.getString("COMMENTS") + "\n     */\n");
                sb.append("     private " + type + " " + camelCol(rs.getString(COLUMN_NAME)) + ";\n\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb;
    }
    
    public static String changeDataType(String dataType) {
        String type = "";
        if(VARCHAR2.equals(dataType)) {
            type = "String";
        }else if(NUMBER.equals(dataType)){
            type = "Integer";
        }else if(DATE.equals(dataType)) {
            type = "Date";
        }else if("TIMESTAMP".equals(dataType)) {
            type = "LocalDateTime";
        }
        return type;
    }
    
    /**
     * get、set方法
     * @param sb
     * @param rs
     * @return
     */
    public static StringBuilder voDataMt(StringBuilder sb,ResultSet rs, String pkName) {
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
                if (pkName != null && !pkName.isEmpty()) {
                    sb.append("     public " + pkName + " " + "get" + camelMt(pkName) + "() " + "{\n");
                    sb.append("         return " + camelCol(pkName) + ";\n");
                    sb.append("     }\n\n");
                    sb.append("     public void "+ "set" + camelMt(pkName) + "(" + pkName + " " + camelCol(pkName)  + ") " + "{\n");
                    sb.append("         this." + camelCol(pkName) + " = " + camelCol(pkName) + ";\n");
                    sb.append("     }\n");
                    pkName = null;
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
    
    /**
     * 執行 executeQuery
     * @param conn 連線
     * @param sql 需執行之sql
     * @param conditions 參數
     * @param rs ResultSet
     * @return
     */
    public static ResultSet executePstmt(Connection conn ,String sql,List<String> conditions, ResultSet rs) {
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            if (conditions != null && !conditions.isEmpty()) {
                for (int i = 0; i < conditions.size(); i++) {
                    pstmt.setString(i+1, conditions.get(i));
                }
            }
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }
    
    /**
     * 屬性駝峰
     * @param name
     * @return
     */
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
    
    /**
     * getter、setter方法駝峰
     * @param name
     * @return
     */
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
    
}
