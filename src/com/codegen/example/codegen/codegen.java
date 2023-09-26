package com.codegen.example.codegen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.GapContent;

import com.codegen.example.utils.CodegenUtil;

public class codegen {
    
    
    /**
     * 複合式pkey產製路徑
     */
    private static String PK_JAVABEAN = "com.codegen.example.entity.embeddedId";
    /**
     * entity產檔路徑
     */
    private static String DEST_PATH_JAVABEAN_ENTITY = "com.codegen.example.entity";
    /**
     * repository產檔路徑
     */
    private static String DEST_PATH_JAVABEAN_REPOSITORY = "com.codegen.example.repository";
    
    private static String ENTITY = "Entity";
    
    private static String DATA_TYPE = "DATA_TYPE";
    
    private static String DATE = "DATE";
    
    public static void main(String[]args) {
        //放入table名稱
        toCodegen("STORE_AUTH");
    }
    
    public static void toCodegen(String tableName) {
        System.out.println("開始產製 Table名:=> " + tableName);
        entityCodegen(tableName);
        System.out.println("Table:" + tableName + " 產製成功");
    }
    
    /**
     * 產製entity 及 呼叫 產製repository
     * @param tableName
     */
    public static void entityCodegen(String tableName) {
        Connection conn = CodegenUtil.getConnection();
        ResultSet rs = null;
        ResultSet rsPk = null;
        //放置PreparedStatement所需之參數
        List<String> conditions = new ArrayList<>();
        String sql = "";
        try {
            //取得檔案路徑
            String destEntity = CodegenUtil.dest(DEST_PATH_JAVABEAN_ENTITY);
            String pk = tableName + "_PK";
            // 查詢除pkey外欄位、型別、註解
            sql = "SELECT b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME "
                + "FROM user_col_comments a "
                + "LEFT JOIN all_tab_columns b ON a.TABLE_NAME = b.TABLE_NAME AND a.COLUMN_NAME = b.COLUMN_NAME "
                + "LEFT JOIN (SELECT * FROM user_cons_columns WHERE CONSTRAINT_NAME = ?) c ON a.TABLE_NAME = c.TABLE_NAME AND a.COLUMN_NAME = c.COLUMN_NAME "
                + "WHERE a.Table_Name = ? AND c.CONSTRAINT_NAME IS NULL ";
            conditions.add(pk);
            conditions.add(tableName);
            rs = CodegenUtil.executePstmt(conn, sql, conditions, rs);
            conditions.clear();
            // 查詢pkey欄位、型別、註解
            sql = "select b.DATA_TYPE, a.COLUMN_NAME, c.COMMENTS "
                    + "from user_cons_columns a ,all_tab_columns b , user_col_comments c "
                    + "WHERE A.Table_Name = B.Table_Name AND A.Column_Name = B.Column_Name AND c.TABLE_NAME = b.TABLE_NAME  AND c.COLUMN_NAME = a.COLUMN_NAME "
                    + "AND A.Table_Name = ? and a.constraint_name = ?";
            conditions.add(tableName);
            conditions.add(pk);
            rsPk = CodegenUtil.executePstmt(conn, sql, conditions, rsPk);
            // 取得欄位數以判斷是否為多pkey
            rsPk.last();
            int pkCount = rsPk.getRow();
            List<String> pkList = null;
            String pkName = "";
            String idPk = "";
            // 判斷是否為複合式pkey
            if (pkCount > 1) {
                pkList = new ArrayList<>();
                rsPk.beforeFirst();
                // 建立 @EmbeddedId 複合式pkey
                pkName = createPkBean(rsPk, tableName);
                pkList.add(pkName);
            }else {
                pkList = new ArrayList<>();
                String type = CodegenUtil.changeDataType(rsPk.getString(DATA_TYPE));
                pkList.add(rsPk.getString("COLUMN_NAME"));
                pkList.add(rsPk.getString("COMMENTS"));
                pkList.add(type);
                idPk = rsPk.getString(DATA_TYPE);
            }
            if(pkList.isEmpty()) return;
            boolean isDate = false;
            boolean isLocalDateTime = false;
            //建立儲存字串的物件
            StringBuilder sb = new StringBuilder();
            sb.append("package " + DEST_PATH_JAVABEAN_ENTITY + " ;\n\n");
            while (rs.next()) {
                String type = CodegenUtil.changeDataType(rs.getString(DATA_TYPE));
                if (DATE.equals(type)) {
                    isDate = true;
                }else if ("TIMESTAMP".equals(type)) {
                    isLocalDateTime = true;
                }
            }
            // 是否需要import date || localdatetime
            if(isDate) sb.append("import java.util.Date;\n\n");
            if (isLocalDateTime) sb.append("import java.time.LocalDateTime;\n\n");
            // 複合式pkey需import來源class檔
            if (!pkName.isEmpty()) {
                sb.append("import com.codegen.example.entity.embeddedId." + pkName + ";\n");
                sb.append("import javax.persistence.EmbeddedId;\n");
            }
            sb.append("import javax.persistence.Entity;\n\n");
            sb.append("@Entity\n");
            sb.append("public class " + CodegenUtil.camel(tableName) + ENTITY + " {\n\n");
            // 欄位名稱及屬性
            rs.beforeFirst();
            CodegenUtil.voData(sb, rs, pkList, pkName);
            // get、set方法
            rs.beforeFirst();
           CodegenUtil.voDataMt(sb,rs, pkName);
           //輸出
           CodegenUtil.write(destEntity, CodegenUtil.camel(tableName)+ENTITY, sb);
           //產製repository
           repositoryCodegen(tableName, pkName, idPk);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn != null && rs != null && rsPk != null) {
                CodegenUtil.close(conn, null,rs, rsPk);
            }
        }
    }
    
    /**
     * 產製複合式pkey之class檔
     * @param rsPk 查詢pkey之resultSet物件
     * @param tableName 資料表名稱
     * @return
     */
    public static String createPkBean(ResultSet rsPk, String tableName) {
        StringBuilder sb = new StringBuilder();
        boolean isDate = false;
        boolean isLocalDateTime = false;
        sb.append("package " + PK_JAVABEAN + ";\n\n");
        try {
            while (rsPk.next()) {
                if("DATE".equals(rsPk.getString(DATA_TYPE))) {
                    isDate = true;
                }else if("TIMESTAMP".equals(rsPk.getString(DATA_TYPE))) {
                    isLocalDateTime = true;
                }
            }
            if(isDate) {
                sb.append("import java.util.Date;\n\n");
            }else if(isLocalDateTime) {
                sb.append("import java.time.LocalDateTime;\n\n");
            }
            sb.append("import java.io.Serializable;\n\n");
            sb.append("public class " + CodegenUtil.camel(tableName)+"Pkey" + " implements Serializable{\n\n");
            rsPk.beforeFirst();
            CodegenUtil.voData(sb, rsPk, null, null);
            // get、set方法
            rsPk.beforeFirst();
            CodegenUtil.voDataMt(sb,rsPk, null);
            CodegenUtil.write(CodegenUtil.dest(PK_JAVABEAN), CodegenUtil.camel(tableName)+"Pkey", sb);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CodegenUtil.camel(tableName)+"Pkey";
    }
    
    /**
     * 產製 repository
     * @param tableName 資料表名稱
     * @param pkName 複合式pkey
     * @param idPk 單pkey型別
     */
    public static void repositoryCodegen(String tableName, String pkName, String idPk) {
        //repository 產製路徑
        String destRepository = CodegenUtil.dest(DEST_PATH_JAVABEAN_REPOSITORY);
        Connection conn = CodegenUtil.getConnection();
        StringBuilder repositorySb = new StringBuilder();
        String type = "";
        try {
            repositorySb.append("package com.codegen.example.repository;\n\n");
            if (!pkName.isEmpty() && idPk.isEmpty()) {
                type = pkName;
                repositorySb.append("import com.codegen.example.entity.embeddedId." + pkName + ";\n");
            }else {
                type = CodegenUtil.changeDataType(idPk);
            }
            repositorySb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
            repositorySb.append("import com.codegen.example.entity." + CodegenUtil.camel(tableName) + "Entity" + ";\n\n");
            repositorySb.append("public interface " + CodegenUtil.camel(tableName) + "Repository extends JpaRepository<" + CodegenUtil.camel(tableName) + ENTITY + "," + type + ">{\n\n}");
            CodegenUtil.write(destRepository, CodegenUtil.camel(tableName)+"Repository", repositorySb);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn != null) {
                CodegenUtil.close(conn, null, null, null);
            }
        }
    }
    
}
