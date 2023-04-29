import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Tracker {
	
	private TreeMap<LocalDate, Boolean> db;
	private File filePath;
	
	private enum UIOption { today, print, stats, backup, exit }
	private static DateTimeFormatter globalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale( Locale.ITALIAN );

	int MAX_LENGHT_AFTER_PADDING = 9;


	public Tracker(String path) {
		File filePath = new File(path);
		this.filePath = filePath;
		if (filePath.exists()) {
			db = restore(filePath);
		} else {
			db = new TreeMap<>();
		}
		
	}
	
	public TreeMap<LocalDate, Boolean> getDb() {
		return new TreeMap<>(db);
	}
	
	
	// ------------ save / restore database ------------
	public void save(File file) {
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		try {
			mapper.writeValue(file, new TreeMap<String, Double>());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("No file found");
		}
	}
	
	public TreeMap<LocalDate, Boolean> restore(File file) { // TreeMap<Date, Boolean> 
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		TreeMap<String, Boolean> value = null;
		try {
			value = mapper.readValue(file, TreeMap.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("No file found");
		}
		return castStringToLocalDate(value);
	}
	
	private TreeMap<LocalDate, Boolean> castStringToLocalDate(TreeMap<String, Boolean> value) {
			
		TreeMap<LocalDate, Boolean> ret = new TreeMap<>();
		for (String key : value.keySet()) {
			LocalDate date = LocalDate.parse(key, globalFormatter);
			ret.put(date, value.get(key));
		}
		
		return ret;
	}

	// ------------ new entries ------------
	void fillDay(String day, boolean value) {
		LocalDate date = LocalDate.parse(day, globalFormatter);
		fillDay(date, value);
	}
	
	private void fillDay(LocalDate day, boolean value) {
		if (db.containsKey(day)) {
			throw new IllegalStateException("Value already exists");
		}
		db.put(day, value);
	}
	
	void fillToday(boolean value) {
		LocalDate today = getTodayDate();
		fillDay(today, value);		
	}
	
	// ------------ dates ------------
	private Comparator<LocalDate> ldc = (o1, o2) -> {
		if ( o1.isAfter(o2) ) return 1;
		else if ( o1.isBefore(o2) ) return -1;
		return 0;
	};
	
	private LocalDate getTodayDate() {
		return LocalDate.now();
	}
	
	private int daysOfTheMonth(LocalDate date) {
		return date.getMonth()
				.length( date.isLeapYear() );
	}
	
	private List<LocalDate> totalGoodDays() {
		return goodDaysOverRange(
				earliestDayDistance()
				);
	}
	
	private List<LocalDate> goodDaysOverRange(int days) {
		LocalDate base = getTodayDate().minusDays(days);
		return db.keySet().stream()
				.filter( entry -> db.get(entry) )
				.filter( entry -> entry.isAfter(base))
				.sorted( ldc )
				.collect( Collectors.toList() );
	}
	
	private List<Integer> getConsecutives() {
		List<LocalDate> goodDays = goodDaysOverRange(
				earliestDayDistance()
				);
		List<Integer> ret = new ArrayList<>();
		
		if (goodDays.size() < 2) {
			ret.add(goodDays.size());
			return ret;
		}
		
		Iterator<LocalDate> it = goodDays.iterator();
		LocalDate current = it.next();
		int count = 0;
		while (it.hasNext()) {
			LocalDate next = it.next();
			if (current.plusDays(1).equals(next)) {
				count += 1;
			} else {
				ret.add(count);
				count = 0;
			}
			current = next;
		}
		return ret;
	}

	
	private List<Integer> yearSearchSpace() {
		Set<LocalDate> keys = db.keySet();
		if (keys.size() < 1) throw new IllegalStateException();

		Supplier<Stream<LocalDate>> dates = () -> keys.stream();
		LocalDate min = dates.get().min(ldc).orElseThrow();
		LocalDate max = dates.get().max(ldc).orElseThrow();
		
		LinkedList<Integer> yearRange = new LinkedList<>();
		for (int i = min.getYear(); i <= max.getYear(); i++) {
			yearRange.add(i);
		}
		return yearRange;
	}
	
	private int earliestDayDistance() {
		return (int) db.keySet().stream().min(ldc).orElseThrow()
				.until(getTodayDate(), ChronoUnit.DAYS);
	}
	
	// ------------ statistics ------------
	private String getStats() {		
		if (db.keySet().size() == 0) {
			return "No data is filled in";
		}
		
		HashMap<String, Double> stats = new HashMap<>();
		
		// totals
		stats.put("total",	   (double) db.keySet().size());
		stats.put("good days", (double) totalGoodDays().size());
		stats.put("bad days",  (double) stats.get("total") - stats.get("good days"));
		
		int maxTimeRange = earliestDayDistance();
		for(TimeRange tr : TimeRange.values()) {
			TimeRangeParser trp = new TimeRangeParser(tr, stats.get("total").intValue());
			String descriptor = trp.getIterations() + trp.getType();
			int time = trp.getValue();
			if (time <= maxTimeRange) {
				int goodDays = goodDaysOverRange(time).size();
				stats.put(
					descriptor + " good/bad ratio",
					(double) goodDays * 100 / time
				);
				// TODO: add comparison string "(good days, bad days)"
			}
		}
		
		// streaks
		Supplier<Stream<Integer>> consecutives = () -> getConsecutives().stream();
		stats.put(
			"average streak",
			(double) consecutives.get().mapToInt(o->o).sum() / consecutives.get().count()
		);
		stats.put(
			"max streak",
			(double) consecutives.get().mapToInt(e -> e).max().orElseThrow());
		
		return statsMapToString(stats);
	}
	
	private String statsMapToString(HashMap<String, Double> stats) {
		StringBuilder sb = new StringBuilder();
		sb.append("------------ Stats ------------\n");
		for (String field : stats.keySet()) {
			sb.append(field);
			sb.append(": ");
			sb.append(stats.get(field));
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	// ------------ string manipulation ------------
	private String monthToString(LocalDate date) {
		StringBuilder sb = new StringBuilder();
		Month month = date.getMonth();
		sb.append(padStringLeft(month.toString(), MAX_LENGHT_AFTER_PADDING));
		sb.append(" | ");
		for (int day = 1; day <= daysOfTheMonth(date); day++) {
			sb.append(padStringLeft(String.valueOf(day), 2));
			addSpaces(sb, 1);
		}
		return sb.toString();
	}
	
	private void addSpaces(StringBuilder sb, int padding) {
		sb.append("\s".repeat(padding));
	}
	
	private String padStringLeft(String s, int lenght) {
		int n = s.length();
		int padding = lenght - n;
		if (padding < 0 ) throw new IllegalArgumentException("Too little max lenght");
		return s + "\s".repeat(padding);
	}
	
	
	// ------------ object methods ------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		Set<LocalDate> keys = db.keySet();
		if (keys.size() < 1) return "Empty database";
		
		Supplier<Stream<LocalDate>> dates = () -> keys.stream();
		LocalDate current = dates.get().min(ldc).orElseThrow().withDayOfMonth(1);
		LocalDate max = dates.get().max(ldc).orElseThrow();
		
		while ( !current.equals(max.plusDays(1)) ) {
			if (current.getDayOfMonth() == 1) {
				sb.append("\n");
				addSpaces(sb, MAX_LENGHT_AFTER_PADDING);
				sb.append(" |\n");
				sb.append(monthToString(current));
				sb.append("\n");
				addSpaces(sb, MAX_LENGHT_AFTER_PADDING);
				sb.append(" | ");
			}
			checkAndAddEmoji(sb, current);
			current = current.plusDays(1);
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	private void checkAndAddEmoji(StringBuilder sb, LocalDate date) {
		if (db.containsKey(date)) {
			if (db.get(date)) sb.append("✅");
			else sb.append("❌");
		} else {
			addSpaces(sb, 2);
		}
		addSpaces(sb, 1);
	}
	
}

