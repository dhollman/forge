    // ==================== Batch 2: Method Overrides for Logging ====================
    
    @Override
    public boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, 
                                           String message, String logic) {
        System.out.println("[ClaudePlayerController] confirmStaticApplication called");
        System.out.println("[ClaudePlayerController]   Host card: " + 
                          (hostCard != null ? hostCard.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Mode: " + mode);
        System.out.println("[ClaudePlayerController]   Message: " + message);
        System.out.println("[ClaudePlayerController]   Logic: " + logic);
        
        boolean result = super.confirmStaticApplication(hostCard, mode, message, logic);
        
        System.out.println("[ClaudePlayerController] confirmStaticApplication returning: " + result);
        return result;
    }
    
    @Override
    public boolean confirmTrigger(WrappedAbility wrapper) {
        System.out.println("[ClaudePlayerController] confirmTrigger called");
        System.out.println("[ClaudePlayerController]   Wrapped ability: " + 
                          (wrapper != null ? wrapper.toString() : "null"));
        
        boolean result = super.confirmTrigger(wrapper);
        
        System.out.println("[ClaudePlayerController] confirmTrigger returning: " + result);
        return result;
    }
    
    @Override
    public boolean confirmPayment(CostPart costPart, String prompt, SpellAbility sa) {
        System.out.println("[ClaudePlayerController] confirmPayment called");
        System.out.println("[ClaudePlayerController]   Cost part: " + 
                          (costPart != null ? costPart.toString() : "null"));
        System.out.println("[ClaudePlayerController]   Prompt: " + prompt);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        
        boolean result = super.confirmPayment(costPart, prompt, sa);
        
        System.out.println("[ClaudePlayerController] confirmPayment returning: " + result);
        return result;
    }
    
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, 
                                           GameEntity affected, String question) {
        System.out.println("[ClaudePlayerController] confirmReplacementEffect called");
        System.out.println("[ClaudePlayerController]   Replacement effect: " + 
                          (replacementEffect != null ? replacementEffect.toString() : "null"));
        System.out.println("[ClaudePlayerController]   Effect SA: " + 
                          (effectSA != null ? effectSA.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Affected: " + 
                          (affected != null ? affected.toString() : "null"));
        System.out.println("[ClaudePlayerController]   Question: " + question);
        
        boolean result = super.confirmReplacementEffect(replacementEffect, effectSA, affected, question);
        
        System.out.println("[ClaudePlayerController] confirmReplacementEffect returning: " + result);
        return result;
    }
    
    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        System.out.println("[ClaudePlayerController] exertAttackers called");
        System.out.println("[ClaudePlayerController]   Attackers count: " + 
                          (attackers != null ? attackers.size() : 0));
        if (attackers != null && !attackers.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Attackers: " + attackers);
        }
        
        List<Card> result = super.exertAttackers(attackers);
        
        System.out.println("[ClaudePlayerController] exertAttackers returning: " + 
                          (result != null ? result.size() + " cards to exert" : "null"));
        return result;
    }
    
    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        System.out.println("[ClaudePlayerController] enlistAttackers called");
        System.out.println("[ClaudePlayerController]   Attackers count: " + 
                          (attackers != null ? attackers.size() : 0));
        if (attackers != null && !attackers.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Attackers: " + attackers);
        }
        
        List<Card> result = super.enlistAttackers(attackers);
        
        System.out.println("[ClaudePlayerController] enlistAttackers returning: " + 
                          (result != null ? result.size() + " cards to enlist" : "null"));
        return result;
    }
    
    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        System.out.println("[ClaudePlayerController] orderBlockers called");
        System.out.println("[ClaudePlayerController]   Attacker: " + 
                          (attacker != null ? attacker.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Blockers count: " + 
                          (blockers != null ? blockers.size() : 0));
        if (blockers != null && !blockers.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Blockers: " + blockers);
        }
        
        CardCollection result = super.orderBlockers(attacker, blockers);
        
        System.out.println("[ClaudePlayerController] orderBlockers returning ordered list of " + 
                          (result != null ? result.size() + " blockers" : "null"));
        return result;
    }
    
    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        System.out.println("[ClaudePlayerController] orderBlocker called");
        System.out.println("[ClaudePlayerController]   Attacker: " + 
                          (attacker != null ? attacker.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Blocker: " + 
                          (blocker != null ? blocker.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Old blockers count: " + 
                          (oldBlockers != null ? oldBlockers.size() : 0));
        
        CardCollection result = super.orderBlocker(attacker, blocker, oldBlockers);
        
        System.out.println("[ClaudePlayerController] orderBlocker returning ordered list of " + 
                          (result != null ? result.size() + " blockers" : "null"));
        return result;
    }
    
    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        System.out.println("[ClaudePlayerController] orderAttackers called");
        System.out.println("[ClaudePlayerController]   Blocker: " + 
                          (blocker != null ? blocker.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Attackers count: " + 
                          (attackers != null ? attackers.size() : 0));
        if (attackers != null && !attackers.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Attackers: " + attackers);
        }
        
        CardCollection result = super.orderAttackers(blocker, attackers);
        
        System.out.println("[ClaudePlayerController] orderAttackers returning ordered list of " + 
                          (result != null ? result.size() + " attackers" : "null"));
        return result;
    }
    
    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, 
                      String messagePrefix, boolean addSuffix) {
        System.out.println("[ClaudePlayerController] reveal(CardCollectionView) called");
        System.out.println("[ClaudePlayerController]   Cards count: " + 
                          (cards != null ? cards.size() : 0));
        System.out.println("[ClaudePlayerController]   Zone: " + zone);
        System.out.println("[ClaudePlayerController]   Owner: " + 
                          (owner != null ? owner.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Message prefix: " + messagePrefix);
        System.out.println("[ClaudePlayerController]   Add suffix: " + addSuffix);
        
        super.reveal(cards, zone, owner, messagePrefix, addSuffix);
        
        System.out.println("[ClaudePlayerController] reveal(CardCollectionView) completed");
    }
    
    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, 
                      String messagePrefix, boolean addSuffix) {
        System.out.println("[ClaudePlayerController] reveal(List<CardView>) called");
        System.out.println("[ClaudePlayerController]   Cards count: " + 
                          (cards != null ? cards.size() : 0));
        System.out.println("[ClaudePlayerController]   Zone: " + zone);
        System.out.println("[ClaudePlayerController]   Owner: " + 
                          (owner != null ? owner.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Message prefix: " + messagePrefix);
        System.out.println("[ClaudePlayerController]   Add suffix: " + addSuffix);
        
        super.reveal(cards, zone, owner, messagePrefix, addSuffix);
        
        System.out.println("[ClaudePlayerController] reveal(List<CardView>) completed");
    }
    
    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        System.out.println("[ClaudePlayerController] arrangeForScry called");
        System.out.println("[ClaudePlayerController]   Cards to scry: " + 
                          (topN != null ? topN.size() : 0));
        if (topN != null && !topN.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Cards: " + topN);
        }
        
        ImmutablePair<CardCollection, CardCollection> result = super.arrangeForScry(topN);
        
        if (result != null) {
            System.out.println("[ClaudePlayerController] arrangeForScry returning: " + 
                              result.left.size() + " on top, " + result.right.size() + " on bottom");
        } else {
            System.out.println("[ClaudePlayerController] arrangeForScry returning: null");
        }
        return result;
    }
    
    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        System.out.println("[ClaudePlayerController] arrangeForSurveil called");
        System.out.println("[ClaudePlayerController]   Cards to surveil: " + 
                          (topN != null ? topN.size() : 0));
        if (topN != null && !topN.isEmpty()) {
            System.out.println("[ClaudePlayerController]   Cards: " + topN);
        }
        
        ImmutablePair<CardCollection, CardCollection> result = super.arrangeForSurveil(topN);
        
        if (result != null) {
            System.out.println("[ClaudePlayerController] arrangeForSurveil returning: " + 
                              result.left.size() + " on top, " + result.right.size() + " to graveyard");
        } else {
            System.out.println("[ClaudePlayerController] arrangeForSurveil returning: null");
        }
        return result;
    }
    
    @Override
    public boolean willPutCardOnTop(Card c) {
        System.out.println("[ClaudePlayerController] willPutCardOnTop called");
        System.out.println("[ClaudePlayerController]   Card: " + 
                          (c != null ? c.getName() : "null"));
        
        boolean result = super.willPutCardOnTop(c);
        
        System.out.println("[ClaudePlayerController] willPutCardOnTop returning: " + result);
        return result;
    }
    
    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, 
                                                  SpellAbility source) {
        System.out.println("[ClaudePlayerController] orderMoveToZoneList called");
        System.out.println("[ClaudePlayerController]   Cards count: " + 
                          (cards != null ? cards.size() : 0));
        System.out.println("[ClaudePlayerController]   Destination zone: " + destinationZone);
        System.out.println("[ClaudePlayerController]   Source: " + 
                          (source != null ? source.getDescription() : "null"));
        
        CardCollectionView result = super.orderMoveToZoneList(cards, destinationZone, source);
        
        System.out.println("[ClaudePlayerController] orderMoveToZoneList returning ordered list of " + 
                          (result != null ? result.size() + " cards" : "null"));
        return result;
    }
    
    @Override
    public CardCollection chooseCardsToDiscardFrom(Player p, SpellAbility sa, CardCollection validCards, 
                                                   int min, int max) {
        System.out.println("[ClaudePlayerController] chooseCardsToDiscardFrom called");
        System.out.println("[ClaudePlayerController]   Player: " + 
                          (p != null ? p.getName() : "null"));
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Valid cards count: " + 
                          (validCards != null ? validCards.size() : 0));
        System.out.println("[ClaudePlayerController]   Min: " + min + ", Max: " + max);
        
        CardCollection result = super.chooseCardsToDiscardFrom(p, sa, validCards, min, max);
        
        System.out.println("[ClaudePlayerController] chooseCardsToDiscardFrom returning: " + 
                          (result != null ? result.size() + " cards to discard" : "null"));
        return result;
    }
    
    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        System.out.println("[ClaudePlayerController] playSpellAbilityNoStack called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (effectSA != null ? effectSA.getDescription() : "null"));
        System.out.println("[ClaudePlayerController]   Can setup targets: " + canSetupTargets);
        
        super.playSpellAbilityNoStack(effectSA, canSetupTargets);
        
        System.out.println("[ClaudePlayerController] playSpellAbilityNoStack completed");
    }
    
    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        System.out.println("[ClaudePlayerController] chooseCardsToDelve called");
        System.out.println("[ClaudePlayerController]   Generic amount: " + genericAmount);
        System.out.println("[ClaudePlayerController]   Graveyard size: " + 
                          (grave != null ? grave.size() : 0));
        
        CardCollectionView result = super.chooseCardsToDelve(genericAmount, grave);
        
        System.out.println("[ClaudePlayerController] chooseCardsToDelve returning: " + 
                          (result != null ? result.size() + " cards to exile" : "null"));
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int num, CardCollectionView hand, 
                                                             String uType, SpellAbility sa) {
        System.out.println("[ClaudePlayerController] chooseCardsToDiscardUnlessType called");
        System.out.println("[ClaudePlayerController]   Number: " + num);
        System.out.println("[ClaudePlayerController]   Hand size: " + 
                          (hand != null ? hand.size() : 0));
        System.out.println("[ClaudePlayerController]   Unless type: " + uType);
        System.out.println("[ClaudePlayerController]   SpellAbility: " + 
                          (sa != null ? sa.getDescription() : "null"));
        
        CardCollectionView result = super.chooseCardsToDiscardUnlessType(num, hand, uType, sa);
        
        System.out.println("[ClaudePlayerController] chooseCardsToDiscardUnlessType returning: " + 
                          (result != null ? result.size() + " cards to discard" : "null"));
        return result;
    }