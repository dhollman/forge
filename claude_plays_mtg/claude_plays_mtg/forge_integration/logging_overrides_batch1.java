    // ==================== Batch 1: Method Overrides for Logging ====================
    
    @Override
    public boolean pilotsNonAggroDeck() {
        System.out.println("[ClaudePlayerController] pilotsNonAggroDeck called");
        boolean result = super.pilotsNonAggroDeck();
        System.out.println("[ClaudePlayerController] pilotsNonAggroDeck returning: " + result);
        return result;
    }
    
    @Override
    public void setupAutoProfile(Deck deck) {
        System.out.println("[ClaudePlayerController] setupAutoProfile called with deck: " + 
                          (deck != null ? deck.getName() : "null"));
        super.setupAutoProfile(deck);
        System.out.println("[ClaudePlayerController] setupAutoProfile completed");
    }
    
    @Override
    public void allowCheatShuffle(boolean value) {
        System.out.println("[ClaudePlayerController] allowCheatShuffle called with value: " + value);
        super.allowCheatShuffle(value);
        System.out.println("[ClaudePlayerController] allowCheatShuffle completed");
    }
    
    @Override
    public void setUseSimulation(boolean value) {
        System.out.println("[ClaudePlayerController] setUseSimulation called with value: " + value);
        super.setUseSimulation(value);
        System.out.println("[ClaudePlayerController] setUseSimulation completed");
    }
    
    @Override
    public ComputerUtilAI getAi() {
        System.out.println("[ClaudePlayerController] getAi called");
        ComputerUtilAI result = super.getAi();
        System.out.println("[ClaudePlayerController] getAi returning: " + 
                          (result != null ? result.getClass().getSimpleName() : "null"));
        return result;
    }
    
    @Override
    public boolean isAI() {
        System.out.println("[ClaudePlayerController] isAI called");
        boolean result = super.isAI();
        System.out.println("[ClaudePlayerController] isAI returning: " + result);
        return result;
    }
    
    @Override
    public void sideboard(Deck deck, GameType gameType, String message) {
        System.out.println("[ClaudePlayerController] sideboard called");
        System.out.println("[ClaudePlayerController]   Deck: " + (deck != null ? deck.getName() : "null"));
        System.out.println("[ClaudePlayerController]   GameType: " + gameType);
        System.out.println("[ClaudePlayerController]   Message: " + message);
        super.sideboard(deck, gameType, message);
        System.out.println("[ClaudePlayerController] sideboard completed");
    }
    
    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, 
                                                  CardCollectionView remaining, int damageDealt, 
                                                  GameEntity defender, boolean overrideOrder) {
        System.out.println("[ClaudePlayerController] assignCombatDamage called");
        System.out.println("[ClaudePlayerController]   Attacker: " + 
                          (attacker != null ? attacker.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Blockers count: " + 
                          (blockers != null ? blockers.size() : 0));
        System.out.println("[ClaudePlayerController]   Damage to assign: " + damageDealt);
        System.out.println("[ClaudePlayerController]   Override order: " + overrideOrder);
        
        Map<Card, Integer> result = super.assignCombatDamage(attacker, blockers, remaining, 
                                                             damageDealt, defender, overrideOrder);
        
        System.out.println("[ClaudePlayerController] assignCombatDamage returning damage assignments: " + 
                          (result != null ? result.size() + " assignments" : "null"));
        return result;
    }
    
    @Override
    public Integer divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        System.out.println("[ClaudePlayerController] divideShield called");
        System.out.println("[ClaudePlayerController]   Effect source: " + 
                          (effectSource != null ? effectSource.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Affected entities: " + 
                          (affected != null ? affected.size() : 0));
        System.out.println("[ClaudePlayerController]   Shield amount: " + shieldAmount);
        
        Integer result = super.divideShield(effectSource, affected, shieldAmount);
        
        System.out.println("[ClaudePlayerController] divideShield returning: " + result);
        return result;
    }
    
    @Override
    public String specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        System.out.println("[ClaudePlayerController] specifyManaCombo called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   ColorSet: " + colorSet);
        System.out.println("[ClaudePlayerController]   Mana amount: " + manaAmount);
        System.out.println("[ClaudePlayerController]   Different: " + different);
        
        String result = super.specifyManaCombo(sa, colorSet, manaAmount, different);
        
        System.out.println("[ClaudePlayerController] specifyManaCombo returning: " + result);
        return result;
    }
    
    @Override
    public String announceRequirements(SpellAbility ability, String announce) {
        System.out.println("[ClaudePlayerController] announceRequirements called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (ability != null ? ability.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Announce prompt: " + announce);
        
        String result = super.announceRequirements(ability, announce);
        
        System.out.println("[ClaudePlayerController] announceRequirements returning: " + result);
        return result;
    }
    
    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, 
                                                        CardCollectionView validTargets, String message) {
        System.out.println("[ClaudePlayerController] choosePermanentsToDestroy called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        System.out.println("[ClaudePlayerController]   Valid targets count: " + 
                          (validTargets != null ? validTargets.size() : 0));
        System.out.println("[ClaudePlayerController]   Message: " + message);
        
        CardCollectionView result = super.choosePermanentsToDestroy(sa, min, max, validTargets, message);
        
        System.out.println("[ClaudePlayerController] choosePermanentsToDestroy returning: " + 
                          (result != null ? result.size() + " permanents" : "null"));
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, 
                                                   String title, int min, int max, boolean isOptional, 
                                                   Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseCardsForEffect called");
        System.out.println("[ClaudePlayerController]   Source list size: " + 
                          (sourceList != null ? sourceList.size() : 0));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        System.out.println("[ClaudePlayerController]   Optional: " + isOptional);
        System.out.println("[ClaudePlayerController]   Params: " + params);
        
        CardCollectionView result = super.chooseCardsForEffect(sourceList, sa, title, min, max, 
                                                               isOptional, params);
        
        System.out.println("[ClaudePlayerController] chooseCardsForEffect returning: " + 
                          (result != null ? result.size() + " cards" : "null"));
        return result;
    }
    
    @Override
    public List<Card> chooseContraptionsToCrank(List<Card> contraptions) {
        System.out.println("[ClaudePlayerController] chooseContraptionsToCrank called");
        System.out.println("[ClaudePlayerController]   Contraptions count: " + 
                          (contraptions != null ? contraptions.size() : 0));
        
        List<Card> result = super.chooseContraptionsToCrank(contraptions);
        
        System.out.println("[ClaudePlayerController] chooseContraptionsToCrank returning: " + 
                          (result != null ? result.size() + " contraptions" : "null"));
        return result;
    }
    
    @Override
    public void helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested) {
        System.out.println("[ClaudePlayerController] helpPayForAssistSpell called");
        System.out.println("[ClaudePlayerController]   Cost: " + cost);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Max: " + max + ", Requested: " + requested);
        
        super.helpPayForAssistSpell(cost, sa, max, requested);
        
        System.out.println("[ClaudePlayerController] helpPayForAssistSpell completed");
    }
    
    @Override
    public int choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, 
                                           String title, int max) {
        System.out.println("[ClaudePlayerController] choosePlayerToAssistPayment called");
        System.out.println("[ClaudePlayerController]   Options count: " + 
                          (optionList != null ? optionList.size() : 0));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Max: " + max);
        
        int result = super.choosePlayerToAssistPayment(optionList, sa, title, max);
        
        System.out.println("[ClaudePlayerController] choosePlayerToAssistPayment returning: " + result);
        return result;
    }
    
    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, 
                                                                 DelayedReveal delayedReveal, 
                                                                 SpellAbility sa, String title, 
                                                                 boolean isOptional, Player targetedPlayer, 
                                                                 Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseSingleEntityForEffect called");
        System.out.println("[ClaudePlayerController]   Options count: " + 
                          (optionList != null ? optionList.size() : 0));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Optional: " + isOptional);
        System.out.println("[ClaudePlayerController]   Targeted player: " + 
                          (targetedPlayer != null ? targetedPlayer.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Params: " + params);
        
        T result = super.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title, 
                                                     isOptional, targetedPlayer, params);
        
        System.out.println("[ClaudePlayerController] chooseSingleEntityForEffect returning: " + 
                          (result != null ? result.toString() : "null"));
        return result;
    }
    
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, 
                                List<String> options, Card cardToShow, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] confirmAction called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Mode: " + mode);
        System.out.println("[ClaudePlayerController]   Message: " + message);
        System.out.println("[ClaudePlayerController]   Options: " + options);
        System.out.println("[ClaudePlayerController]   Card to show: " + 
                          (cardToShow != null ? cardToShow.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Params: " + params);
        
        boolean result = super.confirmAction(sa, mode, message, options, cardToShow, params);
        
        System.out.println("[ClaudePlayerController] confirmAction returning: " + result);
        return result;
    }
    
    @Override
    public int confirmBidAction(SpellAbility sa, PlayerActionConfirmMode mode, String string, 
                               int min, int max, Player winner) {
        System.out.println("[ClaudePlayerController] confirmBidAction called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Mode: " + mode);
        System.out.println("[ClaudePlayerController]   String: " + string);
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        System.out.println("[ClaudePlayerController]   Current winner: " + 
                          (winner != null ? winner.getName() : "null"));
        
        int result = super.confirmBidAction(sa, mode, string, min, max, winner);
        
        System.out.println("[ClaudePlayerController] confirmBidAction returning: " + result);
        return result;
    }