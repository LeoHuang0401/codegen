# codegen
在main方法內的toCodegen()方法內放入table名稱，即可自動產出相對應的javabean(欄位名稱、欄位類型、欄位註解、get、set方法)

# 功能說明：
- 使用SQL找出除了pkey的欄位、欄位類型、欄位註解。
```
SELECT b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME 
FROM user_col_comments a 
LEFT JOIN all_tab_columns b ON a.TABLE_NAME = b.TABLE_NAME AND a.COLUMN_NAME = b.COLUMN_NAME 
LEFT JOIN (SELECT * FROM user_cons_columns WHERE CONSTRAINT_NAME = tableName+_PK ) c ON a.TABLE_NAME = c.TABLE_NAME 
AND a.COLUMN_NAME = c.COLUMN_NAME WHERE A.Table_Name = tableName  AND c.CONSTRAINT_NAME IS NULL ;
```
- 使用SQL找出pkey的欄位、欄位類型、欄位註解。
```
select rownum,b.DATA_TYPE, a.COLUMN_NAME, c.COMMENTS from user_cons_columns a ,all_tab_columns b , user_col_comments c WHERE A.Table_Name = B.Table_Name AND A.Column_Name = B.Column_Name AND c.TABLE_NAME = b.TABLE_NAME  AND c.COLUMN_NAME = a.COLUMN_NAME 
AND A.Table_Name = tableName and a.constraint_name = tableName+_PK;
```
- 利用camel的方法轉換成大駝峰、小駝峰命名方式。

- 使用StringBuilder，建立Class 及 屬性名稱 、 get,set方法。

- 最後使用FileUtils將.java檔案產到專案指定路徑。


# codegen.java程式說明：
- DEST_PATH_JAVABEAN_ENTITY : 產製entity時專案src資料夾下的路徑。
****
- DEST_PATH_JAVABEAN_REPOSITORY : 產製repository時專案src資料夾下的路徑。
****
- PK_JAVABEAN : 產製embeddedId時專案src資料夾下的路徑。
