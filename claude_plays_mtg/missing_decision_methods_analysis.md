# PlayerControllerAi Decision Methods Analysis

This document systematically analyzes all gameplay decision methods in PlayerControllerAi.java and identifies which ones are not overridden in ClaudePlayerController.java, grouped by category.

## Overview

ClaudePlayerController currently overrides:
- `chooseSpellAbilityToPlay()` - Main phase spell/ability selection
- `getAbilityToPlay()` - Generic ability selection (delegates to parent for single option)
- `payManaCost()` - Mana payment (delegates to parent with logging)

## Critical Missing Methods by Category

### 1. **Combat Decisions** ⚔️
These are CRITICAL for Claude to play Magic effectively:

- **`declareAttackers(Player attacker, Combat combat)`** - Choose which creatures attack
- **`declareBlockers(Player defender, Combat combat)`** - Choose how to block
- **`assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder)`** - Assign damage order
- **`orderBlockers(Card attacker, CardCollection blockers)`** - Order multiple blockers
- **`orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers)`** - Order a single blocker
- **`orderAttackers(Card blocker, CardCollection attackers)`** - Order attackers when blocking
- **`exertAttackers(List<Card> attackers)`** - Choose which attackers to exert
- **`enlistAttackers(List<Card> attackers)`** - Choose which creatures to enlist

### 2. **Hand Management** 🃏
Essential for resource management:

- **`chooseCardsToDiscardFrom(Player p, SpellAbility sa, CardCollection validCards, int min, int max)`** - Choose cards to discard
- **`chooseCardsToDiscardToMaximumHandSize(int numDiscard)`** - Discard at end of turn
- **`chooseCardsToDiscardUnlessType(int num, CardCollectionView hand, String uType, SpellAbility sa)`** - Conditional discard
- **`chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid)`** - Reveal cards from hand

### 3. **Mulligan Decisions** 🔄
Important for game start:

- **`mulliganKeepHand(Player firstPlayer, int cardsToReturn)`** - Decide whether to keep opening hand
- **`londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn)`** - Choose cards to put back
- **`confirmMulliganScry(Player p)`** - Confirm mulligan scry

### 4. **Card Selection and Ordering** 📚
Critical for many effects:

- **`chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params)`** - Generic card selection
- **`chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer, Map<String, Object> params)`** - Choose single entity
- **`chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player targetedPlayer, Map<String, Object> params)`** - Choose multiple entities
- **`orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source)`** - Order cards going to a zone
- **`arrangeForScry(CardCollection topN)`** - Scry decision (top/bottom)
- **`arrangeForSurveil(CardCollection topN)`** - Surveil decision
- **`willPutCardOnTop(Card c)`** - Clash and similar effects

### 5. **Modal and Choice Effects** 🎯
Essential for flexible spells:

- **`chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat)`** - Choose modes for modal spells
- **`chooseOptionalCosts(SpellAbility chosen, List<OptionalCostValue> optionalCostValues)`** - Kicker, buyback, etc.
- **`chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params)`** - Choose multiple spell abilities
- **`chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params)`** - Choose single spell ability

### 6. **Targeting and Sacrifice** 🎯
Critical for interaction:

- **`choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message)`** - Choose what to sacrifice
- **`choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message)`** - Choose what to destroy
- **`chooseTarget(SpellAbility saSrc, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets)`** - Retarget spells

### 7. **Color and Type Choices** 🎨
Important for many effects:

- **`chooseColor(String message, SpellAbility sa, ColorSet colors)`** - Choose a color
- **`chooseColors(String message, SpellAbility sa, int min, int max, List<String> options)`** - Choose multiple colors
- **`chooseColorAllowColorless(String message, Card card, ColorSet colors)`** - Choose color or colorless
- **`chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional)`** - Choose card type, creature type, etc.
- **`chooseCardName(SpellAbility sa, List<ICardFace> faces, String message)`** - Name a card
- **`chooseProtectionType(String string, SpellAbility sa, List<String> choices)`** - Choose protection type

### 8. **Numeric Choices** 🔢
Common in many effects:

- **`chooseNumber(SpellAbility sa, String title, int min, int max)`** - Choose a number
- **`chooseNumber(SpellAbility sa, String string, int min, int max, Map<String, Object> params)`** - Choose with params
- **`chooseNumber(SpellAbility sa, String title, List<Integer> options, Player relatedPlayer)`** - Choose from list
- **`announceRequirements(SpellAbility ability, String announce)`** - X costs, etc.
- **`chooseNumberForCostReduction(SpellAbility sa, int min, int max)`** - Cost reduction choices

### 9. **Replacement Effects and Triggers** 🔄
Very important for proper play:

- **`confirmTrigger(WrappedAbility wrapper)`** - Currently overridden but delegates to parent
- **`confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question)`** - Choose replacement effects
- **`chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers)`** - Order replacement effects
- **`orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs)`** - Order simultaneous triggers
- **`playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory)`** - Play triggered abilities

### 10. **Binary Choices** ✅❌
Common decision points:

- **`chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal)`** - Various binary choices
- **`chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Map<String, Object> params)`** - Binary with params
- **`confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params)`** - Confirm actions
- **`confirmPayment(CostPart costPart, String prompt, SpellAbility sa)`** - Confirm cost payments

### 11. **Zone Change Decisions** 📍
Important for tutoring and similar effects:

- **`chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider)`** - Tutor effects
- **`chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider)`** - Multiple card zone changes

### 12. **Special Card Mechanics** 🎲
Unique mechanics:

- **`chooseCardsToDelve(int genericAmount, CardCollection grave)`** - Delve mechanic
- **`chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise)`** - Convoke/Improvise
- **`chooseCardsForSplice(SpellAbility sa, List<Card> cards)`** - Splice onto Arcane
- **`divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount)`** - Shield counters
- **`specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different)`** - Specific mana combinations

### 13. **Game Start and Sideboarding** 🎮
Pre-game decisions:

- **`sideboard(Deck deck, GameType gameType, String message)`** - Sideboard decisions
- **`chooseStartingPlayer(boolean isFirstgame)`** - Choose play/draw
- **`chooseStartingHand(List<PlayerZone> zones)`** - Multiple starting hands (Vanguard)
- **`chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand)`** - Leylines, etc.

### 14. **Other Important Decisions** 🎯
Miscellaneous but important:

- **`vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional)`** - Council voting
- **`chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp)`** - Fact or Fiction effects
- **`chooseDungeon(Player ai, List<PaperCard> dungeonCards, String message)`** - Dungeon selection
- **`chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params)`** - Counter type selection
- **`chooseKeywordForPump(List<String> options, SpellAbility sa, String prompt, Card tgtCard)`** - Keyword granting

## Priority Recommendations

### Must Have (Core Gameplay):
1. **Combat decisions** - `declareAttackers`, `declareBlockers`, `assignCombatDamage`
2. **Targeting** - `chooseTargetsFor` (via `chooseSpellAbilityToPlay` enhancements)
3. **Basic card selection** - `chooseCardsForEffect`, `chooseSingleEntityForEffect`
4. **Discard decisions** - `chooseCardsToDiscardFrom`, `chooseCardsToDiscardToMaximumHandSize`
5. **Mulligan** - `mulliganKeepHand`, `londonMulliganReturnCards`

### Should Have (Common Situations):
1. **Modal spells** - `chooseModeForAbility`
2. **Sacrifice/destroy** - `choosePermanentsToSacrifice`, `choosePermanentsToDestroy`
3. **Color/type choices** - `chooseColor`, `chooseSomeType`
4. **Triggers** - `confirmTrigger` (proper implementation), `orderAndPlaySimultaneousSa`
5. **Scry/Surveil** - `arrangeForScry`, `arrangeForSurveil`

### Nice to Have (Advanced):
1. **Optional costs** - `chooseOptionalCosts`
2. **Special mechanics** - `chooseCardsToDelve`, `chooseCardsForConvokeOrImprovise`
3. **Replacement effects** - `chooseSingleReplacementEffect`
4. **Voting** - `vote`
5. **Sideboarding** - `sideboard`

## Implementation Notes

1. **Many methods delegate to AI subcomponents** - We'll need to understand these delegation patterns
2. **Some methods have AI logic parameters** - Look for `sa.getParam("AILogic")` usage
3. **Context is critical** - Many decisions depend on game state, phase, and what's on stack
4. **Error handling** - All overrides should gracefully fall back to parent implementation
5. **Performance** - Combat and targeting decisions happen frequently and need to be fast

## Next Steps

1. Start with combat decisions as they're most critical
2. Add basic targeting support to existing `chooseSpellAbilityToPlay`
3. Implement card selection methods for common effects
4. Add mulligan support for better game starts
5. Gradually add other methods based on gameplay testing needs