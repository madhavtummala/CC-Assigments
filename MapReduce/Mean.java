package samples.mean;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.TaskLog;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.WritableComparable;

import java.io.*;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: andrea
 * Date: 3/7/14
 * Time: 9:19 AM
 */

/**
 * this mapreduce job computes the mean of the temperature of the month for every year
 * given a data file like this:
 * 01012014,-2.1,5.0
 * 02012014,-1.8,5.1
 * 03012014,-3.6,2.5
 * ...
 */
public class Mean {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: Mean <in> <out>");
            System.exit(2);
        }
        Job job = Job.getInstance(conf);
        job.setJobName("Mean");
        job.setJarByClass(Mean.class);
        job.setMapperClass(MeanMapper.class);
        job.setReducerClass(MeanReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(SumCount.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class MeanMapper extends Mapper<Object, Text, Text, SumCount> {

        private final int DATE = 0;
        private final int MIN = 1;
        private final int MAX = 2;

        private Map<Text, List<Double>> maxMap = new HashMap<>();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            // gets the fields of the CSV line
            String[] values = value.toString().split((","));

            // defensive check
            if (values.length != 3) {
                return;
            }

            // gets date and max temperature
            String date = values[DATE];
            Text month = new Text(date.substring(2));
            Double max = Double.parseDouble(values[MAX]);

            // if not present, put this month into the map
            if (!maxMap.containsKey(month)) {
                maxMap.put(month, new ArrayList<Double>());
            }

            // adds the max temperature for this day to the list of temperatures
            maxMap.get(month).add(max);
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {

            // loops over the months collected in the map() method
            for (Text month: maxMap.keySet()) {

                List<Double> temperatures = maxMap.get(month);

                // computes the sum of the max temperatures for this month
                Double sum = 0d;
                for (Double max: temperatures) {
                    sum += max;
                }

                // emits the month as the key and a SumCount as the value
                context.write(month, new SumCount(sum, temperatures.size()));
            }
        }
    }

    public static class MeanReducer extends Reducer<Text, SumCount, Text, DoubleWritable> {

        private Map<Text, SumCount> sumCountMap = new HashMap<>();

        @Override
        public void reduce(Text key, Iterable<SumCount> values, Context context) throws IOException, InterruptedException {

            SumCount totalSumCount = new SumCount();

            // loops over all the SumCount objects received for this month (the "key" param)
            for (SumCount sumCount : values) {

                // sums all of them
                totalSumCount.addSumCount(sumCount);
            }

            // puts the resulting SumCount into a map
            sumCountMap.put(new Text(key), totalSumCount);
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {

            // loops over the months collected in the reduce() method
            for (Text month: sumCountMap.keySet()) {

                double sum = sumCountMap.get(month).getSum().get();
                int count = sumCountMap.get(month).getCount().get();

                // emits the month and the mean of the max temperatures for the month
                context.write(month, new DoubleWritable(sum/count));
            }
        }
    }


	static class SumCount implements WritableComparable<SumCount> {

	    DoubleWritable sum;
	    IntWritable count;

	    public SumCount() {
	        set(new DoubleWritable(0), new IntWritable(0));
	    }

	    public SumCount(Double sum, Integer count) {
	        set(new DoubleWritable(sum), new IntWritable(count));
	    }

	    public void set(DoubleWritable _sum, IntWritable _count) {
	        sum = _sum;
	        count = _count;
	    }

	    public DoubleWritable getSum() {
	        return sum;
	    }

	    public IntWritable getCount() {
	        return count;
	    }

	    public void addSumCount(SumCount sumCount) {
	        set(new DoubleWritable(this.sum.get() + sumCount.getSum().get()), new IntWritable(this.count.get() + sumCount.getCount().get()));
	    }

	    @Override
	    public void write(DataOutput dataOutput) throws IOException {

	        sum.write(dataOutput);
	        count.write(dataOutput);
	    }

	    @Override
	    public void readFields(DataInput dataInput) throws IOException {

	        sum.readFields(dataInput);
	        count.readFields(dataInput);
	    }

	    @Override
	    public int compareTo(SumCount sumCount) {

	        // compares the first of the two values
	        int comparison = sum.compareTo(sumCount.sum);

	         // if they're not equal, return the value of compareTo between the "sum" value
	        if (comparison != 0) {
	            return comparison;
	        }

	        // else return the value of compareTo between the "count" value
	        return count.compareTo(sumCount.count);
	    }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;

	        SumCount sumCount = (SumCount) o;

	        return count.equals(sumCount.count) && sum.equals(sumCount.sum);
	    }

	    @Override
	    public int hashCode() {
	        int result = sum.hashCode();
	        result = 31 * result + count.hashCode();
	        return result;
	    }
	}


}
