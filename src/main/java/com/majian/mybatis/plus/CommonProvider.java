package com.majian.mybatis.plus;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.majian.mybatis.plus.annotation.*;
import org.apache.ibatis.jdbc.SQL;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommonProvider {
    private static Map<Class<?>, TableInfo> tableInfoMap = new ConcurrentHashMap<>();

    public String genInsertSql(Object object) {
        Preconditions.checkNotNull(object);
        TableInfo tableInfo = tableInfoMap.computeIfAbsent(object.getClass(), TableInfo::new);
        SQL sql = new SQL().INSERT_INTO(getTableName(tableInfo, object));
        for (ColumnInfo column : tableInfo.columns) {
            if (column.isNotNull(object)) {
                sql.VALUES(column.columnName, String.format("#{%s}", column.fieldName));
            }
        }
        return sql.toString();
    }

    public String genUpdateSql(Object object) {
        Preconditions.checkNotNull(object);
        TableInfo tableInfo = tableInfoMap.computeIfAbsent(object.getClass(), TableInfo::new);
        if (!tableInfo.primaryKeyColumn.isNotNull(object)) {
            throw new RuntimeException("primary key can not null for object:"+object);
        }
        SQL sql = new SQL().UPDATE(getTableName(tableInfo, object));

        for (ColumnInfo column : tableInfo.columns) {
            if (column.isPrimaryKey) {
                continue;
            }
            if (column.isNotNull(object)) {
                if (column.isVersionKey) {
                    sql.SET(String.format("%s = %s + 1", column.columnName, column.columnName));
                }else {
                    sql.SET(String.format("%s = #{%s}", column.columnName, column.fieldName));
                }
            }
        }

        sql.WHERE(String.format("%s = #{%s}", tableInfo.primaryKeyColumn.columnName, tableInfo.primaryKeyColumn.fieldName));
        if (tableInfo.versionColumn != null && tableInfo.versionColumn.isNotNull(object)) {
            sql.WHERE(String.format("%s = #{%s}", tableInfo.versionColumn.columnName, tableInfo.versionColumn.fieldName));
        }
        return sql.toString();
    }

    private String getTableName(TableInfo tableInfo, Object object) {
        return tableInfo.tableName;
    }

    private static class ColumnInfo {
        String fieldName;
        Field field;
        String columnName;
        boolean isVersionKey;
        boolean isPrimaryKey;

        ColumnInfo(Field field) {
            fieldName = field.getName();
            Column column = field.getAnnotation(Column.class);
            columnName = column != null ? column.value() : camelToUnderscore(field.getName());
            isVersionKey = field.isAnnotationPresent(Version.class);
            isPrimaryKey = field.isAnnotationPresent(Id.class);
            field.setAccessible(true);
            this.field = field;
        }

        boolean isNotNull(Object obj) {
            try {
                return field.get(obj) != null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String camelToUnderscore(String fieldName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }

    private static class TableInfo {
        String tableName;
        ColumnInfo primaryKeyColumn;
        ColumnInfo versionColumn;
        List<ColumnInfo> columns = Lists.newArrayList();

        public TableInfo(Class<?> cls) {
            Table table = Preconditions.checkNotNull(cls.getAnnotation(Table.class),"@Table not used on class:" + cls.getName());
            tableName = table.value();
            boolean tableIsExist = !tableName.isEmpty();
            Preconditions.checkArgument(tableIsExist, "table name is needed for class:" + cls.getName());

            while (!Object.class.equals(cls)) {
                for (Field field : cls.getDeclaredFields()) {
                    if (field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || field.getAnnotation(Transient.class) != null) {
                        continue;
                    }

                    ColumnInfo columnInfo = new ColumnInfo(field);
                    columns.add(columnInfo);

                    if (columnInfo.isPrimaryKey) {
                        primaryKeyColumn = columnInfo;
                    }
                    if (columnInfo.isVersionKey) {
                        versionColumn = columnInfo;
                    }
                    cls = cls.getSuperclass();
                }
            }
        }
    }
}
