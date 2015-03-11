import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class Company {
	
	String symbol;
	ArrayList<Date> dates = new ArrayList<Date>();
	
	static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public Company(String symbol) {
		this.symbol = symbol;
	}
	
	public void addDate(Date date) {
		dates.add(date);
	}
	
	public String toString() {
		String out = "";
		Collections.sort(dates);
		
		ArrayList<DateRange> ranges = new ArrayList<DateRange>();
		ranges.add(new DateRange(dates.get(0)));
		for (int index = 1; index < dates.size(); index++) {
			DateRange latestRange = ranges.get(ranges.size() - 1);
			DateRange nextRange = new DateRange(dates.get(index));
			if (!latestRange.addRange(nextRange)) {
				ranges.add(nextRange);
			} else {
				
			}
		}
		
		for (DateRange range: ranges) {
			String startDate = dateFormat.format(range.startDate);
			String endDate = dateFormat.format(range.endDate);
			out += symbol + "," + startDate + "," + endDate + "\r";
		}
		
		
		return out;
	}

}
