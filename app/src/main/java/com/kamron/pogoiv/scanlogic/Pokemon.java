package com.kamron.pogoiv.scanlogic;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pokemon class for each form, it has a reference to the base corresponding to the number in the Pokedex.
 *
 * Created by Kamron on 7/30/2016.
 */

public class Pokemon {

    public static final String NORMAL_FORM = "NORMAL";

    public enum Gender {
        F("♀", "F"),
        M("♂", "M"),
        N("", "N");

        private String symbol;
        private String letter;

        Gender(@NonNull String symbol, @NonNull String letter) {
            this.symbol = symbol;
            this.letter = letter;
        }

        @Override public String toString() {
            return letter;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getLetter() {
            return letter;
        }
    }

    public enum Type {
        NORMAL,
        FIRE,
        WATER,
        GRASS,
        ELECTRIC,
        ICE,
        FIGHTING,
        POISON,
        GROUND,
        FLYING,
        PSYCHIC,
        BUG,
        ROCK,
        GHOST,
        DRAGON,
        DARK,
        STEEL,
        FAIRY,
    }

    /**
     * Evolutions of this Pokemon, sorted in alphabetical order.
     * Try to avoid assumptions that only hold for Gen. I Pokemon: evolutions can have smaller
     * Pokedex number, not be consecutive, etc.
     */
    public final List<Pokemon> evolutions;
    public final List<Pokemon> devolutions;

    /**
     * Pokemon name for OCR, this is what you saw in PokemonGo app.
     */
    public final String name;

    /**
     * Pokemon name for display, this is what you wanna see in GoIV's result UI.
     */
    private final String displayName;

    public final PokemonBase base;
    public final String formName;
    public final String internalFormName;
    public final boolean isNormalForm;
    // Copy of the value in the base class
    public final int number;
    public final int baseAttack;
    public final int baseDefense;
    public final int baseStamina;
    // Copy of the value in the base class
    public final int candyEvolutionCost;

    public Pokemon(PokemonBase base, @NonNull String formName, @NonNull String internalFormName, int baseAttack, int baseDefense, int baseStamina) {
        this.base = base;
        this.formName = formName;
        this.internalFormName = internalFormName;
        this.isNormalForm = internalFormName.equals(NORMAL_FORM);
        formName = base.hasMultipleForms ? " - " + formName : "";
        this.name = base.name + formName;
        this.displayName = base.displayName + formName;
        this.number = base.number;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseStamina = baseStamina;
        this.candyEvolutionCost = base.candyEvolutionCost;
        this.evolutions = new ArrayList<>();
        this.devolutions = new ArrayList<>();
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Checks if this Pokemon is the direct evolution of otherPokemon.
     * Example:
     * - Charmeleon.isInNextEvolution(Charmander) returns true
     * - Charizard.isInNextEvolution(Charmander) returns false (it has to be the NEXT evolution)
     *
     * @param otherPokemon the pokemon which is potentially an evolution of this
     * @return true if evolution
     */
    public boolean isNextEvolutionOf(Pokemon otherPokemon) {
        return otherPokemon.evolutions.contains(this);
    }

    private void addDevolutions(List<Pokemon> list) {
        list.addAll(0, devolutions);
        for (Pokemon devolution : devolutions) {
            devolution.addDevolutions(list);
        }
    }

    private void addEvolutions(List<Pokemon> list) {
        list.addAll(evolutions);
        for (Pokemon evolution : evolutions) {
            evolution.addEvolutions(list);
        }
    }

    /**
     * Returns the evolution line of this pokemon.
     *
     * @return a list with pokemon, input pokemon plus its (d)evolutions
     */
    public ArrayList<Pokemon> getEvolutionLine() {
        ArrayList<Pokemon> list = new ArrayList<>();
        addDevolutions(list);
        list.add(this);
        addEvolutions(list);
        return list;
    }
}
