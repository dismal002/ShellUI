# ShellUI User Guide

ShellUI is a powerful Android utility that allows you to trigger native UI components directly from the shell (`adb` or on-device scripts in `/data/local/tmp`) and receive user input back as a standard return value.

---

## 🚀 Getting Started

### 1. Installation
1. Build the APK using `./gradlew assembleDebug`.
2. Install it on your device: `adb install app/build/outputs/apk/debug/app-debug.apk`.
3. The app does not have a launcher icon. Use the commands below to interact with it.

### 2. Basic Command Structure
ShellUI is invoked via `am start`. To wait for user input, use the `-W` flag. To capture output, specify a file path with `--es output`.

---

## 🛠 Feature Reference & Examples

All examples below are designed to run in a shell script (e.g., in `/data/local/tmp/myscript.sh`).

### 🍞 Toasts
Toasts are non-blocking and do not return values.
```bash
# Display a short message
am start -n com.dismal.shellui/.ShellActivity --es action toast --es message "Operation Successful"
```

### 💬 Button Dialogs
Simple dialogs with custom buttons.
```bash
OUT="/data/local/tmp/ui_out"
rm -f $OUT
am start -W -n com.dismal.shellui/.ShellActivity \
    --es action dialog --es title "Update" --es message "An update is available." \
    --es buttons "Upgrade,Later,Cancel" \
    --es output "$OUT" > /dev/null
RESULT=$(cat $OUT)
echo "User chose: $RESULT"
```

### 📝 Text Input
Captures a single line of text.
```bash
OUT="/data/local/tmp/ui_out"
rm -f $OUT
am start -W -n com.dismal.shellui/.ShellActivity \
    --es action input --es title "Identify" --es hint "Enter your name" \
    --es output "$OUT" > /dev/null
NAME=$(cat $OUT)
echo "Hello, $NAME"
```

### 🔘 Radio Buttons
Allows selecting exactly one item from a list.
```bash
OUT="/data/local/tmp/ui_out"
rm -f $OUT
am start -W -n com.dismal.shellui/.ShellActivity \
    --es action dialog --es title "Favorite Color" --es mode radio \
    --es items "Red,Blue,Green,Yellow" \
    --es output "$OUT" > /dev/null
COLOR=$(cat $OUT)
echo "Selected color: $COLOR"
```

### 📅 Date & Time Pickers
Native Android pickers for date and time.
```bash
# Date (Returns YYYY-MM-DD)
OUT="/data/local/tmp/ui_out"
am start -W -n com.dismal.shellui/.ShellActivity --es action date --es title "Select Date" --es output "$OUT" > /dev/null
echo "Selected Date: $(cat $OUT)"

# Time (Returns HH:MM)
am start -W -n com.dismal.shellui/.ShellActivity --es action time --es title "Set Alarm" --es output "$OUT" > /dev/null
echo "Selected Time: $(cat $OUT)"
```

### 🎨 Color Picker
Currently provides a simple notification as the advanced picker requires an external module.
```bash
OUT="/data/local/tmp/ui_out"
am start -W -n com.dismal.shellui/.ShellActivity --es action color --es title "Pick a Theme Color" --es output "$OUT" > /dev/null
# Note: Currently stubs to "Color picker not available"
```

### 📋 Custom Forms (Includes Dropdowns)
Create complex UIs. Dropdowns are called `spinner` in the schema.
- **Schema**: `Label:Type:Options;...`
- **Field Types**: `text`, `radio`, `checkbox`, `spinner`
```bash
OUT="/data/local/tmp/ui_out"
rm -f $OUT
SCHEMA="Username:text;Role:spinner:Admin,User,Guest;Agreed:checkbox"
am start -W -n com.dismal.shellui/.ShellActivity \
    --es action form --es title "Registration" \
    --es schema "$SCHEMA" \
    --es output "$OUT" > /dev/null

echo "Form Results:"
cat $OUT
# Output format:
# Username:Alice
# Role:Admin
# Agreed:true
```

### ⏳ Loading Spinner (Persistent)
```bash
# Start the spinner (Non-blocking, no -W)
am start -n com.dismal.shellui/.ShellActivity --es action loading --es title "Syncing" --es message "Please wait..."

# ... perform background work ...
sleep 3

# Hide the spinner
am start -n com.dismal.shellui/.ShellActivity --es action hide
```

---

## ↩️ Potential Return Values

| Component | Format | Example |
|:--- |:--- |:--- |
| **Simple Dialog** | Button Label | `Upgrade` |
| **Radio/List** | Item Label | `Blue` |
| **Checkbox** | Comma-separated labels | `Option1,Option3` |
| **Input** | String | `Alice` |
| **Date** | `YYYY-MM-DD` | `2026-01-10` |
| **Time** | `HH:MM` | `16:15` |
| **Form** | `Label:Value` (Newlines) | `Name:Alice\nRole:Admin` |

---

### 🛠 Capture Directly to Variable (No File)
You can avoid using files by capturing the app's Logcat output. ShellUI logs all results with the tag `ShellUI_Result`.

```bash
# 1. Clear previous logs
logcat -c

# 2. Start the activity (Wait for finish)
am start -W -n com.dismal.shellui/.ShellActivity --es action input --es title "Fast Input" > /dev/null

# 3. Capture the last log entry for our tag
# We use cut -d: -f3 because the log line looks like 'I/ShellUI_Result( 123): Your Result'
RESULT=$(logcat -d -s ShellUI_Result -b main | grep "ShellUI_Result" | tail -n 1 | cut -d: -f3 | xargs)

echo "The variable contains: $RESULT"
```

---

## 💻 Running Scripts in /data/local/tmp

For scripts running directly on the device, follow this robust pattern:

```bash
#!/system/bin/sh
# 1. Define output file
OUT="/data/local/tmp/ui_response"

# 2. Function to trigger UI and wait for result
get_user_input() {
    rm -f $OUT
    # triggering the activity
    am start -W -n com.dismal.shellui/.ShellActivity "$@" --es output $OUT > /dev/null
    
    # Wait for the file to exist (ShellUI writes it just before finishing)
    while [ ! -f $OUT ]; do
        sleep 0.1
    done
    cat $OUT
}

# Example Usage:
AGE=$(get_user_input --es action input --es title "Age" --es hint "e.g. 25")
echo "User age is $AGE"
```

---

## ⚠️ Troubleshooting
- **Permission Denied**: Ensure your script has execution permissions: `chmod +x /data/local/tmp/script.sh`.
- **Hanging**: Always use `-W` if you expect a response file.
- **No Output**: Ensure the path provided to `--es output` is writable by the app (e.g., `/data/local/tmp` usually works if permissions are set correctly, or use `/sdcard`).
