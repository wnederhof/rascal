@license{
  Copyright (c) 2009-2015 CWI
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
}
module analysis::statistics::Descriptive

import IO;
import Exception;
import util::Math;


real geometricMean(list[num] l:[]) {
	throw IllegalArgument(l,"Geometric mean cannot be calculated for empty lists");
}
@doc{
Synopsis: Geometric mean of data values.

Description:

Computes the [geometric mean](http://en.wikipedia.org/wiki/Geometric_mean) of the given data values.
}
default real geometricMean([num hd, *num tl]) {
	if (tl == []) {
		return toReal(hd);	
	}
	prod = (hd | it * v | v <- tl);
	if (prod < 0) {
		throw ArithmeticException("Geometric mean can only be calculated for positive numbers");	
	}
	if (prod == 0) {
		return toReal(prod);
	}
	return nroot(prod, 1 + size(tl));
}

@doc{
Synopsis: Kurtosis of data values.

Description:

Computes the [kurtosis](http://en.wikipedia.org/wiki/Kurtosis) of the given data values.
Kurtosis is a measure of the "peakedness" of a distribution.
}
real kurtosis(list[num] values) {
	if (values == []) {
		throw IllegalArgument(values,"Kurtosis cannot be calculated for empty lists");
	}
	varPow = pow(variance(values), 2);
	if (varPow == 0.) {
		throw ArithmeticException("kurtosis is undefined for values with 0 variance");	
	}
	return centralMoment(values, order= 4) / varPow;
}

@doc{
Synopsis: Kurtosis excess of data values.

Description:

Computes the [kurtosis excess](http://en.wikipedia.org/wiki/Kurtosis) of the given data values.
Kurtosis excess is a measure of the "peakedness" of a distribution corrected such that a normal distribution will be 0.
}
real kurtosisExcess(list[num] values) = kurtosis(values) - 3;

@doc{
Synopsis: Largest data value.
}
(&T <: num) max(list[&T <: num] nums) throws EmptyList
	= (head(nums) | it < n ? n : it | n <- tail(nums));


real mean(list[&T<:num] l:[]) {
	throw IllegalArgument(l,"Mean cannot be calculated for empty lists");
}

@doc{
Synopsis: Arithmetic mean of data values.

Description:

Computes the [arithmetic mean](http://en.wikipedia.org/wiki/Arithmetic_mean) of the data values.
}
default real mean(list[num] nums)
	= toReal(sum(nums)) / size(nums);




real median(list[num] l:[]) {
	throw IllegalArgument(l,"Median cannot be calculated for empty lists");
}

@doc{
Synopsis: Median of data values.

Description:

Returns the [median](http://en.wikipedia.org/wiki/Median) of the available values.
This is the same as the 50th [percentile].

Examples:
<screen>
import analysis::statistics::Descriptive;
median([1,2,5,7,8]);
median([1,2,2,6,7,8]);
</screen>

}
default real median(list[num] nums) 
	= mean(middle(nums));

private list[&T] middle(list[&T] nums) {
	nums = sort(nums);
	n = size(nums);
	if (n % 2 == 1) {
		return [nums[n/2]];	
	}	
	n = n / 2;
	return nums[n-1..n+1];
}

@doc{
Synopsis: Smallest data value.
}
(&T <: num) min(list[&T <: num] nums) throws EmptyList
	= (head(nums) | it > n ? n : it | n <- tail(nums));

@doc{
Synopsis: Percentile of data values.

Description:

Returns the `p`th [percentile](http://en.wikipedia.org/wiki/Percentile) of the data values.
 0 < `p` <= 100 should hold.

}
&T <: num percentile(list[&T <: num] nums, num p) {
	if (0 > p || p > 100) {
		throw IllegalArgument(p, "Percentile argument should be between 0 and 100");
	}
	if (nums == []) {
		throw EmptyList();
	}
	nums = sort(nums);
	idx = max(1., toReal(size(nums)) * (toReal(p) / 100));
	return nums[ceil(idx) - 1];
}


num variance(list[num] l:[]) {
	throw IllegalArgument(l,"variance cannot be calculated for empty lists");
}
@doc{
Synopsis: Variance of data values.

Description:
Computes the [variance](http://en.wikipedia.org/wiki/Variance) of the data values.
It measures how far a set of numbers is spread out.
}
num variance([num hd, *num tl]) {
	if (tl == []) {
		return 0.;	
	}
	//Compensated variant of the two pass algorithm
	n = 1 + size(tl);
	mn = mean(tl + hd);
	sum2 = (pow(hd - mn, 2) | it + pow(i - mn, 2) | i <- tl); 
	sum3 = (hd - mn | it + (i - mn) | i <- tl); 
	return (sum2 - (pow(sum3,2)/n)) / (n -1);
}

real skewness(list[num] l:[]) {
	throw IllegalArgument(l,"Standard Deviation cannot be calculated for empty lists");
} 
@doc{
Synopsis: Skewness of data values.

Description:
Returns the [skewness](http://en.wikipedia.org/wiki/Skewness) of the available values. Skewness is a measure of the asymmetry of a given distribution.
}
default real skewness(list[num] values) 
	= centralMoment(values, order=3) / pow(centralMoment(values, order=2), 3/2);

@doc{
Synopsis: Standard deviation of data values.

Description:
Computes the [standard deviation](http://en.wikipedia.org/wiki/Standard_deviation)
of the data values. It shows how much variation exists from the average (mean, or expected value). 
}
real standardDeviation(list[num] values) {
	if (values == []) {
		throw IllegalArgument(values,"Standard Deviation cannot be calculated for empty lists");
	}
	return sqrt(variance(values));
}

public (&T <:num) sum(list[(&T <:num)] _:[]) {
	throw ArithmeticException(
		"For the emtpy list it is not possible to decide the correct precision to return.\n
		'If you want to call sum on empty lists, use sum([0.000]+lst) or sum([0r] +lst) or sum([0]+lst) 
		'to make the list non-empty and indicate the required precision for the sum of the empty list
		");
}
@doc{
Synopsis: Sum of data values.
}
public default (&T <:num) sum([(&T <: num) hd, *(&T <: num) tl])
	= (hd | it + i | i <- tl);

@doc{
Synopsis: Sum of the squares of data values.
}
(&T <:num) sumsq(list[&T <:num] values)
	= sum([ n * n | n <- values]);

@doc{
	Calculate the k-th central moment
}
real centralMoment(list[num] nums, int order = 1) {
	if (nums == []) {
		throw IllegalArgument(nums,"Central moment cannot be calculated for empty lists");
	}
	if (order < 0) {
		throw IllegalArgument(nums,"Central moment cannot be calculated for the <order>-th order.");
	}
	if (order == 0) {
		return 1.;	
	}
	if (order == 1) {
		return 0.;	
	}
	mn = mean(nums);
	return moment([n - mn | n <- nums], order = order);
}

@doc{
	Calculate the k-th moment
}
real moment(list[num] nums, int order = 1) {
	if (nums == []) {
		throw IllegalArgument(nums,"Moment cannot be calculated for empty lists");
	}
	if (order < 0) {
		throw IllegalArgument(order,"Central moment cannot be calculated for the <order>-th order.");
	}
	if (order == 0) {
		return 1.;	
	}
	if (order == 1) {
		return toReal(sum(nums)) / size(nums);	
	}
	return (0. | it + pow(n, order) | n <- nums) / size(nums);
}


// importing functions from List.rsc to avoid the overlapping to cause typechecker issues
@javaClass{org.rascalmpl.library.Prelude}
private java int size(list[&T] lst);
@javaClass{org.rascalmpl.library.Prelude}
private java list[&T] tail(list[&T] lst) throws EmptyList;
@javaClass{org.rascalmpl.library.Prelude}
private java &T head(list[&T] lst) throws EmptyList;

private list[&T] sort(list[&T] lst) =
	sort(lst, bool (&T a,&T b) { return a < b; } );
	
@javaClass{org.rascalmpl.library.Prelude}
private java list[&T] sort(list[&T] l, bool (&T a, &T b) less) ;