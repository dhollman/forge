#!/bin/bash
# Build script for Forge with Claude integration
# This script handles the complete build process including resource copying

set -e  # Exit on any error

# Get the Forge root directory (two levels up from this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "🔧 Starting Forge build process..."
echo "   Working in: $FORGE_ROOT"

# Change to forge root directory
cd "$FORGE_ROOT"

# Step 1: Clean the project
echo "📦 Cleaning project..."
mvn clean -q

# Step 2: Build forge-gui-desktop with all dependencies (this includes forge-ai)
echo "🏗️  Building forge-gui-desktop module..."
mvn package -pl forge-gui-desktop -am -DskipTests -Dcheckstyle.skip=true -q

# Step 3: Copy resources to target directory
echo "📁 Copying game resources..."
mkdir -p forge-gui-desktop/target/res
cp -r forge-gui/res/* forge-gui-desktop/target/res/

# Step 4: Copy launch scripts
echo "📋 Copying launch scripts..."
cp forge-gui-desktop/src/main/config/forge.sh forge-gui-desktop/target/
chmod +x forge-gui-desktop/target/forge.sh

# Step 5: Verify the build
echo "✅ Build complete! Verifying..."
if [ -f "forge-gui-desktop/target/forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "✓ forge-gui-desktop JAR built successfully"
    ls -lh forge-gui-desktop/target/forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar
else
    echo "❌ Error: forge-gui-desktop JAR not found!"
    exit 1
fi

if [ -d "forge-gui-desktop/target/res/cardsfolder" ]; then
    echo "✓ Resources copied successfully"
else
    echo "❌ Error: Resources not copied properly!"
    exit 1
fi

echo "🎉 Build and copy process complete!"
echo ""
echo "To run Forge:"
echo "  cd $FORGE_ROOT/forge-gui-desktop/target"
echo "  ./forge.sh"
echo ""
echo "To run the Claude integration demo:"
echo "  cd $SCRIPT_DIR"
echo "  ./run_simple_demo.sh"