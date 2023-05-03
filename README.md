# Habit Tracker

Terminal-based daily habits tracker to be used to track days in which an activity was done (e.g. going to the gym).

## Build

To build the project and download the dependencies, the following script uses `maven`.

```
./build_jar.sh
```

## Usage

```
./habit_tracker.sh path_of_tracker.json
```

- To create a new tracker insert the path of the file you want to save it to as `path_of_tracker.json`. <br>
- To restore a previously saved tracker insert its path as `path_of_tracker.json`. <br>

:warning: always select exit to close the file if you want to save the changes made to the tracker.
