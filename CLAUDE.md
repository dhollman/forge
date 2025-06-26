# Forge Build Instructions for macOS

This document contains build instructions for Forge on macOS, specifically for the claude-plays-mtg hack week project.

## Prerequisites

### Java Installation
Forge requires Java 17 or later. Install via Homebrew:

```bash
brew install openjdk@17  # Or use existing openjdk@24
```

**Important**: Homebrew installs Java but doesn't add it to PATH. You need to either:

1. Add to PATH (recommended):
   ```bash
   echo 'export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

2. Create an alias:
   ```bash
   echo 'alias java="/opt/homebrew/opt/openjdk/bin/java"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. Create system-wide symlink:
   ```bash
   sudo ln -sfn /opt/homebrew/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk
   ```

### Maven
Ensure Maven is installed:
```bash
brew install maven
```

## Building Forge (Desktop Only)

The standard build includes mobile modules (Android/iOS) which are problematic on macOS. Use this modified build process:

### 1. Disable launch4j Plugin
The launch4j plugin tries to build Windows executables and fails on macOS. Edit `forge-gui-desktop/pom.xml` and comment out the launch4j plugin section (lines 73-130).

### 2. Build Command
Run this command from the forge root directory:

```bash
mvn clean package -pl "!forge-gui-mobile,!forge-gui-mobile-dev,!forge-gui-android,!forge-gui-ios,!adventure-editor,!forge-installer" -DskipTests
```

This excludes:
- `forge-gui-mobile` - Mobile GUI logic
- `forge-gui-mobile-dev` - Mobile development desktop runner
- `forge-gui-android` - Android build
- `forge-gui-ios` - iOS build  
- `adventure-editor` - Depends on mobile modules
- `forge-installer` - Depends on mobile modules

### 3. Copy Resources
After building, copy the game resources to the target directory:

```bash
cp -r forge-gui/res forge-gui-desktop/target/
```

## Running Forge

### Option 1: Using the launch script
```bash
cd forge-gui-desktop/target
./forge.sh
```

### Option 2: Direct Java command
```bash
java -Xmx4096m -jar forge-gui-desktop/target/forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar
```

Note: Run from the `target` directory to ensure resources are found.

### Option 3: With full Java path (if PATH not set)
```bash
cd forge-gui-desktop/target
/opt/homebrew/opt/openjdk/bin/java -Xmx4096m -jar forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar
```

## Build Output

The build creates:
- `forge-gui-desktop/target/forge-gui-desktop-2.0.05-SNAPSHOT-jar-with-dependencies.jar` (~38MB) - Main executable
- `forge-gui-desktop/target/forge.sh` - Launch script for Unix/macOS
- `forge-gui-desktop/target/forge.command` - Launch script for macOS

## Known Issues

1. **FSkin Warning**: "FSkin > can't find skins directory!" - This warning appears but doesn't prevent the game from running
2. **Unsupported Cards**: Some Secret Lair Drop (SLD) cards show as unsupported
3. **Unsafe Warnings**: Java 24 shows deprecation warnings for Unsafe operations - these can be ignored

## For claude-plays-mtg Project

This build provides the desktop GUI version of Forge suitable for:
- Manual playtesting and verification of game mechanics
- Understanding MTG rules implementation
- Debugging AI behavior and game state
- Reference implementation for MTG comprehensive rules

### Integration with claude-plays-mtg

**🎉 INTEGRATION COMPLETE!** Claude can now play Magic through Forge!

The claude-plays-mtg project (located at `../claude_plays_mtg/`) is designed to have Claude play Magic: The Gathering as both judge and players. Forge now serves as:

1. **Rules Reference**: Forge implements the comprehensive MTG rules that claude-plays-mtg can reference
2. **Validation Tool**: Run games in Forge to verify claude-plays-mtg's game state handling
3. **AI Comparison**: Compare Claude's play decisions with Forge's built-in AI
4. **Test Data Source**: Use Forge's deck formats and card pools for testing

### Key Directories
- Game resources: `forge-gui/res/`
- Card definitions: `forge-gui/res/cardsfolder/` (30,000+ cards)
- AI definitions: `forge-gui/res/ai/`
- Deck formats: `forge-gui/res/formats/`
- Example decks: `forge-gui/res/decks/`

### API Entry Points for Integration
- Main GUI class: `forge.view.Main`
- Game engine: `forge-game` module
  - `forge.game.Game` - Core game state
  - `forge.game.player.Player` - Player representation
  - `forge.game.card.Card` - Card instances
- AI system: `forge-ai` module
  - `forge.ai.ComputerUtil` - AI decision making
  - `forge.ai.AiController` - AI player controller
- Rules engine: `forge-core` module
  - `forge.card.CardDb` - Card database
  - `forge.deck.Deck` - Deck representation

### Running Side-by-Side

To run both Forge and claude-plays-mtg for comparison:

1. **Forge GUI**:
   ```bash
   cd forge-gui-desktop/target
   ./forge.sh
   ```

2. **claude-plays-mtg Web UI**:
   ```bash
   cd ../claude_plays_mtg
   python run_web_server.py
   ```

This allows you to:
- Play the same matchup in both systems
- Compare rule interpretations
- Validate game states
- Debug discrepancies

### Useful Forge Features for Development

1. **Game State Export**: Forge can export game states for analysis
2. **AI Debugging**: Enable AI logging to understand decision-making
3. **Rules Enforcement**: Strict rules checking helps validate claude-plays-mtg
4. **Deck Import/Export**: Share deck lists between systems

### Notes

- Forge uses Java 17+ while claude-plays-mtg uses Python
- Both projects use the same card data source (AtomicCards.json)
- Forge's comprehensive rules implementation can serve as a reference for edge cases
- The `forge-gui/res/cardsfolder/` directory contains scripted implementations for all cards
- The claude-plays-mtg project considers Forge as one of two implementation approaches (the other being a Claude-as-judge system)
- See `claude_plays_mtg/docs/design.md` for detailed integration plans with Forge

### 🚀 Claude Integration Status

**WORKING FEATURES:**
- ✅ ClaudePlayerController integrated into forge-ai module
- ✅ TCP/JSON protocol for Java-Python communication (port 8889)
- ✅ Mana payment decisions - Claude chooses which lands to tap
- ✅ Spell targeting - Claude selects targets for spells
- ✅ Full game flow - games run to completion with Claude making all decisions

**HOW TO USE CLAUDE WITH FORGE:**

1. **Build Forge with Claude integration:**
   ```bash
   cd claude_plays_mtg/forge_integration_scripts
   ./build_and_copy.sh
   ```

2. **Run integration test:**
   ```bash
   cd claude_plays_mtg/forge_integration_scripts
   ./run_simple_demo.sh
   ```

3. **Launch Forge GUI with Claude:**
   ```bash
   cd claude_plays_mtg/forge_integration_scripts
   ./launch_forge_with_claude.sh
   ```
   Then select "Claude" as an AI opponent in Constructed mode.

**INTEGRATION FILES:**
- Java: `forge-ai/src/main/java/forge/ai/claude/` - All Claude integration classes
- Python: `claude_plays_mtg/claude_plays_mtg/forge_integration/` - Server and protocol
- Scripts: `claude_plays_mtg/forge_integration_scripts/` - Build and test scripts
- Docs: `claude_plays_mtg/CLAUDE_FORGE_INTEGRATION.md` - Complete integration guide

### Development Tips

1. **Start Forge First**: Launch Forge to understand the game flow before testing claude-plays-mtg
2. **Use Same Decks**: Copy deck lists from Forge to ensure consistent testing
3. **Enable Logging**: Both systems support detailed logging for debugging
4. **Monitor Resources**: Forge requires 4GB heap; claude-plays-mtg requires API credits
5. **Check Integration**: See `claude_plays_mtg/CLAUDE_FORGE_INTEGRATION.md` for details