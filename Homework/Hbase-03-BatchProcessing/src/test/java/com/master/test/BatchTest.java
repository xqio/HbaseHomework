package com.master.test;


import com.master.util.HbaseFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BatchTest {
    private HbaseFactory hbaseFactory;
    private String[][] data;

    @Before
    public void init() throws IOException {
        hbaseFactory = new HbaseFactory();
        data = getData();
    }

    /**
     * 批量添加数据
     *
     * @throws IOException 异常
     */
    @Test
    public void batchAdd() throws IOException {
        //建表
        TableDescriptorBuilder order_info = TableDescriptorBuilder.newBuilder(TableName.valueOf(Bytes.toBytes("ORDER_INFO")));
        ColumnFamilyDescriptor c1 = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("C1")).build();
        order_info.setColumnFamily(c1);
        hbaseFactory.getAdmin().createTable(order_info.build());

        //增加数据
        List<Put> puts = new ArrayList<Put>();
        String[] columns = {"status", "money", "pay_way", "user_id", "operation_time", "category"};
        for (String[] datum : data) {
            Put put = new Put(Bytes.toBytes(datum[0]));
            for (int j = 0; j < columns.length; j++) {
                put.addColumn(Bytes.toBytes("C1"), Bytes.toBytes(columns[j]), Bytes.toBytes(datum[j + 1]));
            }
            puts.add(put);
        }
        Table order = hbaseFactory.getConnection().getTable(TableName.valueOf("ORDER_INFO"));
        order.put(puts);
    }

    @Test
    public void delete() throws IOException {
        TableName order = TableName.valueOf(Bytes.toBytes("ORDER_INFO"));
        hbaseFactory.getAdmin().disableTable(order);
        hbaseFactory.getAdmin().deleteTable(order);
    }
    /**
     * 批量获取数据
     *
     * @throws IOException 异常
     */
    @Test
    public void batchGet() throws IOException {
        Table order = hbaseFactory.getConnection().getTable(TableName.valueOf("ORDER_INFO"));
        List<Get> gets = new ArrayList<Get>();
        for (int i = 0; i < 6; i++) {
            Get get = new Get(Bytes.toBytes(data[8 + i][0]));
            gets.add(get);
        }
        Result[] results = order.get(gets);
        for (Result result : results) {
            String row = Bytes.toString(result.getRow());
            System.out.println("row = " + row);
            Cell[] cells = result.rawCells();
            for (Cell cell : cells) {
                String family = Bytes.toString(CellUtil.cloneFamily(cell));
                String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
                String value = Bytes.toString(CellUtil.cloneValue(cell));
                System.out.println("row = " + row);
                System.out.println(family + ":" + qualifier + "\t" + value);
            }
        }
    }


    /**
     * 批量删除数据
     *
     * @throws IOException 异常
     */
    @Test
    public void patchDelete() throws IOException {
        Table order = hbaseFactory.getConnection().getTable(TableName.valueOf("ORDER_INFO"));
        List<Delete> deletes = new ArrayList<Delete>();
        for (int i = 0; i < 6; i++) {
            Delete delete = new Delete(Bytes.toBytes(data[8 + i][0]));
            deletes.add(delete);
        }
        order.delete(deletes);
    }


    /**
     * 缓存器扫描
     */
    @Test
    public void scanner() throws IOException {
        Table order = hbaseFactory.getConnection().getTable(TableName.valueOf("ORDER_INFO"));
        Scan scan = new Scan();
        scan.setCaching(10);
        scan.withStartRow(Bytes.toBytes("000015"));
        scan.withStopRow(Bytes.toBytes("000359"));
        ResultScanner scanner = order.getScanner(scan);
        for (Result result : scanner) {
            String row = Bytes.toString(result.getRow());
            System.out.println("row = " + row);
            Cell[] cells = result.rawCells();
            for (Cell cell : cells) {
                String family = Bytes.toString(CellUtil.cloneFamily(cell));
                String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
                String value = Bytes.toString(CellUtil.cloneValue(cell));
                System.out.println("row = " + row);
                System.out.println(family + ":" + qualifier + "\t" + value);
            }
        }
    }

    //    3.5 使用过滤器查询rowkey35-80，付款方式为1，金额大于4000的物品
    @Test
    public void filterScanner() throws IOException {
        Table order = hbaseFactory.getConnection().getTable(TableName.valueOf("ORDER_INFO"));

        FilterList filterList=new FilterList(FilterList.Operator.MUST_PASS_ALL);
        Filter filter1=new RowFilter(CompareOperator.GREATER_OR_EQUAL,new BinaryComparator(Bytes.toBytes("000035")));
        Filter filter2=new RowFilter(CompareOperator.LESS_OR_EQUAL,new BinaryComparator(Bytes.toBytes("000080")));
        Filter filter3=new SingleColumnValueFilter(Bytes.toBytes("C1"),Bytes.toBytes("pay_way"),CompareOperator.EQUAL,Bytes.toBytes("1"));
        Filter filter4=new SingleColumnValueFilter(Bytes.toBytes("C1"),Bytes.toBytes("money"),CompareOperator.GREATER_OR_EQUAL,Bytes.toBytes("4000"));
        filterList.addFilter(filter1);
        filterList.addFilter(filter2);
        filterList.addFilter(filter3);
        filterList.addFilter(filter4);
        Scan scan = new Scan();
        scan.setFilter(filterList);
        ResultScanner scanner = order.getScanner(scan);
        for (Result result:scanner){
            String row = Bytes.toString(result.getRow());
            System.out.println("row = " + row);
            Cell[] cells = result.rawCells();
            for (Cell cell : cells) {
                String family = Bytes.toString(CellUtil.cloneFamily(cell));
                String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
                String value = Bytes.toString(CellUtil.cloneValue(cell));
                System.out.println("row = " + row);
                System.out.println(family + ":" + qualifier + "\t" + value);
            }
        }
    }


    private String[][] getData() {
        return new String[][]{
                {"000002", "已提交", "4070", "1", "4944191", "2020/04/25 12:09:16", "手机"},
                {"000003", "已完成", "4350", "1", "1625615", "2020/04/25 12:09:37", "家用电器;电脑"},
                {"000004", "已提交", "6370", "3", "3919700", "2020/04/25 12:09:39", "男装;男鞋"},
                {"000005", "已付款", "6370", "3", "3919700", "2020/04/25 12:09:44", "男装;男鞋"},
                {"000006", "已提交", "9380", "1", "2993700", "2020/04/25 12:09:41", "维修;手机"},
                {"000007", "已付款", "9380", "1", "2993700", "2020/04/25 12:09:46", "维修;手机"},
                {"000008", "已完成", "6400", "2", "5037058", "2020/04/25 12:10:13", "数码;女装"},
                {"000009", "已付款", "280", "1", "3018827", "2020/04/25 12:09:53", "男鞋,汽车"},
                {"000010", "已完成", "5600", "1", "6489579", "2020/04/25 12:08:55", "食品;家用器"},
                {
                        "000011", "已付款", "5600", "1", "6489579", "2020/04/25 12:09:00", "食品;家用器"
                }, {
                "000012", "已提交", "8340", "2", "2948003", "2020/04/25 12:09:26", "男装;男鞋"
        }, {
                "000013", "已付款", "8340", "2", "2948003", "2020/04/25 12:09:30", "男装;男鞋"
        }, {
                "000014", "已提交", "7060", "2", "2092774", "2020/04/25 12:09:38", "酒店;旅游"
        }, {
                "000015", "已提交", "640", "3", "7152356", "2020/04/25 12:09:49", "维修;手机"
        }, {
                "000016", "已付款", "9410", "3", "7152356", "2020/04/25 12:10:01", "维修;手机"
        }, {
                "000017", "已提交", "9390", "3", "8237476", "2020/04/25 12:10:08", "男鞋;汽车"
        }, {
                "000018", "已提交", "7490", "2", "7813118", "2020/04/25 12:09:05", "机票;文娱"
        }, {
                "000019", "已付款", "7490", "2", "7813118", "2020/04/25 12:09:06", "机票;文娱"
        }, {
                "000020", "已付款", "5360", "2", "5301038", "2020/04/25 12:08:50", "维修;手机"
        }, {
                "000021", "已提交", "5360", "2", "5301038", "2020/04/25 12:08:53", "维修;手机"
        }, {
                "000022", "已取消", "5360", "2", "5301038", "2020/04/25 12:08:58", "维修;手机"
        }, {
                "000023", "已付款", "6490", "0", "3141181", "2020/04/25 12:09:22", "食品;家用器"
        }, {
                "000024", "已付款", "3820", "1", "9054826", "2020/04/25 12:10:04", "家用电器;电脑"
        }, {
                "000025", "已提交", "4650", "2", "5837271", "2020/04/25 12:08:52", "机票;文娱"
        }, {
                "000026", "已付款", "4650", "2", "5837271", "2020/04/25 12:08:57", "机票;文娱"
        }, {
                "000027", "已提交", "5000", "1", "5686435", "2020/04/25 12:08:51", "家用电器;电脑"
        }, {
                "000028", "已完成", "5000", "1", "5686435", "2020/04/25 12:08:56", "家用电器;电脑"
        }, {
                "000029", "已提交", "5000", "3", "1274270", "2020/04/25 12:08:41", "男装;男鞋"
        }, {
                "000030", "已付款", "5000", "3", "1274270", "2020/04/25 12:08:42", "男装;男鞋"
        }, {
                "000031", "已完成", "5000", "1", "1274270", "2020/04/25 12:08:43", "男装;男鞋"
        }, {
                "000032", "已完成", "3600", "2", "2661641", "2020/04/25 12:09:58", "维修;手机"
        }, {
                "000033", "已提交", "3950", "1", "3855371", "2020/04/25 12:08:39", "数码;女装"
        }, {
                "000034", "已付款", "3950", "1", "3855371", "2020/04/25 12:08:40", "数码;女装"
        }, {
                "000035", "已完成", "3280", "0", "5553283", "2020/04/25 12:09:01", "食品;家用电器"
        }, {
                "000036", "已提交", "50", "2", "1764961", "2020/04/25 12:10:07", "家用电;电脑"
        }, {
                "000037", "已提交", "6310", "2", "1292805", "2020/04/25 12:09:36", "男装;男鞋"
        }, {
                "000038", "已完成", "8980", "2", "6202324", "2020/04/25 12:09:54", "机票;文娱"
        }, {
                "000039", "已完成", "6830", "3", "6977236", "2020/04/25 12:10:06", "酒店;旅游"
        }, {
                "000040", "已提交", "8610", "1", "5264116", "2020/04/25 12:09:14", "维修;手机"
        }, {
                "000041", "已付款", "8610", "1", "5264116", "2020/04/25 12:09:18", "维修;手机"
        }, {
                "000042", "已提交", "5970", "0", "8051757", "2020/04/25 12:09:07", "男鞋;汽车"
        }, {
                "000043", "已付款", "5970", "0", "8051757", "2020/04/25 12:09:19", "男鞋;汽车"
        }, {
                "000044", "已提交", "4570", "0", "5514248", "2020/04/25 12:09:34", "酒店;旅游"
        }, {
                "000045", "已付款", "4100", "1", "8598963", "2020/04/25 12:09:08", "维修;手机"
        }, {
                "000046", "已完成", "9740", "1", "4816392", "2020/04/25 12:09:51", "数码;女装"
        }, {
                "000047", "已提交", "9740", "1", "4816392", "2020/04/25 12:10:03", "数码;女装"
        }, {
                "000048", "已付款", "6550", "3", "2393699", "2020/04/25 12:08:47", "男鞋;汽车"
        }, {
                "000049", "已付款", "6550", "3", "2393699", "2020/04/25 12:08:48", "男鞋;汽车"
        }, {
                "000050", "已完成", "6550", "3", "2393699", "2020/04/25 12:08:49", "男鞋;汽车"
        }, {
                "000051", "已提交", "4090", "1", "2536942", "2020/04/25 12:10:12", "机票;文娱"
        }, {
                "000052", "已付款", "4090", "1", "2536942", "2020/04/25 12:10:14", "机票;文娱"
        }, {
                "000053", "已完成", "3850", "3", "6803936", "2020/04/25 12:09:20", "酒店;旅游"
        }, {
                "000054", "已提交", "1060", "0", "6119810", "2020/04/25 12:09:21", "维修;手机"
        }, {
                "000055", "已付款", "9270", "2", "5818454", "2020/04/25 12:10:09", "数码;女装"
        }, {
                "000056", "已完成", "8380", "2", "6804703", "2020/04/25 12:09:52", "男鞋;汽车"
        }, {
                "000057", "已提交", "9750", "1", "4382852", "2020/04/25 12:09:43", "数码;女装"
        }, {
                "000058", "已付款", "9750", "1", "4382852", "2020/04/25 12:09:48", "数码;女装"
        }, {
                "000059", "已取消", "9750", "1", "4382852", "2020/04/25 12:10:00", "数码;女装"
        }, {
                "000060", "已提交", "9390", "1", "4182962", "2020/04/25 12:09:57", "机票;文娱"
        }, {
                "000061", "已付款", "9350", "1", "5937549", "2020/04/25 12:09:02", "酒店;旅游"
        }, {
                "000062", "已提交", "4370", "0", "4666456", "2020/04/25 12:09:13", "维修;手机"
        }, {
                "000063", "已付款", "3190", "3", "3200759", "2020/04/25 12:09:25", "数码;女装"
        }, {
                "000064", "已提交", "1100", "0", "3457528", "2020/04/25 12:10:11", "数码;女装"
        }, {
                "000065", "已提交", "850", "0", "8835231", "2020/04/25 12:09:40", "男鞋,汽车"
        }, {
                "000066", "已付款", "850", "0", "8835231", "2020/04/25 12:09:45", "食品,家用电器"
        }, {
                "000067", "已提交", "8040", "0", "8206022", "2020/04/25 12:09:50", "家用电器;电脑"
        }, {
                "000068", "已付款", "8040", "0", "8206022", "2020/04/25 12:10:02", "家用电器;电脑"
        }, {
                "000069", "已付款", "8570", "2", "5319315", "2020/04/25 12:08:44", "机票;文娱"
        }, {
                "000070", "已提交", "5700", "3", "6486444", "2020/04/25 12:09:27", "酒店;旅游"
        }, {
                "000071", "已付款", "5700", "3", "6486444", "2020/04/25 12:09:31", "酒店;旅游"
        }, {
                "000072", "已付款", "7460", "1", "2379296", "2020/04/25 12:09:23", "维修;手机"
        }, {
                "000073", "已提交", "2690", "3", "6686018", "2020/04/25 12:09:55", "数码;女装"
        }, {
                "000074", "已提交", "6310", "2", "1552851", "2020/04/25 12:09:15", "男鞋;汽车"
        }, {
                "000075", "已提交", "4000", "1", "3260372", "2020/04/25 12:09:35", "机票;文娱"
        }, {
                "000076", "已提交", "7370", "3", "3107867", "2020/04/25 12:08:45", "数码;女装"
        }, {
                "000077", "已付款", "7370", "3", "3107867", "2020/04/25 12:08:46", "数码;女装"
        }, {
                "000078", "已提交", "720", "2", "5034117", "2020/04/25 12:09:03", "机票,文娱"
        }, {
                "000079", "已提交", "3630", "1", "6435854", "2020/04/25 12:09:10", "酒店;旅游"
        }, {
                "000080", "已付款", "5000", "0", "2007322", "2020/04/25 12:08:38", "维修;手机"
        }, {
                "000081", "已提交", "2660", "2", "7928516", "2020/04/25 12:09:42", "数码;女装"
        }, {
                "000082", "已付款", "2660", "2", "7928516", "2020/04/25 12:09:47", "数码;女装"
        }, {
                "000083", "已完成", "2660", "2", "7928516", "2020/04/25 12:09:59", "数码;女装"
        }, {
                "000084", "已付款", "8750", "2", "1250995", "2020/04/25 12:09:09", "食品;家用电器"
        }, {
                "000085", "已完成", "410", "0", "1923817", "2020/04/25 12:09:56", "家用,器;电脑"
        }, {
                "000086", "已付款", "6760", "0", "2457464", "2020/04/25 12:08:54", "数码;女装"
        }, {
                "000087", "已提交", "6760", "0", "2457464", "2020/04/25 12:08:59", "数码;女装"
        }, {
                "000088", "已付款", "8120", "2", "7645270", "2020/04/25 12:09:28", "男鞋;汽车"
        }, {
                "000089", "已完成", "8120", "2", "7645270", "2020/04/25 12:09:32", "男鞋;汽车"
        }, {
                "000090", "已付款", "8170", "2", "7695668", "2020/04/25 12:09:11", "家用电器;电脑"
        }, {
                "000091", "已完成", "2560", "2", "4405460", "2020/04/25 12:10:05", "男装;男鞋"
        }, {
                "000092", "已完成", "2370", "2", "8233485", "2020/04/25 12:09:24", "机票;文娱"
        }, {
                "000093", "已付款", "8070", "3", "6387107", "2020/04/25 12:09:04", "酒店;旅游"
        }, {
                "000094", "已完成", "8070", "3", "6387107", "2020/04/25 12:09:17", "酒店;旅游"
        }, {
                "000095", "已付款", "4410", "3", "1981968", "2020/04/25 12:10:10", "维修;手机"
        }, {
                "000096", "已提交", "4010", "1", "6463215", "2020/04/25 12:09:29", "男鞋;汽车"
        }, {
                "000097", "已付款", "4010", "1", "6463215", "2020/04/25 12:09:33", "男鞋;汽车"
        }, {
                "000098", "已付款", "5950", "3", "4060214", "2020/04/25 12:09:12", "机票;文娱"
        }
        };
    }

    @After
    public void destroy() {
        hbaseFactory.destroy();
    }
}
