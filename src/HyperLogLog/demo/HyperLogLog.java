package HyperLogLog.demo;

public class HyperLogLog {

    private final RegisterSet registerSet;
    /**
     * 用于constant常数的选择
     * log(m)
     */
    private final int log2m;
    private final double alphaMM;


    /**
     *
     *  rsd = 1.04/sqrt(m)
     * @param rsd  相对标准偏差
     */
    public HyperLogLog(double rsd) {
        this(log2m(rsd));
    }

    /**
     * rsd = 1.04/sqrt(m)
     * m = (1.04 / rsd)^2
     * @param rsd 相对标准偏差
     * @return
     */
    private static int log2m(double rsd) {
        return (int) (Math.log(( 1.04 / rsd) * ( 1.04 / rsd)) / Math.log(2));
    }

    /**
     * 如果分桶越多，那么估计的精度就会越高，统计学上用来衡量估计精度的一个指标是“相对标准误差”(relative standard deviation，
     * 简称RSD)，RSD的计算公式这里就不给出了，百科上一搜就可以知道，从直观上理解，RSD的值其实就是（(每次估计的值）在（估计均值）
     * 上下的波动）占（估计均值）的比例（这句话加那么多括号是为了方便大家断句）。RSD的值与分桶数m存在如下的计算关系：
     * 有了这个公式，你可以先确定你想要达到的RSD的值，然后再推出分桶的数目m。
     * @param log2m
     * @return
     */
    private static double rsd(int log2m) {
        return  1.04 / Math.sqrt(Math.exp(log2m * Math.log(2)));
    }


    /**
     * accuracy = 1.04/sqrt(2^log2m)
     *
     * @param log2m
     */
    public HyperLogLog(int log2m) {
        this(log2m, new RegisterSet(1 << log2m));
    }

    /**
     *
     * @param registerSet
     */
    public HyperLogLog(int log2m, RegisterSet registerSet) {
        this.registerSet = registerSet;
        this.log2m = log2m;
        // 从log2m中算出m
        int m = 1 << this.log2m;

        alphaMM = getAlphaMM(log2m, m);
    }


    public boolean offerHashed(int hashedValue) {
        // j 代表第几个桶,取hashedValue的前log2m位即可
        // j 介于 0 到 m
        final int j = hashedValue >>> (Integer.SIZE - log2m);
        // r代表 除去前log2m位剩下部分的前导零 + 1
        final int r = Integer.numberOfLeadingZeros((hashedValue << this.log2m) | (1 << (this.log2m - 1)) + 1) + 1;
        return registerSet.updateIfGreater(j, r);
    }

    /**
     * 添加元素
     * @param o  要被添加的元素
     * @return
     */
    public boolean offer(Object o) {
        final int x = MurmurHash.hash(o);
        return offerHashed(x);
    }


    public long cardinality() {
        // 求和平均数
        double registerSum = 0;
        int count = registerSet.count;
        double zeros = 0.0;
        //count是桶的数量
        for (int j = 0; j < registerSet.count; j++) {
            int val = registerSet.get(j);
            registerSum += 1.0 / (1 << val);
            if (val == 0) {
                zeros++;
            }
        }
        // 估计的基数值
        double estimate = alphaMM * (1 / registerSum);

        if (estimate <= (5.0 / 2.0) * count) {  //小数据量修正
            return Math.round(linearCounting(count, zeros));
        } else {
            return Math.round(estimate);
        }
    }


    /**
     *  计算constant常数的取值
     * @param p   log2m
     * @param m   m
     * @return
     */
    protected static double getAlphaMM(final int p, final int m) {
        // See the paper.
        switch (p) {
            case 4:
                return 0.673 * m * m;
            case 5:
                return 0.697 * m * m;
            case 6:
                return 0.709 * m * m;
            default:
                return (0.7213 / (1 + 1.079 / m)) * m * m;
        }
    }

    /**
     *
     * @param m   桶的数目
     * @param V   桶中0的数目
     * @return
     */
    protected static double linearCounting(int m, double V) {
        // 桶的数量 / log(桶的数量/最大连续0的数量)
        return m * Math.log(m / V);
    }

    public static void main(String[] args) {
        HyperLogLog hyperLogLog = new HyperLogLog(0.1325);//64个桶
        //集合中只有下面这些元素
        hyperLogLog.offer("hhh");
        hyperLogLog.offer("mmm");
        hyperLogLog.offer("ccc");
        hyperLogLog.offer("abc");
        hyperLogLog.offer("abd");
        hyperLogLog.offer("jkm");
        hyperLogLog.offer("plplpl");
        hyperLogLog.offer("b");
        hyperLogLog.offer("gg");
        hyperLogLog.offer("aa");
        hyperLogLog.offer("aaa");
        hyperLogLog.offer("bb");
        hyperLogLog.offer("bbb");
        hyperLogLog.offer("bbc");


        int n = 4;
        int res = 1 << n;
        System.out.println("1<<n : " + res);




        //估算基数
        System.out.println(hyperLogLog.cardinality());
    }

}
