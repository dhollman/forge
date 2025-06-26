// Additional imports needed for the logging overrides
// Add these to ClaudePlayerController.java if not already present

import forge.ai.ComputerUtilAI;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.game.GameEntity;
import forge.game.GameType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPaymentPurpose;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;