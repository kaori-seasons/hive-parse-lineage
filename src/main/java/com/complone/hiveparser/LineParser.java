package com.complone.hiveparser;

import com.complone.hiveparser.common.Operation;
import com.complone.hiveparser.dao.MetaDataQuery;
import com.complone.hiveparser.entity.ColLine;
import com.complone.hiveparser.entity.ColLineParse;
import com.complone.hiveparser.exeption.SQLParseException;
import com.complone.hiveparser.service.MetaDataQueryImpl;
import com.zaxxer.hikari.HikariDataSource;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.ParseDriver;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseResult;
import org.apache.hadoop.hive.ql.parse.SemanticException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static com.complone.hiveparser.common.Constants.*;
import static org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer.unescapeIdentifier;

public class LineParser {

    private MetaDataQuery dao = new MetaDataQueryImpl();
    
    private Map<String, List<ColLineParse>> subQueryMap;
    private Map<String/*table*/, List<String/*column*/>> dbMap;
    private List<ColLine> colLineList;
    
    private Operation operation;
    private boolean joinClause;
    private ASTNode joinOn = null;
    private String nowQueryDB;
    private int unionTimes;
    
    private Map<String, String> alias;
    private Set<String> conditions = new HashSet<String>();
    private List<ColLineParse> cols = new ArrayList<ColLineParse>();
    private Set<String> outputTables = new HashSet<String>();
    private Set<String> inputTables = new HashSet<String>();
    
    private Stack<String> tableNameStack = new Stack<String>();
    private Stack<Operation> operStack = new Stack<Operation>();
    private Stack<Boolean> joinStack = new Stack<Boolean>();
    private Stack<ASTNode> joinOnStack = new Stack<ASTNode>();
    private String nowQueryTable;
    private boolean isStaticUnion;
    
    private Class<? extends DataSource> dataSource;
    private String databaseType;
    
    
    public LineParser(DataSource dataSource, String databaseType){
        operation = Operation.DEFAULT;
        subQueryMap = new HashMap<String,  List<ColLineParse>>();
        dbMap = new HashMap<String, List<String>>();
        colLineList = new ArrayList<ColLine>();
        joinClause = false;
        nowQueryDB = "default";
        nowQueryTable = "default";
        unionTimes = 0;
        alias = new HashMap<String, String>();
        isStaticUnion = true;
        dataSource = dataSource;
        databaseType = databaseType;
    }
    
    
    public void parse(String sqlAll, boolean validate) throws SQLException{
        int i = 0; //当前是第几个sql
        for (String sql : sqlAll.split("(?<!\\\\);")) {
            ParseDriver pd = new ParseDriver();
            ASTNode ast = null;
            try {
                ParseResult parseResult = pd.parse(sql);
                ast = parseResult.getTree();
                System.out.println(ast.toStringTree());
                parseAST(ast);
                endParse(++i);
            } catch (ParseException | SemanticException e) {
                e.printStackTrace();
                throw new SQLParseException(e.getMessage());
            }
        }
    }
    
    /**
     * 所有解析完毕之后的后期处理
     */
    private void endParse(int sqlIndex) throws SQLException{
        putSubQueryMap(sqlIndex, TOK_EOF);
        putDBMap();
        setColLineList();
    }
    
    private void putDBMap() throws SQLException {
        Set<String> outputTables = getOutputTables();
        for (String table : outputTables) {
            String sql = table.replaceAll("[\\s\\t\\n\\r]", "").trim().toUpperCase();
            String[] pdt = sql.split(".");
            List<String> list = dao.getColumnByDBAndTable(dataSource, databaseType, pdt[0], pdt[1]);
            dbMap.put(table, list);
        }
    }
    
    private void setColLineList() {
        Map<String, List<ColLineParse>> map = new HashMap<String, List<ColLineParse>>();
        for (Map.Entry<String, List<ColLineParse>> entry : subQueryMap.entrySet()) {
            if (entry.getKey().startsWith(TOK_EOF)) {
                List<ColLineParse> value = entry.getValue();
                for (ColLineParse colLineParse : value) {
                    List<ColLineParse> list = map.get(colLineParse.getToTable());
                    if (CollectionUtils.isEmpty(list)) {
                        list = new ArrayList<ColLineParse>();
                        map.put(colLineParse.getToTable(), list);
                    }
                    list.add(colLineParse);
                }
            }
        }
        
        for (Map.Entry<String, List<ColLineParse>> entry : map.entrySet()) {
            String table = entry.getKey();
            List<ColLineParse> pList = entry.getValue();
            List<String> dList = dbMap.get(table);
            int metaSize = CollectionUtils.isEmpty(dList) ? 0 : dList.size();
            for (int i = 0; i < pList.size(); i++) { //按顺序插入对应的字段
                ColLineParse clp = pList.get(i);
                String colName = null;
                if (i < metaSize) {
                    colName = table + SPLIT_DOT + dList.get(i);
                }
                ColLine colLine = new ColLine(table, colName , clp.getToNameParse(),
                    clp.getFromName(), clp.getConditionSet());
                colLineList.add(colLine);
            }
        }
        
    }
    
    public Set<String> getOutputTables() {
        return outputTables;
    }
    public Set<String> getInputTables() {
        return inputTables;
    }
    
    private void parseAST(ASTNode ast) throws SemanticException,SQLException {
        parseIteral(ast);
    }
    
    private Set<String> parseIteral(ASTNode ast) throws SemanticException, SQLException {
        Set<String> set= new HashSet<String>();//当前查询所对应到的表集合
        prepareToParseCurrentNodeAndChilds(ast);
        set.addAll(parseChildNodes(ast));
        set.addAll(parseCurrentNode(ast, set));
        endParseCurrentNode(ast);
        return set;
    }
    
    /**
     * 解析所有子节点
     * @param ast
     * @return
     */
    private Set<String> parseChildNodes(ASTNode ast) throws SemanticException, SQLException {
        Set<String> set= new HashSet<String>();
        int numCh = ast.getChildCount();
        if (numCh > 0) {
            for (int num = 0; num < numCh; num++) {
                ASTNode child = (ASTNode) ast.getChild(num);
                set.addAll(parseIteral(child));
            }
        }
        return set;
    }
    
    private void putSubQueryMap(String tableAlias) {
        putSubQueryMap(0, tableAlias); //一个sql之间不会有别名相同的情况
    }
    
    /**
     * 保存subQuery查询别名和字段信息
     * @param sqlIndex
     * @param tableAlias
     */
    private void putSubQueryMap(int sqlIndex, String tableAlias) {
        List<ColLineParse> list = new ArrayList<ColLineParse>();
        if (TOK_EOF.equals(tableAlias) && unionTimes > 0) { //开头是union的处理
            int size = cols.size();
            int tableNum = unionTimes + 1; //1个union,2个表
            int colNum = size / tableNum;
            for (int i = 0; i < colNum; i++) { //合并字段
                ColLineParse col = cols.get(i);
                for (int j = i + colNum; j < size; j = j + colNum) {
                    ColLineParse col2 = cols.get(j);
                    if (notNormalCol(col.getToNameParse()) && !notNormalCol(col2.getToNameParse())) {
                        col.setToNameParse(col2.getToNameParse());
                    }
                    col.addFromName(col2.getFromName());
                    Set<String> conditionSet = col2.getConditionSet();
                    conditionSet.addAll(conditions);
                    col.addConditionSet(conditionSet);
                }
                list.add(col);
            }
        } else {
            for (ColLineParse entry : cols) {
                Set<String> conditionSet = entry.getConditionSet();
                conditionSet.addAll(conditions);
                list.add(new ColLineParse(entry.getToTable(), entry.getToNameParse(), entry.getFromName(), conditionSet));
            }
        }
        String key = sqlIndex == 0 ? tableAlias : tableAlias + sqlIndex; //没有重名的情况就不用标记
        subQueryMap.put(key, list);
    }
    
    
    
    /**
     * 结束解析当前节点
     * @param ast
     */
    private void endParseCurrentNode(ASTNode ast){
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) { //join 从句结束，跳出join
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                case HiveParser.TOK_LEFTSEMIJOIN:
                case HiveParser.TOK_FULLOUTERJOIN:
                case HiveParser.TOK_UNIQUEJOIN:
                    joinClause = joinStack.pop();
                    joinOn = joinOnStack.pop();
                    break;
                case HiveParser.TOK_QUERY:
                    break;
                case HiveParser.TOK_INSERT:
                case HiveParser.TOK_SELECT:
                    nowQueryTable = tableNameStack.pop();
                    operation = operStack.pop();
                    break;
            }
        }
    }
    
    /**
     * 准备解析当前节点
     * @param ast
     */
    private void prepareToParseCurrentNodeAndChilds(ASTNode ast){
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_SWITCHDATABASE:
                    System.out.println("nowQueryDB changed " + nowQueryDB+ " to " +ast.getChild(0).getText());
                    nowQueryDB = ast.getChild(0).getText();
                    break;
                case HiveParser.TOK_UNIONTYPE: //join 从句开始
                    if (isStaticUnion && (ast.getParent().isNil() || ast.getParent().getType() == HiveParser.TOK_UNIONTYPE)) {
                        unionTimes++;
                    } else if (ast.getParent().getType() != HiveParser.TOK_UNIONTYPE) {
                        isStaticUnion = false;
                    }
                    break;
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                case HiveParser.TOK_LEFTSEMIJOIN:
                    //已经归属子查询的atomicJoin语法
//                case HiveParser.TOK_MAPJOIN:
                case HiveParser.TOK_FULLOUTERJOIN:
                case HiveParser.TOK_UNIQUEJOIN:
                    joinStack.push(joinClause);
                    joinClause = true;
                    joinOnStack.push(joinOn);
                    joinOn = ast;
                    break;
                case HiveParser.TOK_QUERY:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(operation);
                    nowQueryTable = "";//sql22
                    operation = Operation.SELECT;
                    break;
                case HiveParser.TOK_INSERT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(operation);
                    operation = Operation.INSERT;
                    break;
                case HiveParser.TOK_SELECT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(operation);
                    operation = Operation.SELECT;
                    break;
                case HiveParser.TOK_DROPTABLE:
                    operation = Operation.DROP;
                    break;
                case HiveParser.TOK_TRUNCATETABLE:
                    operation = Operation.TRUNCATE;
                    break;
                case HiveParser.TOK_LOAD:
                    operation = Operation.LOAD;
                    break;
                case HiveParser.TOK_CREATETABLE:
                    operation = Operation.CREATETABLE;
                    break;
            }
            if (ast.getToken() != null
                && ast.getToken().getType() >= HiveParser.TOK_ALTERDATABASE_PROPERTIES
                && ast.getToken().getType() <= HiveParser.TOK_ALTERVIEW_RENAME) {
                operation = Operation.ALTER;
            }
        }
    }
        
        /**
         * 解析当前节点
         * @param ast
         * @param set
         * @return
         */
    private Set<String> parseCurrentNode(ASTNode ast,Set<String> set) throws SemanticException, SQLException {
        if (ast.getToken() != null){
            switch (ast.getToken().getType()){
                //解析创建表的操作
                case HiveParser.TOK_CREATETABLE:
                    outputTables.add(fillDB(BaseSemanticAnalyzer.getUnescapedName((ASTNode) ast.getChild(0))));
                case HiveParser.TOK_TAB:
                    String tableTab = BaseSemanticAnalyzer.getUnescapedName((ASTNode) ast.getChild(0));
                    outputTables.add(fillDB(tableTab));
                    //解析别名
                case HiveParser.TOK_TABREF:
                    ASTNode astTree = (ASTNode) ast.getChild(0);
                    StringBuilder tableTargetColumn = new StringBuilder();
                    String tableName = (astTree.getChildCount() == 1) ?
                        BaseSemanticAnalyzer.getUnescapedName((ASTNode) astTree.getChild(0)):
                        tableTargetColumn.append( BaseSemanticAnalyzer.getUnescapedName((ASTNode) astTree.getChild(0)) )
                            .append(SPLIT_DOT).append((ASTNode)astTree.getChild(1)).toString();
                    if (operation == Operation.SELECT){
                        //是否存在两表join的情况
                        if (joinClause && !"".equals(nowQueryDB)){
                            nowQueryDB += SPLIT_AND + tableName;
                        }else {
                            nowQueryDB = tableName;
                        }
                        set.add(tableName);
                    }
                    inputTables.add(fillDB(tableName));
                    if (ast.getChild(1) != null) { //(TOK_TABREF (TOK_TABNAME detail usersequence_client) c)
                        String alia = ast.getChild(1).getText().toLowerCase();
                        alias.put(alia, tableName);
                    }
                    break;
                case HiveParser.TOK_SUBQUERY:
                    if (ast.getChildCount() == 2) {
                        String tableAlias = unescapeIdentifier(ast.getChild(1).getText());
                        String aliaReal = "";
                        for(String table : set){
                            aliaReal+=table+SPLIT_AND;
                        }
                        if(aliaReal.length() !=0){
                            aliaReal = aliaReal.substring(0, aliaReal.length()-1);
                        }
                        alias.put(tableAlias, aliaReal);
                        putSubQueryMap(tableAlias);
                        cols.clear();
                    }
                    break;
                case HiveParser.TOK_SELEXPR: //输入输出字段的处理
                    /**
                     * (TOK_DESTINATION (TOK_DIR TOK_TMP_FILE))
                     *     (TOK_SELECT (TOK_SELEXPR TOK_ALLCOLREF))
                     *
                     * (TOK_DESTINATION (TOK_DIR TOK_TMP_FILE))
                     *     (TOK_SELECT
                     *         (TOK_SELEXPR (. (TOK_TABLE_OR_COL p) datekey) datekey)
                     *         (TOK_SELEXPR (TOK_TABLE_OR_COL datekey))
                     *         (TOK_SELEXPR (TOK_FUNCTIONDI count (. (TOK_TABLE_OR_COL base) userid)) buyer_count))
                     *         (TOK_SELEXPR (TOK_FUNCTION when (> (. (TOK_TABLE_OR_COL base) userid) 5) (. (TOK_TABLE_OR_COL base) clienttype) (> (. (TOK_TABLE_OR_COL base) userid) 1) (+ (. (TOK_TABLE_OR_COL base) datekey) 5) (+ (. (TOK_TABLE_OR_COL base) clienttype) 1)) bbbaaa)
                     */
                    //解析需要插入的表
                    Tree tok_insert = ast.getParent().getParent();
                    Tree child = tok_insert.getChild(0).getChild(0);
                    String tName = BaseSemanticAnalyzer.getUnescapedName((ASTNode) child.getChild(0));
                    String destTable = "TOK_TMP_FILE".equals(tName) ? "TOK_TMP_FILE" : fillDB(tName);
        
                    //select * from 的情况
                    if (ast.getChild(0).getType() == HiveParser.TOK_ALLCOLREF) {
                        String tableOrAlias = getColOrData((ASTNode) ast.getChild(0), true, false);
                        String nowTable =  fillDB(getRealTable(null, tableOrAlias));
                        String[] tableArr = nowTable.split(SPLIT_AND); //fact.test&test2
                        for (String tables : tableArr) {
                            String[] split = tables.split("\\.");
                            if (split.length > 2) {
                                throw new SQLParseException("parse table:" + nowTable);
                            }
                            String db = split.length == 2 ? split[0] : "" ;
                            String table = split.length == 2 ? split[1] : split[0] ;
                            List<String> colByTab = dao.getColumnByDBAndTable(dataSource, databaseType, db, table);
                            for (String column : colByTab) {
                                cols.add(new ColLineParse(destTable, column, tables + SPLIT_DOT + column, new HashSet<String>()));
                            }
                        }
                        break;
                    }
        
                    //select c1 from t的情况
                    String columnOrData = filterData(getColOrData((ASTNode)ast.getChild(0), false, false));
                    //2、过滤条件的处理select类
                    String condition = getCondition((ASTNode)ast.getChild(0));
                    Set<String> clone = filterCondition(columnOrData, condition);
                    String column = ast.getChild(1) != null ? parseColOrData((ASTNode)ast.getChild(1), false)
                        : parseColOrData((ASTNode)ast.getChild(0), true); //别名
                    cols.add(new ColLineParse(destTable, column, columnOrData, clone));
                    break;
                case HiveParser.TOK_WHERE: //3、过滤条件的处理select类
                    conditions.add("WHERE:" + getCondition((ASTNode) ast.getChild(0)));
                    break;
                case HiveParser.TOK_ALTERTABLE_ADDPARTS:
                case HiveParser.TOK_ALTERTABLE_RENAME:
                case HiveParser.TOK_ALTERTABLE_ADDCOLS:
                    ASTNode alterTableName = (ASTNode) ast.getChild(0);
                    outputTables.add(alterTableName.getText() + "\t" + operation);
                    break;
                default:
                    /**
                     * (or
                     *   (> (. (TOK_TABLE_OR_COL p) orderid) (. (TOK_TABLE_OR_COL c) orderid))
                     *   (and (= (. (TOK_TABLE_OR_COL p) a) (. (TOK_TABLE_OR_COL c) b))
                     *        (= (. (TOK_TABLE_OR_COL p) aaa) (. (TOK_TABLE_OR_COL c) bbb))))
                     */
                    //1、过滤条件的处理join类
                    if (joinOn != null && joinOn.getTokenStartIndex() == ast.getTokenStartIndex()
                        && joinOn.getTokenStopIndex() == ast.getTokenStopIndex()) {
                        ASTNode astCon = (ASTNode)ast.getChild(2);
                        conditions.add(ast.getText().substring(4) + ":" + getCondition(astCon));
                        break;
                    }
            }
        }
        return set;
    }
    
    /**
     * 解析获得列名或者字符数字等
     * @param ast
     * @param isSimple
     * @return
     */
    private String parseColOrData(ASTNode ast, boolean isSimple) {
        if (ast.getType() == HiveParser.DOT
            && ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL
            && ast.getChild(0).getChildCount() == 1
            && ast.getChild(1).getType() == HiveParser.Identifier) {
            String column = BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(1)
                .getText().toLowerCase());
            String alia = BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(0)
                .getChild(0).getText().toLowerCase());
            String realTable = getRealTable(column, alia);
            return isSimple ? column : fillDB(realTable) + SPLIT_DOT + column;
        } else if (ast.getType() == HiveParser.TOK_TABLE_OR_COL
            && ast.getChildCount() == 1
            && ast.getChild(0).getType() == HiveParser.Identifier) {
            String column = ast.getChild(0).getText();
            return isSimple ? column : fillDB(getRealTable(column, null)) + SPLIT_DOT + column;
        } else if (ast.getType() == HiveParser.Number
            || ast.getType() == HiveParser.StringLiteral
            || ast.getType() == HiveParser.Identifier) {
            return ast.getText();
        }
        return null;
    }
    
    
    
    /**
     * 过滤无意义条件（空、与字段相等），应用在select端
     * 如：select col1,col2+1 from table1, 此处col1的条件需要过滤
     * @param columnOrData
     * @param condition
     * @return
     */
    private Set<String> filterCondition(String columnOrData, String condition) {
        Set<String> clone = new HashSet<String>();
        if (StringUtils.isNotBlank(condition) //条件为空
            && !columnOrData.equals(condition)) { //条件和字段相等认为没有条件
            clone.add("COLFUN:" + condition);
        }
        return clone;
    }
    
    /**
     * 过滤掉无用的列：如col1,123,'2013',col2 ==>> col1,col2
     * @param col
     * @return
     */
    private String filterData(String col){
        String[] split = col.split(SPLIT_COMMA);
        StringBuilder sb = new StringBuilder();
        for (String string : split) {
            if (!notNormalCol(string)) {
                sb.append(string).append(SPLIT_COMMA);
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
    
    /**
     * 获得subquery查询中的table名称
     * @param column 对应的列
     * @param alia 别名
     * @param defaultTable 默认表名
     * @return
     */
    private String getSubQueryTable(String column, String alia,String defaultTable) {
        List<ColLineParse> list = subQueryMap.get(alia);
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(column) && CollectionUtils.isNotEmpty(list)) {
            for (ColLineParse colLine : list) {
                if (column.equals(colLine.getToNameParse())) {
                    String fromName = colLine.getFromName(); //处理一个字段对应多个字段的情况，如union
                    sb.append(fromName.substring(0, fromName.lastIndexOf(SPLIT_DOT))).append(SPLIT_AND);
                }
            }
            if (sb.length()>0) {
                sb.setLength(sb.length()-1);
            }
        }
        return sb.length() > 0 ? sb.toString() : defaultTable;
    }
    
    
    
    
    
    /**
     * 获得真实的表
     * @param column
     * @param alia
     * @return
     */
    private String getRealTable(String column, String alia) {
        String realTable = nowQueryTable;
        if (inputTables.contains(alia)) {
            realTable = alia;
        } else if (alias.get(alia) != null) {
            realTable = alias.get(alia);
        }
        if (StringUtils.isEmpty(alia)) {
            alia = fixAlia(realTable);
        }
        if (realTable.indexOf(SPLIT_AND) > 0) {
            realTable = getSubQueryTable(column, alia ,realTable);
        } else if (StringUtils.isEmpty(realTable)) {
            throw new SQLParseException("can't parse realTable column:" + column + ",alias:"+alia);
        }
        return realTable;
    }
    
    /**
     * 修正别名
     * @param realTable
     * @return
     */
    private String fixAlia(String realTable) {
        for (Map.Entry<String, String> entry : alias.entrySet()) {
            if (entry.getValue().equals(realTable)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    
    
    /**
     * 取得处理条件，主要应用在WHERE、JOIN和SELECT端
     * 如： <p>where a=1
     * <p>t1 join t2 on t1.col1=t2.col1 and t1.col2=123
     * <p>select count(distinct col1) from t1
     * @param ast
     * @return
     */
    private String getCondition(ASTNode ast) {
        if (ast.getType() == HiveParser.KW_OR
            ||ast.getType() == HiveParser.KW_AND) {
            return  "(" + getCondition((ASTNode)ast.getChild(0))
                + " " + ast.getText()
                + " " + getCondition((ASTNode)ast.getChild(1)) + ")";
        } else if (ast.getType() == HiveParser.NOTEQUAL //判断条件  > < like in
            || ast.getType() == HiveParser.EQUAL
            || ast.getType() == HiveParser.LESSTHAN
            || ast.getType() == HiveParser.LESSTHANOREQUALTO
            || ast.getType() == HiveParser.GREATERTHAN
            || ast.getType() == HiveParser.GREATERTHANOREQUALTO
            || ast.getType() == HiveParser.KW_LIKE
            || ast.getType() == HiveParser.DIVIDE
            || ast.getType() == HiveParser.PLUS
            || ast.getType() == HiveParser.MINUS
            || ast.getType() == HiveParser.STAR
            || ast.getType() == HiveParser.MOD
            || ast.getType() == HiveParser.AMPERSAND
            || ast.getType() == HiveParser.TILDE
            || ast.getType() == HiveParser.BITWISEOR
            || ast.getType() == HiveParser.BITWISEXOR) {
            return getColOrData((ASTNode)ast.getChild(0), false, true)
                + " " + ast.getText() + " "
                + getColOrData((ASTNode)ast.getChild(1), false, true);
        } else if (ast.getType() == HiveParser.TOK_FUNCTIONDI) {
            String condition = ast.getChild(0).getText();
            return condition + "(distinct (" + getCondition((ASTNode) ast.getChild(1)) +"))";
        } else {
            return getColOrData(ast, false, true);
        }
    }
    
    /**
     * 解析when条件
     * @param ast
     * @return case when c1>100 then col1 when c1>0 col2 else col3 end
     */
    private String getWhenCondition(ASTNode ast) {
        int cnt = ast.getChildCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < cnt; i++) {
            String condition = getCondition((ASTNode)ast.getChild(i));
            if (i == 1) {
                sb.append("case when " + condition);
            } else if (i == cnt-1) { //else
                sb.append(" else " + condition + " end");
            } else if (i % 2 == 0){ //then
                sb.append(" then " + condition);
            } else {
                sb.append(" when " + condition);
            }
        }
        return sb.toString();
    }
    
    
    
    /***
     * 递归解析获得列名或者字符数字等
     * @param ast
     * @param isSimple 是否是简写， 如isSimple=true:col1;isSimple=false:db1.table1.col1
     * @param withCond 是否包含条件，如true:nvl(col1,0)=>nvl(col1,0);false:col1
     * @return 解析得到的列名或者字符数字等
     */
    private String getColOrData(ASTNode ast,boolean isSimple, boolean withCond) {
        if(ast.getType() == HiveParser.TOK_FUNCTIONDI
            || ast.getType() == HiveParser.TOK_FUNCTION){
            String fun = ast.getChild(0).getText();
            String column = getColOrData((ASTNode) ast.getChild(1), isSimple, withCond);
            if ("when".equalsIgnoreCase(fun)) {
                return withCond ? getWhenCondition(ast) : getWhenColumn(ast, isSimple);
            } else if("IN".equalsIgnoreCase(fun)) {
                String col = getColOrData((ASTNode)ast.getChild(1), false, false);
                return col + " in (" + processChilds(ast, 2, true, false) + ")";
            } else if("TOK_ISNOTNULL".equalsIgnoreCase(fun) //isnull isnotnull
                || "TOK_ISNULL".equalsIgnoreCase(fun)){
                String col = getColOrData((ASTNode)ast.getChild(1), false, false);
                return col + " " + fun.toLowerCase().substring(4);
            } else if("CONCAT".equalsIgnoreCase(fun) //concat
                || "NVL".equalsIgnoreCase(fun) //NVl
                || "date_sub".equalsIgnoreCase(fun)){
                column = processChilds(ast, 1, isSimple, withCond);
            }
            return !withCond ? column : fun +"("+ column + ")";
        } else if(ast.getType() == HiveParser.LSQUARE){ //map,array
            String column = getColOrData((ASTNode) ast.getChild(0), isSimple, withCond);
            String key = getColOrData((ASTNode) ast.getChild(1), isSimple, withCond);
            return !withCond ?  column : column +"["+ key + "]";
        } else {
            String column = parseColOrData(ast, isSimple);
            if(StringUtils.isNotBlank(column)){
                return column;
            }
            return processChilds(ast, 0, isSimple, withCond);
        }
    }
    
    /**
     * 从指定索引位置开始解析子树
     * @param ast
     * @param startIndex 开始索引
     * @param isSimple 是否简写
     * @param withCond 是否包含条件
     * @return
     */
    private String processChilds(ASTNode ast,int startIndex, boolean isSimple,
                                 boolean withCond) {
        StringBuilder sb = new StringBuilder();
        int cnt = ast.getChildCount();
        for (int i = startIndex; i < cnt; i++) {
            String columnOrData = getColOrData((ASTNode) ast.getChild(i), isSimple, withCond);
            if (StringUtils.isNotBlank(columnOrData)){
                sb.append(columnOrData).append(SPLIT_COMMA);
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
    
    /***
     *  解析when的字段信息 case when c1>100 then col1 when c1>0 col2 else col3 end
     * @param ast
     * @param isSimple 是否是简写
     * @return col1,col2,col3
     */
    private String getWhenColumn(ASTNode ast, boolean isSimple) {
        int cnt = ast.getChildCount();
        Set<String> re = new HashSet<String>();
        for (int i = 2; i < cnt; i=i+2) {
            re.add(getColOrData((ASTNode) ast.getChild(i), isSimple, false));
            if (i+1 == cnt-1) { //else
                re.add(getColOrData((ASTNode) ast.getChild(i+1), isSimple, false));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String string : re) {
            sb.append(string).append(SPLIT_COMMA);
        }
        sb.setLength(sb.length()-1);
        return sb.toString();
    }
    
    
    
    
    
    
    /**
     * 判断正常列，
     * 正常：a as col, a
     * 异常：1 ，'a' //数字、字符等作为列名
     */
    private boolean notNormalCol(String column) {
        return StringUtils.isEmpty(column) || NumberUtils.isNumber(column)
            || column.startsWith("\"") || column.startsWith("\'");
    }
    
    /**
     * 补全db信息
     * table1 ==>> db1.table1
     * db1.table1 ==>> db1.table1
     * db2.t1&t2 ==>> db2.t1&db1.t2
     * @param nowTable
     */
    private String fillDB(String nowTable) {
        String nowQueryDB = "default";
        StringBuilder sb = new StringBuilder();
        String[] tableArr = nowTable.split(SPLIT_AND); //fact.test&test2&test3
        for (String tables : tableArr) {
            String[] split = tables.split("\\" + SPLIT_DOT);
            if (split.length > 2) {
                System.out.println(tables);
                throw new SQLParseException("parse table:" + nowTable);
            }
            String db = split.length == 2 ? split[0] : nowQueryDB ;
            String table = split.length == 2 ? split[1] : split[0] ;
            sb.append(db).append(SPLIT_DOT).append(table).append(SPLIT_AND);
        }
        if (sb.length()>0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
    
    public List<ColLine> getColLines() {
        return colLineList;
    }
    
    public static void main(String[] args) throws SQLException {
        DataSource dataSource = new HikariDataSource();
        String databaseType = "MySQL";
        LineParser lineParser = new LineParser(dataSource, databaseType);
        lineParser.parse("INSERT OVERWRITE TABLE dest1 partition (ds = '111')  "
            + "SELECT s.* FROM srcpart TABLESAMPLE (BUCKET 1 OUT OF 1) s "
            + "WHERE s.ds='2008-04-08' and s.hr='11'",true);
        System.out.println(lineParser.getColLines());
    }
    
}
