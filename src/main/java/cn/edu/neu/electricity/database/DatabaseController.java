package cn.edu.neu.electricity.database;

import jdk.internal.org.objectweb.asm.Type;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sql.DataSource;
import java.sql.*;

public class DatabaseController {
    private static Connection conn = null;

    private static PreparedStatement SELECT_ALL_ELECTRICITY_USERS;
    private static PreparedStatement SELECT_ALL_ELECTRICITY_DEVICES;
    private static PreparedStatement SELECT_MY_ELECTRICITY_USER;
    private static PreparedStatement SELECT_ALL_MY_ELECTRICITY_DEVICES;
    private static PreparedStatement SELECT_MY_ELECTRICITY_DEVICE;
    private static PreparedStatement SELECT_MY_DEVICE_ELECTRICITY_RECORD;
    private static PreparedStatement SELECT_ALL_MY_DEVICE_ELECTRICITY_RECORD;
    private static PreparedStatement SELECT_ALL_MY_PAYMENT_ORDER;
    private static PreparedStatement SELECT_ALL_MY_BANK_ACCOUNT;
    private static PreparedStatement SELECT_ALL_MY_BALANCE_LOG;
    private static PreparedStatement SELECT_ALL_EXCEPTIONS;


    private static CallableStatement GET_ALL_FEE_TO_PAY;
    private static CallableStatement REVERSAL_ORDER_PAYMENT;
    private static CallableStatement SUBMIT_ORDER_PAYMENT;
    private static CallableStatement SUBMIT_ELECTRICITY_RECORD;
    private static CallableStatement CHARGE_WITH_BALANCE;
    private static CallableStatement CALCULATE_LATE_FEE;

    private static CallableStatement TOTAL_CHECK;
    private static CallableStatement PAYMENT_CHECK;

    private static BasicDataSource ds = null;

    public static void init() throws SQLException {
        if(conn == null || conn.isClosed()){
            connect();
        }

        SELECT_ALL_ELECTRICITY_USERS = conn.prepareStatement("SELECT * FROM ELECTRICITY_USER");
        SELECT_ALL_ELECTRICITY_DEVICES = conn.prepareStatement("SELECT * FROM ELECTRICITY_DEVICE");
        // 1.USER_ID
        SELECT_MY_ELECTRICITY_USER = conn.prepareStatement("SELECT * FROM ELECTRICITY_USER WHERE USER_ID = ?");
        // 1.USER_ID
        SELECT_ALL_MY_ELECTRICITY_DEVICES = conn.prepareStatement("SELECT * FROM ELECTRICITY_DEVICE a FULL JOIN (SELECT ELECTRICITY_DEVICE.DEVICE_ID, SUM(FEE_TO_PAY - FEE_PAID) AS FEE FROM ELECTRICITY_DEVICE JOIN ELECTRICITY_RECORD ON ELECTRICITY_DEVICE.DEVICE_ID = ELECTRICITY_RECORD.DEVICE_ID WHERE USER_ID = ? GROUP BY ELECTRICITY_DEVICE.DEVICE_ID, DEVICE_ADDRESS, DEVICE_TYPE) b ON a.DEVICE_ID = b.DEVICE_ID WHERE USER_ID = ? ");

        // 1.DEVICE_ID
        SELECT_MY_DEVICE_ELECTRICITY_RECORD = conn.prepareStatement("SELECT * FROM ELECTRICITY_RECORD WHERE DEVICE_ID = ?");

        // 1.USER_ID
        SELECT_ALL_MY_DEVICE_ELECTRICITY_RECORD = conn.prepareStatement(
                "SELECT * FROM ELECTRICITY_RECORD WHERE DEVICE_ID IN (SELECT DEVICE_ID FROM ELECTRICITY_DEVICE WHERE USER_ID = ?)"
        );

        // 1.USER_ID
        SELECT_ALL_MY_PAYMENT_ORDER = conn.prepareStatement(
                "SELECT * FROM PAYMENT_ORDER WHERE BANK_ACCOUNT_ID IN (SELECT BANK_ACCOUNT_ID FROM BANK_ACCOUNT WHERE USER_ID = ?)"
        );

        // 1.USER_ID
        SELECT_ALL_MY_BANK_ACCOUNT = conn.prepareStatement(
                "SELECT BANK.BANK_ID as BANK_ID, BANK_NAME, BANK_ACCOUNT_ID FROM BANK_ACCOUNT,BANK WHERE BANK_ACCOUNT.BANK_ID = BANK.BANK_ID and USER_ID = ?"
        );

        // 1.USER_ID
        SELECT_ALL_MY_BALANCE_LOG = conn.prepareStatement("SELECT * FROM USER_BALANCE_LOG WHERE USER_ID = ? ORDER BY LOG_ID DESC");

        SELECT_ALL_EXCEPTIONS = conn.prepareStatement("SELECT * FROM ACCOUNT_ITEM_EXCEPTION order by EXCEPTION_ID DESC");

        GET_ALL_FEE_TO_PAY = conn.prepareCall("call GET_ALL_FEE_TO_PAY(?, ?)");

        // 1:ORDER_ID  2:PAY_AMOUNT  3:RESULT_VAR 4.balance returned
        REVERSAL_ORDER_PAYMENT = conn.prepareCall("call reversal_order_payment(?, ?, ?, ?)");

        // 1.PAY_AMOUNT 2.BANK_ACCOUNT_ID(VARCHAR2)  3.DEVICE_ID  4.RESULT_VAR
        SUBMIT_ORDER_PAYMENT = conn.prepareCall("call submit_order_payment(?, ?, ?, ?, ?, ?)");

        // 1.METER_NUMBER_NOW 2.DEVICE_ID 3.TIME
        SUBMIT_ELECTRICITY_RECORD = conn.prepareCall("call SUBMIT_ELECTRICITY_RECORD(?, ?, ?, ?, ?)");

        // 1.USER_ID 2.RESULT_VAR
        CHARGE_WITH_BALANCE = conn.prepareCall("call CHARGE_WITH_BALANCE(?, ?)");

        // 1.CUR_DATE
        CALCULATE_LATE_FEE = conn.prepareCall("call CALCULATE_LATE_FEE(?)");

        // 1.BANK 2.DATE 3.AMOUNT 4.CONT 5.RESULT_VAR
        TOTAL_CHECK=conn.prepareCall("call PAYMENT_TOTAL_CHECK(?, ?, ?, ?, ?)");

        // 1.DATE 2.RESULT_VAR
        PAYMENT_CHECK = conn.prepareCall("call PAYMENT_CHECK(?, ?)");
    }

    public static void connect(){
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");//找到oracle驱动器所在的类
            String url="jdbc:oracle:thin:@localhost:1521:XE"; //URL地址
            String username="USER"; // 修改为本地数据库用户名
            String password="password";  // 修改为本地数据库密码
            if(ds==null) {
                //创建连接池
                ds = new BasicDataSource();
                //设置参数
                ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
                ds.setUrl(url);
                ds.setUsername(username);
                ds.setPassword(password);
                ds.setInitialSize(2);
                ds.setMaxTotal(5);
            }
            conn = ds.getConnection();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 将resultSet转化为JSON对象
     * @param rs 输入结果集
     * @return 转换结果
     * @throws SQLException
     * @throws JSONException
     */
    private static JSONObject resultSetToJsonObject(ResultSet rs) throws SQLException,JSONException
    {
        JSONObject jsonObj = new JSONObject();
        ResultSetMetaData metaData = rs.getMetaData();// 获取列数
        int columnCount = metaData.getColumnCount();
        if (rs.next()) {// 遍历ResultSet中的每条数据
            for (int i = 1; i <= columnCount; i++) {// 遍历每一列
                String columnName =metaData.getColumnLabel(i);
                String value = rs.getString(columnName);
                jsonObj.put(columnName, value);
            }
        }
        System.out.println(jsonObj.toString());
        return jsonObj;
    }

    /**
     * 将resultSet转化为JSON数组
     * @param rs 输入结果集
     * @return 转换结果
     * @throws SQLException
     * @throws JSONException
     */
    private static JSONArray resultSetToJsonArray(ResultSet rs) throws SQLException,JSONException
    {
        // JSON 数组
        JSONArray array = new JSONArray();
        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 遍历ResultSet中的每条数据
        while (rs.next()) {
            JSONObject jsonObj = new JSONObject();
            // 遍历每一列
            for (int i = 1; i <= columnCount; i++) {
                String columnName =metaData.getColumnLabel(i);
                String value = rs.getString(columnName);
                jsonObj.put(columnName, value);
            }
            array.put(jsonObj);
        }
        System.out.println(array.toString());
        return array;
    }

    public static JSONArray getAllElectricityUsers() throws SQLException {
        ResultSet resultSet = SELECT_ALL_ELECTRICITY_USERS.executeQuery();
        return resultSetToJsonArray(resultSet);
    }

    public static JSONObject getMyUser(int userId) throws SQLException {
        SELECT_MY_ELECTRICITY_USER.setInt(1, userId);
        ResultSet resultSet = SELECT_MY_ELECTRICITY_USER.executeQuery();
        JSONObject toReturn = resultSetToJsonObject(resultSet);
        SELECT_MY_ELECTRICITY_USER.clearParameters();
        return toReturn;
    }

    public static JSONArray getMyDevices(int userId) throws SQLException {
        SELECT_ALL_MY_ELECTRICITY_DEVICES.setInt(1, userId);
        SELECT_ALL_MY_ELECTRICITY_DEVICES.setInt(2, userId);
        ResultSet resultSet = SELECT_ALL_MY_ELECTRICITY_DEVICES.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_ALL_MY_ELECTRICITY_DEVICES.clearParameters();
        return toReturn;
    }

    public static JSONArray getMyDeviceRecords(int deviceId) throws SQLException {
        SELECT_MY_DEVICE_ELECTRICITY_RECORD.setInt(1, deviceId);
        ResultSet resultSet = SELECT_MY_DEVICE_ELECTRICITY_RECORD.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_MY_DEVICE_ELECTRICITY_RECORD.clearParameters();
        return toReturn;
    }

    public static JSONArray getAllMyRecords(int userId) throws SQLException {
        SELECT_ALL_MY_DEVICE_ELECTRICITY_RECORD.setInt(1, userId);
        ResultSet resultSet = SELECT_ALL_MY_DEVICE_ELECTRICITY_RECORD.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_ALL_MY_DEVICE_ELECTRICITY_RECORD.clearParameters();
        return toReturn;
    }

    public static JSONArray getAllPaymentOrders(int userId) throws SQLException {
        SELECT_ALL_MY_PAYMENT_ORDER.setInt(1, userId);
        ResultSet resultSet = SELECT_ALL_MY_PAYMENT_ORDER.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_ALL_MY_PAYMENT_ORDER.clearParameters();
        return toReturn;
    }

    public static JSONArray getAllBankAccounts(int userId) throws SQLException {
        SELECT_ALL_MY_BANK_ACCOUNT.setInt(1, userId);
        ResultSet resultSet = SELECT_ALL_MY_BANK_ACCOUNT.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_ALL_MY_BANK_ACCOUNT.clearParameters();
        return toReturn;
    }

    public static JSONArray getAllBalanceLogs(int userId) throws SQLException {
        SELECT_ALL_MY_BALANCE_LOG.setInt(1, userId);
        ResultSet resultSet = SELECT_ALL_MY_BALANCE_LOG.executeQuery();
        JSONArray toReturn = resultSetToJsonArray(resultSet);
        SELECT_ALL_MY_BALANCE_LOG.clearParameters();
        return toReturn;
    }

    public static JSONArray getAllExceptions() throws SQLException {
        return resultSetToJsonArray(SELECT_ALL_EXCEPTIONS.executeQuery());
    }

    public static double getAllFeeToPay(int userId) throws SQLException {
        GET_ALL_FEE_TO_PAY.setInt(1, userId);
        GET_ALL_FEE_TO_PAY.registerOutParameter(2, Types.DOUBLE);
        GET_ALL_FEE_TO_PAY.execute();
        double feeToPay = GET_ALL_FEE_TO_PAY.getDouble(2);
        GET_ALL_FEE_TO_PAY.clearParameters();
        return feeToPay;
    }

    public static JSONObject submitElectricityRecord(int deviceId, double currentMeter, Date currentDate) throws SQLException {
        SUBMIT_ELECTRICITY_RECORD.setDouble(1, currentMeter);
        SUBMIT_ELECTRICITY_RECORD.setInt(2, deviceId);
        SUBMIT_ELECTRICITY_RECORD.setDate(3, currentDate);
        SUBMIT_ELECTRICITY_RECORD.registerOutParameter(4, Types.INTEGER);
        SUBMIT_ELECTRICITY_RECORD.registerOutParameter(5, Types.DOUBLE);
        SUBMIT_ELECTRICITY_RECORD.execute();

        JSONObject result = new JSONObject();
        result.put("RESULT", SUBMIT_ELECTRICITY_RECORD.getInt(4));
        result.put("FEE", SUBMIT_ELECTRICITY_RECORD.getDouble(5));
        SUBMIT_ELECTRICITY_RECORD.clearParameters();
        return result;
    }

    public static JSONObject reversalOrderPayment(int orderId, double amount) throws SQLException {
        REVERSAL_ORDER_PAYMENT.setInt(1, orderId);
        REVERSAL_ORDER_PAYMENT.setDouble(2, amount);
        REVERSAL_ORDER_PAYMENT.registerOutParameter(3, Types.INTEGER);
        REVERSAL_ORDER_PAYMENT.registerOutParameter(4, Types.DOUBLE);
        REVERSAL_ORDER_PAYMENT.execute();
        JSONObject result = new JSONObject();
        result.put("RESULT", REVERSAL_ORDER_PAYMENT.getInt(3));
        result.put("BALANCE", REVERSAL_ORDER_PAYMENT.getDouble(4));
        REVERSAL_ORDER_PAYMENT.clearParameters();
        return result;
    }

    // 1.PAY_AMOUNT 2.BANK_ACCOUNT_ID(VARCHAR2)  3.DEVICE_ID  4.RESULT_VAR
    public static JSONObject submitOrderPayment(int deviceId, double amount, String bankAccountId, String bankId, int userId) throws SQLException {
        System.out.println("DEVID="+deviceId+" AMOUNT=" + amount + " ACCID=" + bankAccountId + " BANKID=" + bankId + " userID=" + userId);
        SUBMIT_ORDER_PAYMENT.setDouble(1, amount);
        SUBMIT_ORDER_PAYMENT.setString(2, bankAccountId);
        SUBMIT_ORDER_PAYMENT.setString(3, bankId);
        SUBMIT_ORDER_PAYMENT.setInt(4, userId);
        SUBMIT_ORDER_PAYMENT.setInt(5, deviceId);
        SUBMIT_ORDER_PAYMENT.registerOutParameter(6, Types.INTEGER);
        SUBMIT_ORDER_PAYMENT.execute();
        JSONObject result = new JSONObject();
        result.put("RESULT", SUBMIT_ORDER_PAYMENT.getInt(6));
        SUBMIT_ORDER_PAYMENT.clearParameters();
        return result;
    }


    public static JSONObject chargeWithBalance(int userId) throws SQLException {
        CHARGE_WITH_BALANCE.setInt(1, userId);
        CHARGE_WITH_BALANCE.registerOutParameter(2, Types.INTEGER);
        CHARGE_WITH_BALANCE.execute();
        JSONObject result = new JSONObject();
        result.put("RESULT", CHARGE_WITH_BALANCE.getInt(2));
        CHARGE_WITH_BALANCE.clearParameters();
        return result;
    }

    public static void calculateLateFee(Date currentDate) throws SQLException {
        CALCULATE_LATE_FEE.setDate(1, currentDate);
        CALCULATE_LATE_FEE.execute();
        CALCULATE_LATE_FEE.clearParameters();
    }

    public static int checkDetail(Date date) throws SQLException {
        PAYMENT_CHECK.setDate(1, date);
        PAYMENT_CHECK.registerOutParameter(2, Types.INTEGER);
        PAYMENT_CHECK.execute();
        int res = PAYMENT_CHECK.getInt(2);
        PAYMENT_CHECK.clearParameters();
        return  res;
    }

    // 1.BANK 2.DATE 3.AMOUNT 4.CONT 5.RESULT_VAR
    public static int checkTotal(Date date, String bank, double amount, int count) throws SQLException {
        TOTAL_CHECK.setString(1, bank);
        TOTAL_CHECK.setDate(2, date);
        TOTAL_CHECK.setDouble(3, amount);
        TOTAL_CHECK.setInt(4, count);

        TOTAL_CHECK.registerOutParameter(5, Types.INTEGER);
        TOTAL_CHECK.execute();
        int res = TOTAL_CHECK.getInt(5);
        TOTAL_CHECK.clearParameters();
        return  res;
    }
}
