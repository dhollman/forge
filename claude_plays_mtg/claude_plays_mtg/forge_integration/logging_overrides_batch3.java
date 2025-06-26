    // ==================== Batch 3: Method Overrides for Logging ====================
    
    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        System.out.println("[ClaudePlayerController] chooseManaFromPool called");
        System.out.println("[ClaudePlayerController]   Mana choices count: " + 
                          (manaChoices != null ? manaChoices.size() : 0));
        if (manaChoices != null && !manaChoices.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Available mana: " + manaChoices);
        }
        
        Mana result = super.chooseManaFromPool(manaChoices);
        
        System.out.println("[ClaudePlayerController] chooseManaFromPool returning: " + result);
        return result;
    }
    
    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, 
                                boolean isOptional) {
        System.out.println("[ClaudePlayerController] chooseSomeType called");
        System.out.println("[ClaudePlayerController]   Kind of type: " + kindOfType);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Valid types count: " + 
                          (validTypes != null ? validTypes.size() : 0));
        System.out.println("[ClaudePlayerController]   Optional: " + isOptional);
        if (validTypes != null && !validTypes.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Valid types: " + validTypes);
        }
        
        String result = super.chooseSomeType(kindOfType, sa, validTypes, isOptional);
        
        System.out.println("[ClaudePlayerController] chooseSomeType returning: " + result);
        return result;
    }
    
    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, 
                      ListMultimap<Object, Player> votes, Player forPlayer, boolean optional) {
        System.out.println("[ClaudePlayerController] vote called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Prompt: " + prompt);
        System.out.println("[ClaudePlayerController]   Options count: " + 
                          (options != null ? options.size() : 0));
        System.out.println("[ClaudePlayerController]   For player: " + 
                          (forPlayer != null ? forPlayer.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Optional: " + optional);
        if (options != null && !options.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Options: " + options);
        }
        
        Object result = super.vote(sa, prompt, options, votes, forPlayer, optional);
        
        System.out.println("[ClaudePlayerController] vote returning: " + result);
        return result;
    }
    
    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        System.out.println("[ClaudePlayerController] chooseSector called");
        System.out.println("[ClaudePlayerController]   Assignee: " + 
                          (assignee != null ? assignee.getName() : "null"));
        System.out.println("[ClaudePlayerController]   AI hint: " + ai);
        System.out.println("[ClaudePlayerController]   Sectors count: " + 
                          (sectors != null ? sectors.size() : 0));
        if (sectors != null && !sectors.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Available sectors: " + sectors);
        }
        
        String result = super.chooseSector(assignee, ai, sectors);
        
        System.out.println("[ClaudePlayerController] chooseSector returning: " + result);
        return result;
    }
    
    @Override
    public int chooseSprocket(Card assignee, boolean forceDifferent) {
        System.out.println("[ClaudePlayerController] chooseSprocket called");
        System.out.println("[ClaudePlayerController]   Assignee: " + 
                          (assignee != null ? assignee.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Force different: " + forceDifferent);
        
        int result = super.chooseSprocket(assignee, forceDifferent);
        
        System.out.println("[ClaudePlayerController] chooseSprocket returning: " + result);
        return result;
    }
    
    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(
            FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal,
            SpellAbility sa, String title, Player targetedPlayer, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseEntitiesForEffect called");
        System.out.println("[ClaudePlayerController]   Options count: " + 
                          (optionList != null ? optionList.size() : 0));
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Targeted player: " + 
                          (targetedPlayer != null ? targetedPlayer.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Params: " + params);
        
        List<T> result = super.chooseEntitiesForEffect(optionList, min, max, delayedReveal, 
                                                       sa, title, targetedPlayer, params);
        
        System.out.println("[ClaudePlayerController] chooseEntitiesForEffect returning: " + 
                          (result != null ? result.size() + " entities" : "null"));
        return result;
    }
    
    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, 
                                                            String title, int num, 
                                                            Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseSpellAbilitiesForEffect called");
        System.out.println("[ClaudePlayerController]   Spells count: " + 
                          (spells != null ? spells.size() : 0));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Number to choose: " + num);
        System.out.println("[ClaudePlayerController]   Params: " + params);
        
        List<SpellAbility> result = super.chooseSpellAbilitiesForEffect(spells, sa, title, num, params);
        
        System.out.println("[ClaudePlayerController] chooseSpellAbilitiesForEffect returning: " + 
                          (result != null ? result.size() + " abilities" : "null"));
        return result;
    }
    
    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, 
                                                   String title, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseSingleSpellForEffect called");
        System.out.println("[ClaudePlayerController]   Spells count: " + 
                          (spells != null ? spells.size() : 0));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Params: " + params);
        if (spells != null && !spells.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Available spells:");
            for (int i = 0; i < spells.size(); i++) {
                System.out.println("[ClaudePlayerController]     " + i + ": " + 
                                  spells.get(i).getDescription());
            }
        }
        
        SpellAbility result = super.chooseSingleSpellForEffect(spells, sa, title, params);
        
        System.out.println("[ClaudePlayerController] chooseSingleSpellForEffect returning: " + 
                          (result != null ? result.getDescription() : "null"));
        return result;
    }
    
    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // This is already overridden in the main class with extensive logging
        // Delegating to the existing implementation
        return super.chooseSpellAbilityToPlay();
    }
    
    // Additional methods that might be useful to override for comprehensive logging:
    
    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, 
                                              SpellAbility sa, CardCollection fetchList, 
                                              DelayedReveal delayedReveal, String selectPrompt, 
                                              boolean isOptional, Player decider) {
        System.out.println("[ClaudePlayerController] chooseSingleCardForZoneChange called");
        System.out.println("[ClaudePlayerController]   Destination: " + destination);
        System.out.println("[ClaudePlayerController]   Origins: " + origin);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Fetch list size: " + 
                          (fetchList != null ? fetchList.size() : 0));
        System.out.println("[ClaudePlayerController]   Prompt: " + selectPrompt);
        System.out.println("[ClaudePlayerController]   Optional: " + isOptional);
        System.out.println("[ClaudePlayerController]   Decider: " + 
                          (decider != null ? decider.getName() : "null"));
        
        Card result = super.chooseSingleCardForZoneChange(destination, origin, sa, fetchList, 
                                                          delayedReveal, selectPrompt, isOptional, decider);
        
        System.out.println("[ClaudePlayerController] chooseSingleCardForZoneChange returning: " + 
                          (result != null ? result.getName() : "null"));
        return result;
    }
    
    @Override
    public List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, 
                                               SpellAbility sa, CardCollection fetchList, 
                                               DelayedReveal delayedReveal, String selectPrompt, 
                                               int min, int max, Player decider) {
        System.out.println("[ClaudePlayerController] chooseCardsForZoneChange called");
        System.out.println("[ClaudePlayerController]   Destination: " + destination);
        System.out.println("[ClaudePlayerController]   Origins: " + origin);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Fetch list size: " + 
                          (fetchList != null ? fetchList.size() : 0));
        System.out.println("[ClaudePlayerController]   Prompt: " + selectPrompt);
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        System.out.println("[ClaudePlayerController]   Decider: " + 
                          (decider != null ? decider.getName() : "null"));
        
        List<Card> result = super.chooseCardsForZoneChange(destination, origin, sa, fetchList, 
                                                           delayedReveal, selectPrompt, min, max, decider);
        
        System.out.println("[ClaudePlayerController] chooseCardsForZoneChange returning: " + 
                          (result != null ? result.size() + " cards" : "null"));
        return result;
    }
    
    @Override
    public boolean payManaOptional(Card source, Cost c, SpellAbility sa, String prompt, 
                                  ManaPaymentPurpose purpose) {
        System.out.println("[ClaudePlayerController] payManaOptional called");
        System.out.println("[ClaudePlayerController]   Source: " + 
                          (source != null ? source.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Cost: " + c);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Prompt: " + prompt);
        System.out.println("[ClaudePlayerController]   Purpose: " + purpose);
        
        boolean result = super.payManaOptional(source, c, sa, prompt, purpose);
        
        System.out.println("[ClaudePlayerController] payManaOptional returning: " + result);
        return result;
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        System.out.println("[ClaudePlayerController] chooseNumber called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        
        int result = super.chooseNumber(sa, title, min, max);
        
        System.out.println("[ClaudePlayerController] chooseNumber returning: " + result);
        return result;
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer) {
        System.out.println("[ClaudePlayerController] chooseNumber (with values list) called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Values: " + values);
        System.out.println("[ClaudePlayerController]   Related player: " + 
                          (relatedPlayer != null ? relatedPlayer.getName() : "null"));
        
        int result = super.chooseNumber(sa, title, values, relatedPlayer);
        
        System.out.println("[ClaudePlayerController] chooseNumber (with values list) returning: " + result);
        return result;
    }