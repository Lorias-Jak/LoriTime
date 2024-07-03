#!/bin/bash

# Array of files to copy
files_to_copy=(
    .gitignore
    .editorconfig
    .gitattributes
    .github/pull_request_template.md
)

# Get the absolute path to the script's directory
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Define the path to the .gitmodules file relative to the script's directory
gitmodules_file="$script_dir/../.gitmodules"

# Check if the .gitmodules file exists
if [ ! -f "$gitmodules_file" ]; then
    echo "Error: .gitmodules file not found."
    exit 1
fi

# Extract submodule names from .gitmodules and store in an array
mapfile -t submodule_names < <(awk -F'=' '/path/ {gsub(/^[ \t]+/, "", $2); print $2}' "$gitmodules_file")

# Copy specified files to each submodule
for submodule in "${submodule_names[@]}"; do
    submodule_path="$script_dir/../$submodule"
    if [ -d "$submodule_path" ]; then
        for file in "${files_to_copy[@]}"; do
            destination="$submodule_path/$file"
            mkdir -p "$(dirname "$destination")"
            cp "$script_dir/../$file" "$destination"
            echo "$file copied to $submodule"
        done
    else
        echo "Warning: Submodule directory $submodule_path not found."
    fi
done
