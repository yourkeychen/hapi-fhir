package ca.uhn.fhir.jpa.migrate.taskdef;

import ca.uhn.fhir.jpa.migrate.JdbcUtils;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ModifyColumnTask extends BaseTableColumnTypeTask<ModifyColumnTask> {

	private static final Logger ourLog = LoggerFactory.getLogger(ModifyColumnTask.class);


	@Override
	public void execute() {

		String existingType;
		try {
			existingType = JdbcUtils.getColumnType(getConnectionProperties(), getTableName(), getColumnName());
		} catch (SQLException e) {
			throw new InternalErrorException(e);
		}

		String wantedType = getColumnType().getDescriptor(getColumnLength());
		if (existingType.equals(wantedType)) {
			ourLog.info("Column {} on table {} is already of type {} - No action performed", getColumnName(), getTableName(), wantedType);
			return;
		}

		String type = getSqlType();
		String notNull = getSqlNotNull();

		String sql;
		String sqlNotNull = null;
		switch (getDriverType()) {
			case DERBY_EMBEDDED:
				sql = "alter table " + getTableName() + " alter column " + getColumnName() + " set data type " + type;
				break;
			case MARIADB_10_1:
			case MYSQL_5_7:
				sql = "alter table " + getTableName() + " modify column " + getColumnName() + " " + type + notNull;
				break;
			case POSTGRES_9_4:
				sql = "alter table " + getTableName() + " alter column " + getColumnName() + " type " + type;
				if (isNullable() == false) {
					sqlNotNull = "alter table " + getTableName() + " alter column " + getColumnName() + " set not null";
				}
				break;
			case ORACLE_12C:
				sql = "alter table " + getTableName() + " modify " + getColumnName() + " " + type + notNull;
				break;
			case MSSQL_2012:
				sql = "alter table " + getTableName() + " alter column " + getColumnName() + " " + type + notNull;
				break;
			default:
				throw new IllegalStateException("Dont know how to handle " + getDriverType());
		}

		ourLog.info("Updating column {} on table {} to type {}", getColumnName(), getTableName(), type);
		executeSql(sql);

		if (sqlNotNull != null) {
			ourLog.info("Updating column {} on table {} to not null", getColumnName(), getTableName());
			executeSql(sqlNotNull);
		}
	}

}
