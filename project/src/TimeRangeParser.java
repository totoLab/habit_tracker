
public class TimeRangeParser {

	private String type;
	private int iterations;
	private int value;
	private final int WEEKS = 7, MONTH = WEEKS * 4; // value in days
	
	public TimeRangeParser(TimeRange t, int total) {
		String stringRepresentation = t.toString();
		if (stringRepresentation.equals("total")) {
			type = "total";
			value = total;
		} else if (stringRepresentation.matches("(month[0-9]+|weeks[0-9]+)")){
			type = stringRepresentation.substring(0, 5);
			iterations = Integer.valueOf(stringRepresentation.substring(5, stringRepresentation.length()));
			value = typeValue(type) * iterations;
					
		} else {
			throw new IllegalStateException("Bad time ranges config file names ");
		}
	}
	
	private int typeValue(String type) {
		type = type.toUpperCase();
		if (type.equals("WEEKS")) return WEEKS;
		else if (type.equals("MONTH")) return MONTH;
		else throw new IllegalStateException("Bad time ranges config file values");
	}
	
	public String getType() {
		return type;
	}
	
	public int getValue() {
		return value;
	}

	public int getIterations() {
		return iterations;
	}
	
}
