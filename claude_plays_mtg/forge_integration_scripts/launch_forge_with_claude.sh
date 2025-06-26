#!/usr/bin/env zsh

# Launch Forge with Claude integration enabled

# Source zshrc for Java path
source ~/.zshrc

echo "Launching Forge with Claude integration..."

# Get the Forge root directory (two levels up from this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Change to forge gui desktop target directory where resources are located
cd "$FORGE_ROOT/forge-gui-desktop/target"

# Add Java module options for XStream to work properly with newer Java versions
JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.util=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.text=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.desktop/java.awt.font=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.desktop/java.awt=ALL-UNNAMED"

# Launch Forge with Claude integration
java -Xmx6G \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.text=ALL-UNNAMED \
  --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
  --add-opens java.desktop/java.awt=ALL-UNNAMED \
  -jar forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar

echo "Forge has exited."
