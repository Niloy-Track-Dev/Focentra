import re

with open("CHANGELOG.md", "r") as f:
    text = f.read()

text = text.replace("Updated `versionCode` to `7` and `versionName` to `1.3.2`", "Updated `versionCode` to `5` and `versionName` to `1.3.2`")
text = text.replace("Updated `versionCode` to `6` and `versionName` to `1.3.1`", "Updated `versionCode` to `4` and `versionName` to `1.3.1`")
text = text.replace("Updated `versionCode` to `5` and `versionName` to `1.3.0`", "Updated `versionCode` to `3` and `versionName` to `1.3.0`")
text = text.replace("Set `versionCode` to `3` and `versionName` to `1.2.0`", "Set `versionCode` to `2` and `versionName` to `1.2.0`")

# For 1.1.0 and 1.2.1, let's remove the versionCode mention entirely to avoid confusion since they weren't official releases
text = re.sub(r"- \*\*Version Bump\*\*: Upgraded app `versionCode` to `2` and `versionName` to `1\.1\.0`\.\n", "- **Version**: Pre-release version `1.1.0`.\n", text)
text = re.sub(r"  - Incremented `versionCode` to `4` and `versionName` to `1\.2\.1`\.\n", "  - Version bumped to `1.2.1`.\n", text)


with open("CHANGELOG.md", "w") as f:
    f.write(text)

