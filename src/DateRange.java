import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class DateRange {
	
	Date startDate;
	Date endDate;
	
	/**
	 * when no end date specified, add 3 months to start date
	 * @param startDate
	 */
	public DateRange(Date startDate) {
		this.startDate = startDate;
		Calendar cal = new GregorianCalendar();
		cal.setTime((Date) startDate.clone());
		cal.add(Calendar.MONTH, 3);
		this.endDate = cal.getTime();
	}
	
	public DateRange(Date startDate, Date endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public boolean isInRange(Date date) {
		if (date.equals(startDate) || date.after(startDate)) {
			if (date.equals(endDate) || date.before(endDate)) {
				return true;
			}
			return false;
		}
		return false;
	}
	
	public boolean overlaps(DateRange other) {
		// true if start date is in other range
		if (other.isInRange(startDate)) return true;
		
		// true if end date is in other range
		if (other.isInRange(endDate)) return true;
		
		// true if other end date is in this range
		if (isInRange(other.endDate)) return true;
		
		// otherwise false
		return false;
	}
	
	public DateRange merge(DateRange other) {
		if (!overlaps(other)) return null;
		Date mergedStart = (Date) startDate.clone();
		if (other.startDate.before(mergedStart)) mergedStart = (Date) other.startDate.clone();
		Date mergedEnd = (Date) endDate.clone();
		if (other.endDate.after(mergedStart)) mergedEnd = (Date) other.endDate.clone();
		return new DateRange(mergedStart, mergedEnd);
	}
	
	public boolean addRange(DateRange other) {
		if (!overlaps(other)) return false;
		if (other.startDate.before(startDate)) startDate = (Date) other.startDate.clone();
		if (other.endDate.after(endDate)) endDate = (Date) other.endDate.clone();
		return true;
	}

}
