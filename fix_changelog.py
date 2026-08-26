import re

with open("CHANGELOG.md", "r") as f:
    text = f.read()

# We need to extract the blocks. Let's split by "## ["
blocks = text.split("## [")

header = blocks[0]
versions = {}
unreleased = ""

for b in blocks[1:]:
    if b.startswith("Unreleased]"):
        unreleased = "## [" + b
        continue
    
    # Extract version
    match = re.match(r'([0-9\.]+)\].*', b)
    if match:
        v = match.group(1)
        versions[v] = "## [" + b

# Let's fix the version codes in the text
def fix_code(text, new_code):
    text = re.sub(r'versionCode` to `[0-9]+`', f'versionCode` to `{new_code}`', text)
    text = re.sub(r'Incremented `versionCode` to `[0-9]+`', f'Incremented `versionCode` to `{new_code}`', text)
    text = re.sub(r'Set `versionCode` to `[0-9]+`', f'Set `versionCode` to `{new_code}`', text)
    text = re.sub(r'Upgraded app `versionCode` to `[0-9]+`', f'Upgraded app `versionCode` to `{new_code}`', text)
    return text

versions["1.3.1"] = fix_code(versions["1.3.1"], 6)
versions["1.3.0"] = fix_code(versions["1.3.0"], 5)
versions["1.2.1"] = fix_code(versions["1.2.1"], 4)
versions["1.2.0"] = fix_code(versions["1.2.0"], 3)
if "1.1.0" in versions:
    versions["1.1.0"] = fix_code(versions["1.1.0"], 2)

# Create 1.3.2 block
new_1_3_2 = """## [1.3.2] - 2026-08-25

### Changed
- **Version Bump**: Updated `versionCode` to `7` and `versionName` to `1.3.2` to ensure consistent and correct chronological versioning history.
- **Documentation**: Updated `README.md` setup commands to correctly recommend the Gradle wrapper (`./gradlew`) rather than the global Gradle installation.

---

"""

# Reassemble
sorted_versions = ["1.3.1", "1.3.0", "1.2.1", "1.2.0", "1.1.0", "1.0.0"]

final_text = header + unreleased + new_1_3_2
for v in sorted_versions:
    if v in versions:
        final_text += versions[v]

with open("CHANGELOG.md", "w") as f:
    f.write(final_text)

print("Changelog fixed.")
