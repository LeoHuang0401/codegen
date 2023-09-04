package com.codegen.example.codegen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.codegen.example.utils.CodegenUtil;

public class codegen {
    
    private static String ENTITY = "Entity";
    
    private static String PATH_JAVABEAN = "com.codegen.example.entity";
    
    private static String PATH_REPOSITORY_JAVABEAN = "com.codegen.example.repository";
    /**
     * entity產檔路徑
     */
    private static String DEST_PATH_JAVABEAN_ENTITY = "com.codegen.example.entity";
    /**
     * repository產檔路徑
     */
    private static String DEST_PATH_JAVABEAN_REPOSITORY = "com.codegen.example.repository";
    
    private static String DATA_TYPE = "DATA_TYPE";
    
    private static String VARCHAR2 = "VARCHAR2";
    
    private static String DATE = "DATE";
     
    private static String NUMBER = "NUMBER";
    
    public static void main(String[]args) {
        //放入table名稱
        toCodegen("STORE_AUTH");
    }
    
    public static void toCodegen(String tableName) {
        System.out.println("開始產製 table名為=> " + tableName);
        entityCodegen(tableName);
        System.out.println(tableName + " 產製成功");
    }
    
    public static void entityCodegen(String tableName) {
        Connection conn = CodegenUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean bl = false;
        try {
            //取得檔案路徑
            String destEntity = CodegenUtil.dest(DEST_PATH_JAVABEAN_ENTITY);
            pstmt = conn.prepareStatement("select b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME  from user_col_comments a, all_tab_columns b WHERE   A.Table_Name = B.Table_Name and A.Column_Name = B.Column_Name and A.Table_Name = " + " ? ");
            pstmt.setString(1, tableName);
            rs = pstmt.executeQuery();
            //建立儲存字串的物件
            StringBuilder sb = new StringBuilder();
            sb.append("package " + PATH_JAVABEAN + " ;\n\n");
            while (rs.next()) {
                if("DATE".equals(rs.getString(DATA_TYPE))) {
                    bl = true;
                }
            }
            if(bl) {
                sb.append("import java.util.Date;\n\n");
            }
            sb.append("import javax.persistence.Entity;\n\n");
            sb.append("@Entity\n");
            sb.append("public class " + CodegenUtil.camel(tableName) + ENTITY + " {\n\n");
            ResultSet rsRe = pstmt.executeQuery();
            // 欄位名稱及屬性
            CodegenUtil.voData(sb,rsRe);
            // get、set方法
            ResultSet rs1 = pstmt.executeQuery();
            CodegenUtil.voDataMt(sb,rs1);
            //輸出
           CodegenUtil.write(destEntity, CodegenUtil.camel(tableName)+ENTITY, sb);
           repositoryCodegen(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn != null && pstmt != null && rs != null) {
                CodegenUtil.close(conn,pstmt,rs);
            }
        }
    }
    
    public static void repositoryCodegen(String tableName) {
        String destRepository = CodegenUtil.dest(DEST_PATH_JAVABEAN_REPOSITORY);
        Connection conn = CodegenUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder repositorySb = new StringBuilder();
        String line = "";
        try {
            pstmt = conn.prepareStatement("select b.DATA_TYPE,a.CONSTRAINT_NAME  from user_cons_columns a ,all_tab_columns b WHERE A.Table_Name = B.Table_Name and A.Column_Name = B.Column_Name and A.Table_Name = ? and a.constraint_name = ?");
            pstmt.setString(1, tableName);
            pstmt.setString(2, tableName + "_PK");
            rs = pstmt.executeQuery();
            repositorySb.append("package " + PATH_REPOSITORY_JAVABEAN + ";\n\n");
            repositorySb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
            repositorySb.append("import " + PATH_JAVABEAN + "." + CodegenUtil.camel(tableName) + "Entity" + ";\n\n");
            while(rs.next()) {
                if(VARCHAR2.equals(rs.getString(DATA_TYPE))) {
                    line = "String";
                }else if(NUMBER.equals(rs.getString(DATA_TYPE))){
                    line = "Integer";
                }else if(DATE.equals(rs.getString(DATA_TYPE))) {
                    line = "Date";
                }
            }
            repositorySb.append("public interface " + CodegenUtil.camel(tableName) + "Repository extends JpaRepository<" + CodegenUtil.camel(tableName) + ENTITY + "," + line + ">{\n\n}");
            CodegenUtil.write(destRepository, CodegenUtil.camel(tableName)+"Repository", repositorySb);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn != null && pstmt != null && rs != null) {
                CodegenUtil.close(conn,pstmt,rs);
            }
        }
    }
    
}
