package ut;


import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;

public class TestAll {
    public static void main(String[] args) {
        String sql = "SELECT a.id, b.name FROM table_a a JOIN table_b b ON a.id = b.id";

        // 创建SQL解析器
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, "mysql");
        SQLStatement statement = parser.parseStatement();

        // 使用SchemaStatVisitor解析表级血缘
        SchemaStatVisitor visitor = new SchemaStatVisitor();
        statement.accept(visitor);

        // 输出表级血缘
        System.out.println("Tables:");
        for (TableStat.Condition condition : visitor.getConditions()) {
            System.out.println("Table: " + condition.getColumn().getTable());
        }

        // 输出列级血缘
        System.out.println("Columns:");
        for (TableStat.Column column : visitor.getColumns()) {
            System.out.println("Column: " + column.getName() + " -> Table: " + column.getTable());
        }
    }
}

