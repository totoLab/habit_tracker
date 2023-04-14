import json, datetime, calendar, emoji

# ---------- global ---------- #

class SignalingStrings:
    clean = emoji.emojize(":check_mark_button:")
    ops = emoji.emojize(":cross_mark:")

class DatesConstants:
    WEEK = 7
    MONTH = 4 * WEEK


# ---------- filenames ---------- #

def get_basename_noext(path):
    tmp = path.split("/")
    return tmp[len(tmp) - 1].split(".")[0]

def get_path(path):
    tmp = path.split("/")
    tmp.pop()
    return "/".join(tmp)

# ---------- files ---------- #

def create_empty(path):
    dump_data({}, path)

def dump_data(data, path):
    with open(path, "w") as f:
        json.dump(data, f)

def load_data(path):
    with open(path, "r") as f:
        return json.load(f)

# ---------- dates ---------- #

def earlier_latest_years(calendar_dict):
    if len(calendar_dict) != 0:
        ordered_dates = sorted(calendar_dict)
        dates = [ ordered_dates[0], ordered_dates[len(ordered_dates) - 1] ]
        ret = []
        for date in dates:
            ret.append(int(date[:4]))
        return ret
    else:
        current_year = datetime.date.today().year
        return current_year, current_year

def good_over_range(calendar_dict, days):
    ordered_dates = sorted(calendar_dict)
    last = len(ordered_dates) - 1
    first = last - days
    good_counter = 0
    for date_index in range(last, first, -1):
        date = ordered_dates[date_index]
        if calendar_dict[date]:
            good_counter += 1
    return good_counter
    
# ---------- output ---------- #

def print_as_calendar(calendar_dict):
    earlier_year, latest_year = earlier_latest_years(calendar_dict)

    for year in range(earlier_year, latest_year + 1):
        print(f"{year}|{' '.join(f'{i:02d}' for i in range(1,32))}")
        for month in range(1, 13):
            month_str = calendar.month_name[month][:3]
            row = ""
            for day in range(1, 32):
                date_str = f"{year}-{month:02}-{day:02}"
                if date_str in calendar_dict:
                    value = calendar_dict[date_str]
                    if value:
                        row += f"{SignalingStrings.clean} "
                    else:
                        row += f"{SignalingStrings.ops} "
                else:
                    row += "   "
            if any(char in row for char in [SignalingStrings.clean, SignalingStrings.ops]):
                print(f"{month_str}  {row}")
        print("\n" if year != latest_year else "", end="")

def stats(data):
    if len(data) == 0:
        print("No data")
        return


    total = len(data)
    s = {}
    s["total days"] = total
    s["good days"] = good_over_range(data, total)
    s["bad days"] = s["total days"] - s["good days"]

    # this it the total ratio, add last 6 months, last 8 weeks, last 4 weeks
    time_ranges = [ ("total" , total), ( "6 months", 6 * DatesConstants.MONTH), ("12 weeks", 12 * DatesConstants.WEEK), ("8 weeks", 8 * DatesConstants.WEEK), ("4 weeks", 4 * DatesConstants.WEEK), ("2 weeks", 2 * DatesConstants.WEEK)]
    for descriptor, time_range in time_ranges:
        if not time_range > total:
            good_days =  good_over_range(data, time_range)
            ratio = (good_days, time_range - good_days)
            s[f"{descriptor} good/bad ratio"] = f"{round( good_days * 100 / time_range, 2)}% -> {ratio}"

    streaks = []
    total_counter = 0
    for key in data:
        value = data[key]
        if value:
            total_counter += 1
        else:
            streaks.append(total_counter)
            total_counter = 0
    
    s["average streak"] = round( sum(streaks) / len(streaks) )
    s["max streak"] = max(streaks)
    
    for key in s:
        print(f"{key}: {s[key]}")

# ---------- backups ---------- #

def backup(data):
    lib.dump_data(data, "backup1.json")