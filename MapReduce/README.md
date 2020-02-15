## Application of Map Reduce

### Calculation of Mean of Data

The given program has two files, Mean.java and Count.java

`Count.java` has the implementation of SumCount class which implements WritableComparable class of hadoop and maintains the sum and count (till that point) in private variables. It has functions for updating the sum and count from new values belonging to the vector produced after map function. So, this is file has the implementation of `reduce` feature.

`Mean.java` has the implementation of `<map` feature. It takes the input data (the format is specified in the file itself). The example input is date and the temperature on that date. Using map, we have aggregated the month name as key value and made a list of all the temperatures belonging to a month over all the years in the data set.

After appling the reduce funciton, we get the mean temperature for each month from the data set as `sum/count` for that month. If a large data set is taken, then this hadoop implementation will be considerable faster than the general code for doing the same.

### Running

You need to have maven and java setup.
The `lib` path of the maven project should be given as an argument.

```bash
javac Count.java -cp <lib_path>
java Mean.java -cp <lib_path>
```

Recommended to use an IDE instead of command line since java is shitty.
