package dev.halwax.minecraftPoker.game.round;

/**
 * Repräsentiert das Ergebnis einer Spieleraktion.
 * Verwendet als Record-Klasse für immutable Daten.
 */
public record PlayerActionResult(int newBet, int potIncrease) {
    // Record enthält automatisch:
    // - newBet: Der neue Gesamteinsatz des Spielers
    // - potIncrease: Die Erhöhung des Pots durch diese Aktion
}