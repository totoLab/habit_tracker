import lib, datetime, os

# ---------- global ---------- #

default_path = "./backup.json"

class Options:
    options = ["today", "print", "print stats", "backup", "exit"]


# ---------- user handling ---------- #

def fill_today(data):
    today = str(datetime.date.today())
    fill(data, today)

def fill(data, day):
    print(day, end=" ? ")
    data[day] = user_fill()

def user_fill():
    return input("[y/n]: ") == "y"

def fill_today_ui(data):
    lib.print_as_calendar(data)
    fill_today(data)
    print()
    lib.print_as_calendar(data)

def ui_db_handler(data, options=Options.options):
    choice = ui_choice(options)
    if choice == "today":
        fill_today_ui(data)
    elif choice == "backup":
        print("Deactivated feature. Don't try")
        # lib.backup(data)
    elif choice == "print":
        lib.print_as_calendar(data)
    elif choice == "print stats":
        lib.stats(data)
    else:
        return
    return ui_db_handler(data)

def ui_choice(options):
    print("Menu:")
    print(f"{' '.join(f'{i}) {options[i]}' for i in range(len(options)))}")
    choice = options[menu(options)]
    return choice

def menu(options):
    option = ""
    while not option.isnumeric() or not 0 <= int(option) <= len(options) - 1:
        option = input("Choose a valid option: ")
    return int(option)

# ---------- program flow ---------- #

def flow(default, path):
    print("Loading last save...")
    print(f"Opened tracker {lib.get_basename_noext(path)}")
    if default:
        data = lib.load_data(path)
        ui_db_handler(data)
        lib.dump_data(data, path)
    else:
        continuos_data_load(path)

def main(default=True, path=default_path):

    folder = lib.get_path(os.path.realpath(__file__))
    list_of_dbs = list(filter(lambda name: ".json" in name, os.listdir(folder)))

    fullpath = folder + "/" + ui_choice(list_of_dbs)
    flow(
        default,
        fullpath
    )
    
    print("Quitting...")

if __name__ == "__main__":
    main()
