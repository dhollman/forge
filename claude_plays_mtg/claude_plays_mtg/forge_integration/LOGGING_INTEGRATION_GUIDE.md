# Claude Player Controller - Logging Integration Guide

## Overview
This guide explains how to integrate comprehensive logging into ClaudePlayerController.java to debug which methods are being called during gameplay.

## Files Generated
1. `logging_overrides_batch1.java` - First batch of method overrides (20 methods)
2. `logging_overrides_batch2.java` - Second batch of method overrides (20 methods)  
3. `logging_overrides_batch3.java` - Final batch of method overrides and additional utility methods
4. `logging_overrides_imports.java` - Required import statements

## Integration Steps

### 1. Add Import Statements
First, ensure all necessary imports are present at the top of ClaudePlayerController.java. Add any missing imports from `logging_overrides_imports.java`.

### 2. Add Method Overrides
Copy the method overrides from the three batch files and paste them into ClaudePlayerController.java, preferably after the existing overridden methods and before the helper methods section.

### 3. Methods Already Implemented
The following methods are already overridden in ClaudePlayerController.java and have logging:
- `chooseSpellAbilityToPlay()`
- `getAbilityToPlay()`
- `payManaCost()`
- `chooseTargetsFor()`
- `declareAttackers()`
- `declareBlockers()`
- `chooseCardsToDiscardToMaximumHandSize()`
- `choosePermanentsToSacrifice()`

### 4. Key Logging Patterns
Each override follows this pattern:
```java
@Override
public ReturnType methodName(Parameters...) {
    System.out.println("[ClaudePlayerController] methodName called");
    System.out.println("[ClaudePlayerController]   Param1: " + param1);
    // ... log other parameters
    
    ReturnType result = super.methodName(parameters...);
    
    System.out.println("[ClaudePlayerController] methodName returning: " + result);
    return result;
}
```

### 5. Testing the Integration
After adding the logging overrides:
1. Compile the project: `mvn clean compile`
2. Run a test game
3. Monitor the console output for `[ClaudePlayerController]` tags
4. Look for patterns in which methods are called and in what order

### 6. Debugging Tips
- Methods called frequently: `isAI()`, `getAi()`
- Combat-related: `assignCombatDamage()`, `orderBlockers()`, `orderAttackers()`
- Choice methods: `chooseSingleEntityForEffect()`, `chooseCardsForEffect()`
- Mana-related: `chooseManaFromPool()`, `payManaOptional()`
- Zone changes: `chooseSingleCardForZoneChange()`, `chooseCardsForZoneChange()`

### 7. Performance Considerations
The logging is verbose and may impact performance. Consider:
- Adding a debug flag to enable/disable logging
- Using a proper logging framework instead of System.out.println
- Filtering which methods to log based on your debugging needs

### 8. Next Steps
Once you identify which methods are being called:
1. Focus on implementing Claude integration for the most critical methods
2. Start with simple decisions and gradually add complexity
3. Consider the order of method calls to understand the game flow

## Method Categories

### Configuration Methods
- `pilotsNonAggroDeck()`
- `setupAutoProfile()`
- `allowCheatShuffle()`
- `setUseSimulation()`

### Decision Methods
- `confirmAction()`
- `confirmBidAction()`
- `confirmStaticApplication()`
- `confirmTrigger()`
- `confirmPayment()`
- `confirmReplacementEffect()`

### Combat Methods
- `assignCombatDamage()`
- `orderBlockers()`
- `orderAttackers()`
- `exertAttackers()`
- `enlistAttackers()`

### Card Selection Methods
- `chooseCardsForEffect()`
- `chooseSingleEntityForEffect()`
- `chooseEntitiesForEffect()`
- `choosePermanentsToDestroy()`
- `chooseCardsToDiscardFrom()`

### Utility Methods
- `reveal()`
- `arrangeForScry()`
- `arrangeForSurveil()`
- `willPutCardOnTop()`
- `orderMoveToZoneList()`

### Resource Management Methods
- `chooseManaFromPool()`
- `specifyManaCombo()`
- `divideShield()`
- `payManaOptional()`

### Special Game Mechanics
- `vote()`
- `chooseSector()`
- `chooseSprocket()`
- `chooseContraptionsToCrank()`
- `helpPayForAssistSpell()`
- `choosePlayerToAssistPayment()`

This comprehensive logging will help you understand the full lifecycle of AI decisions in Forge and identify the best integration points for Claude's strategic intelligence.