# codegen
在main方法內的toCodegen()方法內放入table名稱，即可自動產出相對應的javabean(欄位名稱、欄位類型、欄位註解、get、set方法)

功能說明：
利用 select b.DATA_TYPE , a.COMMENTS ,b.COLUMN_NAME  from user_col_comments a, all_tab_columns b 的SQL找出欄位、欄位類型、欄位註解。
再利用camel的方法轉換成大駝峰、小駝峰命名方式建立Class 及 屬性名稱 、 get,set方法。
