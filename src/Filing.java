import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;


public class Filing implements Comparable<Filing>{
	
	// top level info
	boolean isValid = true;
	boolean isQuarterlyFiling = false;
	double benfordScore = 0;
	int talliedIndicatorCount = 0;
	
	// quality check
	double theoreticalBenfordScore = 0;
	
	// maps column names to index values
	HashMap<String, Integer> indices;
	
	// Filing information
	String symbol;
	Integer year = -1;
	Integer quarter = -1;
	Integer yearQuarter = -1;
	String filingString;
	Date filingDate;
	Float totalRevenue;
	
	// sort options
	public static final int SORT_BY_FILING = 1;
	public static final int SORT_BY_BENFORD = 2;
	public static final int SORT_BY_THEORETICAL_BENFORD = 3;
	
	// how we are sorting
	public static int sortBy = SORT_BY_BENFORD;
	
	// private static
	static Date today = new Date();
	static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * Source: https://en.wikipedia.org/wiki/Benford%27s_law
	 * 1	30.1%	
	 * 2	17.6%	
	 * 3	12.5%	
	 * 4	9.7%	
	 * 5	7.9%	
	 * 6	6.7%	
	 * 7	5.8%	
	 * 8	5.1%	
	 * 9	4.6%	
	 * @param values
	 */
	static final double [] benfordsDistribution = {0, 0.301, 0.176, 0.125, 0.097, 0.079, 0.067, 0.058, 0.051, 0.046};
	
	
	public Filing(String [] values, HashMap<String, Integer> indices) {
		this.indices = indices;
		setup(values);
		calculateBenford(values);
	}
	
	
	public void calculateBenford(String [] values) {
		HashMap<Integer, Integer> tallies = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> threeDigits = new HashMap<Integer, Integer>();
		
		// tallying the most significant digits
		//for Zacks fundamentals, the first 60 values are non-numeric
		for (int index = 60; index < values.length; index++) {
			if (values[index].isEmpty()) continue;
			Float value = null;
			try {
				value = Float.parseFloat(values[index]);
			} catch (NumberFormatException nfe) {}
			if (value != null) {
				int sigFig = getMostSignificantDigit(value);
				int firstThree = getFirstThreeDigits(value);
				Integer previousFirstThree = threeDigits.get(firstThree);
				if (previousFirstThree == null) {
					threeDigits.put(firstThree, 1);
					Integer tally = tallies.get(sigFig);
					if (tally == null) tally = 0;
					tally++;
					tallies.put(sigFig, tally);
					talliedIndicatorCount++;
				}
			}
		}
		
		benfordScore = calculateScoreFromTallies(tallies);

	}
	
	
	/**
	 * calculating the Benford score as greatest difference between
	 * and empirical value and the known Benford value
	 * @param tallies
	 * @return
	 */
	private double calculateScoreFromTallies(HashMap<Integer, Integer> tallies) {
		double maxDifference = 0;
		for (int index = 1; index < 10; index++) {
			Integer tally = tallies.get(index);
			if (tally == null) tally = 0;
			double percent = (double) tally / talliedIndicatorCount;
			double difference = Math.abs(percent - benfordsDistribution[index]);
			if (difference > maxDifference) maxDifference = difference;
		}
		return maxDifference;
	}
	
	
	/**
	 * creates fake benford score based on benford distribution
	 * and using the same number of entries as tallied indicators for the empirical score
	 * @param seed
	 */
	public void createFakeBenford(long seed) {
		Random random = new Random(seed);
		// for quality checking, create a fake benford score based
		// on the benford distribution
		HashMap<Integer, Integer> tallies = new HashMap<Integer, Integer>();
		
		for (int index = 0; index < talliedIndicatorCount; index++) {
			double target = random.nextDouble();
			double zone = 1;
			int sigFig = 0;
			int zoneLevel = 0;
			for (double level: benfordsDistribution) {
				zone -= level;
				if (zone < target) {
					sigFig = zoneLevel;
					break;
				}
				zoneLevel++;
			}
			Integer tally = tallies.get(sigFig);
			if (tally == null) tally = 0;
			tally++;
			tallies.put(sigFig, tally);
		}
		
		theoreticalBenfordScore = calculateScoreFromTallies(tallies);
	}

	
	
	@Override
	public int compareTo(Filing b) {
		int comparison = 0;
		if (sortBy == SORT_BY_FILING) comparison = filingDate.compareTo(b.filingDate);
		if (sortBy == SORT_BY_BENFORD) {
			if (benfordScore > b.benfordScore) comparison = 1;
			if (benfordScore < b.benfordScore) comparison = -1;
		}
		if (sortBy == SORT_BY_THEORETICAL_BENFORD) {
			if (theoreticalBenfordScore > b.theoreticalBenfordScore) comparison = 1;
			if (theoreticalBenfordScore < b.theoreticalBenfordScore) comparison = -1;
		}
		return comparison;
	}


	private void setup(String [] values) {
		isQuarterlyFiling = getInfo("FILING_TYPE", values).equals("10-Q");
		symbol = getInfo("TICKER", values);

		if (isQuarterlyFiling) {
			try {
				year = getInt("PER_FISC_YEAR", values);
				quarter = getInt("PER_FISC_QTR", values);
				yearQuarter = getInt("QTR_NBR", values);
				totalRevenue = getFloat("TOT_REVNU", values);
			} catch (NumberFormatException nfe) {
				isValid = false;
			}
		}
		
		String dateString = getInfo("FILING_DATE", values);
		filingString = dateString;
		if (dateString != null) {
			try {
				filingDate = dateFormat.parse(dateString);
			} catch (ParseException e) {
				isValid = false;
			}
		}
		
		if (year == null) isValid = false;
		if (quarter == null) isValid = false;
		if (yearQuarter == null) isValid = false;
		if (filingDate == null) {
				isValid = false;
		} else {
			if (filingDate.compareTo(today) > 0) isValid = false;
		}
	}
	
	
	private Float getFloat(String key, String [] values) {
		String info = getInfo(key, values);
		if (info == null) {
			return null;
		} else {
			return Float.parseFloat(info);
		}
	}
	
	
	private Integer getInt(String key, String [] values) {
		String info = getInfo(key, values);
		if (info == null) {
			return null;
		} else {
			return Integer.parseInt(info);
		}
	}
	
	
	private static int getMostSignificantDigit(double value) {
		value = Math.abs(value);
		if (value == 0) return 0;
		while (value < 1) value *= 10;
		char firstChar = String.valueOf(value).charAt(0);
		return Integer.parseInt(firstChar + "");
	}
	
	private static int getFirstThreeDigits(double value) {
		value = Math.abs(value);
		if (value == 0) return 0;
		while (value < 100) value *= 10;
		while (value > 1000) value /= 10;
		String digits = String.valueOf(value).substring(0, 3);
		return Integer.parseInt(digits);
	}
	
	
	private String getInfo(String code, String [] values) {
		String out = values[indices.get(code)];
		if (out.isEmpty()) out = null;
		return out;
	}

}
