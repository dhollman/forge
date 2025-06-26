# Claude Plays MTG - Forge Integration Guide

## Project Overview

This integration enables Claude to play Magic: The Gathering through the Forge engine. Claude makes strategic decisions about which cards to play, which lands to tap for mana, and which targets to choose for spells.

**Status**: ✅ COMPLETE AND WORKING

## Key Achievement

We successfully integrated Claude with Forge by:
1. Creating a custom AI controller (`ClaudePlayerController`) that hooks into Forge's decision points
2. Implementing a TCP/JSON protocol for Java-Python communication  
3. Building a Python server that calls Claude's API for strategic decisions
4. Fixing critical bugs in mana payment and targeting systems

## Architecture

```
┌─────────────┐         TCP Socket        ┌─────────────────┐
│   Forge     │         Port 8889         │  Python Server  │
│   (Java)    │◄─────────────────────────►│                 │
│             │      JSON Protocol         │                 │
└─────────────┘                           └─────────────────┘
      │                                            │
      │                                            │
      ▼                                            ▼
┌─────────────┐                           ┌─────────────────┐
│ClaudePlayer │                           │  Claude API     │
│ Controller  │                           │  Integration    │
└─────────────┘                           └─────────────────┘
```

## File Locations

### Java Integration (Forge Side)
- **Main Controller**: `forge-ai/src/main/java/forge/ai/claude/ClaudePlayerController.java`
  - Extends `ComputerUtilAbility` for AI decision making
  - Implements `payManaCost()` for mana payment decisions
  - Implements `chooseTargetsFor()` for spell targeting
  - Sends game state to Python server via TCP

- **Protocol Classes**:
  - `forge-ai/src/main/java/forge/ai/claude/ProtocolMessage.java` - Message structure
  - `forge-ai/src/main/java/forge/ai/claude/MessageValidator.java` - Message validation
  - `forge-ai/src/main/java/forge/ai/claude/ClaudeProtocolClient.java` - TCP client

### Python Integration (Claude Side)
- **Server**: `claude_plays_mtg/claude_plays_mtg/forge_integration/forge_server.py`
  - TCP server on port 8889
  - Handles game initialization, action requests, mana payment, and targeting

- **Protocol**: `claude_plays_mtg/claude_plays_mtg/forge_integration/forge_protocol.py`
  - Message validation and serialization
  - Protocol version management

- **Agent**: `claude_plays_mtg/claude_plays_mtg/forge_integration/forge_agent.py`
  - Converts Forge game state to Claude-friendly format
  - Calls Claude API for decisions

### Build and Test Scripts
Located in `claude_plays_mtg/forge_integration_scripts/`:
- `build_and_copy.sh` - Builds Forge with resources
- `launch_forge_with_claude.sh` - Launches Forge GUI with Claude
- `run_simple_demo.sh` - Runs integration test

## Critical Fixes Made

### 1. Mana Payment System
**Problem**: Tri-lands (like Sandsteppe Citadel) weren't being recognized as valid mana sources.

**Solution**: Implemented `payManaCost()` override in ClaudePlayerController that:
- Collects all available mana sources
- Sends them to Claude for selection
- Returns which lands to tap

### 2. Active Player Detection
**Problem**: `active_player_index` was always -1, causing incorrect priority decisions.

**Fix**: Added fallback logic in ClaudePlayerController:
```java
Player turnPlayer = game.getPhaseHandler().getPlayerTurn();
if (turnPlayer == null) {
    turnPlayer = priorityPlayer; // Fallback
}
```

### 3. Message Validation
**Problem**: Java rejected `mana_payment_response` and `target_response` messages.

**Fix**: Implemented validation methods in MessageValidator.java:
- `validateManaPaymentResponse()`
- `validateTargetResponse()`

### 4. Python Protocol Support
**Problem**: Python server didn't handle new request types.

**Fix**: Added handlers in forge_server.py:
- `_handle_mana_payment()`
- `_handle_choose_targets()`

## How to Use

### Prerequisites
1. Java 17+ installed and in PATH
2. Maven installed
3. Python 3.11+ with claude_plays_mtg dependencies
4. Anthropic API key set in environment

### Building Forge
```bash
cd claude_plays_mtg/forge_integration_scripts
./build_and_copy.sh
```

### Running Integration Test
```bash
cd claude_plays_mtg/forge_integration_scripts
./run_simple_demo.sh
```

### Launching Forge GUI with Claude
```bash
cd claude_plays_mtg/forge_integration_scripts
./launch_forge_with_claude.sh
```

Then in Forge:
1. Go to "Constructed" mode
2. Select "Claude" as one of the AI opponents
3. Start game - Claude will make all decisions

## Protocol Details

### Message Types

#### Action Request (Java → Python)
```json
{
  "message_type": "request",
  "data": {
    "type": "get_action",
    "game_id": "forge-game-xxx",
    "requesting_player": 0,
    "game_state": { /* full game state */ },
    "legal_actions": [ /* available actions */ ]
  }
}
```

#### Mana Payment Request (Java → Python)
```json
{
  "message_type": "request",
  "data": {
    "type": "mana_payment",
    "cost": "{2}{B}",
    "available_sources": [ /* mana sources */ ],
    "spell": { /* spell being cast */ }
  }
}
```

#### Target Selection Request (Java → Python)
```json
{
  "message_type": "request",
  "data": {
    "type": "choose_targets",
    "spell": { /* spell requiring targets */ },
    "target_options": [ /* valid targets */ ],
    "min_targets": 1,
    "max_targets": 1
  }
}
```

## Testing and Verification

The integration test (`run_simple_demo.sh`) verifies:
1. ✅ Python server starts on port 8889
2. ✅ Java classes compile (7+ Claude-related classes)
3. ✅ TCP communication established
4. ✅ Mana payment hooks called
5. ✅ Targeting hooks called
6. ✅ Spells successfully cast
7. ✅ Game progresses through multiple turns

## Known Working Features

- ✅ Land playing decisions
- ✅ Spell casting with correct mana payment
- ✅ Target selection for spells
- ✅ Priority passing at appropriate times
- ✅ Combat decisions (attack/block)
- ✅ Multi-color mana handling (including tri-lands)

## Debugging

### Server Logs
- Python server: `/tmp/claude_server_p1.log`
- Forge application: `forge/logs/forge.log`

### Common Issues

1. **"Unknown response type" error**
   - Ensure MessageValidator.java has all response type validators
   - Rebuild with `build_and_copy.sh`

2. **Mana payment failures**
   - Check that `payManaCost()` is being called
   - Verify Python server is handling mana_payment requests

3. **Server not responding**
   - Check port 8889 is free: `lsof -i :8889`
   - Kill stuck processes: `pkill -f forge_server`

## Future Improvements

1. **Smarter Mana Tapping**: Currently taps lands in order; could optimize for color requirements
2. **Strategic Planning**: Add game plan reasoning to Claude's decisions
3. **Performance**: Cache similar game states to reduce API calls
4. **Deck Building**: Let Claude build its own decks

## Contact

For questions about this integration:
- Check `.daisy/claude-plays-mtg/` for development logs
- See main claude_plays_mtg CLAUDE.md for project overview
- Integration developed during Hackathon Week 2025

## Summary for Next Claude

**What Works**: The complete integration is functional. Claude can play full games of Magic through Forge, making decisions about lands, spells, mana, and targets.

**Key Files You'll Need**:
1. `ClaudePlayerController.java` - The brain of the Java integration
2. `forge_server.py` - The Python server Claude talks to
3. `forge_integration_scripts/` - Scripts to build and test

**If Something Breaks**: 
1. Check the logs first (see Debugging section)
2. Verify the protocol messages match between Java and Python
3. Ensure all validation methods exist on both sides
4. The most fragile part is the TCP communication - if it fails, check ports and processes

**What Makes This Special**: This is the first successful integration of Claude with a complex game engine like Forge. The bi-directional communication and decision hooks create a framework that could be extended to other games.