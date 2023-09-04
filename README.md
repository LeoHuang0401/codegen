# codegen
在main方法內的toCodegen()方法內放入table名稱，即可自動產出相對應的javabean(欄位名稱、欄位類型、欄位註解、get、set方法)

# 功能說明：
- 使用SQL找出欄位、欄位類型、欄位註解。
```
select b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME  from user_col_comments a, all_tab_columns b
```
- 利用camel的方法轉換成大駝峰、小駝峰命名方式。

- 使用StringBuilder，建立Class 及 屬性名稱 、 get,set方法。

- 最後使用FileUtils將.java檔案產到專案指定路徑。


# codegen.java程式說明：
- PATH_JAVABEAN : 建置entity時import的專案路徑。
****
- DEST_PATH_JAVABEAN_ENTITY : 產製entity時專案src資料夾下的路徑。
****
- DEST_PATH_JAVABEAN_REPOSITORY : 產製repository時專案src資料夾下的路徑。
